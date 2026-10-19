package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.ChessAI
import com.example.engine.ChessEngine
import com.example.engine.TacticalPuzzle
import com.example.engine.ThreatMap
import com.example.engine.TutorEngine
import com.example.engine.TutorHint
import com.example.model.AIDifficulty
import com.example.model.ChessMove
import com.example.model.GameState
import com.example.model.GameStatus
import com.example.model.PieceColor
import com.example.model.PieceType
import com.example.model.Position
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChessUiState(
    val gameState: GameState = GameState.initial(),
    val is3DMode: Boolean = true,
    val aiDifficulty: AIDifficulty = AIDifficulty.CENTURION,
    val isAiThinking: Boolean = false,
    val playerColor: PieceColor = PieceColor.WHITE,
    val isLearningMode: Boolean = true,
    val showThreatMap: Boolean = false,
    val activeHint: TutorHint? = null,
    val threatMap: ThreatMap? = null,
    val pendingPromotion: Pair<Position, Position>? = null,
    val showReferenceSheet: Boolean = false,
    val showPuzzlesDialog: Boolean = false,
    val activePuzzle: TacticalPuzzle? = null,
    val evaluationScore: Int = 0,
    val cameraState: com.example.model.Camera3DState = com.example.model.Camera3DState()
)

class ChessViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChessUiState())
    val uiState: StateFlow<ChessUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<GameState>()

    init {
        updateEvaluationAndThreats(_uiState.value.gameState)
    }

    fun toggle3DMode() {
        _uiState.update { current ->
            val new3D = !current.is3DMode
            current.copy(
                is3DMode = new3D,
                cameraState = if (!new3D) {
                    current.cameraState.copy(pitch = 0f, yaw = 0f, activePreset = com.example.model.Camera3DPreset.ZENITH)
                } else {
                    current.cameraState.copy(pitch = 38f, yaw = 0f, activePreset = com.example.model.Camera3DPreset.WHITE_CAMP)
                }
            )
        }
    }

    fun applyCameraPreset(preset: com.example.model.Camera3DPreset) {
        _uiState.update { current ->
            current.copy(
                is3DMode = preset != com.example.model.Camera3DPreset.ZENITH,
                cameraState = current.cameraState.copy(
                    pitch = preset.pitch,
                    yaw = preset.yaw,
                    activePreset = preset
                )
            )
        }
    }

    fun rotateYawBy(deltaDegrees: Float) {
        _uiState.update { current ->
            var newYaw = (current.cameraState.yaw + deltaDegrees) % 360f
            if (newYaw < 0f) newYaw += 360f
            current.copy(
                is3DMode = true,
                cameraState = current.cameraState.copy(
                    yaw = newYaw,
                    activePreset = null // custom manual angle
                )
            )
        }
    }

    fun updateCameraManual(pitch: Float, yaw: Float, zoom: Float) {
        _uiState.update { current ->
            val clampedPitch = pitch.coerceIn(0f, 65f)
            var normalizedYaw = yaw % 360f
            if (normalizedYaw < 0f) normalizedYaw += 360f
            val clampedZoom = zoom.coerceIn(0.75f, 1.45f)
            current.copy(
                is3DMode = clampedPitch > 5f,
                cameraState = current.cameraState.copy(
                    pitch = clampedPitch,
                    yaw = normalizedYaw,
                    zoom = clampedZoom,
                    activePreset = null
                )
            )
        }
    }

    fun toggleOrbitMode(active: Boolean? = null) {
        _uiState.update { current ->
            val nextOrbit = active ?: !current.cameraState.isOrbitModeActive
            current.copy(
                cameraState = current.cameraState.copy(isOrbitModeActive = nextOrbit)
            )
        }
    }

    fun resetCamera() {
        applyCameraPreset(com.example.model.Camera3DPreset.WHITE_CAMP)
    }

    fun setDifficulty(difficulty: AIDifficulty) {
        _uiState.update { it.copy(aiDifficulty = difficulty) }
    }

    fun toggleLearningMode(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isLearningMode = enabled,
                activeHint = if (!enabled) null else it.activeHint,
                showThreatMap = if (!enabled) false else it.showThreatMap
            )
        }
    }

    fun toggleThreatMap(show: Boolean) {
        _uiState.update { it.copy(showThreatMap = show) }
    }

    fun toggleReferenceSheet(show: Boolean) {
        _uiState.update { it.copy(showReferenceSheet = show) }
    }

    fun togglePuzzlesDialog(show: Boolean) {
        _uiState.update { it.copy(showPuzzlesDialog = show) }
    }

    fun requestHint() {
        val current = _uiState.value.gameState
        if (current.currentTurn != _uiState.value.playerColor) return
        viewModelScope.launch(Dispatchers.Default) {
            val hint = TutorEngine.generateHint(current)
            _uiState.update { it.copy(activeHint = hint) }
        }
    }

    fun onMoveRequested(from: Position, to: Position, preSelectedPromo: PieceType? = null) {
        val current = _uiState.value.gameState
        val piece = current.pieceAt(from) ?: return
        if (piece.color != current.currentTurn) return

        // Check if pawn reaches back rank and promotion piece is needed
        val isPromo = piece.type == PieceType.PAWN &&
                ((piece.color == PieceColor.WHITE && to.row == 0) ||
                 (piece.color == PieceColor.BLACK && to.row == 7))

        if (isPromo && preSelectedPromo == null) {
            _uiState.update { it.copy(pendingPromotion = Pair(from, to)) }
            return
        }

        executeMove(from, to, preSelectedPromo)
    }

    fun onPromotionSelected(promoPiece: PieceType) {
        val pending = _uiState.value.pendingPromotion ?: return
        _uiState.update { it.copy(pendingPromotion = null) }
        executeMove(pending.first, pending.second, promoPiece)
    }

    private fun executeMove(from: Position, to: Position, promotion: PieceType?) {
        val current = _uiState.value.gameState
        val legalMoves = ChessEngine.getLegalMoves(current, from)
        val move = legalMoves.firstOrNull { it.to == to && (promotion == null || it.promotionPiece == promotion) }
            ?: return

        undoStack.add(current)
        val nextState = ChessEngine.applyMove(current, move)

        // Evaluate move quality in learning mode
        if (_uiState.value.isLearningMode) {
            val quality = TutorEngine.analyzeMoveQuality(current, move, nextState)
            move.quality = quality
        }

        _uiState.update {
            it.copy(
                gameState = nextState,
                activeHint = null // Clear hint after move
            )
        }

        updateEvaluationAndThreats(nextState)

        // If active puzzle in progress, check if user solved it
        val activePuzzle = _uiState.value.activePuzzle
        if (activePuzzle != null) {
            if (move.from == activePuzzle.solutionMove.from && move.to == activePuzzle.solutionMove.to) {
                // Solved!
            }
        }

        // Trigger AI move if it's AI's turn
        if (nextState.status == GameStatus.IN_PROGRESS || nextState.status == GameStatus.CHECK) {
            if (nextState.currentTurn != _uiState.value.playerColor) {
                scheduleAiMove(nextState)
            }
        }
    }

    private fun scheduleAiMove(state: GameState) {
        _uiState.update { it.copy(isAiThinking = true) }
        viewModelScope.launch(Dispatchers.Default) {
            // Thinking delay based on difficulty
            val delayMs = when (_uiState.value.aiDifficulty) {
                AIDifficulty.NOVICE -> 450L
                AIDifficulty.CENTURION -> 750L
                AIDifficulty.IMPERATOR -> 1100L
            }
            delay(delayMs)

            val aiMove = ChessAI.chooseMove(state, _uiState.value.aiDifficulty)
            if (aiMove != null) {
                val postAiState = ChessEngine.applyMove(state, aiMove)
                _uiState.update {
                    it.copy(
                        gameState = postAiState,
                        isAiThinking = false
                    )
                }
                updateEvaluationAndThreats(postAiState)
            } else {
                _uiState.update { it.copy(isAiThinking = false) }
            }
        }
    }

    private fun updateEvaluationAndThreats(state: GameState) {
        viewModelScope.launch(Dispatchers.Default) {
            val eval = ChessAI.evaluateBoard(state)
            val threatMap = TutorEngine.computeThreatMap(state)
            _uiState.update {
                it.copy(
                    evaluationScore = eval,
                    threatMap = threatMap
                )
            }
        }
    }

    fun undoMove() {
        if (undoStack.isEmpty() || _uiState.value.isAiThinking) return
        // In AI match, undo both AI's move and player's move so it's player's turn again
        if (undoStack.size >= 2 && _uiState.value.gameState.currentTurn == _uiState.value.playerColor) {
            undoStack.removeLast() // pop player move
            val previousState = undoStack.removeLast() // pop AI move
            _uiState.update { it.copy(gameState = previousState, activeHint = null) }
            updateEvaluationAndThreats(previousState)
        } else if (undoStack.isNotEmpty()) {
            val previousState = undoStack.removeLast()
            _uiState.update { it.copy(gameState = previousState, activeHint = null) }
            updateEvaluationAndThreats(previousState)
        }
    }

    fun startNewGame() {
        undoStack.clear()
        val fresh = GameState.initial()
        _uiState.update {
            it.copy(
                gameState = fresh,
                activeHint = null,
                activePuzzle = null,
                isAiThinking = false
            )
        }
        updateEvaluationAndThreats(fresh)
    }

    fun loadPuzzle(puzzle: TacticalPuzzle) {
        undoStack.clear()
        _uiState.update {
            it.copy(
                gameState = puzzle.setupState,
                activePuzzle = puzzle,
                activeHint = null,
                isAiThinking = false
            )
        }
        updateEvaluationAndThreats(puzzle.setupState)
    }
}
