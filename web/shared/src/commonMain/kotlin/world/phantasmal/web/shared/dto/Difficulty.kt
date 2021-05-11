package world.phantasmal.web.shared.dto

import kotlinx.serialization.Serializable

@Serializable
enum class Difficulty {
    Normal,
    Hard,
    VHard,
    Ultimate;

    companion object {
        val VALUES: Array<Difficulty> = values()
        val VALUES_LIST: List<Difficulty> = VALUES.toList()
    }
}
