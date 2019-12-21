import { DisposableThreeRenderer, Renderer } from "../../core/rendering/Renderer";
import { Group, Mesh, MeshLambertMaterial, Object3D, PerspectiveCamera } from "three";
import { QuestEntityModel } from "../model/QuestEntityModel";
import { QuestModelManager } from "./QuestModelManager";
import { Disposer } from "../../core/observable/Disposer";
import { ColorType, EntityUserData, NPC_COLORS, OBJECT_COLORS } from "./conversion/entities";
import { QuestNpcModel } from "../model/QuestNpcModel";

export class QuestRenderer extends Renderer {
    private _collision_geometry = new Object3D();
    private _render_geometry = new Object3D();
    private _entity_models = new Object3D();
    private readonly disposer = new Disposer();
    private readonly entity_to_mesh = new Map<QuestEntityModel, Mesh>();
    private hovered_mesh?: Mesh;
    private selected_mesh?: Mesh;

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

    readonly camera = new PerspectiveCamera(60, 1, 10, 10000);

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
        create_model_manager: (renderer: QuestRenderer) => QuestModelManager,
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
