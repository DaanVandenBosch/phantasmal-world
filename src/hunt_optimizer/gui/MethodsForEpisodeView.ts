import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { el } from "../../core/gui/dom";
import { hunt_method_stores } from "../stores/HuntMethodStore";
import { HuntMethodModel } from "../model/HuntMethodModel";
import {
    ENEMY_NPC_TYPES,
    npc_data,
    NpcType,
} from "../../core/data_formats/parsing/quest/npc_types";
import "./MethodsForEpisodeView.css";
import { Disposer } from "../../core/observable/Disposer";
import { DurationInput } from "../../core/gui/DurationInput";

export class MethodsForEpisodeView extends ResizableWidget {
    private readonly episode: Episode;
    private readonly enemy_types: NpcType[];
    private readonly tbody_element: HTMLTableSectionElement;
    private readonly time_disposer = this.disposable(new Disposer());

    constructor(episode: Episode) {
        super(el.div({ class: "hunt_optimizer_MethodsForEpisodeView" }));

        this.episode = episode;

        this.enemy_types = ENEMY_NPC_TYPES.filter(type => npc_data(type).episode === this.episode);

        const table_element = el.table();
        const thead_element = el.thead();
        const header_tr_element = el.tr();

        header_tr_element.append(el.th({ text: "Method" }), el.th({ text: "Time" }));

        for (const enemy_type of this.enemy_types) {
            header_tr_element.append(
                el.th({
                    text: npc_data(enemy_type).simple_name,
                }),
            );
        }

        this.tbody_element = el.tbody();

        thead_element.append(header_tr_element);
        table_element.append(thead_element, this.tbody_element);
        this.element.append(table_element);

        hunt_method_stores.current.val.methods.load();

        this.disposables(
            hunt_method_stores.current
                .flat_map(current => current.methods)
                .observe(({ value }) => this.update_table(value)),
        );
    }

    private update_table(methods: HuntMethodModel[]): void {
        this.time_disposer.dispose_all();
        const frag = document.createDocumentFragment();

        for (const method of methods) {
            if (method.episode === this.episode) {
                const time_input = this.time_disposer.add(new DurationInput(method.time.val));

                this.time_disposer.add(
                    time_input.value.observe(({ value }) => method.set_user_time(value)),
                );

                const cells: HTMLTableCellElement[] = [
                    el.th({ text: method.name }),
                    el.th({ class: "input" }, time_input.element),
                ];

                // One cell per enemy type.
                for (const enemy_type of this.enemy_types) {
                    const count = method.enemy_counts.get(enemy_type);
                    cells.push(el.td({ text: count == undefined ? "" : count.toString() }));
                }

                frag.append(el.tr({}, ...cells));
            }
        }

        this.tbody_element.innerHTML = "";
        this.tbody_element.append(frag);
    }
}
