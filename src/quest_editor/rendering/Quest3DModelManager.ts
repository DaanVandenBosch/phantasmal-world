import { Intersection, Mesh, Object3D, Raycaster, Vector3 } from "three";
import { QuestRenderer } from "./QuestRenderer";
import { QuestEntityModel } from "../model/QuestEntityModel";
import { Disposer } from "../../core/observable/Disposer";
import { Disposable } from "../../core/observable/Disposable";
import { create_entity_mesh } from "./conversion/entities";
import { AreaUserData } from "./conversion/areas";
import {
    ListChangeEvent,
    ListChangeType,
    ListProperty,
} from "../../core/observable/property/list/ListProperty";
import { QuestNpcModel } from "../model/QuestNpcModel";
import { QuestObjectModel } from "../model/QuestObjectModel";
import { entity_type_to_string } from "../../core/data_formats/parsing/quest/Quest";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { AreaVariantModel } from "../model/AreaVariantModel";
import { EntityAssetLoader } from "../loading/EntityAssetLoader";
import { AreaAssetLoader } from "../loading/AreaAssetLoader";
import { LogManager } from "../../core/logging";
import { Property } from "../../core/observable/property/Property";
import { WaveModel } from "../model/WaveModel";
import { map } from "../../core/observable";

const logger = LogManager.get("quest_editor/rendering/QuestModelManager");

const CAMERA_POSITION = Object.freeze(new Vector3(0, 800, 700));
const CAMERA_LOOK_AT = Object.freeze(new Vector3(0, 0, 0));
const DUMMY_OBJECT = new Object3D();

export type AreaVariantDetails = {
    readonly episode: Episode | undefined;
    readonly area_variant: AreaVariantModel | undefined;
    readonly npcs: ListProperty<QuestNpcModel>;
    readonly objects: ListProperty<QuestObjectModel>;
};

/**
 * Loads the necessary area and entity 3D models into {@link QuestRenderer}.
 */
export abstract class Quest3DModelManager implements Disposable {
    protected readonly disposer = new Disposer();

    private readonly quest_disposer = this.disposer.add(new Disposer());
    private readonly area_model_manager: Area3DModelManager;
    private readonly npc_model_manager: Entity3DModelManager;
    private readonly object_model_manager: Entity3DModelManager;

    protected constructor(
        current_wave: Property<WaveModel | undefined>,
        private readonly renderer: QuestRenderer,
        private readonly area_asset_loader: AreaAssetLoader,
        private readonly entity_asset_loader: EntityAssetLoader,
    ) {
        this.area_model_manager = new Area3DModelManager(this.renderer, area_asset_loader);
        this.npc_model_manager = new Entity3DModelManager(
            current_wave,
            this.renderer,
            entity_asset_loader,
        );
        this.object_model_manager = new Entity3DModelManager(
            current_wave,
            this.renderer,
            entity_asset_loader,
        );
    }

    dispose(): void {
        this.disposer.dispose();
        this.npc_model_manager.remove_all();
        this.object_model_manager.remove_all();
        this.renderer.reset_entity_models();
    }

    /**
     * Called when an area variant needs to be loaded.
     */
    protected abstract get_area_variant_details(): AreaVariantDetails;

    protected area_variant_changed = async (): Promise<void> => {
        // Load area model.
        const { episode, area_variant, npcs, objects } = this.get_area_variant_details();

        await this.area_model_manager.load(episode, area_variant);

        this.quest_disposer.dispose_all();
        this.npc_model_manager.remove_all();
        this.object_model_manager.remove_all();
        this.renderer.reset_entity_models();

        // Load entity models.
        this.quest_disposer.add_all(
            npcs.observe_list(this.npcs_changed, { call_now: true }),
            objects.observe_list(this.objects_changed, { call_now: true }),
        );
    };

    private npcs_changed = (change: ListChangeEvent<QuestNpcModel>): void => {
        if (change.type === ListChangeType.ListChange) {
            this.npc_model_manager.remove(change.removed);

            this.npc_model_manager.add(change.inserted);
        }
    };

    private objects_changed = (change: ListChangeEvent<QuestObjectModel>): void => {
        if (change.type === ListChangeType.ListChange) {
            this.object_model_manager.remove(change.removed);

            this.object_model_manager.add(change.inserted);
        }
    };
}

