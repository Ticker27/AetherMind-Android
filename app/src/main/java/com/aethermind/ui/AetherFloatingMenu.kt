package com.aethermind.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethermind.ui.overlay.AiSkillLevel
import com.aethermind.ui.overlay.OverlayUiState

enum class EngineStatus {
    Ready,
    Warning,
    Locked,
    Error
}

@Composable
fun AetherFloatingMenuRoot(
    state: OverlayUiState,
    engineStatus: EngineStatus = EngineStatus.Ready,
    onClose: () -> Unit,
    onHideHud: () -> Unit,
    onReset: () -> Unit,
    onOpenPermissions: () -> Unit,
    onToggleVision: () -> Unit,
    onToggleHud: () -> Unit,
    onToggleAim: () -> Unit,
    onToggleDebug: () -> Unit,
    onSetAiSkillLevel: (AiSkillLevel) -> Unit,
    onToggleAutoPlay: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit = { _, _ -> }
) {
    val expanded = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(12.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        if (expanded.value) {
            ExpandedAetherMenu(
                state = state,
                engineStatus = engineStatus,
                expanded = expanded,
                onClose = onClose,
                onHideHud = onHideHud,
                onReset = onReset,
                onOpenPermissions = onOpenPermissions,
                onToggleVision = onToggleVision,
                onToggleHud = onToggleHud,
                onToggleAim = onToggleAim,
                onToggleDebug = onToggleDebug,
                onSetAiSkillLevel = onSetAiSkillLevel,
                onToggleAutoPlay = onToggleAutoPlay
            )
        } else {
            MiniAetherBubble(
                state = state,
                engineStatus = engineStatus,
                onClick = { expanded.value = true }
            )
        }
    }
}

@Composable
private fun MiniAetherBubble(
    state: OverlayUiState,
    engineStatus: EngineStatus,
    onClick: () -> Unit
) {
    val statusColor = statusColor(engineStatus)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xEE10141D),
                        Color(0xEE242B38)
                    )
                )
            )
            .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(statusColor)
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = "AE",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = "${state.confidence}%",
            color = Color(0xFFE5ECFF),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ExpandedAetherMenu(
    state: OverlayUiState,
    engineStatus: EngineStatus,
    expanded: MutableState<Boolean>,
    onClose: () -> Unit,
    onHideHud: () -> Unit,
    onReset: () -> Unit,
    onOpenPermissions: () -> Unit,
    onToggleVision: () -> Unit,
    onToggleHud: () -> Unit,
    onToggleAim: () -> Unit,
    onToggleDebug: () -> Unit,
    onSetAiSkillLevel: (AiSkillLevel) -> Unit,
    onToggleAutoPlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(320.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xF2141822))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        MenuHeader(
            state = state,
            onCollapse = { expanded.value = false },
            onClose = onClose
        )

        Spacer(modifier = Modifier.height(12.dp))
        EngineSummary(state, engineStatus)
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color(0x22FFFFFF))
        Spacer(modifier = Modifier.height(12.dp))
        StatusRows(state)
        Spacer(modifier = Modifier.height(14.dp))

        QuickMenuGrid(
            onToggleHud = onToggleHud,
            onToggleVision = onToggleVision,
            onToggleAim = onToggleAim,
            onToggleDebug = onToggleDebug,
            onOpenPermissions = onOpenPermissions,
            onToggleAutoPlay = onToggleAutoPlay
        )

        Spacer(modifier = Modifier.height(14.dp))
        AiSkillLevelCard(
            state = state,
            onSetAiSkillLevel = onSetAiSkillLevel
        )

        Spacer(modifier = Modifier.height(14.dp))
        AutoPlayCard(
            state = state,
            onToggleAutoPlay = onToggleAutoPlay
        )

        Spacer(modifier = Modifier.height(14.dp))
        VisionCard(state)
        Spacer(modifier = Modifier.height(14.dp))

        BottomActions(
            onHideHud = onHideHud,
            onReset = onReset,
            onClose = onClose
        )
    }
}

@Composable
private fun MenuHeader(
    state: OverlayUiState,
    onCollapse: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Aether Engine",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${state.modeLabel} / AI ${state.aiSkillShortLabel}",
                color = Color(0xFF9BA7C7),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        SmallIconButton(label = "-", onClick = onCollapse)
        Spacer(modifier = Modifier.width(8.dp))
        SmallIconButton(label = "x", onClick = onClose)
    }
}

@Composable
private fun EngineSummary(
    state: OverlayUiState,
    engineStatus: EngineStatus
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF222735))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniRadar(statusColor(engineStatus))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Vision Pipeline",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (state.showVisionMarkers) "${state.aiSkillLabel} / ${state.fps} FPS" else "MARKERS OFF",
                color = Color(0xFF9BA7C7),
                fontSize = 12.sp
            )
        }
        Text(
            text = "${state.confidence}%",
            color = statusColor(engineStatus),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatusRows(state: OverlayUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusRow("Overlay", if (state.showHud) "ON" else "OFF", state.showHud)
        StatusRow("Proposal", if (state.showAimGuide) "ACTIVE" else "OFF", state.showAimGuide)
        StatusRow("Vision", if (state.showVisionMarkers) "ACTIVE" else "OFF", state.showVisionMarkers)
        StatusRow("AI Skill", state.aiSkillShortLabel.uppercase(), true)
        StatusRow("Auto Play", "LOCKED", false, locked = true)
        StatusRow("Execution", "LOCKED", false, locked = true)
    }
}

