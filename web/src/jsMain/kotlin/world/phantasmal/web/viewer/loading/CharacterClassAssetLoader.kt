package world.phantasmal.web.viewer.loading

import world.phantasmal.core.Success
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.fileFormats.ninja.*
import world.phantasmal.psolib.fileFormats.parseAfs
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.loading.LoadingCache
import world.phantasmal.web.shared.dto.SectionId
import world.phantasmal.web.viewer.models.CharacterClass
import world.phantasmal.web.viewer.models.CharacterClass.*
import world.phantasmal.webui.DisposableContainer

class CharacterClassAssetLoader(private val assetLoader: AssetLoader) : DisposableContainer() {
    private val ninjaObjectCache: LoadingCache<CharacterClass, NjObject> =
        addDisposable(LoadingCache(::loadBodyParts) { /* Nothing to dispose. */ })

    private val xvrTextureCache: LoadingCache<CharacterClass, List<XvrTexture?>> =
        addDisposable(LoadingCache(::loadTextures) { /* Nothing to dispose. */ })

    suspend fun loadNinjaObject(char: CharacterClass): NjObject =
        ninjaObjectCache.get(char)

    /**
     * Loads body parts with specific head and hair style numbers.
     * Unlike [loadNinjaObject], this does NOT use the cache since combinations vary.
     */
    suspend fun loadNinjaObject(char: CharacterClass, headStyle: Int, hairStyle: Int): NjObject {
        val texIds = textureIds(char, SectionId.Viridia, 0)

        val body = loadBodyPart(char, "Body")
        val head = loadBodyPart(char, "Head", no = headStyle)
        var shift = 1 + texIds.body.size
        shiftTextureIds(head, shift)
        addToBone(body, head, parentBoneId = 59)

        if (char.hairStyleCount == 0 || hairStyle < 0) {
            return body
        }

        val hair = loadBodyPart(char, "Hair", no = hairStyle)
        shift += texIds.head.size
        shiftTextureIds(hair, shift)
        addToBone(head, hair, parentBoneId = 0)

        if (hairStyle !in char.hairStylesWithAccessory) {
            return body
        }

        try {
            val accessory = loadBodyPart(char, "Accessory", no = hairStyle)
            shift += texIds.hair.size
            shiftTextureIds(accessory, shift)
            addToBone(hair, accessory, parentBoneId = 0)
        } catch (_: Exception) {
            // Accessory file may not exist for this style.
        }

        return body
    }

    /**
     * Loads NPC body parts separately (body, head, optionally hair).
     * Returns a list of NjObjects: [body, head] or [body, head, hair].
     */
    suspend fun loadNpcParts(extraModel: Int): List<NjObject> {
        val slug = npcSlugOrThrow(extraModel)
        val parts = mutableListOf<NjObject>()
        parts += loadNpcBodyPart(slug, "Body")
        parts += loadNpcBodyPart(slug, "Head")

        // Only Flowen (5) and Elly (6) have hair.
        if (extraModel >= 5) {
            try {
                parts += loadNpcBodyPart(slug, "Hair")
            } catch (_: Exception) {
                // Hair file may not exist.
            }
        }
        return parts
    }

    /**
     * Loads all XVR textures for an NPC model from its AFS archive.
     */
    suspend fun loadNpcXvrTextures(extraModel: Int): List<XvrTexture?> {
        val slug = npcSlugOrThrow(extraModel)
        val buffer = assetLoader.loadArrayBuffer("/player/${slug}Tex.afs")
        val afsResult = parseAfs(buffer.cursor(Endianness.Little))

        if (afsResult !is Success) {
            return emptyList()
        }

        return afsResult.value
            .map { parseXvm(it.cursor()) }
            .filterIsInstance<Success<Xvm>>()
            .flatMap { it.value.textures }
    }

    private suspend fun loadNpcBodyPart(slug: String, bodyPart: String): NjObject {
        val buffer = assetLoader.loadArrayBuffer("/player/${slug}${bodyPart}.nj")
        return parseNj(buffer.cursor(Endianness.Little)).unwrap().first()
    }

    private fun npcSlugOrThrow(extraModel: Int): String =
        NPC_SLUGS.getOrNull(extraModel)
            ?: throw IllegalArgumentException("Invalid NPC extra_model: $extraModel")

    suspend fun loadXvrTextures(
        char: CharacterClass,
        sectionId: SectionId,
        body: Int,
    ): List<XvrTexture?> =
        loadXvrTextures(char, sectionId, body, skin = 0, face = 0, head = 0)

    suspend fun loadXvrTextures(
        char: CharacterClass,
        sectionId: SectionId,
        body: Int,
        skin: Int,
        face: Int,
        head: Int,
    ): List<XvrTexture?> {
        val xvrTextures = xvrTextureCache.get(char)
        val texIds = textureIds(char, sectionId, body, skin, face, head)

        return listOf(
            texIds.sectionId,
            *texIds.body,
            *texIds.head,
            *texIds.hair,
            *texIds.accessories,
        ).map { it?.let(xvrTextures::get) }
    }

