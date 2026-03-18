package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.asm.*
import world.phantasmal.psolib.asm.dataFlowAnalysis.FloorMapping
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.fileFormats.parseGslIndex
import world.phantasmal.psolib.test.LibTestSuite
import java.io.File
import java.io.RandomAccessFile
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Generates default quest .qst files from PSO BB city area data files.
 *
 * Each episode's city lobby (Pioneer II / Lab / City02) bin + dat files are loaded from the
 * Ephinea data directory, with fallback to data.gsl for files not found as loose files.
 * English bin variants (_e) are preferred over Japanese (_j) for proper English NPC dialogue.
 *
 * The generator injects set_episode, bb_map_designate, and set_floor_handler into label 0,
 * then calls the original freeroam label 0 code via a trampoline label.
 *
 * Run to regenerate:
 *   ./gradlew :psolib:jvmTest --tests "*GenerateDefaultQuests*"
 */
class GenerateDefaultQuests : LibTestSuite {

    private val dataDir = File("/Users/wangzhen/Documents/data")

    private data class CityDef(
        val episode: Episode,
        val episodeId: Int, // 0=EP1, 1=EP2, 2=EP4
        val floor: Int,
        /** Bin candidates in preference order (English first, then Japanese). */
        val binCandidates: List<String>,
        val objDat: String,
        val npcDat: String,
        val mapId: Int,
        val cityName: String,
    )

    private val cities = listOf(
        CityDef(
            Episode.I, 0, 0,
            listOf("map_city_on_e.bin", "map_city_on_j.bin"),
            "map_city00_00o.dat", "map_city00_00e.dat", 0x00, "Pioneer II",
        ),
        CityDef(
            Episode.II, 1, 0,
            listOf("map_labo_on_e.bin", "map_labo_on_j.bin"),
            "map_labo00_00o.dat", "map_labo00_00e.dat", 0x12, "Lab",
        ),
        CityDef(
            Episode.IV, 2, 0,
            listOf("map_city02_on_e.bin", "map_city02_on_j.bin"),
            "map_city02_00_00o.dat", "map_city02_00_00e.dat", 0x2D, "Pioneer II",
        ),
    )

    /** Lazy-initialized GSL index for fallback reads. */
    private val gslIndex: Map<String, Pair<Int, Int>> by lazy { loadGslIndex() }

    private data class LobbyDef(
        val variant: Int,
        val objDat: String,
        val label: String,
    )

    private val lobbies = listOf(
        LobbyDef(1, "map_lobby_01o.dat", "Lobby 01"),
        LobbyDef(2, "map_lobby_02o.dat", "Lobby 02"),
        LobbyDef(3, "map_lobby_03o.dat", "Lobby 03"),
        LobbyDef(4, "map_lobby_04o.dat", "Lobby 04"),
        LobbyDef(5, "map_lobby_05o.dat", "Lobby 05"),
        LobbyDef(6, "map_lobby_06o.dat", "Lobby 06"),
        LobbyDef(7, "map_lobby_07o.dat", "Lobby 07"),
        LobbyDef(8, "map_lobby_08o.dat", "Lobby 08"),
        LobbyDef(9, "map_lobby_09o.dat", "Lobby 09"),
        LobbyDef(10, "map_lobby_10o.dat", "Lobby 10"),
        LobbyDef(11, "map_lobby_green_be00o.dat", "EP3 Lobby Green"),
        LobbyDef(12, "map_lobby_red_be00o.dat", "EP3 Lobby Red"),
        LobbyDef(13, "map_lobby_yellow_be00o.dat", "EP3 Lobby Yellow"),
        LobbyDef(14, "map_lobby_soccer01o.dat", "Lobby Soccer 1"),
        LobbyDef(15, "map_lobby_soccer02o.dat", "Lobby Soccer 2"),
    )

    @Test
    fun generate_default_quests() {
        assertTrue(dataDir.isDirectory, "Data directory not found: $dataDir")

        for (city in cities) {
            generateDefaultQuest(city)
        }

        for (lobby in lobbies) {
            generateLobbyQuest(lobby)
        }
    }

