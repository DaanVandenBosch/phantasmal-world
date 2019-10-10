import { property } from "../../core/observable";
import { QuestModel } from "../model/QuestModel";
import { Property, PropertyChangeEvent } from "../../core/observable/property/Property";
import { read_file } from "../../core/read_file";
import { parse_quest, write_quest_qst } from "../../core/data_formats/parsing/quest";
import { ArrayBufferCursor } from "../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../core/data_formats/Endianness";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { QuestObjectModel } from "../model/QuestObjectModel";
import { QuestNpcModel } from "../model/QuestNpcModel";
import { AreaModel } from "../model/AreaModel";
import { area_store } from "./AreaStore";
import { SectionModel } from "../model/SectionModel";
import { QuestEntityModel } from "../model/QuestEntityModel";
import { Disposable } from "../../core/observable/Disposable";
import { Disposer } from "../../core/observable/Disposer";
import { gui_store, GuiTool } from "../../core/stores/GuiStore";
import { UndoStack } from "../../core/undo/UndoStack";
import { TranslateEntityAction } from "../actions/TranslateEntityAction";
import { EditShortDescriptionAction } from "../actions/EditShortDescriptionAction";
import { EditLongDescriptionAction } from "../actions/EditLongDescriptionAction";
import { EditNameAction } from "../actions/EditNameAction";
import { EditIdAction } from "../actions/EditIdAction";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { create_new_quest } from "./quest_creation";
import { CreateEntityAction } from "../actions/CreateEntityAction";
import { RemoveEntityAction } from "../actions/RemoveEntityAction";
import { Euler, Vector3 } from "three";
import { vec3_to_threejs } from "../../core/rendering/conversion";
import { RotateEntityAction } from "../actions/RotateEntityAction";
import { ExecutionResult, VirtualMachine } from "../scripting/vm";
import { QuestWaveModel } from "../model/QuestWaveModel";
import { DatWaveActionType } from "../../core/data_formats/parsing/quest/dat";
import {
    QuestWaveActionLockModel,
    QuestWaveActionSpawnNpcsModel,
    QuestWaveActionSpawnWaveModel,
    QuestWaveActionUnlockModel,
} from "../model/QuestWaveActionModel";
import Logger = require("js-logger");

const logger = Logger.get("quest_editor/gui/QuestEditorStore");

export class QuestEditorStore implements Disposable {
    private readonly disposer = new Disposer();
    private readonly _current_quest_filename = property<string | undefined>(undefined);
    private readonly _current_quest = property<QuestModel | undefined>(undefined);
    private readonly _current_area = property<AreaModel | undefined>(undefined);
    private readonly _selected_entity = property<QuestEntityModel | undefined>(undefined);

    readonly debug: WritableProperty<boolean> = property(false);
    readonly undo = new UndoStack();
    readonly current_quest_filename: Property<string | undefined> = this._current_quest_filename;
    readonly current_quest: Property<QuestModel | undefined> = this._current_quest;
    readonly current_area: Property<AreaModel | undefined> = this._current_area;
    readonly selected_entity: Property<QuestEntityModel | undefined> = this._selected_entity;

    constructor() {
        this.disposer.add_all(
            gui_store.tool.observe(
                ({ value: tool }) => {
                    if (tool === GuiTool.QuestEditor) {
                        this.undo.make_current();
                    }
                },
                { call_now: true },
            ),

            this.current_quest
                .flat_map(quest => (quest ? quest.npcs : property([])))
                .observe(({ value: npcs }) => {
                    const selected = this.selected_entity.val;

                    if (selected instanceof QuestNpcModel && !npcs.includes(selected)) {
                        this.set_selected_entity(undefined);
                    }
                }),

            this.current_quest
                .flat_map(quest => (quest ? quest.objects : property([])))
                .observe(({ value: objects }) => {
                    const selected = this.selected_entity.val;

                    if (selected instanceof QuestObjectModel && !objects.includes(selected)) {
                        this.set_selected_entity(undefined);
                    }
                }),
        );
    }

    dispose(): void {
        this.disposer.dispose();
    }

    set_current_area = (area?: AreaModel): void => {
        this._selected_entity.val = undefined;

        this._current_area.val = area;
    };

    set_selected_entity = (entity?: QuestEntityModel): void => {
        if (entity && this.current_quest.val) {
            this._current_area.val = area_store.get_area(
                this.current_quest.val.episode,
                entity.area_id,
            );
        }

        this._selected_entity.val = entity;
    };

    new_quest = (episode: Episode): void => {
        this.set_quest(create_new_quest(episode));
    };

