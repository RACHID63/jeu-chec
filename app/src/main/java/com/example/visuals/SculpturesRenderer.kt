package com.example.visuals

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.ChessPiece
import com.example.model.PieceColor
import com.example.model.PieceType
import kotlin.math.cos
import kotlin.math.sin

object SculpturesRenderer {

    // 1. Carrara White Marble Palette (Camp Blanc)
    private val MarbleSpecular = Color(0xFFFFFFFF)
    private val MarbleHighlight = Color(0xFFFAF7F2)
    private val MarbleLight = Color(0xFFEDE5D6)
    private val MarbleMid = Color(0xFFD6CABA)
    private val MarbleDark = Color(0xFFB5A693)
    private val MarbleShadow = Color(0xFF887763)
    private val MarbleAmbientOcclusion = Color(0xFF5A4C3D)
    private val MarbleVein = Color(0xFFC0B29E)

    // Gold Inlay & Ornament Palette
    private val GoldSpecular = Color(0xFFFFF6D1)
    private val GoldHighlight = Color(0xFFFFE082)
    private val GoldPrimary = Color(0xFFD4AF37)
    private val GoldDark = Color(0xFFAA8012)
    private val GoldShadow = Color(0xFF6B4E03)

    // 2. Weathered Verdigris Patinated Bronze Palette (Camp Noir)
    private val BronzeSpecular = Color(0xFFE2C478)
    private val BronzeHighlight = Color(0xFF736354)
    private val BronzeLight = Color(0xFF4A3F36)
    private val BronzeMid = Color(0xFF2C241F)
    private val BronzeDark = Color(0xFF191411)
    private val BronzeDeepShadow = Color(0xFF0D0A08)

    // Verdigris Copper Oxidation Patina
    private val PatinaBright = Color(0xFF68D9C2)
    private val PatinaLight = Color(0xFF45B09B)
    private val PatinaMid = Color(0xFF287D6E)
    private val PatinaDark = Color(0xFF165247)

    // Volumetric 3D Cylinder / Form Brush (Simulates Studio Key Light from top-left)
    private fun getVolumetricBrush(isWhite: Boolean, left: Float, right: Float): Brush {
        return if (isWhite) {
            Brush.horizontalGradient(
                0.0f to MarbleSpecular,
                0.20f to MarbleHighlight,
                0.55f to MarbleMid,
                0.85f to MarbleDark,
                1.0f to MarbleShadow,
                startX = left,
                endX = right
            )
        } else {
            Brush.horizontalGradient(
                0.0f to BronzeHighlight,
                0.18f to BronzeMid,
                0.50f to PatinaMid,
                0.82f to BronzeDark,
                1.0f to BronzeDeepShadow,
                startX = left,
                endX = right
            )
        }
    }

    private fun getVerticalGradient(isWhite: Boolean, top: Float, bottom: Float): Brush {
        return if (isWhite) {
            Brush.verticalGradient(
                colors = listOf(MarbleSpecular, MarbleHighlight, MarbleMid, MarbleDark),
                startY = top,
                endY = bottom
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(BronzeHighlight, BronzeMid, PatinaMid, BronzeDark),
                startY = top,
                endY = bottom
            )
        }
    }

