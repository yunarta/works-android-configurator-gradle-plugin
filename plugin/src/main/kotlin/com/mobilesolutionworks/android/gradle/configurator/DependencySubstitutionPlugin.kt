package com.mobilesolutionworks.android.gradle.configurator

import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project

class SubstitutionSpec {
}

class ReplacementSpec {
}

interface SubstituteListSpec {

    fun substitute(spec: SubstitutionSpec): Substitution

    fun group(group: String): SubstitutionSpec

    fun spec(group: String): SubstitutionSpec

    fun version(version: String): ReplacementSpec

    interface Substitution {

        fun with(spec: ReplacementSpec) {

        }
    }
}

class SubstitutionImpl : SubstituteListSpec.Substitution {

}


open class SubstituteListSpecImpl(val name: String) : SubstituteListSpec {

    /**
     * Always substitute specified spec with replacement spec
     */
    override fun substitute(spec: SubstitutionSpec): SubstituteListSpec.Substitution {
        println("spect = ${spec}")
        return SubstitutionImpl()
    }

    override fun group(group: String): SubstitutionSpec {
        return SubstitutionSpec()
    }

    override fun spec(spec: String): SubstitutionSpec {
        return SubstitutionSpec()
    }

    override fun version(version: String): ReplacementSpec {
        return ReplacementSpec()
    }

}

open class RootConditionalSubstituteListSpecImpl() : SubstituteListSpecImpl("root") {

    lateinit var conditional: NamedDomainObjectContainer<SubstituteListSpecImpl>

    /**
     * Only replace specified spec of "build" with replacement if the build types is provided during build
     */
    fun `when`(closure: Closure<*>) {
        conditional.configure(closure)
    }
}

open class ConditionalSubstituteListSpecImpl(name: String) : SubstituteListSpecImpl(name) {


}

open class ConditionalBuildContainer(val name: String) {

}

class DependencySubstitutionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val container = project.container(SubstituteListSpecImpl::class.java)

        val substitution = project.extensions.create("worksSubstitution", RootConditionalSubstituteListSpecImpl::class.java)
        substitution.conditional = container

        project.afterEvaluate {
            substitution.conditional.all {
                println("it.name = ${it.name}")
            }
        }

        project.configurations.all {
            it.resolutionStrategy {
                it.dependencySubstitution {
                    //                    it.module()
//                    it.substitute().with()
                }
            }
        }
    }
}
