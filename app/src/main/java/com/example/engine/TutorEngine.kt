package com.example.engine

import com.example.model.ChessMove
import com.example.model.ChessPiece
import com.example.model.GameState
import com.example.model.MoveQuality
import com.example.model.PieceColor
import com.example.model.PieceType
import com.example.model.Position

data class ThreatMap(
    val attackedSquaresByWhite: Set<Position>,
    val attackedSquaresByBlack: Set<Position>,
    val hangingPieces: List<Position>,
    val safeSquaresForTurn: Set<Position>
)

data class TutorHint(
    val move: ChessMove,
    val explanation: String,
    val tacticalTheme: String
)

data class TacticalPuzzle(
    val id: String,
    val title: String,
    val subtitle: String,
    val fenDescription: String,
    val prompt: String,
    val solutionMove: ChessMove,
    val explanation: String,
    val setupState: GameState
)

object TutorEngine {

    fun generateHint(state: GameState): TutorHint? {
        val legalMoves = ChessEngine.getAllLegalMoves(state, state.currentTurn)
        if (legalMoves.isEmpty()) return null

        // Find best move using evaluation
        val isWhite = state.currentTurn == PieceColor.WHITE
        var bestMove = legalMoves.first()
        var bestScore = if (isWhite) -1000000 else 1000000

        for (move in legalMoves) {
            val nextState = ChessEngine.applyMove(state, move)
            val score = ChessAI.evaluateBoard(nextState)
            if (isWhite) {
                if (score > bestScore) {
                    bestScore = score
                    bestMove = move
                }
            } else {
                if (score < bestScore) {
                    bestScore = score
                    bestMove = move
                }
            }
        }

        val (theme, explanation) = formulateAdvice(state, bestMove)
        return TutorHint(move = bestMove, explanation = explanation, tacticalTheme = theme)
    }

    private fun formulateAdvice(state: GameState, move: ChessMove): Pair<String, String> {
        val dest = move.to.toAlgebraic().uppercase()
        val pieceName = move.piece.type.antiqueName

        if (move.isCheckmate) {
            return Pair("Victoire Absolue", "Triomphe immédiat ! $pieceName en $dest inflige l'échec et mat définitif.")
        }
        if (move.isCheck) {
            return Pair("Attaque Royale", "Mettez le souverain ennemi sous pression directe avec $pieceName en $dest.")
        }
        if (move.capturedPiece != null) {
            val victimName = move.capturedPiece.type.antiqueName
            return Pair("Capture Tactique", "Brisez $victimName adverse en $dest pour affaiblir les lignes ennemies.")
        }
        if (move.isCastling) {
            return Pair("Protection Impériale", "Mettez votre Empereur à l'abri des cohortes ennemies et activez votre Colonne.")
        }
        if (move.piece.type == PieceType.PAWN && (move.to.row == 3 || move.to.row == 4) && (move.to.col in 3..4)) {
            return Pair("Domination Centrale", "Occupez le forum central ($dest) avec votre Légionnaire pour restreindre les cavaliers adverses.")
        }
        if (move.piece.type == PieceType.KNIGHT && move.to.row in 2..5 && move.to.col in 2..5) {
            return Pair("Déploiement de Cavalerie", "Positionnez le Cheval de Bronze au cœur du champ de bataille ($dest) avec un champ d'action maximal.")
        }
        if (move.piece.type == PieceType.BISHOP) {
            return Pair("Contrôle des Diagonales", "Placez le Philosophe sur une longue diagonale pour viser le camp opposé.")
        }

        return Pair("Manœuvre Stratégique", "Avancez $pieceName vers $dest pour renforcer votre dispositif.")
    }

    fun analyzeMoveQuality(previousState: GameState, move: ChessMove, newState: GameState): MoveQuality {
        val isWhite = previousState.currentTurn == PieceColor.WHITE
        val prevEval = ChessAI.evaluateBoard(previousState)
        val newEval = ChessAI.evaluateBoard(newState)

        val delta = if (isWhite) newEval - prevEval else prevEval - newEval

        return when {
            move.isCheckmate || delta >= 150 -> MoveQuality.BRILLIANT
            delta in 20..149 -> MoveQuality.GREAT
            delta in -30..19 -> MoveQuality.GOOD
            delta in -120..-31 -> MoveQuality.INACCURACY
            delta in -280..-121 -> MoveQuality.MISTAKE
            else -> MoveQuality.BLUNDER
        }
    }

