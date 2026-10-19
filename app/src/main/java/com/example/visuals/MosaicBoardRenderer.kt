package com.example.visuals

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.example.model.ChessMove
import com.example.model.Position
import kotlin.math.cos
import kotlin.math.sin

object MosaicBoardRenderer {

    // Palette
    private val SlabEdge = Color(0xFF14100D)
    private val FrameOuter = Color(0xFF1E1A16)
    private val FrameBorder = Color(0xFF2C241D)
    private val GoldTrim = Color(0xFFD4AF37)
    private val GoldTrimLight = Color(0xFFFFDF7A)

    // Light square tesserae (Cream / Travertine Marble)
    val LightSquareBase = Color(0xFFEFE8DB)
    private val LightTesserae1 = Color(0xFFF7F3EB)
    private val LightTesserae2 = Color(0xFFE4DCCE)
    private val LightGrout = Color(0xFFC7BBAA)

    // Dark square tesserae (Terracotta / Antique Roman bronze)
    val DarkSquareBase = Color(0xFF5A483E)
    private val DarkTesserae1 = Color(0xFF6B584D)
    private val DarkTesserae2 = Color(0xFF4A3A31)
    private val DarkGrout = Color(0xFF332720)

    fun projectPoint(
        u: Float, // from -1 (file A) to +1 (file H)
        v: Float, // from -1 (rank 8) to +1 (rank 1)
        yaw: Float,
        pitch: Float,
        center: Offset,
        radius: Float
    ): Offset {
        val yawRad = Math.toRadians(yaw.toDouble())
        val cosY = cos(yawRad).toFloat()
        val sinY = sin(yawRad).toFloat()

        val pitchRad = Math.toRadians(pitch.toDouble())
        val cosP = cos(pitchRad).toFloat()

        val rx = u * cosY - v * sinY
        val ry = u * sinY + v * cosY

        val px = center.x + rx * radius
        val py = center.y + ry * radius * cosP
        return Offset(px, py)
    }

