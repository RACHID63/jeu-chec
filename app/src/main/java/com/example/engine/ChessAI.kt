package com.example.engine

import com.example.model.AIDifficulty
import com.example.model.ChessMove
import com.example.model.GameState
import com.example.model.PieceColor
import com.example.model.PieceType
import com.example.model.Position
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object ChessAI {

    fun chooseMove(state: GameState, difficulty: AIDifficulty): ChessMove? {
        val legalMoves = ChessEngine.getAllLegalMoves(state, state.currentTurn)
        if (legalMoves.isEmpty()) return null

        return when (difficulty) {
            AIDifficulty.NOVICE -> chooseNoviceMove(state, legalMoves)
            AIDifficulty.CENTURION -> chooseMinimaxMove(state, legalMoves, depth = 3)
            AIDifficulty.IMPERATOR -> chooseImperatorMove(state, legalMoves, depth = 4)
        }
    }

    private fun chooseNoviceMove(state: GameState, legalMoves: List<ChessMove>): ChessMove {
        // Novice: 75% pick a capture or center move if available, otherwise random
        val captures = legalMoves.filter { it.capturedPiece != null }
        if (captures.isNotEmpty() && Random.nextFloat() < 0.70f) {
            return captures.maxByOrNull { it.capturedPiece?.type?.baseValue ?: 0 } ?: captures.random()
        }

        // Prefer moves that move toward center (rows 3,4 and cols 2..5)
        val centerMoves = legalMoves.filter { it.to.row in 2..5 && it.to.col in 2..5 }
        if (centerMoves.isNotEmpty() && Random.nextFloat() < 0.50f) {
            return centerMoves.random()
        }

        return legalMoves.random()
    }

    private fun chooseMinimaxMove(state: GameState, legalMoves: List<ChessMove>, depth: Int): ChessMove {
        val isMaximizing = state.currentTurn == PieceColor.WHITE
        var bestScore = if (isMaximizing) -1000000 else 1000000
        var bestMoves = mutableListOf<ChessMove>()

        val sortedMoves = orderMoves(legalMoves)

        for (move in sortedMoves) {
            val nextState = ChessEngine.applyMove(state, move)
            val score = alphaBeta(nextState, depth - 1, -1000000, 1000000, !isMaximizing)

            if (isMaximizing) {
                if (score > bestScore) {
                    bestScore = score
                    bestMoves = mutableListOf(move)
                } else if (score == bestScore) {
                    bestMoves.add(move)
                }
            } else {
                if (score < bestScore) {
                    bestScore = score
                    bestMoves = mutableListOf(move)
                } else if (score == bestScore) {
                    bestMoves.add(move)
                }
            }
        }

        return bestMoves.randomOrNull() ?: legalMoves.first()
    }

    private fun chooseImperatorMove(state: GameState, legalMoves: List<ChessMove>, depth: Int): ChessMove {
        val isMaximizing = state.currentTurn == PieceColor.WHITE
        var bestScore = if (isMaximizing) -1000000 else 1000000
        var bestMoves = mutableListOf<ChessMove>()

        val sortedMoves = orderMoves(legalMoves)

        for (move in sortedMoves) {
            val nextState = ChessEngine.applyMove(state, move)
            val score = alphaBetaWithQuiescence(nextState, depth - 1, -1000000, 1000000, !isMaximizing)

            if (isMaximizing) {
                if (score > bestScore) {
                    bestScore = score
                    bestMoves = mutableListOf(move)
                } else if (score == bestScore) {
                    bestMoves.add(move)
                }
            } else {
                if (score < bestScore) {
                    bestScore = score
                    bestMoves = mutableListOf(move)
                } else if (score == bestScore) {
                    bestMoves.add(move)
                }
            }
        }

        return bestMoves.randomOrNull() ?: legalMoves.first()
    }

    private fun alphaBeta(
        state: GameState,
        depth: Int,
        alphaParam: Int,
        betaParam: Int,
        isMaximizing: Boolean
    ): Int {
        if (depth == 0 || state.status != com.example.model.GameStatus.IN_PROGRESS && state.status != com.example.model.GameStatus.CHECK) {
            return evaluateBoard(state)
        }

        var alpha = alphaParam
        var beta = betaParam
        val moves = ChessEngine.getAllLegalMoves(state, state.currentTurn)
        if (moves.isEmpty()) {
            return if (ChessEngine.isKingInCheck(state.currentTurn, state.board)) {
                if (state.currentTurn == PieceColor.WHITE) -90000 - depth else 90000 + depth
            } else {
                0 // Stalemate
            }
        }

        val sortedMoves = orderMoves(moves)

        if (isMaximizing) {
            var maxEval = -1000000
            for (move in sortedMoves) {
                val nextState = ChessEngine.applyMove(state, move)
                val eval = alphaBeta(nextState, depth - 1, alpha, beta, false)
                maxEval = max(maxEval, eval)
                alpha = max(alpha, eval)
                if (beta <= alpha) break
            }
            return maxEval
        } else {
            var minEval = 1000000
            for (move in sortedMoves) {
                val nextState = ChessEngine.applyMove(state, move)
                val eval = alphaBeta(nextState, depth - 1, alpha, beta, true)
                minEval = min(minEval, eval)
                beta = min(beta, eval)
                if (beta <= alpha) break
            }
            return minEval
        }
    }

    private fun alphaBetaWithQuiescence(
        state: GameState,
        depth: Int,
        alphaParam: Int,
        betaParam: Int,
        isMaximizing: Boolean
    ): Int {
        if (depth <= 0) {
            return quiescence(state, alphaParam, betaParam, isMaximizing, maxDepth = 2)
        }

        if (state.status != com.example.model.GameStatus.IN_PROGRESS && state.status != com.example.model.GameStatus.CHECK) {
            return evaluateBoard(state)
        }

        var alpha = alphaParam
        var beta = betaParam
        val moves = ChessEngine.getAllLegalMoves(state, state.currentTurn)
        if (moves.isEmpty()) {
            return if (ChessEngine.isKingInCheck(state.currentTurn, state.board)) {
                if (state.currentTurn == PieceColor.WHITE) -90000 - depth else 90000 + depth
            } else {
                0
            }
        }

        val sortedMoves = orderMoves(moves)

        if (isMaximizing) {
            var maxEval = -1000000
            for (move in sortedMoves) {
                val nextState = ChessEngine.applyMove(state, move)
                val eval = alphaBetaWithQuiescence(nextState, depth - 1, alpha, beta, false)
                maxEval = max(maxEval, eval)
                alpha = max(alpha, eval)
                if (beta <= alpha) break
            }
            return maxEval
        } else {
            var minEval = 1000000
            for (move in sortedMoves) {
                val nextState = ChessEngine.applyMove(state, move)
                val eval = alphaBetaWithQuiescence(nextState, depth - 1, alpha, beta, true)
                minEval = min(minEval, eval)
                beta = min(beta, eval)
                if (beta <= alpha) break
            }
            return minEval
        }
    }

    private fun quiescence(
        state: GameState,
        alphaParam: Int,
        betaParam: Int,
        isMaximizing: Boolean,
        maxDepth: Int
    ): Int {
        val standPat = evaluateBoard(state)
        if (maxDepth <= 0) return standPat

        var alpha = alphaParam
        var beta = betaParam

        if (isMaximizing) {
            if (standPat >= beta) return beta
            if (alpha < standPat) alpha = standPat

            val captures = ChessEngine.getAllLegalMoves(state, state.currentTurn).filter { it.capturedPiece != null }
            val sortedCaptures = orderMoves(captures)

            for (move in sortedCaptures) {
                val nextState = ChessEngine.applyMove(state, move)
                val score = quiescence(nextState, alpha, beta, false, maxDepth - 1)
                if (score >= beta) return beta
                if (score > alpha) alpha = score
            }
            return alpha
        } else {
            if (standPat <= alpha) return alpha
            if (beta > standPat) beta = standPat

            val captures = ChessEngine.getAllLegalMoves(state, state.currentTurn).filter { it.capturedPiece != null }
            val sortedCaptures = orderMoves(captures)

            for (move in sortedCaptures) {
                val nextState = ChessEngine.applyMove(state, move)
                val score = quiescence(nextState, alpha, beta, true, maxDepth - 1)
                if (score <= alpha) return alpha
                if (score < beta) beta = score
            }
            return beta
        }
    }

    private fun orderMoves(moves: List<ChessMove>): List<ChessMove> {
        return moves.sortedByDescending { move ->
            var score = 0
            // MVV-LVA (Most Valuable Victim - Least Valuable Attacker)
            if (move.capturedPiece != null) {
                score += 10 * move.capturedPiece.type.baseValue - move.piece.type.baseValue
            }
            if (move.promotionPiece != null) {
                score += move.promotionPiece.baseValue
            }
            if (move.isCastling) {
                score += 50
            }
            score
        }
    }

    fun evaluateBoard(state: GameState): Int {
        if (state.status == com.example.model.GameStatus.CHECKMATE) {
            return if (state.currentTurn == PieceColor.WHITE) -90000 else 90000
        }
        if (state.status == com.example.model.GameStatus.STALEMATE ||
            state.status == com.example.model.GameStatus.DRAW_INSUFFICIENT_MATERIAL ||
            state.status == com.example.model.GameStatus.DRAW_FIFTY_MOVES
        ) {
            return 0
        }

        var totalScore = 0

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = state.board[r][c] ?: continue
                val base = piece.type.baseValue
                val posScore = getPieceSquareValue(piece.type, piece.color, r, c)
                val pieceScore = base + posScore

                if (piece.color == PieceColor.WHITE) {
                    totalScore += pieceScore
                } else {
                    totalScore -= pieceScore
                }
            }
        }

        return totalScore
    }

    // Piece-Square Tables (oriented from White's perspective; mirrored for Black)
    private fun getPieceSquareValue(type: PieceType, color: PieceColor, r: Int, c: Int): Int {
        val row = if (color == PieceColor.WHITE) r else 7 - r
        val col = c

        return when (type) {
            PieceType.PAWN -> PAWN_TABLE[row][col]
            PieceType.KNIGHT -> KNIGHT_TABLE[row][col]
            PieceType.BISHOP -> BISHOP_TABLE[row][col]
            PieceType.ROOK -> ROOK_TABLE[row][col]
            PieceType.QUEEN -> QUEEN_TABLE[row][col]
            PieceType.KING -> KING_MIDDLE_TABLE[row][col]
        }
    }

    private val PAWN_TABLE = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(50, 50, 50, 50, 50, 50, 50, 50),
        intArrayOf(10, 10, 20, 30, 30, 20, 10, 10),
        intArrayOf(5, 5, 10, 25, 25, 10, 5, 5),
        intArrayOf(0, 0, 0, 20, 20, 0, 0, 0),
        intArrayOf(5, -5, -10, 0, 0, -10, -5, 5),
        intArrayOf(5, 10, 10, -20, -20, 10, 10, 5),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    )

    private val KNIGHT_TABLE = arrayOf(
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50),
        intArrayOf(-40, -20, 0, 0, 0, 0, -20, -40),
        intArrayOf(-30, 0, 10, 15, 15, 10, 0, -30),
        intArrayOf(-30, 5, 15, 20, 20, 15, 5, -30),
        intArrayOf(-30, 0, 15, 20, 20, 15, 0, -30),
        intArrayOf(-30, 5, 10, 15, 15, 10, 5, -30),
        intArrayOf(-40, -20, 0, 5, 5, 0, -20, -40),
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50)
    )

    private val BISHOP_TABLE = arrayOf(
        intArrayOf(-20, -10, -10, -10, -10, -10, -10, -20),
        intArrayOf(-10, 0, 0, 0, 0, 0, 0, -10),
        intArrayOf(-10, 0, 5, 10, 10, 5, 0, -10),
        intArrayOf(-10, 5, 5, 10, 10, 5, 5, -10),
        intArrayOf(-10, 0, 10, 10, 10, 10, 0, -10),
        intArrayOf(-10, 10, 10, 10, 10, 10, 10, -10),
        intArrayOf(-10, 5, 0, 0, 0, 0, 5, -10),
        intArrayOf(-20, -10, -10, -10, -10, -10, -10, -20)
    )

    private val ROOK_TABLE = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(5, 10, 10, 10, 10, 10, 10, 5),
        intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
        intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
        intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
        intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
        intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
        intArrayOf(0, 0, 0, 5, 5, 0, 0, 0)
    )

    private val QUEEN_TABLE = arrayOf(
        intArrayOf(-20, -10, -10, -5, -5, -10, -10, -20),
        intArrayOf(-10, 0, 0, 0, 0, 0, 0, -10),
        intArrayOf(-10, 0, 5, 5, 5, 5, 0, -10),
        intArrayOf(-5, 0, 5, 5, 5, 5, 0, -5),
        intArrayOf(0, 0, 5, 5, 5, 5, 0, -5),
        intArrayOf(-10, 5, 5, 5, 5, 5, 0, -10),
        intArrayOf(-10, 0, 5, 0, 0, 0, 0, -10),
        intArrayOf(-20, -10, -10, -5, -5, -10, -10, -20)
    )

    private val KING_MIDDLE_TABLE = arrayOf(
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-20, -30, -30, -40, -40, -30, -30, -20),
        intArrayOf(-10, -20, -20, -20, -20, -20, -20, -10),
        intArrayOf(20, 20, 0, 0, 0, 0, 20, 20),
        intArrayOf(20, 30, 10, 0, 0, 10, 30, 20)
    )
}
