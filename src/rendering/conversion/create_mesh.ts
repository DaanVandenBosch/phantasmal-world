import {
    BufferGeometry,
    DoubleSide,
    Material,
    Mesh,
    MeshLambertMaterial,
    Skeleton,
    SkinnedMesh,
} from "three";
import { BuilderData } from "./GeometryBuilder";

const DUMMY_MATERIAL = new MeshLambertMaterial({
    color: 0x00ff00,
    side: DoubleSide,
});
const DEFAULT_MATERIAL = new MeshLambertMaterial({
    color: 0xff00ff,
    side: DoubleSide,
});
const DEFAULT_SKINNED_MATERIAL = new MeshLambertMaterial({
    skinning: true,
    color: 0xff00ff,
    side: DoubleSide,
});

export function create_mesh(
    geometry: BufferGeometry,
    material?: Material | Material[],
    default_material: Material = DEFAULT_MATERIAL
): Mesh {
    return create(geometry, material, default_material, Mesh);
}

export function create_skinned_mesh(
    geometry: BufferGeometry,
    material?: Material | Material[],
    default_material: Material = DEFAULT_SKINNED_MATERIAL
): SkinnedMesh {
    return create(geometry, material, default_material, SkinnedMesh);
}

function create<M extends Mesh>(
    geometry: BufferGeometry,
    material: Material | Material[] | undefined,
    default_material: Material,
    mesh_constructor: new (geometry: BufferGeometry, material: Material | Material[]) => M
): M {
    const {
        created_by_geometry_builder,
        normalized_material_indices: mat_idxs,
        bones,
    } = geometry.userData as BuilderData;

    let mat: Material | Material[];

    if (Array.isArray(material)) {
        if (created_by_geometry_builder) {
            mat = [DUMMY_MATERIAL];

            for (const [idx, normalized_idx] of mat_idxs.entries()) {
                if (normalized_idx > 0) {
                    mat[normalized_idx] = material[idx] || default_material;
                }
            }
        } else {
            mat = material;
        }
    } else if (material) {
        mat = material;
    } else {
        mat = default_material;
    }

    const mesh = new mesh_constructor(geometry, mat);

    if (created_by_geometry_builder && bones.length && mesh instanceof SkinnedMesh) {
        mesh.add(bones[0]);
        mesh.bind(new Skeleton(bones));
    }

    return mesh;
}
