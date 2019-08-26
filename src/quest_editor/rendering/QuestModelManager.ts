import Logger from "js-logger";
import { Intersection, Mesh, Object3D, Raycaster, Vector3 } from "three";
import { QuestRenderer } from "./QuestRenderer";
import { QuestModel } from "../model/QuestModel";
import {
    load_npc_geometry,
    load_npc_textures,
    load_object_geometry,
    load_object_textures,
} from "../loading/entities";
import { load_area_collision_geometry, load_area_render_geometry } from "../loading/areas";
import { QuestEntityModel } from "../model/QuestEntityModel";
import { Disposer } from "../../core/observable/Disposer";
import { Disposable } from "../../core/observable/Disposable";
import { AreaModel } from "../model/AreaModel";
import { AreaVariantModel } from "../model/AreaVariantModel";
import { area_store } from "../stores/AreaStore";
import { create_npc_mesh, create_object_mesh } from "./conversion/entities";
import { AreaUserData } from "./conversion/areas";

const logger = Logger.get("quest_editor/rendering/QuestModelManager");

const CAMERA_POSITION = new Vector3(0, 800, 700);
const CAMERA_LOOK_AT = new Vector3(0, 0, 0);
const DUMMY_OBJECT = new Object3D();

export class QuestModelManager implements Disposable {
    private quest?: QuestModel;
    private area?: AreaModel;
    private area_variant?: AreaVariantModel;
    private disposer = new Disposer();

    constructor(private renderer: QuestRenderer) {}

    async load_models(quest?: QuestModel, area?: AreaModel): Promise<void> {
        let area_variant: AreaVariantModel | undefined;

        if (quest && area) {
            area_variant =
                quest.area_variants.val.find(v => v.area.id === area.id) ||
                area_store.get_variant(quest.episode, area.id, 0);
        }

        if (this.quest === quest && this.area_variant === area_variant) {
            return;
        }

        this.quest = quest;
        this.area = area;
        this.area_variant = area_variant;

        this.disposer.dispose_all();

        if (quest && area) {
            try {
                // Load necessary area geometry.
                const episode = quest.episode;
                const area_id = area.id;
                const variant_id = area_variant ? area_variant.id : 0;

                const collision_geometry = await load_area_collision_geometry(
                    episode,
                    area_id,
                    variant_id,
                );

                const render_geometry = await load_area_render_geometry(
                    episode,
                    area_id,
                    variant_id,
                );

                this.add_sections_to_collision_geometry(collision_geometry, render_geometry);

                if (this.quest !== quest || this.area_variant !== area_variant) return;

                this.renderer.collision_geometry = collision_geometry;
                this.renderer.render_geometry = render_geometry;

                this.renderer.reset_camera(CAMERA_POSITION, CAMERA_LOOK_AT);

                // Load entity models.
                this.renderer.reset_entity_models();

                for (const npc of quest.npcs.val) {
                    if (npc.area_id === area.id) {
                        const npc_geom = await load_npc_geometry(npc.type);
                        const npc_tex = await load_npc_textures(npc.type);

                        if (this.quest !== quest || this.area_variant !== area_variant) return;

                        const model = create_npc_mesh(npc, npc_geom, npc_tex);
                        this.update_entity_geometry(npc, model);
                    }
                }

                for (const object of quest.objects.val) {
                    if (object.area_id === area.id) {
                        const object_geom = await load_object_geometry(object.type);
                        const object_tex = await load_object_textures(object.type);

                        if (this.quest !== quest || this.area_variant !== area_variant) return;

                        const model = create_object_mesh(object, object_geom, object_tex);
                        this.update_entity_geometry(object, model);
                    }
                }
            } catch (e) {
                logger.error(`Couldn't load models for quest ${quest.id}, ${area.name}.`, e);
                this.renderer.collision_geometry = DUMMY_OBJECT;
                this.renderer.render_geometry = DUMMY_OBJECT;
                this.renderer.reset_entity_models();
            }
        } else {
            this.renderer.collision_geometry = DUMMY_OBJECT;
            this.renderer.render_geometry = DUMMY_OBJECT;
            this.renderer.reset_entity_models();
        }
    }

    dispose(): void {
        this.disposer.dispose();
    }

    private add_sections_to_collision_geometry(
        collision_geom: Object3D,
        render_geom: Object3D,
    ): void {
        const raycaster = new Raycaster();
        const origin = new Vector3();
        const down = new Vector3(0, -1, 0);
        const up = new Vector3(0, 1, 0);

        for (const collision_area of collision_geom.children) {
            (collision_area as Mesh).geometry.boundingBox.getCenter(origin);

            raycaster.set(origin, down);
            const intersection1 = raycaster
                .intersectObject(render_geom, true)
                .find(i => (i.object.userData as AreaUserData).section != undefined);

            raycaster.set(origin, up);
            const intersection2 = raycaster
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

    private update_entity_geometry(entity: QuestEntityModel, model: Mesh): void {
        this.renderer.add_entity_model(model);

        this.disposer.add_all(
            entity.world_position.observe(({ value: { x, y, z } }) => {
                model.position.set(x, y, z);
                this.renderer.schedule_render();
            }),

            entity.rotation.observe(({ value: { x, y, z } }) => {
                model.rotation.set(x, y, z);
                this.renderer.schedule_render();
            }),
        );
    }
}
