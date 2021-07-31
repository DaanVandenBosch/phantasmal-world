package world.phantasmal.psoserv.encryption

import world.phantasmal.psolib.buffer.Buffer
import kotlin.experimental.xor
import kotlin.random.Random

/**
 * Blowfish with some modifications.
 */
class BbCipher(override val key: ByteArray = createKey()) : Cipher {
    private val pArray: UIntArray = P_ARRAY.copyOf()
    private val sBoxes: Array<UIntArray> = Array(S_BOXES.size) { S_BOXES[it].copyOf() }
    private val scrambledKey: ByteArray = ByteArray(KEY_SIZE)

    override val blockSize: Int = 8

    init {
        require(key.size == KEY_SIZE) {
            "Expected key of size $KEY_SIZE, but was ${key.size}."
        }

        scrambleKey()
        initPArrayAndSBoxes()
    }

    private fun scrambleKey() {
        for (i in key.indices step 3) {
            scrambledKey[i] = key[i] xor 0x19
            scrambledKey[i + 1] = key[i + 1] xor 0x16
            scrambledKey[i + 2] = key[i + 2] xor 0x18
        }
    }

    private fun initPArrayAndSBoxes() {
        // PSOBB P-array scramble.
        for (i in pArray.indices) {
            val pt = ((pArray[i] and 0xFF00u) shr 8) or ((pArray[i] and 0x00FFu) shl 8)
            pArray[i] = (((pArray[i] shr 16) xor pt) shl 16) or pt
        }

        // Standard Blowfish P-array initialization.
        for (i in pArray.indices) {
            var k = 0u
            val keyPos = 4 * (i % 12)

            repeat(4) {
                k = (k shl 8) or scrambledKey[keyPos + it].toUByte().toUInt()
            }

            pArray[i] = pArray[i] xor k
        }

        // Standard Blowfish key expansion.
        var l = 0u
        var r = 0u

        for (i in pArray.indices step 2) {
            encryptBlock(l, r, setL = { l = it }, setR = { r = it }, iterations = 16)
            pArray[i] = l
            pArray[i + 1] = r
        }

        for (sBox in sBoxes) {
            for (i in sBox.indices step 2) {
                encryptBlock(l, r, setL = { l = it }, setR = { r = it }, iterations = 16)
                sBox[i] = l
                sBox[i + 1] = r
            }
        }
    }

    override fun encrypt(data: Buffer, offset: Int, blocks: Int) {
        require(offset >= 0)
        require(blocks >= 0)
        val limit = offset + blocks * blockSize
        require(limit <= data.size)

        for (i in offset until limit step blockSize) {
            val lIndex = i
            val rIndex = i + 4
            encryptBlock(
                l = data.getUInt(lIndex),
                r = data.getUInt(rIndex),
                setL = { data.setUInt(lIndex, it) },
                setR = { data.setUInt(rIndex, it) },
                iterations = 4,
            )
        }
    }

    private inline fun encryptBlock(
        l: UInt,
        r: UInt,
        setL: (UInt) -> Unit,
        setR: (UInt) -> Unit,
        iterations: Int,
    ) {
        var newL = l
        var newR = r

        for (i in 0 until iterations) {
            newL = newL xor pArray[i]
            newR = newR xor f(newL)
            // Swap l and r.
            val tmp = newL
            newL = newR
            newR = tmp
        }

        newL = newL xor pArray[iterations]
        newR = newR xor pArray[iterations + 1]

        // Swap l and r.
        setL(newR)
        setR(newL)
    }

    override fun decrypt(data: Buffer, offset: Int, blocks: Int) {
        require(offset >= 0)
        require(blocks >= 0)
        val limit = offset + blocks * blockSize
        require(limit <= data.size)

        for (i in offset until limit step blockSize) {
            decryptBlock(data, lIndex = i, rIndex = i + 4)
        }
    }

    private fun decryptBlock(data: Buffer, lIndex: Int, rIndex: Int) {
        var l = data.getUInt(lIndex)
        var r = data.getUInt(rIndex)

        for (i in 5 downTo 2) {
            l = l xor pArray[i]
            r = r xor f(l)
            // Swap l and r.
            val tmp = l
            l = r
            r = tmp
        }

        l = l xor pArray[1]
        r = r xor pArray[0]

        // Swap l and r.
        data.setUInt(lIndex, r)
        data.setUInt(rIndex, l)
    }

