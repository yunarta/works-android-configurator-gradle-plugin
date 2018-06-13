package com.mobilesolutionworks.android.gradle.configurator

class ModuleSpec private constructor(val group: String, val artifact: String, val version: String) {

    companion object {

        fun create(spec: String): ModuleSpec? {
            val pattern = "([\\w\\.-]+)(?>\$|:([\\w-]+))(?>\$|:{1}(.+))"
            return pattern.toRegex().find(spec)?.groupValues?.let {
                ModuleSpec(it[1], it[2], it[3])
            }
        }
    }
}