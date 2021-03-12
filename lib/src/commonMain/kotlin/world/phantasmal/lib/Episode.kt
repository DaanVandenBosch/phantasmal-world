package world.phantasmal.lib

enum class Episode {
    I,
    II,
    IV;

    fun toInt(): Int = when(this) {
        I -> 1
        II -> 2
        IV -> 4
    }

    companion object {
        fun fromInt(episode: Int) = when (episode) {
            1 -> I
            2 -> II
            4 -> IV
            else -> error("$episode is invalid.")
        }
    }
}
