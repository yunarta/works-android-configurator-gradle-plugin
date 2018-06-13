package com.mobilesolutionworks.android.gradle.configurator.base

class ModuleSpec private constructor(spec: String) {

    companion object {

        fun create(spec: String): ModuleSpec? {
            val regex = "([\\w\\.-]+)(\$|:[\\w-]+)(\$|:[\\d\\.]+)".toRegex()
            regex.find(spec)
        }
    }
}