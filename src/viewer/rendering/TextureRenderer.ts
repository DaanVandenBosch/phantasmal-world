import { Disposer } from "../../core/observable/Disposer";
import { LogManager } from "../../core/Logger";
import { TextureController } from "../controllers/texture/TextureController";
import { XvrTexture } from "../../core/data_formats/parsing/ninja/texture";
import { VertexFormatType } from "../../core/rendering/VertexFormat";
import { Mesh } from "../../core/rendering/Mesh";
import { GfxRenderer } from "../../core/rendering/GfxRenderer";
import { Renderer } from "../../core/rendering/Renderer";
import { xvr_texture_to_texture } from "../../core/rendering/conversion/ninja_textures";
import { Mat4, Vec2, Vec3 } from "../../core/math/linear_algebra";
import { SceneNode } from "../../core/rendering/Scene";

const logger = LogManager.get("viewer/rendering/TextureRenderer");

export class TextureRenderer implements Renderer {
    private readonly disposer = new Disposer();

    readonly canvas_element: HTMLCanvasElement;

    constructor(ctrl: TextureController, private readonly renderer: GfxRenderer) {
        this.canvas_element = renderer.canvas_element;

        renderer.camera.pan(0, 0, 10);

        this.disposer.add_all(
            ctrl.textures.observe(({ value: textures }) => {
                renderer.destroy_scene();
                renderer.camera.reset();
                this.create_quads(textures);
                renderer.schedule_render();
            }),
        );
    }

    dispose(): void {
        this.renderer.dispose();
        this.disposer.dispose();
    }

    start_rendering(): void {
        this.renderer.start_rendering();
    }

    stop_rendering(): void {
        this.renderer.stop_rendering();
    }

    set_size(width: number, height: number): void {
        this.renderer.set_size(width, height);
    }

    private create_quads(textures: readonly XvrTexture[]): void {
        let total_width = 10 * (textures.length - 1); // 10px spacing between textures.
        let total_height = 0;

        for (const tex of textures) {
            total_width += tex.width;
            total_height = Math.max(total_height, tex.height);
        }

        let x = -Math.floor(total_width / 2);
        const y = -Math.floor(total_height / 2);

        for (const tex of textures) {
            try {
                const quad_mesh = this.create_quad(tex);
                quad_mesh.upload(this.renderer.gfx);

                this.renderer.scene.root_node.add_child(
                    new SceneNode(
                        quad_mesh,
                        Mat4.translation(x, y + (total_height - tex.height) / 2, 0),
                    ),
                );
            } catch (e) {
                logger.error("Couldn't create quad for texture.", e);
            }

            x += 10 + tex.width;
        }
    }

    private create_quad(tex: XvrTexture): Mesh {
        return Mesh.builder(VertexFormatType.PosTex)

            .vertex(new Vec3(0, 0, 0), new Vec2(0, 1))
            .vertex(new Vec3(tex.width, 0, 0), new Vec2(1, 1))
            .vertex(new Vec3(tex.width, tex.height, 0), new Vec2(1, 0))
            .vertex(new Vec3(0, tex.height, 0), new Vec2(0, 0))

            .triangle(0, 1, 2)
            .triangle(2, 3, 0)

            .texture(xvr_texture_to_texture(this.renderer.gfx, tex))

            .build();
    }
}
