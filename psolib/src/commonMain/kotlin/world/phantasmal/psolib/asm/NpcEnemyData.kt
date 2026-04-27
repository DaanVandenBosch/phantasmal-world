package world.phantasmal.psolib.asm

import world.phantasmal.psolib.buffer.Buffer

/**
 * PlayerVisualConfig (0x50 = 80 bytes).
 * Used by get_npc_data / load_npc_data to customize NPC appearance.
 */
data class NpcVisualConfig(
    var name: String,           // 0x00, 16 bytes ASCII
    var nameColor: UInt,        // 0x18, u32 ARGB
    var extraModel: UByte,      // 0x1C, u8
    var sectionId: UByte,       // 0x30, u8
    var charClass: UByte,       // 0x31, u8
    var validationFlags: UByte, // 0x32, u8
    var version: UByte,         // 0x33, u8
    var classFlags: UInt,       // 0x34, u32
    var costume: UShort,        // 0x38, u16
    var skin: UShort,           // 0x3A, u16
    var face: UShort,           // 0x3C, u16
    var head: UShort,           // 0x3E, u16
    var hair: UShort,           // 0x40, u16
    var hairR: UShort,          // 0x42, u16
    var hairG: UShort,          // 0x44, u16
    var hairB: UShort,          // 0x46, u16
    var proportionX: Float,     // 0x48, f32
    var proportionY: Float,     // 0x4C, f32
) {
    fun writeTo(buf: Buffer) {
        buf.setStringAscii(0x00, name, 16)
        // 0x10..0x17: unknown_a2 (8 bytes) — preserved from original buffer
        buf.setUInt(0x18, nameColor)
        buf.setUByte(0x1C, extraModel)
        // 0x1D..0x2B: newserv extended fields — preserved from original buffer
        // 0x2C..0x2F: name_color_checksum — preserved from original buffer
        buf.setUByte(0x30, sectionId)
        buf.setUByte(0x31, charClass)
        buf.setUByte(0x32, validationFlags)
        buf.setUByte(0x33, version)
        buf.setUInt(0x34, classFlags)
        buf.setUShort(0x38, costume)
        buf.setUShort(0x3A, skin)
        buf.setUShort(0x3C, face)
        buf.setUShort(0x3E, head)
        buf.setUShort(0x40, hair)
        buf.setUShort(0x42, hairR)
        buf.setUShort(0x44, hairG)
        buf.setUShort(0x46, hairB)
        buf.setFloat(0x48, proportionX)
        buf.setFloat(0x4C, proportionY)
    }

    companion object {
        const val SIZE = 0x50

        fun readFrom(buf: Buffer): NpcVisualConfig = NpcVisualConfig(
            name = buf.getStringAscii(0x00, 16, nullTerminated = true),
            nameColor = buf.getUInt(0x18),
            extraModel = buf.getUByte(0x1C),
            sectionId = buf.getUByte(0x30),
            charClass = buf.getUByte(0x31),
            validationFlags = buf.getUByte(0x32),
            version = buf.getUByte(0x33),
            classFlags = buf.getUInt(0x34),
            costume = buf.getUShort(0x38),
            skin = buf.getUShort(0x3A),
            face = buf.getUShort(0x3C),
            head = buf.getUShort(0x3E),
            hair = buf.getUShort(0x40),
            hairR = buf.getUShort(0x42),
            hairG = buf.getUShort(0x44),
            hairB = buf.getUShort(0x46),
            proportionX = buf.getFloat(0x48),
            proportionY = buf.getFloat(0x4C),
        )
    }
}

/**
 * PlayerStats (0x24 = 36 bytes).
 * Used by get_physical_data to customize enemy physical stats.
 */
data class EnemyPhysicalData(
    var atp: UShort,            // 0x00, u16 — Attack Power
    var mst: UShort,            // 0x02, u16 — Mental Strength
    var evp: UShort,            // 0x04, u16 — Evasion
    var hp: UShort,             // 0x06, u16 — Hit Points
    var dfp: UShort,            // 0x08, u16 — Defense
    var ata: UShort,            // 0x0A, u16 — Attack Accuracy
    var lck: UShort,            // 0x0C, u16 — Luck
    var esp: UShort,            // 0x0E, u16 — ESP
    var attackRange: Float,     // 0x10, f32
    var knockbackRange: Float,  // 0x14, f32
    var level: UInt,            // 0x18, u32 (for enemies: tech level)
    var experience: UInt,       // 0x1C, u32
    var meseta: UInt,           // 0x20, u32 (for enemies: TP)
) {
    fun writeTo(buf: Buffer) {
        buf.setUShort(0x00, atp)
        buf.setUShort(0x02, mst)
        buf.setUShort(0x04, evp)
        buf.setUShort(0x06, hp)
        buf.setUShort(0x08, dfp)
        buf.setUShort(0x0A, ata)
        buf.setUShort(0x0C, lck)
        buf.setUShort(0x0E, esp)
        buf.setFloat(0x10, attackRange)
        buf.setFloat(0x14, knockbackRange)
        buf.setUInt(0x18, level)
        buf.setUInt(0x1C, experience)
        buf.setUInt(0x20, meseta)
    }

    companion object {
        const val SIZE = 0x24

        fun readFrom(buf: Buffer): EnemyPhysicalData = EnemyPhysicalData(
            atp = buf.getUShort(0x00),
            mst = buf.getUShort(0x02),
            evp = buf.getUShort(0x04),
            hp = buf.getUShort(0x06),
            dfp = buf.getUShort(0x08),
            ata = buf.getUShort(0x0A),
            lck = buf.getUShort(0x0C),
            esp = buf.getUShort(0x0E),
            attackRange = buf.getFloat(0x10),
            knockbackRange = buf.getFloat(0x14),
            level = buf.getUInt(0x18),
            experience = buf.getUInt(0x1C),
            meseta = buf.getUInt(0x20),
        )
    }
}

