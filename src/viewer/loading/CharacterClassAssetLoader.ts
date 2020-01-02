import { HttpClient } from "../../core/HttpClient";
import { NjObject, parse_nj } from "../../core/data_formats/parsing/ninja";
import { ArrayBufferCursor } from "../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../core/data_formats/Endianness";
import { NjcmModel } from "../../core/data_formats/parsing/ninja/njcm";
import { NjMotion, parse_njm } from "../../core/data_formats/parsing/ninja/motion";
import { Disposable } from "../../core/observable/Disposable";
import { DisposablePromise } from "../../core/DisposablePromise";
import { CharacterClassModel } from "../model/CharacterClassModel";
import { parse_xvm, XvrTexture } from "../../core/data_formats/parsing/ninja/texture";
import { parse_afs } from "../../core/data_formats/parsing/afs";

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
        return this.load_body_part_geometry(model.name, "Body").then(body => {
            if (!body) {
                throw new Error(`Couldn't load body for player class ${model.name}.`);
            }

            return this.load_body_part_geometry(model.name, "Head", 0).then(head => {
                if (!head) {
                    return body;
                }

                // Shift by 1 for the section ID and once for every body texture ID.
                let shift = 1 + model.body_tex_ids.length;
                this.shift_texture_ids(head, shift);
                this.add_to_bone(body, head, 59);

                if (model.hair_style_count === 0) {
                    return body;
                }

                return this.load_body_part_geometry(model.name, "Hair", 0).then(hair => {
                    if (!hair) {
                        return body;
                    }

                    shift += model.head_tex_ids.length;
                    this.shift_texture_ids(hair, shift);
                    this.add_to_bone(head, hair, 0);

                    if (!model.hair_styles_with_accessory.has(0)) {
                        return body;
                    }

                    return this.load_body_part_geometry(model.name, "Accessory", 0).then(
                        accessory => {
                            if (accessory) {
                                shift += model.hair_tex_ids.length;
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
            .then(buffer => parse_nj(new ArrayBufferCursor(buffer, Endianness.Little))[0]);
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

    load_textures(model: CharacterClassModel): Promise<readonly XvrTexture[]> {
        let xvr_texture = this.xvr_texture_cache.get(model.name);

        if (!xvr_texture) {
            xvr_texture = this.http_client
                .get(`/player/${model.name}Tex.afs`)
                .array_buffer()
                .then(buffer => {
                    const afs = parse_afs(new ArrayBufferCursor(buffer, Endianness.Little));
                    const textures: XvrTexture[] = [];

                    for (const file of afs) {
                        const xvm = parse_xvm(new ArrayBufferCursor(file, Endianness.Little));
                        textures.push(...xvm.textures);
                    }

                    return textures;
                });
        }

        return xvr_texture;
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
