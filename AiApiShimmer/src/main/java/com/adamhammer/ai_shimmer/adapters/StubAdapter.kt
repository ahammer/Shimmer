package com.adamhammer.ai_shimmer.adapters

import BaseApiAdapter

import java.lang.reflect.Method
import kotlin.reflect.KClass

class StubAdapter() : BaseApiAdapter() {
    override fun <R : Any> handleRequest(method: Method, args: Array<out Any>?, resultClass: KClass<R>): R {
        return resultClass.java.getDeclaredConstructor().newInstance()
    }
}