class Area3DModelManager {
    private readonly raycaster = new Raycaster();
    private readonly origin = new Vector3();
    private readonly down = Object.freeze(new Vector3(0, -1, 0));
    private readonly up = Object.freeze(new Vector3(0, 1, 0));
    private area_variant?: AreaVariantModel;

    constructor(
        private readonly renderer: QuestRenderer,
        private readonly area_asset_loader: AreaAssetLoader,
    ) {}

    async load(episode?: Episode, area_variant?: AreaVariantModel): Promise<void> {
        this.area_variant = area_variant;

        if (episode == undefined || area_variant == undefined) {
            this.renderer.collision_geometry = DUMMY_OBJECT;
            this.renderer.render_geometry = DUMMY_OBJECT;
            return;
        }

        try {
            const collision_geometry = await this.area_asset_loader.load_collision_geometry(
                episode,
                area_variant,
            );
            if (this.should_cancel(area_variant)) return;

            const render_geometry = await this.area_asset_loader.load_render_geometry(
                episode,
                area_variant,
            );
            if (this.should_cancel(area_variant)) return;

            this.add_sections_to_collision_geometry(collision_geometry, render_geometry);

            this.renderer.collision_geometry = collision_geometry;
            this.renderer.render_geometry = render_geometry;

            this.renderer.reset_camera(CAMERA_POSITION, CAMERA_LOOK_AT);
        } catch (e) {
            logger.error(
                `Couldn't load models for area ${area_variant.area.id}, variant ${area_variant.id}.`,
                e,
            );

            this.renderer.collision_geometry = DUMMY_OBJECT;
            this.renderer.render_geometry = DUMMY_OBJECT;
        }
    }

    /**
     * Ensures that {@link load} is reentrant.
     */
    private should_cancel(area_variant: AreaVariantModel): boolean {
        return this.area_variant !== area_variant;
    }

    private add_sections_to_collision_geometry(
        collision_geom: Object3D,
        render_geom: Object3D,
    ): void {
        for (const collision_area of collision_geom.children) {
            (collision_area as Mesh).geometry.boundingBox!.getCenter(this.origin);

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

class Entity3DModelManager {
    private readonly queue: QuestEntityModel[] = [];
    private readonly loaded_entities: {
        entity: QuestEntityModel;
        disposer: Disposer;
    }[] = [];
    private loading = false;

    constructor(
        private readonly current_wave: Property<WaveModel | undefined>,
        private readonly renderer: QuestRenderer,
        private readonly entity_asset_loader: EntityAssetLoader,
    ) {}

    async add(entities: readonly QuestEntityModel[]): Promise<void> {
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

    remove(entities: readonly QuestEntityModel[]): void {
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
        let orig_model: number | undefined;

        while (true) {
            orig_model = entity.model.val;

            const geom = await this.entity_asset_loader.load_geometry(
                entity.type,
                entity.model.val,
            );
            if (!this.queue.includes(entity)) return; // Could be cancelled by now.
            if (entity.model.val != orig_model) continue; // Load again if model changed.

            const tex = await this.entity_asset_loader.load_textures(entity.type, entity.model.val);
            if (!this.queue.includes(entity)) return; // Could be cancelled by now.
            if (entity.model.val != orig_model) continue; // Load again if model changed.

            const model = create_entity_mesh(entity, geom, tex);
            this.update_entity_geometry(entity, model);

            break;
        }
    }

    private update_entity_geometry(entity: QuestEntityModel, model: Mesh): void {
        this.renderer.add_entity_model(model);

        const disposer = new Disposer(
            entity.world_position.observe(({ value }) => {
                model.position.copy(value);
                this.renderer.schedule_render();
            }),

            entity.world_rotation.observe(({ value }) => {
                model.rotation.copy(value);
                this.renderer.schedule_render();
            }),

            entity.model.observe(() => {
                this.remove([entity]);
                this.add([entity]);
            }),
        );

        if (entity instanceof QuestNpcModel) {
            disposer.add(
                map(
                    (current_wave, entity_wave) =>
                        current_wave == undefined || current_wave === entity_wave,
                    this.current_wave,
                    entity.wave,
                ).observe(
                    ({ value }) => {
                        model.visible = value;
                        this.renderer.schedule_render();
                    },
                    { call_now: true },
                ),
            );
        }

        this.loaded_entities.push({
            entity,
            disposer,
        });
    }
}
