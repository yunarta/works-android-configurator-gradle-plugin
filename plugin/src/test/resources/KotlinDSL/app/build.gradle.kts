plugins {
    `java-library`
    kotlin("jvm")

    id("io.gitlab.arturbosch.detekt") version "1.0.0.RC7-2"
    id("com.mobilesolutionworks.gradle.publish") version "1.5.3"
    id("com.mobilesolutionworks.gradle.reporting")
}

repositories {
    google()
    jcenter()
    mavenCentral()
}

worksSubstitution {
    substitute(spec("junit")).with(version("4.5"))
}

detekt {
    version = "1.0.0.RC7-2"

    profile("main", Action {
        input = "src/main/kotlin"
        filters = ".*/resources/.*,.*/build/.*"
        config = file("default-detekt-config.yml")
        output = "$buildDir/reports/detekt"
        outputName = "detekt-report"
        baseline = "reports/baseline.xml"
    })
}


version = "1.0.0"

dependencies {
    api("io.reactivex.rxjava2:rxjava:2.1.14")
    api("junit:junit:4.4")
    implementation("junit:junit-dep:4.4")
}

fun stepIn(project: Project, root: ResolvedDependency): MutableSet<String> {
    val dependencies = mutableSetOf<String>()

    val id = root.module.id
    val key = "${id.group}:${id.module.name}:${id.version}"

    dependencies.add(key)

    root.children.forEach {
        dependencies.addAll(stepIn(project, it))
    }

    return dependencies
}

tasks.create("checkAll", Copy::class.java) {
    dependsOn("detektCheck")
    from("build/reports/detekt")
    into("build/reports/check")
}

tasks.create("worksPrintDependencies") {
    doLast {
        val dependencies = mutableSetOf<String>()
        val resolved = project.configurations.flatMapTo(dependencies) { configuration ->
            if (configuration.isCanBeResolved) {
                configuration.resolvedConfiguration.firstLevelModuleDependencies.flatMapTo(dependencies) {
                    stepIn(project, it)
                }
            } else {
                emptySet()
            }
        }

        project.properties["depOutput"]?.let {
            File(project.rootDir, it.toString())
                    .writeText(resolved.joinToString(System.lineSeparator()))
        } ?: resolved.forEach {
            println(it)
        }
    }
}
