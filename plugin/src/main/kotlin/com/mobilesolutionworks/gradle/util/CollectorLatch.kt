package com.mobilesolutionworks.gradle.util

internal class CollectorLatch<T>(private val size: Int, private val instance: T, private val closure: (T) -> Unit) {

    private var count = 0

    fun increment() {
        count += 1
        if (count == size) {
            closure(instance)
        }
    }
}