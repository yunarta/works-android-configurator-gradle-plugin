package com.mobilesolutionworks.android.gradle.configurator.substitution

import com.nhaarman.mockito_kotlin.whenever
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito

class DependencySubstitutionPluginTests {

    @Test
    fun testExtension() {
        val project = Mockito.mock(Project::class.java)
        val extensions = Mockito.mock(ExtensionContainer::class.java)
        whenever(project.extensions).thenReturn(extensions)

        assertNull(project.worksSubstitution)

        val container = Mockito.mock(RootSubstituteListContainer::class.java)
        whenever(extensions.findByName("worksSubstitution")).thenReturn(container)
        assertEquals(container, project.worksSubstitution)
    }
}