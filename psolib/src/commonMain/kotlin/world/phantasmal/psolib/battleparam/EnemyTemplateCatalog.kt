package world.phantasmal.psolib.battleparam

import world.phantasmal.psolib.Episode

/**
 * Catalog of enemy template entries per episode.
 *
 * For each episode this exposes:
 *   - the human-readable enemy name list
 *   - the four BattleParamEntry slot index tables (physical / resist /
 *     attack / movement)
 *
 * A slot index of 0xFF means "no entry" — the enemy has no corresponding
 * row in that particular section.
 */
object EnemyTemplateCatalog {

    const val NO_SLOT: Int = 0xFF

    fun namesFor(episode: Episode): List<String> = when (episode) {
        Episode.I  -> EP1_NAMES
        Episode.II -> EP2_NAMES
        Episode.IV -> EP4_NAMES
    }

    fun physIdsFor(episode: Episode): IntArray = when (episode) {
        Episode.I  -> EP1_PHYS
        Episode.II -> EP2_PHYS
        Episode.IV -> EP4_PHYS
    }

    fun resistIdsFor(episode: Episode): IntArray = when (episode) {
        Episode.I  -> EP1_RESIST
        Episode.II -> EP2_RESIST
        Episode.IV -> EP4_RESIST
    }

    fun attackIdsFor(episode: Episode): IntArray = when (episode) {
        Episode.I  -> EP1_ATTACK
        Episode.II -> EP2_ATTACK
        Episode.IV -> EP4_ATTACK
    }

    fun movementIdsFor(episode: Episode): IntArray = when (episode) {
        Episode.I  -> EP1_MOVEMENT
        Episode.II -> EP2_MOVEMENT
        Episode.IV -> EP4_MOVEMENT
    }

    /**
     * Variants of the "attack" slot for enemies that have multiple attack
     * forms (e.g. Hildebear's tech vs. jump). The first entry is always
     * the standard "Attack" using the regular `attackIds` slot.
     *
     * Returns `null` if the enemy has no extra variants.
     */
    fun attackVariants(episode: Episode, enemyName: String): List<AttackVariant>? {
        val list = mutableListOf<AttackVariant>()

        // Standard attack label varies for ranged-only enemies.
        val baseLabel = when {
            episode == Episode.I  && enemyName == EP1_NAMES[14] -> "Attack (Arm)"   // Dark Belra
            episode == Episode.II && enemyName == EP2_NAMES[42] -> "Attack (Laser)" // Morfos
            else -> "Attack"
        }
        list += AttackVariant(baseLabel, slotOverride = null)

        when (episode) {
            Episode.I -> when (enemyName) {
                EP1_NAMES[14] -> list += AttackVariant("Attack (Swipe)", 0x13)
                EP1_NAMES[46] -> { // Hildebear
                    list += AttackVariant("Attack (Tech)", 0x49)
                    list += AttackVariant("Attack (Jump)", 0x4A)
                }
                EP1_NAMES[47] -> { // Hildeblue
                    list += AttackVariant("Attack (Tech)", 0x4C)
                    list += AttackVariant("Attack (Jump)", 0x4D)
                }
                EP1_NAMES[51] -> { // Grass Assassin
                    list += AttackVariant("Attack (Charge)", 0x52)
                    list += AttackVariant("Attack (Freeze)", 0x53)
                }
                EP1_NAMES[55] -> { // Delsaber
                    list += AttackVariant("Attack (Shield)", 0x58)
                    list += AttackVariant("Attack (Jump)",   0x59)
                }
            }
            Episode.II -> when (enemyName) {
                EP2_NAMES[15] -> list += AttackVariant("Attack (Spear)", 0x11) // Barba Ray
                EP2_NAMES[29] -> { // Ill Gill
                    list += AttackVariant("Attack (Scythe)", 0x27)
                    list += AttackVariant("Attack (Snare)",  0x28)
                    list += AttackVariant("Attack (Charge)", 0x29)
                }
                EP2_NAMES[41] -> { // Gibbles
                    list += AttackVariant("Attack (Pound)", 0x3E)
                    list += AttackVariant("Attack (Jump)",  0x3F)
                }
                EP2_NAMES[42] -> list += AttackVariant("Attack (Push)", 0x50) // Morfos
            }
            Episode.IV -> when (enemyName) {
                EP4_NAMES[1]  -> list += AttackVariant("Attack (Charge)", 0x02) // Ze Boota
                EP4_NAMES[2]  -> list += AttackVariant("Attack (Foie)",   0x04) // Ba Boota
                EP4_NAMES[7]  -> { // Astark
                    list += AttackVariant("Attack (Poison)", 0x0B)
                    list += AttackVariant("Attack (Jump)",   0x0C)
                }
                EP4_NAMES[12] -> list += AttackVariant("Attack (Teleport)", 0x14) // Goran
                EP4_NAMES[13] -> list += AttackVariant("Attack (Teleport)", 0x15) // Pyro Goran
                EP4_NAMES[14] -> list += AttackVariant("Attack (Teleport)", 0x16) // Goran Detonator
            }
        }

        return if (list.size > 1) list else null
    }

