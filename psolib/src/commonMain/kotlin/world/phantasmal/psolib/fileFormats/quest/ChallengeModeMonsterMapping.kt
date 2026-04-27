package world.phantasmal.psolib.fileFormats.quest

/**
 * Maps challenge mode monster type index to the actual type ID (skin) value used in the BB client.
 * Based on the ep1cm_uid_to_monster_skin table at address 0x980760 in the BB client.
 *
 * Index into this array gives you the type ID that can be used to find the corresponding NpcType.
 */
val CHALLENGE_MODE_MONSTER_TYPE_IDS = intArrayOf(
    0x44, // [ 0] Booma
    0x43, // [ 1] Savage Wolf
    0x41, // [ 2] Rag Rappy
    0x42, // [ 3] Monest
    0x40, // [ 4] Hildebear
    0x60, // [ 5] Grass Assassin
    0x61, // [ 6] Poison Lily
    0x62, // [ 7] Nano Dragon
    0x63, // [ 8] Evil Shark
    0x64, // [ 9] Pofuilly Slime
    0x65, // [10] Pan Arms
    0x80, // [11] Dubchic
    0x81, // [12] Garanz
    0x82, // [13] Sinow Beat
    0x83, // [14] Canadine
    0x84, // [15] Canane
    0x85, // [16] Dubswitch
    0xA0, // [17] Delsaber
    0xA1, // [18] Chaos Sorcerer
    0xA2, // [19] Dark Gunner
    0xA3, // [20] Unknown (no NpcType for typeId 0xA3)
    0xA4, // [21] Chaos Bringer
    0xA5, // [22] Dark Belra
    0xA6, // [23] Dimenian
    0xA7, // [24] Bulclaw
    0xA8, // [25] Claw
    0xD4, // [26] Sinow Berill
    0xD5, // [27] Merillia
    0xD6, // [28] Mericarol
    0xD7, // [29] Ul Gibbon
    0xD8, // [30] Gibbles
    0xD9, // [31] Gee
    0xDA, // [32] Gi Gue
    0xDB, // [33] Deldepth
    0xDC, // [34] Delbiter
    0xDD, // [35] Dolmolm
    0xDE, // [36] Morfos
    0xDF, // [37] Recobox
    0xE0, // [38] Sinow Zoa
    0xE0, // [39] Sinow Zoa (duplicate)
    0xE1, // [40] Ill Gill
    0x00, // [41] (end marker)
)

/**
 * Finds the NpcType that corresponds to a challenge mode monster type index.
 * Returns null if the index is out of range or no matching NpcType is found.
 */
fun getNpcTypeForChallengeMonsterIndex(index: Int): NpcType? {
    if (index < 0 || index >= CHALLENGE_MODE_MONSTER_TYPE_IDS.size) {
        return null
    }

    val typeId = CHALLENGE_MODE_MONSTER_TYPE_IDS[index]
    if (typeId == 0) return null  // End marker

    // Find NpcType with matching typeId
    // For monsters with multiple skin variants, return the base version (skin=0 or special=false)
    return NpcType.entries.find { npcType ->
        npcType.typeId == typeId &&
                npcType.enemy &&
                (npcType.skin == 0 && npcType.special != true)
    } ?: NpcType.entries.find { npcType ->
        // Fallback: just match typeId
        npcType.typeId == typeId && npcType.enemy
    }
}
