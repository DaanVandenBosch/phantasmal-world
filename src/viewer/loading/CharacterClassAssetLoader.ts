import { HttpClient } from "../../core/HttpClient";
import { NjObject, parse_nj } from "../../core/data_formats/parsing/ninja";
import { ArrayBufferCursor } from "../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../core/data_formats/Endianness";
import { NjcmModel } from "../../core/data_formats/parsing/ninja/njcm";
import { NjMotion, parse_njm } from "../../core/data_formats/parsing/ninja/motion";
import { Disposable } from "../../core/observable/Disposable";
import { DisposablePromise } from "../../core/DisposablePromise";
import {
    CharacterClassModel,
    FOMAR,
    FOMARL,
    FONEWEARL,
    FONEWM,
    HUCASEAL,
    HUCAST,
    HUMAR,
    HUNEWEARL,
    RACASEAL,
    RACAST,
    RAMAR,
    RAMARL,
} from "../model/CharacterClassModel";
import { parse_xvm, XvrTexture } from "../../core/data_formats/parsing/ninja/texture";
import { parse_afs } from "../../core/data_formats/parsing/afs";
import { SectionId } from "../../core/model";
import { unwrap } from "../../core/Result";

export class CharacterClassAssetLoader implements Disposable {
    private readonly nj_object_cache: Map<
        string,
        DisposablePromise<NjObject<NjcmModel>>
    > = new Map();
    private readonly xvr_texture_cache: Map<
        string,
        DisposablePromise<readonly XvrTexture[]>
    > = new Map();
    private readonly nj_motion_cache: Map<number, DisposablePromise<NjMotion>> = new Map();

    constructor(private readonly http_client: HttpClient) {}

    dispose(): void {
        for (const promise of this.nj_object_cache.values()) {
            promise.dispose();
        }

        for (const promise of this.nj_motion_cache.values()) {
            promise.dispose();
        }

        this.nj_object_cache.clear();
        this.nj_motion_cache.clear();
    }

    load_geometry(model: CharacterClassModel): Promise<NjObject<NjcmModel>> {
        let nj_object = this.nj_object_cache.get(model.name);

        if (!nj_object) {
            nj_object = this.load_all_nj_objects(model);
            this.nj_object_cache.set(model.name, nj_object);
        }

        return nj_object;
    }

    /**
     * Loads the separate body parts and joins them together at the right bones.
     */
    private load_all_nj_objects(
        model: CharacterClassModel,
    ): DisposablePromise<NjObject<NjcmModel>> {
        const tex_ids = texture_ids(model, SectionId.Viridia, 0);

        return this.load_body_part_geometry(model.name, "Body").then(body => {
            if (!body) {
                throw new Error(`Couldn't load body for player class ${model.name}.`);
            }

            return this.load_body_part_geometry(model.name, "Head", 0).then(head => {
                if (!head) {
                    return body;
                }

                // Shift by 1 for the section ID and once for every body texture ID.
                let shift = 1 + tex_ids.body.length;
                this.shift_texture_ids(head, shift);
                this.add_to_bone(body, head, 59);

                if (model.hair_style_count === 0) {
                    return body;
                }

                return this.load_body_part_geometry(model.name, "Hair", 0).then(hair => {
                    if (!hair) {
                        return body;
                    }

                    shift += tex_ids.head.length;
                    this.shift_texture_ids(hair, shift);
                    this.add_to_bone(head, hair, 0);

                    if (!model.hair_styles_with_accessory.has(0)) {
                        return body;
                    }

                    return this.load_body_part_geometry(model.name, "Accessory", 0).then(
                        accessory => {
                            if (accessory) {
                                shift += tex_ids.hair.length;
                                this.shift_texture_ids(accessory, shift);
                                this.add_to_bone(hair, accessory, 0);
                            }

                            return body;
                        },
                    );
                });
            });
        });
    }

    private load_body_part_geometry(
        player_class: string,
        body_part: string,
        no?: number,
    ): DisposablePromise<NjObject<NjcmModel> | undefined> {
        return this.http_client
            .get(character_class_to_url(player_class, body_part, no))
            .array_buffer()
            .then(buffer => unwrap(parse_nj(new ArrayBufferCursor(buffer, Endianness.Little)))[0]);
    }

    /**
     * Shift texture IDs so that the IDs of different body parts don't overlap.
     */
    private shift_texture_ids(nj_object: NjObject<NjcmModel>, shift: number): void {
        if (nj_object.model) {
            for (const mesh of nj_object.model.meshes) {
                if (mesh.texture_id != undefined) {
                    mesh.texture_id += shift;
                }
            }
        }

        for (const child of nj_object.children) {
            this.shift_texture_ids(child, shift);
        }
    }

    private add_to_bone(object: NjObject, head_part: NjObject, bone_id: number): void {
        const bone = object.get_bone(bone_id);

        if (bone) {
            bone.evaluation_flags.hidden = false;
            bone.evaluation_flags.break_child_trace = false;
            bone.add_child(head_part);
        }
    }

