# PSO Quest File Formats

## Overview

PSO quests use three file types with distinct roles:

| Format | Role | Content |
|--------|------|---------|
| **QST** | Archive container (like tar) | Packs BIN + DAT into interleaved 1024-byte chunks |
| **BIN** | Quest metadata + script | Quest ID, name, description, bytecode |
| **DAT** | Entity data | Objects, NPCs/monsters, events, challenge mode data |

```
QST (archive, no compression)
 ├── .bin (PRS compressed) → metadata + bytecode
 └── .dat (PRS compressed) → entity tables
```

## Compression Rules

Compression depends on the **usage scenario**, not the container format:

| Scenario | BIN/DAT compressed? | Notes |
|----------|---------------------|-------|
| Online quests (inside QST) | Yes, PRS compressed | For network transfer / online distribution |
| Offline quests (local files) | No, uncompressed | Client reads directly from disk |
| Freeplay / Free Roam | No, uncompressed | Client reads directly from disk |

- **QST itself does not compress** — it only chunks and interleaves
- **PRS** is the compression algorithm used (LZ77 variant with flag-byte control)

## Offline Quest File Layout

```
/ (root)
├── quest01_j.dat      # DAT files always in root, always *_j.dat
├── quest02_j.dat      # regardless of language
├── ...
└── Quest/
    ├── quest01_e.bin  # BIN files in /Quest/ subdirectory
    ├── quest01_j.bin  # multiple languages: _e(EN) _f(FR) _g(DE) _j(JP) _s(ES)
    └── ...
```

- DAT files are always named `*_j.dat` no matter which language is used
- To use a non-Japanese language: copy DAT files into the quest folder and rename `_j` to the target language suffix (e.g., `_e` for English)

## Version Differences

| Version | BIN bytecode offset | Header encoding | QST chunk size |
|---------|--------------------|-----------------|--------------------|
| DC/GC | 468 | ASCII | 1048 (1024 data + 24 overhead) |
| PC | 920 | UTF-16 | 1048 |
| BB | 4652 | UTF-16 | 1056 (1024 data + 32 overhead) |

## Identifying Compressed vs Uncompressed Files

- **Uncompressed BIN**: First bytes are offset values (e.g., `D4 01 00 00` = 468 for DC/GC bytecode offset)
- **PRS compressed**: First byte is a flag byte controlling the compression stream — does not match expected offset patterns

## Platform-Specific Encoding

| Platform | Header Size | Text Encoding | Assembly Mode |
|----------|-------------|---------------|---------------|
| DC/GC | `0x1D4` (468 bytes) | ASCII | — |
| PC | `0x394` (916 bytes) | UTF-16 | — |
| BB | `0x394` (916 bytes) | UTF-16 | 32-bit |

## Distribution Formats Comparison

Four distinct output formats exist, differing in compression, encryption, and packaging:

| | Raw | Kohle | Server QST | Download QST |
|---|-----|-------|------------|--------------|
| **Output files** | `.bin` + `.dat` (separate) | `.bin` + `.dat` (separate) | single `.qst` container | single `.qst` container |
| **PRS compression** | No | Yes | Yes | Yes |
| **PSO encryption** | No | No | No | Yes |
| **QST chunk packing** | No | No | Yes | Yes |
| **Use case** | Client local / offline play | Intermediate for toolchains | Server online distribution | Download to client storage |

### Processing Pipeline

```
              Editor internal data (BIN + DAT)
                         │
          ┌──────────────┼──────────────────────┐
          │              │                      │
        Raw(1)    Formats needing compression   │
          │              │                      │
          │         PRS compress                 │
          │              │                      │
          │     ┌────────┼──────────┐           │
          │     │        │          │           │
          │  Server    Download   Kohle         │
          │  (2-5)     (6-9)    (10-12)         │
          │     │        │          │           │
          │     │   PSO encrypt     │           │
          │     │   +8-byte header  │           │
          │     │        │          │           │
          │   QST pack  QST pack  Write files   │
          │  (interleave)(interleave)            │
          │     │        │          │           │
          ▼     ▼        ▼          ▼           │
        .bin   .qst     .qst      .bin          │
        .dat                      .dat          │
```

- **Raw** — Bare uncompressed BIN + DAT, directly readable by the client (`main.pas:5579-5594`)
- **Kohle** — PRS-compressed BIN + DAT as separate files, no container; useful when compressed files are needed without QST wrapping (`main.pas:5547-5576`)
- **Server QST** — PRS-compressed, then packed into a QST container with 1024-byte interleaved chunks (`main.pas:5347-5542`)
- **Download QST** — Same as Server QST, but each .bin/.dat is additionally encrypted with PSO stream cipher before QST packing (`main.pas:5373-5392`)

### Download Quest Encryption

PSO stream cipher (PRNG-based XOR) protects downloaded quest files from casual tampering.

**Encrypted data layout** (prepended to each .bin/.dat inside QST):
```
Offset  Size     Content
0       4 bytes  Original uncompressed data size (little-endian)
4       4 bytes  Encryption seed (random 32-bit value)
8       N bytes  Encrypted data (XOR stream cipher output)
```

