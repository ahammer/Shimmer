package com.adamhammer.shimmer.samples.dnd.api

import com.adamhammer.shimmer.interfaces.ToolProvider
import com.adamhammer.shimmer.model.ToolCall
import com.adamhammer.shimmer.model.ToolDefinition
import com.adamhammer.shimmer.model.ToolResult
import com.adamhammer.shimmer.samples.dnd.CharacterUtils
import com.adamhammer.shimmer.samples.dnd.model.Character

class PlayerToolProvider(
    private val characterProvider: () -> Character
) : ToolProvider {

    override fun listTools(): List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "inspect_inventory",
            description = "List the character's current inventory items.",
            inputSchema = """{"type":"object","properties":{},"additionalProperties":false}"""
        ),
        ToolDefinition(
            name = "inspect_skills",
            description = "Inspect the character's skills, saves, AC, and HP.",
            inputSchema = """{"type":"object","properties":{},"additionalProperties":false}"""
        )
    )

    override fun callTool(call: ToolCall): ToolResult {
        val character = characterProvider()
        val content = when (call.toolName) {
            "inspect_inventory" -> {
                if (character.inventory.isEmpty()) {
                    "Inventory is empty."
                } else {
                    "Inventory: ${character.inventory.joinToString(", ")}" 
                }
            }

            "inspect_skills" -> {
                val str = character.abilityScores
                buildString {
                    append("HP ${character.hp}/${character.maxHp}, AC ${character.ac}, ")
                    append("Saves ${character.savingThrows.joinToString("/")}, Skills ${character.skills.joinToString("/")}. ")
                    append("Mods STR ${CharacterUtils.abilityModifier(str.str)}, DEX ${CharacterUtils.abilityModifier(str.dex)}, ")
                    append("CON ${CharacterUtils.abilityModifier(str.con)}, INT ${CharacterUtils.abilityModifier(str.int)}, ")
                    append("WIS ${CharacterUtils.abilityModifier(str.wis)}, CHA ${CharacterUtils.abilityModifier(str.cha)}")
                }
            }

            else -> return ToolResult(
                id = call.id,
                toolName = call.toolName,
                content = "Unknown tool: ${call.toolName}",
                isError = true
            )
        }

        return ToolResult(
            id = call.id,
            toolName = call.toolName,
            content = content,
            isError = false
        )
    }
}
