import { id_to_object_type, object_data, ObjectType } from "./object_types";
import { Vec3 } from "../../vector";
import { OBJECT_BYTE_SIZE } from "./dat";
import { assert } from "../../../util";

export type QuestObject = {
    area_id: number;
    readonly data: ArrayBuffer;
    readonly view: DataView;
};

export function create_quest_object(type: ObjectType, area_id: number): QuestObject {
    const data = new ArrayBuffer(OBJECT_BYTE_SIZE);
    const obj: QuestObject = {
        area_id,
        data,
        view: new DataView(data),
    };

    set_object_type(obj, type);

    return obj;
}

export function data_to_quest_object(area_id: number, data: ArrayBuffer): QuestObject {
    assert(
        data.byteLength === OBJECT_BYTE_SIZE,
        () => `Data byteLength should be ${OBJECT_BYTE_SIZE} but was ${data.byteLength}.`,
    );

    return {
        area_id,
        data,
        view: new DataView(data),
    };
}

//
// Simple properties that directly map to a part of the data block.
//

export function get_object_type_id(object: QuestObject): number {
    return object.view.getUint16(0, true);
}

export function set_object_type_id(object: QuestObject, type_id: number): void {
    object.view.setUint16(0, type_id, true);
}

export function get_object_id(object: QuestObject): number {
    return object.view.getUint16(8, true);
}

export function get_object_group_id(object: QuestObject): number {
    return object.view.getUint16(10, true);
}

export function get_object_section_id(object: QuestObject): number {
    return object.view.getUint16(12, true);
}

export function set_object_section_id(object: QuestObject, section_id: number): void {
    object.view.setUint16(12, section_id, true);
}

/**
 * Section-relative position.
 */
export function get_object_position(object: QuestObject): Vec3 {
    return {
        x: object.view.getFloat32(16, true),
        y: object.view.getFloat32(20, true),
        z: object.view.getFloat32(24, true),
    };
}

export function set_object_position(object: QuestObject, position: Vec3): void {
    object.view.setFloat32(16, position.x, true);
    object.view.setFloat32(20, position.y, true);
    object.view.setFloat32(24, position.z, true);
}

export function get_object_rotation(object: QuestObject): Vec3 {
    return {
        x: (object.view.getInt32(28, true) / 0xffff) * 2 * Math.PI,
        y: (object.view.getInt32(32, true) / 0xffff) * 2 * Math.PI,
        z: (object.view.getInt32(36, true) / 0xffff) * 2 * Math.PI,
    };
}

export function set_object_rotation(object: QuestObject, rotation: Vec3): void {
    object.view.setInt32(28, Math.round((rotation.x / (2 * Math.PI)) * 0xffff), true);
    object.view.setInt32(32, Math.round((rotation.y / (2 * Math.PI)) * 0xffff), true);
    object.view.setInt32(36, Math.round((rotation.z / (2 * Math.PI)) * 0xffff), true);
}

//
// Complex properties that use multiple parts of the data block and possible other properties.
//

export function get_object_type(object: QuestObject): ObjectType {
    return id_to_object_type(get_object_type_id(object));
}

export function set_object_type(object: QuestObject, type: ObjectType): void {
    set_object_type_id(object, object_data(type).type_id ?? 0);
}

export function get_object_script_label(object: QuestObject): number | undefined {
    switch (get_object_type(object)) {
        case ObjectType.ScriptCollision:
        case ObjectType.ForestConsole:
        case ObjectType.TalkLinkToSupport:
            return object.view.getUint32(52, true);
        case ObjectType.RicoMessagePod:
            return object.view.getUint32(56, true);
        default:
            return undefined;
    }
}

export function get_object_script_label_2(object: QuestObject): number | undefined {
    switch (get_object_type(object)) {
        case ObjectType.RicoMessagePod:
            return object.view.getUint32(60, true);
        default:
            return undefined;
    }
}
