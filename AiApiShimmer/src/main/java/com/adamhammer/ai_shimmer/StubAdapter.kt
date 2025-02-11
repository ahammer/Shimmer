import java.lang.reflect.Method
import kotlin.reflect.KClass

class StubAdapter<T : Any>() : ApiAdapter<T> {
    override fun <R : Any> handleRequest(method: Method, args: Array<out Any>?, resultClass: KClass<R>): R {
        return resultClass.java.getDeclaredConstructor().newInstance()
    }
}