    data class AttackVariant(
        val label: String,
        /** Direct BattleParamEntry slot index, or `null` to use the enemy's regular `attackIds` entry. */
        val slotOverride: Int?,
    )

    // ---- Episode 1 ---------------------------------------------------------

    private val EP1_NAMES: List<String> = listOf(
        "Mothmant", "Monest", "Savage Wolf", "Barbarous Wolf", "Poison Lily",
        "Nar Lily", "Sinow Beat", "Canadine (Solo)", "Canadine (Ring)", "Canane",
        "Chaos Sorcerer", "Bee R", "Bee L", "Chaos Bringer", "Dark Belra",
        "De Rol Le (Body)", "De Rol Le (Shell)", "De Rol Le (Tail Mine)", "Dragon", "Sinow Gold",
        "Rag Rappy", "Al Rappy", "Nano Dragon", "Dubchic", "Gillchic",
        "Garanz", "Dark Gunner", "Bulclaw", "Claw", "Vol Opt (Phase 1 Core)",
        "Vol Opt (Phase 1 Pillar)", "Vol Opt (Phase 1 Monitor)", "Vol Opt (Phase 1 Spire)",
        "Vol Opt (Phase 2 Core)", "Vol Opt (Phase 2 Prison)", "Pofuilly Slime", "Pan Arms", "Hidoom",
        "Migium", "Pouilly Slime", "Darvant (Mine Field)", "Dark Falz (Phase 1)", "Dark Falz (Phase 2)",
        "Dark Falz (Phase 3)", "Darvant (Phase 2 Ult)", "Dubwitch", "Hildebear", "Hildeblue",
        "Booma", "Gobooma", "Gigobooma", "Grass Assassin", "Evil Shark",
        "Pal Shark", "Guil Shark", "Delsaber", "Dimenian", "La Dimenian",
        "So Dimenian",
    )

    private val EP1_PHYS = intArrayOf(
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
        0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13,
        0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x21,
        0x22, 0x23, 0x24, 0x25, 0x26, 0x30, 0x31, 0x32, 0x33, 0x34,
        0x35, 0x36, 0x37, 0x38, 0x39, 0x48, 0x49, 0x4A, 0x4B, 0x4C,
        0x4D, 0x4E, 0x4F, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55,
    )

    private val EP1_RESIST = intArrayOf(
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
        0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13,
        0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x21,
        0x22, 0x23, 0x24, 0x25, 0x26, 0x30, 0x31, 0x32, 0x33, 0x34,
        0x35, 0x36, 0x37, 0x38, 0x39, 0xFF, 0x48, 0x49, 0x4A, 0x4B,
        0x4C, 0x4D, 0x4E, 0x4F, 0x50, 0x51, 0x52, 0x53, 0x54,
    )

    private val EP1_ATTACK = intArrayOf(
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E,
        0x0F, 0x10, 0x11, 0x12, 0x47, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x21,
        0x22, 0x23, 0x24, 0x25, 0x26, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
        0xFF, 0x48, 0x4B, 0x4E, 0x4F, 0x50, 0x51, 0x54, 0x55, 0x56, 0x57, 0x5A, 0x5B, 0x5C,
    )

    private val EP1_MOVEMENT = intArrayOf(
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E,
        0x0F, 0xFF, 0x11, 0x12, 0x10, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x21,
        0x22, 0x23, 0x24, 0x25, 0x26, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
        0xFF, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50, 0x51, 0x52, 0x53, 0x54,
    )

    // ---- Episode 2 ---------------------------------------------------------

    private val EP2_NAMES: List<String> = listOf(
        "Mothmant", "Monest", "Savage Wolf", "Barbarous Wolf", "Poison Lily",
        "Nar Lily", "Sinow Berill", "Gee", "Pig Ray", "Ul Ray",
        "Chaos Sorceror", "Bee R", "Bee L", "Delbiter", "Dark Belra",
        "Barba Ray", "Barba Ray (Shell)", "Gol Dragon", "Sinow Spigell", "Rag Rappy",
        "Love Rappy", "Gi Gue", "Dubchic", "Gillchic", "Garanz",
        "Gal Gryphon", "Epsilon", "Epsigard", "Del Lily", "Ill Gill",
        "Olga Flow (Phase 1)", "Olga Flow (Phase 2)", "Gael", "Giel", "Deldepth",
        "Pan Arms", "Hidoom", "Migium", "Mericarol", "Ul Gibbon",
        "Zol Gibbon", "Gibbles", "Morfos", "Recobox", "Recon",
        "Sinow Zoa", "Sinow Zele", "Merikle", "Mericus", "Dubwitch",
        "Hildebear", "Hildeblue", "Merillia", "Meriltas", "Grass Assassin",
        "Dolmolm", "Dolmdarl", "Delsaber", "Dimenian", "La Dimenian",
        "So Dimenian",
    )