    // TODO: notify user of problems.
    open_file = async (file: File): Promise<void> => {
        try {
            const buffer = await read_file(file);
            const quest = parse_quest(new ArrayBufferCursor(buffer, Endianness.Little));
            this.set_quest(
                quest &&
                    new QuestModel(
                        quest.id,
                        quest.language,
                        quest.name,
                        quest.short_description,
                        quest.long_description,
                        quest.episode,
                        quest.map_designations,
                        quest.objects.map(
                            obj =>
                                new QuestObjectModel(
                                    obj.type,
                                    obj.id,
                                    obj.group_id,
                                    obj.area_id,
                                    obj.section_id,
                                    vec3_to_threejs(obj.position),
                                    new Euler(
                                        obj.rotation.x,
                                        obj.rotation.y,
                                        obj.rotation.z,
                                        "ZXY",
                                    ),
                                    obj.properties,
                                    obj.unknown,
                                ),
                        ),
                        quest.npcs.map(
                            npc =>
                                new QuestNpcModel(
                                    npc.type,
                                    npc.pso_type_id,
                                    npc.npc_id,
                                    npc.script_label,
                                    npc.pso_roaming,
                                    npc.area_id,
                                    npc.section_id,
                                    vec3_to_threejs(npc.position),
                                    new Euler(
                                        npc.rotation.x,
                                        npc.rotation.y,
                                        npc.rotation.z,
                                        "ZXY",
                                    ),
                                    vec3_to_threejs(npc.scale),
                                    npc.unknown,
                                ),
                        ),
                        quest.waves.map(
                            wave =>
                                new QuestWaveModel(
                                    wave.id,
                                    wave.section_id,
                                    wave.wave,
                                    wave.delay,
                                    wave.actions.map(action => {
                                        switch (action.type) {
                                            case DatWaveActionType.SpawnNpcs:
                                                return new QuestWaveActionSpawnNpcsModel(
                                                    action.section_id,
                                                    action.appear_flag,
                                                );
                                            case DatWaveActionType.Unlock:
                                                return new QuestWaveActionUnlockModel(
                                                    action.door_id,
                                                );
                                            case DatWaveActionType.Lock:
                                                return new QuestWaveActionLockModel(action.door_id);
                                            case DatWaveActionType.SpawnWave:
                                                return new QuestWaveActionSpawnWaveModel(
                                                    action.wave_id,
                                                );
                                        }
                                    }),
                                    wave.area_id,
                                    wave.unknown,
                                ),
                        ),
                        quest.dat_unknowns,
                        quest.object_code,
                        quest.shop_items,
                    ),
                file.name,
            );
        } catch (e) {
            logger.error("Couldn't read file.", e);
        }
    };

    save_as = (): void => {
        const quest = this.current_quest.val;
        if (!quest) return;

        let default_file_name = this.current_quest_filename.val;

        if (default_file_name) {
            const ext_start = default_file_name.lastIndexOf(".");
            if (ext_start !== -1) default_file_name = default_file_name.slice(0, ext_start);
        }

        let file_name = prompt("File name:", default_file_name);
        if (!file_name) return;

        const buffer = write_quest_qst(
            {
                id: quest.id.val,
                language: quest.language.val,
                name: quest.name.val,
                short_description: quest.short_description.val,
                long_description: quest.long_description.val,
                episode: quest.episode,
                objects: quest.objects.val.map(obj => ({
                    type: obj.type,
                    area_id: obj.area_id,
                    section_id: obj.section_id.val,
                    position: obj.position.val,
                    rotation: obj.rotation.val,
                    unknown: obj.unknown,
                    id: obj.id,
                    group_id: obj.group_id,
                    properties: obj.properties,
                })),
                npcs: quest.npcs.val.map(npc => ({
                    type: npc.type,
                    area_id: npc.area_id,
                    section_id: npc.section_id.val,
                    position: npc.position.val,
                    rotation: npc.rotation.val,
                    scale: npc.scale,
                    unknown: npc.unknown,
                    pso_type_id: npc.pso_type_id,
                    npc_id: npc.npc_id,
                    script_label: npc.script_label,
                    pso_roaming: npc.pso_roaming,
                })),
                waves: quest.waves.val.map(wave => ({
                    id: wave.id,
                    section_id: wave.section_id,
                    wave: wave.wave,
                    delay: wave.delay,
                    actions: wave.actions.map(action => {
                        if (action instanceof QuestWaveActionSpawnNpcsModel) {
                            return {
                                type: DatWaveActionType.SpawnNpcs,
                                section_id: action.section_id,
                                appear_flag: action.appear_flag,
                            };
                        } else if (action instanceof QuestWaveActionUnlockModel) {
                            return {
                                type: DatWaveActionType.Unlock,
                                door_id: action.door_id,
                            };
                        } else if (action instanceof QuestWaveActionLockModel) {
                            return {
                                type: DatWaveActionType.Lock,
                                door_id: action.door_id,
                            };
                        } else if (action instanceof QuestWaveActionSpawnWaveModel) {
                            return {
                                type: DatWaveActionType.SpawnWave,
                                wave_id: action.wave_id,
                            };
                        } else {
                            throw new Error(
                                `Unknown wave action type ${
                                    Object.getPrototypeOf(action).constructor
                                }`,
                            );
                        }
                    }),
                    area_id: wave.area_id,
                    unknown: wave.unknown,
                })),
                dat_unknowns: quest.dat_unknowns,
                object_code: quest.object_code,
                shop_items: quest.shop_items,
                map_designations: quest.map_designations.val,
            },
            file_name,
        );

        if (!file_name.endsWith(".qst")) {
            file_name += ".qst";
        }

        const a = document.createElement("a");
        a.href = URL.createObjectURL(new Blob([buffer], { type: "application/octet-stream" }));
        a.download = file_name;
        document.body.appendChild(a);
        a.click();
        URL.revokeObjectURL(a.href);
        document.body.removeChild(a);
    };

