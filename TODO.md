# Script Editor Enhancement TODOs

## Progress (4/12)

- [x] Hex/Decimal Toggle
- [x] Monaco DocumentSymbolProvider Fix (Ctrl+Shift+O)
- [x] View Menu Display Options
- [x] City Quest Loading Simplification
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

## Next Up: Slide-out Panels

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
