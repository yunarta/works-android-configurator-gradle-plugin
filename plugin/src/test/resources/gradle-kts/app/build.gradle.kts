import com.mobilesolutionworks.android.gradle.configurator.substitution.worksSubstitution

plugins {
    `java-library`
}

repositories {
    google()
    jcenter()
    mavenCentral()
}

apply {
    plugin("works-publish")
    plugin("works-dependency-substitute")
}

worksSubstitution?.apply {
    substitute(spec("junit")).with(version("4.5"))
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
