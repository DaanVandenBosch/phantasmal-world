import { Disposer } from "../../core/observable/Disposer";
import { LogManager } from "../../core/Logger";
import { TextureController } from "../controllers/TextureController";
import { XvrTexture } from "../../core/data_formats/parsing/ninja/texture";
import { TranslateTransform } from "../../core/rendering/Transform";
import { VertexFormat } from "../../core/rendering/VertexFormat";
import { Texture, TextureFormat } from "../../core/rendering/Texture";
import { Mesh } from "../../core/rendering/Mesh";
import { GfxRenderer } from "../../core/rendering/GfxRenderer";
import { Renderer } from "../../core/rendering/Renderer";

const logger = LogManager.get("viewer/rendering/TextureRenderer");

export class TextureRenderer implements Renderer {
    private readonly disposer = new Disposer();

    readonly canvas_element: HTMLCanvasElement;

    constructor(ctrl: TextureController, private readonly renderer: GfxRenderer) {
        this.canvas_element = renderer.canvas_element;

        this.disposer.add_all(
            ctrl.textures.observe(({ value: textures }) => {
                renderer.scene.destroy();
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

                this.renderer.scene.root_node.add_child(
                    quad_mesh,
                    new TranslateTransform(x, y + (total_height - tex.height) / 2, 0),
                );
            } catch (e) {
                logger.error("Couldn't create quad for texture.", e);
            }

            x += 10 + tex.width;
        }
    }

    private create_quad(tex: XvrTexture): Mesh {
        return this.renderer
            .mesh_builder(VertexFormat.PosTex)
            .vertex(0, 0, 0, 0, 1)
            .vertex(tex.width, 0, 0, 1, 1)
            .vertex(tex.width, tex.height, 0, 1, 0)
            .vertex(0, tex.height, 0, 0, 0)

            .triangle(0, 1, 2)
            .triangle(2, 3, 0)

            .texture(this.xvr_texture_to_texture(tex))

            .build();
    }

    private xvr_texture_to_texture(tex: XvrTexture): Texture {
        let format: TextureFormat;
        let data_size: number;

        // Ignore mipmaps.
        switch (tex.format[1]) {
            case 6:
                format = TextureFormat.RGBA_S3TC_DXT1;
                data_size = (tex.width * tex.height) / 2;
                break;
            case 7:
                format = TextureFormat.RGBA_S3TC_DXT3;
                data_size = tex.width * tex.height;
                break;
            default:
                throw new Error(`Format ${tex.format.join(", ")} not supported.`);
        }

        return new Texture(
            this.renderer.gfx!,
            format,
            tex.width,
            tex.height,
            tex.data.slice(0, data_size),
        );
    }
}
