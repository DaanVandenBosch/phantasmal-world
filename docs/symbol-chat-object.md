# Symbol Chat Object (typeId 0x21)

The small floating markers in PSO levels that display an emoticon/indicator
image — for example the "Cave 1 Map 1" area indicators in challenge mode —
are essentially a level object called a **Symbol Chat Object**.

newserv calls it `TOSymbolchatColli` ("symbol chat collision"), with typeId
`0x21` (33).

> Don't confuse this with the symbol chats that players manually send through
> the chat system. Both share the same 60-byte `SymbolChatT` data structure,
> but a Symbol Chat Object is a **world object**: it triggers display when
> the player walks inside its radius, not a message on a chat channel.

## Two data-source paths

| Creation path | Data used | Can customize the 60-byte content? |
|---|---|---|
| **DAT ObjectTable static placement** | Built-in index into `symbolchatcolli.prs` | ❌ Can only pick an index |
| **Script `symbol_chat_create` dynamic creation** (0xF8A6) | dlabel points to custom `SymbolChatT` | ✅ Can override the built-ins |

### Path A: DAT static placement

The most common usage. A Symbol Chat Object, like boxes / warps / NPCs,
exists as an ObjectTable entry in the quest's `.dat`. Its property layout:

| Field | Offset | Type | Meaning |
|---|---|---|---|
| `radius` | 40 | f32 | Trigger radius |
| `spec1` | 52 | u32 | switch+SC pair 1 |
| `spec2` | 56 | u32 | switch+SC pair 2 |
| `spec3` | 60 | u32 | switch+SC pair 3 |

Each spec is a 32-bit dual field:
- High 16 bits = **switch flag number** (0..255)
- Low 16 bits = **entry index into `symbolchatcolli.prs`**

**Trigger logic** (evaluated in reverse: spec3 → spec2 → spec1):
- None of the three switch flags set → show spec1's SC
- spec1's flag set, spec2/spec3 not set → show spec2's SC
- spec2's flag set, spec3 not set → show spec3's SC
- spec3's flag set → show nothing

In other words, a single Symbol Chat Object can switch between 3 images
based on story progress.

### Path B: script `symbol_chat_create`

`0xF8A6`, qedit name `symbol_chat_create`, newserv name `set_symbol_chat_collision`.
Takes 10 registers as an `R_REG_SET_FIXED, 10`:

| regsA | Meaning |
|---|---|
| 0..2 | Position x, y, z (int) |
| 3 | radius |
| 4..6 | spec1 / spec2 / spec3 (same as above) |
| 7..9 | **dlabels: 3 pointers to 60-byte SymbolChat structures** |

Key property (`Map.cc:1019`):

> The entry index is ignored if the corresponding data label from the
> F8A6 quest opcode is not null (and the data from the label is used
> instead).

So when a dlabel is non-null, the built-in index into `symbolchatcolli.prs`
is **completely overridden**, and the game uses the 60 bytes of custom data
supplied by the script directly. This corresponds to the "Mark label as:
Symbol Chat" feature in this repo's quest editor — `SymbolChatData` cannot
be auto-detected by opcode because the dlabel is passed indirectly through
registers, which static analysis cannot see.

GameCube note: `SymbolChatT`'s byte order on GC is wrong —
`set_symbol_chat_collision` does not byte-swap correctly, so GC quests must
use the big-endian `SymbolChatBE`: the first 32-bit field and the four
following 16-bit fields must all be manually byteswapped. The BE detection
heuristic in this repo's `SymbolChatDialog.kt` is exactly for this.

## `SymbolChatT` (60 bytes)

Both the custom path (dlabel) and the built-in path (symbolchatcolli.prs)
decode to the same 60-byte payload. Preview rendering only needs one entry
point to handle it.

Sources: qedit `FSymbolChat.pas:10..21` (`TSymbolData`) and newserv
`PlayerSubordinates.hh:1082..1119` (`SymbolChatT<BE>`).

```
offset  size  field
+0x00   4     spec : u32          bitfield for face shape/color/sound etc.
+0x04   8     corners[4]          2 bytes each (icon u8, param u8)
+0x0C   48    parts[12]           4 bytes each (partId, posX, posY, mirror)
+0x3C         (60 bytes total)
```

**`spec` u32 bitfield** (from newserv `PlayerSubordinates.hh:1092`):

```
----------------------DMSSSCCCFF
                      | |  |  |
                      | |  |  +-- F:2  face shape  (qedit `face and 3`)
                      | |  +----- C:3  face color  (qedit `(face shr 2) and 7`)
                      | +-------- S:3  sound id
                      +---------- M:1  mute sound
                                  D:1  capture
```

