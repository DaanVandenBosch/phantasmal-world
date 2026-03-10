# Script Editor Enhancement TODOs

## Progress (9/10)

- [x] Hex/Decimal Toggle
- [x] Monaco DocumentSymbolProvider Fix (Ctrl+Shift+O)
- [x] View Menu Display Options
- [x] City Quest Loading Simplification
- [x] Hide NOPs Toggle
- [x] Label List Panel
- [x] Opcode Reference Panel
- [x] Register List Panel
- [x] Data/String Segment List
- [ ] Free Roam Map Loading (expand to all areas)

### Deferred
- Data Editing (Right-click Context Menu)
- Label Flag Marking
- Annotation Support

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

### Hide NOPs Toggle
- Added `hideNops: Boolean = false` parameter to `disassemble()` in `Disassembly.kt`
- Skips NOP instructions (`opcode.code == OP_NOP.code`) during instruction emission
- `AsmStore` adds `_hideNops` cell, observer triggers re-disassembly
- "Hide NOPs" checkbox in `AsmToolbarWidget` next to "Hex"

### Label List Panel
- Slide-out overlay panel on right side of ASM editor (same pattern as renderer NPC/Object panels)
- Lists all labels sorted by label ID, showing label name + line number
- Click navigates to label position in editor via `goToLabelRange()`
- Labels fetched async from worker via `AsmAnalyser.getLabels()`, refreshed on asm change
- Files: `AsmWidget.kt`, `AsmStore.kt` (labels cell + refreshLabels), `AsmEditorController.kt`

### Register List Panel
- Slide-out overlay panel showing all registers referenced in current script
- New worker request `GetRegisters`: iterates bytecodeIr instruction segments, collects register IDs from `RegRefType` params
- Shows register ID (r0, r1, ...) + read/write counts (R3/W1)
- Sorted by register ID
- Files: `Messages.kt` (RegisterInfo, Request/Response.GetRegisters), worker `AsmAnalyser.kt`, `AsmServer.kt`, web `AsmAnalyser.kt`, `AsmStore.kt`, `AsmEditorController.kt`, `AsmWidget.kt`

### Data/String Segment List
- Slide-out overlay panel listing all `.data` and `.string` segments
- New worker request `GetSegments`: iterates bytecodeIr segments, returns type + labels + range + size
- Shows segment type (.data/.string) + label IDs + size (bytes/chars)
- Click navigates to segment position in editor
- Files: `Messages.kt` (SegmentInfo, SegmentInfoType, Request/Response.GetSegments), same chain as Register List

### Opcode Reference Panel
- Slide-out overlay panel below Label List panel
- Lists all known opcodes (from `OPCODES`, `OPCODES_F8`, `OPCODES_F9` arrays) with mnemonic + hex code
- Filter/search input (matches mnemonic, hex code, or doc text)
- Click navigates to opcode occurrence in editor; consecutive clicks cycle through all occurrences
- Hover shows opcode documentation tooltip
- Files: `AsmWidget.kt`, `AsmEditorController.kt` (findOpcodeMatches + goToMatch)

---

## Next Up: Free Roam Map Loading

### Overview
Currently only City/Lab areas are supported (EP1 Pioneer II, EP2 Lab, EP4 Pioneer II). Need to expand to all free roam areas.
Loading logic is the same as city/lab: bin + dat → `synthesizeDat` → `parseBinDatToQuest`, just need to expand file discovery.

### Game Data Directory
Files under `{game_root}/data/` (using Ephinea as reference).

### All Free Roam Area File List

