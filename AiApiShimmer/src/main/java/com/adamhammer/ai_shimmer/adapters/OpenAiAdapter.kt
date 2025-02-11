package com.adamhammer.ai_shimmer.adapters

import ApiAdapter
import com.adamhammer.ai_shimmer.MethodUtils
import java.lang.reflect.Method
import kotlin.reflect.KClass

class OpenAiAdapter<T : Any>() : ApiAdapter<T> {
    override fun <R : Any> handleRequest(method: Method, args: Array<out Any>?, resultClass: KClass<R>): R {
        val metaData = MethodUtils.buildMetaData(method)
        val resultSchema = MethodUtils.buildSchema(resultClass)
        val inputs = MethodUtils.buildParameterData(args);

        println("Input Schema:")
        println(metaData)
        println("Input Values:")
        println(inputs)
        println("Output Schema:")
        println(resultSchema)
        throw NotImplementedError()
    }
}