import java.lang.reflect.Method
import kotlin.reflect.KClass

interface ApiAdapter<T> {
    fun <R : Any> handleRequest(
        method: Method,
        args: Array<out Any>?,
        resultClass: KClass<R>,
    ): R
}
