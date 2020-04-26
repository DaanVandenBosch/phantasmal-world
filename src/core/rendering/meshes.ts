import { Mesh } from "./Mesh";
import { VertexFormatType } from "./VertexFormat";
import { Vec3 } from "../math/linear_algebra";

export function cube_mesh(): Mesh {
    return (
        Mesh.builder(VertexFormatType.PosNorm)
            // Front
            .vertex(new Vec3(1, 1, -1), new Vec3(0, 0, -1))
            .vertex(new Vec3(-1, 1, -1), new Vec3(0, 0, -1))
            .vertex(new Vec3(-1, -1, -1), new Vec3(0, 0, -1))
            .vertex(new Vec3(1, -1, -1), new Vec3(0, 0, -1))
            .triangle(0, 1, 2)
            .triangle(0, 2, 3)

            // Back
            .vertex(new Vec3(1, 1, 1), new Vec3(0, 0, 1))
            .vertex(new Vec3(1, -1, 1), new Vec3(0, 0, 1))
            .vertex(new Vec3(-1, -1, 1), new Vec3(0, 0, 1))
            .vertex(new Vec3(-1, 1, 1), new Vec3(0, 0, 1))
            .triangle(4, 5, 6)
            .triangle(4, 6, 7)

            // Top
            .vertex(new Vec3(1, 1, 1), new Vec3(0, 1, 0))
            .vertex(new Vec3(-1, 1, 1), new Vec3(0, 1, 0))
            .vertex(new Vec3(-1, 1, -1), new Vec3(0, 1, 0))
            .vertex(new Vec3(1, 1, -1), new Vec3(0, 1, 0))
            .triangle(8, 9, 10)
            .triangle(8, 10, 11)

            // Bottom
            .vertex(new Vec3(1, -1, 1), new Vec3(0, -1, 0))
            .vertex(new Vec3(1, -1, -1), new Vec3(0, -1, 0))
            .vertex(new Vec3(-1, -1, -1), new Vec3(0, -1, 0))
            .vertex(new Vec3(-1, -1, 1), new Vec3(0, -1, 0))
            .triangle(12, 13, 14)
            .triangle(12, 14, 15)

            // Right
            .vertex(new Vec3(1, 1, 1), new Vec3(1, 0, 0))
            .vertex(new Vec3(1, 1, -1), new Vec3(1, 0, 0))
            .vertex(new Vec3(1, -1, -1), new Vec3(1, 0, 0))
            .vertex(new Vec3(1, -1, 1), new Vec3(1, 0, 0))
            .triangle(16, 17, 18)
            .triangle(16, 18, 19)

            // Left
            .vertex(new Vec3(-1, 1, 1), new Vec3(-1, 0, 0))
            .vertex(new Vec3(-1, -1, 1), new Vec3(-1, 0, 0))
            .vertex(new Vec3(-1, -1, -1), new Vec3(-1, 0, 0))
            .vertex(new Vec3(-1, 1, -1), new Vec3(-1, 0, 0))
            .triangle(20, 21, 22)
            .triangle(20, 22, 23)

            .build()
    );
}
