import {
    AdditiveBlending,
    BufferGeometry,
    DoubleSide,
    Material,
    Mesh,
    MeshBasicMaterial,
    MeshLambertMaterial,
    Skeleton,
    SkinnedMesh,
    Texture,
} from "three";
import { BuilderData } from "./GeometryBuilder";
import { MeshBasicMaterialParameters } from "three/src/materials/MeshBasicMaterial";

const DUMMY_MATERIAL = new MeshLambertMaterial({
    color: 0x00ff00,
    side: DoubleSide,
});

export function create_mesh(
    geometry: BufferGeometry,
    textures: (Texture | undefined)[],
    default_material: Material,
    skinning: boolean,
): Mesh {
    const { created_by_geometry_builder, materials, bones } = geometry.userData as BuilderData;

    let mat: Material | Material[];

    if (textures.length && created_by_geometry_builder) {
        mat = [DUMMY_MATERIAL];

        for (let i = 1; i < materials.length; i++) {
            const { texture_id, alpha, additive_blending } = materials[i];
            const tex = texture_id == undefined ? undefined : textures[texture_id];

            if (tex) {
                const mat_params: MeshBasicMaterialParameters = {
                    skinning,
                    map: tex,
                    side: DoubleSide,
                };

                if (alpha) {
                    mat_params.transparent = true;
                    mat_params.alphaTest = 0.01;
                }

                if (additive_blending) {
                    mat_params.transparent = true;
                    mat_params.alphaTest = 0.01;
                    mat_params.blending = AdditiveBlending;
                }

                mat.push(new MeshBasicMaterial(mat_params));
            } else {
                mat.push(
                    new MeshLambertMaterial({
                        skinning,
                        side: DoubleSide,
                    }),
                );
            }
        }
    } else {
        mat = default_material;
    }

    if (created_by_geometry_builder && bones.length && skinning) {
        const mesh = new SkinnedMesh(geometry, mat);
        mesh.add(bones[0]);
        mesh.bind(new Skeleton(bones));
        return mesh;
    } else {
        return new Mesh(geometry, mat);
    }
}