    override fun advance(blocks: Int) {
        // Do nothing.
    }

    private fun f(x: UInt): UInt {
        var h = sBoxes[0][(x shr 24).toInt()]
        h += sBoxes[1][((x shr 16) and 0xFFu).toInt()]
        h = h xor sBoxes[2][((x shr 8) and 0xFFu).toInt()]
        h += sBoxes[3][(x and 0xFFu).toInt()]
        return h
    }

    companion object {
        const val KEY_SIZE = 48

        fun createKey(): ByteArray = Random.nextBytes(KEY_SIZE)

        private val P_ARRAY: UIntArray = uintArrayOf(
            0x640cded2u, 0xca6cf7cfu, 0xc7bc95fbu, 0x7d0d60a3u, 0xcf23ad88u, 0x8ffb62dcu,
            0x6c3da5ccu, 0x6bfcd6d6u, 0x63f492dfu, 0xe32ebe65u, 0xc3746b6du, 0xc5703934u,
            0xdc940bceu, 0x590e0892u, 0xea9413e8u, 0xf4b13de7u, 0x505893fcu, 0xe3d696e3u,
        )
        private val S_BOXES: Array<UIntArray> = arrayOf(
            uintArrayOf(
                0x5CC73FD6u, 0x19572A8Eu, 0x1EAD320Eu, 0x29913B33u,
                0x05C06104u, 0xC5A1316Eu, 0x456D82A7u, 0x5A987789u,
                0xBFDCAA97u, 0x23094413u, 0x70B100F7u, 0xEF18F524u,
                0x9B2632B1u, 0x1A7AA450u, 0x36355519u, 0x1A8FC2ADu,
                0xE13D6A17u, 0xC74AF6AFu, 0xB771FC73u, 0x8C332A8Cu,
                0x13792C10u, 0xA707616Fu, 0x69D18CE4u, 0x4BB744C2u,
                0x74DA584Bu, 0xC2186564u, 0xBEF96BFDu, 0x9FF42F9Eu,
                0xF6334290u, 0x74249103u, 0xC0D5CCCCu, 0xACC295F2u,
                0x7BB4D473u, 0xBA4753A2u, 0x46806E9Fu, 0x7F8EB321u,
                0x16803FE6u, 0x891FD2BCu, 0xE7218373u, 0x82CDF207u,
                0x819879E3u, 0xB0CDE742u, 0xA9160843u, 0x14336F73u,
                0xFC0B51A2u, 0x2FD13817u, 0x231C4134u, 0xBEA851C9u,
                0x1B8B5DBFu, 0xB225A875u, 0x6BCC7EC4u, 0xCC5C66EFu,
                0xB5C06E89u, 0xDC479976u, 0x1B984ED0u, 0x35BD70C1u,
                0x8EC73F26u, 0xC85ED3FBu, 0x93A2CFF3u, 0xC0C1889Fu,
                0x74C405A6u, 0xB4BA3842u, 0x62A89A52u, 0x850373D1u,
                0xA8AD015Eu, 0x4087946Au, 0x1E81C985u, 0xE0278FEEu,
                0x6D38EC05u, 0xBF4E158Au, 0x63E32BDDu, 0x17578163u,
                0x9861C874u, 0x535ED4CFu, 0xE0674A4Au, 0xA2233B6Cu,
                0x523574E3u, 0x35D19568u, 0x0247AFF9u, 0xED2BF2A5u,
                0x1A404CC6u, 0x5700A52Cu, 0x3F5847FCu, 0x9139F9FCu,
                0x8721985Au, 0x17A0493Bu, 0xF333A0D4u, 0x489411FFu,
                0xD92EF4DBu, 0x1E50C960u, 0x833757F6u, 0xBBC00C1Bu,
                0x01558F29u, 0x68058035u, 0xDB7D2645u, 0x5D11A667u,
                0xAEE02660u, 0x3F5B5474u, 0xE3CF9E79u, 0x8E0E6574u,
                0x2E4CEC6Fu, 0xB900EBB0u, 0x30F703C1u, 0xE73ECEE4u,
                0x907D69CBu, 0xB785648Du, 0xEE57BBE1u, 0xA0862CB0u,
                0xE942E1C5u, 0x2C7D0221u, 0xDF5445F7u, 0xD8C8AC9Fu,
                0x22F05641u, 0x3E295EACu, 0x1E138FAAu, 0x3598F64Du,
                0xDA199769u, 0xF157C46Du, 0x7CA171C5u, 0x94301DB9u,
                0xFDC90D52u, 0x387128A3u, 0x41D7C806u, 0xD3190DABu,
                0x3ACD7A85u, 0xE83EBBE3u, 0x14322C57u, 0x26845B42u,
                0xB2CD49CBu, 0xE4D22B24u, 0x23C11989u, 0xE4FCD996u,
                0x0FC3AD3Du, 0xE17A680Cu, 0xF4F0F8D8u, 0x72350D14u,
                0x4C747633u, 0xC9633B10u, 0xFEC3618Bu, 0xFDE8DD1Cu,
                0x9369EDF4u, 0xC8AECED7u, 0xE7160549u, 0x75BD584Cu,
                0xF0451846u, 0xCEFB421Cu, 0x50FC8705u, 0xD67643AEu,
                0x970AFDE8u, 0x09F8DEBAu, 0x6E82EAADu, 0x80CEB947u,
                0x51AFE307u, 0x727B3F2Fu, 0xB22B287Bu, 0xF077F03Au,
                0x4B670178u, 0x1F942DDEu, 0x37AFEAFFu, 0xE569CDE3u,
                0xB78DD11Du, 0x6E8307D1u, 0x95CE57C6u, 0xC0E34476u,
                0x2CA562D1u, 0x6373D161u, 0x2E549898u, 0xC6F47EC6u,
                0x4A2A6BE4u, 0x6898DD70u, 0xFF954A7Cu, 0x8F033CD0u,
                0xCD64C8E8u, 0x3C0A7D7Bu, 0xA3057D95u, 0xECD438E0u,
                0xC111363Au, 0xB94FD214u, 0x7F224DFEu, 0xF042A491u,
                0x9F1489FCu, 0x75E73DC9u, 0x1EA04F71u, 0xA38F2685u,
                0x8BA7AF61u, 0x8DBF33DFu, 0x4EACD05Du, 0x3CEF9B0Eu,
                0x9604FE9Fu, 0xB65D9990u, 0xBBCB14BAu, 0x06FC3A41u,
                0xE15376DEu, 0x97D9BC59u, 0x8318618Au, 0x2DB10C0Cu,
                0x3736FC1Fu, 0x6E8136D8u, 0x7E470DB5u, 0xC60DAED2u,
                0x5A19532Fu, 0x98094AA8u, 0xE830FEA2u, 0x126A0685u,
                0x2B76B98Fu, 0xA378F291u, 0xD36FB474u, 0xA3849120u,
                0x7868242Au, 0x87743EA2u, 0x1D74914Fu, 0xB341998Cu,
                0xD5B45B60u, 0xCD97DD2Eu, 0x9CEF94C3u, 0x907D0C7Bu,
                0xAA967285u, 0x2C0C2B35u, 0x852D480Bu, 0xAFB7455Cu,
                0x0FB40A91u, 0xDE019AC6u, 0xF285AA86u, 0xB5AF214Bu,
                0x94E3A9D8u, 0x61CC82DEu, 0x592CE330u, 0x24943EEFu,
                0xC689113Fu, 0x68A7FE73u, 0xCE85CAE0u, 0x9477D5B7u,
                0x7EE161B8u, 0x1C4F6B1Bu, 0xAD1073F1u, 0xFBA9FFF8u,
                0x11A5CE22u, 0x19BE7AF3u, 0x8646D47Au, 0xDD92E45Bu,
                0xA5B089C8u, 0x05DB18A7u, 0xD915FB67u, 0xAE545E52u,
                0x738B8333u, 0xE351E074u, 0xD846F324u, 0x4C4C85AEu,
                0x1F705EAFu, 0x3C65970Cu, 0xB540A652u, 0x08355576u,
                0x88FD52F2u, 0x1176FA93u, 0x04D2406Au, 0xA53E17C7u,
            ),
            uintArrayOf(
                0xC5FB6441u, 0xD36FC212u, 0x5C5AC0C9u, 0xE2C932C2u,
                0xD22A7467u, 0xAD1D4B06u, 0xDC30354Au, 0x09F640EAu,
                0x1B063309u, 0x0777B7A2u, 0xE30F2845u, 0xB16ED5E6u,
                0x897B6ABFu, 0x1E2EC223u, 0xCFB0AC5Cu, 0x0297F232u,
                0x7F56F89Du, 0xA3F50491u, 0x7C847191u, 0x61D4B903u,
                0x25EE2690u, 0x58F77A26u, 0xC2D527FEu, 0x8123AFBEu,
                0x7DFF42E6u, 0x9572104Bu, 0x15D8E9F6u, 0x23F908C8u,
                0x1156A4DCu, 0xF8816E83u, 0xAEA972ECu, 0x9095ECFBu,
                0xFDD7AFADu, 0xAA156F86u, 0x3306C3ADu, 0x5B21343Du,
                0x13D0F0D9u, 0xA9098ABFu, 0x522944F1u, 0x76D2A256u,
                0xE259A0B5u, 0x4675D80Du, 0x8B3DFC79u, 0xB9A76F83u,
                0xF168CD53u, 0x0609A55Bu, 0x98E96452u, 0xB17832D9u,
                0x8A90CBC9u, 0xC0229573u, 0x17266917u, 0x20055F24u,
                0xAAF79B0Du, 0xE0D393EBu, 0x282C0B07u, 0x63AF3BBEu,
                0x9FD9AE8Fu, 0xA0325E5Cu, 0x759B22ACu, 0xABB02882u,
                0xAA56E55Cu, 0xA302AA9Eu, 0x95E40019u, 0x1F41E3C9u,
                0x164B605Du, 0x30CD6081u, 0xF46F6677u, 0x66FDDBB7u,
                0xAE738ACEu, 0x64A9B3FFu, 0x76CB795Fu, 0x8671B0E4u,
                0x946FDF07u, 0x0F0712DCu, 0x14BE281Au, 0xEE01E411u,
                0x5473C49Fu, 0xDD572435u, 0x6183D89Bu, 0xD8946913u,
                0xCCA66FF9u, 0x39D5A9BEu, 0x3B1A7D18u, 0xA72B5D96u,
                0x111E8E30u, 0xDAB26740u, 0x3F64B3DEu, 0xD1695E1Au,
                0x33A19648u, 0x31DC630Au, 0xF5F35694u, 0xB91ED674u,
                0x06FE9043u, 0xBE9E4E5Bu, 0xDA426AABu, 0x535055ECu,
                0x0D2B265Eu, 0xEF43B103u, 0xB7EDF4B1u, 0xAA2618F5u,
                0xA3D00018u, 0xEFA242CCu, 0x49D47F55u, 0x562677C2u,
                0x7D41EEDAu, 0x40BF3AA5u, 0x135B8EEAu, 0x5DBED1DAu,
                0x99BC688Au, 0xFE073B61u, 0x34E62A8Eu, 0x5125D336u,
                0xDC70A9A6u, 0x292C52B4u, 0x2C7E2F60u, 0x04647F1Fu,
                0x8A1989C4u, 0xEBA69244u, 0xA54A3897u, 0xFAC0D4D0u,
                0xAD47205Bu, 0xF794C013u, 0xCD3C0A23u, 0xBA9671ACu,
                0x8D1EAEA6u, 0x0DE2E83Eu, 0x9FBEE730u, 0xFA0684A3u,
                0x42D96104u, 0x0E97CE42u, 0x698374A6u, 0xEF7D8288u,
                0xF590DE72u, 0x6899F987u, 0x1BFD58ECu, 0x38B4B274u,
                0x088A50AEu, 0xAE2113B0u, 0xE64CF295u, 0xBB67F9BEu,
                0xDFA77BC0u, 0x598481EAu, 0x13E267B8u, 0xA7EB1033u,
                0x7CA6DDDAu, 0x4A836CEDu, 0xBF89C618u, 0xBFBE9DAEu,
                0xA44FE33Au, 0xA0BE3198u, 0xED12AF84u, 0x20976BE3u,
                0x4754AA5Bu, 0x72930C88u, 0xB68D8550u, 0x532558E9u,
                0x230F5F40u, 0xC0BD9035u, 0x672F3482u, 0xA89A61BFu,
                0x4AA288DCu, 0x2045C67Au, 0xC59B9AE6u, 0xA0337DF9u,
                0xE1857270u, 0xFD3DFF5Du, 0xE301EC12u, 0x50FFAE66u,
                0x89DFE89Cu, 0x768C6E14u, 0x0AA10D87u, 0xFE2FEEF1u,
                0x61B3A2EEu, 0xD5A31E6Fu, 0x7789B9F2u, 0x0FF5C3B1u,
                0x29F1C194u, 0x77D011B0u, 0xECC10B84u, 0x6F931750u,
                0x70B62A8Fu, 0xBB83CEFEu, 0x5F497EB2u, 0xF17666D6u,
                0x5D785704u, 0x865C980Bu, 0xF0249EC2u, 0xAAE844DBu,
                0x4CD28E52u, 0xDA93ADE9u, 0xD966908Cu, 0xA4B9FDDCu,
                0x1FAE7671u, 0x96513D07u, 0x98F07CB6u, 0x7C13B222u,
                0x1F05FFE7u, 0xFF903B48u, 0xC8D0DBBBu, 0xA6E52EB5u,
                0x7D7BC10Au, 0xAFE0D2F7u, 0x01B79CC8u, 0x578225F9u,
                0xE40C41B3u, 0xB5C7E26Au, 0xA46286EFu, 0x7B138D12u,
                0x432661B3u, 0xC9C8124Eu, 0xE4BE379Bu, 0x34AEE10Du,
                0x59AFF4CBu, 0xDAD26C27u, 0x5C9561B8u, 0x4D6B0452u,
                0x10955F82u, 0x8AAD8718u, 0x4AAF2843u, 0xB94C51F7u,
                0x756FF181u, 0xE701F22Eu, 0xA70427EEu, 0x52654509u,
                0x2E4C3CABu, 0x33E7AF57u, 0xCCDC8F42u, 0xB8B3CA13u,
                0x9122C3F3u, 0xF074441Au, 0x48E1C890u, 0xAE102653u,
                0xC977A7F2u, 0x2FE76749u, 0x754513C2u, 0xA2A86DF9u,
                0x7312F6B7u, 0xCCA4E105u, 0xACFB96CDu, 0xA0A9A9B2u,
                0x237FAF6Du, 0x45B7EB4Du, 0x0C3E5872u, 0x460C5991u,
                0x97248330u, 0xA47541B2u, 0xBF76D53Bu, 0x6C6C782Bu,
                0x38A76A50u, 0x712E9FECu, 0xE7071507u, 0x0E4202B2u,
                0x95A4154Eu, 0x62F6DA87u, 0x3DFD5418u, 0xD7AB33F5u,
            ),
            uintArrayOf(
                0x8E13062Cu, 0x2CEEE22Eu, 0x0B54E6A6u, 0xD073C03Au,
                0x3D3F670Eu, 0xDB090F3Au, 0xCB73AB2Du, 0x210CC211u,
                0x79FC9477u, 0x56DB66CEu, 0x7607573Au, 0xC56D0340u,
                0x0D6F50E7u, 0x0F911F2Au, 0x16F5699Bu, 0x63123CB0u,
                0x0015F81Bu, 0xFC22CC2Bu, 0x6594C4BAu, 0x1D645134u,
                0x8633C3C5u, 0x6565D5D9u, 0xC902200Bu, 0x8EA7AA6Eu,
                0xA28B3D86u, 0x9F22EF15u, 0x9E80E834u, 0x1931D611u,
                0xD25095EDu, 0xDCE57608u, 0xBE54D17Au, 0xB75B7B77u,
                0xFF53C715u, 0x6D1FE6F3u, 0xF4F1E1E8u, 0x507749B1u,
                0x0C153DB4u, 0x7E80AD1Cu, 0xA5791026u, 0xAD3DBE27u,
                0x7A65A28Fu, 0x9361771Bu, 0x570CC089u, 0x8D3412AAu,
                0xA68FD2E0u, 0xDAB72770u, 0x2A303EDCu, 0x6477E936u,
                0x16F913E0u, 0x09274ED9u, 0xE49A321Du, 0x1E64052Eu,
                0x74AB96C9u, 0xD5FDD822u, 0x3DB27BD0u, 0x13E13918u,
                0xD083F603u, 0xA4CC1CD1u, 0x2FF33194u, 0x8F610AB0u,
                0xA1472C0Fu, 0x618F44D7u, 0x25294EABu, 0x4D6915BFu,
                0xFCE933D0u, 0x32454A0Au, 0xA0BDC3A7u, 0xA5E7417Cu,
                0x736BE207u, 0xE1859393u, 0x4B2BA3CAu, 0x689C8713u,
                0xA1431A31u, 0xB1E88845u, 0xF1AB868Bu, 0x5A832C62u,
                0xB774E1EAu, 0xF334763Cu, 0x1692AA49u, 0xDEBB4312u,
                0x934B30B3u, 0x551E3EEDu, 0x7E832F92u, 0x73E7DF4Au,
                0x0E51B5EBu, 0xEFA0C479u, 0x08804ADFu, 0x770EE5F0u,
                0x3F35314Au, 0x9E2CABCCu, 0x40C2F1E4u, 0xE9764A79u,
                0xE947E751u, 0x52261A4Du, 0x8C0A9EE8u, 0x23E5D212u,
                0x954E09E5u, 0xCD1AF9F0u, 0x23B48F97u, 0x5A1A7DDCu,
                0xC4D467CFu, 0x8A1301D3u, 0x30A40AE0u, 0xDC9B40A1u,
                0x102BFB9Fu, 0x5A429B7Fu, 0xB0025E38u, 0x58D3215Eu,
                0xCD199BDBu, 0x6738E9BDu, 0xD063B1F4u, 0xF72FFC51u,
                0x56C10096u, 0xA7959937u, 0xA9E12B93u, 0x40C42AB1u,
                0xA812D5CAu, 0x712A414Eu, 0x55242B16u, 0x3C1E0AD7u,
                0x069B7F70u, 0xF7B3E6C8u, 0x5A592AA1u, 0x84438CA2u,
                0xBC775FD6u, 0xA9B80BD7u, 0x089BAD81u, 0x0D8DE9CCu,
                0xC8B58CC9u, 0xB35975C1u, 0x5B39B997u, 0xBFF2C526u,
                0xB4256EB5u, 0x71675891u, 0x6FBE1984u, 0x306519F6u,
                0x08CE4519u, 0xF2357ABEu, 0x3FC05C11u, 0x30C6E91Du,
                0x7763FDA3u, 0xFDD5D266u, 0x110B6F90u, 0x1F2EFD86u,
                0x98D90A21u, 0xAE8EDDECu, 0xA2E88E17u, 0xDF6D25D9u,
                0xB783C519u, 0xFF880B82u, 0x3BF0C612u, 0x2BD6849Cu,
                0x7354B07Au, 0x020B7961u, 0xEBA8E89Eu, 0x2ED7D4BFu,
                0x8F438E34u, 0xF14B33E9u, 0xE6FE502Fu, 0xBF986A6Eu,
                0xA103993Au, 0x27C5B0FFu, 0x3ABB8CA0u, 0x86EDF8D4u,
                0xD01E172Eu, 0x38F4A865u, 0x0DAE791Au, 0x1C89748Fu,
                0xEB3E3795u, 0xBFE7D73Bu, 0x4EC6C12Au, 0x877EF600u,
                0x5A3CBC36u, 0x116030C8u, 0xD5B7A87Cu, 0x524D84D9u,
                0x23E3E04Fu, 0x78097FA7u, 0xFEC92E57u, 0x7E4DB0C5u,
                0x3B66D2C0u, 0x2DDEF511u, 0x3ED80C4Bu, 0x13A4087Fu,
                0x0D5EE881u, 0xAD6AD02Eu, 0x5A542426u, 0x2BDEF8E7u,
                0x446A7DA7u, 0xFC268A55u, 0x5D9D00BDu, 0x3710D1B5u,
                0x270F7612u, 0x38F22C86u, 0xFFBFEC26u, 0x9482AA51u,
                0x8DD6673Bu, 0x8F7C80EFu, 0x5C12531Fu, 0x86AE5611u,
                0x9CCCD007u, 0x4D29CBF6u, 0x8A0FF3A8u, 0xF0F2332Du,
                0x275D7034u, 0xDA8F94FDu, 0x5AC736FAu, 0xB4CB60E4u,
                0x1E74C5A9u, 0x53CC5AC5u, 0xEC538437u, 0x825489D9u,
                0x0BA43378u, 0x07657513u, 0x35EC8375u, 0x1DA2A732u,
                0x7A3B5EDEu, 0xAB6FD84Eu, 0x6F8B7EDAu, 0x39994295u,
                0xD45F7FAFu, 0xBF6AE7C4u, 0xE4257C3Du, 0x5EE315A1u,
                0x0BB321C5u, 0x0E88401Bu, 0xB7053E8Bu, 0xD25E9808u,
                0x9FF33EF5u, 0x89A0BD64u, 0xFFDB0F83u, 0xA34404C9u,
                0x70C36E1Eu, 0x9BE9BABBu, 0x2A932500u, 0x5750FD0Eu,
                0xA4CAB6F5u, 0x9EC00D66u, 0x1B5F057Du, 0xC88A5A6Bu,
                0x57E3D177u, 0xBC09B7D8u, 0xB7EBA4D3u, 0x077F3FE7u,
                0xF8DC24F4u, 0x25E5CF54u, 0xD052AEF5u, 0x30C74026u,
                0xFD5E2773u, 0xCE327753u, 0xCABD0692u, 0xCF4C8BE0u,
                0x3AF2851Fu, 0xF2B8CC7Cu, 0x2838C54Bu, 0xBD2729DBu,
            ),
            uintArrayOf(
                0xC570A03Cu, 0x1CD9298Du, 0x53AC5593u, 0x5CB35E31u,
                0xCA7F4500u, 0x868E31F8u, 0x68BF5639u, 0x927BB899u,
                0x97869F8Cu, 0x22C8AFF2u, 0xE97AB5ACu, 0xC4E199F7u,
                0x11F56E63u, 0x316E6F9Bu, 0xDC0B25B0u, 0x3C0E37BFu,
                0x2260AB3Du, 0xC7F5E4FEu, 0x3D408195u, 0x618DC6C1u,
                0x8801C70Eu, 0xC181139Du, 0xEECFB730u, 0x19F23DE1u,
                0xD9C4ED07u, 0x6E4C91A3u, 0x4B7131FDu, 0x882FD1B0u,
                0x95DAC0A1u, 0xC764F41Bu, 0xE8B192A7u, 0x8C8AB9C3u,
                0x035446CBu, 0xC8655163u, 0xF6CA7757u, 0xFA554923u,
                0x850ADB81u, 0x9F44293Cu, 0x06742262u, 0x872A79D3u,
                0xCC79E9FDu, 0xBDAF5759u, 0xA75653BBu, 0x25A1C64Fu,
                0x33BF5313u, 0xFCC408F4u, 0xC61DBE73u, 0x2DA095D3u,
                0xDA93F942u, 0x2807D44Au, 0x6663B694u, 0x1383C9A1u,
                0xFCCF0B6Cu, 0x2EBE6EC6u, 0x3EDF77ECu, 0x066D5D70u,
                0x6BC67BB5u, 0xC54EE732u, 0xEBE6E605u, 0xA5F5CF9Au,
                0x3D6C0AB2u, 0x572BE8C0u, 0xF02195C5u, 0xCC75FF05u,
                0x5454BCE7u, 0xED431C7Au, 0x35FF8D73u, 0xA69F1357u,
                0xBE3322DFu, 0x8D5701D3u, 0x8227C6E1u, 0x7F92B847u,
                0x17503B18u, 0xFEBF088Fu, 0xB969378Cu, 0x80695378u,
                0x6EB6C428u, 0xF6AE7809u, 0xF8115237u, 0x72659F3Du,
                0x90DD9052u, 0xF60E6B5Bu, 0xE98A45D4u, 0x6CA89B02u,
                0x85327733u, 0x7C899229u, 0x923FCEDAu, 0x987066D2u,
                0x3497E625u, 0x3E04C58Du, 0xB1BE8DB6u, 0x172BAEF9u,
                0x30C3CC5Au, 0x573DDE84u, 0x67F06558u, 0x8E21FF58u,
                0x00F3F92Du, 0x6CF4CFC2u, 0x13415015u, 0xF461CD1Du,
                0xAD6C6355u, 0x92BF842Cu, 0x274E705Eu, 0xEE44FD1Du,
                0x05FD79B4u, 0x40741777u, 0x70A40BF2u, 0x261632C0u,
                0xD3DAE96Bu, 0xE8EBC9BDu, 0x3BE3D490u, 0xB4530A30u,
                0xBA6DFDE5u, 0x3A648E2Du, 0xB14C4F26u, 0x7C7D0A3Eu,
                0x559FB601u, 0x44B1A722u, 0x72FCFF7Fu, 0xF62F6ADFu,
                0xAEC6F92Eu, 0x6511AD20u, 0x4AF6AD4Au, 0xAA5B3A09u,
                0x5303B2BEu, 0xBB66DF75u, 0xA2490B13u, 0xEACF61BAu,
                0x73B29C61u, 0x509A66EEu, 0x8080BDDAu, 0x9216DACAu,
                0xFAEFC031u, 0x65896009u, 0x3FA36CFCu, 0x995FEFF2u,
                0xCE98EAB5u, 0x66D7E0CDu, 0xE5A71216u, 0xD182BC77u,
                0xC7D769A6u, 0xDA5ECC66u, 0x0473072Cu, 0xE84B6CC7u,
                0x8BBD0177u, 0x0D1075AAu, 0x2BF0168Cu, 0xA7229229u,
                0xBB80827Bu, 0xF0066C50u, 0x5A614BF6u, 0x23AFE56Au,
                0x067DAA78u, 0xBF01EEE6u, 0x5B081768u, 0x1CC2F422u,
                0xFB6A0382u, 0xA5A777A7u, 0x7609E111u, 0x77097C89u,
                0x075C4FBFu, 0x51E9004Fu, 0xEC84F0CBu, 0x1DE8CC73u,
                0x2A54A800u, 0x09A89025u, 0xFA5F8045u, 0xC29B195Du,
                0xCFF9BFE6u, 0x522DBFF5u, 0x9C374800u, 0x347DCD8Fu,
                0x974DA9C0u, 0x8D6A6D88u, 0xB47EF442u, 0xA51E66CAu,
                0xA210C54Au, 0x4F63C725u, 0xFF1A465Du, 0x813EB31Bu,
                0x0E0058D5u, 0x3C18CE5Au, 0xC4D7D98Cu, 0x4E24DA16u,
                0x5AEC5AF6u, 0x912CD19Fu, 0xB12BF2D1u, 0x184D3B0Bu,
                0x82DBD6DAu, 0xD29EAD22u, 0x9D13DCE5u, 0xBEA27F78u,
                0xB957B027u, 0xE0DAE424u, 0x1AE3AB8Fu, 0x49C349A2u,
                0x74EFDA3Du, 0x88539BD4u, 0xB9F027C3u, 0x5739E997u,
                0x08D6028Eu, 0x8D1F0B8Fu, 0x63256408u, 0x9B216118u,
                0xD89432D3u, 0x3BEBF6CAu, 0x21735953u, 0x0EDA4BFBu,
                0xE6AFC2D4u, 0xA9DB95F9u, 0x1F1C6BB0u, 0xBAAF121Bu,
                0x8CDC1B36u, 0x3913F9FDu, 0x863BCB1Au, 0xBD34ADCBu,
                0xDA48457Au, 0x4F584129u, 0xDD85156Cu, 0x0324F396u,
                0xD41E1EE1u, 0xC3B48F82u, 0x2124FB4Cu, 0x6C0B2635u,
                0x95CE3157u, 0xA8DACA8Cu, 0xB54E1542u, 0xD989F76Au,
                0x0C1EA5E3u, 0x973FE85Cu, 0xE6E91D97u, 0x2916B8DFu,
                0x5B0B05E2u, 0x57BDC906u, 0x7CB2CCEFu, 0x131C7553u,
                0x41CA9311u, 0x6E70E1C1u, 0x0F972BF2u, 0x8CF59D7Au,
                0xE6613022u, 0x69218E19u, 0xC350744Au, 0xA2D5BF1Bu,
                0x9FA14B7Bu, 0x8867F25Cu, 0xB8E19AF6u, 0x777A25DFu,
                0xA2004E28u, 0xFB929664u, 0x2DA8A284u, 0x21955D47u,
                0xF0CEBD06u, 0x9887B1E9u, 0xBF7810C1u, 0x265D91F9u,
            )
        )

        init {
            check(P_ARRAY.size == 18)
            check(S_BOXES.size == 4)

            for (box in S_BOXES) {
                check(box.size == 256)
            }
        }
    }
}
