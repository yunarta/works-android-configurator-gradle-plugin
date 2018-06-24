package com.mobilesolutionworks.android.gradle.configurator.util

import java.io.File
import java.nio.file.Paths

@Suppress("SpreadOperator")
fun File.paths(vararg paths: String) = Paths.get(this.absolutePath, *paths).toFile()

@Suppress("SpreadOperator", "HasPlatformType")
fun File.withPaths(vararg paths: String) = Paths.get(this.absolutePath, *paths).toFile()