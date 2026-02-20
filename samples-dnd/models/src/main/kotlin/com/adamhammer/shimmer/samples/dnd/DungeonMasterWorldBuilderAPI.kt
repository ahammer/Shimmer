package com.adamhammer.shimmer.samples.dnd

import com.adamhammer.shimmer.annotations.Terminal
import com.adamhammer.shimmer.annotations.AiOperation
import com.adamhammer.shimmer.annotations.AiResponse
import com.adamhammer.shimmer.annotations.Memorize
import com.adamhammer.shimmer.samples.dnd.model.NpcRegistryResult
import com.adamhammer.shimmer.samples.dnd.model.WorldBuildResult
import java.util.concurrent.Future

interface DungeonMasterWorldBuilderAPI {

    @AiOperation(
        summary = "Build Campaign Premise",
        description = "Define the world premise, tone, conflict, and immediate danger facing the party."
    )
    @AiResponse(description = "A concise campaign premise", responseClass = String::class)
    @Memorize(label = "world-premise")
    fun buildCampaignPremise(): Future<String>

    @AiOperation(
        summary = "Build Location Graph",
        description = "Define important locations near the opening scene and how they connect."
    )
    @AiResponse(description = "A location network summary", responseClass = String::class)
    @Memorize(label = "world-locations")
    fun buildLocationGraph(): Future<String>

    @AiOperation(
        summary = "Build NPC Registry",
        description = "Define key NPCs, their goals, and their stance toward the party."
    )
    @AiResponse(description = "A structured registry of NPC motivations", responseClass = NpcRegistryResult::class)
    @Memorize(label = "world-npcs")
    fun buildNpcRegistry(): Future<NpcRegistryResult>

    @AiOperation(
        summary = "Build Plot Hooks",
        description = "Define actionable hooks that will pull the party into the first few rounds."
    )
    @AiResponse(description = "Early-session hooks", responseClass = String::class)
    @Memorize(label = "world-hooks")
    fun buildPlotHooks(): Future<String>

    @AiOperation(
        summary = "Commit World Setup",
        description = "Commit the final world setup and opening scene as structured data."
    )
    @AiResponse(description = "Structured world setup for game start", responseClass = WorldBuildResult::class)
    @Memorize(label = "world-setup")
    @Terminal
    fun commitWorldSetup(): Future<WorldBuildResult>
}
