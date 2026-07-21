#!/usr/bin/env python3
"""
Extract qedit's TImageList sega-part atlases from FSymbolChat.dfm.

qedit's symbol-chat editor (Form33 in qedit/FSymbolChat.pas) draws using
4 hard-coded 256x256 RGBA bitmaps stored as a TImageList resource embedded
in the DFM. At runtime FormCreate copies them out:

    for x:= 0 to 3 do begin
      segaPics[x] := tbitmap.Create;
      segaPics[x].PixelFormat := pf32bit;
      segaPics[x].Width := 256;
      segaPics[x].Height := 256;
      imagelist1.draw(segaPics[x].Canvas, 0, 0, x);
    end;

Indexing (per DrawSymbolChat in FSymbolChat.pas):
  segaPics[0..2] -> corner icon sets (icons 0..63, 64..127, 128..191)
  segaPics[3]    -> shared atlas: face expressions, parts shapes,
                    background corner tile

Usage:
    python3 tools/extract_qedit_symbol_chat_assets.py \\
        /path/to/qedit/FSymbolChat.dfm \\
        web/src/jsMain/resources/assets/symbol_chat

Writes sega_0.png .. sega_3.png into the output directory.

Requires Pillow:  pip install Pillow
"""
import os
import re
import struct
import sys

try:
    from PIL import Image
except ImportError:
    sys.exit("This script needs Pillow:  pip install Pillow")


# 28-byte TImageList prefix before the embedded BMP. Bytes 8..9 = image
# count (LE u16). The remainder is reserved/transparency colour.
TIMAGELIST_HEADER_LEN = 28


def extract_bitmap_block(dfm_text: str, name: str) -> bytes:
    """Find `object {name}: TImageList` and decode its `Bitmap = { ... }` hex blob."""
    obj_match = re.search(rf"object\s+{re.escape(name)}\s*:\s*TImageList", dfm_text)
    if not obj_match:
        raise ValueError(f"Could not find object {name}")

    blob_match = re.search(r"Bitmap\s*=\s*\{([^}]*)\}", dfm_text[obj_match.end():])
    if not blob_match:
        raise ValueError(f"Could not find Bitmap block for {name}")

    hex_blob = re.sub(r"\s+", "", blob_match.group(1))
    return bytes.fromhex(hex_blob)


def parse_bmp(data: bytes):
    """Decode a packed Windows BMP. Returns (width, height, bpp, pixel_bytes)."""
    if data[0:2] != b"BM":
        raise ValueError("Not a BMP (no BM magic)")
    pixel_offset = struct.unpack_from("<I", data, 0x0A)[0]
    width = struct.unpack_from("<i", data, 0x12)[0]
    height = struct.unpack_from("<i", data, 0x16)[0]
    bpp = struct.unpack_from("<H", data, 0x1C)[0]
    pixels = data[pixel_offset:]
    return width, height, bpp, pixels


def bmp_32bpp_to_rgba(width: int, height: int, pixels: bytes) -> Image.Image:
    """Decode BI_RGB 32bpp pixels (BGRA, bottom-up) into a Pillow RGBA image."""
    row_bytes = width * 4
    image_bytes = bytearray(row_bytes * abs(height))
    for y in range(abs(height)):
        src_row = (abs(height) - 1 - y) * row_bytes if height > 0 else y * row_bytes
        dst_row = y * row_bytes
        for x in range(width):
            b, g, r, a = pixels[src_row + x*4 : src_row + x*4 + 4]
            image_bytes[dst_row + x*4 + 0] = r
            image_bytes[dst_row + x*4 + 1] = g
            image_bytes[dst_row + x*4 + 2] = b
            image_bytes[dst_row + x*4 + 3] = a
    return Image.frombytes("RGBA", (width, abs(height)), bytes(image_bytes))


def slice_atlas(img: Image.Image, n_images: int, tile_w: int, tile_h: int):
    """
    qedit's ImageList1 stores 4 256x256 RGBA images as a 1024x512 BMP.
    The icons live in the TOP 1024x256 strip (4 tiles laid out horizontally)
    and the bottom 256 rows are unused mask data.

    The source BMP is 32bpp with a REAL alpha channel — the ImageList stores
    proper per-pixel alpha (opaque outlines at A=255, anti-aliased edges at
    A=1..254, transparent background at A=0).  We simply use that alpha as-is;
    the old luminance-based reconstruction was wrong because it treated fully-
    opaque black outline pixels (R=G=B=0, A=255) as transparent.
    """
    w, h = img.size
    if w != tile_w * n_images or h not in (tile_h, tile_h * 2):
        raise ValueError(
            f"Unexpected atlas size {w}x{h} for {n_images}x{tile_w}x{tile_h}"
        )

    for i in range(n_images):
        # Crop the tile; bmp_32bpp_to_rgba already wrote the real alpha channel.
        tile = img.crop((i * tile_w, 0, (i + 1) * tile_w, tile_h)).copy()
        yield tile


def main(dfm_path: str, out_dir: str) -> None:
    with open(dfm_path, "r", encoding="latin-1") as f:
        dfm_text = f.read()

    raw = extract_bitmap_block(dfm_text, "ImageList1")
    print(f"ImageList1 raw blob: {len(raw)} bytes")

    bmp = raw[TIMAGELIST_HEADER_LEN:]
    width, height, bpp, pixels = parse_bmp(bmp)
    print(f"  embedded BMP: {width}x{height} {bpp}bpp ({len(pixels)} pixel bytes)")

    if bpp != 32:
        raise NotImplementedError(f"Only 32bpp BMPs handled, got {bpp}")

    img = bmp_32bpp_to_rgba(width, height, pixels)

    os.makedirs(out_dir, exist_ok=True)
    for i, tile in enumerate(slice_atlas(img, n_images=4, tile_w=256, tile_h=256)):
        out_path = os.path.join(out_dir, f"sega_{i}.png")
        tile.save(out_path, "PNG")
        print(f"  -> {out_path}")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit("usage: extract_qedit_symbol_chat_assets.py <FSymbolChat.dfm> <out_dir>")
    main(sys.argv[1], sys.argv[2])
