import { QuestEntityModel } from "../../model/QuestEntityModel";
import { QuestObjectModel } from "../../model/QuestObjectModel";
import { BufferGeometry, DoubleSide, Mesh, MeshLambertMaterial, Texture } from "three";
import { ObjectType } from "../../../core/data_formats/parsing/quest/object_types";
import { QuestNpcModel } from "../../model/QuestNpcModel";
import { NpcType } from "../../../core/data_formats/parsing/quest/npc_types";
import { create_mesh } from "../../../core/rendering/conversion/create_mesh";

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

export function create_object_mesh(
    object: QuestObjectModel,
    geometry: BufferGeometry,
    textures: Texture[],
): Mesh {
    return create(
        object,
        geometry,
        textures,
        OBJECT_COLORS[ColorType.Normal],
        ObjectType[object.type],
    );
}

export function create_npc_mesh(
    npc: QuestNpcModel,
    geometry: BufferGeometry,
    textures: Texture[],
): Mesh {
    return create(npc, geometry, textures, NPC_COLORS[ColorType.Normal], NpcType[npc.type]);
}

function create(
    entity: QuestEntityModel,
    geometry: BufferGeometry,
    textures: Texture[],
    color: number,
    name: string,
): Mesh {
    const default_material = new MeshLambertMaterial({
        color,
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

    mesh.name = name;
    (mesh.userData as EntityUserData).entity = entity;

    const { x, y, z } = entity.world_position.val;
    mesh.position.set(x, y, z);
    const rot = entity.rotation.val;
    mesh.rotation.set(rot.x, rot.y, rot.z);

    return mesh;
}
