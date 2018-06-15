buildCount = env.DEFAULT_HISTORY_COUNT ?: "5"

pipeline {
    agent {
        node {
            label 'java'
        }
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: buildCount))
        disableConcurrentBuilds()
    }

    stages {
        stage('Select') {
            parallel {
                stage('Checkout') {
                    when {
                        expression {
                            notIntegration()
                        }
                    }

                    steps {
                        checkout scm
                        seedReset()
                    }
                }

                stage('Integrate') {
                    when {
                        expression {
                            isIntegration()
                        }
                    }

                    steps {
                        echo "Execute integration"
                        stopUnless(isStartedBy("upstream"))
                    }
                }
            }
        }

        stage("Coverage, Analyze and Test") {
            when {
                expression {
                    notIntegration() && notRelease()
                }
            }

            options {
                retry(2)
            }

            steps {
                seedGrow("test")

                echo "Build for test and analyze"
                sh '''./gradlew automationCheck -q'''
                sh """echo "Execute test"
                        ./gradlew cleanTest automationTest -PignoreFailures=${
                    seedEval("test", [1: "true", "else": "false"])
                }"""
            }
        }

        stage("Publish CAT") {
            when {
                expression {
                    notIntegration() && notRelease()
                }
            }

            steps {
                echo "Publishing test and analyze result"

                jacoco execPattern: 'plugin/build/jacoco/*.exec', classPattern: 'plugin/build/classes/kotlin/main', sourcePattern: ''
                junit allowEmptyResults: true, testResults: '**/test-results/**/*.xml'
                checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: 'plugin/build/reports/detekt/detekt-report.xml', unHealthy: ''

                codeCoverage()
            }
        }

        stage("Build") {
            when {
                expression {
                    notIntegration() && notFeatureBranch()
                }
            }

            parallel {
                stage("Snapshot") {
                    when {
                        expression {
                            notRelease()
                        }
                    }

                    steps {
                        sh './gradlew clean worksGeneratePublication'
                    }
                }

                stage("Release") {
                    when {
                        expression {
                            isRelease()
                        }
                    }

                    steps {
                        sh """./gradlew cleanTest test worksGeneratePublication -PignoreFailures=false"""
                    }
                }
            }
        }

        stage("Compare") {
            when {
                expression {
                    notIntegration() && notFeatureBranch()
                }
            }


            parallel {
                stage("Snapshot") {
                    when {
                        expression {
                            notRelease()
                        }
                    }

                    steps {
                        echo "Compare snapshot"
                        compareArtifact("snapshot", "integrate/snapshot")
                    }
                }

                stage("Release") {
                    when {
                        expression {
                            isRelease()
                        }
                    }

                    steps {
                        echo "Compare release"
                        compareArtifact("release", "integrate/release")
                    }
                }
            }
        }

        stage("Publish") {
            when {
                expression {
                    doPublish()
                }
            }

            parallel {
                stage("Snapshot") {
                    when {
                        expression {
                            notIntegration() && notRelease()
                        }
                    }

                    steps {
                        echo "Publishing snapshot"
                        publish("snapshot")
                    }
                }

                stage("Release") {
                    when {
                        expression {
                            notIntegration() && isRelease()
                        }
                    }

                    steps {
                        echo "Publishing release"
                        publish("release")
                    }
                }
            }
        }
    }

    post {
        success {
            notifyDownstream()
        }
    }
}

def compareArtifact(String repo, String job) {
    bintrayDownload([
            dir       : ".compare",
            credential: "mobilesolutionworks.jfrog.org",
            pkg       : readProperties(file: 'plugin/module.properties'),
            repo      : "mobilesolutionworks/${repo}",
            src       : "plugin/build/libs"
    ])

    def same = bintrayCompare([
            dir       : ".compare",
            credential: "mobilesolutionworks.jfrog.org",
            pkg       : readProperties(file: 'plugin/module.properties'),
            repo      : "mobilesolutionworks/${repo}",
            src       : "plugin/build/libs"
    ])

    if (fileExists(".notify")) {
        sh "rm .notify"
    }

    if (same) {
        echo "Artifact output is identical, no integration needed"
    } else {
        writeFile file: ".notify", text: job
    }
}

def doPublish() {
    return fileExists(".notify")
}

def notifyDownstream() {
//    if (fileExists(".notify")) {
//
//        def job = readFile file: ".notify"
//        def encodedJob = java.net.URLEncoder.encode(job, "UTF-8")
//
//        build job: "github/yunarta/works-controller-android/${encodedJob}", propagate: false, wait: false
//    }
}

def publish(String repo) {
    def who = env.JENKINS_WHO ?: "anon"
    if (who == "works") {
        bintrayPublish([
                credential: "mobilesolutionworks.jfrog.org",
                pkg       : readProperties(file: 'plugin/module.properties'),
                repo      : "mobilesolutionworks/${repo}",
                src       : "plugin/build/libs"
        ])
    }
}

def codeCoverage() {
    withCredentials([[$class: 'StringBinding', credentialsId: "codecov-token", variable: "CODECOV_TOKEN"]]) {
        sh "curl -s https://codecov.io/bash | bash -s - -f plugin/build/reports/jacocoCoverageTest/jacocoCoverageTest.xml"
    }
}
