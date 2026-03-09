# Script Editor Enhancement TODOs

## Progress (4/13)

- [x] Hex/Decimal Toggle
- [x] Monaco DocumentSymbolProvider Fix (Ctrl+Shift+O)
- [x] View Menu Display Options
- [x] City Quest Loading Simplification
- [ ] Free Roam Map Loading (expand to all areas)
- [ ] Label List Panel
- [ ] Register List Panel
- [ ] Opcode Reference Panel
- [ ] Data/String Segment List
- [ ] Hide NOPs Toggle
- [ ] Data Editing (Right-click Context Menu)
- [ ] Label Flag Marking
- [ ] Annotation Support

---

## Completed Details

### Hex/Decimal Toggle
- Added `intFormat` parameter to `disassemble()` in `Disassembly.kt` (default: `"dec"`)
- `AsmStore` passes `"hex"` or `"dec"` based on `hexFormat` cell (UI default: hex=true)
- "Hex" checkbox in `AsmToolbarWidget` next to "Inline args"
- Hex format: unsigned, 0x-prefixed, bit-width-aware (`0xEC` for Byte, `0x00EC` for Short, `0xFFFFFFEC` for Int)
- Tokenizer uses `toUIntOrNull(16)` (NOT `toLongOrNull`) to avoid Kotlin/JS Long emulation issues
- Tests added in `AsmTokenizationTests.kt` for large hex values

### Monaco DocumentSymbolProvider Fix (Ctrl+Shift+O)
- Rewrote as plain JS object factory (`createDocumentSymbolProvider`) using `js("({})")`
- Avoids Monaco `objectHash` infinite recursion on Kotlin class prototype chains
- File: `web/.../asm/monaco/AsmDocumentSymbolProvider.kt`

### View Menu Display Options
- Added Section IDs, Door & Fence IDs, Spawn Ground, Origin Point to View menu
- File: `web/.../widgets/QuestEditorToolbarWidget.kt`

### City Quest Loading Simplification
- Removed `npcDatName`/`objDatName` from `CityBinInfo`, removed unused methods
- Single "Open City Quest..." menu item with file picker flow

---

## Next Up: Free Roam Map Loading

### Overview
当前只支持 City/Lab（EP1 Pioneer II、EP2 Lab、EP4 Pioneer II）。需要扩展到所有 free roam 区域。
加载逻辑与 city/lab 一致：bin + dat → `synthesizeDat` → `parseBinDatToQuest`，只需扩展文件查找。

### 游戏数据目录
`{game_root}/data/` 下的文件（以 Ephinea 为参考）。

### 所有 Free Roam 区域文件清单

| Episode | 区域 | bin 文件 | dat 文件 (data/) | dat 类型 |
|---------|------|----------|-----------------|----------|
| I | Pioneer II | `map_city_{on\|off}_{lang}.bin` | `map_city00_00{e\|o}[_s].dat` | NPC+OBJ |
| I | Forest | `map_forest_{lang}[_u].bin` | 无 | 纯脚本 |
| I | Cave | `map_cave_{lang}[_u].bin` | `map_cave{01-03}_{var}_{subvar}o.dat` | OBJ only |
| I | Mine | `map_machine_{lang}[_u].bin` | `map_machine{01-02}_{var}_{subvar}o.dat` | OBJ only |
| I | Ruins | `map_ruin_{lang}[_u].bin` | `map_ruins{01-02}_{var}_00o.dat` | OBJ only |
| II | Lab | `map_labo_{on\|off}_{lang}[_u].bin` | `map_labo00_00{e\|o}[_s].dat` | NPC+OBJ |
| II | Jungle | `map_jungle_{lang}[_u].bin` | 无 | 纯脚本 |
| II | Temple | `map_ancient_{lang}[_u].bin` | `map_ancient{01-03}_{var}_{subvar}o.dat` | OBJ only |
| II | Seabed | `map_seabed_{lang}[_u].bin` | `map_seabed{01-02}_{var}_{subvar}o.dat` | OBJ only |
| II | CCA | `map_space_{lang}[_u].bin` | `map_space{01-02}_{var}_00o.dat` | OBJ only |
| IV | Pioneer II | `map_city02_{on\|off}_{lang}.bin` | `map_city02_00_00{e\|o}[_s].dat` | NPC+OBJ |
| IV | Crater | 无独立 bin（city02 控制） | `map_crater01_00_{var}{e\|o}.dat` | NPC+OBJ+EVT |
| IV | Desert | 无独立 bin（city02 控制） | `map_desert{01-03}_{var}_00{e\|o}.dat` | NPC+OBJ+EVT |
| IV | Wilds | 无独立 bin（city02 控制） | `map_wilds01_{var}_{subvar}{e\|o}.dat` | NPC+OBJ+EVT |

说明：`{lang}` = `j`(日文)、`e`(英文) 等；`[_u]` = Ultimate 难度变体；`{var}` = 地图变体。

