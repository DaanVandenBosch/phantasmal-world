import Logger from "js-logger";
import { Intersection, Mesh, Object3D, Raycaster, Vector3 } from "three";
import { QuestRenderer } from "./QuestRenderer";
import { QuestModel } from "../model/QuestModel";
import { load_entity_geometry, load_entity_textures } from "../loading/entities";
import { load_area_collision_geometry, load_area_render_geometry } from "../loading/areas";
import { QuestEntityModel } from "../model/QuestEntityModel";
import { Disposer } from "../../core/observable/Disposer";
import { Disposable } from "../../core/observable/Disposable";
import { AreaModel } from "../model/AreaModel";
import { create_entity_mesh } from "./conversion/entities";
import { AreaUserData } from "./conversion/areas";
import { quest_editor_store } from "../stores/QuestEditorStore";
import {
    ListChangeType,
    ListPropertyChangeEvent,
} from "../../core/observable/property/list/ListProperty";
import { QuestNpcModel } from "../model/QuestNpcModel";
import { QuestObjectModel } from "../model/QuestObjectModel";
import { entity_type_to_string } from "../../core/data_formats/parsing/quest/entities";

const logger = Logger.get("quest_editor/rendering/QuestModelManager");

const CAMERA_POSITION = new Vector3(0, 800, 700);
const CAMERA_LOOK_AT = new Vector3(0, 0, 0);
const DUMMY_OBJECT = new Object3D();

export class QuestModelManager implements Disposable {
    private readonly disposer = new Disposer();
    private readonly quest_disposer = this.disposer.add(new Disposer());
    private readonly area_model_manager: AreaModelManager;
    private readonly npc_model_manager: EntityModelManager;
    private readonly object_model_manager: EntityModelManager;

    constructor(private readonly renderer: QuestRenderer) {
        this.area_model_manager = new AreaModelManager(this.renderer);
        this.npc_model_manager = new EntityModelManager(this.renderer);
        this.object_model_manager = new EntityModelManager(this.renderer);

        this.disposer.add_all(
            quest_editor_store.current_quest.observe(this.quest_or_area_changed),

            quest_editor_store.current_area.observe(this.quest_or_area_changed),
        );
    }

    dispose(): void {
        this.disposer.dispose();
    }

    private quest_or_area_changed = async (): Promise<void> => {
        const quest = quest_editor_store.current_quest.val;
        const area = quest_editor_store.current_area.val;

        // Load area model.
        await this.area_model_manager.load(quest, area);

        if (
            quest !== quest_editor_store.current_quest.val ||
            area !== quest_editor_store.current_area.val
        ) {
            return;
        }

        this.quest_disposer.dispose_all();
        this.npc_model_manager.remove_all();
        this.object_model_manager.remove_all();
        this.renderer.reset_entity_models();

        // Load entity models.
        if (quest && area) {
            this.npc_model_manager.add(quest.npcs.val.filter(entity => entity.area_id === area.id));
            this.object_model_manager.add(
                quest.objects.val.filter(entity => entity.area_id === area.id),
            );
            this.quest_disposer.add_all(
                quest.npcs.observe_list(this.npcs_changed),
                quest.objects.observe_list(this.objects_changed),
            );
        }
    };

    private npcs_changed = (change: ListPropertyChangeEvent<QuestNpcModel>): void => {
        const area = quest_editor_store.current_area.val;

        if (change.type === ListChangeType.ListChange && area) {
            this.npc_model_manager.remove(change.removed);

            this.npc_model_manager.add(
                change.inserted.filter(entity => entity.area_id === area.id),
            );
        }
    };

    private objects_changed = (change: ListPropertyChangeEvent<QuestObjectModel>): void => {
        const area = quest_editor_store.current_area.val;

        if (change.type === ListChangeType.ListChange && area) {
            this.object_model_manager.remove(change.removed);

            this.object_model_manager.add(
                change.inserted.filter(entity => entity.area_id === area.id),
            );
        }
    };
}

class AreaModelManager {
    private readonly raycaster = new Raycaster();
    private readonly origin = new Vector3();
    private readonly down = new Vector3(0, -1, 0);
    private readonly up = new Vector3(0, 1, 0);
    private quest?: QuestModel;
    private area?: AreaModel;

    constructor(private readonly renderer: QuestRenderer) {}