| Episode | Area | bin file | dat file (data/) | dat type |
|---------|------|----------|-----------------|----------|
| I | Pioneer II | `map_city_{on\|off}_{lang}.bin` | `map_city00_00{e\|o}[_s].dat` | NPC+OBJ |
| I | Forest | `map_forest_{lang}[_u].bin` | none | script only |
| I | Cave | `map_cave_{lang}[_u].bin` | `map_cave{01-03}_{var}_{subvar}o.dat` | OBJ only |
| I | Mine | `map_machine_{lang}[_u].bin` | `map_machine{01-02}_{var}_{subvar}o.dat` | OBJ only |
| I | Ruins | `map_ruin_{lang}[_u].bin` | `map_ruins{01-02}_{var}_00o.dat` | OBJ only |
| II | Lab | `map_labo_{on\|off}_{lang}[_u].bin` | `map_labo00_00{e\|o}[_s].dat` | NPC+OBJ |
| II | Jungle | `map_jungle_{lang}[_u].bin` | none | script only |
| II | Temple | `map_ancient_{lang}[_u].bin` | `map_ancient{01-03}_{var}_{subvar}o.dat` | OBJ only |
| II | Seabed | `map_seabed_{lang}[_u].bin` | `map_seabed{01-02}_{var}_{subvar}o.dat` | OBJ only |
| II | CCA | `map_space_{lang}[_u].bin` | `map_space{01-02}_{var}_00o.dat` | OBJ only |
| IV | Pioneer II | `map_city02_{on\|off}_{lang}.bin` | `map_city02_00_00{e\|o}[_s].dat` | NPC+OBJ |
| IV | Crater | no standalone bin (controlled by city02) | `map_crater01_00_{var}{e\|o}.dat` | NPC+OBJ+EVT |
| IV | Desert | no standalone bin (controlled by city02) | `map_desert{01-03}_{var}_00{e\|o}.dat` | NPC+OBJ+EVT |
| IV | Wilds | no standalone bin (controlled by city02) | `map_wilds01_{var}_{subvar}{e\|o}.dat` | NPC+OBJ+EVT |

Notes: `{lang}` = `j` (Japanese), `e` (English), etc.; `[_u]` = Ultimate difficulty variant; `{var}` = map variant.

### Loading Logic Categories

**1. Supported: City/Lab (bin + single NPC dat + single OBJ dat)**
- EP1 Pioneer II, EP2 Lab, EP4 Pioneer II
- Flow: load bin → load 1 NPC dat + 1 OBJ dat → `synthesizeDat` → `parseBinDatToQuest`

**2. Script-only areas (bin only, no dat)**
- EP1 Forest, EP2 Jungle
- Flow: load bin → synthesize empty dat → `parseBinDatToQuest`

**3. bin + multi-area OBJ dat (no NPC dat)**
- EP1 Cave/Mine/Ruins, EP2 Temple/Seabed/CCA
- Flow: load bin → find and load multiple OBJ dat → merge into single DAT (NPC section empty) → `parseBinDatToQuest`
- dat files named as `{area}{sub}_{variant}_{subvariant}o.dat`, requires directory traversal to match

**4. EP4 outdoor areas (no standalone bin, controlled by city02)**
- Crater/Desert/Wilds referenced by `map_city02` bin script via `bb_map_designate`
- Have NPC+OBJ dat + EVT event files
- Approach: extend city02 loading to also load outdoor dat; or defer outdoor dat editing, script editing only

### Implementation Notes
- `QuestLoader.kt`: extend `loadCityLobbyQuest` or create new method, support multi-dat merging
- `synthesizeDat`: change to accept multiple (areaId, objData, npcData) groups, each generating a DAT table entry
- `parseCityBinFilename`: extend regex to recognize all free roam bin filenames
- `QuestEditorToolbarController.kt`: extend file selection/save flow
- `extractRawEntityData`: split back into multiple dat files by area when saving

### Reference Files
- `SetDataTableOn.rel` / `SetDataTableOff.rel`: game engine map data config tables, define data prefixes loaded per area
- `data/scene/`: 3D rendering resources (models, collision, textures), separate from entity dat

---

## Slide-out Panels

### Label List Panel [done]
### Opcode Reference Panel [done]

### Register List Panel [done]
### Data/String Segment List [done]

---

## Later: Advanced Features

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
