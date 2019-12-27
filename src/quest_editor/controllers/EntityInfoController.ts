import { Controller } from "../../core/controllers/Controller";
import { QuestEditorStore } from "../stores/QuestEditorStore";
import { Property } from "../../core/observable/property/Property";
import { QuestNpcModel } from "../model/QuestNpcModel";
import { entity_data } from "../../core/data_formats/parsing/quest/entities";
import { property } from "../../core/observable";
import { Euler, Vector3 } from "three";
import { deg_to_rad } from "../../core/math";

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
    }

    focused = (): void => {
        this.store.undo.make_current();
    };

    set_pos_x(x: number): void {
        const entity = this.store.selected_entity.val;

        if (entity) {
            const pos = entity.position.val;
            this.store.translate_entity(
                entity,
                entity.section.val,
                entity.section.val,
                pos,
                new Vector3(x, pos.y, pos.z),
                false,
            );
        }
    }

    set_pos_y(y: number): void {
        const entity = this.store.selected_entity.val;

        if (entity) {
            const pos = entity.position.val;
            this.store.translate_entity(
                entity,
                entity.section.val,
                entity.section.val,
                pos,
                new Vector3(pos.x, y, pos.z),
                false,
            );
        }
    }

    set_pos_z(z: number): void {
        const entity = this.store.selected_entity.val;

        if (entity) {
            const pos = entity.position.val;
            this.store.translate_entity(
                entity,
                entity.section.val,
                entity.section.val,
                pos,
                new Vector3(pos.x, pos.y, z),
                false,
            );
        }
    }

    set_rot_x(x: number): void {
        const entity = this.store.selected_entity.val;

        if (entity) {
            const rot = entity.rotation.val;
            this.store.rotate_entity(
                entity,
                rot,
                new Euler(deg_to_rad(x), rot.y, rot.z, "ZXY"),
                false,
            );
        }
    }

    set_rot_y(y: number): void {
        const entity = this.store.selected_entity.val;

        if (entity) {
            const rot = entity.rotation.val;
            this.store.rotate_entity(
                entity,
                rot,
                new Euler(rot.x, deg_to_rad(y), rot.z, "ZXY"),
                false,
            );
        }
    }

    set_rot_z(z: number): void {
        const entity = this.store.selected_entity.val;

        if (entity) {
            const rot = entity.rotation.val;
            this.store.rotate_entity(
                entity,
                rot,
                new Euler(rot.x, rot.y, deg_to_rad(z), "ZXY"),
                false,
            );
        }
    }
}