    fun drawUprightPiece(
        drawScope: DrawScope,
        piece: ChessPiece,
        groundCenter: Offset,
        baseSquareHeight: Float,
        isSelected: Boolean = false
    ) {
        val isWhite = piece.color == PieceColor.WHITE

        // Authentic Piece Height Hierarchy (King tallest, Queen sleek, Pawns compact)
        val pieceHeight = when (piece.type) {
            PieceType.KING -> baseSquareHeight * 1.15f
            PieceType.QUEEN -> baseSquareHeight * 1.08f
            PieceType.BISHOP -> baseSquareHeight * 0.98f
            PieceType.KNIGHT -> baseSquareHeight * 0.95f
            PieceType.ROOK -> baseSquareHeight * 0.90f
            PieceType.PAWN -> baseSquareHeight * 0.76f
        }
        val width = pieceHeight * 0.72f

        // 1. Realistic Cast Ground Shadow on Mosaic Floor
        val shadowRadiusX = width * 0.58f
        val shadowRadiusY = width * 0.28f
        drawScope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = if (isSelected) 0.60f else 0.46f),
                    Color.Black.copy(alpha = if (isSelected) 0.35f else 0.24f),
                    Color.Transparent
                ),
                center = Offset(groundCenter.x + width * 0.10f, groundCenter.y + shadowRadiusY * 0.15f),
                radius = shadowRadiusX
            ),
            topLeft = Offset(groundCenter.x - shadowRadiusX + width * 0.10f, groundCenter.y - shadowRadiusY),
            size = Size(shadowRadiusX * 2.2f, shadowRadiusY * 2f)
        )

        // 2. Base Lift if selected (subtle float effect)
        val yLift = if (isSelected) pieceHeight * 0.18f else 0f
        val baseCenter = Offset(groundCenter.x, groundCenter.y - yLift)

        // Draw Upright Sculpted 3D Figure
        when (piece.type) {
            PieceType.PAWN -> drawRealisticLegionary(drawScope, baseCenter, width, pieceHeight, isWhite)
            PieceType.ROOK -> drawRealisticColumn(drawScope, baseCenter, width, pieceHeight, isWhite)
            PieceType.KNIGHT -> drawRealisticWarhorse(drawScope, baseCenter, width, pieceHeight, isWhite)
            PieceType.BISHOP -> drawRealisticPhilosopher(drawScope, baseCenter, width, pieceHeight, isWhite)
            PieceType.QUEEN -> drawRealisticGoddess(drawScope, baseCenter, width, pieceHeight, isWhite)
            PieceType.KING -> drawRealisticEmperor(drawScope, baseCenter, width, pieceHeight, isWhite)
        }

        // Selection Imperial Halo Ring
        if (isSelected) {
            drawScope.drawOval(
                brush = Brush.sweepGradient(
                    colors = listOf(GoldSpecular, GoldPrimary, GoldDark, GoldPrimary, GoldSpecular),
                    center = baseCenter
                ),
                topLeft = Offset(baseCenter.x - shadowRadiusX * 1.05f, baseCenter.y - shadowRadiusY * 0.9f),
                size = Size(shadowRadiusX * 2.1f, shadowRadiusY * 1.8f),
                style = Stroke(width = 3.5f)
            )
        }
    }

    // Double-tiered Molded Roman Plinth with Carved Torus and Gold Trim
    private fun drawMoldedPedestal(
        drawScope: DrawScope,
        baseCenter: Offset,
        width: Float,
        height: Float,
        isWhite: Boolean
    ): Float {
        val pedestalW = width * 0.74f
        val pedestalH = height * 0.18f
        val groundY = baseCenter.y

        // Lower plinth ring with ambient ground shadow
        val ring1H = pedestalH * 0.55f
        val ring1Y = groundY - ring1H
        drawScope.drawOval(
            brush = getVolumetricBrush(isWhite, baseCenter.x - pedestalW / 2f, baseCenter.x + pedestalW / 2f),
            topLeft = Offset(baseCenter.x - pedestalW / 2f, ring1Y),
            size = Size(pedestalW, ring1H * 1.7f)
        )
        // Rim highlight on plinth base
        drawScope.drawArc(
            color = if (isWhite) MarbleSpecular.copy(alpha = 0.85f) else BronzeSpecular.copy(alpha = 0.75f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(baseCenter.x - pedestalW / 2f, ring1Y),
            size = Size(pedestalW, ring1H * 1.7f),
            style = Stroke(width = 2f)
        )

        // Molded upper step (Scotiated torus)
        val ring2W = pedestalW * 0.84f
        val ring2H = pedestalH * 0.45f
        val ring2Y = ring1Y - ring2H * 0.75f
        drawScope.drawOval(
            brush = getVolumetricBrush(isWhite, baseCenter.x - ring2W / 2f, baseCenter.x + ring2W / 2f),
            topLeft = Offset(baseCenter.x - ring2W / 2f, ring2Y),
            size = Size(ring2W, ring2H * 1.6f)
        )

        // Gilded/Patinated Meander accent band
        val trimColor = if (isWhite) GoldPrimary else PatinaBright
        drawScope.drawOval(
            color = trimColor.copy(alpha = 0.9f),
            topLeft = Offset(baseCenter.x - ring2W * 0.44f, ring2Y + 2f),
            size = Size(ring2W * 0.88f, ring2H * 0.70f),
            style = Stroke(width = 2.5f)
        )

        return ring2Y // top surface of pedestal where the statue stands
    }

    // 1. PION : LÉGIONNAIRE ROMAIN (Iconic Roman Soldier with Scutum, Galea & Pilum)
    private fun drawRealisticLegionary(
        drawScope: DrawScope,
        baseCenter: Offset,
        width: Float,
        height: Float,
        isWhite: Boolean
    ) {
        val pedestalTop = drawMoldedPedestal(drawScope, baseCenter, width, height, isWhite)
        val bodyH = height * 0.78f
        val topY = pedestalTop - bodyH

        val torsoW = width * 0.40f
        val torsoH = bodyH * 0.42f
        val torsoY = pedestalTop - torsoH

        // Greaves / Legs beneath tunic
        val legW = torsoW * 0.28f
        val legH = pedestalTop - torsoY
        drawScope.drawRoundRect(
            brush = getVolumetricBrush(isWhite, baseCenter.x - torsoW * 0.35f, baseCenter.x - torsoW * 0.05f),
            topLeft = Offset(baseCenter.x - torsoW * 0.35f, torsoY + torsoH * 0.7f),
            size = Size(legW, legH * 0.35f),
            cornerRadius = CornerRadius(3f, 3f)
        )
        drawScope.drawRoundRect(
            brush = getVolumetricBrush(isWhite, baseCenter.x + torsoW * 0.05f, baseCenter.x + torsoW * 0.35f),
            topLeft = Offset(baseCenter.x + torsoW * 0.05f, torsoY + torsoH * 0.7f),
            size = Size(legW, legH * 0.35f),
            cornerRadius = CornerRadius(3f, 3f)
        )

        // Torso / Cuirasse (Lorica Segmentata)
        val torsoPath = Path().apply {
            moveTo(baseCenter.x - torsoW * 0.44f, pedestalTop - torsoH * 0.15f)
            lineTo(baseCenter.x - torsoW * 0.38f, torsoY)
            lineTo(baseCenter.x + torsoW * 0.38f, torsoY)
            lineTo(baseCenter.x + torsoW * 0.44f, pedestalTop - torsoH * 0.15f)
            close()
        }
        drawScope.drawPath(
            torsoPath,
            getVolumetricBrush(isWhite, baseCenter.x - torsoW / 2f, baseCenter.x + torsoW / 2f)
        )

        // Shaded overlapping horizontal armor bands with brass hinges
        val bandCount = 4
        for (i in 1..bandCount) {
            val by = torsoY + torsoH * (i / (bandCount + 1f))
            drawScope.drawLine(
                color = if (isWhite) MarbleShadow else BronzeDeepShadow,
                start = Offset(baseCenter.x - torsoW * 0.38f, by),
                end = Offset(baseCenter.x + torsoW * 0.38f, by),
                strokeWidth = 2.5f
            )
            drawScope.drawLine(
                color = if (isWhite) MarbleSpecular.copy(alpha = 0.8f) else PatinaBright.copy(alpha = 0.75f),
                start = Offset(baseCenter.x - torsoW * 0.35f, by - 1.5f),
                end = Offset(baseCenter.x - torsoW * 0.05f, by - 1.5f),
                strokeWidth = 1.5f
            )
            // Brass center buckle
            drawScope.drawCircle(
                color = GoldPrimary,
                radius = 2.2f,
                center = Offset(baseCenter.x, by)
            )
        }

        // Roman Scutum Shield (Prominent, heavily curved rectangular battle shield)
        val shieldW = width * 0.34f
        val shieldH = bodyH * 0.68f
        val shieldX = baseCenter.x - width * 0.48f
        val shieldY = pedestalTop - shieldH * 0.95f
        val shieldColor = if (isWhite) Color(0xFF8B1E1E) else Color(0xFF1B3830)

        // Shield body with 3D beveling
        drawScope.drawRoundRect(
            color = shieldColor,
            topLeft = Offset(shieldX, shieldY),
            size = Size(shieldW, shieldH),
            cornerRadius = CornerRadius(10f, 10f)
        )
        // Gilded metal perimeter rim
        drawScope.drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(GoldSpecular, GoldPrimary, GoldDark),
                startX = shieldX,
                endX = shieldX + shieldW
            ),
            topLeft = Offset(shieldX + 1.5f, shieldY + 1.5f),
            size = Size(shieldW - 3f, shieldH - 3f),
            cornerRadius = CornerRadius(9f, 9f),
            style = Stroke(width = 3f)
        )
        // Golden Umbo (Convex Center Boss with radial shine)
        val umboCenter = Offset(shieldX + shieldW * 0.5f, shieldY + shieldH * 0.5f)
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GoldSpecular, GoldPrimary, GoldShadow),
                center = Offset(umboCenter.x - 3f, umboCenter.y - 3f),
                radius = shieldW * 0.28f
            ),
            radius = shieldW * 0.22f,
            center = umboCenter
        )
        // Winged Thunderbolts (Fulmen) motif engraved on shield
        val wingColor = GoldSpecular
        drawScope.drawLine(
            color = wingColor,
            start = Offset(umboCenter.x - shieldW * 0.35f, umboCenter.y - shieldH * 0.22f),
            end = Offset(umboCenter.x + shieldW * 0.35f, umboCenter.y + shieldH * 0.22f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
        drawScope.drawLine(
            color = wingColor,
            start = Offset(umboCenter.x + shieldW * 0.35f, umboCenter.y - shieldH * 0.22f),
            end = Offset(umboCenter.x - shieldW * 0.35f, umboCenter.y + shieldH * 0.22f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )

        // Roman Pilum Spear on right side
        val spearX = baseCenter.x + width * 0.38f
        val spearTopY = topY - bodyH * 0.18f
        drawScope.drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(GoldPrimary, if (isWhite) MarbleDark else BronzeMid),
                startY = spearTopY,
                endY = pedestalTop
            ),
            start = Offset(spearX, spearTopY),
            end = Offset(spearX, pedestalTop),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
        // Pyramid spear tip
        val spearTip = Path().apply {
            moveTo(spearX, spearTopY - 16f)
            lineTo(spearX - 5.5f, spearTopY)
            lineTo(spearX + 5.5f, spearTopY)
            close()
        }
        drawScope.drawPath(spearTip, GoldSpecular)

        // Head & Galea Helmet (Imperial Gallic with neck guard & cheek pieces)
        val headRadius = width * 0.18f
        val headCenter = Offset(baseCenter.x, torsoY - headRadius * 0.65f)

        drawScope.drawCircle(
            brush = getVolumetricBrush(isWhite, headCenter.x - headRadius, headCenter.x + headRadius),
            radius = headRadius,
            center = headCenter
        )

        // Helmet Visor / Brow Guard
        drawScope.drawArc(
            color = if (isWhite) GoldPrimary else BronzeSpecular,
            startAngle = 170f,
            sweepAngle = 200f,
            useCenter = false,
            topLeft = Offset(headCenter.x - headRadius * 1.08f, headCenter.y - headRadius * 0.45f),
            size = Size(headRadius * 2.16f, headRadius * 1.4f),
            style = Stroke(width = 3.5f)
        )

        // Cheek Guards (Bucculae)
        drawScope.drawRoundRect(
            color = if (isWhite) MarbleLight else BronzeLight,
            topLeft = Offset(headCenter.x - headRadius * 0.9f, headCenter.y),
            size = Size(headRadius * 0.35f, headRadius * 0.65f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawScope.drawRoundRect(
            color = if (isWhite) MarbleMid else BronzeMid,
            topLeft = Offset(headCenter.x + headRadius * 0.55f, headCenter.y),
            size = Size(headRadius * 0.35f, headRadius * 0.65f),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // Transverse Red Horsehair Crest (Crista) sweeping atop the helmet
        val crestColor = if (isWhite) Color(0xFFC62828) else Color(0xFFB71C1C)
        val crestPath = Path().apply {
            moveTo(headCenter.x - headRadius * 1.15f, headCenter.y - headRadius * 0.35f)
            cubicTo(
                headCenter.x - headRadius * 0.7f, headCenter.y - headRadius * 2.0f,
                headCenter.x + headRadius * 0.7f, headCenter.y - headRadius * 2.0f,
                headCenter.x + headRadius * 1.15f, headCenter.y - headRadius * 0.35f
            )
            lineTo(headCenter.x + headRadius * 0.95f, headCenter.y - headRadius * 0.85f)
            cubicTo(
                headCenter.x + headRadius * 0.45f, headCenter.y - headRadius * 1.5f,
                headCenter.x - headRadius * 0.45f, headCenter.y - headRadius * 1.5f,
                headCenter.x - headRadius * 0.95f, headCenter.y - headRadius * 0.85f
            )
            close()
        }
        drawScope.drawPath(crestPath, crestColor)
    }

    // 2. TOUR : COLONNE CORINTHIENNE (Classical Fluted Architectural Column)
    private fun drawRealisticColumn(
        drawScope: DrawScope,
        baseCenter: Offset,
        width: Float,
        height: Float,
        isWhite: Boolean
    ) {
        val pedestalTop = drawMoldedPedestal(drawScope, baseCenter, width, height, isWhite)
        val colH = height * 0.82f
        val colW = width * 0.44f
        val shaftTop = pedestalTop - colH * 0.68f

        // Column Shaft with 3D cylindrical shading
        drawScope.drawRect(
            brush = getVolumetricBrush(isWhite, baseCenter.x - colW / 2f, baseCenter.x + colW / 2f),
            topLeft = Offset(baseCenter.x - colW / 2f, shaftTop),
            size = Size(colW, pedestalTop - shaftTop)
        )

        // 6 Vertical Flutes (Cannelures) with shaded depth and highlight ridges
        val fluteCount = 5
        for (i in 1..fluteCount) {
            val fx = baseCenter.x - colW / 2f + colW * (i / (fluteCount + 1f))
            // Core shadow of flute
            drawScope.drawLine(
                color = if (isWhite) MarbleDark else BronzeDeepShadow,
                start = Offset(fx + 1.5f, shaftTop + 6f),
                end = Offset(fx + 1.5f, pedestalTop - 6f),
                strokeWidth = 2.8f
            )
            // Left specular ridge
            drawScope.drawLine(
                color = if (isWhite) MarbleSpecular else PatinaBright.copy(alpha = 0.85f),
                start = Offset(fx - 1.2f, shaftTop + 6f),
                end = Offset(fx - 1.2f, pedestalTop - 6f),
                strokeWidth = 1.6f
            )
        }

        // Corinthian Capital (Chapiteau à double rangée d'acanthe et volutes)
        val capW = width * 0.72f
        val capH = colH * 0.18f
        val capTop = shaftTop - capH

        val capPath = Path().apply {
            moveTo(baseCenter.x - colW / 2f, shaftTop)
            lineTo(baseCenter.x - capW / 2f, capTop)
            lineTo(baseCenter.x + capW / 2f, capTop)
            lineTo(baseCenter.x + colW / 2f, shaftTop)
            close()
        }
        drawScope.drawPath(capPath, getVolumetricBrush(isWhite, baseCenter.x - capW / 2f, baseCenter.x + capW / 2f))

        // Volute corner spirals
        val voluteColor = if (isWhite) GoldPrimary else BronzeSpecular
        drawScope.drawCircle(
            color = voluteColor,
            radius = width * 0.08f,
            center = Offset(baseCenter.x - capW * 0.44f, capTop + capH * 0.35f)
        )
        drawScope.drawCircle(
            color = voluteColor,
            radius = width * 0.08f,
            center = Offset(baseCenter.x + capW * 0.44f, capTop + capH * 0.35f)
        )
        // Center acanthus leaf rosette
        drawScope.drawCircle(
            color = GoldPrimary,
            radius = width * 0.055f,
            center = Offset(baseCenter.x, capTop + capH * 0.35f)
        )

        // Classical Crenellated Entablature (Couronnement crénelé de forteresse)
        val entabH = colH * 0.14f
        val entabTop = capTop - entabH
        val crenelW = capW / 5f

        // Cornice shelf
        drawScope.drawRect(
            color = if (isWhite) GoldPrimary else PatinaBright,
            topLeft = Offset(baseCenter.x - capW / 2f - 2f, capTop - 3f),
            size = Size(capW + 4f, 4f)
        )

        // 5 Crenellations
        for (c in 0 until 5) {
            val cx = baseCenter.x - capW / 2f + c * crenelW
            val cH = if (c % 2 == 0) entabH else entabH * 0.50f
            drawScope.drawRoundRect(
                brush = getVolumetricBrush(isWhite, cx, cx + crenelW),
                topLeft = Offset(cx + 1f, entabTop + (entabH - cH)),
                size = Size(crenelW - 2f, cH),
                cornerRadius = CornerRadius(2.5f, 2.5f)
            )
        }
    }

    // 3. CAVALIER : ÉTALON DE GUERRE / CHEVAL DE BRONZE (Expressive Equestrian Warhorse)
    private fun drawRealisticWarhorse(
        drawScope: DrawScope,
        baseCenter: Offset,
        width: Float,
        height: Float,
        isWhite: Boolean
    ) {
        val pedestalTop = drawMoldedPedestal(drawScope, baseCenter, width, height, isWhite)
        val horseH = height * 0.86f
        val topY = pedestalTop - horseH

        // Sculpted Warhorse Silhouette (muscular chest, arched neck, proud head)
        val horsePath = Path().apply {
            moveTo(baseCenter.x - width * 0.24f, pedestalTop)
            // Muscular barrel chest
            cubicTo(
                baseCenter.x - width * 0.52f, pedestalTop - horseH * 0.28f,
                baseCenter.x - width * 0.54f, pedestalTop - horseH * 0.60f,
                baseCenter.x - width * 0.30f, pedestalTop - horseH * 0.78f
            )
            // Muzzle / Flared Nostril
            lineTo(baseCenter.x - width * 0.48f, pedestalTop - horseH * 0.72f)
            lineTo(baseCenter.x - width * 0.40f, pedestalTop - horseH * 0.88f)
            // Forehead & Crest
            lineTo(baseCenter.x - width * 0.12f, topY + horseH * 0.06f)
            // Alert Pricked Ears
            lineTo(baseCenter.x - width * 0.08f, topY)
            lineTo(baseCenter.x + width * 0.02f, topY + horseH * 0.08f)
            // Arched crest and back of neck
            cubicTo(
                baseCenter.x + width * 0.38f, pedestalTop - horseH * 0.70f,
                baseCenter.x + width * 0.42f, pedestalTop - horseH * 0.35f,
                baseCenter.x + width * 0.24f, pedestalTop
            )
            close()
        }
        drawScope.drawPath(
            horsePath,
            getVolumetricBrush(isWhite, baseCenter.x - width * 0.52f, baseCenter.x + width * 0.42f)
        )

        // Sculpted Wavy Mane Strands catching light
        val maneColor = if (isWhite) GoldPrimary else PatinaBright
        val maneX = baseCenter.x + width * 0.18f
        for (i in 0..4) {
            val my = topY + horseH * (0.16f + i * 0.14f)
            drawScope.drawLine(
                color = maneColor,
                start = Offset(maneX - 12f, my),
                end = Offset(maneX + 16f, my + 10f),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }

        // Detailed Eye Socket & Nostril
        val eyeCenter = Offset(baseCenter.x - width * 0.22f, topY + horseH * 0.24f)
        drawScope.drawCircle(
            color = if (isWhite) MarbleShadow else BronzeDeepShadow,
            radius = 4.5f,
            center = eyeCenter
        )
        // Nostril indent
        drawScope.drawCircle(
            color = if (isWhite) MarbleShadow else BronzeDeepShadow,
            radius = 3f,
            center = Offset(baseCenter.x - width * 0.42f, topY + horseH * 0.24f)
        )

        // Roman Cavalry Harness & Embossed Sunburst Phalera Medallion
        drawScope.drawLine(
            color = GoldPrimary,
            start = Offset(eyeCenter.x - 14f, eyeCenter.y + 12f),
            end = Offset(baseCenter.x + width * 0.04f, topY + horseH * 0.44f),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
        // Large chest phalera medallion
        val phaleraCenter = Offset(baseCenter.x - width * 0.10f, pedestalTop - horseH * 0.28f)
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GoldSpecular, GoldPrimary, GoldShadow),
                center = phaleraCenter,
                radius = width * 0.14f
            ),
            radius = width * 0.11f,
            center = phaleraCenter
        )
        drawScope.drawCircle(
            color = GoldSpecular,
            radius = width * 0.045f,
            center = phaleraCenter
        )
    }

    // 4. FOU : PHILOSOPHE ANTIQUE (Venerable Bearded Sage holding Papyrus Scroll)
    private fun drawRealisticPhilosopher(
        drawScope: DrawScope,
        baseCenter: Offset,
        width: Float,
        height: Float,
        isWhite: Boolean
    ) {
        val pedestalTop = drawMoldedPedestal(drawScope, baseCenter, width, height, isWhite)
        val bodyH = height * 0.88f
        val topY = pedestalTop - bodyH

        val robeW = width * 0.60f
        val robeTop = pedestalTop - bodyH * 0.65f

        // Draped Himation (Philosopher's Robe)
        val robePath = Path().apply {
            moveTo(baseCenter.x - robeW * 0.48f, pedestalTop)
            cubicTo(
                baseCenter.x - robeW * 0.58f, robeTop + bodyH * 0.3f,
                baseCenter.x - robeW * 0.40f, robeTop,
                baseCenter.x - robeW * 0.20f, robeTop
            )
            lineTo(baseCenter.x + robeW * 0.20f, robeTop)
            cubicTo(
                baseCenter.x + robeW * 0.40f, robeTop,
                baseCenter.x + robeW * 0.58f, robeTop + bodyH * 0.3f,
                baseCenter.x + robeW * 0.48f, pedestalTop
            )
            close()
        }
        drawScope.drawPath(
            robePath,
            getVolumetricBrush(isWhite, baseCenter.x - robeW / 2f, baseCenter.x + robeW / 2f)
        )

        // Sculpted drapery folds
        for (i in 1..4) {
            val fy = robeTop + bodyH * (i * 0.12f)
            drawScope.drawLine(
                color = if (isWhite) MarbleShadow.copy(alpha = 0.65f) else BronzeDeepShadow.copy(alpha = 0.75f),
                start = Offset(baseCenter.x - robeW * 0.36f, fy - 6f),
                end = Offset(baseCenter.x + robeW * 0.26f, fy + 8f),
                strokeWidth = 2.8f,
                cap = StrokeCap.Round
            )
        }

        // Papyrus Scroll (Volumen) held horizontally across the chest
        val scrollW = width * 0.50f
        val scrollH = height * 0.17f
        val scrollX = baseCenter.x - scrollW / 2f
        val scrollY = pedestalTop - bodyH * 0.38f
        val scrollColor = if (isWhite) Color(0xFFF7F1E1) else Color(0xFF9E8F7C)

        drawScope.drawRoundRect(
            color = scrollColor,
            topLeft = Offset(scrollX, scrollY),
            size = Size(scrollW, scrollH),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // Golden wooden spindle knobs (Umbilici)
        drawScope.drawCircle(color = GoldPrimary, radius = 5f, center = Offset(scrollX, scrollY + scrollH / 2f))
        drawScope.drawCircle(color = GoldPrimary, radius = 5f, center = Offset(scrollX + scrollW, scrollY + scrollH / 2f))
        // Latin philosophical text lines
        for (l in 1..3) {
            val ly = scrollY + scrollH * (l / 4f)
            drawScope.drawLine(
                color = if (isWhite) MarbleDark else BronzeDark,
                start = Offset(scrollX + 10f, ly),
                end = Offset(scrollX + scrollW - 10f, ly),
                strokeWidth = 2f
            )
        }

        // Head & Majestic Philosopher's Beard
        val headRadius = width * 0.20f
        val headCenter = Offset(baseCenter.x, robeTop - headRadius * 0.85f)

        // Head
        drawScope.drawCircle(
            brush = getVolumetricBrush(isWhite, headCenter.x - headRadius, headCenter.x + headRadius),
            radius = headRadius,
            center = headCenter
        )

        // Sculpted curly beard (Marcus Aurelius / Socrates style)
        val beardPath = Path().apply {
            moveTo(headCenter.x - headRadius * 0.82f, headCenter.y + headRadius * 0.2f)
            cubicTo(
                headCenter.x - headRadius * 0.65f, headCenter.y + headRadius * 1.9f,
                headCenter.x + headRadius * 0.65f, headCenter.y + headRadius * 1.9f,
                headCenter.x + headRadius * 0.82f, headCenter.y + headRadius * 0.2f
            )
            close()
        }
        drawScope.drawPath(beardPath, if (isWhite) MarbleLight else PatinaMid)

        // Beard curly locks texture lines
        val textureColor = if (isWhite) MarbleDark else PatinaBright
        drawScope.drawLine(
            color = textureColor,
            start = Offset(headCenter.x - 7f, headCenter.y + headRadius * 0.4f),
            end = Offset(headCenter.x - 4f, headCenter.y + headRadius * 1.6f),
            strokeWidth = 2.4f,
            cap = StrokeCap.Round
        )
        drawScope.drawLine(
            color = textureColor,
            start = Offset(headCenter.x + 7f, headCenter.y + headRadius * 0.4f),
            end = Offset(headCenter.x + 4f, headCenter.y + headRadius * 1.6f),
            strokeWidth = 2.4f,
            cap = StrokeCap.Round
        )
    }

    // 5. DAME : DÉESSE DRAPÉE / AUGUSTA VICTORIA (Graceful Goddess with Stephane Diadem)
    private fun drawRealisticGoddess(
        drawScope: DrawScope,
        baseCenter: Offset,
        width: Float,
        height: Float,
        isWhite: Boolean
    ) {
        val pedestalTop = drawMoldedPedestal(drawScope, baseCenter, width, height, isWhite)
        val bodyH = height * 0.94f
        val topY = pedestalTop - bodyH

        val dressW = width * 0.64f
        val dressTop = pedestalTop - bodyH * 0.70f

        // Draped Stola & Palla with elegant contrapposto curve
        val dressPath = Path().apply {
            moveTo(baseCenter.x - dressW * 0.48f, pedestalTop)
            cubicTo(
                baseCenter.x - dressW * 0.58f, dressTop + bodyH * 0.35f,
                baseCenter.x - dressW * 0.38f, dressTop,
                baseCenter.x - dressW * 0.18f, dressTop
            )
            lineTo(baseCenter.x + dressW * 0.18f, dressTop)
            cubicTo(
                baseCenter.x + dressW * 0.38f, dressTop,
                baseCenter.x + dressW * 0.58f, dressTop + bodyH * 0.35f,
                baseCenter.x + dressW * 0.48f, pedestalTop
            )
            close()
        }
        drawScope.drawPath(
            dressPath,
            getVolumetricBrush(isWhite, baseCenter.x - dressW / 2f, baseCenter.x + dressW / 2f)
        )

        // Fine pleating vertical shaded drapery lines
        val foldCount = 6
        for (i in 1..foldCount) {
            val fx = baseCenter.x - dressW * 0.42f + dressW * (i / (foldCount + 1f))
            drawScope.drawLine(
                color = if (isWhite) MarbleShadow.copy(alpha = 0.55f) else BronzeDeepShadow.copy(alpha = 0.65f),
                start = Offset(fx, dressTop + 12f),
                end = Offset(fx + 2f, pedestalTop - 4f),
                strokeWidth = 2.4f,
                cap = StrokeCap.Round
            )
        }

        // Golden Victory Patera / Libation Bowl held in hand
        val urnCenter = Offset(baseCenter.x + width * 0.28f, dressTop + bodyH * 0.28f)
        drawScope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(GoldSpecular, GoldPrimary, GoldShadow),
                center = urnCenter,
                radius = width * 0.14f
            ),
            topLeft = Offset(urnCenter.x - width * 0.11f, urnCenter.y - width * 0.07f),
            size = Size(width * 0.22f, width * 0.14f)
        )

        // Head and Grecian Braided Coiffure
        val headRadius = width * 0.19f
        val headCenter = Offset(baseCenter.x, dressTop - headRadius * 0.88f)

        drawScope.drawCircle(
            brush = getVolumetricBrush(isWhite, headCenter.x - headRadius, headCenter.x + headRadius),
            radius = headRadius,
            center = headCenter
        )

        // Golden Stephane (Imperial Diadem / High Crown)
        val diademPath = Path().apply {
            moveTo(headCenter.x - headRadius * 1.20f, headCenter.y - headRadius * 0.4f)
            lineTo(headCenter.x, headCenter.y - headRadius * 1.45f)
            lineTo(headCenter.x + headRadius * 1.20f, headCenter.y - headRadius * 0.4f)
            close()
        }
        drawScope.drawPath(
            diademPath,
            Brush.verticalGradient(listOf(GoldSpecular, GoldPrimary, GoldShadow))
        )
        // Ruby gem cabochon in crown
        drawScope.drawCircle(
            color = Color(0xFFD32F2F),
            radius = 4f,
            center = Offset(headCenter.x, headCenter.y - headRadius * 0.75f)
        )
    }

    // 6. ROI : EMPEREUR ROMAIN / IMPERATOR CAESAR (Regal Caesar with Laurel & Aquila)
    private fun drawRealisticEmperor(
        drawScope: DrawScope,
        baseCenter: Offset,
        width: Float,
        height: Float,
        isWhite: Boolean
    ) {
        val pedestalTop = drawMoldedPedestal(drawScope, baseCenter, width, height, isWhite)
        val bodyH = height * 0.98f
        val topY = pedestalTop - bodyH

        val togaW = width * 0.70f
        val togaTop = pedestalTop - bodyH * 0.72f

        // Imperial Paludamentum / Senatorial Toga
        val togaPath = Path().apply {
            moveTo(baseCenter.x - togaW * 0.48f, pedestalTop)
            cubicTo(
                baseCenter.x - togaW * 0.60f, togaTop + bodyH * 0.35f,
                baseCenter.x - togaW * 0.42f, togaTop,
                baseCenter.x - togaW * 0.22f, togaTop
            )
            lineTo(baseCenter.x + togaW * 0.22f, togaTop)
            cubicTo(
                baseCenter.x + togaW * 0.42f, togaTop,
                baseCenter.x + togaW * 0.60f, togaTop + bodyH * 0.35f,
                baseCenter.x + togaW * 0.48f, pedestalTop
            )
            close()
        }
        drawScope.drawPath(
            togaPath,
            getVolumetricBrush(isWhite, baseCenter.x - togaW / 2f, baseCenter.x + togaW / 2f)
        )

        // Diagonal Sinus drapery fold across the chest
        drawScope.drawArc(
            color = if (isWhite) MarbleShadow else BronzeDeepShadow,
            startAngle = 10f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(baseCenter.x - togaW * 0.42f, togaTop + 6f),
            size = Size(togaW * 0.80f, bodyH * 0.38f),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        // Golden Fibula Brooch with Gorgoneion / Imperial Emblem
        val fibulaCenter = Offset(baseCenter.x - togaW * 0.22f, togaTop + 8f)
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GoldSpecular, GoldPrimary, GoldShadow),
                center = fibulaCenter,
                radius = width * 0.09f
            ),
            radius = width * 0.075f,
            center = fibulaCenter
        )

        // Imperial Aquila Scepter on left
        val scepterX = baseCenter.x + width * 0.38f
        val scepterTopY = topY - height * 0.08f
        drawScope.drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(GoldSpecular, GoldPrimary, GoldDark),
                startY = scepterTopY,
                endY = pedestalTop
            ),
            start = Offset(scepterX, scepterTopY),
            end = Offset(scepterX, pedestalTop),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        // Roman Golden Eagle (Aquila) with spread wings atop globe
        val eagleCenter = Offset(scepterX, scepterTopY)
        drawScope.drawCircle(color = GoldPrimary, radius = 5f, center = eagleCenter)
        val eaglePath = Path().apply {
            moveTo(eagleCenter.x, eagleCenter.y - 16f)
            lineTo(eagleCenter.x - 14f, eagleCenter.y - 5f)
            lineTo(eagleCenter.x - 6f, eagleCenter.y)
            lineTo(eagleCenter.x + 6f, eagleCenter.y)
            lineTo(eagleCenter.x + 14f, eagleCenter.y - 5f)
            close()
        }
        drawScope.drawPath(eaglePath, GoldSpecular)

        // Head of Caesar Augustus
        val headRadius = width * 0.21f
        val headCenter = Offset(baseCenter.x, togaTop - headRadius * 0.90f)

        drawScope.drawCircle(
            brush = getVolumetricBrush(isWhite, headCenter.x - headRadius, headCenter.x + headRadius),
            radius = headRadius,
            center = headCenter
        )

        // Golden Laurel Wreath (Corona Triumphalis) encircling Caesar's brow
        val laurelRadius = headRadius * 1.12f
        drawScope.drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(GoldSpecular, GoldPrimary, GoldDark, GoldPrimary, GoldSpecular),
                center = headCenter
            ),
            startAngle = 175f,
            sweepAngle = 190f,
            useCenter = false,
            topLeft = Offset(headCenter.x - laurelRadius, headCenter.y - laurelRadius * 0.72f),
            size = Size(laurelRadius * 2f, laurelRadius * 1.45f),
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )
        // Individual Gilded Laurel Leaves
        for (angle in listOf(185f, 215f, 250f, 290f, 325f, 355f)) {
            val rad = Math.toRadians(angle.toDouble())
            val lx = headCenter.x + (laurelRadius * cos(rad)).toFloat()
            val ly = headCenter.y + (laurelRadius * 0.72f * sin(rad)).toFloat()
            drawScope.drawCircle(
                color = GoldSpecular,
                radius = 4.5f,
                center = Offset(lx, ly)
            )
        }
    }
}
