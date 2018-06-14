package com.mobilesolutionworks.android.gradle.configurator.substitution

import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.artifacts.ModuleVersionIdentifier

class ModuleSpec constructor(val group: String, val module: String, val version: String) {

    override fun toString(): String {
        return group
                .plus((module.takeIf { it.isNotBlank() }?.let { ":$it" } ?: ""))
                .plus((version.takeIf { it.isNotBlank() }?.let { ":$it" } ?: ""))
    }

    val searchList: Set<String>
        get() {
            return sortedSetOf(
                    group.plus((module.takeIf { it.isNotBlank() }?.let { ":$it" } ?: ""))
                            .plus((version.takeIf { it.isNotBlank() }?.let { ":$it" } ?: "")),
                    group.plus((module.takeIf { it.isNotBlank() }?.let { ":$it" } ?: "")),
                    group
            )
        }

    companion object {
        fun create(id: ModuleVersionIdentifier): ModuleSpec {
            return ModuleSpec(id.module.group, id.module.name, id.version)
        }

        fun create(spec: String): ModuleSpec? {
            val pattern = "([\\w\\.-]+)(?>\$|:([\\w-]+))(?>\$|:{1}(.+))"
            return pattern.toRegex().find(spec)?.groupValues?.let {
                ModuleSpec(it[1], it[2], it[3])
            }
        }
    }
}

class RequestSpec(val spec: ModuleSpec)


class SubstitutionImpl(override val request: RequestSpec) : SubstituteListSpec.Substitution {

    override var replacement: ReplacementSpec? = null

    override fun with(spec: ReplacementSpec) {
        this.replacement = spec
    }
}

open class SubstituteListContainer(val name: String) : SubstituteListSpec {

    val requests = mutableListOf<SubstituteListSpec.Substitution>()

    val requestMap = mutableMapOf<String, SubstituteListSpec.Substitution>()

    /**
     * Always substitute specified spec with replacement spec
     */
    override fun substitute(spec: RequestSpec?): SubstituteListSpec.Substitution? {
        return spec?.let { SubstitutionImpl(it) }?.also {
            requests.add(it)
            requestMap[it.request.spec.toString()] = it
        }
    }

    override fun group(group: String): RequestSpec? = ModuleSpec.create(group)?.let {
        return RequestSpec(it)
    }

    override fun spec(spec: String): RequestSpec? = ModuleSpec.create(spec)?.let {
        return RequestSpec(it)
    }

    override fun version(version: String): ReplacementSpec {
        return WithVersionSpec(version)
    }

}

open class RootSubstituteListContainer(val conditional: NamedDomainObjectContainer<SubstituteListContainer>) :
        SubstituteListContainer("root") {

    /**
     * Only replace specified spec of "build" with replacement if the build types is provided during build
     */
    fun `when`(closure: Closure<*>) {
        conditional.configure(closure)
    }

    fun evaluate(configuration: String?): MutableMap<String, SubstituteListSpec.Substitution> {
        var finalized = mutableMapOf<String, SubstituteListSpec.Substitution>()
        finalized.putAll(requestMap)

        configuration?.let {
            it.splitToSequence(",").forEach {
                val trim = it.trim()
                val findByName = conditional.findByName(trim)
                findByName?.let {
                    finalized.putAll(it.requestMap)
                }
            }
        }

        return finalized
    }
}