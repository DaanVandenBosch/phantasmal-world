import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { bind_attr, div, table, td, th, tr } from "../../core/gui/dom";
import { UnavailableView } from "./UnavailableView";
import "./EntityInfoView.css";
import { NumberInput } from "../../core/gui/NumberInput";
import { rad_to_deg } from "../../core/math";
import { EntityInfoController } from "../controllers/EntityInfoController";

export class EntityInfoView extends ResizableWidget {
    readonly element = div({ className: "quest_editor_EntityInfoView", tabIndex: -1 });

    private readonly no_entity_view = new UnavailableView("No entity selected.");

    private readonly table_element = table();

    private readonly type_element: HTMLTableCellElement;
    private readonly name_element: HTMLTableCellElement;
    private readonly section_id_element: HTMLTableCellElement;
    private readonly wave_element: HTMLTableCellElement;
    private readonly wave_row_element: HTMLTableRowElement;
    private readonly pos_x_element = this.disposable(
        new NumberInput(0, { width: 80, round_to: 3 }),
    );
    private readonly pos_y_element = this.disposable(
        new NumberInput(0, { width: 80, round_to: 3 }),
    );
    private readonly pos_z_element = this.disposable(
        new NumberInput(0, { width: 80, round_to: 3 }),
    );
    private readonly rot_x_element = this.disposable(
        new NumberInput(0, { width: 80, round_to: 3 }),
    );
    private readonly rot_y_element = this.disposable(
        new NumberInput(0, { width: 80, round_to: 3 }),
    );
    private readonly rot_z_element = this.disposable(
        new NumberInput(0, { width: 80, round_to: 3 }),
    );

    constructor(private readonly ctrl: EntityInfoController) {
        super();

        const coord_class = "quest_editor_EntityInfoView_coord";

        this.table_element.append(
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

        this.element.append(this.table_element, this.no_entity_view.element);

        this.element.addEventListener("focus", ctrl.focused, true);

        this.disposables(
            bind_attr(this.table_element, "hidden", ctrl.unavailable),
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

        this.finalize_construction();
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
}
