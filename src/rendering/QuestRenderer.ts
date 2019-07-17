import { autorun } from "mobx";
import { Object3D, PerspectiveCamera } from "three";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { EntityControls } from "./EntityControls";
import { QuestModelManager } from "./QuestModelManager";
import { Renderer } from "./Renderer";

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

    private _obj_geometry = new Object3D();

    get obj_geometry(): Object3D {
        return this._obj_geometry;
    }

    set obj_geometry(obj_geometry: Object3D) {
        this.scene.remove(this._obj_geometry);
        this._obj_geometry = obj_geometry;
        this.scene.add(obj_geometry);
    }

    private _npc_geometry = new Object3D();

    get npc_geometry(): Object3D {
        return this._npc_geometry;
    }

    set npc_geometry(npc_geometry: Object3D) {
        this.scene.remove(this._npc_geometry);
        this._npc_geometry = npc_geometry;
        this.scene.add(npc_geometry);
    }

    constructor() {
        super(new PerspectiveCamera(60, 1, 10, 10000));

        const model_manager = new QuestModelManager(this);

        autorun(() => {
            model_manager.load_models(
                quest_editor_store.current_quest,
                quest_editor_store.current_area
            );
        });

        const entity_controls = new EntityControls(this);

        this.dom_element.addEventListener("mousedown", entity_controls.on_mouse_down);
        this.dom_element.addEventListener("mouseup", entity_controls.on_mouse_up);
        this.dom_element.addEventListener("mousemove", entity_controls.on_mouse_move);
    }

    set_size(width: number, height: number): void {
        this.camera.aspect = width / height;
        this.camera.updateProjectionMatrix();
        super.set_size(width, height);
    }
}
