package com.example.model

enum class GameStatus(val description: String) {
    IN_PROGRESS("Partie en cours"),
    CHECK("Échec au Roi !"),
    CHECKMATE("Échec et Mat !"),
    STALEMATE("Pat (Nulle)"),
    DRAW_INSUFFICIENT_MATERIAL("Nulle : Matériel insuffisant"),
    DRAW_FIFTY_MOVES("Nulle : Règle des 50 coups"),
    DRAW_REPETITION("Nulle : Répétition de position")
}

enum class AIDifficulty(
    val title: String,
    val subtitle: String,
    val searchDepth: Int,
    val description: String
) {
    NOVICE(
        title = "Légionnaire",
        subtitle = "Niveau I (Initié)",
        searchDepth = 1,
        description = "Adversaire accessible qui apprend la tactique romaine et privilégie les captures directes."
    ),
    CENTURION(
        title = "Centurion",
        subtitle = "Niveau II (Vétéran)",
        searchDepth = 3,
        description = "Commandant aguerri utilisant l'élagage Alpha-Beta et la maîtrise des cases centrales."
    ),
    IMPERATOR(
        title = "César",
        subtitle = "Niveau III (Stratège Suprême)",
        searchDepth = 4,
        description = "L'esprit de Jules César : profondeur tactique avancée, sécurité du roi et recherche de quiétude."
    )
}

data class CastlingRights(
    val whiteKingside: Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside: Boolean = true,
    val blackQueenside: Boolean = true
)

data class GameState(
    val board: Array<Array<ChessPiece?>>,
    val currentTurn: PieceColor = PieceColor.WHITE,
    val castlingRights: CastlingRights = CastlingRights(),
    val enPassantTarget: Position? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1,
    val moveHistory: List<ChessMove> = emptyList(),
    val capturedWhite: List<ChessPiece> = emptyList(), // Captured by black (white pieces lost)
    val capturedBlack: List<ChessPiece> = emptyList(), // Captured by white (black pieces lost)
    val status: GameStatus = GameStatus.IN_PROGRESS,
    val winner: PieceColor? = null
) {
    fun pieceAt(pos: Position): ChessPiece? = board[pos.row][pos.col]
    fun pieceAt(row: Int, col: Int): ChessPiece? = if (Position.isValid(row, col)) board[row][col] else null

    fun copyBoard(): Array<Array<ChessPiece?>> {
        return Array(8) { r ->
            Array(8) { c ->
                board[r][c]
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GameState
        if (!board.contentDeepEquals(other.board)) return false
        if (currentTurn != other.currentTurn) return false
        if (castlingRights != other.castlingRights) return false
        if (enPassantTarget != other.enPassantTarget) return false
        if (status != other.status) return false
        return true
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + currentTurn.hashCode()
        result = 31 * result + castlingRights.hashCode()
        result = 31 * result + (enPassantTarget?.hashCode() ?: 0)
        result = 31 * result + status.hashCode()
        return result
    }

    companion object {
        fun initial(): GameState {
            val board = Array(8) { Array<ChessPiece?>(8) { null } }

            // Row 0: Black major pieces
            board[0][0] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
            board[0][1] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
            board[0][2] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
            board[0][3] = ChessPiece(PieceType.QUEEN, PieceColor.BLACK)
            board[0][4] = ChessPiece(PieceType.KING, PieceColor.BLACK)
            board[0][5] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
            board[0][6] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
            board[0][7] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)

            // Row 1: Black pawns
            for (c in 0..7) {
                board[1][c] = ChessPiece(PieceType.PAWN, PieceColor.BLACK)
            }

            // Row 6: White pawns
            for (c in 0..7) {
                board[6][c] = ChessPiece(PieceType.PAWN, PieceColor.WHITE)
            }

            // Row 7: White major pieces
            board[7][0] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)
            board[7][1] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
            board[7][2] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
            board[7][3] = ChessPiece(PieceType.QUEEN, PieceColor.WHITE)
            board[7][4] = ChessPiece(PieceType.KING, PieceColor.WHITE)
            board[7][5] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
            board[7][6] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
            board[7][7] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)

            return GameState(board = board)
        }
    }
}
