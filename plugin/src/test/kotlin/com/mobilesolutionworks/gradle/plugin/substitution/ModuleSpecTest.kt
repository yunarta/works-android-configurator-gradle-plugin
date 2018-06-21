package com.mobilesolutionworks.gradle.plugin.substitution

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ModuleSpecTest {

    @Test
    fun `test parsing Gradle module spec`() {
        ModuleSpec.create("com.mobilesolutionworks:works-publish:1.0.0-DEV@aar-what")?.let {
            assertEquals("com.mobilesolutionworks", it.group)
            assertEquals("works-publish", it.module)
            assertEquals("1.0.0-DEV@aar-what", it.version)
        } ?: fail()

        ModuleSpec.create("com.mobilesolutionworks:works-publish:1.0.0-DEV@aar")?.let {
            assertEquals("com.mobilesolutionworks", it.group)
            assertEquals("works-publish", it.module)
            assertEquals("1.0.0-DEV@aar", it.version)
        } ?: fail()

        ModuleSpec.create("com.mobilesolutionworks:works-publish:1.0.0-DEV@jar")?.let {
            assertEquals("com.mobilesolutionworks", it.group)
            assertEquals("works-publish", it.module)
            assertEquals("1.0.0-DEV@jar", it.version)
        } ?: fail()

        ModuleSpec.create("com.mobilesolutionworks:works-publish:1.0.0-DEV")?.let {
            assertEquals("com.mobilesolutionworks", it.group)
            assertEquals("works-publish", it.module)
            assertEquals("1.0.0-DEV", it.version)
        } ?: fail()

        ModuleSpec.create("com.mobilesolutionworks:works-publish:1.0.0")?.let {
            assertEquals("com.mobilesolutionworks", it.group)
            assertEquals("works-publish", it.module)
            assertEquals("1.0.0", it.version)
        } ?: fail()

        ModuleSpec.create("com.mobilesolutionworks:works-publish:")?.let {
            fail()
        }

        ModuleSpec.create("com.mobilesolutionworks:works-publish")?.let {
            assertEquals("com.mobilesolutionworks", it.group)
            assertEquals("works-publish", it.module)
            assertEquals("", it.version)
        } ?: fail()

        ModuleSpec.create("com.mobilesolutionworks:")?.let {
            fail()
        }

        ModuleSpec.create("com.mobilesolutionworks")?.let {
            assertEquals("com.mobilesolutionworks", it.group)
            assertEquals("", it.module)
            assertEquals("", it.version)
        } ?: fail()
    }

    @Test
    fun `test toString()`() {
        ModuleSpec.create("com.mobilesolutionworks:works-publish:1.0.0")?.let {
            assertEquals("com.mobilesolutionworks:works-publish:1.0.0", it.toString())
        } ?: fail()

        ModuleSpec.create("com.mobilesolutionworks:works-publish")?.let {
            assertEquals("com.mobilesolutionworks:works-publish", it.toString())
        } ?: fail()

        ModuleSpec.create("com.mobilesolutionworks")?.let {
            assertEquals("com.mobilesolutionworks", it.toString())
        } ?: fail()
    }
}