**Key generation (`CreateKey`):**
1. Initialize a 56-DWORD (224-byte) key table from a 32-bit seed
2. Seed fills `Key[55]` and `Key[56]`, then iterates with step `0x15` mod `0x37` using subtraction
3. Call `MixKey()` 4 times to scramble the table (subtract-based mixing, similar to Mersenne Twister twist)

**Encryption/Decryption (`PSOEnc`):**
- XOR each byte against key stream: `output[i] = input[i] XOR KeyBytes[KeyPos]`
- Every 224 bytes consumed, call `MixKey()` to regenerate key stream
- Symmetric: same function encrypts and decrypts (seed is stored in plaintext header)

> The encryption is weak by design — the seed is stored alongside the ciphertext. It only deters casual modification, not reverse engineering.

## Complete Format Matrix (from Qedit)

Qedit supports 13 save formats and 6 load formats, covering all PSO platforms:

### Save Formats

| # | Format | Extension | Platform | Type |
|---|--------|-----------|----------|------|
| 1 | Quest file (PC Raw) | `.bin` | PC | Raw BIN+DAT |
| 2 | Server Quest file (PC) | `.qst` | PC | Server QST |
| 3 | Server Quest file (DC) | `.qst` | DC | Server QST |
| 4 | Server Quest file (GC) | `.qst` | GC | Server QST |
| 5 | Server Quest file (BB) | `.qst` | BB | Server QST |
| 6 | Download Quest file (DC) | `.qst` | DC | Download QST (encrypted) |
| 7 | Download Quest file (PC) | `.qst` | PC | Download QST (encrypted) |
| 8 | Download Quest file (GC) | `.qst` | GC | Download QST (encrypted) |
| 9 | Download Quest file (Xbox) | `.qst` | Xbox | Download QST (encrypted) |
| 10 | Kohle basic format (PC) | `.bin` | PC | Kohle |
| 11 | Kohle basic format (DC) | `.bin` | DC | Kohle |
| 12 | Kohle basic format (GC) | `.bin` | GC | Kohle |
| 13 | Quest project | `.qprj` | — | Editor project |

### Load Formats

| # | Format | Extension | Notes |
|---|--------|-----------|-------|
| 1 | Raw Quest | `.bin` | Uncompressed BIN+DAT pair |
| 2 | Compressed Quest | `.bin` | PRS-compressed BIN |
| 3 | Server Quest File | `.qst` | PC/DC/GC server format |
| 4 | BB Server Quest File | `.qst` | BB server format |
| 5 | Download Quest File | `.qst` | Encrypted download format |
| 6 | Quest project | `.qprj` | Editor project file |

### Format Auto-Detection (on load)

Detection by header magic bytes:
- `0x44` → GC/DC server format
- `0xA6` → DC download format
- `0x58` at offset 0 + `0x44` at offset 2 → BB format

## Qedit Project Format (.qprj)

Qedit's internal project format preserves the full editor state:

```
Offset  Size        Content
0       128 bytes   Quest title (Unicode)
128     1024 bytes  Quest info/description (Unicode)
1152    2048 bytes  Full description (Unicode)
3200    4 bytes     Quest ID (uint32)
3204    2 bytes     Language code
3206    2 bytes     Filter flags
3208    × 30 floors:
          ~3400 bytes  Floor data structure
          4 bytes      Floor enabled flag
          1152 bytes   Map path (512) + XVM path (512) + Floor name (128)
...     Script backup (assembly source)
...     Custom script functions list
...     Custom script function definitions
...     Custom script registers
...     Custom script opcodes
```

## DAT Chunk Export (from Qedit)

Qedit can export DAT data selectively by chunk type:

| Filter | Pattern | Content |
|--------|---------|---------|
| All chunks | `*.*` | Full DAT file |
| Object only | `*o.dat` | Object/item entities |
| Monster only | `*e.dat` | NPC/monster entities |
| Event only | `*.evt` | Event/trigger data |

## BIN Text Encoding

Text encoding in the BIN header depends on platform and language:

| Platform | Encoding | Notes |
|----------|----------|-------|
| DC/GC (non-Japanese) | Latin-1 (ISO-8859-1) | Supports accented chars (French/German/Spanish) |
| DC/GC (Japanese) | Shift-JIS | Multi-byte encoding for Japanese characters |
| PC / BB | UTF-16 | Full Unicode support |

### Language Detection

The BIN header's `language` field is **unreliable** for encoding detection — many offline quest
files set it to 0 regardless of actual language. Detection must use the **BIN filename suffix**:

| Suffix | Language | DC/GC Encoding |
|--------|----------|----------------|
| `_j` | Japanese | Shift-JIS |
| `_e` | English | Latin-1 |
| `_f` | French | Latin-1 |
| `_g` | German | Latin-1 |
| `_s` | Spanish | Latin-1 |

Shift-JIS is **not** a superset of Latin-1. Bytes in the 0x80-0x9F and 0xE0-0xEF ranges are
treated as lead bytes of 2-byte sequences in Shift-JIS, which corrupts Latin-1 accented characters
(e.g., `é` = 0xE9). ASCII (0x00-0x7F) is compatible with both encodings.

