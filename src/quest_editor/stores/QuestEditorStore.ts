import { property } from "../../core/observable";
import { QuestModel } from "../model/QuestModel";
import { Property } from "../../core/observable/property/Property";
import { QuestObjectModel } from "../model/QuestObjectModel";
import { QuestNpcModel } from "../model/QuestNpcModel";
import { AreaModel } from "../model/AreaModel";
import { SectionModel } from "../model/SectionModel";
import { QuestEntityModel } from "../model/QuestEntityModel";
import { GuiStore, GuiTool } from "../../core/stores/GuiStore";
import { UndoStack } from "../../core/undo/UndoStack";
import { TranslateEntityAction } from "../actions/TranslateEntityAction";
import { CreateEntityAction } from "../actions/CreateEntityAction";
import { RemoveEntityAction } from "../actions/RemoveEntityAction";
import { Euler, Vector3 } from "three";
import { RotateEntityAction } from "../actions/RotateEntityAction";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { QuestRunner } from "../QuestRunner";
import { AreaStore } from "./AreaStore";
import { disposable_listener } from "../../core/gui/dom";
import { Store } from "../../core/stores/Store";
import { LogManager } from "../../core/Logger";

const logger = LogManager.get("quest_editor/gui/QuestEditorStore");

export class QuestEditorStore extends Store {
    private readonly _current_quest = property<QuestModel | undefined>(undefined);
    private readonly _current_area = property<AreaModel | undefined>(undefined);
    private readonly _selected_entity = property<QuestEntityModel | undefined>(undefined);

    readonly quest_runner: QuestRunner;
    readonly debug: WritableProperty<boolean> = property(false);
    readonly undo = new UndoStack();
    readonly current_quest: Property<QuestModel | undefined> = this._current_quest;
    readonly current_area: Property<AreaModel | undefined> = this._current_area;
    readonly selected_entity: Property<QuestEntityModel | undefined> = this._selected_entity;

    constructor(gui_store: GuiStore, private readonly area_store: AreaStore) {
        super();

        this.quest_runner = new QuestRunner(area_store);

        this.disposables(
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

            disposable_listener(window, "beforeunload", e => {
                this.quest_runner.stop();

                if (this.undo.can_undo.val) {
                    e.preventDefault();
                    e.returnValue = false;
                }
            }),
        );
    }

    dispose(): void {
        this.quest_runner.stop();
        super.dispose();
    }

    set_current_area = (area?: AreaModel): void => {
        this._selected_entity.val = undefined;

        this._current_area.val = area;
    };

    set_selected_entity = (entity?: QuestEntityModel): void => {
        if (entity && this.current_quest.val) {
            this._current_area.val = this.area_store.get_area(
                this.current_quest.val.episode,
                entity.area_id,
            );
        }

        this._selected_entity.val = entity;
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
                    this,
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
        this.undo
            .push(new RotateEntityAction(this, entity, old_rotation, new_rotation, world))
            .redo();
    };

    push_create_entity_action = (entity: QuestEntityModel): void => {
        this.undo.push(new CreateEntityAction(this, entity));
    };

    remove_entity = (entity: QuestEntityModel): void => {
        this.undo.push(new RemoveEntityAction(this, entity)).redo();
    };

    async set_quest(quest?: QuestModel): Promise<void> {
        this.undo.reset();

        this.quest_runner.stop();

        this._current_area.val = undefined;
        this._selected_entity.val = undefined;

        this._current_quest.val = quest;

        if (quest) {
            this._current_area.val = this.area_store.get_area(quest.episode, 0);

            // Load section data.
            for (const variant of quest.area_variants.val) {
                const sections = await this.area_store.get_area_sections(quest.episode, variant);
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
}
