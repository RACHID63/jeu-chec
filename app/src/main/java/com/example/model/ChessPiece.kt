package com.example.model

enum class PieceColor(val displayName: String, val materialName: String) {
    WHITE("Blancs", "Marbre de Carrare"),
    BLACK("Noirs", "Bronze Patiné");

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(
    val symbol: String,
    val antiqueName: String,
    val antiqueTitle: String,
    val baseValue: Int,
    val lore: String
) {
    KING(
        symbol = "K",
        antiqueName = "Empereur Romain",
        antiqueTitle = "Imperator Caesar",
        baseValue = 10000,
        lore = "Sculpté avec la toge sénatoriale, la couronne de laurier triomphale et le sceptre surmonté de l'aigle impérial (aquila)."
    ),
    QUEEN(
        symbol = "Q",
        antiqueName = "Déesse Drapée",
        antiqueTitle = "Augusta Victoria",
        baseValue = 900,
        lore = "Représentée avec une stola drapée majestueuse, un diadème orné et une coupe sacrée apportant la victoire."
    ),
    BISHOP(
        symbol = "B",
        antiqueName = "Philosophe",
        antiqueTitle = "Philosophus",
        baseValue = 320,
        lore = "Sage barbu à la toge antique tenant un volumen de papyrus scellé contenant les secrets stratégiques."
    ),
    KNIGHT(
        symbol = "N",
        antiqueName = "Cheval de Bronze",
        antiqueTitle = "Equus Bellator",
        baseValue = 300,
        lore = "Étalon de guerre fougueux à la crinière ciselée, paré d'un harnais d'apparat et de phalères gravées."
    ),
    ROOK(
        symbol = "R",
        antiqueName = "Colonne Antique",
        antiqueTitle = "Columna Triumphalis",
        baseValue = 500,
        lore = "Colonne cannelée d'ordre corinthien sommée de feuilles d'acanthe et d'un entablement impérial crénelé."
    ),
    PAWN(
        symbol = "P",
        antiqueName = "Légionnaire",
        antiqueTitle = "Legionarius",
        baseValue = 100,
        lore = "Guerrier romain casqué (galea) avec sa cuirasse segmentée, armé du pilum et du grand bouclier (scutum)."
    )
}

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor,
    val id: String = "${color}_${type}_${System.nanoTime()}_${(0..999).random()}"
)
