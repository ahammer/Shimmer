package com.adamhammer.shimmer.samples.dnd.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Badge
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerDndApp() {
    val scope = rememberCoroutineScope()
    val controller = remember { ComposeGameController(scope) }

    MaterialTheme {
        when (controller.screen) {
            AppScreen.SETUP -> SetupScreen(controller)
            AppScreen.PLAYING -> PlayingScreen(controller)
            AppScreen.GAME_OVER -> GameOverScreen(controller)
        }
    }
}

@Composable
private fun SetupScreen(controller: ComposeGameController) {
    val setup = controller.setupState

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Shimmer D&D", style = MaterialTheme.typography.h4)
        Text(
            "Build your party and open with a shared premise. Blank fields let the AI co-author characters.",
            style = MaterialTheme.typography.body1
        )
        Text(
            "Flow: Setup → Opening Scene → Turn Decisions → Consequences → Round Summary.",
            style = MaterialTheme.typography.caption
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Party Builder", style = MaterialTheme.typography.h6)
                Text("Party Size: ${setup.partySize}")
                Slider(
                    value = setup.partySize.toFloat(),
                    onValueChange = { controller.updatePartySize(it.toInt().coerceIn(1, 4)) },
                    valueRange = 1f..4f,
                    steps = 2
                )
            }
        }

        setup.drafts.forEachIndexed { index, draft ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Character ${index + 1}", style = MaterialTheme.typography.h6)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = draft.name,
                        onValueChange = { controller.updateDraft(index) { old -> old.copy(name = it) } },
                        label = { Text("Name (blank = AI)") }
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = draft.race,
                        onValueChange = { controller.updateDraft(index) { old -> old.copy(race = it) } },
                        label = { Text("Race (blank = AI)") }
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = draft.characterClass,
                        onValueChange = { controller.updateDraft(index) { old -> old.copy(characterClass = it) } },
                        label = { Text("Class (blank = AI)") }
                    )

                    Text(
                        "This sample runs fully automated AI players. Leave fields blank for AI-generated concepts.",
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }

        Text("Story Goal", style = MaterialTheme.typography.h6)
        Text(
            "Aim for a varied party and distinct motives. The UI is tuned to show scene → action → consequence as a readable timeline.",
            style = MaterialTheme.typography.body2
        )

        controller.errorMessage?.let {
            Text(it, color = MaterialTheme.colors.error)
        }

        Button(onClick = { controller.startGame() }, enabled = !controller.isBusy) {
            Text(if (controller.isBusy) "Starting..." else "Start Adventure")
        }
    }
}

