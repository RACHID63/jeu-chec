package com.example.model

data class Position(val row: Int, val col: Int) {
    init {
        require(row in 0..7 && col in 0..7) { "Row and col must be in 0..7: row=$row, col=$col" }
    }

    val rank: Int get() = 8 - row
    val fileChar: Char get() = ('a'.code + col).toChar()

    fun toAlgebraic(): String = "$fileChar$rank"

    fun toRomanRank(): String = when (rank) {
        1 -> "I"
        2 -> "II"
        3 -> "III"
        4 -> "IV"
        5 -> "V"
        6 -> "VI"
        7 -> "VII"
        8 -> "VIII"
        else -> rank.toString()
    }

    companion object {
        fun fromAlgebraic(algebraic: String): Position? {
            if (algebraic.length != 2) return null
            val file = algebraic[0].lowercaseChar()
            val rankChar = algebraic[1]
            if (file !in 'a'..'h' || rankChar !in '1'..'8') return null
            val col = file - 'a'
            val row = 8 - (rankChar - '0')
            return Position(row, col)
        }

        fun isValid(row: Int, col: Int): Boolean = row in 0..7 && col in 0..7
    }
}
