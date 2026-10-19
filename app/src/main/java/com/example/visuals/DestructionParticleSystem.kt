package com.example.visuals

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.model.PieceColor
import com.example.model.Position
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class ShardParticle(
    var x: Float,
    var y: Float,
    var z: Float, // Height above ground
    var vx: Float,
    var vy: Float,
    var vz: Float,
    var rotation: Float,
    var vRot: Float,
    val size: Float,
    val color: Color,
    val secondaryColor: Color,
    val vertexOffsets: List<Offset>,
    var life: Float = 1.0f,
    val decay: Float = Random.nextFloat() * 0.4f + 0.5f // life decay per second
)

class DestructionEffect(
    val squarePosition: Position,
    val centerOffset: Offset,
    val pieceColor: PieceColor,
    val durationSeconds: Float = 1.6f
) {
    var elapsed: Float = 0f
    val isFinished: Boolean get() = elapsed >= durationSeconds

    private val shards = mutableListOf<ShardParticle>()
    private val dustPuffs = mutableListOf<DustPuff>()

    init {
        val shardCount = Random.nextInt(36, 48)
        val isWhite = pieceColor == PieceColor.WHITE

        val palette = if (isWhite) {
            listOf(
                Color(0xFFFAF8F5), // White Carrara marble
                Color(0xFFEDE7DF), // Ivory marble
                Color(0xFFD6CEBE), // Veined gray-cream
                Color(0xFFB0A694), // Deep stone shadow
                Color(0xFFFFD54F)  // Gold inlay fleck
            )
        } else {
            listOf(
                Color(0xFF2B2824), // Antique dark bronze
                Color(0xFF3E3831), // Weathered metal
                Color(0xFF2D7264), // Verdigris patina (emerald/copper)
                Color(0xFF3FA894), // Bright oxidized copper
                Color(0xFFD4AF37)  // Burnished gold-bronze highlight
            )
        }

        for (i in 0 until shardCount) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 260f + 140f
            val upwardSpeed = Random.nextFloat() * 320f + 180f
            val baseSize = Random.nextFloat() * 14f + 8f

            // Generate irregular 4-5 vertex polygon for realistic shattered stone/bronze
            val numVertices = Random.nextInt(3, 6)
            val vertices = mutableListOf<Offset>()
            for (v in 0 until numVertices) {
                val vAngle = (v.toFloat() / numVertices) * 2f * Math.PI.toFloat() + (Random.nextFloat() - 0.5f) * 0.4f
                val vRad = baseSize * (Random.nextFloat() * 0.6f + 0.7f)
                vertices.add(Offset(cos(vAngle) * vRad, sin(vAngle) * vRad))
            }

            val c1 = palette.random()
            val c2 = palette.random()

            shards.add(
                ShardParticle(
                    x = centerOffset.x + (Random.nextFloat() - 0.5f) * 20f,
                    y = centerOffset.y + (Random.nextFloat() - 0.5f) * 20f,
                    z = Random.nextFloat() * 30f + 10f,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed * 0.7f, // Perspective foreshortening
                    vz = upwardSpeed,
                    rotation = Random.nextFloat() * 360f,
                    vRot = (Random.nextFloat() - 0.5f) * 720f,
                    size = baseSize,
                    color = c1,
                    secondaryColor = c2,
                    vertexOffsets = vertices
                )
            )
        }

        // Dust puffs / smoke cloud
        val puffCount = 14
        for (i in 0 until puffCount) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val dist = Random.nextFloat() * 40f
            dustPuffs.add(
                DustPuff(
                    x = centerOffset.x + cos(angle) * dist,
                    y = centerOffset.y + sin(angle) * dist * 0.6f,
                    vx = cos(angle) * (Random.nextFloat() * 40f + 10f),
                    vy = sin(angle) * (Random.nextFloat() * 30f + 10f),
                    radius = Random.nextFloat() * 20f + 15f,
                    color = if (isWhite) Color(0xCCEDE7DF) else Color(0xBB3B5B53)
                )
            )
        }
    }

    fun update(deltaSeconds: Float) {
        elapsed += deltaSeconds
        val gravity = 700f

        for (shard in shards) {
            shard.x += shard.vx * deltaSeconds
            shard.y += shard.vy * deltaSeconds
            shard.z += shard.vz * deltaSeconds - 0.5f * gravity * deltaSeconds * deltaSeconds
            shard.vz -= gravity * deltaSeconds
            shard.rotation += shard.vRot * deltaSeconds

            // Ground bounce simulation
            if (shard.z < 0f) {
                shard.z = 0f
                shard.vz = -shard.vz * 0.4f // bounce dampening
                shard.vx *= 0.7f // friction
                shard.vy *= 0.7f
                shard.vRot *= 0.6f
            }

            shard.life = maxOf(0f, 1f - (elapsed / durationSeconds))
        }

        for (puff in dustPuffs) {
            puff.x += puff.vx * deltaSeconds
            puff.y += puff.vy * deltaSeconds
            puff.radius += deltaSeconds * 25f
            puff.alpha = maxOf(0f, 1f - (elapsed / (durationSeconds * 0.8f)))
        }
    }

    fun draw(drawScope: DrawScope) {
        // Draw expanding dust clouds first (behind shards)
        for (puff in dustPuffs) {
            if (puff.alpha > 0.02f) {
                drawScope.drawCircle(
                    color = puff.color.copy(alpha = puff.color.alpha * puff.alpha * 0.4f),
                    radius = puff.radius,
                    center = Offset(puff.x, puff.y)
                )
            }
        }

        // Draw shards
        for (shard in shards) {
            if (shard.life <= 0.01f) continue

            // 3D perspective shadow on ground
            val shadowCenter = Offset(shard.x, shard.y)
            drawScope.drawCircle(
                color = Color.Black.copy(alpha = 0.25f * shard.life * (1f - (shard.z / 300f).coerceIn(0f, 0.8f))),
                radius = shard.size * 0.6f,
                center = shadowCenter
            )

            // Piece shard in air: offset y by height z
            val drawCenter = Offset(shard.x, shard.y - shard.z)
            val path = Path()
            val rad = Math.toRadians(shard.rotation.toDouble())
            val cosR = cos(rad).toFloat()
            val sinR = sin(rad).toFloat()

            for ((index, offset) in shard.vertexOffsets.withIndex()) {
                val rx = offset.x * cosR - offset.y * sinR
                val ry = offset.x * sinR + offset.y * cosR
                val pt = Offset(drawCenter.x + rx, drawCenter.y + ry)
                if (index == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            path.close()

            // Main facet
            drawScope.drawPath(
                path = path,
                color = shard.color.copy(alpha = shard.life)
            )

            // Specular / metallic rim highlight on edge
            drawScope.drawCircle(
                color = shard.secondaryColor.copy(alpha = shard.life * 0.9f),
                radius = shard.size * 0.25f,
                center = drawCenter
            )
        }
    }

    private class DustPuff(
        var x: Float,
        var y: Float,
        val vx: Float,
        val vy: Float,
        var radius: Float,
        val color: Color,
        var alpha: Float = 1.0f
    )
}
