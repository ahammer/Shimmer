package com.adamhammer.shimmer.samples.dnd.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.skia.Image as SkiaImage
import java.util.Base64

// ── Color Palette ───────────────────────────────────────────────────────────

private val DmColor = Color(0xFF7E57C2)         // Purple for DM narration
private val SceneColor = Color(0xFF5C6BC0)       // Indigo for scene descriptions
private val SummaryColor = Color(0xFF26A69A)     // Teal for round summaries
private val WorldBuildColor = Color(0xFF78909C)  // Blue Grey for world building
private val DiceColor = Color(0xFFFFA726)        // Orange for dice rolls
private val WhisperColor = Color(0xFF8D6E63)     // Brown for whispers
private val SystemColor = Color(0xFF90A4AE)      // Light grey for system
private val ErrorColor = Color(0xFFEF5350)       // Red for errors
private val SuccessColor = Color(0xFF66BB6A)     // Green for success
private val FailColor = Color(0xFFEF5350)        // Red for failure
private val ThinkingColor = Color(0xFF546E7A)    // Dark grey for thinking

private val DarkSurface = Color(0xFF1E1E2E)
private val DarkCardColor = Color(0xFF2A2A3D)
private val DarkCardAlt = Color(0xFF252538)
private val DimText = Color(0xFFB0B0C0)
private val BrightText = Color(0xFFE0E0F0)

// ── Dark Theme ──────────────────────────────────────────────────────────────

private val DndDarkColors = darkColors(
    primary = Color(0xFF7E57C2),
    primaryVariant = Color(0xFF5C6BC0),
    secondary = Color(0xFF26A69A),
    background = DarkSurface,
    surface = DarkCardColor,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = BrightText,
    onSurface = BrightText,
    error = ErrorColor
)

// ── Main App Entry ──────────────────────────────────────────────────────────

@Composable
fun ShimmerDndAppV2() {
    val scope = rememberCoroutineScope()
    val controller = remember { ComposeGameControllerV2(scope) }

    MaterialTheme(colors = DndDarkColors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
            when (controller.screen) {
                AppScreen.SETUP -> SetupScreenV2(controller)
                AppScreen.PLAYING -> GameTimelineScreen(controller, isPostmortem = false)
                AppScreen.GAME_OVER -> GameTimelineScreen(controller, isPostmortem = true)
            }
        }
    }
}

// ── Setup Screen (cleaned up V1) ────────────────────────────────────────────

