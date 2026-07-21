package world.phantasmal.psolib.battleparam

import world.phantasmal.psolib.asm.EnemyAttackData
import world.phantasmal.psolib.asm.EnemyMovementData
import world.phantasmal.psolib.asm.EnemyPhysicalData
import world.phantasmal.psolib.asm.EnemyResistData
import world.phantasmal.psolib.buffer.Buffer

/**
 * One PSOBB BattleParamEntry file.
 *
 * Layout per file (0xF600 = 62976 bytes total, matches newserv's
 * `BattleParamsIndex::Table` in `src/BattleParamsIndex.hh`):
 *   stats        [4 difficulties][96 slots]   = 4 * 96 * 0x24 = 0x3600 bytes  (offset 0x0000)
 *   attack_data  [4 difficulties][96 slots]   = 4 * 96 * 0x30 = 0x4800 bytes  (offset 0x3600)
 *   resist_data  [4 difficulties][96 slots]   = 4 * 96 * 0x20 = 0x3000 bytes  (offset 0x7E00)
 *   movement_data[4 difficulties][96 slots]   = 4 * 96 * 0x30 = 0x4800 bytes  (offset 0xAE00)
 *
 * Files are little-endian on every platform (per newserv comment).
 *
 * `set` identifies which of the 6 BB files this table came from.
 */
class BattleParamTable(val set: BattleParamSet, private val buf: Buffer) {

    init {
        require(buf.size >= FILE_SIZE) {
            "BattleParamEntry buffer too small: ${buf.size} < $FILE_SIZE"
        }
    }

    fun physical(difficulty: Int, slot: Int): EnemyPhysicalData =
        EnemyPhysicalData.readFrom(buf.slice(physicalOffset(difficulty, slot), EnemyPhysicalData.SIZE))

    fun attack(difficulty: Int, slot: Int): EnemyAttackData =
        EnemyAttackData.readFrom(buf.slice(attackOffset(difficulty, slot), EnemyAttackData.SIZE))

    fun resist(difficulty: Int, slot: Int): EnemyResistData =
        EnemyResistData.readFrom(buf.slice(resistOffset(difficulty, slot), EnemyResistData.SIZE))

    fun movement(difficulty: Int, slot: Int): EnemyMovementData =
        EnemyMovementData.readFrom(buf.slice(movementOffset(difficulty, slot), EnemyMovementData.SIZE))

    private fun physicalOffset(d: Int, s: Int): Int {
        checkIdx(d, s); return PHYSICAL_BASE + (d * SLOTS + s) * EnemyPhysicalData.SIZE
    }

    private fun attackOffset(d: Int, s: Int): Int {
        checkIdx(d, s); return ATTACK_BASE + (d * SLOTS + s) * EnemyAttackData.SIZE
    }

    private fun resistOffset(d: Int, s: Int): Int {
        checkIdx(d, s); return RESIST_BASE + (d * SLOTS + s) * EnemyResistData.SIZE
    }

    private fun movementOffset(d: Int, s: Int): Int {
        checkIdx(d, s); return MOVEMENT_BASE + (d * SLOTS + s) * EnemyMovementData.SIZE
    }

    private fun checkIdx(d: Int, s: Int) {
        require(d in 0 until DIFFICULTIES) { "difficulty out of range: $d" }
        require(s in 0 until SLOTS) { "slot out of range: $s" }
    }

    companion object {
        const val SLOTS = 96
        const val DIFFICULTIES = 4

        const val PHYSICAL_BASE = 0
        const val PHYSICAL_TOTAL = DIFFICULTIES * SLOTS * EnemyPhysicalData.SIZE   // 13824
        const val ATTACK_BASE = PHYSICAL_BASE + PHYSICAL_TOTAL
        const val ATTACK_TOTAL = DIFFICULTIES * SLOTS * EnemyAttackData.SIZE       // 18432
        const val RESIST_BASE = ATTACK_BASE + ATTACK_TOTAL
        const val RESIST_TOTAL = DIFFICULTIES * SLOTS * EnemyResistData.SIZE       // 12288
        const val MOVEMENT_BASE = RESIST_BASE + RESIST_TOTAL
        const val MOVEMENT_TOTAL = DIFFICULTIES * SLOTS * EnemyMovementData.SIZE   // 18432
        const val FILE_SIZE = MOVEMENT_BASE + MOVEMENT_TOTAL                       // 62976
    }
}
