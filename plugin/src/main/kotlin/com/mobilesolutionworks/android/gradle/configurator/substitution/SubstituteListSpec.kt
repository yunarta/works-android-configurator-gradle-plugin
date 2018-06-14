package com.mobilesolutionworks.android.gradle.configurator.substitution

import org.gradle.api.artifacts.DependencyResolveDetails

interface SubstituteListSpec {

    fun substitute(spec: RequestSpec?): Substitution?

    fun group(group: String): RequestSpec?

    fun spec(spec: String): RequestSpec?

    fun version(version: String): ReplacementSpec

    interface Substitution {

        val request: RequestSpec

        var replacement: ReplacementSpec?

        fun with(spec: ReplacementSpec)
    }
}

sealed class ReplacementSpec {

    abstract fun replace(resolve: DependencyResolveDetails)
}

class WithVersionSpec(val version: String) : ReplacementSpec() {

    override fun replace(resolve: DependencyResolveDetails) {
        resolve.useVersion(version)
    }

    override fun toString(): String {
        return "replace with version $version"
    }

}

