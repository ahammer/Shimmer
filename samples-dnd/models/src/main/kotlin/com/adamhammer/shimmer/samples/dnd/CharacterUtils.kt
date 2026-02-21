package com.adamhammer.shimmer.samples.dnd

import com.adamhammer.shimmer.samples.dnd.model.AbilityScores
import com.adamhammer.shimmer.samples.dnd.model.Character

/** D&D stat computation utilities. */
object CharacterUtils {

    fun abilityModifier(score: Int): Int = (score - 10) / 2

    /** Standard array for ability score assignment. */
    val standardArray = listOf(15, 14, 13, 12, 10, 8)

    /** Ability names in assignment order. */
    val abilityNames = listOf("STR", "DEX", "CON", "INT", "WIS", "CHA")

    fun startingHp(characterClass: String, conModifier: Int): Int {
        val hitDie = when (characterClass.lowercase()) {
            "fighter" -> 10
            "wizard" -> 6
            "rogue" -> 8
            "cleric" -> 8
            else -> 8
        }
        return (hitDie + conModifier).coerceAtLeast(1)
    }

    fun startingAc(characterClass: String, dexModifier: Int): Int {
        return when (characterClass.lowercase()) {
            "fighter" -> 16 + dexModifier.coerceAtMost(2)  // chain mail
            "rogue" -> 11 + dexModifier                     // leather armor
            "cleric" -> 14 + dexModifier.coerceAtMost(2)   // scale mail
            "wizard" -> 10 + dexModifier                     // no armor
            else -> 10 + dexModifier
        }
    }

    fun classSavingThrows(characterClass: String): List<String> = when (characterClass.lowercase()) {
        "fighter" -> listOf("STR", "CON")
        "wizard" -> listOf("INT", "WIS")
        "rogue" -> listOf("DEX", "INT")
        "cleric" -> listOf("WIS", "CHA")
        else -> listOf("STR", "DEX")
    }

    fun classSkills(characterClass: String): List<String> = when (characterClass.lowercase()) {
        "fighter" -> listOf("Athletics", "Intimidation")
        "wizard" -> listOf("Arcana", "Investigation")
        "rogue" -> listOf("Stealth", "Sleight of Hand", "Perception")
        "cleric" -> listOf("Medicine", "Religion", "Insight")
        else -> listOf("Athletics", "Perception")
    }

    fun classStartingItems(characterClass: String): List<String> = when (characterClass.lowercase()) {
        "fighter" -> listOf("Longsword", "Shield", "Chain Mail", "Torch", "Rope")
        "wizard" -> listOf("Spellbook", "Quarterstaff", "Component Pouch", "Torch", "Rope")
        "rogue" -> listOf("Shortsword", "Dagger", "Leather Armor", "Thieves' Tools", "Torch", "Rope")
        "cleric" -> listOf("Mace", "Scale Mail", "Shield", "Holy Symbol", "Torch", "Rope")
        else -> listOf("Dagger", "Torch", "Rope")
    }

    /** Build a Character from ability scores and class/race info. */
    fun buildCharacter(
        name: String,
        look: String,
        race: String,
        characterClass: String,
        abilityScores: AbilityScores,
        backstory: String,
        aiSuggestedItems: List<String> = emptyList()
    ): Character {
        val conMod = abilityModifier(abilityScores.con)
        val dexMod = abilityModifier(abilityScores.dex)
        val hp = startingHp(characterClass, conMod)
        val isKnownClass = characterClass.lowercase() in listOf("fighter", "wizard", "rogue", "cleric")
        val items = if (!isKnownClass && aiSuggestedItems.isNotEmpty()) {
            aiSuggestedItems
        } else {
            classStartingItems(characterClass)
        }
        return Character(
            name = name,
            look = look,
            race = race,
            characterClass = characterClass,
            level = 1,
            abilityScores = abilityScores,
            ac = startingAc(characterClass, dexMod),
            hp = hp,
            maxHp = hp,
            proficiencyBonus = 2,
            savingThrows = classSavingThrows(characterClass),
            skills = classSkills(characterClass),
            inventory = items,
            status = "healthy",
            backstory = backstory,
            goals = listOf(
                "Protect the party",
                "Uncover the truth behind the current quest"
            ),
            relationships = emptyMap(),
            emotionalState = "focused",
            journal = emptyList()
        )
    }

    /** Suggested ability score priority per class (for auto-assignment). */
    fun classPriority(characterClass: String): List<String> = when (characterClass.lowercase()) {
        "fighter" -> listOf("STR", "CON", "DEX", "WIS", "CHA", "INT")
        "wizard" -> listOf("INT", "DEX", "CON", "WIS", "CHA", "STR")
        "rogue" -> listOf("DEX", "INT", "CON", "CHA", "WIS", "STR")
        "cleric" -> listOf("WIS", "CON", "STR", "CHA", "DEX", "INT")
        else -> listOf("STR", "DEX", "CON", "INT", "WIS", "CHA")
    }

    /** Auto-assign the standard array based on class priority. */
    fun autoAssignScores(characterClass: String): AbilityScores {
        val priority = classPriority(characterClass)
        val assignment = mutableMapOf<String, Int>()
        priority.forEachIndexed { index, ability ->
            assignment[ability] = standardArray[index]
        }
        return AbilityScores(
            str = assignment["STR"] ?: 10,
            dex = assignment["DEX"] ?: 10,
            con = assignment["CON"] ?: 10,
            int = assignment["INT"] ?: 10,
            wis = assignment["WIS"] ?: 10,
            cha = assignment["CHA"] ?: 10
        )
    }

    /** Format a character's stats for display. */
    fun formatStats(c: Character): String = buildString {
        appendLine("═══ ${c.name} the ${c.race} ${c.characterClass} (Lv.${c.level}) ═══")
        val scores = c.abilityScores
        fun fmt(name: String, score: Int): String {
            val mod = abilityModifier(score)
            val sign = if (mod >= 0) "+" else ""
            return "$name $score ($sign$mod)"
        }
        appendLine("  ${fmt("STR", scores.str)} | ${fmt("DEX", scores.dex)} | ${fmt("CON", scores.con)}")
        appendLine("  ${fmt("INT", scores.int)} | ${fmt("WIS", scores.wis)} | ${fmt("CHA", scores.cha)}")
        appendLine("  HP: ${c.hp}/${c.maxHp}  |  AC: ${c.ac}  |  Prof: +${c.proficiencyBonus}")
        appendLine("  Saves: ${c.savingThrows.joinToString()} | Skills: ${c.skills.joinToString()}")
        appendLine("  Status: ${c.status}")
        appendLine("  Emotion: ${c.emotionalState}")
        if (c.goals.isNotEmpty()) appendLine("  Goals: ${c.goals.joinToString(" | ")}")
        appendLine("  Inventory: ${c.inventory.joinToString()}")
        if (c.backstory.isNotBlank()) {
            appendLine("  Backstory: ${c.backstory.take(120)}${if (c.backstory.length > 120) "..." else ""}")
        }
    }
}
