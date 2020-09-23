import { bind_attr, bind_children_to, div, table, td, th, tr } from "../../core/gui/dom";
import { UnavailableView } from "./UnavailableView";
import "./EntityInfoView.css";
import { NumberInput } from "../../core/gui/NumberInput";
import { rad_to_deg } from "../../core/math";
import { EntityInfoController } from "../controllers/EntityInfoController";
import { ResizableView } from "../../core/gui/ResizableView";
import { QuestEntityPropModel } from "../model/QuestEntityPropModel";
import { Disposable } from "../../core/observable/Disposable";
import { Disposer } from "../../core/observable/Disposer";
import { EntityPropType } from "../../core/data_formats/parsing/quest/properties";

export class EntityInfoView extends ResizableView {
    readonly element = div({ className: "quest_editor_EntityInfoView", tabIndex: -1 });

    private readonly no_entity_view = new UnavailableView("No entity selected.");

    private readonly standard_props_element = table();
    private readonly specific_props_element = table();

    private readonly type_element: HTMLTableCellElement;
    private readonly name_element: HTMLTableCellElement;
    private readonly section_id_element: HTMLTableCellElement;
    private readonly wave_element: HTMLTableCellElement;
    private readonly wave_row_element: HTMLTableRowElement;
    private readonly pos_x_element = this.add(new NumberInput(0, { round_to: 3 }));
    private readonly pos_y_element = this.add(new NumberInput(0, { round_to: 3 }));
    private readonly pos_z_element = this.add(new NumberInput(0, { round_to: 3 }));
    private readonly rot_x_element = this.add(new NumberInput(0, { round_to: 3 }));
    private readonly rot_y_element = this.add(new NumberInput(0, { round_to: 3 }));
    private readonly rot_z_element = this.add(new NumberInput(0, { round_to: 3 }));

    constructor(private readonly ctrl: EntityInfoController) {
        super();

        const coord_class = "quest_editor_EntityInfoView_coord";

        this.standard_props_element.append(
            tr(th("Type:"), (this.type_element = td())),
            tr(th("Name:"), (this.name_element = td())),
            tr(th("Section:"), (this.section_id_element = td())),
            (this.wave_row_element = tr(th("Wave:"), (this.wave_element = td()))),
            tr(th({ colSpan: 2 }, "Position:")),
            tr(th({ className: coord_class }, "X:"), td(this.pos_x_element.element)),
            tr(th({ className: coord_class }, "Y:"), td(this.pos_y_element.element)),
            tr(th({ className: coord_class }, "Z:"), td(this.pos_z_element.element)),
            tr(th({ colSpan: 2 }, "Rotation:")),
            tr(th({ className: coord_class }, "X:"), td(this.rot_x_element.element)),
            tr(th({ className: coord_class }, "Y:"), td(this.rot_y_element.element)),
            tr(th({ className: coord_class }, "Z:"), td(this.rot_z_element.element)),
        );

        bind_children_to(this.specific_props_element, ctrl.props, this.create_prop_row);

        this.element.append(
            this.standard_props_element,
            this.specific_props_element,
            this.no_entity_view.element,
        );

        this.element.addEventListener("focus", ctrl.focused, true);

        this.disposables(
            bind_attr(this.standard_props_element, "hidden", ctrl.unavailable),
            this.no_entity_view.visible.bind_to(ctrl.unavailable),

            bind_attr(this.type_element, "textContent", ctrl.type),
            bind_attr(this.name_element, "textContent", ctrl.name),
            bind_attr(this.section_id_element, "textContent", ctrl.section_id),
            bind_attr(this.wave_element, "textContent", ctrl.wave),
            bind_attr(this.wave_row_element, "hidden", ctrl.wave_hidden),

            ctrl.position.observe(
                ({ value: { x, y, z } }) => {
                    this.pos_x_element.value.val = x;
                    this.pos_y_element.value.val = y;
                    this.pos_z_element.value.val = z;
                },
                { call_now: true },
            ),

            ctrl.rotation.observe(
                ({ value: { x, y, z } }) => {
                    this.rot_x_element.value.val = rad_to_deg(x);
                    this.rot_y_element.value.val = rad_to_deg(y);
                    this.rot_z_element.value.val = rad_to_deg(z);
                },
                { call_now: true },
            ),

            this.pos_x_element.value.observe(({ value }) => ctrl.set_pos_x(value)),
            this.pos_y_element.value.observe(({ value }) => ctrl.set_pos_y(value)),
            this.pos_z_element.value.observe(({ value }) => ctrl.set_pos_z(value)),

            this.rot_x_element.value.observe(({ value }) => ctrl.set_rot_x(value)),
            this.rot_y_element.value.observe(({ value }) => ctrl.set_rot_y(value)),
            this.rot_z_element.value.observe(({ value }) => ctrl.set_rot_z(value)),

            this.enabled.bind_to(ctrl.enabled),
        );

        this.finalize_construction(EntityInfoView);
    }

    protected set_enabled(enabled: boolean): void {
        super.set_enabled(enabled);

        this.pos_x_element.enabled.val = enabled;
        this.pos_y_element.enabled.val = enabled;
        this.pos_z_element.enabled.val = enabled;

        this.rot_x_element.enabled.val = enabled;
        this.rot_y_element.enabled.val = enabled;
        this.rot_z_element.enabled.val = enabled;
    }

    private create_prop_row(prop: QuestEntityPropModel): [HTMLTableRowElement, Disposable] {
        const disposer = new Disposer();

        let min: number | undefined;
        let max: number | undefined;

        switch (prop.type) {
            case EntityPropType.U8:
                min = 0;
                max = 0xff;
                break;
            case EntityPropType.U16:
                min = 0;
                max = 0xffff;
                break;
            case EntityPropType.U32:
                min = 0;
                max = 0xffffffff;
                break;
            case EntityPropType.I8:
                min = -0x80;
                max = 0x7f;
                break;
            case EntityPropType.I16:
                min = -0x8000;
                max = 0x7fff;
                break;
            case EntityPropType.I32:
                min = -0x80000000;
                max = 0x7fffffff;
                break;
            case EntityPropType.Angle:
                min = -2 * Math.PI;
                max = 2 * Math.PI;
                break;
        }

        const round_to =
            prop.type === EntityPropType.F32 || prop.type === EntityPropType.Angle ? 3 : 1;

        const value_input = disposer.add(
            new NumberInput(prop.value.val, {
                min,
                max,
                round_to,
                enabled: false,
            }),
        );

        disposer.add_all(value_input.value.bind_to(prop.value));

        const element = tr(th(`${prop.name}:`), td(value_input.element));

        return [element, disposer];
    }
}
