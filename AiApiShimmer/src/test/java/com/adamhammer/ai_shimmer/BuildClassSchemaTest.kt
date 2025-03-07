package com.adamhammer.ai_shimmer
import com.adamhammer.ai_shimmer.utils.toJsonStructureString
import org.junit.jupiter.api.Test
import kotlinx.serialization.Serializable

import io.swagger.v3.oas.annotations.media.Schema
import org.junit.jupiter.api.Assertions.*


class ClassSchemaTest {
    @Serializable
    @Schema(title = "The Answer", description = "Holds the answer to the question.")
    class Answer(
        @field:Schema(title = "Answer", description = "A resoundingly deep answer to the question")
        val answer: String = ""
    )

    @Test
    fun testAnswerSchema() {
        val expect = """{
    "answer": "A resoundingly deep answer to the question"
}"""
        val result = Answer::class.toJsonStructureString()
        assertEquals(expect, result)
    }

    // 1) With an embedded enum
    enum class Color { RED, GREEN, BLUE }

    @Serializable
    @Schema(title = "EnumHolder", description = "Holds an enum value")
    class EnumHolder(
        @field:Schema(title = "Color", description = "The color enum")
        val color: Color
    )

    @Test
    fun testEnumSchema() {
        val expect = """{
    "color": "Enum color (RED/GREEN/BLUE)"
}"""
        val result = EnumHolder::class.toJsonStructureString()
        assertEquals(expect, result)
    }

    // 2) With an embedded list
    @Serializable
    @Schema(title = "ListHolder", description = "Holds a list of strings")
    class ListHolder(
        @field:Schema(title = "Items", description = "A list of items")
        val items: List<String>
    )

    @Test
    fun testListSchema() {
        val expect = """{
    "items": [
        "A list of items"
    ]
}"""

        val result = ListHolder::class.toJsonStructureString()
        assertEquals(expect, result)
    }

    // 3) With an embedded set
    @Serializable
    @Schema(title = "SetHolder", description = "Holds a set of strings")
    class SetHolder(
        @field:Schema(title = "Elements", description = "A set of elements")
        val elements: Set<String>
    )

    // 4) With an embedded map
    @Serializable
    @Schema(title = "MapHolder", description = "Holds a map of strings")
    class MapHolder(
        @field:Schema(title = "Mapping", description = "A mapping of key-value pairs")
        val mapping: Map<String, String>
    )

    @Test
    fun testMapSchema() {
        val expect = """{
    "mapping": {
        "key": "value"
    }
}"""
        val result = MapHolder::class.toJsonStructureString()
        assertEquals(expect, result)
    }

    // 5) With an embedded object
    @Serializable
    @Schema(title = "Nested", description = "A nested object")
    class Nested(
        @field:Schema(title = "Field", description = "Nested field")
        val field: String
    )

    @Serializable
    @Schema(title = "ObjectHolder", description = "Holds an object")
    class ObjectHolder(
        @field:Schema(title = "Nested", description = "A nested object")
        val nested: Nested
    )

    @Test
    fun testObjectSchema() {
        val expect = """{
    "nested": {
        "field": "Nested field"
    }
}"""
        val result = ObjectHolder::class.toJsonStructureString()
        assertEquals(expect, result)
    }

    // 6) A complex example with all of the above.
    @Serializable
    @Schema(title = "Complex", description = "A complex object with various fields")
    class Complex(
        @field:Schema(title = "Primitive", description = "A simple string")
        val primitive: String,
        @field:Schema(title = "Enum", description = "An enum field")
        val color: Color,
        @field:Schema(title = "List", description = "A list of strings")
        val list: List<String>,
        @field:Schema(title = "Set", description = "A set of strings")
        val set: Set<String>,
        @field:Schema(title = "Map", description = "A map of strings")
        val map: Map<String, String>,
        @field:Schema(title = "Object", description = "An embedded object")
        val nested: Nested
    )

    @Test
    fun testComplexSchema() {
        val expect = """{
    "color": "Enum color (RED/GREEN/BLUE)",
    "list": [
        "A list of strings"
    ],
    "map": {
        "key": "value"
    },
    "nested": {
        "field": "Nested field"
    },
    "primitive": "A simple string",
    "set": [
        "A set of strings"
    ]
}"""
        val result = Complex::class.toJsonStructureString()
        assertEquals(expect, result)
    }
}
