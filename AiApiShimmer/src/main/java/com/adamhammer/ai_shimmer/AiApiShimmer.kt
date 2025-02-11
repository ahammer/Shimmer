import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

internal class ApiInvocationHandler<T>(val adapter: ApiAdapter<T>) : InvocationHandler {
    // Create a Json instance configured to your liking.
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(InternalSerializationApi::class)
    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        // Only handle methods returning a Future.
        if (Future::class.java.isAssignableFrom(method.returnType)) {
            return CompletableFuture.supplyAsync {
                // Build metadata (e.g. method name).
                val metaData = mapOf("method" to method.name)

                // Build a map of input properties.
                // (For simplicity we name them "arg0", "arg1", etc.
                // In a real-world scenario you might have parameter names available.)
                val inputProperties = mutableMapOf<String, String>()
                args?.forEachIndexed { index, arg ->
                    // Use Kotlin serialization to encode the argument.
                    // This assumes that each argument’s class is annotated with @Serializable.
                    val serializer = arg::class.serializer()
                    inputProperties["arg$index"] = "";//json.encodeToString(serializer, arg)
                }

                // Extract the expected result type from Future<T>.
                val genericReturnType = method.genericReturnType
                if (genericReturnType is ParameterizedType) {
                    val actualType = genericReturnType.actualTypeArguments[0]
                    val clazz = actualType as? Class<*>
                        ?: throw UnsupportedOperationException("Expected a class type as the generic parameter")
                    val kClass = clazz.kotlin

                    // Construct a (dummy) result schema.
                    val resultSchema = "Schema for ${kClass.simpleName}"

                    // Delegate to the adapter.
                    adapter.handleRequest(metaData, inputProperties, resultSchema, kClass)
                } else {
                    throw IllegalStateException("Return type of method ${method.name} is not parameterized")
                }
            }
        }
        throw UnsupportedOperationException("Method ${method.name} is not supported by the proxy")
    }
}
