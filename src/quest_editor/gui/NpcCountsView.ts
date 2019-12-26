import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { bind_attr, el } from "../../core/gui/dom";
import "./NpcCountsView.css";
import { UnavailableView } from "./UnavailableView";
import { NameWithCount, NpcCountsController } from "../controllers/NpcCountsController";

export class NpcCountsView extends ResizableWidget {
    readonly element = el.div({ class: "quest_editor_NpcCountsView" });

    private readonly table_element = el.table();

    private readonly unavailable_view = new UnavailableView("No quest loaded.");

    constructor(ctrl: NpcCountsController) {
        super();

        this.element.append(this.table_element, this.unavailable_view.element);

        this.disposables(
            bind_attr(this.table_element, "hidden", ctrl.unavailable),

            this.unavailable_view.visible.bind_to(ctrl.unavailable),

            ctrl.npc_counts.observe(({ value }) => this.update_view(value), { call_now: true }),
        );

        this.finalize_construction();
    }

    private update_view(npcs: readonly NameWithCount[]): void {
        const frag = document.createDocumentFragment();

        for (const { name, count } of npcs) {
            frag.append(el.tr({}, el.th({ text: name + ":" }), el.td({ text: String(count) })));
        }

        this.table_element.innerHTML = "";
        this.table_element.append(frag);
    }
}
