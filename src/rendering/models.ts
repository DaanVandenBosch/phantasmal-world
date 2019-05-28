import { BufferGeometry, DoubleSide, Mesh, MeshLambertMaterial } from 'three';

export function createModelMesh(geometry?: BufferGeometry): Mesh | undefined {
    return geometry && new Mesh(
        geometry,
        new MeshLambertMaterial({
            color: 0xFF00FF,
            side: DoubleSide
        })
    );
}
