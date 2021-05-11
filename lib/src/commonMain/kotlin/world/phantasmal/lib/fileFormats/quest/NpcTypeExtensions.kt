package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.Episode

private val EP_AND_NAME_TO_NPC_TYPE: Map<Pair<String, Episode>, NpcType> =
    mutableMapOf<Pair<String, Episode>, NpcType>().also { map ->
        for (npcType in NpcType.VALUES) {
            if (npcType.episode != null) {
                map[Pair(npcType.simpleName, npcType.episode)] = npcType
                map[Pair(npcType.ultimateName, npcType.episode)] = npcType
            }
        }
    }

/**
 * Uniquely identifies an NPC. Tries to match on [NpcType.simpleName] and [NpcType.ultimateName].
 */
fun NpcType.Companion.fromNameAndEpisode(name: String, episode: Episode): NpcType? =
    EP_AND_NAME_TO_NPC_TYPE[Pair(name, episode)]