    fun computeThreatMap(state: GameState): ThreatMap {
        val whiteAttacks = mutableSetOf<Position>()
        val blackAttacks = mutableSetOf<Position>()

        for (r in 0..7) {
            for (c in 0..7) {
                val p = Position(r, c)
                if (ChessEngine.isSquareAttacked(p, PieceColor.WHITE, state.board)) {
                    whiteAttacks.add(p)
                }
                if (ChessEngine.isSquareAttacked(p, PieceColor.BLACK, state.board)) {
                    blackAttacks.add(p)
                }
            }
        }

        val currentTurn = state.currentTurn
        val enemyAttacks = if (currentTurn == PieceColor.WHITE) blackAttacks else whiteAttacks
        val hangingPieces = mutableListOf<Position>()

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = state.board[r][c] ?: continue
                if (piece.color == currentTurn) {
                    val pos = Position(r, c)
                    val isAttacked = enemyAttacks.contains(pos)
                    val isDefended = if (currentTurn == PieceColor.WHITE) whiteAttacks.contains(pos) else blackAttacks.contains(pos)
                    if (isAttacked && !isDefended) {
                        hangingPieces.add(pos)
                    }
                }
            }
        }

        val safeSquares = mutableSetOf<Position>()
        for (r in 0..7) {
            for (c in 0..7) {
                val pos = Position(r, c)
                if (!enemyAttacks.contains(pos)) {
                    safeSquares.add(pos)
                }
            }
        }

        return ThreatMap(
            attackedSquaresByWhite = whiteAttacks,
            attackedSquaresByBlack = blackAttacks,
            hangingPieces = hangingPieces,
            safeSquaresForTurn = safeSquares
        )
    }

    fun getPuzzles(): List<TacticalPuzzle> {
        val puzzles = mutableListOf<TacticalPuzzle>()

        // Puzzle 1: "Fourchette de Mars" (Knight Fork)
        // White Knight on e4 attacks black King on c7 and Queen on g7 from d6
        val b1 = Array(8) { Array<ChessPiece?>(8) { null } }
        b1[1][2] = ChessPiece(PieceType.KING, PieceColor.BLACK) // c7
        b1[1][6] = ChessPiece(PieceType.QUEEN, PieceColor.BLACK) // g7
        b1[4][4] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE) // e4
        b1[7][4] = ChessPiece(PieceType.KING, PieceColor.WHITE) // e1
        val state1 = GameState(board = b1, currentTurn = PieceColor.WHITE)
        puzzles.add(
            TacticalPuzzle(
                id = "puzzle_1",
                title = "La Fourchette de Mars",
                subtitle = "Assaut de Cavalerie",
                fenDescription = "Trouvez le coup de cavalier dévastateur.",
                prompt = "Le Cheval de Bronze blanc peut frapper l'Empereur et la Déesse noire simultanément.",
                solutionMove = ChessMove(
                    from = Position(4, 4),
                    to = Position(2, 3), // d6
                    piece = b1[4][4]!!,
                    isCheck = true
                ),
                explanation = "En jouant Cavalier d6+, vous effectuez une fourchette royale imparable : le Roi noir doit fuir, laissant la Dame à votre merci !",
                setupState = state1
            )
        )

        // Puzzle 2: "Le Mat du Forum" (Back-rank Mate with Rook Column)
        val b2 = Array(8) { Array<ChessPiece?>(8) { null } }
        b2[0][6] = ChessPiece(PieceType.KING, PieceColor.BLACK) // g8
        b2[1][5] = ChessPiece(PieceType.PAWN, PieceColor.BLACK) // f7
        b2[1][6] = ChessPiece(PieceType.PAWN, PieceColor.BLACK) // g7
        b2[1][7] = ChessPiece(PieceType.PAWN, PieceColor.BLACK) // h7
        b2[7][0] = ChessPiece(PieceType.ROOK, PieceColor.WHITE) // a1
        b2[7][4] = ChessPiece(PieceType.KING, PieceColor.WHITE) // e1
        val state2 = GameState(board = b2, currentTurn = PieceColor.WHITE)
        puzzles.add(
            TacticalPuzzle(
                id = "puzzle_2",
                title = "Le Couloir du Colisée",
                subtitle = "Mat du Couloir",
                fenDescription = "L'Empereur noir est prisonnier derrière ses propres légionnaires.",
                prompt = "Faites triompher la Colonne blanche d'un seul coup décisif.",
                solutionMove = ChessMove(
                    from = Position(7, 0),
                    to = Position(0, 0), // a8
                    piece = b2[7][0]!!,
                    isCheckmate = true
                ),
                explanation = "Colonne a8# ! Les trois légionnaires noirs bloquent la retraite de leur souverain.",
                setupState = state2
            )
        )

        // Puzzle 3: "Le Baiser de Diane" (Goddess Queen Checkmate)
        val b3 = Array(8) { Array<ChessPiece?>(8) { null } }
        b3[0][4] = ChessPiece(PieceType.KING, PieceColor.BLACK) // e8
        b3[2][2] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE) // c6
        b3[4][5] = ChessPiece(PieceType.QUEEN, PieceColor.WHITE) // f4
        b3[7][4] = ChessPiece(PieceType.KING, PieceColor.WHITE) // e1
        val state3 = GameState(board = b3, currentTurn = PieceColor.WHITE)
        puzzles.add(
            TacticalPuzzle(
                id = "puzzle_3",
                title = "Le Triomphe de la Déesse",
                subtitle = "Mat coordonné",
                fenDescription = "Le Philosophe protège la case e7.",
                prompt = "Portez l'estocade finale avec la Déesse drapée.",
                solutionMove = ChessMove(
                    from = Position(4, 5),
                    to = Position(1, 4), // e7
                    piece = b3[4][5]!!,
                    isCheckmate = true
                ),
                explanation = "Déesse e7# ! Protégée par le Philosophe en c6, l'Empereur noir ne peut ni capturer ni s'échapper.",
                setupState = state3
            )
        )

        return puzzles
    }
}