@Composable
private fun StatusRow(
    title: String,
    value: String,
    positive: Boolean,
    locked: Boolean = false
) {
    val color = when {
        locked -> Color(0xFF4DA3FF)
        positive -> Color(0xFF38E27D)
        else -> Color(0xFFFFC857)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(color)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Color(0xFFDDE5FF),
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun QuickMenuGrid(
    onToggleHud: () -> Unit,
    onToggleVision: () -> Unit,
    onToggleAim: () -> Unit,
    onToggleDebug: () -> Unit,
    onOpenPermissions: () -> Unit,
    onToggleAutoPlay: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuChip("HUD", Modifier.weight(1f), onClick = onToggleHud)
            MenuChip("Vision", Modifier.weight(1f), onClick = onToggleVision)
            MenuChip("Aim", Modifier.weight(1f), onClick = onToggleAim)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuChip("Debug", Modifier.weight(1f), onClick = onToggleDebug)
            MenuChip("Auto", Modifier.weight(1f), warning = true, onClick = onToggleAutoPlay)
            MenuChip("Perms", Modifier.weight(1f), onClick = onOpenPermissions)
        }
    }
}


@Composable
private fun AiSkillLevelCard(
    state: OverlayUiState,
    onSetAiSkillLevel: (AiSkillLevel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF141A25))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Skill Level",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.aiSkillLevel.description,
                    color = Color(0xFF8D98B8),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
            Text(
                text = state.aiSkillShortLabel,
                color = Color(0xFF38E27D),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SkillChip(
                text = "Basic",
                selected = state.aiSkillLevel == AiSkillLevel.BEGINNER,
                modifier = Modifier.weight(1f),
                onClick = { onSetAiSkillLevel(AiSkillLevel.BEGINNER) }
            )
            SkillChip(
                text = "Smart",
                selected = state.aiSkillLevel == AiSkillLevel.INTERMEDIATE,
                modifier = Modifier.weight(1f),
                onClick = { onSetAiSkillLevel(AiSkillLevel.INTERMEDIATE) }
            )
            SkillChip(
                text = "Pro",
                selected = state.aiSkillLevel == AiSkillLevel.ADVANCED,
                modifier = Modifier.weight(1f),
                onClick = { onSetAiSkillLevel(AiSkillLevel.ADVANCED) }
            )
        }
    }
}

@Composable
private fun SkillChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFF123B2A) else Color(0xFF232A38)
    val fg = if (selected) Color(0xFF38E27D) else Color(0xFFDDE5FF)
    val border = if (selected) Color(0xAA38E27D) else Color(0x22FFFFFF)

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun VisionCard(state: OverlayUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF11141C))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Vision Snapshot",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        InfoLine("Ball Count", state.ballCount.toString())
        InfoLine("Target", state.targetBall?.label ?: "N/A")
        InfoLine("Confidence", "${state.confidence}%")
        InfoLine("AI Skill", state.aiSkillLabel)
        InfoLine("Auto Play", state.autoPlayStatus)
    }
}

@Composable
private fun AutoPlayCard(
    state: OverlayUiState,
    onToggleAutoPlay: () -> Unit
) {
    val title = if (state.autoPlayEnabled) "Auto Play Armed" else "Auto Play Off"
    val statusColor = if (state.autoPlayEnabled) Color(0xFFFFC857) else Color(0xFF8D98B8)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF171B25))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "C++ policy + package guard + emergency stop",
                    color = Color(0xFF8D98B8),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
            Text(
                text = state.autoPlayStatus,
                color = statusColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        InfoLine("Cadence", "${state.autoPlayIntervalMs} ms")
        InfoLine("Pull Power", "${state.autoPlayPowerPx.toInt()} px")

        Spacer(modifier = Modifier.height(10.dp))
        MenuChip(
            text = if (state.autoPlayEnabled) "Disable Auto" else "Enable Auto",
            modifier = Modifier.fillMaxWidth(),
            warning = !state.autoPlayEnabled,
            danger = state.autoPlayEnabled,
            onClick = onToggleAutoPlay
        )
    }
}

@Composable
private fun BottomActions(
    onHideHud: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MenuChip("Hide HUD", Modifier.weight(1f), onClick = onHideHud)
        MenuChip("Reset", Modifier.weight(1f), warning = true, onClick = onReset)
        MenuChip("Close", Modifier.weight(1f), danger = true, onClick = onClose)
    }
}

@Composable
private fun InfoLine(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFF8D98B8),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = Color(0xFFDDE5FF),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MenuChip(
    text: String,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    warning: Boolean = false,
    onClick: () -> Unit
) {
    val bg = when {
        danger -> Color(0xFF3A1D26)
        warning -> Color(0xFF5A3511)
        else -> Color(0xFF283044)
    }
    val fg = when {
        danger -> Color(0xFFFF8A9A)
        warning -> Color(0xFFFFD08A)
        else -> Color(0xFFDDE5FF)
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 38.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SmallIconButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xFF2A3040))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun MiniRadar(color: Color) {
    Canvas(modifier = Modifier.size(38.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = color.copy(alpha = 0.24f), radius = size.minDimension * 0.48f, center = center)
        drawCircle(color = color.copy(alpha = 0.48f), radius = size.minDimension * 0.32f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
        drawCircle(color = color, radius = size.minDimension * 0.14f, center = center)
        drawLine(color = color.copy(alpha = 0.90f), start = center, end = Offset(size.width * 0.86f, size.height * 0.26f), strokeWidth = 3f)
    }
}

private fun statusColor(status: EngineStatus): Color {
    return when (status) {
        EngineStatus.Ready -> Color(0xFF38E27D)
        EngineStatus.Warning -> Color(0xFFFFC857)
        EngineStatus.Locked -> Color(0xFF4DA3FF)
        EngineStatus.Error -> Color(0xFFFF5F6D)
    }
}