    async load_textures(
        model: CharacterClassModel,
        section_id: SectionId,
        body: number,
    ): Promise<readonly (XvrTexture | undefined)[]> {
        let xvr_textures = this.xvr_texture_cache.get(model.name);

        if (!xvr_textures) {
            xvr_textures = this.http_client
                .get(`/player/${model.name}Tex.afs`)
                .array_buffer()
                .then(buffer => {
                    const afs_result = parse_afs(new ArrayBufferCursor(buffer, Endianness.Little));
                    const textures: XvrTexture[] = [];

                    if (afs_result.success) {
                        for (const file of afs_result.value) {
                            const xvm = parse_xvm(new ArrayBufferCursor(file, Endianness.Little));

                            if (xvm.success) {
                                textures.push(...xvm.value.textures);
                            }
                        }
                    }

                    return textures;
                });
        }

        const tex = await xvr_textures;
        const tex_ids = texture_ids(model, section_id, body);

        return [
            tex_ids.section_id,
            ...tex_ids.body,
            ...tex_ids.head,
            ...tex_ids.hair,
            ...tex_ids.accessories,
        ].map(idx => (idx == undefined ? undefined : tex[idx]));
    }

    load_animation(animation_id: number, bone_count: number): Promise<NjMotion> {
        let nj_motion = this.nj_motion_cache.get(animation_id);

        if (!nj_motion) {
            nj_motion = this.http_client
                .get(`/player/animation/animation_${animation_id.toString().padStart(3, "0")}.njm`)
                .array_buffer()
                .then(buffer =>
                    parse_njm(new ArrayBufferCursor(buffer, Endianness.Little), bone_count),
                );
        }

        return nj_motion;
    }
}

function character_class_to_url(player_class: string, body_part: string, no?: number): string {
    return `/player/${player_class}${body_part}${no == null ? "" : no}.nj`;
}

function texture_ids(
    model: CharacterClassModel,
    section_id: SectionId,
    body: number,
): {
    section_id: number;
    body: number[];
    head: number[];
    hair: (number | undefined)[];
    accessories: (number | undefined)[];
} {
    switch (model) {
        case HUMAR: {
            const body_idx = body * 3;
            return {
                section_id: section_id + 126,
                body: [body_idx, body_idx + 1, body_idx + 2, body + 108],
                head: [54, 55],
                hair: [94, 95],
                accessories: [],
            };
        }
        case HUNEWEARL: {
            const body_idx = body * 13;
            return {
                section_id: section_id + 299,
                body: [
                    body_idx + 13,
                    body_idx,
                    body_idx + 1,
                    body_idx + 2,
                    body_idx + 3,
                    277,
                    body + 281,
                ],
                head: [235, 239],
                hair: [260, 259],
                accessories: [],
            };
        }
        case HUCAST: {
            const body_idx = body * 5;
            return {
                section_id: section_id + 275,
                body: [body_idx, body_idx + 1, body_idx + 2, body + 250],
                // Eyes don't look correct because NJCM material chunks (which contain alpha blending
                // details) aren't parsed yet. Material.blending should be AdditiveBlending.
                head: [body_idx + 3, body_idx + 4],
                hair: [],
                accessories: [],
            };
        }
        case HUCASEAL: {
            const body_idx = body * 5;
            return {
                section_id: section_id + 375,
                body: [body_idx, body_idx + 1, body_idx + 2],
                head: [body_idx + 3, body_idx + 4],
                hair: [],
                accessories: [],
            };
        }
        case RAMAR: {
            const body_idx = body * 7;
            return {
                section_id: section_id + 197,
                body: [body_idx + 4, body_idx + 5, body_idx + 6, body + 179],
                head: [126, 127],
                hair: [166, 167],
                accessories: [undefined, undefined, body_idx + 2],
            };
        }
        case RAMARL: {
            const body_idx = body * 16;
            return {
                section_id: section_id + 322,
                body: [body_idx + 15, body_idx + 1, body_idx],
                head: [288],
                hair: [308, 309],
                accessories: [undefined, undefined, body_idx + 8],
            };
        }
        case RACAST: {
            const body_idx = body * 5;
            return {
                section_id: section_id + 300,
                body: [body_idx, body_idx + 1, body_idx + 2, body_idx + 3, body + 275],
                head: [body_idx + 4],
                hair: [],
                accessories: [],
            };
        }
        case RACASEAL: {
            const body_idx = body * 5;
            return {
                section_id: section_id + 375,
                body: [body + 350, body_idx, body_idx + 1, body_idx + 2],
                head: [body_idx + 3],
                hair: [body_idx + 4],
                accessories: [],
            };
        }
        case FOMAR: {
            const body_idx = body === 0 ? 0 : body * 15 + 2;
            return {
                section_id: section_id + 310,
                body: [body_idx + 12, body_idx + 13, body_idx + 14, body_idx],
                head: [276, 272],
                hair: [undefined, 296, 297],
                accessories: [body_idx + 4],
            };
        }
        case FOMARL: {
            const body_idx = body * 16;
            return {
                section_id: section_id + 326,
                body: [body_idx, body_idx + 2, body_idx + 1, 322 /*hands*/],
                head: [288],
                hair: [undefined, undefined, 308],
                accessories: [body_idx + 3, body_idx + 4],
            };
        }
        case FONEWM: {
            const body_idx = body * 17;
            return {
                section_id: section_id + 344,
                body: [body_idx + 4, 340 /*hands*/, body_idx, body_idx + 5],
                head: [306, 310],
                hair: [undefined, undefined, 330],
                // ID 16 for glasses is incorrect but looks decent.
                accessories: [body_idx + 6, body_idx + 16, 330],
            };
        }
        case FONEWEARL: {
            const body_idx = body * 26;
            return {
                section_id: section_id + 505,
                body: [body_idx + 1, body_idx, body_idx + 2, 501 /*hands*/],
                head: [472, 468],
                hair: [undefined, undefined, 492],
                accessories: [body_idx + 12, body_idx + 13],
            };
        }
        default:
            throw new Error(`No textures for character class ${model.name}.`);
    }
}
