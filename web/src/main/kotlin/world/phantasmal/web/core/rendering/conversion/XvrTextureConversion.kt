package world.phantasmal.web.core.rendering.conversion

import org.khronos.webgl.Uint16Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.fileFormats.ninja.XvrTexture
import world.phantasmal.web.externals.three.*
import world.phantasmal.webui.obj
import kotlin.math.roundToInt

fun xvrTextureToThree(xvr: XvrTexture, filter: TextureFilter = LinearFilter): Texture =
    when (xvr.format.second) {
        // D3DFMT_R5G6B5
        2 -> createDataTexture(
            Uint16Array(xvr.data.arrayBuffer),
            xvr.width,
            xvr.height,
            RGBFormat,
            UnsignedShort565Type,
            filter,
        )
        // D3DFMT_A1R5G5B5
        3 -> {
            val originalData = Uint16Array(xvr.data.arrayBuffer)
            val data = Uint16Array(originalData.length)

            // Change bit order from ARGB 1555 to RGBA 5551.
            for (i in 0 until originalData.length) {
                val x = originalData[i].toInt()
                data[i] = ((x shl 1) or (x ushr 15)).toShort()
            }

            createDataTexture(
                data,
                xvr.width,
                xvr.height,
                RGBAFormat,
                UnsignedShort5551Type,
                filter,
            )
        }
        // D3DFMT_DXT1
        6 -> createCompressedTexture(
            Uint8Array(xvr.data.arrayBuffer, 0, (xvr.width * xvr.height) / 2),
            xvr.width,
            xvr.height,
            RGBA_S3TC_DXT1_Format,
            filter,
        )
        // D3DFMT_DXT2
        // TODO: Correctly interpret this (DXT2 is basically DXT3 with premultiplied alpha).
        7 -> createCompressedTexture(
            Uint8Array(xvr.data.arrayBuffer, 0, xvr.width * xvr.height),
            xvr.width,
            xvr.height,
            RGBA_S3TC_DXT3_Format,
            filter,
        )
        // 1 -> D3DFMT_A8R8G8B8
        // 4 -> D3DFMT_A4R4G4B4
        // 5 -> D3DFMT_P8
        // 6 -> D3DFMT_R5G6B5
        // 8 -> D3DFMT_DXT3
        // 9 -> D3DFMT_DXT4
        // 10 -> D3DFMT_DXT5
        // 11 -> D3DFMT_A8R8G8B8
        // 12 -> D3DFMT_R5G6B5
        // 13 -> D3DFMT_A1R5G5B5
        // 14 -> D3DFMT_A4R4G4B4
        // 15 -> D3DFMT_YUY2
        // 16 -> D3DFMT_V8U8
        // 17 -> D3DFMT_A8
        // 18 -> D3DFMT_X1R5G5B5
        // 19 -> D3DFMT_X8R8G8B8
        else -> error("Format ${xvr.format.first}, ${xvr.format.second} not supported.")
    }

private fun createDataTexture(
    data: Any,
    width: Int,
    height: Int,
    format: PixelFormat,
    type: TextureDataType,
    filter: TextureFilter,
): DataTexture =
    DataTexture(
        data,
        width,
        height,
        format,
        type,
        wrapS = MirroredRepeatWrapping,
        wrapT = MirroredRepeatWrapping,
        magFilter = filter,
        minFilter = filter,
    )

private fun createCompressedTexture(
    data: Uint8Array,
    width: Int,
    height: Int,
    format: CompressedPixelFormat,
    filter: TextureFilter,
): CompressedTexture {
    val texture = CompressedTexture(
        arrayOf(obj {
            this.data = data
            this.width = width
            this.height = height
        }),
        width,
        height,
        format,
        wrapS = MirroredRepeatWrapping,
        wrapT = MirroredRepeatWrapping,
        magFilter = filter,
        minFilter = filter,
    )
    texture.needsUpdate = true
    return texture
}

private fun xvrTextureToUint8Array(xvr: XvrTexture): Uint8Array {
    val dataSize = when (xvr.format.second) {
        6 -> (xvr.width * xvr.height) / 2
        7 -> xvr.width * xvr.height
        else -> error("Format ${xvr.format.first}, ${xvr.format.second} not supported.")
    }

    val cursor = xvr.data.cursor(size = dataSize)
    val image = Uint8Array(xvr.width * xvr.height * 4)

    val stride = 4 * xvr.width
    var i = 0

    while (cursor.bytesLeft >= 8) {
        // Each block of 4 x 4 pixels is compressed to 8 bytes.
        val c0 = cursor.uShort().toInt() // Color 0
        val c1 = cursor.uShort().toInt() // Color 1
        val codes = cursor.int() // A 2-bit code per pixel.

        // Extract color components and normalize them to the range [0, 1].
        val c0r = (c0 ushr 11) / 31.0
        val c0g = ((c0 ushr 5) and 0x3F) / 63.0
        val c0b = (c0 and 0x1F) / 31.0

        val c1r = (c1 ushr 11) / 31.0
        val c1g = ((c1 ushr 5) and 0x3F) / 63.0
        val c1b = (c1 and 0x1F) / 31.0

        // Loop over the codes.
        for (j in 0 until 16) {
            val shift = 2 * (16 - j - 1)
            val r: Double
            val g: Double
            val b: Double
            val a: Double

            when ((codes ushr shift) and 0b11) {
                0 -> {
                    r = c0r
                    g = c0g
                    b = c0b
                    a = 1.0
                }
                1 -> {
                    r = c1r
                    g = c1g
                    b = c1b
                    a = 1.0
                }
                2 -> {
                    if (c0 > c1) {
                        r = (2 * c0r + c1r) / 3
                        g = (2 * c0g + c1g) / 3
                        b = (2 * c0b + c1b) / 3
                        a = 1.0
                    } else {
                        r = (c0r + c1r) / 2
                        g = (c0g + c1g) / 2
                        b = (c0b + c1b) / 2
                        a = 1.0
                    }
                }
                3 -> {
                    if (c0 > c1) {
                        r = (c0r + 2 * c1r) / 3
                        g = (c0g + 2 * c1g) / 3
                        b = (c0b + 2 * c1b) / 3
                        a = 1.0
                    } else {
                        r = 0.0
                        g = 0.0
                        b = 0.0
                        a = 0.0
                    }
                }
                // Unreachable case.
                else -> error("Invalid code.")
            }

            // Block-relative pixel coordinates.
            val blockX = 3 - j % 4
            val blockY = 3 - j / 4
            // Offset into the image array.
            val offset = i + (4 * blockX + blockY * stride)
            image[offset] = (r * 255).roundToInt().toByte()
            image[offset + 1] = (g * 255).roundToInt().toByte()
            image[offset + 2] = (b * 255).roundToInt().toByte()
            image[offset + 3] = (a * 255).roundToInt().toByte()
        }

        // Jump ahead 4 pixels.
        i += 16

        if (i % stride == 0) {
            // Jump ahead 4 rows.
            i += 3 * stride
        }
    }

    return image
}
