import { ModelStore } from "../stores/ModelStore";
import { Disposer } from "../../core/observable/Disposer";
import { Renderer } from "../../core/rendering/Renderer";
import { GfxRenderer } from "../../core/rendering/GfxRenderer";
import { ninja_object_to_mesh } from "../../core/rendering/conversion/ninja_geometry";
import { SceneNode } from "../../core/rendering/Scene";
import { Mat4 } from "../../core/math/linear_algebra";

export class ModelGfxRenderer implements Renderer {
    private readonly disposer = new Disposer();

    readonly canvas_element: HTMLCanvasElement;

    constructor(private readonly store: ModelStore, private readonly renderer: GfxRenderer) {
        this.canvas_element = renderer.canvas_element;

        renderer.camera.pan(0, 0, 50);

        this.disposer.add_all(store.current_nj_object.observe(this.nj_object_or_xvm_changed));

        // TODO: remove
        // const cube = cube_mesh();
        // cube.upload(this.renderer.gfx);
        //
        // this.renderer.scene.root_node.add_child(
        //     new SceneNode(
        //         undefined,
        //         Mat4.identity(),
        //         new SceneNode(
        //             cube,
        //             Mat4.compose(
        //                 new Vec3(-3, 0, 0),
        //                 quat_product(
        //                     Quat.euler_angles(Math.PI / 6, 0, 0, EulerOrder.ZYX),
        //                     Quat.euler_angles(0, -Math.PI / 6, 0, EulerOrder.ZYX),
        //                 ),
        //                 new Vec3(1, 1, 1),
        //             ),
        //         ),
        //         new SceneNode(
        //             cube,
        //             Mat4.compose(
        //                 new Vec3(3, 0, 0),
        //                 quat_product(
        //                     Quat.euler_angles(-Math.PI / 6, 0, 0, EulerOrder.ZYX),
        //                     Quat.euler_angles(0, Math.PI / 6, 0, EulerOrder.ZYX),
        //                 ),
        //                 new Vec3(1, 1, 1),
        //             ),
        //         ),
        //     ),
        // );
    }

    dispose(): void {
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

    private nj_object_or_xvm_changed = (): void => {
        this.renderer.destroy_scene();

        const nj_object = this.store.current_nj_object.val;

        if (nj_object) {
            // Convert textures and geometry.
            const node = new SceneNode(ninja_object_to_mesh(nj_object), Mat4.identity());
            this.renderer.scene.root_node.add_child(node);

            this.renderer.scene.traverse(node => {
                node.mesh?.upload(this.renderer.gfx);
            }, undefined);
        }

        this.renderer.schedule_render();
    };
}
