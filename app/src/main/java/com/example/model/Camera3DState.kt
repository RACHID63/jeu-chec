package com.example.model

enum class Camera3DPreset(
    val title: String,
    val subtitle: String,
    val iconName: String,
    val pitch: Float,
    val yaw: Float
) {
    WHITE_CAMP(
        title = "Marbre (0°)",
        subtitle = "Vue Camp Blanc",
        iconName = "white_front",
        pitch = 38f,
        yaw = 0f
    ),
    DIAGONAL_45(
        title = "Diagonale (45°)",
        subtitle = "Perspective Isométrique",
        iconName = "diagonal",
        pitch = 45f,
        yaw = 45f
    ),
    BLACK_CAMP(
        title = "Bronze (180°)",
        subtitle = "Vue Camp Noir",
        iconName = "black_front",
        pitch = 38f,
        yaw = 180f
    ),
    ZENITH(
        title = "Zénith (90°)",
        subtitle = "Vue Aérienne Tactique",
        iconName = "zenith",
        pitch = 0f,
        yaw = 0f
    )
}

data class Camera3DState(
    val pitch: Float = 38f, // 0 to 65 degrees
    val yaw: Float = 0f,    // 0 to 360 degrees
    val zoom: Float = 1.0f,  // 0.8 to 1.4
    val isOrbitModeActive: Boolean = false,
    val activePreset: Camera3DPreset? = Camera3DPreset.WHITE_CAMP
)
