package world.phantasmal.psolib.battleparam

import world.phantasmal.psolib.Episode

/**
 * Identifies one of the 6 standard PSOBB BattleParamEntry files.
 *
 * Filename ↔ episode mapping comes from newserv (`src/BattleParamsIndex.hh`),
 * which loads these files in `system/blueburst/`. Note that despite the
 * `_lab` suffix, those are the **Episode 2** tables, not Ep1.
 */
enum class BattleParamSet(
    val episode: Episode,
    val online: Boolean,
    val fileName: String,
    val displayName: String,
) {
    Ep1Offline(Episode.I,  online = false, "BattleParamEntry.dat",        "Ep1 Offline"),
    Ep1Online (Episode.I,  online = true,  "BattleParamEntry_on.dat",     "Ep1 Online"),
    Ep2Offline(Episode.II, online = false, "BattleParamEntry_lab.dat",    "Ep2 Offline"),
    Ep2Online (Episode.II, online = true,  "BattleParamEntry_lab_on.dat", "Ep2 Online"),
    Ep4Offline(Episode.IV, online = false, "BattleParamEntry_ep4.dat",    "Ep4 Offline"),
    Ep4Online (Episode.IV, online = true,  "BattleParamEntry_ep4_on.dat", "Ep4 Online");
}

/** PSO difficulty levels, in the order they appear inside a BattleParamEntry file. */
enum class BattleParamDifficulty(val displayName: String) {
    Normal("Normal"),
    Hard("Hard"),
    VeryHard("V.Hard"),
    Ultimate("Ultimate"),
}
