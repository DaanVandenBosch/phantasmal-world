import Logger from 'js-logger';
import { action, observable } from 'mobx';
import { AnimationClip, AnimationMixer, Object3D } from 'three';
import { BufferCursor } from '../bin_data/BufferCursor';
import { get_area_sections } from '../bin_data/loading/areas';
import { get_npc_geometry, get_object_geometry } from '../bin_data/loading/entities';
import { NinjaModel, NinjaObject, parse_nj, parse_xj } from '../bin_data/parsing/ninja';
import { parse_njm_4 } from '../bin_data/parsing/ninja/motion';
import { parse_quest, write_quest_qst } from '../bin_data/parsing/quest';
import { Area, Quest, QuestEntity, Section, Vec3 } from '../domain';
import { create_animation_clip } from '../rendering/animation';
import { create_npc_mesh as create_npc_object_3d, create_object_mesh as create_object_object_3d } from '../rendering/entities';
import { ninja_object_to_skinned_mesh } from '../rendering/models';

const logger = Logger.get('stores/QuestEditorStore');

function traverse(
    object: NinjaObject<NinjaModel>,
    head_part: NinjaObject<NinjaModel>,
    id_ref: [number]
) {
    if (!object.evaluation_flags.eval_skip) {
        const id = id_ref[0]++;

        if (id === 59) {
            object.evaluation_flags.hidden = false;
            object.evaluation_flags.break_child_trace = false;
            object.children.push(head_part);
            return;
        }
    }

    for (const child of object.children) {
        traverse(child, head_part, id_ref);
    }
}

class QuestEditorStore {
    @observable current_quest?: Quest;
    @observable current_area?: Area;
    @observable selected_entity?: QuestEntity;

    @observable.ref current_model?: NinjaObject<NinjaModel>;
    @observable.ref current_model_obj3d?: Object3D;
    @observable.ref animation_mixer?: AnimationMixer;

    set_quest = action('set_quest', (quest?: Quest) => {
        this.reset_model_and_quest_state();
        this.current_quest = quest;

        if (quest && quest.area_variants.length) {
            this.current_area = quest.area_variants[0].area;
        }
    })

    set_model = action('set_model', (model?: NinjaObject<NinjaModel>) => {
        this.reset_model_and_quest_state();

        if (model) {
            if (this.current_model) {
                traverse(this.current_model, model, [0]);
            } else {
                this.current_model = model;
            }

            this.current_model_obj3d = ninja_object_to_skinned_mesh(this.current_model);
        }
    })

    add_animation = action('add_animation', (clip: AnimationClip) => {
        if (!this.current_model_obj3d) return;

        if (this.animation_mixer) {
            this.animation_mixer.stopAllAction();
            this.animation_mixer.uncacheRoot(this.current_model_obj3d);
        } else {
            this.animation_mixer = new AnimationMixer(this.current_model_obj3d);
        }

        const action = this.animation_mixer.clipAction(clip);
        action.play();
    })

    private reset_model_and_quest_state() {
        this.current_quest = undefined;
        this.current_area = undefined;
        this.selected_entity = undefined;

        if (this.current_model && this.animation_mixer) {
            this.animation_mixer.uncacheRoot(this.current_model);
        }

        this.current_model_obj3d = undefined;
        this.animation_mixer = undefined;
    }

    set_selected_entity = (entity?: QuestEntity) => {
        this.selected_entity = entity;
    }

    set_current_area_id = action('set_current_area_id', (area_id?: number) => {
        this.selected_entity = undefined;

        if (area_id == null) {
            this.current_area = undefined;
        } else if (this.current_quest) {
            const area_variant = this.current_quest.area_variants.find(
                variant => variant.area.id === area_id
            );
            this.current_area = area_variant && area_variant.area;
        }
    })

    load_file = (file: File) => {
        const reader = new FileReader();
        reader.addEventListener('loadend', () => { this.loadend(file, reader) });
        reader.readAsArrayBuffer(file);
    }

    // TODO: notify user of problems.
    private loadend = async (file: File, reader: FileReader) => {
        if (!(reader.result instanceof ArrayBuffer)) {
            logger.error('Couldn\'t read file.');
            return;
        }

        if (file.name.endsWith('.nj')) {
            const model = parse_nj(new BufferCursor(reader.result, true))[0];
            this.set_model(model);
        } else if (file.name.endsWith('.xj')) {
            const model = parse_xj(new BufferCursor(reader.result, true))[0];
            this.set_model(model);
        } else if (file.name.endsWith('.njm')) {
            this.add_animation(
                create_animation_clip(
                    parse_njm_4(new BufferCursor(reader.result, true))
                )
            );
        } else {
            const quest = parse_quest(new BufferCursor(reader.result, true));
            this.set_quest(quest);

            if (quest) {
                // Load section data.
                for (const variant of quest.area_variants) {
                    const sections = await get_area_sections(
                        quest.episode,
                        variant.area.id,
                        variant.id
                    );
                    variant.sections = sections;

                    // Generate object geometry.
                    for (const object of quest.objects.filter(o => o.area_id === variant.area.id)) {
                        try {
                            const object_geom = await get_object_geometry(object.type);
                            this.set_section_on_visible_quest_entity(object, sections);
                            object.object_3d = create_object_object_3d(object, object_geom);
                        } catch (e) {
                            logger.error(e);
                        }
                    }

                    // Generate NPC geometry.
                    for (const npc of quest.npcs.filter(npc => npc.area_id === variant.area.id)) {
                        try {
                            const npc_geom = await get_npc_geometry(npc.type);
                            this.set_section_on_visible_quest_entity(npc, sections);
                            npc.object_3d = create_npc_object_3d(npc, npc_geom);
                        } catch (e) {
                            logger.error(e);
                        }
                    }
                }
            } else {
                logger.error('Couldn\'t parse quest file.');
            }
        }
    }

    private set_section_on_visible_quest_entity = async (
        entity: QuestEntity,
        sections: Section[]
    ) => {
        let { x, y, z } = entity.position;

        const section = sections.find(s => s.id === entity.section_id);
        entity.section = section;

        if (section) {
            const { x: sec_x, y: sec_y, z: sec_z } = section.position;
            const rot_x = section.cos_y_axis_rotation * x + section.sin_y_axis_rotation * z;
            const rot_z = -section.sin_y_axis_rotation * x + section.cos_y_axis_rotation * z;
            x = rot_x + sec_x;
            y += sec_y;
            z = rot_z + sec_z;
        } else {
            logger.warn(`Section ${entity.section_id} not found.`);
        }

        entity.position = new Vec3(x, y, z);
    }

    save_current_quest_to_file = (file_name: string) => {
        if (this.current_quest) {
            const cursor = write_quest_qst(this.current_quest, file_name);

            if (!file_name.endsWith('.qst')) {
                file_name += '.qst';
            }

            const a = document.createElement('a');
            a.href = URL.createObjectURL(new Blob([cursor.buffer]));
            a.download = file_name;
            document.body.appendChild(a);
            a.click();
            URL.revokeObjectURL(a.href);
            document.body.removeChild(a);
        }
    }
}

export const quest_editor_store = new QuestEditorStore();
