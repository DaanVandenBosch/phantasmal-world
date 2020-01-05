import { bind_attr, div, table, td, th, tr } from "../../core/gui/dom";
import "./NpcCountsView.css";
import { UnavailableView } from "./UnavailableView";
import { NameWithCount, NpcCountsController } from "../controllers/NpcCountsController";
import { ResizableView } from "../../core/gui/ResizableView";

export class NpcCountsView extends ResizableView {
    readonly element = div({ className: "quest_editor_NpcCountsView" });

    private readonly table_element = table();

    private readonly unavailable_view = this.add(new UnavailableView("No quest loaded."));

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
            frag.append(tr(th(name + ":"), td(String(count))));
        }

        this.table_element.innerHTML = "";
        this.table_element.append(frag);
    }
}
