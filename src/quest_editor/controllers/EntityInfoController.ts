import { Controller } from "../../core/controllers/Controller";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { Property } from "../../core/observable/property/Property";
import { QuestNpcModel } from "../model/QuestNpcModel";
import { flat_map_to_list, list_property, property } from "../../core/observable";
import { Euler, Vector3 } from "three";
import { deg_to_rad } from "../../core/math";
import { TranslateEntityAction } from "../actions/TranslateEntityAction";
import { RotateEntityAction } from "../actions/RotateEntityAction";
import { euler } from "../model/euler";
import { entity_data } from "../../core/data_formats/parsing/quest/Quest";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { QuestEntityPropModel } from "../model/QuestEntityPropModel";

const DUMMY_VECTOR = Object.freeze(new Vector3());
const DUMMY_EULER = Object.freeze(new Euler());

export class EntityInfoController extends Controller {
    readonly unavailable: Property<boolean>;
    readonly enabled: Property<boolean>;
    readonly type: Property<string>;
    readonly name: Property<string>;
    readonly section_id: Property<string>;
    readonly wave: Property<string>;
    readonly wave_hidden: Property<boolean>;
    readonly position: Property<Vector3>;
    readonly rotation: Property<Euler>;
    readonly props: ListProperty<QuestEntityPropModel>;

    constructor(private readonly store: QuestEditorStore) {
        super();

        const entity = store.selected_entity;
        this.unavailable = entity.map(e => e == undefined);
        this.enabled = store.quest_runner.running.map(r => !r);

        this.type = entity.map(e => (e instanceof QuestNpcModel ? "NPC" : "Object"));
        this.name = entity.map(e => (e == undefined ? "" : entity_data(e.type).name));
        this.section_id = entity.flat_map(e =>
            e == undefined ? property("") : e.section_id.map(id => id.toString()),
        );
        this.wave = entity.flat_map(e =>
            e instanceof QuestNpcModel
                ? e.wave.flat_map(w => w?.id?.map(w => w.toString()) ?? property("None"))
                : property(""),
        );
        this.wave_hidden = entity.map(e => !(e instanceof QuestNpcModel));
        this.position = entity.flat_map(e => e?.position ?? property(DUMMY_VECTOR));
        this.rotation = entity.flat_map(e => e?.rotation ?? property(DUMMY_EULER));
        this.props = flat_map_to_list(e => e?.props ?? list_property(), entity);
    }

    focused = (): void => {
        this.store.undo.make_current();
    };

    set_pos_x(x: number): void {
        const entity = this.store.selected_entity.val;

        if (entity) {
            const pos = entity.position.val;
            this.store.undo
                .push(
                    new TranslateEntityAction(
                        this.store,
                        entity,
                        entity.section.val,
                        entity.section.val,
                        pos,
                        new Vector3(x, pos.y, pos.z),
                        false,
                    ),
                )
                .redo();
        }
    }

    set_pos_y(y: number): void {
        const entity = this.store.selected_entity.val;

        if (entity) {
            const pos = entity.position.val;
            this.store.undo
                .push(
                    new TranslateEntityAction(
                        this.store,
                        entity,
                        entity.section.val,
                        entity.section.val,
                        pos,
                        new Vector3(pos.x, y, pos.z),
                        false,
                    ),
                )
                .redo();
        }
    }

    set_pos_z(z: number): void {
        const entity = this.store.selected_entity.val;

        if (entity) {
            const pos = entity.position.val;
            this.store.undo
                .push(
                    new TranslateEntityAction(
                        this.store,
                        entity,
                        entity.section.val,
                        entity.section.val,
                        pos,
                        new Vector3(pos.x, pos.y, z),
                        false,
                    ),
                )
                .redo();
        }
    }

    set_rot_x(x: number): void {
        const entity = this.store.selected_entity.val;

        if (entity) {
            const rot = entity.rotation.val;
            this.store.undo
                .push(
                    new RotateEntityAction(
                        this.store,
                        entity,
                        rot,
                        euler(deg_to_rad(x), rot.y, rot.z),
                        false,
                    ),
                )
                .redo();
        }
    }

    set_rot_y(y: number): void {
        const entity = this.store.selected_entity.val;

        if (entity) {
            const rot = entity.rotation.val;
            this.store.undo
                .push(
                    new RotateEntityAction(
                        this.store,
                        entity,
                        rot,
                        euler(rot.x, deg_to_rad(y), rot.z),
                        false,
                    ),
                )
                .redo();
        }
    }

    set_rot_z(z: number): void {
        const entity = this.store.selected_entity.val;

        if (entity) {
            const rot = entity.rotation.val;
            this.store.undo
                .push(
                    new RotateEntityAction(
                        this.store,
                        entity,
                        rot,
                        euler(rot.x, rot.y, deg_to_rad(z)),
                        false,
                    ),
                )
                .redo();
        }
    }
}
