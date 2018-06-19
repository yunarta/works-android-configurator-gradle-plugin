buildscript {
    var kotlinVersion: String by extra
    kotlinVersion = "1.2.50"

    repositories {
        jcenter()
        google()
        mavenCentral()
        maven {
            url = java.net.URI("https://dl.bintray.com/mobilesolutionworks/release")
        }
    }

    dependencies {
        classpath(kotlin("gradle-plugin", kotlinVersion))
        classpath("com.mobilesolutionworks:works-publish:1.5.2")
    }
}

allprojects {
    repositories {
        jcenter()
        google()
        mavenCentral()
    }
}