qedit only uses the F + C 5 bits. newserv additionally documents the S/M/D
5 bits; the remaining 22 high bits are unused.

**`corners[i]` 2 bytes** (from qedit's perspective, followed by phantasmal-world):

| Byte | Field | Bitfield |
|---|---|---|
| 0 | `icon` (u8) | Index into the sega part table; `0xFF` = corner not rendered |
| 1 | `param` (u8) | `color:3 + mirrorH:1 + mirrorV:1` |

newserv merges the two bytes into a single u16 `corner_objects[i]` with
bitfield `---VHCCCZZZZZZZZ`. Both descriptions are equivalent on LE; on GC
big-endian the icon/param bytes must be swapped — which is exactly what
phantasmal-world's `SymbolChatDialog.kt:344` does manually.

**`parts[i]` 4 bytes**:

| Offset | Field |
|---|---|
| 0 | `partId` (u8) — sega part index; `0xFF` = empty slot |
| 1 | `posX` (u8) — range 0..63 |
| 2 | `posY` (u8) — range 0..63 |
| 3 | `mirror` (u8) — bits `------VH` |

The GC platform does not need swapping (everything is a single byte).

## `symbolchatcolli.prs` file location and packed structure

The PSO BB client packs it inside `data.gsl`:

```
/Users/wangzhen/Documents/data/data.gsl  →  symbolchatcolli.prs
```

After GSL unpacking it is 704 bytes of PRS-compressed data, **2496 bytes
decompressed**, which is exactly **24 entries × 104 bytes/entry**.

### Single entry 104-byte layout (empirically verified)

```
offset  size  field
+0x00   4     valid_flag : u32     always 0x00000001
+0x04   40    unused              all 0xCD (MSVC heap-uninit pattern)
+0x2C   60    SymbolChatT (LE)    ★ the actual payload, same format as above ★
+0x68         (104 bytes)
```

**Byte order is fixed to LE** — the client only packs the LE version, PC/BB
use it directly, and DC/GC byteswap on the fly at load time. So parsing
`symbolchatcolli.prs` does not need to be platform-specific.

### Verification method

I wrote a script that, for each 104-byte entry, tries various candidate
offsets and reads 60 bytes as a `SymbolChatT`, then checks whether every
field lies in a reasonable range (face shape < 4, color < 8, partId < 256
or 0xFF, coordinates in 0..63, `0xFF` meaning empty slot). Results:

| candidate offset | Pass rate |
|---|---|
| 0 | 95/384 |
| 4 | 119/384 |
| ... 0xCD region ... | Linearly increasing |
| 40 | 383/384 |
| **44 (0x2C)** | **384/384** ✅ |

After decoding all 24 entries (see the "Extraction script" section below),
every face/corner/part field is in range, and the part combinations match
one-to-one with the semantics listed in newserv `Map.cc:1023..1046`
("Drop Meseta", "Push button", etc.).

### About the 0xCD padding

Those 40 bytes are the MSVC debug-build `malloc` uninitialized heap sentinel
value (as opposed to the stack's 0xCC). This tells us that sega's internal
tool at the time was doing `malloc(sizeof(SymbolChatColliEntry))` and then
**only filling `valid_flag` and `data`**, writing the 40 bytes in between
straight to disk with `fwrite` without ever assigning them. The most
likely source structure (speculative, not confirmed):

```c
struct SymbolChatColliEntry {
    uint32_t   valid;       // 4   always 1
    char       name[40];    // 40  entry name/label, never filled
    SymbolChatT data;       // 60
};                          // 104 total
```

40 is a typical name-buffer length in sega's tooling (the PSO client has
plenty of 16/32/40/64-byte fixed-length name fields). At runtime, parsing
should **just skip the first 44 bytes** — there is no need to interpret
the semantics of those 40 bytes.

Neither qedit nor newserv **loads** `symbolchatcolli.prs`, so there is no
reverse-engineered record of this wrapper — the `SymbolChatColliEntry`
above is only an educated guess. To know for sure what those 40 bytes are,
you would have to decompile the function in the PSO client that loads
`symbolchatcolli.prs`.

### Default entries (all 24, from newserv `Map.cc` + qedit.info)

| index | Meaning |
|---|---|
| 0x00 | Drop Meseta |
| 0x01 | Meseta has been dropped |
| 0x02 | Drop 1 weapon |
| 0x03 | Drop 4 weapons |
| 0x04 | Drop 1 shield |
| 0x05 | Drop 4 shields |
| 0x06 | Drop 1 mag |
| 0x07 | Drop 4 mags |
| 0x08 | Drop tool item |
| 0x09 | ???? |
| 0x0A | XXXX |
| 0x0B | All circles like OK |
| 0x0C | Key with Yes |
| 0x0D | Key with Cool |
| 0x0E | Key with ... |
| 0x0F | Go right |
| 0x10 | Go left |
| 0x11 | Push button with gun |
| 0x12 | Key icons |
| 0x13 | Key has been pressed |
| 0x14 | Run/go |
| 0x15 | Push 1 button on |
| 0x16 | Push 2 button on |
| 0x17 | Clock (hurry) |

**It has been empirically verified that the file contains only the 24
entries 0x00..0x17** — file size = 24 × 104 bytes, no more data. So SC IDs
above 0x17 have no corresponding built-in entry in the client; when such
an ID appears in a quest (e.g. sc30 in spec3 of 1c2_e.qst), it is really
a **sentinel value** — combined with the spec3 trigger logic ("no symbol
chat appears" when the switch flag is set), sc30 is never actually looked
up. The preview implementation only needs to support id ∈ [0, 24).

## Extraction script

A reproducible script that pulls `symbolchatcolli.prs` out of GSL and
decodes all 24 entries:

```python
import struct

def prs_decompress(src):
    out = bytearray(); pos = 0; flag = 0; bits = 0
    def rb():
        nonlocal flag, bits, pos
        if bits == 0: flag = src[pos]; pos += 1; bits = 8
        r = flag & 1; flag >>= 1; bits -= 1; return r
    try:
        while pos < len(src):
            if rb():
                out.append(src[pos]); pos += 1
            else:
                if rb():
                    a_low = src[pos]; pos += 1
                    a_high = src[pos]; pos += 1
                    a = (a_high << 8) | a_low
                    if (a >> 3) == 0: break
                    offset = (a >> 3) - 0x2000
                    count = a & 7
                    if count == 0: count = src[pos] + 1; pos += 1
                    else: count += 2
                else:
                    count = rb() << 1; count |= rb(); count += 2
                    offset = src[pos] - 256; pos += 1
                for _ in range(count):
                    out.append(out[len(out) + offset])
    except: pass
    return bytes(out)

# 1) Find symbolchatcolli.prs inside data.gsl
gsl = open('/Users/wangzhen/Documents/data/data.gsl', 'rb').read()
i = 0
while True:
    name = gsl[i:i+0x20].split(b'\x00', 1)[0]
    if not name: break
    if name == b'symbolchatcolli.prs':
        off_pages, size = struct.unpack_from('<II', gsl, i + 0x20)
        comp = gsl[off_pages * 0x800 : off_pages * 0x800 + size]
        break
    i += 0x30

# 2) PRS decompress
dec = prs_decompress(comp)
assert len(dec) == 2496 and len(dec) % 104 == 0

# 3) Slice 24 entries
entries = [dec[r*104 + 44 : r*104 + 104] for r in range(24)]
# Each entry is a 60-byte SymbolChatT (LE) that can be fed directly to SymbolChatRenderer
```

## Case study: challenge 1c2_e.qst

qedit's `Quests/Other/chl/ep1/1c2_e.qst` corresponds to newserv's
`system/quests/challenge-ep1/c88102-*` series.

Searching the disassembly of all four platforms (BB/DC/GC/PC) for the
`0xF8A6` opcode: **0 hits in every one**. That is, this quest does not
use `symbol_chat_create` at all.

On the other hand, after PRS-decompressing `c88102-bb.dat` and scanning
the ObjectTable, **11 SymbolChatObjects with typeId=0x21** are found,
spread across areas 3/4/5/7 (Cave 1/2/3 + Mine 2):

```
area=3 #40  pos=(  0, -0,  8) r=32 spec1=(sw81,sc30) spec2=(sw0,sc23) spec3=(sw0,sc30)
area=3 #69  pos=(  0, -6,  7) r=13 spec1=(sw252,sc17) ...
area=3 #80  pos=(  0,115,  1) r=30 spec1=(sw85,sc2)  ...
area=3 #139 pos=(  0, -0, 15) r=55 spec1=(sw60,sc23) ...
area=4 #58  pos=(  0,  0,  5) r=18 spec1=(sw20,sc15) ...
area=4 #69  pos=(  0,  0, 10) r=36 spec1=(sw8,sc9)   ...
area=5 #24  pos=(  0,  0,  2) r=30 spec1=(sw61,sc3)  ...
area=5 #32  pos=(  0,  0,  2) r=25 spec1=(sw99,sc4)  ...
area=7 #60  pos=(  0,  0,  1) r=40 spec1=(sw60,sc5)  ...
area=7 #110 pos=(  0,183,  5) r=16 spec1=(sw122,sc9) ...
area=7 #170 pos=(  0,102,  4) r=38 spec1=(sw100,sc2) ...
```

**Conclusion: every symbol chat in 1c2_e.qst is DAT-static-placed (path A)
and displayed entirely through built-in indices into `symbolchatcolli.prs`,
with no script-side override whatsoever.**

The "cave1 map1" signpost you see in-game is one of these — the low 16 bits
of spec1 map directly to the index in `symbolchatcolli.prs`. Every SC ID in
the spec1 slots falls within 0..23, matching the 24-entry built-in table
exactly. The sc30 appearing in spec2/spec3 slots is the sentinel value
(meaning "this spec displays nothing") and is never actually looked up.

## Related code in this repo

| Path | Purpose |
|---|---|
| `psolib/.../fileFormats/quest/ObjectType.kt:401` | `SymbolChatObject` entity definition |
| `web/.../questEditor/widgets/SymbolChatDialog.kt` | 60-byte SymbolChat editor (for the dlabel path) |
| `web/.../questEditor/widgets/SymbolChatRenderer.kt` | SymbolChat canvas rendering |
| `web/.../questEditor/asm/DataLabelAnalysis.kt:21` | Note: SymbolChat labels have no opcode-based detection and must be marked manually |

### Known inconsistencies / to-do

1. **`opcodes.yml` 0xF8A6 does not annotate dlabel arguments**. The current
   definition is just `R_REG_SET_FIXED, 10`; the three dlabels are passed
   indirectly through register values. This is why static analysis cannot
   detect SymbolChat labels.

## Editor UI usage

SymbolChatObject has no separate inline preview — its preview and
interaction both live inside the 3D viewport, since the object itself is
a spatial entity.

### 3D billboard (read-only preview)

When a SymbolChatObject is selected, `SymbolChatBillboardManager` renders
a billboard above it facing the camera at constant screen size. The
billboard walks `SC ID 1 / SC ID 2 / SC ID 3` and displays a strip for
**every** slot whose id is in [0, 24). Each strip carries a small orange
"**S1**" / "**S2**" / "**S3**" badge naming the spec slot it came from.
The plane width scales with the number of strips, so a 3-stage object
reads as visibly wider in the viewport than a 1-stage one.

The editor has no way to know a player's runtime switch state, so it
can't pick which single stage is "currently active". Showing all
in-range stages side-by-side is more honest than choosing one and
implying it's the authoritative view.

If all three ids are sentinels (no slot in [0, 24)), the billboard shows
a single-strip gray "nothing at any stage" placeholder so "deliberately
silent throughout" is distinguishable from "missing configuration".

### Edit popup (interactive)

Right-click a SymbolChatObject in the 3D viewport → **Edit symbol chat…**
opens `SymbolChatEditPopup`, a three-section editor mirroring the runtime
cascade:

- **Shown by default** (spec1) — switch flag input + hide checkbox + 24-
  preset picker grid
- **Shown after stage 1's switch flips** (spec2) — same three controls
- **Shown after stage 2's switch flips** (spec3) — same three controls

Clicking a thumbnail assigns that preset to the corresponding `SC ID N`;
ticking *hide* writes `SymbolChatColliTable.HIDE_SENTINEL_ID` (30) and
unticking restores the most recent in-range id the stage has held. All
edits route through `EditEntityPropCommand`, so each is independently
undoable.

If the selection changes while the popup is open (to a non-SC entity or
a different SymbolChatObject), the popup closes rather than silently
retargeting — edits can never land on the wrong entity.

### Custom HEX symbol chat (`set_symbol_chat_collision`)

The script-path (HEX) symbol chats referenced by `set_symbol_chat_collision`
are edited through `SymbolChatDialog` in the ASM editor — right-click a
label flagged as `SymbolChatHexData` → **Edit Symbol Chat…**. That dialog
handles the 4-byte extended header before the standard 60-byte layout.

## Sega colour palette (`SEGA_COLORS`)

`SymbolChatRenderer` and `SymbolChatDialog` share an 8-entry (+ 1 white
internal) palette that maps the 3-bit colour field in `spec` / `param` to
RGB fill colours. The correct values were calibrated by pixel-sampling the
PSO BB in-game rendering of `symbolchatcolli` entry **#23** (sc23,
"Clock/hurry", spec `0x00000294`):

| Index | Field values that use it | Game colour | `SEGA_COLORS` hex |
|---|---|---|---|
| 0 | face or corner | light gray | `0xCFCFCF` |
| 1 | face (entries #1,3,5,7) | dark blue | `0x3C50F3` |
| 2 | face (entries #0,2,4,6,8,10,13) | sky blue | `0x2FA3FF` |
| 3 | corner (entries #11 all corners; #23 corner[1]) | **yellow** | `0xF0F000` |
| 4 | face (entry #11); corner (many) | green | `0x79FD79` |
| 5 | face (entries #9,12,19,20,**23**) | **cornflower blue** | `0x88A8E8` |
| 6 | face (entries #14-18,21,22); corner (#23 corner[3]) | **lavender/violet** | `0xB492D1` |
| 7 | (unused in symbolchatcolli) | dark gray | `0x787878` |
| 8 | internal — parts always render white | white | `0xFFFFFF` |

### Calibration evidence

Entry #23 binary data (`symbolchatcolli.bin`, offset `23×104+44 = 2436`):

```
spec = 0x00000294
  face_shape = spec & 3          = 0   (round face)
  face_color = (spec >> 2) & 7   = 5   → game renders BLUE  → SEGA_COLORS[5]
corners:
  [0] icon=0x58, param_color=0   → game renders LIGHT GRAY → SEGA_COLORS[0]
  [1] icon=0x58, param_color=3   → game renders YELLOW     → SEGA_COLORS[3]
  [2] icon=0x58, param_color=4   → game renders GREEN      → SEGA_COLORS[4]
  [3] icon=0x58, param_color=6   → game renders PURPLE     → SEGA_COLORS[6]
```

Pixel sampling of the in-game screenshot (PNG, no lossy compression):

| Region | Filtered pure-fill avg | Adopted hex |
|---|---|---|
| Blue face fill | `(136, 168, 232)` | `0x88A8E8` |
| Yellow corner fill | peak `(237, 237, 13)` → R≈G, B≈0 | `0xF0F000` |
| Purple corner fill | `(180, 146, 209)` | `0xB492D1` |
| Gray corner fill | `(211, 204, 204)` (approx) | `0xCFCFCF` (unchanged) |
| Green corner fill | confirmed acceptable | `0x79FD79` (unchanged) |

### Previously incorrect values (before this fix)

The original palette had three wrong entries — `0x04FDDF` (cyan) at index 3,
`0xFAAC87` (peach/salmon) at index 5, and `0xF68BD5` (hot pink) at index 6.
These caused entry #23 to render as a peach face with cyan and pink corners
instead of the correct blue face with yellow and purple corners.

## Future work: preview support

For preview rendering, **both paths decode to the same 60-byte
`SymbolChatT`**, so they can share `SymbolChatRenderer`. What needs to be
added:

| Task | Suggested location | Notes |
|---|---|---|
| `SymbolChatColliTable.parse(buffer)` | `psolib/.../battleparam` or a new `psolib/.../symbolchat` | Input is the 2496 bytes after PRS decompression; output is `List<SymbolChatData>(24)`. Each entry is sliced as `record[r*104+44 : r*104+104]` |
| `SymbolChatColliRepository` | `web/.../questEditor/loading` | Modeled on `BattleParamRepository`: load from GSL or a standalone resource and cache |
| Asset preparation | `web/src/jsMain/resources/assets/symbol_chat/` | Bundle the 2496 bytes extracted from `symbolchatcolli.prs` straight into the web resources |
| SymbolChatObject property panel preview | `web/.../questEditor` entity inspector | Use `SC ID 1` to index into the repo above and feed it to `SymbolChatRenderer.render(...)` |
| Thumbnails in the `SymbolChatDialog` dlabel list | `SymbolChatDialog.kt` | The existing renderer can be reused directly — loop `SymbolChatRenderer.render` onto small canvases |

Caveats:
- **The built-in path does not need GC BE detection** — `symbolchatcolli.prs`
  is always LE.
- **The custom path** keeps the existing GC BE heuristic in
  `SymbolChatDialog.kt:329`.
- Out-of-range SC IDs like sc30 in spec3 of 1c2_e.qst should be treated
  as "no display" during preview rather than rendering blank or throwing
  an error — consistent with the client's spec3 trigger logic.
- The data source `symbolchatcolli.prs` lives inside `data.gsl`. This
  repo does not include PSO client data files, so you either need to
  extract it from a user-provided `data.gsl` at build time, or check in
  the decoded 2496 bytes as a standalone small resource (simpler, and
  the file is tiny).