@Composable
private fun SetupScreenV2(controller: ComposeGameControllerV2) {
    val setup = controller.setupState
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Shimmer D&D", style = MaterialTheme.typography.h4, color = DmColor)
        Text(
            "Build your party, set scope, and embark.",
            style = MaterialTheme.typography.body2, color = DimText
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { controller.randomizePrimer() },
                colors = ButtonDefaults.buttonColors(backgroundColor = DmColor)
            ) { Text("🎲 Randomize") }
        }

        // Campaign card
        DarkCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Campaign Setting", style = MaterialTheme.typography.h6, color = BrightText)
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = setup.genre,
                    onValueChange = { controller.setupState = controller.setupState.copy(genre = it) },
                    label = { Text("Genre") },
                    colors = darkTextFieldColors()
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = setup.premise,
                    onValueChange = { controller.setupState = controller.setupState.copy(premise = it) },
                    label = { Text("Campaign Premise") },
                    minLines = 2,
                    colors = darkTextFieldColors()
                )
            }
        }

        // Party config card
        DarkCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Party & Rounds", style = MaterialTheme.typography.h6, color = BrightText)
                Text("Party Size: ${setup.partySize}", color = DimText)
                Slider(
                    value = setup.partySize.toFloat(),
                    onValueChange = { controller.updatePartySize(it.toInt().coerceIn(1, 4)) },
                    valueRange = 1f..4f, steps = 2,
                    colors = SliderDefaults.colors(thumbColor = DmColor, activeTrackColor = DmColor)
                )
                Text("Rounds: ${setup.maxRounds}", color = DimText)
                Slider(
                    value = setup.maxRounds.toFloat(),
                    onValueChange = { controller.updateMaxRounds(it.toInt().coerceIn(3, 100)) },
                    valueRange = 3f..100f, steps = 96,
                    colors = SliderDefaults.colors(thumbColor = DmColor, activeTrackColor = DmColor)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = setup.enableImages,
                        onCheckedChange = { controller.setupState = controller.setupState.copy(enableImages = it) },
                        colors = CheckboxDefaults.colors(checkedColor = DmColor)
                    )
                    Text("Enable Images", color = DimText)
                }
                if (setup.enableImages) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = setup.artStyle,
                        onValueChange = { controller.setupState = controller.setupState.copy(artStyle = it) },
                        label = { Text("Art Style") },
                        colors = darkTextFieldColors()
                    )
                    Text("Image Quality", style = MaterialTheme.typography.subtitle2, color = DimText)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = setup.imageModel == com.openai.models.ImageModel.DALL_E_3,
                                onClick = {
                                    controller.setupState = controller.setupState.copy(
                                        imageModel = com.openai.models.ImageModel.DALL_E_3,
                                        imageSize = com.openai.models.ImageGenerateParams.Size._1024X1024
                                    )
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = DmColor)
                            )
                            Text("High (DALL-E 3)", color = DimText)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = setup.imageModel == com.openai.models.ImageModel.DALL_E_2,
                                onClick = {
                                    controller.setupState = controller.setupState.copy(
                                        imageModel = com.openai.models.ImageModel.DALL_E_2,
                                        imageSize = com.openai.models.ImageGenerateParams.Size._256X256
                                    )
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = DmColor)
                            )
                            Text("Low (DALL-E 2)", color = DimText)
                        }
                    }
                }
            }
        }

        // Character drafts
        setup.drafts.forEachIndexed { index, draft ->
            val avatarColor = AvatarColors.colorFor(draft.name.ifBlank { "Player ${index + 1}" })
            DarkCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AvatarCircle(draft.name.ifBlank { "P${index + 1}" }, avatarColor)
                        Text("Character ${index + 1}", style = MaterialTheme.typography.h6, color = BrightText)
                    }
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = draft.name,
                        onValueChange = { controller.updateDraft(index) { old -> old.copy(name = it) } },
                        label = { Text("Name (blank = AI)") },
                        colors = darkTextFieldColors()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = draft.race,
                            onValueChange = { controller.updateDraft(index) { old -> old.copy(race = it) } },
                            label = { Text("Race") },
                            colors = darkTextFieldColors()
                        )
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = draft.characterClass,
                            onValueChange = { controller.updateDraft(index) { old -> old.copy(characterClass = it) } },
                            label = { Text("Class") },
                            colors = darkTextFieldColors()
                        )
                    }
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = draft.backstory,
                        onValueChange = { controller.updateDraft(index) { old -> old.copy(backstory = it, manualBackstory = it.isNotBlank()) } },
                        label = { Text("Backstory (blank = AI)") },
                        minLines = 2,
                        colors = darkTextFieldColors()
                    )
                }
            }
        }

        controller.errorMessage?.let {
            Text(it, color = ErrorColor)
        }

        Button(
            onClick = { controller.startGame() },
            enabled = !controller.isBusy,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = DmColor)
        ) {
            if (controller.isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White, strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Starting...", color = Color.White)
            } else {
                Text("⚔ Start Adventure", color = Color.White)
            }
        }
    }
}

// ── Unified Game Timeline Screen (Playing + Postmortem) ─────────────────────

