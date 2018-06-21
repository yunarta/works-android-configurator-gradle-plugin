package com.mobilesolutionworks.android.gradle.configurator.report.manager

import com.mobilesolutionworks.gradle.plugin.report.manager.ReporterExtension
import com.mobilesolutionworks.gradle.plugin.report.manager.worksReportManager
import com.nhaarman.mockito_kotlin.whenever
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito

class ReporterPluginTests {

    @Test
    fun testExtension() {
        val project = Mockito.mock(Project::class.java)
        val extensions = Mockito.mock(ExtensionContainer::class.java)
        whenever(project.extensions).thenReturn(extensions)

        assertNull(project.worksReportManager)

        val container = Mockito.mock(ReporterExtension::class.java)
        whenever(extensions.findByName("worksReportManager")).thenReturn(container)
        assertEquals(container, project.worksReportManager)
    }
}