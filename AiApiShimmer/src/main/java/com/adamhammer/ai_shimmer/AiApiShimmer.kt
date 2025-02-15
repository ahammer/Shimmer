import com.adamhammer.ai_shimmer.Memorize
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import kotlin.reflect.KClass

// The invocation handler.
internal class ApiInvocationHandler<T>(
    private val adapter: ApiAdapter<T>
) : InvocationHandler {

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        // Only support methods returning a Future.
        if (!Future::class.java.isAssignableFrom(method.returnType)) {
            throw UnsupportedOperationException("Method ${method.name} is not supported by the proxy")
        }

        // Retrieve the memorize annotation, if present.
        val memorizeAnnotation = method.getAnnotation(Memorize::class.java)
        val memorizeKey = memorizeAnnotation?.label

        // Always execute the method asynchronously.
        return CompletableFuture.supplyAsync {
            val genericReturnType = method.genericReturnType
            if (genericReturnType is ParameterizedType) {
                val actualType = genericReturnType.actualTypeArguments[0]
                val clazz = actualType as? Class<*>
                    ?: throw UnsupportedOperationException("Expected a class type as the generic parameter")
                val kClass = clazz.kotlin

                // Execute the request using the adapter.
                val result = adapter.handleRequest(method, args, kClass)

                // If the method is annotated with @Memorize, store the result.
                if (memorizeKey != null) {
                    adapter.getMemoryMap()[memorizeKey] = result
                }
                result
            } else {
                throw IllegalStateException("Return type of method ${method.name} is not parameterized")
            }
        }
    }
}