@Composable
private fun GameTimelineScreen(controller: ComposeGameControllerV2, isPostmortem: Boolean) {
    val timelineState = rememberLazyListState()

    // Auto-scroll to bottom during live play
    if (!isPostmortem) {
        LaunchedEffect(controller.timelineEntries.size) {
            if (controller.timelineEntries.isNotEmpty()) {
                timelineState.animateScrollToItem(controller.timelineEntries.lastIndex)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky header bar
        StickyHeaderBar(controller, isPostmortem)

        // The timeline
        LazyColumn(
            state = timelineState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (isPostmortem) {
                item { PostmortemBanner(controller) }
                item { Spacer(Modifier.height(8.dp)) }
            }

            items(controller.timelineEntries, key = { it.timestamp }) { entry ->
                TimelineEntryCard(entry)
            }

            if (isPostmortem) {
                item { Spacer(Modifier.height(12.dp)) }
                item {
                    Button(
                        onClick = { controller.backToSetup() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = DmColor)
                    ) { Text("← Back to Setup", color = Color.White) }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun StickyHeaderBar(controller: ComposeGameControllerV2, isPostmortem: Boolean) {
    Surface(
        color = DarkCardAlt,
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isPostmortem) {
                    Text("⚰ Game Over", style = MaterialTheme.typography.h6, color = ErrorColor)
                } else {
                    Text(
                        "Round ${controller.currentRound}",
                        style = MaterialTheme.typography.h6, color = DmColor
                    )
                }
                Text("•", color = DimText)
                Text(controller.world.location.name, style = MaterialTheme.typography.subtitle1, color = BrightText)

                Spacer(Modifier.weight(1f))

                if (!isPostmortem && controller.isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = DmColor)
                    Spacer(Modifier.width(6.dp))
                    controller.activeTurnCharacterName?.let {
                        Text("$it thinking...", style = MaterialTheme.typography.caption, color = DimText)
                    }
                }
            }

            // Compact party HP row
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                controller.world.party.forEach { c ->
                    val color = AvatarColors.colorFor(c.name)
                    val hpRatio = if (c.maxHp == 0) 0f else c.hp.toFloat() / c.maxHp
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape).background(color)
                        )
                        Text(
                            "${c.name} ${c.hp}/${c.maxHp}",
                            style = MaterialTheme.typography.caption,
                            color = when {
                                hpRatio > 0.5f -> SuccessColor
                                hpRatio > 0.2f -> DiceColor
                                else -> FailColor
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostmortemBanner(controller: ComposeGameControllerV2) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = DarkCardAlt,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Adventure Complete", style = MaterialTheme.typography.h5, color = DmColor)
            Text(
                "The full story of your party's journey, from beginning to end.",
                style = MaterialTheme.typography.body2, color = DimText
            )
            Divider(color = DimText.copy(alpha = 0.2f), thickness = 1.dp)
            Text("Final Party Status", style = MaterialTheme.typography.subtitle1, color = BrightText)
            controller.world.party.forEach { c ->
                val color = AvatarColors.colorFor(c.name)
                val hpRatio = if (c.maxHp == 0) 0f else c.hp.toFloat() / c.maxHp
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    AvatarCircle(c.name, color, size = 28)
                    Column {
                        Text(
                            "${c.name} — ${c.race} ${c.characterClass}",
                            style = MaterialTheme.typography.body2, color = BrightText
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "HP ${c.hp}/${c.maxHp}",
                                style = MaterialTheme.typography.caption,
                                color = when {
                                    hpRatio > 0.5f -> SuccessColor
                                    hpRatio > 0.2f -> DiceColor
                                    else -> FailColor
                                }
                            )
                            Text("· ${c.status}", style = MaterialTheme.typography.caption, color = DimText)
                        }
                    }
                }
            }
            if (controller.world.questLog.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Quests", style = MaterialTheme.typography.subtitle2, color = DimText)
                controller.world.questLog.forEach { q ->
                    Text("• $q", style = MaterialTheme.typography.caption, color = DimText)
                }
            }
        }
    }
}

// ── Timeline Entry Cards ────────────────────────────────────────────────────

@Composable
private fun TimelineEntryCard(entry: TimelineEntry) {
    when (entry) {
        is TimelineEntry.RoundHeader -> RoundHeaderCard(entry)
        is TimelineEntry.DmNarration -> DmNarrationCard(entry)
        is TimelineEntry.SceneImage -> SceneImageCard(entry)
        is TimelineEntry.CharacterAction -> CharacterActionCard(entry)
        is TimelineEntry.CharacterThinking -> CharacterThinkingCard(entry)
        is TimelineEntry.DiceRoll -> DiceRollCard(entry)
        is TimelineEntry.Whisper -> WhisperCard(entry)
        is TimelineEntry.SystemMessage -> SystemMessageCard(entry)
    }
}

@Composable
private fun RoundHeaderCard(entry: TimelineEntry.RoundHeader) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Divider(modifier = Modifier.weight(1f), color = DmColor.copy(alpha = 0.4f))
        Surface(
            color = DmColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = "⚔ Round ${entry.round} — ${entry.locationName}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.subtitle2,
                color = DmColor
            )
        }
        Divider(modifier = Modifier.weight(1f), color = DmColor.copy(alpha = 0.4f))
    }
}

@Composable
private fun DmNarrationCard(entry: TimelineEntry.DmNarration) {
    val accentColor = when (entry.category) {
        TimelineEntry.DmCategory.SCENE -> SceneColor
        TimelineEntry.DmCategory.SUMMARY -> SummaryColor
        TimelineEntry.DmCategory.WORLD_BUILD -> WorldBuildColor
        TimelineEntry.DmCategory.WORLD_EVENT -> DmColor
    }
    val emoji = when (entry.category) {
        TimelineEntry.DmCategory.SCENE -> "🗺️"
        TimelineEntry.DmCategory.SUMMARY -> "📜"
        TimelineEntry.DmCategory.WORLD_BUILD -> "⚙️"
        TimelineEntry.DmCategory.WORLD_EVENT -> "🌍"
    }

    ColorBorderCard(accentColor) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "$emoji ${entry.title}",
                style = MaterialTheme.typography.subtitle2,
                color = accentColor
            )
            Text(
                entry.narrative,
                style = MaterialTheme.typography.body2,
                color = BrightText
            )
        }
    }
}

