import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { el } from "../../core/gui/dom";
import { DisabledView } from "./DisabledView";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { QuestNpcModel } from "../model/QuestNpcModel";
import { entity_data } from "../../core/data_formats/parsing/quest/entities";
import "./EntityInfoView.css";
import { NumberInput } from "../../core/gui/NumberInput";
import { Disposer } from "../../core/observable/Disposer";
import { Property } from "../../core/observable/property/Property";
import { Vec3 } from "../../core/data_formats/vector";
import { QuestEntityModel } from "../model/QuestEntityModel";

export class EntityInfoView extends ResizableWidget {
    readonly element = el.div({ class: "quest_editor_EntityInfoView", tab_index: -1 });

    private readonly no_entity_view = new DisabledView("No entity selected.");

    private readonly table_element = el.table();

    private readonly type_element: HTMLTableCellElement;
    private readonly name_element: HTMLTableCellElement;
    private readonly section_id_element: HTMLTableCellElement;
    private readonly pos_x_element = this.disposable(
        new NumberInput(0, { width: 80, round_to: 3 }),
    );
    private readonly pos_y_element = this.disposable(
        new NumberInput(0, { width: 80, round_to: 3 }),
    );
    private readonly pos_z_element = this.disposable(
        new NumberInput(0, { width: 80, round_to: 3 }),
    );
    private readonly world_pos_x_element = this.disposable(
        new NumberInput(0, { width: 80, round_to: 3 }),
    );
    private readonly world_pos_y_element = this.disposable(
        new NumberInput(0, { width: 80, round_to: 3 }),
    );
    private readonly world_pos_z_element = this.disposable(
        new NumberInput(0, { width: 80, round_to: 3 }),
    );

    private readonly entity_disposer = new Disposer();

    constructor() {
        super();

        const entity = quest_editor_store.selected_entity;
        const no_entity = entity.map(e => e == undefined);
        const coord_class = "quest_editor_EntityInfoView_coord";

        this.table_element.append(
            el.tr({}, el.th({ text: "Type:" }), (this.type_element = el.td())),
            el.tr({}, el.th({ text: "Name:" }), (this.name_element = el.td())),
            el.tr({}, el.th({ text: "Section:" }), (this.section_id_element = el.td())),
            el.tr({}, el.th({ text: "Section position:", col_span: 2 })),
            el.tr(
                {},
                el.th({ text: "X:", class: coord_class }),
                el.td({}, this.pos_x_element.element),
            ),
            el.tr(
                {},
                el.th({ text: "Y:", class: coord_class }),
                el.td({}, this.pos_y_element.element),
            ),
            el.tr(
                {},
                el.th({ text: "Z:", class: coord_class }),
                el.td({}, this.pos_z_element.element),
            ),
            el.tr({}, el.th({ text: "World position:", col_span: 2 })),
            el.tr(
                {},
                el.th({ text: "X:", class: coord_class }),
                el.td({}, this.world_pos_x_element.element),
            ),
            el.tr(
                {},
                el.th({ text: "Y:", class: coord_class }),
                el.td({}, this.world_pos_y_element.element),
            ),
            el.tr(
                {},
                el.th({ text: "Z:", class: coord_class }),
                el.td({}, this.world_pos_z_element.element),
            ),
        );

        this.element.append(this.table_element, this.no_entity_view.element);

        this.element.addEventListener("focus", () => quest_editor_store.undo.make_current(), true);

        this.bind_hidden(this.table_element, no_entity);

        this.disposables(
            this.no_entity_view.visible.bind_to(no_entity),

            entity.observe(({ value: entity }) => {
                this.entity_disposer.dispose_all();

                if (entity) {
                    this.type_element.innerText =
                        entity instanceof QuestNpcModel ? "NPC" : "Object";
                    const name = entity_data(entity.type).name;
                    this.name_element.innerText = name;
                    this.name_element.title = name;

                    this.entity_disposer.add(
                        entity.section_id.observe(
                            ({ value: section_id }) => {
                                this.section_id_element.innerText = section_id.toString();
                            },
                            { call_now: true },
                        ),
                    );

                    this.observe(
                        entity,
                        entity.position,
                        false,
                        this.pos_x_element,
                        this.pos_y_element,
                        this.pos_z_element,
                    );

                    this.observe(
                        entity,
                        entity.world_position,
                        true,
                        this.world_pos_x_element,
                        this.world_pos_y_element,
                        this.world_pos_z_element,
                    );
                }
            }),
        );

        this.finalize_construction(EntityInfoView.prototype);
    }

    dispose(): void {
        super.dispose();
        this.entity_disposer.dispose();
    }

    private observe(
        entity: QuestEntityModel,
        pos: Property<Vec3>,
        world: boolean,
        x_input: NumberInput,
        y_input: NumberInput,
        z_input: NumberInput,
    ): void {
        this.entity_disposer.add_all(
            pos.observe(
                ({ value: { x, y, z } }) => {
                    x_input.value.val = x;
                    y_input.value.val = y;
                    z_input.value.val = z;
                },
                { call_now: true },
            ),

            x_input.value.observe(({ value }) =>
                quest_editor_store.translate_entity(
                    entity,
                    entity.section.val,
                    entity.section.val,
                    pos.val,
                    new Vec3(value, pos.val.y, pos.val.z),
                    world,
                ),
            ),

            y_input.value.observe(({ value }) =>
                quest_editor_store.translate_entity(
                    entity,
                    entity.section.val,
                    entity.section.val,
                    pos.val,
                    new Vec3(pos.val.x, value, pos.val.z),
                    world,
                ),
            ),

            z_input.value.observe(({ value }) =>
                quest_editor_store.translate_entity(
                    entity,
                    entity.section.val,
                    entity.section.val,
                    pos.val,
                    new Vec3(pos.val.x, pos.val.y, value),
                    world,
                ),
            ),
        );
    }
}
