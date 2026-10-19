package com.example

import com.example.engine.ChessAI
import com.example.engine.ChessEngine
import com.example.engine.TutorEngine
import com.example.model.AIDifficulty
import com.example.model.ChessMove
import com.example.model.ChessPiece
import com.example.model.GameState
import com.example.model.PieceColor
import com.example.model.PieceType
import com.example.model.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun initialBoard_hasCorrectSetup() {
        val state = GameState.initial()
        assertEquals(PieceColor.WHITE, state.currentTurn)

        // 16 white pieces, 16 black pieces
        var whiteCount = 0
        var blackCount = 0
        for (r in 0..7) {
            for (c in 0..7) {
                val p = state.board[r][c] ?: continue
                if (p.color == PieceColor.WHITE) whiteCount++ else blackCount++
            }
        }
        assertEquals(16, whiteCount)
        assertEquals(16, blackCount)

        // White king at e1 (row 7, col 4)
        val whiteKing = state.pieceAt(Position(7, 4))
        assertNotNull(whiteKing)
        assertEquals(PieceType.KING, whiteKing?.type)
        assertEquals(PieceColor.WHITE, whiteKing?.color)
    }

    @Test
    fun pawnMoves_initialRank_allowsOneAndTwoSteps() {
        val state = GameState.initial()
        val e2Moves = ChessEngine.getLegalMoves(state, Position(6, 4)) // e2
        assertEquals(2, e2Moves.size)

        val targetRows = e2Moves.map { it.to.row }
        assertTrue(targetRows.contains(5)) // e3
        assertTrue(targetRows.contains(4)) // e4
    }

    @Test
    fun ai_canChooseValidMovesForAllDifficulties() {
        val state = GameState.initial()

        val noviceMove = ChessAI.chooseMove(state, AIDifficulty.NOVICE)
        assertNotNull(noviceMove)

        val centurionMove = ChessAI.chooseMove(state, AIDifficulty.CENTURION)
        assertNotNull(centurionMove)

        val imperatorMove = ChessAI.chooseMove(state, AIDifficulty.IMPERATOR)
        assertNotNull(imperatorMove)
    }

    @Test
    fun castling_detectedAndAppliedProperly() {
        // Clear between king and rook
        val board = Array(8) { Array<ChessPiece?>(8) { null } }
        board[7][4] = ChessPiece(PieceType.KING, PieceColor.WHITE)
        board[7][7] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)
        board[0][4] = ChessPiece(PieceType.KING, PieceColor.BLACK)

        val state = GameState(board = board, currentTurn = PieceColor.WHITE)
        val kingMoves = ChessEngine.getLegalMoves(state, Position(7, 4))

        val castlingMove = kingMoves.firstOrNull { it.isCastling && it.to == Position(7, 6) }
        assertNotNull("Kingside castling should be available", castlingMove)

        val postCastling = ChessEngine.applyMove(state, castlingMove!!)
        assertEquals(PieceType.KING, postCastling.pieceAt(Position(7, 6))?.type)
        assertEquals(PieceType.ROOK, postCastling.pieceAt(Position(7, 5))?.type)
    }

    @Test
    fun tutorEngine_evaluatesThreatsAndHints() {
        val state = GameState.initial()
        val hint = TutorEngine.generateHint(state)
        assertNotNull(hint)
        assertTrue(hint!!.explanation.isNotEmpty())

        val threats = TutorEngine.computeThreatMap(state)
        assertTrue(threats.safeSquaresForTurn.isNotEmpty())
    }

    @Test
    fun cameraPresets_haveValidAnglesAndIncludeDiagonal45() {
        val presets = com.example.model.Camera3DPreset.entries
        assertEquals(4, presets.size)

        val diagonal45 = com.example.model.Camera3DPreset.DIAGONAL_45
        assertEquals(45f, diagonal45.pitch)
        assertEquals(45f, diagonal45.yaw)

        val whiteCamp = com.example.model.Camera3DPreset.WHITE_CAMP
        assertEquals(0f, whiteCamp.yaw)

        val blackCamp = com.example.model.Camera3DPreset.BLACK_CAMP
        assertEquals(180f, blackCamp.yaw)

        val zenith = com.example.model.Camera3DPreset.ZENITH
        assertEquals(0f, zenith.pitch)
    }
}
