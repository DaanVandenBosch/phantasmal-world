import { BufferGeometry, DoubleSide, Mesh, MeshLambertMaterial, Texture } from "three";
import { QuestEntity, QuestNpc, QuestObject } from "../../domain";
import { create_mesh } from "./create_mesh";

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
    entity: QuestEntity;
};

export function create_object_mesh(
    object: QuestObject,
    geometry: BufferGeometry,
    textures: Texture[]
): Mesh {
    return create(object, geometry, textures, OBJECT_COLORS[ColorType.Normal], object.type.name);
}

export function create_npc_mesh(
    npc: QuestNpc,
    geometry: BufferGeometry,
    textures: Texture[]
): Mesh {
    return create(npc, geometry, textures, NPC_COLORS[ColorType.Normal], npc.type.code);
}

function create(
    entity: QuestEntity,
    geometry: BufferGeometry,
    textures: Texture[],
    color: number,
    name: string
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
                      })
              )
            : default_material,
        default_material
    );

    mesh.name = name;
    (mesh.userData as EntityUserData).entity = entity;

    const { x, y, z } = entity.position;
    mesh.position.set(x, y, z);
    const rot = entity.rotation;
    mesh.rotation.set(rot.x, rot.y, rot.z);

    return mesh;
}
