import { Cursor } from "../../cursor/Cursor";
import { Vec3 } from "../../vector";
import { parse_iff } from "../iff";
import { NjcmModel, parse_njcm_model } from "./njcm";
import { parse_xj_model, XjModel } from "./xj";
import { Result, success } from "../../../Result";

export const ANGLE_TO_RAD = (2 * Math.PI) / 0xffff;

const NJCM = 0x4d434a4e;

export type NjModel = NjcmModel | XjModel;

export function is_njcm_model(model: NjModel): model is NjcmModel {
    return model.type === "njcm";
}

export function is_xj_model(model: NjModel): model is XjModel {
    return model.type === "xj";
}

export class NjObject<M extends NjModel = NjModel> {
    private readonly _children: NjObject<M>[];

    readonly evaluation_flags: NjEvaluationFlags;
    readonly model?: M;
    readonly position: Vec3;
    readonly rotation: Vec3; // Euler angles in radians.
    readonly scale: Vec3;
    readonly children: readonly NjObject<M>[];

    constructor(
        evaluation_flags: NjEvaluationFlags,
        model: M | undefined,
        position: Vec3,
        rotation: Vec3, // Euler angles in radians.
        scale: Vec3,
        children: NjObject<M>[],
    ) {
        this.evaluation_flags = evaluation_flags;
        this.model = model;
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
        this._children = children;
        this.children = this._children;
    }

    bone_count(): number {
        const id_ref: [number] = [0];
        this.get_bone_internal(this, Number.MAX_SAFE_INTEGER, id_ref);
        return id_ref[0];
    }

    get_bone(bone_id: number): NjObject<M> | undefined {
        return this.get_bone_internal(this, bone_id, [0]);
    }

    add_child(child: NjObject<M>): void {
        this._children.push(child);
    }

    clear_children(): void {
        this._children.splice(0);
    }

    private get_bone_internal(
        object: NjObject<M>,
        bone_id: number,
        id_ref: [number],
    ): NjObject<M> | undefined {
        if (!object.evaluation_flags.skip) {
            const id = id_ref[0]++;

            if (id === bone_id) {
                return object;
            }
        }

        if (!object.evaluation_flags.break_child_trace) {
            for (const child of object.children) {
                const bone = this.get_bone_internal(child, bone_id, id_ref);
                if (bone) return bone;
            }
        }
    }
}

export type NjEvaluationFlags = {
    no_translate: boolean;
    no_rotate: boolean;
    no_scale: boolean;
    hidden: boolean;
    break_child_trace: boolean;
    zxy_rotation_order: boolean;
    skip: boolean;
    shape_skip: boolean;
};

/**
 * Parses an NJCM file.
 */
export function parse_nj(cursor: Cursor): Result<NjObject<NjcmModel>[]> {
    return parse_ninja(cursor, parse_njcm_model, []);
}

/**
 * Parses an NJCM file.
 */
export function parse_xj(cursor: Cursor): Result<NjObject<XjModel>[]> {
    return parse_ninja(cursor, parse_xj_model, undefined);
}

/**
 * Parses a ninja object.
 */
export function parse_xj_object(cursor: Cursor): NjObject<XjModel>[] {
    return parse_sibling_objects(cursor, parse_xj_model, undefined);
}

function parse_ninja<M extends NjModel>(
    cursor: Cursor,
    parse_model: (cursor: Cursor, context: any) => M,
    context: any,
): Result<NjObject<M>[]> {
    const parse_iff_result = parse_iff(cursor);

    if (!parse_iff_result.success) {
        return parse_iff_result;
    }

    // POF0 and other chunks types are ignored.
    const njcm_chunks = parse_iff_result.value.filter(chunk => chunk.type === NJCM);
    const objects: NjObject<M>[] = [];

    for (const chunk of njcm_chunks) {
        objects.push(...parse_sibling_objects(chunk.data, parse_model, context));
    }

    return success(objects, parse_iff_result.problems);
}

// TODO: cache model and object offsets so we don't reparse the same data.
function parse_sibling_objects<M extends NjModel>(
    cursor: Cursor,
    parse_model: (cursor: Cursor, context: any) => M,
    context: any,
): NjObject<M>[] {
    const eval_flags = cursor.u32();
    const no_translate = (eval_flags & 0b1) !== 0;
    const no_rotate = (eval_flags & 0b10) !== 0;
    const no_scale = (eval_flags & 0b100) !== 0;
    const hidden = (eval_flags & 0b1000) !== 0;
    const break_child_trace = (eval_flags & 0b10000) !== 0;
    const zxy_rotation_order = (eval_flags & 0b100000) !== 0;
    const skip = (eval_flags & 0b1000000) !== 0;
    const shape_skip = (eval_flags & 0b10000000) !== 0;

    const model_offset = cursor.u32();
    const pos = cursor.vec3_f32();
    const rotation = {
        x: cursor.i32() * ANGLE_TO_RAD,
        y: cursor.i32() * ANGLE_TO_RAD,
        z: cursor.i32() * ANGLE_TO_RAD,
    };
    const scale = cursor.vec3_f32();
    const child_offset = cursor.u32();
    const sibling_offset = cursor.u32();

    let model: M | undefined;
    let children: NjObject<M>[];
    let siblings: NjObject<M>[];

    if (model_offset) {
        cursor.seek_start(model_offset);
        model = parse_model(cursor, context);
    }

    if (child_offset) {
        cursor.seek_start(child_offset);
        children = parse_sibling_objects(cursor, parse_model, context);
    } else {
        children = [];
    }

    if (sibling_offset) {
        cursor.seek_start(sibling_offset);
        siblings = parse_sibling_objects(cursor, parse_model, context);
    } else {
        siblings = [];
    }

    const object = new NjObject<M>(
        {
            no_translate,
            no_rotate,
            no_scale,
            hidden,
            break_child_trace,
            zxy_rotation_order,
            skip,
            shape_skip,
        },
        model,
        pos,
        rotation,
        scale,
        children,
    );

    return [object, ...siblings];
}
