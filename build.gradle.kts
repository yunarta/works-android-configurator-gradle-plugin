buildscript {
    var kotlinVersion: String by extra
    kotlinVersion = "1.2.41"

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", kotlinVersion))
    }
}

allprojects {
    repositories {
        mavenCentral()
    }
}

afterEvaluate {
    tasks.createLater("describe") {
        findProject(":works-android-configure")?.also {
            it.tasks.getByNameLater(Task::class.java, "createClasspathManifest").configure {
                this@createLater.dependsOn(path)
            }
        }
    }
}