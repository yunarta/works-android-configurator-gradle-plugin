buildscript {
    var kotlinVersion: String by extra
    kotlinVersion = "1.2.50"

    repositories {
        mavenCentral()
        maven {
            url = java.net.URI("https://dl.bintray.com/mobilesolutionworks/release")
        }

    }

    dependencies {
        classpath(kotlin("gradle-plugin", kotlinVersion))
        classpath("com.mobilesolutionworks:works-publish:1.0.3")
    }
}

allprojects {
    repositories {
        mavenCentral()
    }
}

afterEvaluate {
    tasks.createLater("jacocoRootReport") {
        group = "automation"
        description = "Execute test with coverage"

        dependsOn(*childProjects.values.mapNotNull {
            it.tasks.findByPath(":${it.name}:jacocoCoverageTest")
        }.map {
            it.path
        }.toTypedArray())
    }


    tasks.createLater("describe") {
        findProject(":works-android-configure")?.also {
            it.tasks.getByNameLater(Task::class.java, "createClasspathManifest").configure {
                this@createLater.dependsOn(path)
            }
        }
    }
}