package world.phantasmal.web.viewer.loading

import world.phantasmal.core.Success
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.fileFormats.ninja.*
import world.phantasmal.lib.fileFormats.parseAfs
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.questEditor.loading.LoadingCache
import world.phantasmal.web.shared.dto.SectionId
import world.phantasmal.web.viewer.models.CharacterClass
import world.phantasmal.web.viewer.models.CharacterClass.*
import world.phantasmal.webui.DisposableContainer

class CharacterClassAssetLoader(private val assetLoader: AssetLoader) : DisposableContainer() {
    private val ninjaObjectCache: LoadingCache<CharacterClass, NinjaObject<NjModel>> =
        addDisposable(LoadingCache(::loadBodyParts) { /* Nothing to dispose. */ })

    private val xvrTextureCache: LoadingCache<CharacterClass, List<XvrTexture?>> =
        addDisposable(LoadingCache(::loadTextures) { /* Nothing to dispose. */ })

    suspend fun loadNinjaObject(char: CharacterClass): NinjaObject<NjModel> =
        ninjaObjectCache.get(char)

    suspend fun loadXvrTextures(
        char: CharacterClass,
        sectionId: SectionId,
        body: Int,
    ): List<XvrTexture?> {
        val xvrTextures = xvrTextureCache.get(char)
        val texIds = textureIds(char, sectionId, body)

        return listOf(
            texIds.section_id,
            *texIds.body,
            *texIds.head,
            *texIds.hair,
            *texIds.accessories,
        ).map { it?.let(xvrTextures::get) }
    }

    /**
     * Loads the separate body parts and joins them together at the right bones.
     */
    private suspend fun loadBodyParts(char: CharacterClass): NinjaObject<NjModel> {
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
    ): NinjaObject<NjModel> {
        val buffer = assetLoader.loadArrayBuffer("/player/${char.slug}${bodyPart}${no ?: ""}.nj")
        return parseNj(buffer.cursor(Endianness.Little)).unwrap().first()
    }

    /**
     * Shift texture IDs so that the IDs of different body parts don't overlap.
     */
    private fun shiftTextureIds(njObject: NinjaObject<NjModel>, shift: Int) {
        njObject.model?.let { model ->
            for (mesh in model.meshes) {
                mesh.textureId = mesh.textureId?.plus(shift)
            }
        }

        for (child in njObject.children) {
            shiftTextureIds(child, shift)
        }
    }

