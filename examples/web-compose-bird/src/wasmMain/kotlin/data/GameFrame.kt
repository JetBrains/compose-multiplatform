package data

data class GameFrame(
    val birdPos: Int,
    val tubes: List<Tube>,
    val isGameOver: Boolean,
    val isGameWon : Boolean,
    val score: Int,
)
