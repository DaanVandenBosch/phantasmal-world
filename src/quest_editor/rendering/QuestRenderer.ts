import { DisposableThreeRenderer, Renderer } from "../../core/rendering/Renderer";
import {
    Group,
    Mesh,
    MeshLambertMaterial,
    Object3D,
    PerspectiveCamera,
    Vector2,
    Vector3,
} from "three";
import { QuestEntityModel } from "../model/QuestEntityModel";
import { Quest3DModelManager } from "./Quest3DModelManager";
import { Disposer } from "../../core/observable/Disposer";
import { ColorType, EntityUserData, NPC_COLORS, OBJECT_COLORS } from "./conversion/entities";
import { QuestNpcModel } from "../model/QuestNpcModel";
import { pick_ground } from "./pick_ground";

const ZERO_VECTOR_2 = Object.freeze(new Vector2(0, 0));
const ZERO_VECTOR_3 = Object.freeze(new Vector3(0, 0, 0));

export class QuestRenderer extends Renderer {
    private _collision_geometry = new Object3D();
    private _render_geometry = new Object3D();
    private _entity_models = new Object3D();
    private readonly disposer = new Disposer();
    private readonly entity_to_mesh = new Map<QuestEntityModel, Mesh>();
    private hovered_mesh?: Mesh;
    private selected_mesh?: Mesh;
    private camera_target_timeout?: number;
    private old_camera_target = new Vector3();

    get debug(): boolean {
        return super.debug;
    }

    set debug(debug: boolean) {
        if (this.debug !== debug) {
            super.debug = debug;
            this._render_geometry.visible = debug;
            this.schedule_render();
        }
    }

    readonly camera = new PerspectiveCamera(60, 1, 10, 5_000);

    get collision_geometry(): Object3D {
        return this._collision_geometry;
    }

    set collision_geometry(collision_geometry: Object3D) {
        this.scene.remove(this.collision_geometry);
        this._collision_geometry = collision_geometry;
        this.scene.add(collision_geometry);
    }

    set render_geometry(render_geometry: Object3D) {
        this.scene.remove(this._render_geometry);
        this._render_geometry = render_geometry;
        render_geometry.visible = this.debug;
        this.scene.add(render_geometry);
    }

    get entity_models(): Object3D {
        return this._entity_models;
    }

    selected_entity: QuestEntityModel | undefined;

    constructor(
        three_renderer: DisposableThreeRenderer,
        create_model_manager: (renderer: QuestRenderer) => Quest3DModelManager,
    ) {
        super(three_renderer);

        this.disposer.add(create_model_manager(this));
    }

    /**
     * Initialize camera-controls after {@link QuestEntityControls} to ensure correct order of event
     * listener registration. This is a fragile work-around for the fact that camera-controls
     * doesn't support intercepting pointer events.
     */
    init_camera_controls(): void {
        super.init_camera_controls();

        this.controls.verticalDragToForward = true;
        this.controls.truckSpeed = 2.5;

        this.controls.addEventListener("update", this.camera_controls_updated);
    }

    dispose(): void {
        super.dispose();
        this.disposer.dispose();
    }

    set_size(width: number, height: number): void {
        this.camera.aspect = width / height;
        this.camera.updateProjectionMatrix();
        super.set_size(width, height);
    }

    reset_entity_models(): void {
        this.scene.remove(this._entity_models);
        this._entity_models = new Group();
        this.scene.add(this._entity_models);
        this.entity_to_mesh.clear();
        this.schedule_render();
    }

    add_entity_model(model: Mesh): void {
        const entity = (model.userData as EntityUserData).entity;
        this._entity_models.add(model);
        this.entity_to_mesh.set(entity, model);

        if (entity === this.selected_entity) {
            this.mark_selected(model);
        }

        this.schedule_render();
    }

    remove_entity_model(entity: QuestEntityModel): void {
        const mesh = this.entity_to_mesh.get(entity);

        if (mesh) {
            this.entity_to_mesh.delete(entity);
            this._entity_models.remove(mesh);
            this.schedule_render();
        }
    }

    get_entity_mesh(entity: QuestEntityModel): Mesh | undefined {
        return this.entity_to_mesh.get(entity);
    }

    mark_selected(entity_mesh: Mesh): void {
        if (entity_mesh === this.hovered_mesh) {
            this.hovered_mesh = undefined;
        }

        if (entity_mesh !== this.selected_mesh) {
            if (this.selected_mesh) {
                set_color(this.selected_mesh, ColorType.Normal);
            }

            set_color(entity_mesh, ColorType.Selected);

            this.schedule_render();
        }

        this.selected_mesh = entity_mesh;
    }

    mark_hovered(entity_mesh?: Mesh): void {
        if (!this.selected_mesh || entity_mesh !== this.selected_mesh) {
            if (entity_mesh !== this.hovered_mesh) {
                if (this.hovered_mesh) {
                    set_color(this.hovered_mesh, ColorType.Normal);
                }

                if (entity_mesh) {
                    set_color(entity_mesh, ColorType.Hovered);
                }

                this.schedule_render();
            }

            this.hovered_mesh = entity_mesh;
        }
    }

    unmark_selected(): void {
        if (this.selected_mesh) {
            set_color(this.selected_mesh, ColorType.Normal);
            this.schedule_render();
        }

        this.selected_mesh = undefined;
    }

    protected render(): void {
        const distance = this.controls.distance;
        this.camera.near = distance / 100;
        this.camera.far = Math.max(1_000, distance * 5);
        this.camera.updateProjectionMatrix();
        super.render();
    }

    private camera_controls_updated = (): void => {
        window.clearTimeout(this.camera_target_timeout);
        // If we call update_camera_target directly here, the camera will
        // randomly rotate when panning and releasing the mouse button quickly.
        // No idea why, but wrapping this call in a timeout makes it work.
        this.camera_target_timeout = window.setTimeout(this.update_camera_target, 100);
    };

    private update_camera_target = (): void => {
        // If the user moved the camera, try setting the camera
        // target to a better point.
        this.controls.updateCameraUp();
        const { intersection } = pick_ground(this, ZERO_VECTOR_2, ZERO_VECTOR_3);

        if (intersection) {
            this.controls.getTarget(this.old_camera_target);
            const new_target = intersection.point;

            if (new_target.distanceTo(this.old_camera_target) > 10) {
                this.controls.setTarget(new_target.x, new_target.y, new_target.z);
            }
        }

        this.camera_target_timeout = undefined;
    };
}

function set_color(mesh: Mesh, type: ColorType): void {
    const entity = (mesh.userData as EntityUserData).entity;
    const color = entity instanceof QuestNpcModel ? NPC_COLORS[type] : OBJECT_COLORS[type];

    if (mesh) {
        if (Array.isArray(mesh.material)) {
            for (const mat of mesh.material as MeshLambertMaterial[]) {
                if (type === ColorType.Normal && mat.map) {
                    mat.color.set(0xffffff);
                } else {
                    mat.color.set(color);
                }
            }
        } else {
            (mesh.material as MeshLambertMaterial).color.set(color);
        }
    }
}
