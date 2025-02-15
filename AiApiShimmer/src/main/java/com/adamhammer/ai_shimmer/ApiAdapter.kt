import java.lang.reflect.Method
import kotlin.reflect.KClass

interface ApiAdapter<T> {
    // Handles a request
    fun <R : Any> handleRequest(
        method: Method,
        args: Array<out Any>?,
        resultClass: KClass<R>,
    ): R

    // Gets the memory for this instance.
    fun getMemoryMap() : MutableMap<String, Any>

}

abstract class BaseApiAdapter<T>: ApiAdapter<T> {
    private val memoryMap: MutableMap<String, Any> = mutableMapOf()
    override fun getMemoryMap() = memoryMap
}