    private val EP2_PHYS = intArrayOf(
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
        0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x12, 0x13, 0x18,
        0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x23, 0x24, 0x25, 0x26,
        0x2B, 0x2C, 0x2D, 0x2E, 0x30, 0x31, 0x32, 0x33, 0x3A, 0x3B,
        0x3C, 0x3D, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x48,
        0x49, 0x4A, 0x4B, 0x4C, 0x4E, 0x4F, 0x50, 0x52, 0x53, 0x54,
        0x55,
    )

    private val EP2_RESIST = intArrayOf(
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
        0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x12, 0x13, 0x18,
        0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x23, 0x24, 0x25, 0x26,
        0x2B, 0x2C, 0x2D, 0x2E, 0x30, 0x31, 0x32, 0x33, 0x3A, 0x3B,
        0x3C, 0x3D, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0xFF,
        0x48, 0x49, 0x4A, 0x4B, 0x4D, 0x4E, 0x4F, 0x51, 0x52, 0x53,
        0x54,
    )

    private val EP2_ATTACK = intArrayOf(
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E,
        0x0F, 0x10, 0x12, 0x47, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x23, 0x24, 0x25, 0x26,
        0x2B, 0x2C, 0x2D, 0x2E, 0x30, 0x31, 0x32, 0x33, 0x3A, 0x3B, 0x3C, 0x3D, 0x40, 0x41, 0x42,
        0x43, 0x44, 0x45, 0x46, 0xFF, 0x48, 0x4B, 0x4E, 0x4F, 0x51, 0x54, 0x55, 0x57, 0x5A, 0x5B,
        0x5C,
    )

    private val EP2_MOVEMENT = intArrayOf(
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E,
        0x0F, 0xFF, 0x12, 0x10, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x23, 0x24, 0x25, 0x26,
        0x2B, 0x2C, 0x2D, 0x2E, 0x30, 0x31, 0x32, 0x33, 0x3A, 0x3B, 0x3C, 0x3D, 0x40, 0x41, 0x42,
        0x43, 0x44, 0x45, 0x46, 0xFF, 0x48, 0x49, 0x4A, 0x4B, 0x4D, 0x4E, 0x4F, 0x51, 0x52, 0x53,
        0x54,
    )

    // ---- Episode 4 ---------------------------------------------------------

    private val EP4_NAMES: List<String> = listOf(
        "Boota", "Ze Boota", "Ba Boota", "Sand Rappy (Crater)", "Del Rappy (Crater)",
        "Zu (Crater)", "Pazuzu (Crater)", "Astark", "Satellite Lizard (Crater)", "Yowie (Crater)",
        "Dorphon", "Dorphon Eclair", "Goran", "Pyro Goran", "Goran Detonator",
        "Sand Rappy (Desert)", "Del Rappy (Desert)", "Merissa A", "Merissa AA", "Zu (Desert)",
        "Pazuzu (Desert)", "Satellite Lizard (Desert)", "Yowie (Desert)", "Girtablulu",
        "Saint-Milion (Phase 1)", "Spinner (Saint-Milion 1)", "Saint-Milion (Phase 2)", "Spinner (Saint-Milion 2)",
        "Shambertin (Phase 1)", "Spinner (Shambertin 1)", "Shambertin (Phase 2)", "Spinner (Shambertin 2)",
        "Kondrieu (Phase 1)", "Spinner (Kondrieu 1)", "Kondrieu (Phase 2)", "Spinner (Kondrieu 2)",
    )

    private val EP4_PHYS = intArrayOf(
        0x00, 0x01, 0x03, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0D, 0x0E,
        0x0F, 0x10, 0x11, 0x12, 0x13, 0x17, 0x18, 0x19, 0x1A, 0x1B,
        0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25,
        0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B,
    )

    private val EP4_RESIST = EP4_PHYS // same slot mapping as physical

    private val EP4_ATTACK = intArrayOf(
        0x00, 0x01, 0x03, 0x05, 0x06, 0x07, 0x08, 0x0A, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13,
        0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25,
        0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B,
    )

    private val EP4_MOVEMENT = EP4_PHYS // same slot mapping as physical

    init {
        check(EP1_NAMES.size == 59 && EP1_PHYS.size == 59 && EP1_RESIST.size == 59 &&
              EP1_ATTACK.size == 59 && EP1_MOVEMENT.size == 59) { "Ep1 catalog size mismatch" }
        check(EP2_NAMES.size == 61 && EP2_PHYS.size == 61 && EP2_RESIST.size == 61 &&
              EP2_ATTACK.size == 61 && EP2_MOVEMENT.size == 61) { "Ep2 catalog size mismatch" }
        check(EP4_NAMES.size == 36 && EP4_PHYS.size == 36 && EP4_RESIST.size == 36 &&
              EP4_ATTACK.size == 36 && EP4_MOVEMENT.size == 36) { "Ep4 catalog size mismatch" }
    }
}
