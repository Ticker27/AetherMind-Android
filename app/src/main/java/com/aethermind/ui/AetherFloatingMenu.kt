package com.aethermind.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
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

enum class EngineStatus {
    Ready,
    Warning,
    Locked,
    Error
}

data class FloatingMenuState(
    val engineStatus: EngineStatus = EngineStatus.Ready,
    val visionActive: Boolean = true,
    val overlayVisible: Boolean = true,
    val trajectoryReady: Boolean = true,
    val executionLocked: Boolean = true,
    val fps: Int = 30,
    val ballCount: Int = 8,
    val confidence: Int = 91,
    val targetLabel: String = "Yellow Ball",
    val modeLabel: String = "PROPOSE ONLY"
)

@Composable
fun AetherFloatingMenuRoot(
    state: FloatingMenuState,
    onClose: () -> Unit,
    onHideHud: () -> Unit,
    onReset: () -> Unit,
    onOpenPermissions: () -> Unit,
    onToggleVision: () -> Unit,
    onToggleHud: () -> Unit
) {
    val expanded = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(12.dp)
    ) {
        if (expanded.value) {
            ExpandedAetherMenu(
                state = state,
                expanded = expanded,
                onClose = onClose,
                onHideHud = onHideHud,
                onReset = onReset,
                onOpenPermissions = onOpenPermissions,
                onToggleVision = onToggleVision,
                onToggleHud = onToggleHud
            )
        } else {
            MiniAetherBubble(
                state = state,
                onClick = { expanded.value = true }
            )
        }
    }
}

@Composable
private fun MiniAetherBubble(
    state: FloatingMenuState,
    onClick: () -> Unit
) {
    val statusColor = when (state.engineStatus) {
        EngineStatus.Ready -> Color(0xFF38E27D)
        EngineStatus.Warning -> Color(0xFFFFC857)
        EngineStatus.Locked -> Color(0xFF7DA7FF)
        EngineStatus.Error -> Color(0xFFFF5F6D)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xE61B1D26),
                        Color(0xE62B2F3A)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color(0x55FFFFFF),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(statusColor)

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "AE",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${state.confidence}%",
            color = Color(0xFFD8E2FF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ExpandedAetherMenu(
    state: FloatingMenuState,
    expanded: MutableState<Boolean>,
    onClose: () -> Unit,
    onHideHud: () -> Unit,
    onReset: () -> Unit,
    onOpenPermissions: () -> Unit,
    onToggleVision: () -> Unit,
    onToggleHud: () -> Unit
) {
    val width by animateDpAsState(
        targetValue = 320.dp,
        label = "menuWidth"
    )

    Column(
        modifier = Modifier
            .width(width)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xF21A1D26))
            .border(
                width = 1.dp,
                color = Color(0x33FFFFFF),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
    ) {
        MenuHeader(
            state = state,
            onCollapse = { expanded.value = false },
            onClose = onClose
        )

        Spacer(modifier = Modifier.height(12.dp))

        EngineSummary(state)

        Spacer(modifier = Modifier.height(12.dp))

        Divider(color = Color(0x22FFFFFF))

        Spacer(modifier = Modifier.height(12.dp))

        StatusRows(state)

        Spacer(modifier = Modifier.height(14.dp))

        QuickMenuGrid(
            onToggleHud = onToggleHud,
            onToggleVision = onToggleVision,
            onOpenPermissions = onOpenPermissions
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
    state: FloatingMenuState,
    onCollapse: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Aether Engine",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = state.modeLabel,
                color = Color(0xFF9BA7C7),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        SmallIconButton(
            label = "—",
            onClick = onCollapse
        )

        Spacer(modifier = Modifier.width(8.dp))

        SmallIconButton(
            label = "×",
            onClick = onClose
        )
    }
}

@Composable
private fun EngineSummary(state: FloatingMenuState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF222735))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniRadar()

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Vision Pipeline",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = if (state.visionActive) "READY / ${state.fps} FPS" else "OFFLINE",
                color = Color(0xFF9BA7C7),
                fontSize = 12.sp
            )
        }

        Text(
            text = "${state.confidence}%",
            color = Color(0xFF38E27D),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatusRows(state: FloatingMenuState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusRow("Overlay", if (state.overlayVisible) "ON" else "OFF", state.overlayVisible)
        StatusRow("Vision", if (state.visionActive) "ACTIVE" else "OFF", state.visionActive)
        StatusRow("Trajectory", if (state.trajectoryReady) "READY" else "WAITING", state.trajectoryReady)
        StatusRow("Execution", if (state.executionLocked) "LOCKED" else "ACTIVE", !state.executionLocked)
    }
}

@Composable
private fun StatusRow(
    title: String,
    value: String,
    positive: Boolean
) {
    val color = if (positive) Color(0xFF38E27D) else Color(0xFFFFC857)

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
    onOpenPermissions: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MenuChip(
                text = "HUD",
                modifier = Modifier.weight(1f),
                onClick = onToggleHud
            )

            MenuChip(
                text = "Vision",
                modifier = Modifier.weight(1f),
                onClick = onToggleVision
            )

            MenuChip(
                text = "Aim",
                modifier = Modifier.weight(1f),
                onClick = {}
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MenuChip(
                text = "Debug",
                modifier = Modifier.weight(1f),
                onClick = {}
            )

            MenuChip(
                text = "Rules",
                modifier = Modifier.weight(1f),
                onClick = {}
            )

            MenuChip(
                text = "Perms",
                modifier = Modifier.weight(1f),
                onClick = onOpenPermissions
            )
        }
    }
}

@Composable
private fun VisionCard(state: FloatingMenuState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF11141C))
            .border(
                width = 1.dp,
                color = Color(0x22FFFFFF),
                shape = RoundedCornerShape(18.dp)
            )
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
        InfoLine("Target", state.targetLabel)
        InfoLine("Confidence", "${state.confidence}%")
    }
}

@Composable
private fun BottomActions(
    onHideHud: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MenuChip(
            text = "Hide HUD",
            modifier = Modifier.weight(1f),
            onClick = onHideHud
        )

        MenuChip(
            text = "Reset",
            modifier = Modifier.weight(1f),
            onClick = onReset
        )

        MenuChip(
            text = "Close",
            modifier = Modifier.weight(1f),
            danger = true,
            onClick = onClose
        )
    }
}

@Composable
private fun InfoLine(
    title: String,
    value: String
) {
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
    onClick: () -> Unit
) {
    val bg = if (danger) Color(0xFF3A1D26) else Color(0xFF283044)
    val fg = if (danger) Color(0xFFFF8A9A) else Color(0xFFDDE5FF)

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 38.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(
                width = 1.dp,
                color = Color(0x22FFFFFF),
                shape = RoundedCornerShape(14.dp)
            )
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
private fun SmallIconButton(
    label: String,
    onClick: () -> Unit
) {
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
            .size(9.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun MiniRadar() {
    Canvas(
        modifier = Modifier.size(38.dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = Color(0x5538E27D),
            radius = size.minDimension * 0.48f,
            center = center
        )
        drawCircle(
            color = Color(0xFF38E27D),
            radius = size.minDimension * 0.16f,
            center = center
        )
        drawLine(
            color = Color(0xAA38E27D),
            start = center,
            end = Offset(size.width * 0.85f, size.height * 0.25f),
            strokeWidth = 3f
        )
    }
}