    fun drawProjectedMosaicBoard(
        drawScope: DrawScope,
        center: Offset,
        boardRadius: Float,
        yaw: Float,
        pitch: Float,
        selectedPos: Position?,
        legalDestinations: List<ChessMove>,
        lastMove: ChessMove?,
        checkPos: Position?,
        hintMove: ChessMove?,
        threatSquares: Set<Position>?,
        safeSquares: Set<Position>?
    ) {
        val pitchRad = Math.toRadians(pitch.toDouble())
        val cosP = cos(pitchRad).toFloat()

        // 1. Draw 3D Slab Thickness (Extruded vertical marble rim downward)
        if (pitch > 8f) {
            val slabThickness = boardRadius * 0.08f * (pitch / 45f).coerceIn(0.2f, 1.2f)
            val outerU = 1.15f
            val outerV = 1.15f

            // Project bottom rim and top rim
            val cornersTop = listOf(
                projectPoint(-outerU, -outerV, yaw, pitch, center, boardRadius),
                projectPoint(outerU, -outerV, yaw, pitch, center, boardRadius),
                projectPoint(outerU, outerV, yaw, pitch, center, boardRadius),
                projectPoint(-outerU, outerV, yaw, pitch, center, boardRadius)
            )

            // Draw side drop faces for the two visible front edges
            for (i in 0 until 4) {
                val next = (i + 1) % 4
                val p1 = cornersTop[i]
                val p2 = cornersTop[next]
                // Only draw edges facing forward (downwards)
                if (p2.y >= p1.y || p1.y >= center.y) {
                    val sidePath = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p2.x, p2.y + slabThickness)
                        lineTo(p1.x, p1.y + slabThickness)
                        close()
                    }
                    drawScope.drawPath(
                        sidePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(FrameBorder, SlabEdge),
                            startY = p1.y,
                            endY = p1.y + slabThickness
                        )
                    )
                    drawScope.drawLine(
                        color = GoldTrim.copy(alpha = 0.5f),
                        start = Offset(p1.x, p1.y + slabThickness),
                        end = Offset(p2.x, p2.y + slabThickness),
                        strokeWidth = 2f
                    )
                }
            }
        }

        // 2. Draw Outer Stone Plinth Frame
        val frameOuterPath = Path().apply {
            val uOuter = 1.14f
            val vOuter = 1.14f
            val p0 = projectPoint(-uOuter, -vOuter, yaw, pitch, center, boardRadius)
            val p1 = projectPoint(uOuter, -vOuter, yaw, pitch, center, boardRadius)
            val p2 = projectPoint(uOuter, vOuter, yaw, pitch, center, boardRadius)
            val p3 = projectPoint(-uOuter, vOuter, yaw, pitch, center, boardRadius)
            moveTo(p0.x, p0.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            close()
        }
        drawScope.drawPath(frameOuterPath, FrameOuter)

        // Gold inlay border
        val goldBorderPath = Path().apply {
            val uGold = 1.05f
            val vGold = 1.05f
            val p0 = projectPoint(-uGold, -vGold, yaw, pitch, center, boardRadius)
            val p1 = projectPoint(uGold, -vGold, yaw, pitch, center, boardRadius)
            val p2 = projectPoint(uGold, vGold, yaw, pitch, center, boardRadius)
            val p3 = projectPoint(-uGold, vGold, yaw, pitch, center, boardRadius)
            moveTo(p0.x, p0.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            close()
        }
        drawScope.drawPath(goldBorderPath, GoldTrim, style = Stroke(width = 3f))

        // 3. Draw 64 Projected Mosaic Squares
        for (r in 0..7) {
            for (c in 0..7) {
                val pos = Position(r, c)
                val isLight = (r + c) % 2 == 0

                val u0 = (c / 4f) - 1f
                val u1 = ((c + 1) / 4f) - 1f
                val v0 = (r / 4f) - 1f
                val v1 = ((r + 1) / 4f) - 1f

                val p0 = projectPoint(u0, v0, yaw, pitch, center, boardRadius)
                val p1 = projectPoint(u1, v0, yaw, pitch, center, boardRadius)
                val p2 = projectPoint(u1, v1, yaw, pitch, center, boardRadius)
                val p3 = projectPoint(u0, v1, yaw, pitch, center, boardRadius)

                val sqPath = Path().apply {
                    moveTo(p0.x, p0.y)
                    lineTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p3.x, p3.y)
                    close()
                }

                // Base square color with 2x2 mosaic tesserae sub-tiles
                val baseColor = if (isLight) LightSquareBase else DarkSquareBase
                drawScope.drawPath(sqPath, baseColor)

                // 2x2 miniature mosaic tesserae subdivision inside each square
                val tCol1 = if (isLight) LightTesserae1 else DarkTesserae1
                val tCol2 = if (isLight) LightTesserae2 else DarkTesserae2
                val uMid = (u0 + u1) / 2f
                val vMid = (v0 + v1) / 2f

                val pMidTop = projectPoint(uMid, v0, yaw, pitch, center, boardRadius)
                val pMidBottom = projectPoint(uMid, v1, yaw, pitch, center, boardRadius)
                val pMidLeft = projectPoint(u0, vMid, yaw, pitch, center, boardRadius)
                val pMidRight = projectPoint(u1, vMid, yaw, pitch, center, boardRadius)
                val pCenter = projectPoint(uMid, vMid, yaw, pitch, center, boardRadius)

                // Sub-tessera 1 (top-left)
                val sub1 = Path().apply {
                    moveTo(p0.x, p0.y); lineTo(pMidTop.x, pMidTop.y); lineTo(pCenter.x, pCenter.y); lineTo(pMidLeft.x, pMidLeft.y); close()
                }
                drawScope.drawPath(sub1, tCol1)

                // Sub-tessera 2 (bottom-right)
                val sub2 = Path().apply {
                    moveTo(pCenter.x, pCenter.y); lineTo(pMidRight.x, pMidRight.y); lineTo(p2.x, p2.y); lineTo(pMidBottom.x, pMidBottom.y); close()
                }
                drawScope.drawPath(sub2, tCol2)

                // Grout grid lines
                val grout = if (isLight) LightGrout else DarkGrout
                drawScope.drawLine(color = grout, start = pMidTop, end = pMidBottom, strokeWidth = 1.2f)
                drawScope.drawLine(color = grout, start = pMidLeft, end = pMidRight, strokeWidth = 1.2f)
                drawScope.drawPath(sqPath, grout, style = Stroke(width = 1.5f))

                // Threat map overlay
                if (threatSquares != null && threatSquares.contains(pos)) {
                    drawScope.drawPath(sqPath, Color(0x44E53935))
                } else if (safeSquares != null && safeSquares.contains(pos)) {
                    drawScope.drawPath(sqPath, Color(0x3043A047))
                }

                // Last move highlight
                if (lastMove != null && (lastMove.from == pos || lastMove.to == pos)) {
                    drawScope.drawPath(sqPath, Color(0x40D4AF37))
                }

                // Check highlight
                if (checkPos == pos) {
                    drawScope.drawPath(sqPath, Color(0x66D32F2F))
                    drawScope.drawPath(sqPath, Color(0xFFE53935), style = Stroke(width = 3.5f))
                }

                // Selected square highlight
                if (selectedPos == pos) {
                    drawScope.drawPath(sqPath, Color(0x55D4AF37))
                    drawScope.drawPath(sqPath, GoldTrimLight, style = Stroke(width = 4f))
                }
            }
        }

        // 4 Corner Roman SPQR Gold Medallions on the outer plinth
        val cornerDist = 1.10f
        for (sx in listOf(-1f, 1f)) {
            for (sy in listOf(-1f, 1f)) {
                val medPt = projectPoint(sx * cornerDist, sy * cornerDist, yaw, pitch, center, boardRadius)
                drawScope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GoldTrimLight, GoldTrim, Color(0xFF6B4E03)),
                        center = Offset(medPt.x - 2f, medPt.y - 2f),
                        radius = boardRadius * 0.05f
                    ),
                    radius = boardRadius * 0.038f,
                    center = medPt
                )
            }
        }

        // 4. Draw Coordinates (Roman Numerals and Latin Letters)
        drawProjectedCoordinates(drawScope, center, boardRadius, yaw, pitch)

        // 5. Draw Legal Move Markers on Mosaic Tiles
        val squareRadius = (boardRadius / 4f) * 0.5f
        for (move in legalDestinations) {
            val u = ((move.to.col + 0.5f) / 4f) - 1f
            val v = ((move.to.row + 0.5f) / 4f) - 1f
            val pt = projectPoint(u, v, yaw, pitch, center, boardRadius)

            if (move.capturedPiece != null) {
                // Capture target: Antique golden laurel ring on tile
                drawScope.drawOval(
                    color = GoldTrim,
                    topLeft = Offset(pt.x - squareRadius * 0.75f, pt.y - squareRadius * 0.75f * cosP),
                    size = Size(squareRadius * 1.5f, squareRadius * 1.5f * cosP),
                    style = Stroke(width = 3.5f)
                )
            } else {
                // Empty destination: Marble/Gold mosaic marker
                drawScope.drawOval(
                    color = Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(pt.x - squareRadius * 0.35f, pt.y - squareRadius * 0.35f * cosP + 1.5f),
                    size = Size(squareRadius * 0.7f, squareRadius * 0.7f * cosP)
                )
                drawScope.drawOval(
                    color = GoldTrim,
                    topLeft = Offset(pt.x - squareRadius * 0.30f, pt.y - squareRadius * 0.30f * cosP),
                    size = Size(squareRadius * 0.6f, squareRadius * 0.6f * cosP)
                )
            }
        }

        // 6. Draw Hint Arrow (if requested in learning mode)
        if (hintMove != null) {
            val uFrom = ((hintMove.from.col + 0.5f) / 4f) - 1f
            val vFrom = ((hintMove.from.row + 0.5f) / 4f) - 1f
            val ptFrom = projectPoint(uFrom, vFrom, yaw, pitch, center, boardRadius)

            val uTo = ((hintMove.to.col + 0.5f) / 4f) - 1f
            val vTo = ((hintMove.to.row + 0.5f) / 4f) - 1f
            val ptTo = projectPoint(uTo, vTo, yaw, pitch, center, boardRadius)

            drawScope.drawLine(
                color = GoldTrimLight,
                start = ptFrom,
                end = ptTo,
                strokeWidth = 5.5f,
                cap = StrokeCap.Round
            )
            drawScope.drawCircle(color = GoldTrimLight, radius = 6f, center = ptTo)
        }
    }

    private fun drawProjectedCoordinates(
        drawScope: DrawScope,
        center: Offset,
        boardRadius: Float,
        yaw: Float,
        pitch: Float
    ) {
        val paint = Paint().apply {
            color = android.graphics.Color.argb(220, 212, 175, 55)
            textSize = boardRadius * 0.055f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
        }
        val canvas = drawScope.drawContext.canvas.nativeCanvas

        val files = arrayOf("A", "B", "C", "D", "E", "F", "G", "H")
        for (c in 0..7) {
            val u = ((c + 0.5f) / 4f) - 1f
            // Front margin
            val pt = projectPoint(u, 1.08f, yaw, pitch, center, boardRadius)
            canvas.drawText(files[c], pt.x, pt.y + 4f, paint)
        }

        val romanRanks = arrayOf("VIII", "VII", "VI", "V", "IV", "III", "II", "I")
        for (r in 0..7) {
            val v = ((r + 0.5f) / 4f) - 1f
            val pt = projectPoint(-1.08f, v, yaw, pitch, center, boardRadius)
            canvas.drawText(romanRanks[r], pt.x, pt.y + 4f, paint)
        }
    }
}