    private fun generateDefaultQuest(city: CityDef) {
        val episode = city.episode
        val epInt = episode.toInt()

        // Find the best bin file (prefer English).
        val (binBuf, binName) = findBinFile(city.binCandidates)
        val isShiftJis = binName.contains("_j")

        // Read per-floor dat files and synthesize a combined DAT.
        val objBuf = readDataFile(city.objDat)
        val npcBuf = readDataFile(city.npcDat)
        val datBuf = synthesizeDat(city.floor, objBuf, npcBuf)

        // Parse the combined quest.
        val quest = parseBinDatToQuest(
            binCursor = binBuf.cursor(),
            datCursor = datBuf.cursor(),
            lenient = true,
            compressed = false,
            shiftJis = isShiftJis,
        ).unwrap()

        // Fix up episode and NPC episode.
        quest.episode = episode
        if (quest.language < 0) quest.language = 0
        for (npc in quest.npcs) { npc.episode = episode }

        // Clear non-standard bytecodeOffset so writeBin uses the standard BB offset.
        quest.bytecodeOffset = null

        // English metadata.
        quest.name = "${city.cityName} (EP${epInt})"
        quest.shortDescription = "Episode ${epInt} ${city.cityName}"
        quest.longDescription = "Default quest for Episode ${epInt}.\n${city.cityName} lobby area."

        // Inject set_episode + bb_map_designate + set_floor_handler into label 0.
        injectLabel0Setup(quest, city)

        // Translate Japanese script strings to English (for bins that only have _j).
        if (isShiftJis) {
            translateStrings(quest)
        }

        // Ensure floor mappings include the city floor.
        if (quest.floorMappings.none { it.floorId == city.floor }) {
            quest.floorMappings = listOf(
                FloorMapping(city.floor, city.mapId, city.floor, 0, episode)
            )
        }

        // Write to .qst file.
        val qstBuffer = writeQuestToQst(
            quest,
            "default_ep_$epInt",
            Version.BB,
            online = true,
        )

        val projectRoot = File(System.getProperty("user.dir")).let {
            if (it.name == "psolib") it.parentFile else it
        }
        val outputDir = File(projectRoot, "web/src/jsMain/resources/assets/quests/defaults")
        outputDir.mkdirs()
        val outputFile = File(outputDir, "city_ep_$epInt.qst")
        outputFile.writeBytes(qstBuffer.byteArray.copyOf(qstBuffer.size))

        println("Generated: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
        println("  Bin: $binName (shiftJis=$isShiftJis)")
        println("  Episode: $episode, City: ${city.cityName}")
        println("  Objects: ${quest.objects.size}, NPCs: ${quest.npcs.size}")
        println("  Segments: ${quest.bytecodeIr.segments.size}")
        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 100, "Generated file too small")
    }