/**
 * AttackData (0x30 = 48 bytes).
 * Used by get_attack_data to customize enemy attack properties.
 */
data class EnemyAttackData(
    var minAtp: Short,      // 0x00, i16
    var maxAtp: Short,      // 0x02, i16
    var minAta: Short,      // 0x04, i16
    var maxAta: Short,      // 0x06, i16
    var distanceX: Float,   // 0x08, f32
    var angle: UInt,        // 0x0C, u32 (0x10000 unit system)
    var distanceY: Float,   // 0x10, f32
    // 0x14..0x2F: unknown (28 bytes) — preserved from original buffer
) {
    fun writeTo(buf: Buffer) {
        buf.setShort(0x00, minAtp)
        buf.setShort(0x02, maxAtp)
        buf.setShort(0x04, minAta)
        buf.setShort(0x06, maxAta)
        buf.setFloat(0x08, distanceX)
        buf.setUInt(0x0C, angle)
        buf.setFloat(0x10, distanceY)
    }

    companion object {
        const val SIZE = 0x30

        fun readFrom(buf: Buffer): EnemyAttackData = EnemyAttackData(
            minAtp = buf.getShort(0x00),
            maxAtp = buf.getShort(0x02),
            minAta = buf.getShort(0x04),
            maxAta = buf.getShort(0x06),
            distanceX = buf.getFloat(0x08),
            angle = buf.getUInt(0x0C),
            distanceY = buf.getFloat(0x10),
        )
    }
}

/**
 * ResistData (0x20 = 32 bytes).
 * Used by get_resist_data to customize enemy elemental resistances.
 */
data class EnemyResistData(
    var evpBonus: Short,    // 0x00, i16
    var efr: UShort,        // 0x02, u16 — Fire Resist
    var eic: UShort,        // 0x04, u16 — Ice Resist
    var eth: UShort,        // 0x06, u16 — Thunder Resist
    var elt: UShort,        // 0x08, u16 — Light Resist
    var edk: UShort,        // 0x0A, u16 — Dark Resist
    // 0x0C..0x1B: unknown (16 bytes) — preserved from original buffer
    var dfpBonus: Int,      // 0x1C, i32
) {
    fun writeTo(buf: Buffer) {
        buf.setShort(0x00, evpBonus)
        buf.setUShort(0x02, efr)
        buf.setUShort(0x04, eic)
        buf.setUShort(0x06, eth)
        buf.setUShort(0x08, elt)
        buf.setUShort(0x0A, edk)
        buf.setInt(0x1C, dfpBonus)
    }

    companion object {
        const val SIZE = 0x20

        fun readFrom(buf: Buffer): EnemyResistData = EnemyResistData(
            evpBonus = buf.getShort(0x00),
            efr = buf.getUShort(0x02),
            eic = buf.getUShort(0x04),
            eth = buf.getUShort(0x06),
            elt = buf.getUShort(0x08),
            edk = buf.getUShort(0x0A),
            dfpBonus = buf.getInt(0x1C),
        )
    }
}

/**
 * MovementData (0x30 = 48 bytes).
 * Used by get_movement_data to customize enemy movement/navigation properties.
 */
data class EnemyMovementData(
    var f1: Float,  // 0x00
    var f2: Float,  // 0x04
    var f3: Float,  // 0x08
    var f4: Float,  // 0x0C
    var f5: Float,  // 0x10
    var f6: Float,  // 0x14
    var i1: Int,    // 0x18
    var i2: Int,    // 0x1C
    var i3: Int,    // 0x20
    var i4: Int,    // 0x24
    var i5: Int,    // 0x28
    var i6: Int,    // 0x2C
) {
    fun writeTo(buf: Buffer) {
        buf.setFloat(0x00, f1)
        buf.setFloat(0x04, f2)
        buf.setFloat(0x08, f3)
        buf.setFloat(0x0C, f4)
        buf.setFloat(0x10, f5)
        buf.setFloat(0x14, f6)
        buf.setInt(0x18, i1)
        buf.setInt(0x1C, i2)
        buf.setInt(0x20, i3)
        buf.setInt(0x24, i4)
        buf.setInt(0x28, i5)
        buf.setInt(0x2C, i6)
    }

    companion object {
        const val SIZE = 0x30

        fun readFrom(buf: Buffer): EnemyMovementData = EnemyMovementData(
            f1 = buf.getFloat(0x00),
            f2 = buf.getFloat(0x04),
            f3 = buf.getFloat(0x08),
            f4 = buf.getFloat(0x0C),
            f5 = buf.getFloat(0x10),
            f6 = buf.getFloat(0x14),
            i1 = buf.getInt(0x18),
            i2 = buf.getInt(0x1C),
            i3 = buf.getInt(0x20),
            i4 = buf.getInt(0x24),
            i5 = buf.getInt(0x28),
            i6 = buf.getInt(0x2C),
        )
    }
}