## Bytecode Calling Conventions (Cross-Platform)

PSO quest bytecode uses two different calling conventions depending on the platform.
This is the primary barrier to cross-platform quest conversion.

### AsmMode Overview

| AsmMode | Platforms | Convention | Description |
|---------|-----------|------------|-------------|
| 0 | PC, DC | Immediate | Arguments follow the opcode inline |
| 2 | GC, BB | Stack-based | Arguments are pushed onto a stack before the opcode |

### Immediate Mode (PC/DC — AsmMode=0)

Arguments are encoded directly after the opcode:

```
[opcode] [reg_arg] [imm_arg] ...
```

Example: `call_function R1, 42` →
```
XX 01 2A
```

### Stack Mode (GC/BB — AsmMode=2)

Arguments are pushed onto a stack using dedicated push instructions, then the opcode
reads them from the stack:

```
[push_reg]  [reg_id]     ; 0x48 = arg_pushr (push register)
[push_byte] [value]      ; 0x49 = arg_pushb (push byte immediate)
[opcode]                 ; consumes arguments from stack
```

Push instructions:
| Opcode | Name | Size | Description |
|--------|------|------|-------------|
| `0x48` | arg_pushr | 1 byte | Push register value |
| `0x49` | arg_pushb | 1 byte | Push byte immediate |
| `0x4A` | arg_pushw | 2 bytes | Push word immediate |
| `0x4B` | arg_pushl | 4 bytes | Push dword immediate |
| `0x4C` | arg_pushf | 4 bytes | Push float immediate |
| `0x4D` | arg_pushs | string | Push string |
| `0x4E` | arg_pusha | 1 byte | Push register address |

### Instruction Differences

Some instructions have different opcodes between platforms:
- `0xD9` (PC/DC) ↔ `0xEF` (GC/BB) — automatic mapping during compilation
- Some V3 instructions require extra trailing parameters in PC/DC mode
  (e.g., `set_ally_NPC1_V3` needs an extra `0x07` argument)
- Instructions marked `T_DC` (DC-only) are skipped on GC/BB

### How Qedit Handles Cross-Platform Conversion

Qedit can save any quest to any platform format because its intermediate form is
**platform-agnostic**. The key is that the disassembly step **consumes** push instructions
and merges their values into the target opcode's arguments:

**Example: `window_msg R5, 1`**

BB binary (stack mode):
```
0x49 0x01        ; arg_pushb 1      ← push immediate 1
0x48 0x05        ; arg_pushr R5     ← push register R5
0xF8 0x51        ; window_msg       ← consumes args from stack
```

PC binary (inline mode):
```
0xF8 0x51 0x05 0x01  ; window_msg R5, 1  ← args follow opcode directly
```

Qedit's disassembly (identical for both):
```
window_msg R5, 1
```

The processing pipeline:

```
Load .bin file
    │
    ▼
Read binary bytecode
    │
    ▼
Disassemble (Unit1.pas ~line 460-900):
    │  - Decode opcodes and arguments byte by byte
    │  - AsmMode=2: push instructions are NOT emitted as text;
    │    instead values are stored in Stack[] array and later
    │    merged into the next opcode's argument list
    │  - Output: unified assembly text lines in ListBox
    ▼
User edits assembly text
    │
    ▼
Compile — QuestBuild() (Unit1.pas ~line 1161+):
    │  - Parse text lines back into opcodes + arguments
    │  - AsmMode=2: for T_ARGS instructions, emit push instructions
    │    (0x48-0x4E) before the opcode based on argument types
    │  - AsmMode=0: encode arguments inline after the opcode
    ▼
Write binary bytecode to target format
```

### Phantasmal World — Current Limitations

Our `BytecodeIr` is **not** platform-agnostic. Unlike Qedit's unified text, our disassembly
**preserves push instructions as-is**:

```kotlin
// BB quest loaded → BytecodeIr retains push instructions:
Instruction(OP_ARG_PUSHB, args=[1])
Instruction(OP_ARG_PUSHR, args=[R5])
Instruction(OP_WINDOW_MSG)             // no args — reads from stack

// PC quest loaded → BytecodeIr has inline arguments:
Instruction(OP_WINDOW_MSG, args=[R5, 1])  // args are inline
```

The same logical operation produces **structurally different IR** depending on the source platform.
This means directly changing the target `BinFormat` during save would produce invalid bytecode —
BB's IR contains push opcodes that PC/DC clients don't understand.

### What Cross-Platform Conversion Would Require

| Direction | IR Transformation |
|-----------|-------------------|
| GC/BB → PC/DC | Find push sequences before each `T_ARGS` opcode, **inline** their values as opcode arguments, **remove** the push instructions |
| PC/DC → GC/BB | Extract inline arguments from each `T_ARGS` opcode, **insert** corresponding push instructions before it, **clear** the opcode's inline args |

This is essentially the same work Qedit splits across disassembly (merge) and compilation (split),
but performed as a direct IR-to-IR transformation pass.