    private fun <M : NinjaModel> addToBone(
        obj: NinjaObject<M>,
        child: NinjaObject<M>,
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

    private fun textureIds(char: CharacterClass, sectionId: SectionId, body: Int): TextureIds =
        when (char) {
            HUmar -> {
                val bodyIdx = body * 3
                TextureIds(
                    section_id = sectionId.ordinal + 126,
                    body = arrayOf(bodyIdx, bodyIdx + 1, bodyIdx + 2, body + 108),
                    head = arrayOf(54, 55),
                    hair = arrayOf(94, 95),
                    accessories = arrayOf(),
                )
            }
            HUnewearl -> {
                val bodyIdx = body * 13
                TextureIds(
                    section_id = sectionId.ordinal + 299,
                    body = arrayOf(
                        bodyIdx + 13,
                        bodyIdx,
                        bodyIdx + 1,
                        bodyIdx + 2,
                        bodyIdx + 3,
                        277,
                        body + 281,
                    ),
                    head = arrayOf(235, 239),
                    hair = arrayOf(260, 259),
                    accessories = arrayOf(),
                )
            }
            HUcast -> {
                val bodyIdx = body * 5
                TextureIds(
                    section_id = sectionId.ordinal + 275,
                    body = arrayOf(bodyIdx, bodyIdx + 1, bodyIdx + 2, body + 250),
                    head = arrayOf(bodyIdx + 3, bodyIdx + 4),
                    hair = arrayOf(),
                    accessories = arrayOf(),
                )
            }
            HUcaseal -> {
                val bodyIdx = body * 5
                TextureIds(
                    section_id = sectionId.ordinal + 375,
                    body = arrayOf(bodyIdx, bodyIdx + 1, bodyIdx + 2),
                    head = arrayOf(bodyIdx + 3, bodyIdx + 4),
                    hair = arrayOf(),
                    accessories = arrayOf(),
                )
            }
            RAmar -> {
                val bodyIdx = body * 7
                TextureIds(
                    section_id = sectionId.ordinal + 197,
                    body = arrayOf(bodyIdx + 4, bodyIdx + 5, bodyIdx + 6, body + 179),
                    head = arrayOf(126, 127),
                    hair = arrayOf(166, 167),
                    accessories = arrayOf(null, null, bodyIdx + 2),
                )
            }
            RAmarl -> {
                val bodyIdx = body * 16
                TextureIds(
                    section_id = sectionId.ordinal + 322,
                    body = arrayOf(bodyIdx + 15, bodyIdx + 1, bodyIdx),
                    head = arrayOf(288),
                    hair = arrayOf(308, 309),
                    accessories = arrayOf(null, null, bodyIdx + 8),
                )
            }
            RAcast -> {
                val bodyIdx = body * 5
                TextureIds(
                    section_id = sectionId.ordinal + 300,
                    body = arrayOf(bodyIdx, bodyIdx + 1, bodyIdx + 2, bodyIdx + 3, body + 275),
                    head = arrayOf(bodyIdx + 4),
                    hair = arrayOf(),
                    accessories = arrayOf(),
                )
            }
            RAcaseal -> {
                val bodyIdx = body * 5
                TextureIds(
                    section_id = sectionId.ordinal + 375,
                    body = arrayOf(body + 350, bodyIdx, bodyIdx + 1, bodyIdx + 2),
                    head = arrayOf(bodyIdx + 3),
                    hair = arrayOf(bodyIdx + 4),
                    accessories = arrayOf(),
                )
            }
            FOmar -> {
                val bodyIdx = if (body == 0) 0 else body * 15 + 2
                TextureIds(
                    section_id = sectionId.ordinal + 310,
                    body = arrayOf(bodyIdx + 12, bodyIdx + 13, bodyIdx + 14, bodyIdx),
                    head = arrayOf(276, 272),
                    hair = arrayOf(null, 296, 297),
                    accessories = arrayOf(bodyIdx + 4),
                )
            }
            FOmarl -> {
                val bodyIdx = body * 16
                TextureIds(
                    section_id = sectionId.ordinal + 326,
                    body = arrayOf(bodyIdx, bodyIdx + 2, bodyIdx + 1, 322 /*hands*/),
                    head = arrayOf(288),
                    hair = arrayOf(null, null, 308),
                    accessories = arrayOf(bodyIdx + 3, bodyIdx + 4),
                )
            }
            FOnewm -> {
                val bodyIdx = body * 17
                TextureIds(
                    section_id = sectionId.ordinal + 344,
                    body = arrayOf(bodyIdx + 4, 340 /*hands*/, bodyIdx, bodyIdx + 5),
                    head = arrayOf(306, 310),
                    hair = arrayOf(null, null, 330),
                    // ID 16 for glasses is incorrect but looks decent.
                    accessories = arrayOf(bodyIdx + 6, bodyIdx + 16, 330),
                )
            }
            FOnewearl -> {
                val bodyIdx = body * 26
                TextureIds(
                    section_id = sectionId.ordinal + 505,
                    body = arrayOf(bodyIdx + 1, bodyIdx, bodyIdx + 2, 501 /*hands*/),
                    head = arrayOf(472, 468),
                    hair = arrayOf(null, null, 492),
                    accessories = arrayOf(bodyIdx + 12, bodyIdx + 13),
                )
            }
        }

    private class TextureIds(
        val section_id: Int,
        val body: Array<Int>,
        val head: Array<Int>,
        val hair: Array<Int?>,
        val accessories: Array<Int?>,
    )
}
