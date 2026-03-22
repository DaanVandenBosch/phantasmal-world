package world.phantasmal.psolib.compatibility

import world.phantasmal.psolib.fileFormats.quest.Version

/**
 * PSO version for compatibility checking.
 * More granular than [Version] to distinguish between DC V1 and V2.
 */
enum class PSOVersion(
    val verId: Int,
    val displayName: String,
) {
    DC_V1(0, "Phantasy Star Online DC V1"),
    DC_V2(1, "Phantasy Star Online DC V2"),
    PC(2, "Phantasy Star Online PC"),
    GC_EP12(3, "Phantasy Star Online GC Ep1&2"),
    BLUE_BURST(4, "Phantasy Star Online Blue Burst");

    companion object {
        fun fromId(id: Int): PSOVersion? = entries.firstOrNull { it.verId == id }

        /**
         * Convert from file format [Version] to compatibility [PSOVersion].
         * DC maps to DC_V2 by default since V1 is rare.
         */
        fun fromVersion(version: Version): PSOVersion = when (version) {
            Version.DC -> DC_V2
            Version.GC -> GC_EP12
            Version.PC -> PC
            Version.BB -> BLUE_BURST
        }
    }
}