    /**
     * Loads the separate body parts and joins them together at the right bones.
     */
    private suspend fun loadBodyParts(char: CharacterClass): NjObject {
        val texIds = textureIds(char, SectionId.Viridia, 0)

        val body = loadBodyPart(char, "Body")
        val head = loadBodyPart(char, "Head", no = 0)
        // Shift by 1 for the section ID and once for every body texture ID.
        var shift = 1 + texIds.body.size
        shiftTextureIds(head, shift)
        addToBone(body, head, parentBoneId = 59)

        if (char.hairStyleCount == 0) {
            return body
        }

        val hair = loadBodyPart(char, "Hair", no = 0)
        shift += texIds.head.size
        shiftTextureIds(hair, shift)
        addToBone(head, hair, parentBoneId = 0)

        if (0 !in char.hairStylesWithAccessory) {
            return body
        }

        val accessory = loadBodyPart(char, "Accessory", no = 0)
        shift += texIds.hair.size
        shiftTextureIds(accessory, shift)
        addToBone(hair, accessory, parentBoneId = 0)

        return body
    }

    private suspend fun loadBodyPart(
        char: CharacterClass,
        bodyPart: String,
        no: Int? = null,
    ): NjObject {
        val buffer = assetLoader.loadArrayBuffer("/player/${char.slug}${bodyPart}${no ?: ""}.nj")
        return parseNj(buffer.cursor(Endianness.Little)).unwrap().first()
    }

    /**
     * Shift texture IDs so that the IDs of different body parts don't overlap.
     */
    private fun shiftTextureIds(njObject: NjObject, shift: Int) {
        njObject.model?.let { model ->
            for (mesh in model.meshes) {
                mesh.textureId = mesh.textureId?.plus(shift)
            }
        }

        for (child in njObject.children) {
            shiftTextureIds(child, shift)
        }
    }

    private fun addToBone(
        obj: NjObject,
        child: NjObject,
        parentBoneId: Int,
    ) {
        obj.getBone(parentBoneId)?.let { bone ->
            bone.evaluationFlags.hidden = false
            bone.evaluationFlags.breakChildTrace = false
            bone.addChild(child)
        }
    }

    private suspend fun loadTextures(char: CharacterClass): List<XvrTexture?> {
        val buffer = assetLoader.loadArrayBuffer("/player/${char.slug}Tex.afs")
        val afsResult = parseAfs(buffer.cursor(Endianness.Little))

        if (afsResult !is Success) {
            return emptyList()
        }

        return afsResult.value
            .map { parseXvm(it.cursor()) }
            .filterIsInstance<Success<Xvm>>()
            .flatMap { it.value.textures }
    }

