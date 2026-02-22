package com.adamhammer.shimmer
import com.adamhammer.shimmer.utils.toJsonStructureString
import org.junit.jupiter.api.Test
import kotlinx.serialization.Serializable

import com.adamhammer.shimmer.annotations.AiSchema
import org.junit.jupiter.api.Assertions.*

class ClassSchemaTest {
    @Serializable
    @AiSchema(title = "The Answer", description = "Holds the answer to the question.")
    class Answer(
        @field:AiSchema(title = "Answer", description = "A resoundingly deep answer to the question")
        val answer: String = ""
    )

    @Test
    fun testAnswerSchema() {
        val expect = """{"answer":"A resoundingly deep answer to the question"}"""
        val result = Answer::class.toJsonStructureString()
        assertEquals(expect, result)
    }

    // 1) With an embedded enum
    enum class Color { RED, GREEN, BLUE }

    @Serializable
    @AiSchema(title = "EnumHolder", description = "Holds an enum value")
    class EnumHolder(
        @field:AiSchema(title = "Color", description = "The color enum")
        val color: Color
    )

    @Test
    fun testEnumSchema() {
        val expect = """{"color":"Enum color (RED/GREEN/BLUE)"}"""
        val result = EnumHolder::class.toJsonStructureString()
        assertEquals(expect, result)
    }

    // 2) With an embedded list
    @Serializable
    @AiSchema(title = "ListHolder", description = "Holds a list of strings")
    class ListHolder(
        @field:AiSchema(title = "Items", description = "A list of items")
        val items: List<String>
    )

    @Test
    fun testListSchema() {
        val expect = """{"items":["A list of items"]}"""
        val result = ListHolder::class.toJsonStructureString()
        assertEquals(expect, result)
    }

    // 3) With an embedded set
    @Serializable
    @AiSchema(title = "SetHolder", description = "Holds a set of strings")
    class SetHolder(
        @field:AiSchema(title = "Elements", description = "A set of elements")
        val elements: Set<String>
    )

    // 4) With an embedded map
    @Serializable
    @AiSchema(title = "MapHolder", description = "Holds a map of strings")
    class MapHolder(
        @field:AiSchema(title = "Mapping", description = "A mapping of key-value pairs")
        val mapping: Map<String, String>
    )

    @Test
    fun testMapSchema() {
        val expect = """{"mapping":{"key":"value"}}"""
        val result = MapHolder::class.toJsonStructureString()
        assertEquals(expect, result)
    }

    // 5) With an embedded object
    @Serializable
    @AiSchema(title = "Nested", description = "A nested object")
    class Nested(
        @field:AiSchema(title = "Field", description = "Nested field")
        val field: String
    )

    @Serializable
    @AiSchema(title = "ObjectHolder", description = "Holds an object")
    class ObjectHolder(
        @field:AiSchema(title = "Nested", description = "A nested object")
        val nested: Nested
    )

    @Test
    fun testObjectSchema() {
        val expect = """{"nested":{"field":"Nested field"}}"""
        val result = ObjectHolder::class.toJsonStructureString()
        assertEquals(expect, result)
    }

    // 6) A complex example with all of the above.
    @Serializable
    @AiSchema(title = "Complex", description = "A complex object with various fields")
    class Complex(
        @field:AiSchema(title = "Primitive", description = "A simple string")
        val primitive: String,
        @field:AiSchema(title = "Enum", description = "An enum field")
        val color: Color,
        @field:AiSchema(title = "List", description = "A list of strings")
        val list: List<String>,
        @field:AiSchema(title = "Set", description = "A set of strings")
        val set: Set<String>,
        @field:AiSchema(title = "Map", description = "A map of strings")
        val map: Map<String, String>,
        @field:AiSchema(title = "Object", description = "An embedded object")
        val nested: Nested
    )

    @Test
    fun testComplexSchema() {
        val expect = """{"color":"Enum color (RED/GREEN/BLUE)","list":["A list of strings"],"map":{"key":"value"},"nested":{"field":"Nested field"},"primitive":"A simple string","set":["A set of strings"]}"""
        val result = Complex::class.toJsonStructureString()
        assertEquals(expect, result)
    }
}
