import { Xvm, XvmTexture } from "../data_formats/parsing/ninja/texture";
import {
    Texture,
    LinearFilter,
    RGBA_S3TC_DXT3_Format,
    RGBA_S3TC_DXT1_Format,
    CompressedTexture,
} from "three";

export function xvm_to_textures(xvm: Xvm): Texture[] {
    return xvm.textures.map(xvm_texture_to_texture);
}

export function xvm_texture_to_texture(tex: XvmTexture): Texture {
    const texture_3js = new CompressedTexture(
        [
            {
                data: new Uint8Array(tex.data) as any,
                width: tex.width,
                height: tex.height,
            },
        ],
        tex.width,
        tex.height
    );

    switch (tex.format[1]) {
        case 6:
            texture_3js.format = RGBA_S3TC_DXT1_Format as any;
            break;
        case 7:
            if (tex.format[0] === 2) {
                texture_3js.format = RGBA_S3TC_DXT3_Format as any;
            } else {
                throw new Error(`Format[0] ${tex.format[0]} not supported.`);
            }
            break;
        default:
            throw new Error(`Format[1] ${tex.format[1]} not supported.`);
    }

    texture_3js.minFilter = LinearFilter;
    texture_3js.needsUpdate = true;

    return texture_3js;
}
