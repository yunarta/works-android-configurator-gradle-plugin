package com.mobilesolutionworks.android.gradle.configurator.substitution

import org.gradle.api.artifacts.DependencyResolveDetails

interface SubstituteListSpec {

    fun substitute(spec: RequestSpec): Substitution

    fun spec(spec: String): RequestSpec

    fun version(version: String): ReplacementSpec

    interface Substitution {

        val request: RequestSpec

        var replacement: ReplacementSpec

        fun with(spec: ReplacementSpec)

        fun replace(resolve: DependencyResolveDetails) {
            replacement.replace(resolve)
        }
    }
}

sealed class ReplacementSpec {

    abstract fun replace(resolve: DependencyResolveDetails)
}

class NoReplacementSpec : ReplacementSpec() {

    override fun replace(resolve: DependencyResolveDetails) {
        throw IllegalArgumentException("incomplete substitution request for ${ModuleSpec.create(resolve.requested)}")
    }
}


class WithVersionSpec(private val version: String) : ReplacementSpec() {

    override fun replace(resolve: DependencyResolveDetails) {
        resolve.useVersion(version)
    }

    override fun toString(): String {
        return "replace with version $version"
    }

}

