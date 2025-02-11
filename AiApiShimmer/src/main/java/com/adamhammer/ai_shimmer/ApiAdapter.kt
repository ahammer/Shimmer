import kotlin.reflect.KClass

interface ApiAdapter<T> {
    /**
     * @param metaData       Metadata about the request (e.g. method name).
     * @param inputProperties Map of parameter names to JSON-encoded values.
     * @param resultSchema    A string representing the expected result schema.
     * @return                A JSON string that represents the result.
     */
    fun handleRequest(
        metaData: Map<String, String>,
        inputProperties: Map<String, String>,
        resultSchema: String,
        kClass: KClass<out Any>,
    ): T
}
