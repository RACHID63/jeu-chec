package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AIDifficulty
import com.example.model.ChessPiece
import com.example.model.GameStatus
import com.example.model.PieceColor
import com.example.ui.ChessViewModel
import com.example.ui.components.ChessBoard3D
import com.example.ui.components.PawnPromotionDialog
import com.example.ui.components.PuzzlesDialog
import com.example.ui.components.SculptureReferenceDialog
import com.example.ui.components.TutorOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessGameScreen(viewModel: ChessViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showDifficultyMenu by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "IMPERIUM CHESS",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFFD4AF37),
                            letterSpacing = 1.5.sp
                        )
                    }
                },
                actions = {
                    // Toggle 3D / 2D View
                    IconButton(
                        onClick = { viewModel.toggle3DMode() },
                        modifier = Modifier.testTag("toggle_3d_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "Vue 3D",
                            tint = if (uiState.is3DMode) Color(0xFFD4AF37) else Color(0xFF8D7B68)
                        )
                    }

                    // Open Reference Sheet (Nano Banana Concept Art)
                    IconButton(
                        onClick = { viewModel.toggleReferenceSheet(true) },
                        modifier = Modifier.testTag("reference_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = "Planche Sculptures",
                            tint = Color(0xFFD4AF37)
                        )
                    }

                    // Open Tactical Puzzles
                    IconButton(
                        onClick = { viewModel.togglePuzzlesDialog(true) },
                        modifier = Modifier.testTag("puzzles_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = "Épreuves",
                            tint = Color(0xFFD4AF37)
                        )
                    }

                    // Difficulty selector
                    Box {
                        IconButton(onClick = { showDifficultyMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu Difficulté",
                                tint = Color(0xFFD4AF37)
                            )
                        }
                        DropdownMenu(
                            expanded = showDifficultyMenu,
                            onDismissRequest = { showDifficultyMenu = false }
                        ) {
                            AIDifficulty.entries.forEach { diff ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = diff.title,
                                                fontWeight = if (uiState.aiDifficulty == diff) FontWeight.Bold else FontWeight.Normal,
                                                color = if (uiState.aiDifficulty == diff) Color(0xFFD4AF37) else Color.Unspecified
                                            )
                                            Text(text = diff.subtitle, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    },
                                    onClick = {
                                        viewModel.setDifficulty(diff)
                                        showDifficultyMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF14100D)
                )
            )
        },
        containerColor = Color(0xFF14100D)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Opponent Card (Noirs - Bronze Patiné)
            PlayerCard(
                name = "Adversaire IA : ${uiState.aiDifficulty.title}",
                subtitle = "Bronze Patiné (${uiState.aiDifficulty.subtitle})",
                isTurn = uiState.gameState.currentTurn == PieceColor.BLACK,
                isThinking = uiState.isAiThinking,
                capturedPieces = uiState.gameState.capturedWhite,
                color = PieceColor.BLACK
            )

            // Game Status Alert (Check / Checkmate / Stalemate)
            if (uiState.gameState.status != GameStatus.IN_PROGRESS) {
                StatusBanner(
                    status = uiState.gameState.status,
                    winner = uiState.gameState.winner
                )
            }

            // Interactive 3D Chessboard
            ChessBoard3D(
                state = uiState.gameState,
                cameraState = uiState.cameraState,
                onMoveRequested = { from, to, promo ->
                    viewModel.onMoveRequested(from, to, promo)
                },
                onManualOrbitDrag = { dYaw, dPitch ->
                    viewModel.updateCameraManual(
                        pitch = uiState.cameraState.pitch + dPitch,
                        yaw = uiState.cameraState.yaw + dYaw,
                        zoom = uiState.cameraState.zoom
                    )
                },
                hintMove = if (uiState.isLearningMode) uiState.activeHint?.move else null,
                threatSquares = if (uiState.showThreatMap) uiState.threatMap?.attackedSquaresByBlack else null,
                safeSquares = if (uiState.showThreatMap) uiState.threatMap?.safeSquaresForTurn else null,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // 3D Camera & Orientation Controls (4 standard presets + manual 45° rotation & orbit)
            com.example.ui.components.Camera3DControls(
                cameraState = uiState.cameraState,
                onApplyPreset = { viewModel.applyCameraPreset(it) },
                onRotateYaw = { viewModel.rotateYawBy(it) },
                onUpdateManual = { p, y, z -> viewModel.updateCameraManual(p, y, z) },
                onToggleOrbitMode = { viewModel.toggleOrbitMode() },
                onResetCamera = { viewModel.resetCamera() },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )

            // Player Card (Blancs - Marbre de Carrare)
            PlayerCard(
                name = "Imperator (Vous)",
                subtitle = "Marbre de Carrare",
                isTurn = uiState.gameState.currentTurn == PieceColor.WHITE,
                isThinking = false,
                capturedPieces = uiState.gameState.capturedBlack,
                color = PieceColor.WHITE
            )

            // Primary Game Controls (Nouvelle Partie, Annuler)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.startNewGame() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_game_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C241D)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Recommencer", color = Color(0xFFFAF7F2), fontSize = 13.sp)
                }

                Button(
                    onClick = { viewModel.undoMove() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("undo_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C241D)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = null,
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Annuler coup", color = Color(0xFFFAF7F2), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Interactive Learning Mode Overlay (Précepteur Romain)
            TutorOverlay(
                isLearningMode = uiState.isLearningMode,
                onToggleLearningMode = { viewModel.toggleLearningMode(it) },
                showThreatMap = uiState.showThreatMap,
                onToggleThreatMap = { viewModel.toggleThreatMap(it) },
                activeHint = uiState.activeHint,
                onRequestHint = { viewModel.requestHint() },
                lastMove = uiState.gameState.moveHistory.lastOrNull(),
                evalScore = uiState.evaluationScore,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Dialogs
        if (uiState.showReferenceSheet) {
            SculptureReferenceDialog(onDismiss = { viewModel.toggleReferenceSheet(false) })
        }

        if (uiState.showPuzzlesDialog) {
            PuzzlesDialog(
                onSelectPuzzle = { puzzle -> viewModel.loadPuzzle(puzzle) },
                onDismiss = { viewModel.togglePuzzlesDialog(false) }
            )
        }

        if (uiState.pendingPromotion != null) {
            PawnPromotionDialog(
                color = uiState.gameState.currentTurn,
                onSelect = { promoPiece -> viewModel.onPromotionSelected(promoPiece) }
            )
        }
    }
}

@Composable
private fun PlayerCard(
    name: String,
    subtitle: String,
    isTurn: Boolean,
    isThinking: Boolean,
    capturedPieces: List<ChessPiece>,
    color: PieceColor
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isTurn) Color(0xFF2A231C) else Color(0xFF1C1714))
            .border(
                width = if (isTurn) 1.2.dp else 0.5.dp,
                color = if (isTurn) Color(0xFFD4AF37) else Color(0xFF3E332B),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Colored badge
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (color == PieceColor.WHITE) Color(0xFFFAF7F2) else Color(0xFF2D7264))
                        .border(1.dp, Color(0xFFD4AF37), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name,
                    color = if (isTurn) Color(0xFFFAF7F2) else Color(0xFFC4B8A5),
                    fontWeight = if (isTurn) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp
                )

                if (isThinking) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color(0xFFD4AF37),
                        strokeWidth = 2.dp
                    )
                }
            }
            Text(
                text = subtitle,
                color = Color(0xFF8D7B68),
                fontSize = 11.sp
            )
        }

        // Trophies / Captured pieces by this player
        if (capturedPieces.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                capturedPieces.takeLast(6).forEach { p ->
                    Text(
                        text = p.type.symbol,
                        color = Color(0xFFD4AF37),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (capturedPieces.size > 6) {
                    Text(
                        text = "+${capturedPieces.size - 6}",
                        color = Color(0xFF8D7B68),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(status: GameStatus, winner: PieceColor?) {
    val bgColor = when (status) {
        GameStatus.CHECKMATE -> Color(0xFFB71C1C)
        GameStatus.CHECK -> Color(0xFFE65100)
        else -> Color(0xFF3E2723)
    }

    val statusText = when (status) {
        GameStatus.CHECKMATE -> if (winner == PieceColor.WHITE) "TRIOMPHE IMPÉRIAL ! Vous avez écrasé l'adversaire !" else "DÉFAITE ! L'adversaire s'empare de Rome."
        GameStatus.CHECK -> "ÉCHEC AU ROI ! L'Empereur est sous le feu ennemi !"
        GameStatus.STALEMATE -> "PAT ! La partie se termine par une paix négociée."
        GameStatus.DRAW_INSUFFICIENT_MATERIAL -> "NULLE : Troupes épuisées, armistice décrété."
        GameStatus.DRAW_FIFTY_MOVES -> "NULLE : Règle des 50 coups."
        else -> status.description
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(8.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = statusText,
            color = Color(0xFFFAF7F2),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = 13.sp
        )
    }
}
