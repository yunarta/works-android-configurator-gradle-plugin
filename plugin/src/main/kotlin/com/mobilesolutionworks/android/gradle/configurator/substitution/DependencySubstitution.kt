package com.mobilesolutionworks.android.gradle.configurator.substitution

import com.mobilesolutionworks.android.gradle.configurator.substitution.util.KotlinUtils
import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.artifacts.ModuleVersionSelector

class ModuleSpec constructor(val group: String, val module: String, val version: String) {

    override fun toString(): String {
        return group
                .plus(makePath(module))
                .plus(makePath(version))
    }

    val searchList: Set<String>
        get() {
            return sortedSetOf(
                    group.plus(makePath(module)).plus(makePath(version)),
                    group.plus(makePath(module)),
                    group
            )
        }

    companion object {
        private fun makePath(text: String): String {
            return when {
                text.isNotBlank() -> ":$text"
                else -> ""
            }
        }

        fun create(id: ModuleVersionSelector): ModuleSpec {
            return ModuleSpec(id.group, id.name, id.version)
        }

        @Suppress("MagicNumber")
        fun create(spec: String): ModuleSpec? {
            val pattern = "([\\w\\.-]+)(?>\$|:([\\w-]+))(?>\$|:{1}(.+))"
            return pattern.toRegex().find(spec)?.let {
                it.groupValues.let {
                    ModuleSpec(it[1], it[2], it[3])
                }
            }
        }
    }
}

class RequestSpec(val spec: ModuleSpec)


class SubstitutionImpl(private val build: String, override val request: RequestSpec) : SubstituteListSpec.Substitution {

    override var replacement: ReplacementSpec = NoReplacementSpec

    override fun with(spec: ReplacementSpec) {
        this.replacement = spec.also {
            it.build = build
        }
    }
}

open class SubstituteListContainer(val name: String) : SubstituteListSpec {

    private val requests = mutableListOf<SubstituteListSpec.Substitution>()

    val requestMap = mutableMapOf<String, SubstituteListSpec.Substitution>()

    /**
     * Always substitute specified spec with replacement spec
     */
    override fun substitute(spec: RequestSpec): SubstituteListSpec.Substitution {
        return spec.let {
            SubstitutionImpl(name, it).also {
                requests.add(it)
                requestMap[it.request.spec.toString()] = it
            }
        }
    }

    override fun spec(spec: String): RequestSpec = ModuleSpec.create(spec)?.let {
        return RequestSpec(it)
    } ?: throw IllegalArgumentException("$spec is not in valid gradle dependency format")


    override fun version(version: String): ReplacementSpec {
        return WithVersionSpec(version)
    }

}

open class RootSubstituteListContainer(private val conditional: NamedDomainObjectContainer<SubstituteListContainer>) :
        SubstituteListContainer("force") {

    /**
     * Only replace specified spec of "build" with replacement if the build types is provided during build
     */
    fun `when`(closure: Closure<*>) {
        conditional.configure(closure)
    }

    fun evaluate(configuration: String?): MutableMap<String, SubstituteListSpec.Substitution> {
        val finalized = mutableMapOf<String, SubstituteListSpec.Substitution>()
        finalized.putAll(requestMap)

        configuration?.let {
            it.splitToSequence(",").forEach {
                val findByName = conditional.findByName(KotlinUtils.trim(it))
                findByName?.let {
                    finalized.putAll(it.requestMap)
                }
            }
        }

        return finalized
    }
}