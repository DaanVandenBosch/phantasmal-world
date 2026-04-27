# Symbol Chat Feature Plan

> **Status note (2026-04-21).** All four features below shipped, but the
> overall architecture shifted after the billboard landed:
>
> - **Feature 3 (ASM cursor → overlay preview)** was implemented and then
>   **removed**. The 3D View and Script tabs share one dock stack, so the
>   overlay was never visible while the user was editing ASM — the feature
>   had no reachable surface.
> - **Feature 4 (3D billboard)** shipped but no longer "complements the 2D
>   overlay" — the overlay was retired entirely. The billboard is now the
>   sole in-viewport preview for `SC ID 1` (spec1 only, no fallthrough),
>   and a new `SymbolChatEditPopup` (right-click → "Edit symbol chat…")
>   handles preset picking plus spec1/spec2/spec3 editing in one surface.
>   See `symbol-chat-object.md` → **Editor UI usage** for the current
>   behavior.
> - **Features 1 and 2 (HEX dialog, trigger rings)** shipped as planned
>   and remain current.
>
> The original plan below is kept as a historical record of the
> implementation order; refer to `symbol-chat-object.md` for the living
> behavior documentation.

## Background

The Quest Editor currently supports two symbol chat placement mechanisms:

1. **SymbolChatObject** (dat entity type 0x21) — references 24 built-in SC presets; the editor provides a 2D HTML overlay preview
2. **set_symbol_chat_collision** (opcode 0xF8A6) — ASM runtime trigger specifying world coordinates + radius + up to 3 custom HEX data labels + lock mechanism

Completed so far: palette aligned with qedit, overlay moved into the 3D viewport container, opcode signature fix, automatic SymbolChatHexData recognition, dynamically typed context menu, test coverage.

---

## Implementation Order: 3 → 1 → 2 → 4

---

## Feature 3: ASM Cursor → Overlay Preview Link (Small)

**Goal**: When the cursor lands on a SymbolChatData / SymbolChatHexData label line, show a floating overlay preview.

**Data flow**:
```
Monaco cursor move
  → DataEditorController.cursorLineNo (new MutableCell<Int?>)
  → DataEditorController.asmSymbolChatPreviewBuf (new derived Cell<Buffer?>)
  → SymbolChatOverlayWidget observes merged cell: entity buf ?: asm buf
```

**Changes**:
- `DataEditorController.kt` — add cursorLineNo: MutableCell<Int?> + asmSymbolChatPreviewBuf: Cell<Buffer?> derived (look up dataLabelAtLine, read readSegmentData when type matches)
- `AsmEditorWidget.kt:148` — in the existing onDidChangeCursorPosition callback, add: dataEditorCtrl.cursorLineNo.value = lineNo
- `SymbolChatOverlayWidget.kt` — add DataEditorController to constructor, observe merged cell instead of single symbolChatPreviewBuf
- `QuestEditor.kt` — pass dataEditorController to overlay widget
- `EntityInfoController.kt` — symbolChatPreviewHidden observes merged cell

**Verification**: Open symbol_chat_test.qst, move cursor to label 9000/9024/9025 → overlay shows corresponding SC preview; move away → overlay disappears

---

## Feature 1: SymbolChatHexData Dialog (64-byte Layout) (High)

**Goal**: HEX data referenced by set_symbol_chat_collision has a 4-byte extended header (face type / face color / sound effect / reserved); the dialog must handle this.

**HEX layout** (needs validation against real quest data):
```
Byte 0    face type (u8)
Byte 1    face color (u8)
Byte 2    sound effect ID (u8)
Byte 3    reserved (u8)
Byte 4-63 standard 60-byte SymbolChatT
```

**Changes**:
- New `SymbolChatHexDialog.kt` — mirrors SymbolChatDialog structure, adds sound effect field, offsets reads/writes by 4 bytes
- `SymbolChatRenderer.kt` — no change; dialog internally creates a 60-byte sub-buffer (buf.slice(4, 60)) and passes it to existing renderBuffer
- `AsmWidget.kt` — add symbolChatHexDialogVisible cell + dialog instance
- `AsmEditorWidget.kt` — openDataDialog's SymbolChatHexData branch opens the new dialog
- GC endian: add checkbox in dialog to flip the first 12 bytes of the inner 60-byte block

**Verification**: Right-click "Edit Symbol Chat" on a HEX label → new dialog opens showing sound effect field + correct rendering

---

## Feature 2: 3D Trigger Visualization (Medium)

**Goal**: Extract X/Y/Z/radius from set_symbol_chat_collision's R+0..R+3 and draw semi-transparent rings in the 3D view.

**Data flow**:
```
Quest load
  → analyzeSymbolChatTriggers(bytecodeIr) → List<SymbolChatTriggerInfo>
      { x, y, z, radius, scLabelIds, segmentLabel }
  → SymbolChatTriggerManager creates RingGeometry meshes
  → added to renderContext.helpers
```

**Changes**:
- `DataLabelAnalysis.kt` — add analyzeSymbolChatTriggers() function, reuses existing back-trace but additionally extracts R+0..R+3 float values
- New `SymbolChatTriggerManager.kt` — under rendering/, observes quest changes, creates/cleans up ring meshes
- Reuse RangeCircleRenderer's RingGeometry + MeshBasicMaterial pattern, orange 0xFFAA00 to distinguish
- `QuestEditorMeshManager.kt` — instantiate trigger manager
- Click-to-select + jump to ASM as a follow-up enhancement; first version is visualization only

**Verification**: Open a quest containing set_symbol_chat_collision → orange rings appear in the 3D view

---

## Feature 4: 3D Billboard (Low)

**Goal**: Render an SC preview billboard above the SymbolChatObject's world position, facing the camera.

**Changes**:
- New `SymbolChatBillboardRenderer.kt` — offscreen canvas → CanvasTexture → PlaneGeometry → Mesh, billboard behavior
- `SelectionVisualizationManager.kt` — create billboard when SymbolChatObject is selected, observe symbolChatPreviewBuf to update texture
- `QuestRenderer.kt` — call updateBillboardScale(camera, mesh) in the render loop
- Keep existing 2D overlay (hosts picker interaction); billboard is for position indication only

**Key decision**: Complement rather than replace the 2D overlay — they serve different roles (overlay = interactive editing, billboard = position indication).

**Verification**: Select a SymbolChatObject → SC preview billboard appears above the object

---

## Dependencies

```
Feature 3 (independent, smallest)
    ↓
Feature 1 (independent, but Feature 3's overlay link improves HEX label preview experience)
    ↓
Feature 2 (extends DataLabelAnalysis, needs Feature 1 to confirm HEX layout)
    ↓
Feature 4 (independent, only depends on entity selection)
```
