package ca.adamhammer.shimmer.adapters

import ca.adamhammer.shimmer.interfaces.ApiAdapter
import ca.adamhammer.shimmer.model.PromptContext
import ca.adamhammer.shimmer.model.ShimmerConfigurationException
import kotlin.reflect.KClass

/**
 * Test adapter that returns a default-constructed instance of the result class.
 * Handles String, enums, boxed primitives, and data classes with all-default constructors.
 * Useful for verifying proxy wiring without making real AI calls.
 */
class StubAdapter : ApiAdapter {
    @Suppress("UNCHECKED_CAST")
    override suspend fun <R : Any> handleRequest(context: PromptContext, resultClass: KClass<R>): R {
        return when (resultClass) {
            String::class -> "" as R
            Int::class -> 0 as R
            Long::class -> 0L as R
            Double::class -> 0.0 as R
            Float::class -> 0f as R
            Boolean::class -> false as R
            else -> resolveComplexType(resultClass)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <R : Any> resolveComplexType(resultClass: KClass<R>): R {
        return when {
            resultClass.java.isEnum -> resultClass.java.enumConstants.first() as R
            List::class.java.isAssignableFrom(resultClass.java) -> emptyList<Any>() as R
            Set::class.java.isAssignableFrom(resultClass.java) -> emptySet<Any>() as R
            Map::class.java.isAssignableFrom(resultClass.java) -> emptyMap<Any, Any>() as R
            else -> constructInstance(resultClass)
        }
    }

    private fun <R : Any> constructInstance(resultClass: KClass<R>): R {
        val ktCtor = resultClass.constructors
            .firstOrNull { ctor -> ctor.parameters.all { it.isOptional } }
        if (ktCtor != null) {
            return ktCtor.callBy(emptyMap())
        }
        try {
            return resultClass.java.getDeclaredConstructor().newInstance()
        } catch (e: NoSuchMethodException) {
            throw ShimmerConfigurationException(
                "StubAdapter cannot create an instance of ${resultClass.simpleName}: " +
                "no no-arg constructor found. Ensure the class has a no-arg constructor " +
                "or all-default parameters.",
                e
            )
        }
    }
}