    private fun generateLobbyQuest(lobby: LobbyDef) {
        val objBuf = tryReadDataFile(lobby.objDat)
        if (objBuf == null) {
            println("SKIP: ${lobby.objDat} not found")
            return
        }

        val floorId = 15 // Lobby is always floor 15 in EP1
        val mapId = 0x0F
        val datBuf = synthesizeDat(floorId, objBuf, Buffer.withSize(0, Endianness.Little))
        val dat = parseDat(datBuf.cursor())

        val objects = dat.objs.mapTo(mutableListOf()) { QuestObject(it.areaId, it.data) }

        // Build minimal bytecode: set_episode 0, bb_map_designate, set_floor_handler, ret.
        val floorHandlerLabel = 10000
        val bytecodeIr = BytecodeIr(listOf(
            InstructionSegment(
                mutableListOf(0),
                mutableListOf(
                    Instruction(OP_SET_EPISODE, listOf(IntArg(0)), valid = true, srcLoc = null),
                    Instruction(
                        OP_BB_MAP_DESIGNATE,
                        listOf(IntArg(floorId), IntArg(mapId), IntArg(0), IntArg(0)),
                        valid = true, srcLoc = null,
                    ),
                    Instruction(
                        OP_SET_FLOOR_HANDLER,
                        listOf(IntArg(floorId), IntArg(floorHandlerLabel)),
                        valid = true, srcLoc = null,
                    ),
                    Instruction(OP_RET, emptyList(), valid = true, srcLoc = null),
                ),
            ),
            InstructionSegment(
                mutableListOf(floorHandlerLabel),
                mutableListOf(Instruction(OP_RET, emptyList(), valid = true, srcLoc = null)),
            ),
        ))

        val quest = Quest(
            id = 0,
            language = 0,
            name = lobby.label,
            shortDescription = lobby.label,
            longDescription = "${lobby.label}\nPSO Episode I lobby area.",
            episode = Episode.I,
            objects = objects,
            npcs = mutableListOf(),
            events = mutableListOf(),
            datUnknowns = mutableListOf(),
            bytecodeIr = bytecodeIr,
            shopItems = uintArrayOf(),
            floorMappings = listOf(FloorMapping(floorId, mapId, floorId, 0, Episode.I)),
        )

        val qstBuffer = writeQuestToQst(quest, "lobby_${lobby.variant}", Version.BB, online = true)

        val projectRoot = File(System.getProperty("user.dir")).let {
            if (it.name == "psolib") it.parentFile else it
        }
        val outputDir = File(projectRoot, "web/src/jsMain/resources/assets/quests/defaults")
        outputDir.mkdirs()
        val outputFile = File(outputDir, "lobby_${String.format("%02d", lobby.variant)}.qst")
        outputFile.writeBytes(qstBuffer.byteArray.copyOf(qstBuffer.size))

        println("Generated: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
        println("  ${lobby.label}: Objects=${quest.objects.size}")
        assertTrue(outputFile.exists())
    }

    /**
     * Inject set_episode, bb_map_designate, and set_floor_handler at the start of label 0.
     * The original freeroam label 0 code is relocated to a trampoline label and called.
     */
    private fun injectLabel0Setup(quest: Quest, city: CityDef) {
        val segments = quest.bytecodeIr.segments.toMutableList()
        val allLabels = segments.flatMap { it.labels }.toSet()

        // Find two free labels: one for trampoline, one for floor handler.
        val trampolineLabel = findFreeLabel(allLabels)
        val floorHandlerLabel = findFreeLabel(allLabels + trampolineLabel)

        // Relocate existing label 0 to trampoline.
        val label0Idx = segments.indexOfFirst { it is InstructionSegment && 0 in it.labels }
        if (label0Idx >= 0) {
            val oldSeg = segments[label0Idx] as InstructionSegment
            oldSeg.labels.remove(0)
            oldSeg.labels.add(trampolineLabel)
            oldSeg.labels.sort()
        }

        // Build new label 0: set_episode, bb_map_designate, set_floor_handler, call trampoline, ret.
        val setupInstructions = mutableListOf<Instruction>()

        // set_episode N
        setupInstructions.add(
            Instruction(OP_SET_EPISODE, listOf(IntArg(city.episodeId)), valid = true, srcLoc = null)
        )

        // bb_map_designate floor, mapId, variant, 0
        setupInstructions.add(
            Instruction(
                OP_BB_MAP_DESIGNATE,
                listOf(IntArg(city.floor), IntArg(city.mapId), IntArg(0), IntArg(0)),
                valid = true, srcLoc = null,
            )
        )

        // set_floor_handler 0, floorHandlerLabel
        setupInstructions.add(
            Instruction(
                OP_SET_FLOOR_HANDLER,
                listOf(IntArg(city.floor), IntArg(floorHandlerLabel)),
                valid = true, srcLoc = null,
            )
        )

        // Call original label 0 code.
        if (label0Idx >= 0) {
            setupInstructions.add(
                Instruction(OP_CALL, listOf(IntArg(trampolineLabel)), valid = true, srcLoc = null)
            )
        }

        setupInstructions.add(
            Instruction(OP_RET, emptyList(), valid = true, srcLoc = null)
        )

        val newLabel0Seg = InstructionSegment(mutableListOf(0), setupInstructions)

        // Floor handler segment: just ret.
        val floorHandlerSeg = InstructionSegment(
            mutableListOf(floorHandlerLabel),
            mutableListOf(Instruction(OP_RET, emptyList(), valid = true, srcLoc = null)),
        )

        segments.add(0, newLabel0Seg)
        segments.add(floorHandlerSeg)
        quest.bytecodeIr = BytecodeIr(segments)
    }

    /** Replace Japanese string args with English translations throughout the bytecode IR. */
    private fun translateStrings(quest: Quest) {
        val segments = quest.bytecodeIr.segments.toMutableList()

        for (segIdx in segments.indices) {
            val seg = segments[segIdx]
            if (seg !is InstructionSegment) continue

            for (instIdx in seg.instructions.indices) {
                val inst = seg.instructions[instIdx]
                var changed = false
                val newArgs = inst.args.map { arg ->
                    if (arg is StringArg && arg.value in CITY02_TRANSLATIONS) {
                        changed = true
                        StringArg(CITY02_TRANSLATIONS.getValue(arg.value))
                    } else arg
                }
                if (changed) {
                    seg.instructions[instIdx] = Instruction(inst.opcode, newArgs, inst.valid, inst.srcLoc)
                }
            }
        }

        quest.bytecodeIr = BytecodeIr(segments)
    }

    /** Find the smallest unused label starting from a high range to avoid conflicts. */
    private fun findFreeLabel(usedLabels: Set<Int>): Int {
        // Start from 10000 to stay well clear of any freeroam labels (typically 0-994).
        var label = 10000
        while (label in usedLabels) label++
        return label
    }

    /** Find the first available bin file from candidates, checking loose files then GSL. */
    private fun findBinFile(candidates: List<String>): Pair<Buffer, String> {
        for (name in candidates) {
            val buf = tryReadDataFile(name)
            if (buf != null) return Pair(buf, name)
        }
        error("No bin file found. Tried: $candidates")
    }

    /** Try to read a file from the data directory, falling back to data.gsl. */
    private fun tryReadDataFile(name: String): Buffer? {
        val looseFile = File(dataDir, name)
        if (looseFile.exists()) {
            return Buffer.fromByteArray(looseFile.readBytes(), Endianness.Little)
        }
        // Fall back to data.gsl.
        val (offset, size) = gslIndex[name] ?: return null
        val raf = RandomAccessFile(File(dataDir, "data.gsl"), "r")
        raf.use {
            it.seek(offset.toLong())
            val bytes = ByteArray(size)
            it.readFully(bytes)
            return Buffer.fromByteArray(bytes, Endianness.Little)
        }
    }

    private fun readDataFile(name: String): Buffer {
        return tryReadDataFile(name)
            ?: error("Data file not found: $name (checked loose files and data.gsl)")
    }

    private fun loadGslIndex(): Map<String, Pair<Int, Int>> {
        val gslFile = File(dataDir, "data.gsl")
        if (!gslFile.exists()) return emptyMap()

        // Read enough for the header (128 KB).
        val headerSize = minOf(gslFile.length(), 128L * 1024).toInt()
        val headerBytes = ByteArray(headerSize)
        RandomAccessFile(gslFile, "r").use { it.readFully(headerBytes) }

        val buf = Buffer.fromByteArray(headerBytes, Endianness.Little)
        val entries = parseGslIndex(buf.cursor(), gslFile.length())
        return entries.associate { it.name to Pair(it.offset, it.size) }
    }

    private fun synthesizeDat(floorId: Int, objData: Buffer, npcData: Buffer): Buffer {
        val headerSize = 16
        val objSize = objData.size
        val npcSize = npcData.size

        val totalSize = (if (objSize > 0) headerSize + objSize else 0) +
                (if (npcSize > 0) headerSize + npcSize else 0) +
                headerSize // terminator

        val buf = Buffer.withSize(totalSize, Endianness.Little)
        var offset = 0

        if (objSize > 0) {
            buf.setInt(offset, 1)
            buf.setInt(offset + 4, headerSize + objSize)
            buf.setInt(offset + 8, floorId)
            buf.setInt(offset + 12, objSize)
            offset += headerSize
            objData.copyInto(buf, destinationOffset = offset)
            offset += objSize
        }

        if (npcSize > 0) {
            buf.setInt(offset, 2)
            buf.setInt(offset + 4, headerSize + npcSize)
            buf.setInt(offset + 8, floorId)
            buf.setInt(offset + 12, npcSize)
            offset += headerSize
            npcData.copyInto(buf, destinationOffset = offset)
            offset += npcSize
        }

        return buf
    }

    companion object {
        /**
         * EP4 City02 NPC dialogue translations (Japanese → English).
         * The original map_city02_on_j.bin has no English version in the game data.
         */
        private val CITY02_TRANSLATIONS = mapOf(
            "おかしいなぁ。\n我々の計算では あの隕石は\nラグオルに衝突するはずがないのに。"
                    to "How strange...\nOur calculations showed that\nmeteorite shouldn't have hit Ragol.",
            "隕石の落下以来、\n本当に大忙し。\n情報も足りない。人手も足りない。"
                    to "Ever since the meteorite fell,\nwe've been swamped.\nNot enough intel, not enough hands.",
            "ああ・・・早く知りたいなぁ。"
                    to "Ahh... I just want answers.",
            "知っているか？\nクローズドベータトライアルでは\n物語の詳細は語られないんだって。"
                    to "Did you know?\nThey say the full story won't\nbe revealed in the beta.",
            "・・・早く知りたいなぁ。"
                    to "...I just want to know already.",
            "正式版でも\n私はここに登場できるのだろうか。"
                    to "I wonder if I'll still be\nstanding here in the final release.",
            "ようやくエピソード４って、\n待ちわびた あの新作でしょ？"
                    to "Episode 4 at last!\nThis is that long-awaited\nnew chapter, right?",
            "やっとお披露目なんて\n私なんか 大騒ぎ！"
                    to "Finally unveiled!\nI'm so excited!",
            "これからの毎日は\n気が気じゃないでしょうね。"
                    to "I bet every day from now on\nis going to be nerve-wracking.",
            "隕石がラグオルに\n衝突したんだって？\nパイオニア２をかすめたんだって？"
                    to "A meteorite hit Ragol?\nAnd it nearly grazed\nPioneer 2?",
            "びっくりしたなぁ。\nすごい衝撃だったなあ。"
                    to "That was a real shock.\nQuite the impact.",
            "ちょっと小耳に\n挟んだ噂なんだけど、"
                    to "I heard a little rumor...",
            "エピソード４の\nクローズドベータトライアル\n実施中らしいわ。"
                    to "Apparently the Episode 4\nclosed beta trial\nis underway.",
            "本当かしら？"
                    to "I wonder if it's true?",
            "疲れて傷ついた体は\nメディカルセンターで リフレッシュ！"
                    to "Rest your weary, battered body\nat the Medical Center!",
            "私たちナースは、\nエピソード４でも がんばるわ！"
                    to "We nurses will keep doing\nour best in Episode 4 too!",
            "エピソード４\nクローズドベータトライアルへの\nご参加ありがとうございます。"
                    to "Thank you for participating\nin the Episode 4 closed\nbeta trial.",
            "惑星ラグオルでは\n見たこともない生物が\n暴れまわっているとのこと。"
                    to "Apparently unknown creatures\nare running amok on Ragol.",
            "どうか お気をつけて。"
                    to "Please be careful out there.",
            "エピソード４\nクローズドベータトライアルへ\nようこそ！"
                    to "Welcome to the Episode 4\nclosed beta trial!",
            "惑星ラグオルの新しい地を\n存分にお楽しみください！"
                    to "Enjoy the new lands\nof Ragol to the fullest!",
            "クローズドベータトライアル中は、\nイロイロ変わるみたいね。"
                    to "Things might change a lot\nduring the beta, it seems.",
            "はじめましてぇ。\n新しく総督府の受付事務員として\n配属となりました…"
                    to "Nice to meet you!\nI've just been assigned as the\nnew receptionist at the Governor's office...",
            "<color 5>モモカ<color 0>です。"
                    to "I'm <color 5>Momoka<color 0>.",
            "ふつつかものですが、\n今後とも よろしくお願いしますねぇ。"
                    to "I may be a bit clumsy,\nbut I look forward to\nworking with you!",
            "私が 総督のタイレルだ。"
                    to "I am Principal Tyrell.",
            "…なにか 用か？"
                    to "...Is there something\nyou need?",
            "用があるなら、そこのモモカを\n通してくれたまえ。"
                    to "If you need something,\nplease speak to Momoka.",
            "調子は どうだ？"
                    to "How are things going?",
            "君には 期待しているんでな。"
                    to "I have high hopes for you.",
            "依頼の方、よろしくたのむぞ。"
                    to "I'm counting on you\nfor the mission.",
            "こんにちは。\n私はパイオニア２総督府に\n所属する事務官。"
                    to "Hello.\nI'm an official at the\nPioneer 2 Governor's office.",
            "秘書のアイリーンです。"
                    to "I'm Irene, the secretary.",
            "なにか ご用でしょうか？"
                    to "Is there anything\nI can help you with?",
            "ご用でしたら 受付のモモカに\nお話しください。"
                    to "If you need anything,\nplease talk to Momoka\nat the front desk.",
            "こんにちは。\n調子のほうは どうですか？"
                    to "Hello. How are you doing?",
            "みなさんが あなたに\n期待しています。"
                    to "Everyone is counting on you.",
            "依頼をどうか\nよろしくお願いします。"
                    to "Please do your best\non the missions.",
            "こんにちは！ <color 4>リリス<color 0>といいます。"
                    to "Hi there! My name is\n<color 4>Lilith<color 0>.",
            "ニュース速報！"
                    to "Breaking news!",
            "<color 3>ハンターズブーストロード<color 0>に替わって\n<color 1>ラグオルブーストロード<color 0>が実装されました！"
                    to "<color 3>Hunters Boost Road<color 0> has been\nreplaced with\n<color 1>Ragol Boost Road<color 0>!",
            "<color 1>ラグオルブーストロード<color 0>では、\n頑張ってランク上げを<color 4>しなくても<color 0>\nブーストの恩恵が受けられます..."
                    to "With <color 1>Ragol Boost Road<color 0>,\nyou can enjoy boost benefits\n<color 4>without<color 0> grinding ranks...",
            "パーティを組み、クエストを受けて\nラグオルの原生生物を虐殺してください！\nそしてお目当てのアイテムを手に入れましょう！"
                    to "Form a party, take on quests,\nand hunt Ragol's creatures!\nGrab the items you're after!",
            "現在、どのクエストがブーストされているのかを\n知りたいときは、ロビーのカウンターから\n「<color 1>統計<color 0>」を選択してください。"
                    to "To see which quests are\ncurrently boosted, select\n\"<color 1>Statistics<color 0>\" at the lobby counter.",
            "また「<color 1>/rbr<color 0>」コマンドを入力することでも\n対象クエストを確認できます。"
                    to "You can also type the\n\"<color 1>/rbr<color 0>\" command to check.",
            "さあ、頑張って！"
                    to "Good luck out there!",
            "エネミーの返り<color 4>血<color 0>で真っ赤になって、\n無事に帰ってきてください！"
                    to "Come back safe, even if\nyou're drenched in enemy\n<color 4>blood<color 0>!",
            "時々、ラグオルの原生生物を\n虐殺することを考えて、\n興奮しちゃうんです。"
                    to "Sometimes just thinking about\nhunting Ragol's creatures\ngets me all worked up.",
            "今の状況って、ものすごくラッキー\nじゃないですか？ なんというか、\n殺すための口実があって。"
                    to "Isn't the current situation\nincredibly lucky? I mean,\nwe have an excuse to fight.",
            "オレが 放浪商人こと、\nコレン・ツだ。"
                    to "I'm the wandering merchant,\nKoren'z.",
            "オレがかき集めた\nアレでレアな商品を\nここらで少しサバきたくてな。"
                    to "I've gathered some pretty\nrare goods and I'm looking\nto move them here.",
            "ちょっとメセタを払ってくれりゃ、\n豪華商品が当たるカモしれない。"
                    to "Pay a little Meseta and\nyou might win something\nfancy.",
            "どうだ？ヤッてみるか？"
                    to "What do you say?\nWant to give it a shot?",
            "はい\nいいえ"
                    to "Yes\nNo",
            "当たるも八卦、\n当たらぬも八卦。"
                    to "Win some, lose some.\nThat's how it goes.",
            "ロクなものが当たらなくても\nオレにアタるなよ。"
                    to "Don't blame me if you\ndon't win anything good.",
            "ギャンブルなんて\nそんなモンだからな。"
                    to "That's just how\ngambling works.",
            "で、どのくらいメセタを\n払ってくれるんだい？"
                    to "So, how much Meseta\nare you willing to pay?",
            "メセタさえもらえりゃ、\nそれなりにサービスするぜ？"
                    to "As long as I get paid,\nI'll make it worth your while.",
            "１０００メセタ\n１００００メセタ\n１０００００メセタ\nやっぱり止める"
                    to "1000 Meseta\n10000 Meseta\n100000 Meseta\nNever mind",
            "ん？"
                    to "Hm?",
            "ざんねんだな。"
                    to "Too bad.",
            "まあ、こういうモンはたいてい\n損をするかもしれないからな。"
                    to "Well, you usually end up\nlosing with this kind of thing.",
            "慎重なのもいいモンだ。\n気が向いたら、また話しかけてくれ。"
                    to "Being cautious isn't bad.\nCome talk to me again\nwhenever you feel like it.",
            "アイテムが\nいっぱいみたいだな。"
                    to "Looks like your inventory\nis full.",
            "まずは、アイテムを\n整理してきてくれ。"
                    to "Go sort out your items\nfirst.",
            "話はソレからだ。"
                    to "We'll talk after that.",
            "なんだ？"
                    to "What?",
            "一度、やると言ったのに\n止めるのか。"
                    to "You said you'd do it\nand now you're backing out?",
            "そういうヤツは\nなにやっても失敗するぞ。"
                    to "People like that never\nsucceed at anything.",
            "払うといった額のメセタを\n満たしてないのか？"
                    to "You don't have enough\nMeseta to cover it?",
            "冷やかしならお断りだよ。"
                    to "I don't deal with\nwindow-shoppers.",
            "では、いクぞ。"
                    to "All right, here we go.",
            "いいモンが当たる様に\n念じるんだな。"
                    to "Cross your fingers for\nsomething good.",
            "サーバーセンドエラー"
                    to "Server send error",
            "<bk>\n<color 4><meseta_slot_prize><color 0>を入手した！"
                    to "<bk>\nYou obtained\n<color 4><meseta_slot_prize><color 0>!",
            "ほらよ！\nもっていきな！"
                    to "There you go!\nTake it!",
            "ロクなモンが当たらなくても\nそのうちイイものが"
                    to "Even if you don't win\nanything great, sooner or later",
            "必ず当たる！"
                    to "you're bound to hit\nthe jackpot!",
            "カモしれないからな、\nまたやってくれよな。"
                    to "...Maybe. So come back\nand try again, yeah?",
            "コマンド実行エラー"
                    to "Command execution error",
        )
    }
}