    id_changed = (event: PropertyChangeEvent<number>): void => {
        if (this.current_quest.val) {
            this.undo.push(new EditIdAction(this.current_quest.val, event)).redo();
        }
    };

    name_changed = (event: PropertyChangeEvent<string>): void => {
        if (this.current_quest.val) {
            this.undo.push(new EditNameAction(this.current_quest.val, event)).redo();
        }
    };

    short_description_changed = (event: PropertyChangeEvent<string>): void => {
        if (this.current_quest.val) {
            this.undo.push(new EditShortDescriptionAction(this.current_quest.val, event)).redo();
        }
    };

    long_description_changed = (event: PropertyChangeEvent<string>): void => {
        if (this.current_quest.val) {
            this.undo.push(new EditLongDescriptionAction(this.current_quest.val, event)).redo();
        }
    };

    translate_entity = (
        entity: QuestEntityModel,
        old_section: SectionModel | undefined,
        new_section: SectionModel | undefined,
        old_position: Vector3,
        new_position: Vector3,
        world: boolean,
    ): void => {
        this.undo
            .push(
                new TranslateEntityAction(
                    entity,
                    old_section,
                    new_section,
                    old_position,
                    new_position,
                    world,
                ),
            )
            .redo();
    };

    rotate_entity = (
        entity: QuestEntityModel,
        old_rotation: Euler,
        new_rotation: Euler,
        world: boolean,
    ): void => {
        this.undo.push(new RotateEntityAction(entity, old_rotation, new_rotation, world)).redo();
    };

    push_create_entity_action = (entity: QuestEntityModel): void => {
        this.undo.push(new CreateEntityAction(entity));
    };

    remove_entity = (entity: QuestEntityModel): void => {
        this.undo.push(new RemoveEntityAction(entity)).redo();
    };

    private async set_quest(quest?: QuestModel, filename?: string): Promise<void> {
        this.undo.reset();

        this._current_area.val = undefined;
        this._selected_entity.val = undefined;

        this._current_quest_filename.val = filename;
        this._current_quest.val = quest;

        if (quest) {
            this._current_area.val = area_store.get_area(quest.episode, 0);

            // Load section data.
            for (const variant of quest.area_variants.val) {
                const sections = await area_store.get_area_sections(quest.episode, variant);
                variant.set_sections(sections);

                for (const object of quest.objects.val.filter(o => o.area_id === variant.area.id)) {
                    try {
                        this.set_section_on_quest_entity(object, sections);
                    } catch (e) {
                        logger.error(e);
                    }
                }

                for (const npc of quest.npcs.val.filter(npc => npc.area_id === variant.area.id)) {
                    try {
                        this.set_section_on_quest_entity(npc, sections);
                    } catch (e) {
                        logger.error(e);
                    }
                }
            }
        } else {
            logger.error("Couldn't parse quest file.");
        }
    }

    private set_section_on_quest_entity = (
        entity: QuestEntityModel,
        sections: SectionModel[],
    ): void => {
        const section = sections.find(s => s.id === entity.section_id.val);

        if (section) {
            entity.set_section(section);
        } else {
            logger.warn(`Section ${entity.section_id.val} not found.`);
        }
    };

    run_current_quest_in_vm = (): void => {
        logger.setLevel(logger.TRACE);

        const quest = this.current_quest.val;

        if (!quest) {
            throw new Error("No quest");
        }

        const vm = new VirtualMachine();
        vm.load_object_code(quest.object_code);
        vm.start_thread(0);

        exec_loop: while (true) {
            const exec_result = vm.execute();

            switch (exec_result) {
                case ExecutionResult.Ok:
                    break;
                case ExecutionResult.WaitingVsync:
                    vm.vsync();
                    break;
                case ExecutionResult.Halted:
                    break exec_loop;
            }
        }
    };
}

export const quest_editor_store = new QuestEditorStore();
