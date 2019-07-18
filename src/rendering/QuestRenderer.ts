import { autorun } from "mobx";
import { Mesh, Object3D, PerspectiveCamera, Group } from "three";
import { QuestEntity } from "../domain";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { QuestEntityControls } from "./QuestEntityControls";
import { QuestModelManager } from "./QuestModelManager";
import { Renderer } from "./Renderer";
import { EntityUserData } from "./conversion/entities";

let renderer: QuestRenderer | undefined;

export function get_quest_renderer(): QuestRenderer {
    if (!renderer) renderer = new QuestRenderer();
    return renderer;
}

export class QuestRenderer extends Renderer<PerspectiveCamera> {
    private _collision_geometry = new Object3D();

    get collision_geometry(): Object3D {
        return this._collision_geometry;
    }

    set collision_geometry(collision_geometry: Object3D) {
        this.scene.remove(this.collision_geometry);
        this._collision_geometry = collision_geometry;
        this.scene.add(collision_geometry);
    }

    private _render_geometry = new Object3D();

    get render_geometry(): Object3D {
        return this._render_geometry;
    }

    set render_geometry(render_geometry: Object3D) {
        // this.scene.remove(this._render_geometry);
        this._render_geometry = render_geometry;
        // this.scene.add(render_geometry);
    }

    private _entity_models = new Object3D();

    get entity_models(): Object3D {
        return this._entity_models;
    }

    private entity_to_mesh = new Map<QuestEntity, Mesh>();
    private entity_controls: QuestEntityControls;

    constructor() {
        super(new PerspectiveCamera(60, 1, 10, 10000));

        const model_manager = new QuestModelManager(this);

        autorun(() => {
            model_manager.load_models(
                quest_editor_store.current_quest,
                quest_editor_store.current_area
            );
        });

        this.entity_controls = new QuestEntityControls(this);

        this.dom_element.addEventListener("mousedown", this.entity_controls.on_mouse_down);
        this.dom_element.addEventListener("mouseup", this.entity_controls.on_mouse_up);
        this.dom_element.addEventListener("mousemove", this.entity_controls.on_mouse_move);
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
    }

    add_entity_model(model: Mesh): void {
        const entity = (model.userData as EntityUserData).entity;
        this._entity_models.add(model);
        this.entity_to_mesh.set(entity, model);

        if (entity === quest_editor_store.selected_entity) {
            this.entity_controls.try_highlight_selected();
        }
    }

    get_entity_mesh(entity: QuestEntity): Mesh | undefined {
        return this.entity_to_mesh.get(entity);
    }
}
