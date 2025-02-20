package com.adamhammer.ai_shimmer.adapters

import BaseApiAdapter
import com.adamhammer.ai_shimmer.interfaces.AiDecision
import java.lang.reflect.Method
import java.util.concurrent.Future
import kotlin.reflect.KClass

class StubAdapter : BaseApiAdapter() {
    override fun <R : Any> handleRequest(method: Method, args: Array<out Any>?, resultClass: KClass<R>): R {
        return resultClass.java.getDeclaredConstructor().newInstance()
    }
}