    /**
     * Computes AFS texture indices for each body part.
     * Head texture indices depend on skin, face, and head style (from qedit Faceway/FaceOff).
     */
    private fun textureIds(
        char: CharacterClass,
        sectionId: SectionId,
        body: Int,
        skin: Int = 0,
        face: Int = 0,
        head: Int = 0,
    ): TextureIds =
        when (char) {
            HUmar -> {
                // Faceway=0, FaceOff=54
                val bodyIdx = body * 3
                val fo = 54 + face * 8 + skin * 2
                TextureIds(
                    sectionId = sectionId.ordinal + 126,
                    body = arrayOf(bodyIdx, bodyIdx + 1, bodyIdx + 2, body + 108),
                    head = arrayOf(fo, fo + 1),
                    hair = arrayOf(94, 95),
                    accessories = arrayOf(),
                )
            }
            HUnewearl -> {
                // Faceway=1, FaceOff=235
                val bodyIdx = body * 13
                TextureIds(
                    sectionId = sectionId.ordinal + 299,
                    body = arrayOf(
                        bodyIdx + 13, bodyIdx, bodyIdx + 1, bodyIdx + 2, bodyIdx + 3,
                        277, body + 281,
                    ),
                    head = arrayOf(235 + skin, 235 + face * 4 + skin + 4),
                    hair = arrayOf(260, 259),
                    accessories = arrayOf(),
                )
            }
            HUcast -> {
                // Faceway=7, FaceOff=125
                val bodyIdx = body * 5
                var y = 125 + body + head * 25
                if (head == 0) y += body else y += 25
                TextureIds(
                    sectionId = sectionId.ordinal + 275,
                    body = arrayOf(bodyIdx, bodyIdx + 1, bodyIdx + 2, body + 250),
                    head = arrayOf(y, 164),
                    hair = arrayOf(),
                    accessories = arrayOf(),
                )
            }
            HUcaseal -> {
                // Faceway=4, FaceOff=125
                val bodyIdx = body * 5
                var y = 125 + body * 2 + head * 50
                if (head > 0) y -= 50
                if (head == 3) y += body
                else if (head > 3) y += 25
                if (head == 4) y += body
                TextureIds(
                    sectionId = sectionId.ordinal + 375,
                    body = arrayOf(bodyIdx, bodyIdx + 1, bodyIdx + 2),
                    head = arrayOf(y, y + 1, y + 2),
                    hair = arrayOf(),
                    accessories = arrayOf(),
                )
            }
            RAmar -> {
                // Faceway=0, FaceOff=126
                val bodyIdx = body * 7
                val fo = 126 + face * 8 + skin * 2
                TextureIds(
                    sectionId = sectionId.ordinal + 197,
                    body = arrayOf(bodyIdx + 4, bodyIdx + 5, bodyIdx + 6, body + 179),
                    head = arrayOf(fo, fo + 1),
                    hair = arrayOf(166, 167),
                    accessories = arrayOf(null, null, bodyIdx + 2),
                )
            }
            RAmarl -> {
                // Faceway=2, FaceOff=288
                val bodyIdx = body * 16
                TextureIds(
                    sectionId = sectionId.ordinal + 322,
                    body = arrayOf(bodyIdx + 15, bodyIdx + 1, bodyIdx),
                    head = arrayOf(288 + face * 4 + skin),
                    hair = arrayOf(308, 309),
                    accessories = arrayOf(null, null, bodyIdx + 8),
                )
            }
            RAcast -> {
                // Faceway=7, FaceOff=125
                val bodyIdx = body * 5
                var y = 125 + body + head * 25
                if (head == 0) y += body else y += 25
                if (head == 2) y += body
                else if (head > 2) y += 25
                TextureIds(
                    sectionId = sectionId.ordinal + 300,
                    body = arrayOf(bodyIdx, bodyIdx + 1, bodyIdx + 2, bodyIdx + 3, body + 275),
                    head = arrayOf(y),
                    hair = arrayOf(),
                    accessories = arrayOf(),
                )
            }
            RAcaseal -> {
                // Faceway=6, FaceOff=125
                val bodyIdx = body * 5
                var y = 125 + body * 2 + head * 50
                if (head == 0) y += body else y += 25
                if (head == 4) y -= body
                TextureIds(
                    sectionId = sectionId.ordinal + 375,
                    body = arrayOf(body + 350, bodyIdx, bodyIdx + 1, bodyIdx + 2),
                    head = arrayOf(y, y + 1, y + 2),
                    hair = arrayOf(bodyIdx + 4),
                    accessories = arrayOf(),
                )
            }
            FOmar -> {
                // Faceway=5, FaceOff=270
                val bodyIdx = if (body == 0) 0 else body * 15 + 2
                TextureIds(
                    sectionId = sectionId.ordinal + 310,
                    body = arrayOf(bodyIdx + 12, bodyIdx + 13, bodyIdx + 14, bodyIdx),
                    head = arrayOf(270 + face * 4 + skin + 4, 270 + skin),
                    hair = arrayOf(null, 296, 297),
                    accessories = arrayOf(bodyIdx + 4),
                )
            }
            FOmarl -> {
                // Faceway=2, FaceOff=288
                val bodyIdx = body * 16
                TextureIds(
                    sectionId = sectionId.ordinal + 326,
                    body = arrayOf(bodyIdx, bodyIdx + 2, bodyIdx + 1, 322 /*hands*/),
                    head = arrayOf(288 + face * 4 + skin),
                    hair = arrayOf(null, null, 308),
                    accessories = arrayOf(bodyIdx + 3, bodyIdx + 4),
                )
            }
            FOnewm -> {
                // Faceway=1, FaceOff=306
                val bodyIdx = body * 17
                TextureIds(
                    sectionId = sectionId.ordinal + 344,
                    body = arrayOf(bodyIdx + 4, 340 /*hands*/, bodyIdx, bodyIdx + 5),
                    head = arrayOf(306 + skin, 306 + face * 4 + skin + 4),
                    hair = arrayOf(null, null, 330),
                    accessories = arrayOf(bodyIdx + 6, bodyIdx + 16, 330),
                )
            }
            FOnewearl -> {
                // Faceway=5, FaceOff=468
                val bodyIdx = body * 26
                TextureIds(
                    sectionId = sectionId.ordinal + 505,
                    body = arrayOf(bodyIdx + 1, bodyIdx, bodyIdx + 2, 501 /*hands*/),
                    head = arrayOf(468 + face * 4 + skin + 4, 468 + skin),
                    hair = arrayOf(null, null, 492),
                    accessories = arrayOf(bodyIdx + 12, bodyIdx + 13),
                )
            }
        }

    companion object {
        /** File name prefixes for NPC models, indexed by extra_model (0=GM .. 6=Elly). */
        private val NPC_SLUGS = arrayOf("NpcGM", "NpcRico", "NpcSonic", "NpcKnux", "NpcTails", "NpcFlowen", "NpcElly")

        /** Number of available NPC models (for extra_model). */
        val NPC_MODEL_COUNT: Int = NPC_SLUGS.size
    }

    private class TextureIds(
        val sectionId: Int,
        val body: Array<Int>,
        val head: Array<Int>,
        val hair: Array<Int?>,
        val accessories: Array<Int?>,
    )
}
