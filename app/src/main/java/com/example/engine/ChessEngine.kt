package com.example.engine

import com.example.model.CastlingRights
import com.example.model.ChessMove
import com.example.model.ChessPiece
import com.example.model.GameState
import com.example.model.GameStatus
import com.example.model.PieceColor
import com.example.model.PieceType
import com.example.model.Position

object ChessEngine {

    fun getLegalMoves(state: GameState, from: Position): List<ChessMove> {
        val piece = state.pieceAt(from) ?: return emptyList()
        if (piece.color != state.currentTurn) return emptyList()
        val allLegal = getAllLegalMoves(state, piece.color)
        return allLegal.filter { it.from == from }
    }

    fun getAllLegalMoves(state: GameState, color: PieceColor = state.currentTurn): List<ChessMove> {
        val pseudoMoves = mutableListOf<ChessMove>()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = state.board[r][c] ?: continue
                if (piece.color == color) {
                    val pos = Position(r, c)
                    getPseudoMoves(state, pos, piece, pseudoMoves)
                }
            }
        }

        // Filter out moves that leave king in check
        val legalMoves = mutableListOf<ChessMove>()
        for (move in pseudoMoves) {
            if (isMoveLegal(state, move)) {
                // If pawn reaches back rank without promotion specified, expand to 4 promotion moves
                if (move.piece.type == PieceType.PAWN &&
                    ((move.piece.color == PieceColor.WHITE && move.to.row == 0) ||
                     (move.piece.color == PieceColor.BLACK && move.to.row == 7)) &&
                    move.promotionPiece == null
                ) {
                    legalMoves.add(move.copy(promotionPiece = PieceType.QUEEN))
                    legalMoves.add(move.copy(promotionPiece = PieceType.ROOK))
                    legalMoves.add(move.copy(promotionPiece = PieceType.BISHOP))
                    legalMoves.add(move.copy(promotionPiece = PieceType.KNIGHT))
                } else {
                    legalMoves.add(move)
                }
            }
        }
        return legalMoves
    }

    private fun getPseudoMoves(
        state: GameState,
        from: Position,
        piece: ChessPiece,
        outMoves: MutableList<ChessMove>
    ) {
        when (piece.type) {
            PieceType.PAWN -> getPawnMoves(state, from, piece, outMoves)
            PieceType.KNIGHT -> getKnightMoves(state, from, piece, outMoves)
            PieceType.BISHOP -> getSlidingMoves(state, from, piece, BISHOP_DIRS, outMoves)
            PieceType.ROOK -> getSlidingMoves(state, from, piece, ROOK_DIRS, outMoves)
            PieceType.QUEEN -> getSlidingMoves(state, from, piece, QUEEN_DIRS, outMoves)
            PieceType.KING -> getKingMoves(state, from, piece, outMoves)
        }
    }

    private val KNIGHT_OFFSETS = arrayOf(
        intArrayOf(-2, -1), intArrayOf(-2, 1),
        intArrayOf(-1, -2), intArrayOf(-1, 2),
        intArrayOf(1, -2), intArrayOf(1, 2),
        intArrayOf(2, -1), intArrayOf(2, 1)
    )

    private val BISHOP_DIRS = arrayOf(
        intArrayOf(-1, -1), intArrayOf(-1, 1),
        intArrayOf(1, -1), intArrayOf(1, 1)
    )

    private val ROOK_DIRS = arrayOf(
        intArrayOf(-1, 0), intArrayOf(1, 0),
        intArrayOf(0, -1), intArrayOf(0, 1)
    )

    private val QUEEN_DIRS = arrayOf(
        intArrayOf(-1, -1), intArrayOf(-1, 1),
        intArrayOf(1, -1), intArrayOf(1, 1),
        intArrayOf(-1, 0), intArrayOf(1, 0),
        intArrayOf(0, -1), intArrayOf(0, 1)
    )

    private fun getPawnMoves(
        state: GameState,
        from: Position,
        piece: ChessPiece,
        outMoves: MutableList<ChessMove>
    ) {
        val forward = if (piece.color == PieceColor.WHITE) -1 else 1
        val startRow = if (piece.color == PieceColor.WHITE) 6 else 1

        // 1 step forward
        val r1 = from.row + forward
        if (r1 in 0..7 && state.board[r1][from.col] == null) {
            val to1 = Position(r1, from.col)
            outMoves.add(ChessMove(from, to1, piece))

            // 2 steps forward from starting rank
            val r2 = from.row + 2 * forward
            if (from.row == startRow && state.board[r2][from.col] == null) {
                outMoves.add(ChessMove(from, Position(r2, from.col), piece))
            }
        }

        // Diagonal captures & En Passant
        for (dc in intArrayOf(-1, 1)) {
            val c = from.col + dc
            if (r1 in 0..7 && c in 0..7) {
                val targetPiece = state.board[r1][c]
                val to = Position(r1, c)
                if (targetPiece != null && targetPiece.color != piece.color) {
                    outMoves.add(ChessMove(from, to, piece, capturedPiece = targetPiece))
                } else if (targetPiece == null && state.enPassantTarget == to) {
                    // En Passant capture
                    val victimRow = from.row
                    val victimPiece = state.board[victimRow][c]
                    if (victimPiece != null && victimPiece.color != piece.color && victimPiece.type == PieceType.PAWN) {
                        outMoves.add(ChessMove(from, to, piece, capturedPiece = victimPiece, isEnPassant = true))
                    }
                }
            }
        }
    }

    private fun getKnightMoves(
        state: GameState,
        from: Position,
        piece: ChessPiece,
        outMoves: MutableList<ChessMove>
    ) {
        for (offset in KNIGHT_OFFSETS) {
            val nr = from.row + offset[0]
            val nc = from.col + offset[1]
            if (Position.isValid(nr, nc)) {
                val target = state.board[nr][nc]
                if (target == null) {
                    outMoves.add(ChessMove(from, Position(nr, nc), piece))
                } else if (target.color != piece.color) {
                    outMoves.add(ChessMove(from, Position(nr, nc), piece, capturedPiece = target))
                }
            }
        }
    }

    private fun getSlidingMoves(
        state: GameState,
        from: Position,
        piece: ChessPiece,
        directions: Array<IntArray>,
        outMoves: MutableList<ChessMove>
    ) {
        for (dir in directions) {
            var r = from.row + dir[0]
            var c = from.col + dir[1]
            while (Position.isValid(r, c)) {
                val target = state.board[r][c]
                val to = Position(r, c)
                if (target == null) {
                    outMoves.add(ChessMove(from, to, piece))
                } else {
                    if (target.color != piece.color) {
                        outMoves.add(ChessMove(from, to, piece, capturedPiece = target))
                    }
                    break // Blocked by piece
                }
                r += dir[0]
                c += dir[1]
            }
        }
    }

    private fun getKingMoves(
        state: GameState,
        from: Position,
        piece: ChessPiece,
        outMoves: MutableList<ChessMove>
    ) {
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = from.row + dr
                val nc = from.col + dc
                if (Position.isValid(nr, nc)) {
                    val target = state.board[nr][nc]
                    if (target == null) {
                        outMoves.add(ChessMove(from, Position(nr, nc), piece))
                    } else if (target.color != piece.color) {
                        outMoves.add(ChessMove(from, Position(nr, nc), piece, capturedPiece = target))
                    }
                }
            }
        }

        // Castling
        val enemyColor = piece.color.opposite()
        if (piece.color == PieceColor.WHITE && from.row == 7 && from.col == 4) {
            // Kingside (O-O)
            if (state.castlingRights.whiteKingside &&
                state.board[7][5] == null && state.board[7][6] == null &&
                state.board[7][7]?.type == PieceType.ROOK && state.board[7][7]?.color == PieceColor.WHITE
            ) {
                if (!isSquareAttacked(Position(7, 4), enemyColor, state.board) &&
                    !isSquareAttacked(Position(7, 5), enemyColor, state.board) &&
                    !isSquareAttacked(Position(7, 6), enemyColor, state.board)
                ) {
                    outMoves.add(ChessMove(from, Position(7, 6), piece, isCastling = true))
                }
            }
            // Queenside (O-O-O)
            if (state.castlingRights.whiteQueenside &&
                state.board[7][3] == null && state.board[7][2] == null && state.board[7][1] == null &&
                state.board[7][0]?.type == PieceType.ROOK && state.board[7][0]?.color == PieceColor.WHITE
            ) {
                if (!isSquareAttacked(Position(7, 4), enemyColor, state.board) &&
                    !isSquareAttacked(Position(7, 3), enemyColor, state.board) &&
                    !isSquareAttacked(Position(7, 2), enemyColor, state.board)
                ) {
                    outMoves.add(ChessMove(from, Position(7, 2), piece, isCastling = true))
                }
            }
        } else if (piece.color == PieceColor.BLACK && from.row == 0 && from.col == 4) {
            // Kingside (O-O)
            if (state.castlingRights.blackKingside &&
                state.board[0][5] == null && state.board[0][6] == null &&
                state.board[0][7]?.type == PieceType.ROOK && state.board[0][7]?.color == PieceColor.BLACK
            ) {
                if (!isSquareAttacked(Position(0, 4), enemyColor, state.board) &&
                    !isSquareAttacked(Position(0, 5), enemyColor, state.board) &&
                    !isSquareAttacked(Position(0, 6), enemyColor, state.board)
                ) {
                    outMoves.add(ChessMove(from, Position(0, 6), piece, isCastling = true))
                }
            }
            // Queenside (O-O-O)
            if (state.castlingRights.blackQueenside &&
                state.board[0][3] == null && state.board[0][2] == null && state.board[0][1] == null &&
                state.board[0][0]?.type == PieceType.ROOK && state.board[0][0]?.color == PieceColor.BLACK
            ) {
                if (!isSquareAttacked(Position(0, 4), enemyColor, state.board) &&
                    !isSquareAttacked(Position(0, 3), enemyColor, state.board) &&
                    !isSquareAttacked(Position(0, 2), enemyColor, state.board)
                ) {
                    outMoves.add(ChessMove(from, Position(0, 2), piece, isCastling = true))
                }
            }
        }
    }

    fun isSquareAttacked(pos: Position, attackerColor: PieceColor, board: Array<Array<ChessPiece?>>): Boolean {
        // Attacked by Pawns
        val pawnPusher = if (attackerColor == PieceColor.WHITE) 1 else -1 // white attacks from +1 row, black from -1
        val attackerRow = pos.row + pawnPusher
        if (attackerRow in 0..7) {
            for (dc in intArrayOf(-1, 1)) {
                val ac = pos.col + dc
                if (ac in 0..7) {
                    val p = board[attackerRow][ac]
                    if (p != null && p.color == attackerColor && p.type == PieceType.PAWN) {
                        return true
                    }
                }
            }
        }

        // Attacked by Knights
        for (offset in KNIGHT_OFFSETS) {
            val kr = pos.row + offset[0]
            val kc = pos.col + offset[1]
            if (Position.isValid(kr, kc)) {
                val p = board[kr][kc]
                if (p != null && p.color == attackerColor && p.type == PieceType.KNIGHT) {
                    return true
                }
            }
        }

        // Attacked by Bishop / Queen (Diagonals)
        for (dir in BISHOP_DIRS) {
            var r = pos.row + dir[0]
            var c = pos.col + dir[1]
            while (Position.isValid(r, c)) {
                val p = board[r][c]
                if (p != null) {
                    if (p.color == attackerColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += dir[0]
                c += dir[1]
            }
        }

        // Attacked by Rook / Queen (Orthogonals)
        for (dir in ROOK_DIRS) {
            var r = pos.row + dir[0]
            var c = pos.col + dir[1]
            while (Position.isValid(r, c)) {
                val p = board[r][c]
                if (p != null) {
                    if (p.color == attackerColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += dir[0]
                c += dir[1]
            }
        }

        // Attacked by King
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val kr = pos.row + dr
                val kc = pos.col + dc
                if (Position.isValid(kr, kc)) {
                    val p = board[kr][kc]
                    if (p != null && p.color == attackerColor && p.type == PieceType.KING) {
                        return true
                    }
                }
            }
        }

        return false
    }

    fun findKing(color: PieceColor, board: Array<Array<ChessPiece?>>): Position? {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null && p.color == color && p.type == PieceType.KING) {
                    return Position(r, c)
                }
            }
        }
        return null
    }

    fun isKingInCheck(color: PieceColor, board: Array<Array<ChessPiece?>>): Boolean {
        val kingPos = findKing(color, board) ?: return false
        return isSquareAttacked(kingPos, color.opposite(), board)
    }

    private fun isMoveLegal(state: GameState, move: ChessMove): Boolean {
        val nextBoard = state.copyBoard()
        // Simulate move
        nextBoard[move.from.row][move.from.col] = null
        val placedPiece = if (move.promotionPiece != null) {
            ChessPiece(move.promotionPiece, move.piece.color)
        } else {
            move.piece
        }
        nextBoard[move.to.row][move.to.col] = placedPiece

        // Handle en passant capture removal
        if (move.isEnPassant) {
            nextBoard[move.from.row][move.to.col] = null
        }

        // Handle castling rook movement
        if (move.isCastling) {
            if (move.to.col == 6) { // Kingside
                nextBoard[move.to.row][7] = null
                nextBoard[move.to.row][5] = ChessPiece(PieceType.ROOK, move.piece.color)
            } else if (move.to.col == 2) { // Queenside
                nextBoard[move.to.row][0] = null
                nextBoard[move.to.row][3] = ChessPiece(PieceType.ROOK, move.piece.color)
            }
        }

        return !isKingInCheck(move.piece.color, nextBoard)
    }

    fun applyMove(state: GameState, move: ChessMove): GameState {
        val newBoard = state.copyBoard()
        val movingPiece = move.piece
        newBoard[move.from.row][move.from.col] = null

        val finalPiece = if (move.promotionPiece != null) {
            ChessPiece(move.promotionPiece, movingPiece.color)
        } else {
            movingPiece
        }
        newBoard[move.to.row][move.to.col] = finalPiece

        // Castling rook update
        if (move.isCastling) {
            if (move.to.col == 6) {
                newBoard[move.to.row][7] = null
                newBoard[move.to.row][5] = ChessPiece(PieceType.ROOK, movingPiece.color)
            } else if (move.to.col == 2) {
                newBoard[move.to.row][0] = null
                newBoard[move.to.row][3] = ChessPiece(PieceType.ROOK, movingPiece.color)
            }
        }

        // En passant capture
        var actualCaptured = move.capturedPiece
        if (move.isEnPassant) {
            actualCaptured = newBoard[move.from.row][move.to.col]
            newBoard[move.from.row][move.to.col] = null
        }

        // Update captured lists
        val newCapturedWhite = state.capturedWhite.toMutableList()
        val newCapturedBlack = state.capturedBlack.toMutableList()
        if (actualCaptured != null) {
            if (actualCaptured.color == PieceColor.WHITE) {
                newCapturedWhite.add(actualCaptured)
            } else {
                newCapturedBlack.add(actualCaptured)
            }
        }

        // Update castling rights
        var wKingside = state.castlingRights.whiteKingside
        var wQueenside = state.castlingRights.whiteQueenside
        var bKingside = state.castlingRights.blackKingside
        var bQueenside = state.castlingRights.blackQueenside

        if (movingPiece.type == PieceType.KING) {
            if (movingPiece.color == PieceColor.WHITE) {
                wKingside = false
                wQueenside = false
            } else {
                bKingside = false
                bQueenside = false
            }
        } else if (movingPiece.type == PieceType.ROOK) {
            if (move.from.row == 7 && move.from.col == 7) wKingside = false
            if (move.from.row == 7 && move.from.col == 0) wQueenside = false
            if (move.from.row == 0 && move.from.col == 7) bKingside = false
            if (move.from.row == 0 && move.from.col == 0) bQueenside = false
        }

        // If a rook is captured on its original square, forfeit castling on that side
        if (actualCaptured?.type == PieceType.ROOK) {
            if (move.to.row == 7 && move.to.col == 7) wKingside = false
            if (move.to.row == 7 && move.to.col == 0) wQueenside = false
            if (move.to.row == 0 && move.to.col == 7) bKingside = false
            if (move.to.row == 0 && move.to.col == 0) bQueenside = false
        }

        val newCastlingRights = CastlingRights(wKingside, wQueenside, bKingside, bQueenside)

        // En passant target calculation
        val newEnPassantTarget = if (
            movingPiece.type == PieceType.PAWN &&
            kotlin.math.abs(move.from.row - move.to.row) == 2
        ) {
            Position((move.from.row + move.to.row) / 2, move.from.col)
        } else {
            null
        }

        val isCaptureOrPawn = actualCaptured != null || movingPiece.type == PieceType.PAWN
        val newHalfMoveClock = if (isCaptureOrPawn) 0 else state.halfMoveClock + 1
        val newFullMoveNumber = if (state.currentTurn == PieceColor.BLACK) state.fullMoveNumber + 1 else state.fullMoveNumber

        val nextTurn = state.currentTurn.opposite()
        val opponentInCheck = isKingInCheck(nextTurn, newBoard)

        // Create temporary next state to generate opponent moves
        val tempState = GameState(
            board = newBoard,
            currentTurn = nextTurn,
            castlingRights = newCastlingRights,
            enPassantTarget = newEnPassantTarget,
            halfMoveClock = newHalfMoveClock,
            fullMoveNumber = newFullMoveNumber
        )

        val opponentMoves = getAllLegalMoves(tempState, nextTurn)

        val finalStatus = when {
            opponentInCheck && opponentMoves.isEmpty() -> GameStatus.CHECKMATE
            !opponentInCheck && opponentMoves.isEmpty() -> GameStatus.STALEMATE
            opponentInCheck -> GameStatus.CHECK
            newHalfMoveClock >= 100 -> GameStatus.DRAW_FIFTY_MOVES
            isInsufficientMaterial(newBoard) -> GameStatus.DRAW_INSUFFICIENT_MATERIAL
            else -> GameStatus.IN_PROGRESS
        }

        val winner = if (finalStatus == GameStatus.CHECKMATE) state.currentTurn else null

        // Formulate Short Algebraic Notation (SAN) and antique French commentary
        move.isCheck = opponentInCheck
        move.isCheckmate = finalStatus == GameStatus.CHECKMATE
        move.san = computeSAN(move)
        move.antiqueDescription = computeAntiqueDescription(move)

        val updatedHistory = state.moveHistory + move

        return GameState(
            board = newBoard,
            currentTurn = nextTurn,
            castlingRights = newCastlingRights,
            enPassantTarget = newEnPassantTarget,
            halfMoveClock = newHalfMoveClock,
            fullMoveNumber = newFullMoveNumber,
            moveHistory = updatedHistory,
            capturedWhite = newCapturedWhite,
            capturedBlack = newCapturedBlack,
            status = finalStatus,
            winner = winner
        )
    }

    private fun computeSAN(move: ChessMove): String {
        if (move.isCastling) {
            val castlingStr = if (move.to.col == 6) "O-O" else "O-O-O"
            return if (move.isCheckmate) "$castlingStr#" else if (move.isCheck) "$castlingStr+" else castlingStr
        }

        val sb = StringBuilder()
        if (move.piece.type != PieceType.PAWN) {
            sb.append(move.piece.type.symbol)
        } else if (move.capturedPiece != null) {
            sb.append(move.from.fileChar)
        }

        if (move.capturedPiece != null) {
            sb.append("x")
        }

        sb.append(move.to.toAlgebraic())

        if (move.promotionPiece != null) {
            sb.append("=").append(move.promotionPiece.symbol)
        }

        if (move.isCheckmate) {
            sb.append("#")
        } else if (move.isCheck) {
            sb.append("+")
        }

        return sb.toString()
    }

    private fun computeAntiqueDescription(move: ChessMove): String {
        val pieceName = move.piece.type.antiqueName
        val dest = move.to.toAlgebraic().uppercase()

        if (move.isCastling) {
            val roqueType = if (move.to.col == 6) "le petit roque impérial" else "le grand roque sénatorial"
            return "L'Empereur commande $roqueType"
        }

        if (move.capturedPiece != null) {
            val victimName = move.capturedPiece.type.antiqueName
            return "$pieceName brise $victimName en $dest !"
        }

        if (move.promotionPiece != null) {
            val promoName = move.promotionPiece.antiqueName
            return "Légionnaire promu au rang de $promoName en $dest !"
        }

        return "$pieceName s'avance en $dest"
    }

    fun isInsufficientMaterial(board: Array<Array<ChessPiece?>>): Boolean {
        var whiteKnights = 0
        var whiteBishops = 0
        var blackKnights = 0
        var blackBishops = 0

        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c] ?: continue
                when (p.type) {
                    PieceType.PAWN, PieceType.ROOK, PieceType.QUEEN -> return false
                    PieceType.KNIGHT -> if (p.color == PieceColor.WHITE) whiteKnights++ else blackKnights++
                    PieceType.BISHOP -> if (p.color == PieceColor.WHITE) whiteBishops++ else blackBishops++
                    PieceType.KING -> {}
                }
            }
        }

        val totalWhiteMinors = whiteKnights + whiteBishops
        val totalBlackMinors = blackKnights + blackBishops

        // King vs King
        if (totalWhiteMinors == 0 && totalBlackMinors == 0) return true
        // King + minor vs King
        if ((totalWhiteMinors == 1 && totalBlackMinors == 0) || (totalWhiteMinors == 0 && totalBlackMinors == 1)) return true
        // King + Bishop vs King + Bishop (same color squares simplification)
        if (totalWhiteMinors == 1 && totalBlackMinors == 1 && whiteBishops == 1 && blackBishops == 1) return true

        return false
    }
}
