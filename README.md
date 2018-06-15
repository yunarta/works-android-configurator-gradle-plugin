# Project Configurator - Gradle Plugin

[![Build Status](http://jenkins.mobilesolutionworks.com:8080/job/github/job/yunarta/job/works-ci-configurator-gradle-plugin/job/master/badge/icon)](http://jenkins.mobilesolutionworks.com:8080/job/github/job/yunarta/job/works-ci-configurator-gradle-plugin/job/master/)
[![codecov](https://codecov.io/gh/yunarta/works-ci-configurator-gradle-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/yunarta/works-ci-configurator-gradle-plugin)

This plugin would help you develop a better CI flow by configuring aspect of Gradle build from command line or 
gradle.properties that relates to CI flow.

## Installation

```groovy
buildscript {
    repositories {
        maven {
            url = "https://dl.bintray.com/mobilesolutionworks/release"
        }
    }
    
    dependencies {
        classpath("com.mobilesolutionworks:works-gradle-configurator:1.0.2")
    }    
}
```
## Dependency Substitution

Applying plugin
```groovy
apply plugin: 'works-dependency-substitute'
```
There are two objectives of this plugin which are
- Force a save version against a group of conflicting version of dependencies
- Apply or combine additional substitution rule of different build type  

### Force substitution

Especially in Android development, certain dependencies that you use might bring along older version of 
Android support libraries. The most common way to solve this is to set transitive to false, so it does not
introduce the dependencies into your project.

With this substitution plugin, this can be solved even much easier

root **build.gradle**
```groovy 
subprojects {
    apply plugin: 'works-dependency-substitute'
    worksSubstitution {
        // replace all dependency with same group with specified version   
        substitute spec('com.android.support') with version('27.1.1')
        
        // replace specified dependency with specified version
        substitute spec('io.reactivex.rxjava2:rxjava') with version('2.1.12')
    }
}
```

root **build.gradle.kts**
```kotlin 
import com.mobilesolutionworks.android.gradle.configurator.substitution.worksSubstitution

subprojects {
    apply {
        plugin("works-dependency-substitute")
    }
    
    worksSubstitution?.apply {
        // replace all dependency with same group with specified version   
        substitute(spec("com.android.support")) with version("27.1.1")
        
        // replace specified dependency with specified version
        substitute(spec("io.reactivex.rxjava2:rxjava")) with version("2.1.12")
    }
}
```
The same pattern can be used to substitute dependency within module **build.gradle**

### Conditional substitution

Conditional substitution would allow you to choose which substitution to be used by supplying 
parameters from command line.

This would allow your CI to change build type easily as well, with this brief example:
- When a 2nd tier module had been compiled and release, this would allow the 1st tier module to use it for verification
- Run periodic build and link with latest version of OSS module to see if it breaks your project 

Let us get to usage example now

module **build.gradle**
```groovy
dependencies {
    implementation 'com.team:api-module:1.0.0'
    implementation 'org.oss:http:2.0.0'
}
```
root **build.gradle**

```groovy
subprojects {
    apply plugin: 'works-dependency-substitute'
    worksSubstitution {
        
        when {
            // replace dependencies when build types is integration
            integration {
                substitute spec('com.team:api-module') with version('1.+')
                substitute spec('io.reactivex.rxjava2:rxjava') with version('2.+')
            }
            
            // replace dependencies when build types is unstable
            unstable {
                substitute spec('com.team:api-module') with version('+')
                substitute spec('io.reactivex.rxjava2:rxjava') with version('+')
            }
        }   
    }
}
```

Usage
```
./gradlew build -PbuildSubstitution=[comma separated build types]
```

| Build  configuration   | Team dependency | OSS dependency  |
| ---                    | ---             | ---             |
| no args (local build)  | 1.0.0           | 2.0.0           |
| integration            | 1.+ -> 1.2.0    | 2.+ -> 2.5.0    |
| unstable               | + -> 2.0.0      | + -> 3.0.0      |

