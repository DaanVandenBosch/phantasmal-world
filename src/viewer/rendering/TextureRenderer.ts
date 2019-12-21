import {
    Mesh,
    MeshBasicMaterial,
    OrthographicCamera,
    PlaneGeometry,
    Texture,
    Vector2,
    Vector3,
} from "three";
import { Disposable } from "../../core/observable/Disposable";
import { Renderer } from "../../core/rendering/Renderer";
import { Disposer } from "../../core/observable/Disposer";
import { Xvm } from "../../core/data_formats/parsing/ninja/texture";
import { xvm_texture_to_texture } from "../../core/rendering/conversion/ninja_textures";
import { TextureStore } from "../stores/TextureStore";
import Logger = require("js-logger");

const logger = Logger.get("viewer/rendering/TextureRenderer");

export class TextureRenderer extends Renderer implements Disposable {
    private readonly disposer = new Disposer();
    private readonly quad_meshes: Mesh[] = [];

    readonly camera = new OrthographicCamera(-400, 400, 300, -300, 1, 10);

    constructor(texture_store: TextureStore) {
        super();

        this.disposer.add_all(
            texture_store.current_xvm.observe(({ value: xvm }) => {
                this.scene.remove(...this.quad_meshes);

                if (xvm) {
                    this.render_textures(xvm);
                }

                this.reset_camera(new Vector3(0, 0, 5), new Vector3());
                this.schedule_render();
            }),
        );

        this.init_camera_controls();
        this.controls.dollySpeed = -1;
        this.controls.azimuthRotateSpeed = 0;
        this.controls.polarRotateSpeed = 0;
    }

    set_size(width: number, height: number): void {
        this.camera.left = -Math.floor(width / 2);
        this.camera.right = Math.ceil(width / 2);
        this.camera.top = Math.floor(height / 2);
        this.camera.bottom = -Math.ceil(height / 2);
        this.camera.updateProjectionMatrix();
        super.set_size(width, height);
    }

    dispose(): void {
        super.dispose();
        this.disposer.dispose();
    }

    private render_textures(xvm: Xvm): void {
        let total_width = 10 * (xvm.textures.length - 1); // 10px spacing between textures.
        let total_height = 0;

        for (const tex of xvm.textures) {
            total_width += tex.width;
            total_height = Math.max(total_height, tex.height);
        }

        let x = -Math.floor(total_width / 2);
        const y = -Math.floor(total_height / 2);

        for (const tex of xvm.textures) {
            let tex_3js: Texture | undefined;

            try {
                tex_3js = xvm_texture_to_texture(tex);
            } catch (e) {
                logger.warn("Couldn't convert XVM texture.", e);
            }

            const quad_mesh = new Mesh(
                this.create_quad(
                    x,
                    y + Math.floor((total_height - tex.height) / 2),
                    tex.width,
                    tex.height,
                ),
                tex_3js
                    ? new MeshBasicMaterial({
                          map: tex_3js,
                          transparent: true,
                      })
                    : new MeshBasicMaterial({
                          color: 0xff00ff,
                      }),
            );

            this.quad_meshes.push(quad_mesh);
            this.scene.add(quad_mesh);

            x += 10 + tex.width;
        }
    }

    private create_quad(x: number, y: number, width: number, height: number): PlaneGeometry {
        const quad = new PlaneGeometry(width, height, 1, 1);
        quad.faceVertexUvs = [
            [
                [new Vector2(0, 0), new Vector2(0, 1), new Vector2(1, 0)],
                [new Vector2(0, 1), new Vector2(1, 1), new Vector2(1, 0)],
            ],
        ];
        quad.translate(x + width / 2, y + height / 2, -5);
        return quad;
    }
}
