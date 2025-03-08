package com.adamhammer.ai_shimmer.adapters

import com.adamhammer.ai_shimmer.adapters.BaseApiAdapter

import java.lang.reflect.Method
import kotlin.reflect.KClass

class StubAdapter() : BaseApiAdapter() {
    override fun <R : Any> handleRequest(method: Method, args: Array<out Any>?, resultClass: KClass<R>, memory: Map<String, String>): R {
        return resultClass.java.getDeclaredConstructor().newInstance()
    }
}