    async load(quest?: QuestModel, area?: AreaModel): Promise<void> {
        this.quest = quest;
        this.area = area;

        if (!quest || !area) {
            this.renderer.collision_geometry = DUMMY_OBJECT;
            this.renderer.render_geometry = DUMMY_OBJECT;
            return;
        }

        try {
            const area_variant =
                quest.area_variants.val.find(v => v.area.id === area.id) || area.area_variants[0];

            // Load necessary area geometry.
            const episode = quest.episode;

            const collision_geometry = await load_area_collision_geometry(episode, area_variant);
            if (this.should_cancel(quest, area)) return;

            const render_geometry = await load_area_render_geometry(episode, area_variant);
            if (this.should_cancel(quest, area)) return;

            this.add_sections_to_collision_geometry(collision_geometry, render_geometry);

            this.renderer.collision_geometry = collision_geometry;
            this.renderer.render_geometry = render_geometry;

            this.renderer.reset_camera(CAMERA_POSITION, CAMERA_LOOK_AT);
        } catch (e) {
            logger.error(`Couldn't load area models for quest ${quest.id}, ${area.name}.`, e);

            this.renderer.collision_geometry = DUMMY_OBJECT;
            this.renderer.render_geometry = DUMMY_OBJECT;
        }
    }

    /**
     * Ensures that {@link load} is reentrant.
     */
    private should_cancel(quest: QuestModel, area: AreaModel): boolean {
        return this.quest !== quest || this.area !== area;
    }

    private add_sections_to_collision_geometry(
        collision_geom: Object3D,
        render_geom: Object3D,
    ): void {
        for (const collision_area of collision_geom.children) {
            (collision_area as Mesh).geometry.boundingBox.getCenter(this.origin);

            this.raycaster.set(this.origin, this.down);
            const intersection1 = this.raycaster
                .intersectObject(render_geom, true)
                .find(i => (i.object.userData as AreaUserData).section != undefined);

            this.raycaster.set(this.origin, this.up);
            const intersection2 = this.raycaster
                .intersectObject(render_geom, true)
                .find(i => (i.object.userData as AreaUserData).section != undefined);

            let intersection: Intersection | undefined;

            if (intersection1 && intersection2) {
                intersection =
                    intersection1.distance <= intersection2.distance
                        ? intersection1
                        : intersection2;
            } else {
                intersection = intersection1 || intersection2;
            }

            if (intersection) {
                const cud = collision_area.userData as AreaUserData;
                const rud = intersection.object.userData as AreaUserData;
                cud.section = rud.section;
            }
        }
    }
}

class EntityModelManager {
    private readonly queue: QuestEntityModel[] = [];
    private readonly loaded_entities: {
        entity: QuestEntityModel;
        disposer: Disposer;
    }[] = [];
    private loading = false;

    constructor(private readonly renderer: QuestRenderer) {}

    async add(entities: QuestEntityModel[]): Promise<void> {
        this.queue.push(...entities);

        if (!this.loading) {
            try {
                this.loading = true;

                while (this.queue.length) {
                    const entity = this.queue[0];

                    try {
                        await this.load(entity);
                    } catch (e) {
                        logger.error(
                            `Couldn't load model for entity ${entity_type_to_string(entity.type)}.`,
                            e,
                        );
                    } finally {
                        const index = this.queue.indexOf(entity);

                        if (index !== -1) {
                            this.queue.splice(index, 1);
                        }
                    }
                }
            } finally {
                this.loading = false;
            }
        }
    }

    remove(entities: QuestEntityModel[]): void {
        for (const entity of entities) {
            const queue_index = this.queue.indexOf(entity);

            if (queue_index !== -1) {
                this.queue.splice(queue_index, 1);
            }

            const loaded_index = this.loaded_entities.findIndex(loaded => loaded.entity === entity);

            if (loaded_index !== -1) {
                const loaded = this.loaded_entities.splice(loaded_index, 1)[0];

                this.renderer.remove_entity_model(loaded.entity);
                loaded.disposer.dispose();
            }
        }
    }

    remove_all(): void {
        for (const { disposer } of this.loaded_entities) {
            disposer.dispose();
        }

        this.loaded_entities.splice(0, Infinity);
        this.queue.splice(0, Infinity);
    }

    private async load(entity: QuestEntityModel): Promise<void> {
        const geom = await load_entity_geometry(entity.type);
        const tex = await load_entity_textures(entity.type);
        const model = create_entity_mesh(entity, geom, tex);

        // The model load might be cancelled by now.
        if (this.queue.includes(entity)) {
            this.update_entity_geometry(entity, model);
        }
    }

    private update_entity_geometry(entity: QuestEntityModel, model: Mesh): void {
        this.renderer.add_entity_model(model);

        this.loaded_entities.push({
            entity,
            disposer: new Disposer(
                entity.world_position.observe(({ value }) => {
                    model.position.copy(value);
                    this.renderer.schedule_render();
                }),

                entity.world_rotation.observe(({ value }) => {
                    model.rotation.copy(value);
                    this.renderer.schedule_render();
                }),
            ),
        });
    }
}
