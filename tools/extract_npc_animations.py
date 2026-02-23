"""
Extract NPC animations from PSO Blue Burst game data (BML files inside GSL archive).
Matches decompressed NJ models against existing assets/npcs/*.nj files to determine
which BML belongs to which NPC, then extracts the NJM animation files.

Usage: python extract_npc_animations.py <game_data_dir> <assets_dir>
Example: python extract_npc_animations.py "D:/PSO/EphineaPSO/data" "web/src/jsMain/resources/assets/npcs"
"""

import struct
import sys
import os
from pathlib import Path


# --- PRS Decompression (port of Kotlin PrsDecompress.kt) ---

def prs_decompress(data: bytes) -> bytes:
    src = memoryview(data)
    src_pos = 0
    dst = bytearray()
    flags = 0
    flag_bits_left = 0

    def read_byte():
        nonlocal src_pos
        if src_pos >= len(src):
            raise EOFError("Unexpected end of PRS data")
        b = src[src_pos]
        src_pos += 1
        return b

    def read_flag_bit():
        nonlocal flags, flag_bits_left
        if flag_bits_left == 0:
            flags = read_byte()
            flag_bits_left = 8
        bit = flags & 1
        flags >>= 1
        flag_bits_left -= 1
        return bit

    def offset_copy(offset, size):
        src_idx = len(dst) + offset
        if src_idx < 0:
            raise ValueError(f"offset_copy: src_idx={src_idx} < 0")
        for i in range(size):
            dst.append(dst[src_idx + (i % (-offset))])

    while True:
        if read_flag_bit() == 1:
            # Literal byte copy
            dst.append(read_byte())
        else:
            if read_flag_bit() == 0:
                # Short copy
                size = 2 + ((read_flag_bit() << 1) | read_flag_bit())
                offset = read_byte() - 256
                offset_copy(offset, size)
            else:
                # Long copy or EOF
                lo = read_byte()
                hi = read_byte()
                offset = (hi << 8) | lo
                if offset == 0:
                    break  # End of data
                size = offset & 0b111
                offset >>= 3
                if size == 0:
                    size = read_byte() + 1
                else:
                    size += 2
                offset -= 8192
                offset_copy(offset, size)

    return bytes(dst)


# --- BML Parsing ---

def parse_bml(data: bytes):
    """Parse BML and return list of (name, model_data_compressed, tex_data_compressed)."""
    if len(data) < 0x40:
        return []

    num_entries = struct.unpack_from('<I', data, 4)[0]
    entries = []

    # Parse entry headers
    headers = []
    pos = 0x40
    for i in range(num_entries):
        if pos + 0x40 > len(data):
            break
        name = data[pos:pos + 0x20].split(b'\x00')[0].decode('ascii', errors='replace')
        comp_size = struct.unpack_from('<I', data, pos + 0x20)[0]
        comp_gvm_size = struct.unpack_from('<I', data, pos + 0x2C)[0]
        headers.append((name, comp_size, comp_gvm_size))
        pos += 0x40

    # Read data starting at 0x800
    data_offset = 0x800
    for name, comp_size, comp_gvm_size in headers:
        if data_offset + comp_size > len(data):
            break
        model_data = data[data_offset:data_offset + comp_size]
        data_offset += align_up(comp_size, 0x20)

        tex_data = b''
        if comp_gvm_size > 0:
            if data_offset + comp_gvm_size <= len(data):
                tex_data = data[data_offset:data_offset + comp_gvm_size]
                data_offset += align_up(comp_gvm_size, 0x20)

        entries.append((name, model_data, tex_data))

    return entries


def align_up(value, alignment):
    mask = alignment - 1
    return (value + mask) & ~mask


# --- GSL Parsing ---

def parse_gsl(data: bytes):
    """Parse GSL and return dict of name -> (offset, size)."""
    entries = {}
    pos = 0
    while pos + 0x30 <= len(data):
        if data[pos] == 0:
            break
        name = data[pos:pos + 0x20].split(b'\x00')[0].decode('ascii', errors='replace')
        offset_pages, size = struct.unpack_from('<II', data, pos + 0x20)
        entries[name] = (offset_pages * 0x800, size)
        pos += 0x30
    return entries


