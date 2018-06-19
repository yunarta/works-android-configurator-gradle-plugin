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
        classpath("com.mobilesolutionworks:works-publish:1.5.1")
    }
}

allprojects {
    repositories {
        mavenCentral()
    }
}