@Composable
private fun SceneImageCard(entry: TimelineEntry.SceneImage) {
    val bitmap = remember(entry.base64) { decodeBase64(entry.base64) }
    if (bitmap != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            elevation = 2.dp,
            backgroundColor = DarkCardColor
        ) {
            Column {
                Image(
                    bitmap = bitmap,
                    contentDescription = entry.caption.ifBlank { "Scene illustration" },
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
                if (entry.caption.isNotBlank()) {
                    Text(
                        entry.caption,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.caption,
                        color = DimText
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterActionCard(entry: TimelineEntry.CharacterAction) {
    val color = AvatarColors.colorFor(entry.characterName)
    val outcomeColor = when (entry.success) {
        true -> SuccessColor
        false -> FailColor
        null -> DimText
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Avatar
        AvatarCircle(entry.characterName, color, size = 36)

        // Chat bubble
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.characterName,
                style = MaterialTheme.typography.subtitle2,
                color = color
            )
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(topStart = 2.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        entry.action,
                        style = MaterialTheme.typography.body2,
                        color = BrightText
                    )
                    if (entry.outcome.isNotBlank()) {
                        Divider(color = color.copy(alpha = 0.2f))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            val icon = when (entry.success) {
                                true -> "✓"
                                false -> "✗"
                                null -> "→"
                            }
                            Text(icon, color = outcomeColor, fontSize = 12.sp)
                            Text(
                                entry.outcome,
                                style = MaterialTheme.typography.body2,
                                color = outcomeColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterThinkingCard(entry: TimelineEntry.CharacterThinking) {
    val color = AvatarColors.colorFor(entry.characterName)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // small dot instead of full avatar
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier.size(8.dp).clip(CircleShape).background(color.copy(alpha = 0.5f))
                .align(Alignment.CenterVertically),
            contentAlignment = Alignment.Center
        ) {}

        Text(
            text = "${entry.characterName} → ${entry.methodName}: ${entry.details}",
            style = MaterialTheme.typography.caption,
            color = ThinkingColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DiceRollCard(entry: TimelineEntry.DiceRoll) {
    val color = AvatarColors.colorFor(entry.characterName)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(10.dp))
        Surface(
            color = DiceColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎲", fontSize = 16.sp)
                Text(
                    "${entry.characterName} — ${entry.rollType} vs DC ${entry.difficulty}",
                    style = MaterialTheme.typography.caption,
                    color = DiceColor
                )
            }
        }
    }
}

@Composable
private fun WhisperCard(entry: TimelineEntry.Whisper) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.width(10.dp))
        Surface(
            color = WhisperColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("🤫", fontSize = 14.sp)
                Text(
                    "${entry.from} whispers to ${entry.to}: \"${entry.message}\"",
                    style = MaterialTheme.typography.caption,
                    fontStyle = FontStyle.Italic,
                    color = WhisperColor
                )
            }
        }
    }
}

@Composable
private fun SystemMessageCard(entry: TimelineEntry.SystemMessage) {
    val color = if (entry.isError) ErrorColor else SystemColor

    ColorBorderCard(color, alpha = 0.06f) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                entry.title,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Text(
                entry.details,
                style = MaterialTheme.typography.caption,
                color = if (entry.isError) ErrorColor.copy(alpha = 0.8f) else DimText
            )
        }
    }
}

// ── Reusable Components ─────────────────────────────────────────────────────

@Composable
private fun ColorBorderCard(
    accentColor: Color,
    alpha: Float = 0.08f,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).height(IntrinsicSize.Min)
    ) {
        // Colored left border
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accentColor)
        )

        Surface(
            modifier = Modifier.weight(1f),
            color = accentColor.copy(alpha = alpha),
            shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
        ) {
            Box(modifier = Modifier.padding(10.dp)) { content() }
        }
    }
}

@Composable
fun AvatarCircle(name: String, color: Color, size: Int = 32) {
    val initials = name.take(2).uppercase()
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initials,
            color = Color.White,
            fontSize = (size / 2.5).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DarkCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = DarkCardColor,
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) { content() }
    }
}

@Composable
private fun darkTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    textColor = BrightText,
    cursorColor = DmColor,
    focusedBorderColor = DmColor,
    unfocusedBorderColor = DimText.copy(alpha = 0.3f),
    focusedLabelColor = DmColor,
    unfocusedLabelColor = DimText.copy(alpha = 0.5f)
)

private fun decodeBase64(base64Data: String): ImageBitmap? {
    return try {
        val bytes = Base64.getDecoder().decode(base64Data)
        SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (_: Exception) {
        null
    }
}
