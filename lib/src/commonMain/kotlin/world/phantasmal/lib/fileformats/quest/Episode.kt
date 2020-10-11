package world.phantasmal.lib.fileformats.quest

enum class Episode {
    I,
    II,
    IV;

    companion object {
        fun fromInt(episode: Int) = when (episode) {
            1 -> I
            2 -> II
            4 -> IV
            else -> error("$episode is invalid.")
        }
    }
}
