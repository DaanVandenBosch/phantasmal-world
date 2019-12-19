import { Renderer } from "../../core/rendering/Renderer";
import { Group, Mesh, Object3D, PerspectiveCamera } from "three";
import { QuestEntityModel } from "../model/QuestEntityModel";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { QuestModelManager } from "./QuestModelManager";
import { Disposer } from "../../core/observable/Disposer";
import { QuestEntityControls } from "./QuestEntityControls";
import { EntityUserData } from "./conversion/entities";

export class QuestRenderer extends Renderer {
    private _collision_geometry = new Object3D();
    private _render_geometry = new Object3D();
    private _entity_models = new Object3D();
    private readonly disposer = new Disposer();
    private readonly entity_to_mesh = new Map<QuestEntityModel, Mesh>();
    private readonly entity_controls = this.disposer.add(new QuestEntityControls(this));

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

    constructor(ModelManager: new (renderer: QuestRenderer) => QuestModelManager) {
        super();

        this.disposer.add_all(
            new ModelManager(this),

            quest_editor_store.debug.observe(({ value }) => (this.debug = value)),
        );

        // Initialize camera-controls after QuestEntityControls to ensure correct order of event
        // listener registration. This is a fragile work-around for the fact that camera-controls
        // doesn't support intercepting pointer events.
        this.init_camera_controls();
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

        if (entity === quest_editor_store.selected_entity.val) {
            this.entity_controls.mark_selected(model);
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
}
