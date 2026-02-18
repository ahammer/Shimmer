package com.adamhammer.shimmer.agents

import com.adamhammer.shimmer.ShimmerInstance
import java.lang.reflect.Method
import java.util.concurrent.Future

/**
 * Generic reflective dispatcher that invokes methods on a [ShimmerInstance]
 * based on an [AiDecision].
 *
 * Replaces hard-coded `when` blocks with reflective method lookup, making
 * agent dispatch work with any annotated interface — not just fixed ones.
 *
 * Usage:
 * ```kotlin
 * val dispatcher = AgentDispatcher(shimmerInstance)
 * val result = dispatcher.dispatch(decision) // invokes the method reflectively
 * ```
 */
class AgentDispatcher<T : Any>(
    private val shimmerInstance: ShimmerInstance<T>
) {
    private val methodMap: Map<String, Method> = shimmerInstance.klass.java.declaredMethods
        .associateBy { it.name }

    /**
     * Dispatch an [AiDecision] by reflectively invoking the named method
     * on the [ShimmerInstance]'s API proxy.
     *
     * Handles [Future] unwrapping automatically.
     *
     * @return the result of the method invocation (unwrapped from Future if applicable)
     * @throws IllegalArgumentException if the method is not found or arguments cannot be resolved
     */
    fun dispatch(decision: AiDecision): Any? {
        val method = methodMap[decision.method]
            ?: throw IllegalArgumentException(
                "Unknown method '${decision.method}'. Available: ${methodMap.keys}"
            )

        val args = resolveArguments(method, decision.args)
        val result = if (args.isEmpty()) {
            method.invoke(shimmerInstance.api)
        } else {
            method.invoke(shimmerInstance.api, *args.toTypedArray())
        }

        // Unwrap Future results
        return if (result is Future<*>) result.get() else result
    }

    private fun resolveArguments(method: Method, args: Map<String, String>): List<Any?> {
        checkParameterNames(method)
        return method.parameters.map { param ->
            val value = args[param.name]
            if (value != null) {
                coerceArgument(value, param.type)
            } else if (args.size == 1 && method.parameterCount == 1) {
                // If there's exactly one arg and one param, match by position
                coerceArgument(args.values.first(), param.type)
            } else if (method.parameterCount > 0 && args.isEmpty()) {
                require(false) {
                    "Method '${method.name}' requires ${method.parameterCount} argument(s) but none were provided"
                }
            } else {
                null
            }
        }
    }

    private fun coerceArgument(value: String, targetType: Class<*>): Any = when (targetType) {
        String::class.java -> value
        Int::class.java, java.lang.Integer::class.java -> value.toInt()
        Long::class.java, java.lang.Long::class.java -> value.toLong()
        Double::class.java, java.lang.Double::class.java -> value.toDouble()
        Float::class.java, java.lang.Float::class.java -> value.toFloat()
        Boolean::class.java, java.lang.Boolean::class.java -> value.toBoolean()
        else -> value
    }

    companion object {
        private val SYNTHETIC_NAME = Regex("^arg\\d+$")

        internal fun checkParameterNames(method: Method) {
            val synthetic = method.parameters.filter { SYNTHETIC_NAME.matches(it.name) }
            if (synthetic.isNotEmpty()) {
                throw IllegalStateException(
                    "Parameter names for '${method.name}' look synthetic (${synthetic.joinToString { it.name }}). " +
                    "Ensure the compiler preserves real names by adding '-parameters' (javac) " +
                    "or 'javaParameters = true' (kotlinc) to your build configuration."
                )
            }
        }
    }
}