@Composable
private fun PlayingScreen(controller: ComposeGameController) {
    val world = controller.world
    val scene = controller.currentScene
    val timelineState = rememberLazyListState()

    LaunchedEffect(controller.timeline.size) {
        if (controller.timeline.isNotEmpty()) {
            timelineState.animateScrollToItem(controller.timeline.lastIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Round ${controller.currentRound}", style = MaterialTheme.typography.h5)
                Text("•")
                Text(world.location.name, style = MaterialTheme.typography.h6)
                if (controller.isBusy) {
                    Text("•")
                    Text("The table is waiting for the next beat...")
                }
                controller.activeTurnCharacterName?.let {
                    Text("•")
                    Text("Turn: $it", style = MaterialTheme.typography.subtitle1)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Narrative Arc", style = MaterialTheme.typography.h6)
                Text("Objective: ${controller.chapterObjective}", style = MaterialTheme.typography.body1)
                Text("Dramatic Question: ${controller.dramaticQuestion}", style = MaterialTheme.typography.body2)
                Text("Momentum: ${controller.momentumLabel}", style = MaterialTheme.typography.subtitle2)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(modifier = Modifier.weight(1.8f).fillMaxHeight()) {
                Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                    Text("Current Scene", style = MaterialTheme.typography.h6)
                    Text(world.location.name, style = MaterialTheme.typography.subtitle1)
                    if (scene.asciiArt.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Surface(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                scene.asciiArt,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(scene.narrative.ifBlank { world.location.description })
                    controller.latestSummary?.let {
                        Spacer(Modifier.height(10.dp))
                        Divider()
                        Spacer(Modifier.height(10.dp))
                        Text("Latest Round Summary", style = MaterialTheme.typography.subtitle1)
                        Text(it)
                    }
                    if (scene.availableActions.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Suggested actions:")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            scene.availableActions.take(4).forEach { action ->
                                item {
                                    Surface {
                                        Text(action, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }

                    if (world.actionLog.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Divider()
                        Spacer(Modifier.height(10.dp))
                        Text("Recent Consequences", style = MaterialTheme.typography.subtitle1)
                        world.actionLog.takeLast(3).forEach { log ->
                            Text("• $log", style = MaterialTheme.typography.body2)
                        }
                    }
                }
            }

            Card(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Party", style = MaterialTheme.typography.h6)
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(world.party) { c ->
                            val hpRatio = if (c.maxHp == 0) 0f else c.hp.toFloat() / c.maxHp.toFloat()
                            Text("${c.name} (${c.race} ${c.characterClass})", style = MaterialTheme.typography.subtitle2)
                            Text("HP ${c.hp}/${c.maxHp} · AC ${c.ac}")
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(hpRatio.coerceIn(0f, 1f))
                                        .height(6.dp)
                                        .background(MaterialTheme.colors.primary)
                                )
                            }
                            if (c.status.isNotBlank()) {
                                Text("Status: ${c.status}")
                            }
                            if (c.inventory.isNotEmpty()) {
                                Text("Items: ${c.inventory.take(4).joinToString()}")
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    if (world.questLog.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Quests: ${world.questLog.joinToString(" • ")}")
                    }
                }
            }

            Card(modifier = Modifier.weight(1.8f).fillMaxHeight()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Story Timeline", style = MaterialTheme.typography.h6)
                    Text(
                        "Cause and effect across the session.",
                        style = MaterialTheme.typography.caption
                    )
                    LazyColumn(modifier = Modifier.weight(1f), state = timelineState) {
                        items(controller.timeline.takeLast(150)) { event ->
                            StoryEventCard(event)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        controller.errorMessage?.let {
            Text(it, color = MaterialTheme.colors.error)
        }
    }
}

@Composable
private fun GameOverScreen(controller: ComposeGameController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Game Over", style = MaterialTheme.typography.h4)
        Text("Final state of the party and unresolved threads.")
        controller.world.party.forEach { character ->
            Text("${character.name}: HP ${character.hp}/${character.maxHp} · ${character.status}")
        }
        if (controller.world.questLog.isNotEmpty()) {
            Text("Quests: ${controller.world.questLog.joinToString(" • ")}")
        }
        if (controller.timeline.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Last Story Beats", style = MaterialTheme.typography.h6)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(controller.timeline.takeLast(12)) { event ->
                    StoryEventCard(event)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
        Button(onClick = { controller.backToSetup() }) {
            Text("Back to Setup")
        }
    }
}

@Composable
private fun StoryEventCard(event: StoryEvent) {
    val badgeText = when (event.type) {
        StoryEventType.SYSTEM -> "SYSTEM"
        StoryEventType.ROUND -> "ROUND"
        StoryEventType.SCENE -> "SCENE"
        StoryEventType.TURN -> "TURN"
        StoryEventType.AGENT_STEP -> "STEP"
        StoryEventType.ACTION -> "ACTION"
        StoryEventType.ROLL -> "ROLL"
        StoryEventType.SUMMARY -> "SUMMARY"
        StoryEventType.ERROR -> "ERROR"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge {
                    Text(badgeText)
                }
                Text(event.title, style = MaterialTheme.typography.subtitle2)
            }
            Text(event.details, style = MaterialTheme.typography.body2)
        }
    }
}