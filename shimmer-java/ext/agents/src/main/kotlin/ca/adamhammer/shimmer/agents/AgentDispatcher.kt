package ca.adamhammer.shimmer.agents

import ca.adamhammer.shimmer.ShimmerInstance
import ca.adamhammer.shimmer.annotations.Terminal
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Future
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.hasAnnotation

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
    data class DispatchResult(
        val methodName: String,
        val value: Any?,
        val isTerminal: Boolean
    )

    private val methodMap: Map<String, KFunction<*>> = shimmerInstance.klass.declaredFunctions
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
    fun dispatch(decision: AiDecision): Any? = runBlocking { dispatchWithMetadata(decision).value }

    suspend fun dispatchWithMetadata(decision: AiDecision): DispatchResult {
        val method = methodMap[decision.method]
            ?: throw IllegalArgumentException(
                "Unknown method '${decision.method}'. Available: ${methodMap.keys}"
            )

        val args = resolveArguments(method, decision.argsMap())
        val result = if (method.isSuspend) {
            method.callSuspendBy(args)
        } else {
            method.callBy(args)
        }

        // Unwrap Future results
        val value = if (result is Future<*>) result.get() else result
        return DispatchResult(
            methodName = method.name,
            value = value,
            isTerminal = method.hasAnnotation<Terminal>()
        )
    }

    fun isTerminal(methodName: String): Boolean {
        val method = methodMap[methodName]
            ?: throw IllegalArgumentException(
                "Unknown method '$methodName'. Available: ${methodMap.keys}"
            )
        return method.hasAnnotation<Terminal>()
    }

    fun firstParameterlessTerminalMethodName(): String? = methodMap.values
        .firstOrNull { method ->
            method.parameters.size == 1 && method.hasAnnotation<Terminal>() // 1 parameter is the instance itself
        }
        ?.name

    private fun resolveArguments(method: KFunction<*>, args: Map<String, String>): Map<KParameter, Any?> {
        val resolvedArgs = mutableMapOf<KParameter, Any?>()
        
        // The first parameter is always the instance itself
        val instanceParam = method.parameters.first()
        resolvedArgs[instanceParam] = shimmerInstance.api

        val valueParams = method.parameters.drop(1)
        
        for (param in valueParams) {
            val value = args[param.name]
            if (value != null) {
                resolvedArgs[param] = coerceArgument(value, param.type.classifier as KClass<*>)
            } else if (args.size == 1 && valueParams.size == 1) {
                // If there's exactly one arg and one param, match by position
                resolvedArgs[param] = coerceArgument(args.values.first(), param.type.classifier as KClass<*>)
            } else if (valueParams.isNotEmpty() && args.isEmpty() && !param.isOptional) {
                require(false) {
                    "Method '${method.name}' requires ${valueParams.size} argument(s) but none were provided"
                }
            }
        }
        return resolvedArgs
    }

    private fun coerceArgument(value: String, targetType: KClass<*>): Any = when (targetType) {
        String::class -> value
        Int::class -> value.toInt()
        Long::class -> value.toLong()
        Double::class -> value.toDouble()
        Float::class -> value.toFloat()
        Boolean::class -> value.toBoolean()
        else -> value
    }
}
