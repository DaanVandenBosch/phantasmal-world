import Logger from "js-logger";
import { autorun, IReactionDisposer } from "mobx";
import { Intersection, Mesh, Object3D, Raycaster, Vector3 } from "three";
import { load_area_collision_geometry, load_area_render_geometry } from "../loading/areas";
import {
    load_npc_geometry,
    load_npc_textures,
    load_object_geometry,
    load_object_textures,
} from "../loading/entities";
import { create_npc_mesh, create_object_mesh } from "./conversion/entities";
import { QuestRenderer } from "./QuestRenderer";
import { AreaUserData } from "./conversion/areas";
import { ObservableQuest } from "../domain/ObservableQuest";
import { ObservableArea } from "../domain/ObservableArea";
import { ObservableAreaVariant } from "../domain/ObservableAreaVariant";
import { ObservableQuestEntity } from "../domain/observable_quest_entities";

const logger = Logger.get("rendering/QuestModelManager");

const CAMERA_POSITION = new Vector3(0, 800, 700);
const CAMERA_LOOKAT = new Vector3(0, 0, 0);
const DUMMY_OBJECT = new Object3D();

export class QuestModelManager {
    private quest?: ObservableQuest;
    private area?: ObservableArea;
    private area_variant?: ObservableAreaVariant;
    private entity_reaction_disposers: IReactionDisposer[] = [];

    constructor(private renderer: QuestRenderer) {}

    async load_models(quest?: ObservableQuest, area?: ObservableArea): Promise<void> {
        let area_variant: ObservableAreaVariant | undefined;

        if (quest && area) {
            area_variant = quest.area_variants.find(v => v.area.id === area.id);
        }

        if (this.quest === quest && this.area_variant === area_variant) {
            return;
        }

        this.quest = quest;
        this.area = area;
        this.area_variant = area_variant;

        this.dispose_entity_reactions();

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

                this.renderer.reset_camera(CAMERA_POSITION, CAMERA_LOOKAT);

                // Load entity models.
                this.renderer.reset_entity_models();

                for (const npc of quest.npcs) {
                    if (npc.area_id === area.id) {
                        const npc_geom = await load_npc_geometry(npc.type);
                        const npc_tex = await load_npc_textures(npc.type);

                        if (this.quest !== quest || this.area_variant !== area_variant) return;

                        const model = create_npc_mesh(npc, npc_geom, npc_tex);
                        this.update_entity_geometry(npc, model);
                    }
                }

                for (const object of quest.objects) {
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

    private update_entity_geometry(entity: ObservableQuestEntity, model: Mesh): void {
        this.renderer.add_entity_model(model);

        this.entity_reaction_disposers.push(
            autorun(() => {
                const { x, y, z } = entity.world_position;
                model.position.set(x, y, z);
                const rot = entity.rotation;
                model.rotation.set(rot.x, rot.y, rot.z);
                this.renderer.schedule_render();
            }),
        );
    }

    private dispose_entity_reactions(): void {
        for (const disposer of this.entity_reaction_disposers) {
            disposer();
        }
    }
}
