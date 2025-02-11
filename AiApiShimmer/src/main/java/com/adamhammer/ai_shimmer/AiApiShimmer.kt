import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

internal class ApiInvocationHandler<T>(val adapter: ApiAdapter<T>) : InvocationHandler {
    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        // Only handle methods returning a Future.
        // Since AI is going to be async for a while
        if (Future::class.java.isAssignableFrom(method.returnType)) {
            return CompletableFuture.supplyAsync {
                val genericReturnType = method.genericReturnType
                if (genericReturnType is ParameterizedType) {
                    val actualType = genericReturnType.actualTypeArguments[0]
                    val clazz = actualType as? Class<*>
                        ?: throw UnsupportedOperationException("Expected a class type as the generic parameter")
                    val kClass = clazz.kotlin
                    adapter.handleRequest(method, args, kClass)
                } else {
                    throw IllegalStateException("Return type of method ${method.name} is not parameterized")
                }
            }
        }
        throw UnsupportedOperationException("Method ${method.name} is not supported by the proxy")
    }
}
