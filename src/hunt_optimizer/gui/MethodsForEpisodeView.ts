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
import { Disposable } from "../../core/observable/Disposable";
import { Table } from "../../core/gui/Table";
import { list_property } from "../../core/observable";

export class MethodsForEpisodeView extends ResizableWidget {
    private readonly episode: Episode;
    private readonly enemy_types: NpcType[];
    private hunt_methods_observer?: Disposable;

    constructor(episode: Episode) {
        super(el.div({ class: "hunt_optimizer_MethodsForEpisodeView" }));

        this.episode = episode;

        this.enemy_types = ENEMY_NPC_TYPES.filter(type => npc_data(type).episode === this.episode);

        const hunt_methods = list_property<HuntMethodModel>();

        const table = this.disposable(
            new Table({
                class: "hunt_optimizer_MethodsForEpisodeView_table",
                values: hunt_methods,
                columns: [
                    {
                        title: "Method",
                        sticky: true,
                        width: 250,
                        create_cell(method: HuntMethodModel): HTMLTableDataCellElement {
                            return el.th({ text: method.name });
                        },
                    },
                    {
                        title: "Time",
                        sticky: true,
                        width: 60,
                        create_cell(
                            method: HuntMethodModel,
                            disposer: Disposer,
                        ): HTMLTableDataCellElement {
                            const time_input = disposer.add(new DurationInput(method.time.val));

                            disposer.add(
                                time_input.value.observe(({ value }) =>
                                    method.set_user_time(value),
                                ),
                            );

                            return el.th({ class: "input" }, time_input.element);
                        },
                    },
                    ...this.enemy_types.map(enemy_type => {
                        return {
                            title: npc_data(enemy_type).simple_name,
                            width: 80,
                            create_cell(method: HuntMethodModel): HTMLTableDataCellElement {
                                const count = method.enemy_counts.get(enemy_type);
                                return el.td({ text: count == undefined ? "" : count.toString() });
                            },
                        };
                    }),
                ],
            }),
        );

        this.element.append(table.element);

        this.disposable(
            hunt_method_stores.observe_current(
                hunt_method_store => {
                    if (this.hunt_methods_observer) {
                        this.hunt_methods_observer.dispose();
                    }

                    this.hunt_methods_observer = hunt_method_store.methods.observe(
                        ({ value }) => {
                            hunt_methods.val = value.filter(
                                method => method.episode === this.episode,
                            );
                        },
                        {
                            call_now: true,
                        },
                    );
                },
                { call_now: true },
            ),
        );
    }

    dispose(): void {
        super.dispose();

        if (this.hunt_methods_observer) {
            this.hunt_methods_observer.dispose();
        }
    }
}