# --- Main extraction logic ---

def find_bml_entries(game_data_dir: str):
    """Find all enemy BML entries from GSL archive and standalone files."""
    bml_data = {}  # name -> raw bytes

    # Read GSL archive
    gsl_path = os.path.join(game_data_dir, 'data.gsl')
    if os.path.exists(gsl_path):
        with open(gsl_path, 'rb') as f:
            gsl_data = f.read()
        gsl_entries = parse_gsl(gsl_data)
        for name, (offset, size) in gsl_entries.items():
            if name.startswith('bm_ene_') and name.endswith('.bml'):
                bml_data[name] = gsl_data[offset:offset + size]
            elif name.startswith('bm_boss') and name.endswith('.bml'):
                bml_data[name] = gsl_data[offset:offset + size]

    # Read standalone BML files
    for fname in os.listdir(game_data_dir):
        if fname.endswith('.bml') and (fname.startswith('bm_ene_') or fname.startswith('bm_boss')):
            fpath = os.path.join(game_data_dir, fname)
            with open(fpath, 'rb') as f:
                bml_data[fname] = f.read()

    return bml_data


def main():
    if len(sys.argv) < 3:
        print(f"Usage: {sys.argv[0]} <game_data_dir> <assets_npcs_dir>")
        sys.exit(1)

    game_data_dir = sys.argv[1]
    assets_dir = sys.argv[2]

    if not os.path.isdir(game_data_dir):
        print(f"Error: {game_data_dir} is not a directory")
        sys.exit(1)

    if not os.path.isdir(assets_dir):
        print(f"Error: {assets_dir} is not a directory")
        sys.exit(1)

    # Load existing NJ files from assets for matching
    print("Loading existing NJ assets for matching...")
    existing_nj = {}  # decompressed_bytes -> npc_name
    for fname in os.listdir(assets_dir):
        if fname.endswith('.nj'):
            npc_name = fname[:-3]
            fpath = os.path.join(assets_dir, fname)
            with open(fpath, 'rb') as f:
                existing_nj[f.read()] = npc_name
    print(f"  Loaded {len(existing_nj)} NJ files")

    # Find and process BML files
    print("Finding BML files...")
    bml_files = find_bml_entries(game_data_dir)
    print(f"  Found {len(bml_files)} BML files")

    matched = 0
    extracted = 0

    for bml_name, bml_raw in sorted(bml_files.items()):
        entries = parse_bml(bml_raw)
        if not entries:
            continue

        # Try to match NJ entries against existing assets
        npc_name = None
        for entry_name, model_data, tex_data in entries:
            if entry_name.endswith('.nj'):
                try:
                    decompressed = prs_decompress(model_data)
                    if decompressed in existing_nj:
                        npc_name = existing_nj[decompressed]
                        break
                except Exception as e:
                    pass

        if npc_name is None:
            continue

        matched += 1

        # Extract NJM animation entries
        njm_entries = [(name, data) for name, data, _ in entries if name.endswith('.njm')]
        if not njm_entries:
            continue

        print(f"  {bml_name} -> {npc_name} ({len(njm_entries)} animations)")

        for entry_name, model_data in njm_entries:
            # Extract animation name from entry (e.g., "wait_darkand_body.njm" -> "wait")
            anim_name = entry_name.split('_')[0]

            out_path = os.path.join(assets_dir, f"{npc_name}_{anim_name}.njm")
            try:
                decompressed = prs_decompress(model_data)
                with open(out_path, 'wb') as f:
                    f.write(decompressed)
                extracted += 1
            except Exception as e:
                print(f"    Warning: Failed to decompress {entry_name}: {e}")

    print(f"\nDone! Matched {matched} BML files, extracted {extracted} animations.")


if __name__ == '__main__':
    main()
