import {
    CompressedPixelFormat,
    CompressedTexture,
    LinearFilter,
    MirroredRepeatWrapping,
    RGBA_S3TC_DXT1_Format,
    RGBA_S3TC_DXT3_Format,
    Texture as ThreeTexture,
} from "three";
import { Xvm, XvrTexture } from "../../data_formats/parsing/ninja/texture";

export function xvm_to_three_textures(xvm: Xvm): ThreeTexture[] {
    return xvm.textures.map(xvr_texture_to_three_texture);
}

export function xvr_texture_to_three_texture(xvr: XvrTexture): ThreeTexture {
    let format: CompressedPixelFormat;
    let data_size: number;

    // Ignore mipmaps.
    switch (xvr.format[1]) {
        case 6:
            format = RGBA_S3TC_DXT1_Format;
            data_size = (xvr.width * xvr.height) / 2;
            break;
        case 7:
            format = RGBA_S3TC_DXT3_Format;
            data_size = xvr.width * xvr.height;
            break;
        default:
            throw new Error(`Format ${xvr.format.join(", ")} not supported.`);
    }

    const texture_3js = new CompressedTexture(
        [
            {
                data: new Uint8Array(xvr.data, 0, data_size) as any,
                width: xvr.width,
                height: xvr.height,
            },
        ],
        xvr.width,
        xvr.height,
        format,
    );

    texture_3js.minFilter = LinearFilter;
    texture_3js.wrapS = MirroredRepeatWrapping;
    texture_3js.wrapT = MirroredRepeatWrapping;
    texture_3js.needsUpdate = true;

    return texture_3js;
}
