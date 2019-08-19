import {
    DoubleSide,
    Mesh,
    MeshLambertMaterial,
    Object3D,
    PerspectiveCamera,
    SkeletonHelper,
    Texture,
    Vector3,
} from "three";
import { Renderer } from "../../../core/rendering/Renderer";
import { model_store } from "../stores/ModelStore";
import { Disposable } from "../../core/gui/Disposable";
import { create_mesh, create_skinned_mesh } from "../../../core/rendering/conversion/create_mesh";
import { ninja_object_to_buffer_geometry } from "../../../core/rendering/conversion/ninja_geometry";
import { NjObject } from "../../../core/data_formats/parsing/ninja";

export class ModelRenderer extends Renderer implements Disposable {
    private nj_object?: NjObject;
    private object_3d?: Object3D;
    private skeleton_helper?: SkeletonHelper;
    private perspective_camera: PerspectiveCamera;
    private disposables: Disposable[] = [];

    constructor() {
        super(new PerspectiveCamera(75, 1, 1, 200));

        this.perspective_camera = this.camera as PerspectiveCamera;

        this.disposables.push(model_store.current_nj_data.observe(this.update));
    }

    set_size(width: number, height: number): void {
        this.perspective_camera.aspect = width / height;
        this.perspective_camera.updateProjectionMatrix();
        super.set_size(width, height);
    }

    dispose(): void {
        super.dispose();
        this.disposables.forEach(d => d.dispose());
    }

    protected render(): void {
        // if (model_viewer_store.animation) {
        //     model_viewer_store.animation.mixer.update(model_viewer_store.clock.getDelta());
        //     model_viewer_store.update_animation_frame();
        // }

        this.light_holder.quaternion.copy(this.perspective_camera.quaternion);
        super.render();

        // if (model_viewer_store.animation && !model_viewer_store.animation.action.paused) {
        //     this.schedule_render();
        // }
    }

    private update = () => {
        // TODO:
        const textures: Texture[] | undefined = Math.random() > 1 ? [] : undefined;
        const nj_data = model_store.current_nj_data.get();

        if (nj_data) {
            const { nj_object, has_skeleton } = nj_data;

            if (nj_object !== this.nj_object) {
                this.nj_object = nj_object;

                if (nj_object) {
                    let mesh: Mesh;

                    const materials =
                        textures &&
                        textures.map(
                            tex =>
                                new MeshLambertMaterial({
                                    skinning: has_skeleton,
                                    map: tex,
                                    side: DoubleSide,
                                    alphaTest: 0.5,
                                }),
                        );

                    if (has_skeleton) {
                        mesh = create_skinned_mesh(
                            ninja_object_to_buffer_geometry(nj_object),
                            materials,
                        );
                    } else {
                        mesh = create_mesh(ninja_object_to_buffer_geometry(nj_object), materials);
                    }

                    // Make sure we rotate around the center of the model instead of its origin.
                    const bb = mesh.geometry.boundingBox;
                    const height = bb.max.y - bb.min.y;
                    mesh.translateY(-height / 2 - bb.min.y);

                    this.set_object_3d(mesh);
                } else {
                    this.set_object_3d(undefined);
                }
            }

            if (this.skeleton_helper) {
                this.skeleton_helper.visible = model_store.show_skeleton.get();
            }

            // if (model_viewer_store.animation) {
            //     this.schedule_render();
            // }
            //
            // if (!model_viewer_store.animation_playing) {
            //     // Reference animation_frame here to make sure we render when the user sets the frame manually.
            //     model_viewer_store.animation_frame;
            //     this.schedule_render();
            // }
        } else {
            this.set_object_3d(undefined);
        }

        this.schedule_render();
    };

    private set_object_3d(object_3d?: Object3D): void {
        if (this.object_3d) {
            this.scene.remove(this.object_3d);
            this.scene.remove(this.skeleton_helper!);
            this.skeleton_helper = undefined;
        }

        if (object_3d) {
            this.scene.add(object_3d);
            this.skeleton_helper = new SkeletonHelper(object_3d);
            this.skeleton_helper.visible = model_store.show_skeleton.get();
            (this.skeleton_helper.material as any).linewidth = 3;
            this.scene.add(this.skeleton_helper);
            this.reset_camera(new Vector3(0, 10, 20), new Vector3(0, 0, 0));
        }

        this.object_3d = object_3d;
        this.schedule_render();
    }
}
