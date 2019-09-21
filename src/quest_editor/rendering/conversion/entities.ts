import { QuestEntityModel } from "../../model/QuestEntityModel";
import { BufferGeometry, DoubleSide, Mesh, MeshLambertMaterial, Texture } from "three";
import { create_mesh } from "../../../core/rendering/conversion/create_mesh";
import {
    entity_type_to_string,
    EntityType,
    is_npc_type,
} from "../../../core/data_formats/parsing/quest/entities";

export enum ColorType {
    Normal,
    Hovered,
    Selected,
}

export const OBJECT_COLORS: number[] = [];
OBJECT_COLORS[ColorType.Normal] = 0xffff00;
OBJECT_COLORS[ColorType.Hovered] = 0xffdf3f;
OBJECT_COLORS[ColorType.Selected] = 0xffaa00;

export const NPC_COLORS: number[] = [];
NPC_COLORS[ColorType.Normal] = 0xff0000;
NPC_COLORS[ColorType.Hovered] = 0xff3f5f;
NPC_COLORS[ColorType.Selected] = 0xff0054;

export type EntityUserData = {
    entity: QuestEntityModel;
};

export function create_entity_type_mesh(
    type: EntityType,
    geometry: BufferGeometry,
    textures: Texture[],
): Mesh {
    const default_material = new MeshLambertMaterial({
        color: is_npc_type(type) ? NPC_COLORS[ColorType.Normal] : OBJECT_COLORS[ColorType.Normal],
        side: DoubleSide,
    });

    const mesh = create_mesh(
        geometry,
        textures.length
            ? textures.map(
                  tex =>
                      new MeshLambertMaterial({
                          map: tex,
                          side: DoubleSide,
                          alphaTest: 0.5,
                      }),
              )
            : default_material,
        default_material,
    );

    mesh.name = entity_type_to_string(type);

    return mesh;
}

export function create_entity_mesh(
    entity: QuestEntityModel,
    geometry: BufferGeometry,
    textures: Texture[],
): Mesh {
    const mesh = create_entity_type_mesh(entity.type, geometry, textures);

    (mesh.userData as EntityUserData).entity = entity;

    const { x, y, z } = entity.world_position.val;
    mesh.position.set(x, y, z);
    const rot = entity.rotation.val;
    mesh.rotation.set(rot.x, rot.y, rot.z);

    return mesh;
}
