package com.mobilesolutionworks.android.gradle.configurator.testUtils

import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import java.io.File

class CopyResourceFolder(parent: File, private val resource: String) {

    val root = File(parent, resource)

    init {
        root.mkdirs()
    }

    fun create() {
        FileUtils.copyDirectory(File(javaClass.classLoader.getResource(resource).file), root)
    }

    fun delete() {
        root.deleteRecursively()
    }
}