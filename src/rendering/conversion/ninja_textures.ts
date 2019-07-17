import {
    CompressedTexture,
    LinearFilter,
    MirroredRepeatWrapping,
    RGBA_S3TC_DXT1_Format,
    RGBA_S3TC_DXT3_Format,
    Texture,
    CompressedPixelFormat,
} from "three";
import { Xvm, XvmTexture } from "../../data_formats/parsing/ninja/texture";

export function xvm_to_textures(xvm: Xvm): Texture[] {
    return xvm.textures.map(xvm_texture_to_texture);
}

export function xvm_texture_to_texture(tex: XvmTexture): Texture {
    let format: CompressedPixelFormat;
    let data_size: number;

    // Ignore mipmaps.
    switch (tex.format[1]) {
        case 6:
            format = RGBA_S3TC_DXT1_Format;
            data_size = (tex.width * tex.height) / 2;
            break;
        case 7:
            format = RGBA_S3TC_DXT3_Format;
            data_size = tex.width * tex.height;
            break;
        default:
            throw new Error(`Format ${tex.format.join(", ")} not supported.`);
    }

    const texture_3js = new CompressedTexture(
        [
            {
                data: new Uint8Array(tex.data, 0, data_size) as any,
                width: tex.width,
                height: tex.height,
            },
        ],
        tex.width,
        tex.height,
        format
    );

    texture_3js.minFilter = LinearFilter;
    texture_3js.wrapS = MirroredRepeatWrapping;
    texture_3js.wrapT = MirroredRepeatWrapping;
    texture_3js.needsUpdate = true;

    return texture_3js;
}
