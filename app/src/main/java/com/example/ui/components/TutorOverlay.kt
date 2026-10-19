package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.TutorHint
import com.example.model.ChessMove
import com.example.model.MoveQuality

@Composable
fun TutorOverlay(
    isLearningMode: Boolean,
    onToggleLearningMode: (Boolean) -> Unit,
    showThreatMap: Boolean,
    onToggleThreatMap: (Boolean) -> Unit,
    activeHint: TutorHint?,
    onRequestHint: () -> Unit,
    lastMove: ChessMove?,
    evalScore: Int, // Positive = White advantage, Negative = Black
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A17)),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFD4AF37).copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Mode Header & Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Précepteur",
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MODE PRÉCEPTEUR",
                        color = Color(0xFFD4AF37),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 1.sp
                    )
                }

                Switch(
                    checked = isLearningMode,
                    onCheckedChange = onToggleLearningMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFD4AF37),
                        checkedTrackColor = Color(0xFF4A3E34),
                        uncheckedThumbColor = Color(0xFF8D7B68),
                        uncheckedTrackColor = Color(0xFF2B2520)
                    )
                )
            }

            AnimatedVisibility(visible = isLearningMode) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // Evaluation Bar
                    EvaluationBar(evalScore = evalScore)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Controls Row: "Conseil du Précepteur" and "Afficher Menaces"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onRequestHint,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF1A1612),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Conseil du Précepteur",
                                color = Color(0xFF1A1612),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { onToggleThreatMap(!showThreatMap) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (showThreatMap) Color(0xFFE53935) else Color(0xFFD4AF37).copy(alpha = 0.6f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (showThreatMap) Color(0x33E53935) else Color.Transparent
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (showThreatMap) Color(0xFFEF5350) else Color(0xFFD4AF37),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showThreatMap) "Masquer Menaces" else "Voir Menaces",
                                color = Color(0xFFFAF7F2),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Active Hint Banner
                    if (activeHint != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A231C)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "★ ${activeHint.tacticalTheme}",
                                        color = Color(0xFFD4AF37),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = activeHint.move.san,
                                        color = Color(0xFFFAF7F2),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = activeHint.explanation,
                                    color = Color(0xFFDCD5C9),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Last Move Quality Feedback
                    if (lastMove?.quality != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        MoveQualityBanner(quality = lastMove.quality!!, move = lastMove)
                    }
                }
            }
        }
    }
}

@Composable
private fun EvaluationBar(evalScore: Int) {
    // Score range normalized around +/- 1000 centipawns
    val normalized = ((evalScore + 1000f) / 2000f).coerceIn(0.05f, 0.95f)
    val scoreStr = if (evalScore >= 0) "+%.1f".format(evalScore / 100f) else "%.1f".format(evalScore / 100f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Marbre (Blancs) $scoreStr",
                color = Color(0xFFFAF7F2),
                fontSize = 11.sp,
                fontFamily = FontFamily.Serif
            )
            Text(
                text = "Bronze (Noirs)",
                color = Color(0xFFB5A997),
                fontSize = 11.sp,
                fontFamily = FontFamily.Serif
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF2C241D))
                .border(0.5.dp, Color(0xFFD4AF37).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(normalized)
                    .background(Color(0xFFFAF7F2))
            )
        }
    }
}

@Composable
private fun MoveQualityBanner(quality: MoveQuality, move: ChessMove) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(quality.colorHex).copy(alpha = 0.18f))
            .border(1.dp, Color(quality.colorHex).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = quality.badge,
                color = Color(quality.colorHex),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = quality.label,
                color = Color(quality.colorHex),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        Text(
            text = move.antiqueDescription,
            color = Color(0xFFFAF7F2),
            fontSize = 11.sp
        )
    }
}