### 加载逻辑分类

**1. 已支持：City/Lab（bin + 单 NPC dat + 单 OBJ dat）**
- EP1 Pioneer II、EP2 Lab、EP4 Pioneer II
- 流程：加载 bin → 加载 1 个 NPC dat + 1 个 OBJ dat → `synthesizeDat` → `parseBinDatToQuest`

**2. 纯脚本区域（bin only，无 dat）**
- EP1 Forest、EP2 Jungle
- 流程：加载 bin → 合成空 dat → `parseBinDatToQuest`

**3. bin + 多区域 OBJ dat（无 NPC dat）**
- EP1 Cave/Mine/Ruins、EP2 Temple/Seabed/CCA
- 流程：加载 bin → 查找并加载多个 OBJ dat → 合并为单个 DAT（NPC 部分为空）→ `parseBinDatToQuest`
- dat 文件按 `{area}{sub}_{variant}_{subvariant}o.dat` 命名，需遍历目录匹配

**4. EP4 野外区域（无独立 bin，city02 控制）**
- Crater/Desert/Wilds 由 `map_city02` bin 脚本通过 `bb_map_designate` 引用
- 有 NPC+OBJ dat + EVT 事件文件
- 方案：扩展 city02 加载时一并加载野外 dat；或暂不支持野外 dat 编辑，仅编辑脚本

### 实现要点
- `QuestLoader.kt`：扩展 `loadCityLobbyQuest` 或新建方法，支持多 dat 合并
- `synthesizeDat`：改为接收多组 (areaId, objData, npcData)，每组生成一个 DAT table entry
- `parseCityBinFilename`：扩展正则，识别所有 free roam bin 文件名
- `QuestEditorToolbarController.kt`：扩展文件选择/保存流程
- `extractRawEntityData`：保存时按区域拆分回多个 dat 文件

### 参考文件
- `SetDataTableOn.rel` / `SetDataTableOff.rel`：游戏引擎地图数据配置表，定义各区域加载的数据前缀
- `data/scene/`：3D 渲染资源（模型、碰撞、纹理），与实体 dat 分开

---

## Slide-out Panels

### Label List Panel [next]
- List all labels from `asmAnalyser.getLabels()`, ordered by line number
- Click → navigate to label in editor (reuse `goToLabel` mechanism)
- Show label ID + line number
- Pattern: expandable overlay like `QuestEditorRendererWidget` panels

### Register List Panel
- Show registers referenced in current script
- Need new worker request `GetRegisters` to extract register usage from bytecodeIr
- Show register ID + read/write info

### Opcode Reference Panel
- List all 538 opcodes from `Opcodes.kt`
- Filterable/searchable
- Click → insert opcode mnemonic at cursor, or show docs
- Static data, no worker needed

### Data/String Segment List
- List all `.data` and `.string` segments with their labels
- Click → navigate to segment in editor
- Need new worker request or derive from `getLabels()` + segment type info

### Files to Modify for Panels
- `web/.../widgets/AsmWidget.kt` or new `AsmEditorPanelsWidget.kt`
- `web/.../controllers/AsmEditorController.kt` — expose label/register/segment data
- `web/.../asm/AsmAnalyser.kt` — possibly new requests
- `web/assembly-worker/.../AsmAnalyser.kt` — implement new data extraction
- `web/shared/.../Messages.kt` — new message types if needed

---

## Later: Advanced Features

### Hide NOPs Toggle
- Filter out NOP instructions during disassembly (toggle in toolbar)
- Add `hideNops` parameter to `disassemble()`

### Data Editing (Right-click Context Menu)
- Context menu to insert/edit NPC data, Image, Float, Vector, Enemy stats, Symbol Chat
- Requires understanding data segment structure and entity data formats

### Label Flag Marking
- Mark labels as code / hex data / str data
- Changes how the disassembler treats the corresponding segment

### Annotation Support
- Add custom annotations/comments to assembly lines
- Store separately from script (metadata layer), toggle visibility

---

## Key Technical Notes

### Hex Format Implementation
- `Disassembly.kt:appendHexInt(value, bitSize)` — converts Int to unsigned hex with mask
- `AsmTokenization.kt:tokenizeHexNumber()` — uses `toUIntOrNull(16)` for parsing
- `AsmMonarchLanguage.kt` — regex `-?0x[0-9a-fA-F]+` for syntax highlighting
- Round-trip: `-20` → disasm `0xFFFFFFEC` → asm parse → `-20` ✓
- `disassemble()` default is `"dec"`, UI explicitly passes `"hex"` when checkbox is on

### Monaco Provider Pattern
- Kotlin classes cause `objectHash` infinite recursion in Monaco
- Solution: use `js("({})")` to create plain JS objects, then cast with `@Suppress`
- Applied to `DocumentSymbolProvider`, may need for future providers too
