package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Camera3DPreset
import com.example.model.Camera3DState

@Composable
fun Camera3DControls(
    cameraState: Camera3DState,
    onApplyPreset: (Camera3DPreset) -> Unit,
    onRotateYaw: (Float) -> Unit,
    onUpdateManual: (pitch: Float, yaw: Float, zoom: Float) -> Unit,
    onToggleOrbitMode: () -> Unit,
    onResetCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showManualSliders by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A17)),
        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header with quick rotate & orbit toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = null,
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VUES 3D & ORIENTATION",
                        color = Color(0xFFD4AF37),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick -45°
                    IconButton(
                        onClick = { onRotateYaw(-45f) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("rotate_ccw_45")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Rotate90DegreesCcw,
                            contentDescription = "Rotation -45°",
                            tint = Color(0xFFFAF7F2),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Quick +45°
                    IconButton(
                        onClick = { onRotateYaw(45f) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("rotate_cw_45")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Rotate90DegreesCw,
                            contentDescription = "Rotation +45°",
                            tint = Color(0xFFFAF7F2),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Toggle touch orbit mode
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (cameraState.isOrbitModeActive) Color(0xFFD4AF37) else Color(0xFF2C241D))
                            .clickable { onToggleOrbitMode() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PanTool,
                            contentDescription = "Orbite manuelle",
                            tint = if (cameraState.isOrbitModeActive) Color(0xFF1A1612) else Color(0xFFD4AF37),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Expand manual sliders
                    IconButton(
                        onClick = { showManualSliders = !showManualSliders },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Ajustements manuels",
                            tint = if (showManualSliders) Color(0xFFD4AF37) else Color(0xFF8D7B68),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // The 4 Standard Automatic 3D Presets
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Camera3DPreset.entries.forEach { preset ->
                    val isSelected = cameraState.activePreset == preset
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFFD4AF37) else Color(0xFF2A231D),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFFFFE082) else Color(0xFF4A3E34)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onApplyPreset(preset) }
                    ) {
                        Text(
                            text = preset.title,
                            color = if (isSelected) Color(0xFF171311) else Color(0xFFFAF7F2),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Real-time status / angle readout
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (cameraState.isOrbitModeActive) {
                        "Mode Orbite tactile ACTIF (glissez sur le plateau pour tourner)"
                    } else {
                        "Angle : ${cameraState.yaw.toInt()}° | Inclinaison : ${cameraState.pitch.toInt()}°"
                    },
                    color = if (cameraState.isOrbitModeActive) Color(0xFF64B5F6) else Color(0xFFB5A997),
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )

                if (cameraState.yaw != 0f || cameraState.pitch != 38f) {
                    Text(
                        text = "Réinitialiser",
                        color = Color(0xFFD4AF37),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onResetCamera() }
                            .padding(4.dp)
                    )
                }
            }

            // Expandable Manual Sliders (Yaw & Pitch)
            AnimatedVisibility(visible = showManualSliders) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // Orientation / Yaw Slider (0° - 360°)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rotation (0° - 360°)",
                            color = Color(0xFFFAF7F2),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${cameraState.yaw.toInt()}°",
                            color = Color(0xFFD4AF37),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = cameraState.yaw,
                        onValueChange = { onUpdateManual(cameraState.pitch, it, cameraState.zoom) },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFD4AF37),
                            activeTrackColor = Color(0xFFD4AF37),
                            inactiveTrackColor = Color(0xFF3E332B)
                        ),
                        modifier = Modifier.height(26.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Inclinaison / Pitch Slider (0° - 60°)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Inclinaison (Plongeante à Isométrique)",
                            color = Color(0xFFFAF7F2),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${cameraState.pitch.toInt()}°",
                            color = Color(0xFFD4AF37),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = cameraState.pitch,
                        onValueChange = { onUpdateManual(it, cameraState.yaw, cameraState.zoom) },
                        valueRange = 0f..60f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFD4AF37),
                            activeTrackColor = Color(0xFFD4AF37),
                            inactiveTrackColor = Color(0xFF3E332B)
                        ),
                        modifier = Modifier.height(26.dp)
                    )
                }
            }
        }
    }
}
