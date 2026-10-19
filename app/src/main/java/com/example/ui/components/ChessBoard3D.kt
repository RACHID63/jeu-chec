package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.engine.ChessEngine
import com.example.model.Camera3DState
import com.example.model.ChessMove
import com.example.model.GameState
import com.example.model.PieceType
import com.example.model.Position
import com.example.visuals.DestructionEffect
import com.example.visuals.MosaicBoardRenderer
import com.example.visuals.SculpturesRenderer
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ChessBoard3D(
    state: GameState,
    cameraState: Camera3DState,
    onMoveRequested: (from: Position, to: Position, promotionPiece: PieceType?) -> Unit,
    onManualOrbitDrag: ((deltaYaw: Float, deltaPitch: Float) -> Unit)? = null,
    hintMove: ChessMove? = null,
    threatSquares: Set<Position>? = null,
    safeSquares: Set<Position>? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedPosition by remember { mutableStateOf<Position?>(null) }
    var legalMovesForSelected by remember { mutableStateOf<List<ChessMove>>(emptyList()) }

    // Active particle destruction effects
    val activeDestructions = remember { mutableStateListOf<DestructionEffect>() }

    // Smooth animation for 3D camera angles
    val animatedPitch by animateFloatAsState(
        targetValue = cameraState.pitch,
        animationSpec = tween(durationMillis = 400),
        label = "pitch"
    )
    val animatedYaw by animateFloatAsState(
        targetValue = cameraState.yaw,
        animationSpec = tween(durationMillis = 400),
        label = "yaw"
    )

    // Detect captures from move history to trigger haptic feedback
    LaunchedEffect(state.moveHistory.size) {
        val lastMove = state.moveHistory.lastOrNull()
        if (lastMove?.capturedPiece != null) {
            triggerHapticFeedback(context)
        }
    }

    // Frame loop for particle simulation
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                val dt = (now - lastTime) / 1_000_000_000f
                lastTime = now

                val iterator = activeDestructions.iterator()
                while (iterator.hasNext()) {
                    val effect = iterator.next()
                    effect.update(dt)
                    if (effect.isFinished) {
                        iterator.remove()
                    }
                }
            }
        }
    }

    // Clear selection if turn changed
    LaunchedEffect(state.currentTurn) {
        selectedPosition = null
        legalMovesForSelected = emptyList()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(4.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // Orbit Drag Gesture (when orbit mode is active)
                .pointerInput(cameraState.isOrbitModeActive, cameraState.yaw, cameraState.pitch) {
                    if (cameraState.isOrbitModeActive) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val dYaw = dragAmount.x * 0.45f
                            val dPitch = -dragAmount.y * 0.35f
                            onManualOrbitDrag?.invoke(dYaw, dPitch)
                        }
                    }
                }
                // Tap Gestures to select and move upright pieces
                .pointerInput(state, selectedPosition, legalMovesForSelected, animatedPitch, animatedYaw) {
                    detectTapGestures { tapOffset ->
                        val boardSize = size.width.toFloat()
                        val center = Offset(boardSize / 2f, boardSize / 2f)
                        val boardRadius = boardSize * 0.42f
                        val pieceHeight = (boardRadius / 4f) * 1.65f

                        // 1. First check if user tapped on any standing upright piece (from top to base)
                        // Iterate in reverse depth order (front pieces first)
                        var hitPos: Position? = null

                        for (r in 7 downTo 0) {
                            for (c in 0..7) {
                                val p = state.board[r][c] ?: continue
                                val u = ((c + 0.5f) / 4f) - 1f
                                val v = ((r + 0.5f) / 4f) - 1f
                                val groundPt = MosaicBoardRenderer.projectPoint(u, v, animatedYaw, animatedPitch, center, boardRadius)

                                val pieceW = pieceHeight * 0.72f
                                val topY = groundPt.y - pieceHeight
                                val bottomY = groundPt.y

                                if (tapOffset.x in (groundPt.x - pieceW / 2f)..(groundPt.x + pieceW / 2f) &&
                                    tapOffset.y in topY..bottomY
                                ) {
                                    hitPos = Position(r, c)
                                    break
                                }
                            }
                            if (hitPos != null) break
                        }

                        // 2. If no upright piece touched, check the ground tile projection
                        if (hitPos == null) {
                            val pitchRad = Math.toRadians(animatedPitch.toDouble())
                            val cosP = cos(pitchRad).toFloat().coerceAtLeast(0.20f)
                            val dx = tapOffset.x - center.x
                            val dy = (tapOffset.y - center.y) / cosP

                            val radYaw = Math.toRadians(-animatedYaw.toDouble())
                            val cosY = cos(radYaw).toFloat()
                            val sinY = sin(radYaw).toFloat()

                            val u = (dx * cosY - dy * sinY) / boardRadius
                            val v = (dx * sinY + dy * cosY) / boardRadius

                            val col = ((u + 1f) * 4f).toInt()
                            val row = ((v + 1f) * 4f).toInt()

                            if (Position.isValid(row, col)) {
                                hitPos = Position(row, col)
                            }
                        }

                        if (hitPos != null) {
                            val clickedPos = hitPos
                            val clickedPiece = state.pieceAt(clickedPos)

                            val selected = selectedPosition
                            if (selected != null) {
                                val matchedMove = legalMovesForSelected.firstOrNull { it.to == clickedPos }
                                if (matchedMove != null) {
                                    // Trigger realistic shattering particle explosion
                                    if (matchedMove.capturedPiece != null) {
                                        val u = ((clickedPos.col + 0.5f) / 4f) - 1f
                                        val v = ((clickedPos.row + 0.5f) / 4f) - 1f
                                        val groundPt = MosaicBoardRenderer.projectPoint(u, v, animatedYaw, animatedPitch, center, boardRadius)
                                        activeDestructions.add(
                                            DestructionEffect(
                                                squarePosition = clickedPos,
                                                centerOffset = Offset(groundPt.x, groundPt.y - pieceHeight * 0.4f),
                                                pieceColor = matchedMove.capturedPiece.color
                                            )
                                        )
                                    }

                                    onMoveRequested(selected, clickedPos, matchedMove.promotionPiece)
                                    selectedPosition = null
                                    legalMovesForSelected = emptyList()
                                    return@detectTapGestures
                                }
                            }

                            // Select own piece if valid
                            if (clickedPiece != null && clickedPiece.color == state.currentTurn) {
                                selectedPosition = clickedPos
                                legalMovesForSelected = ChessEngine.getLegalMoves(state, clickedPos)
                            } else {
                                selectedPosition = null
                                legalMovesForSelected = emptyList()
                            }
                        }
                    }
                }
        ) {
            val boardSize = size.width
            val center = Offset(boardSize / 2f, boardSize / 2f)
            val boardRadius = boardSize * 0.42f
            val pieceHeight = (boardRadius / 4f) * 1.65f

            val lastMove = state.moveHistory.lastOrNull()
            val kingInCheckPos = if (state.status == com.example.model.GameStatus.CHECK || state.status == com.example.model.GameStatus.CHECKMATE) {
                ChessEngine.findKing(state.currentTurn, state.board)
            } else null

            // 1. Draw 3D Mosaic Chessboard with slab depth and Roman tesserae
            MosaicBoardRenderer.drawProjectedMosaicBoard(
                drawScope = this,
                center = center,
                boardRadius = boardRadius,
                yaw = animatedYaw,
                pitch = animatedPitch,
                selectedPos = selectedPosition,
                legalDestinations = legalMovesForSelected,
                lastMove = lastMove,
                checkPos = kingInCheckPos,
                hintMove = hintMove,
                threatSquares = threatSquares,
                safeSquares = safeSquares
            )

            // 2. Draw Standing Upright Sculptures
            // Calculate depth for each piece and sort back-to-front
            val yawRad = Math.toRadians(animatedYaw.toDouble())
            val cosY = cos(yawRad).toFloat()
            val sinY = sin(yawRad).toFloat()

            data class UprightPieceEntry(
                val piece: com.example.model.ChessPiece,
                val pos: Position,
                val groundPoint: Offset,
                val depth: Float
            )

            val piecesToRender = mutableListOf<UprightPieceEntry>()

            for (r in 0..7) {
                for (c in 0..7) {
                    val piece = state.board[r][c] ?: continue
                    val pos = Position(r, c)
                    val u = ((c + 0.5f) / 4f) - 1f
                    val v = ((r + 0.5f) / 4f) - 1f

                    val groundPt = MosaicBoardRenderer.projectPoint(u, v, animatedYaw, animatedPitch, center, boardRadius)
                    // Depth along look direction (farthest has lower depth)
                    val depth = u * sinY + v * cosY
                    piecesToRender.add(UprightPieceEntry(piece, pos, groundPt, depth))
                }
            }

            // Sort ascending by depth (farthest drawn first, nearest drawn in front)
            piecesToRender.sortBy { it.depth }

            for (entry in piecesToRender) {
                val isSelected = selectedPosition == entry.pos
                SculpturesRenderer.drawUprightPiece(
                    drawScope = this,
                    piece = entry.piece,
                    groundCenter = entry.groundPoint,
                    baseSquareHeight = pieceHeight,
                    isSelected = isSelected
                )
            }

            // 3. Draw Active Shattering Particles
            for (effect in activeDestructions) {
                effect.draw(this)
            }
        }
    }
}

private fun triggerHapticFeedback(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(70)
            }
        }
    } catch (_: Exception) {
        // Safe fallback
    }
}
