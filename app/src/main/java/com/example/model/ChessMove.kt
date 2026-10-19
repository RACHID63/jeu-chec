package com.example.model

enum class MoveQuality(val label: String, val colorHex: Long, val badge: String) {
    BRILLIANT("Coup Impérial !", 0xFF00C853, "★"),
    GREAT("Excellent", 0xFF2E7D32, "✓✓"),
    GOOD("Bon coup", 0xFF43A047, "✓"),
    INACCURACY("Imprécision", 0xFFF57F17, "?!"),
    MISTAKE("Erreur", 0xFFE65100, "?"),
    BLUNDER("Gaffe majeure", 0xFFC62828, "??")
}

data class ChessMove(
    val from: Position,
    val to: Position,
    val piece: ChessPiece,
    val capturedPiece: ChessPiece? = null,
    val isCastling: Boolean = false,
    val isEnPassant: Boolean = false,
    val promotionPiece: PieceType? = null,
    var isCheck: Boolean = false,
    var isCheckmate: Boolean = false,
    var san: String = "",
    var antiqueDescription: String = "",
    var quality: MoveQuality? = null,
    var tutorCommentary: String? = null
)
