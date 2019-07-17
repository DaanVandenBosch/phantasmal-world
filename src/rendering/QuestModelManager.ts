import { QuestRenderer } from "./QuestRenderer";
import { Quest, Area, QuestEntity } from "../domain";
import { IReactionDisposer, autorun } from "mobx";
import { Object3D, Group, Vector3 } from "three";
import { load_area_collision_geometry, load_area_render_geometry } from "../loading/areas";
import {
    load_object_geometry,
    load_object_tex as load_object_textures,
    load_npc_geometry,
    load_npc_tex as load_npc_textures,
} from "../loading/entities";
import { create_object_mesh, create_npc_mesh } from "./conversion/entities";
import Logger from "js-logger";

const logger = Logger.get("rendering/QuestModelManager");

const CAMERA_POSITION = new Vector3(0, 800, 700);
const CAMERA_LOOKAT = new Vector3(0, 0, 0);
const DUMMY_OBJECT = new Object3D();

export class QuestModelManager {
    private quest?: Quest;
    private area?: Area;
    private entity_reaction_disposers: IReactionDisposer[] = [];

    constructor(private renderer: QuestRenderer) {}

    async load_models(quest?: Quest, area?: Area): Promise<void> {
        if (this.quest === quest && this.area === area) {
            return;
        }

        this.quest = quest;
        this.area = area;

        this.dispose_entity_reactions();

        if (quest && area) {
            try {
                // Load necessary area geometry.
                const episode = quest.episode;
                const area_id = area.id;
                const variant = quest.area_variants.find(v => v.area.id === area_id);
                const variant_id = (variant && variant.id) || 0;

                const collision_geometry = await load_area_collision_geometry(
                    episode,
                    area_id,
                    variant_id
                );

                const render_geometry = await load_area_render_geometry(
                    episode,
                    area_id,
                    variant_id
                );

                if (this.quest !== quest || this.area !== area) return;

                this.renderer.collision_geometry = collision_geometry;
                this.renderer.render_geometry = render_geometry;

                this.renderer.reset_camera(CAMERA_POSITION, CAMERA_LOOKAT);

                // Load entity models.
                const npc_group = new Group();
                const obj_group = new Group();
                this.renderer.npc_geometry = npc_group;
                this.renderer.obj_geometry = obj_group;

                for (const npc of quest.npcs) {
                    if (npc.area_id === area.id) {
                        const npc_geom = await load_npc_geometry(npc.type);
                        const npc_tex = await load_npc_textures(npc.type);

                        if (this.quest !== quest || this.area !== area) return;

                        const model = create_npc_mesh(npc, npc_geom, npc_tex);
                        this.update_entity_geometry(npc, npc_group, model);
                    }
                }

                for (const object of quest.objects) {
                    if (object.area_id === area.id) {
                        const object_geom = await load_object_geometry(object.type);
                        const object_tex = await load_object_textures(object.type);

                        if (this.quest !== quest || this.area !== area) return;

                        const model = create_object_mesh(object, object_geom, object_tex);
                        this.update_entity_geometry(object, obj_group, model);
                    }
                }
            } catch (e) {
                logger.error(`Couldn't load models for quest ${quest.id}, ${area.name}.`, e);
                this.renderer.collision_geometry = DUMMY_OBJECT;
                this.renderer.render_geometry = DUMMY_OBJECT;
                this.renderer.obj_geometry = DUMMY_OBJECT;
                this.renderer.npc_geometry = DUMMY_OBJECT;
            }
        } else {
            this.renderer.collision_geometry = DUMMY_OBJECT;
            this.renderer.render_geometry = DUMMY_OBJECT;
            this.renderer.obj_geometry = DUMMY_OBJECT;
            this.renderer.npc_geometry = DUMMY_OBJECT;
        }
    }

    private update_entity_geometry(entity: QuestEntity, group: Group, model: Object3D): void {
        group.add(model);

        this.entity_reaction_disposers.push(
            autorun(() => {
                const { x, y, z } = entity.position;
                model.position.set(x, y, z);
                const rot = entity.rotation;
                model.rotation.set(rot.x, rot.y, rot.z);
                this.renderer.schedule_render();
            })
        );
    }

    private dispose_entity_reactions(): void {
        for (const disposer of this.entity_reaction_disposers) {
            disposer();
        }
    }
}
