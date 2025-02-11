import kotlin.reflect.KClass

class StubAdapter<T : Any>() : ApiAdapter<T> {
    override fun handleRequest(
        metaData: Map<String, String>,
        inputProperties: Map<String, String>,
        resultSchema: String,
        kClass: KClass<out Any>
    ): T {
        // Ensure T has a no-argument constructor.
        return kClass.java.getDeclaredConstructor().newInstance() as T
    }
}
