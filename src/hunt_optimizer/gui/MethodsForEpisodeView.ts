import { Episode } from "../../core/data_formats/parsing/quest/Episode";
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
import { SortDirection, Table } from "../../core/gui/Table";
import { list_property } from "../../core/observable";
import { ServerMap } from "../../core/stores/ServerMap";
import { HuntMethodStore } from "../stores/HuntMethodStore";
import { LogManager } from "../../core/Logger";
import { div } from "../../core/gui/dom";
import { ResizableView } from "../../core/gui/ResizableView";

const logger = LogManager.get("hunt_optimizer/gui/MethodsForEpisodeView");

export class MethodsForEpisodeView extends ResizableView {
    readonly element = div({ className: "hunt_optimizer_MethodsForEpisodeView" });

    private readonly episode: Episode;
    private readonly enemy_types: NpcType[];
    private hunt_methods_observer?: Disposable;

    constructor(hunt_method_stores: ServerMap<HuntMethodStore>, episode: Episode) {
        super();

        this.episode = episode;

        this.enemy_types = ENEMY_NPC_TYPES.filter(type => npc_data(type).episode === this.episode);

        const hunt_methods = list_property<HuntMethodModel>();

        const table = this.add(
            new Table({
                class: "hunt_optimizer_MethodsForEpisodeView_table",
                values: hunt_methods,
                sort: sort_columns => {
                    hunt_methods.sort((a, b) => {
                        for (const { column, direction } of sort_columns) {
                            let cmp = 0;

                            switch (column.key) {
                                case "method":
                                    cmp = a.name.localeCompare(b.name);
                                    break;

                                case "time":
                                    cmp = a.time.val.as("minutes") - b.time.val.as("minutes");
                                    break;

                                default:
                                    {
                                        const type = (NpcType as any)[column.key!];

                                        if (type) {
                                            cmp =
                                                (a.enemy_counts.get(type) || 0) -
                                                (b.enemy_counts.get(type) || 0);
                                        }
                                    }
                                    break;
                            }

                            if (cmp !== 0) {
                                return direction === SortDirection.Asc ? cmp : -cmp;
                            }
                        }

                        return 0;
                    });
                },
                columns: [
                    {
                        key: "method",
                        title: "Method",
                        fixed: true,
                        width: 250,
                        sortable: true,
                        render_cell(method: HuntMethodModel) {
                            return method.name;
                        },
                    },
                    {
                        key: "time",
                        title: "Time",
                        fixed: true,
                        width: 60,
                        input: true,
                        sortable: true,
                        render_cell(method: HuntMethodModel, disposer: Disposer) {
                            const time_input = disposer.add(new DurationInput(method.time.val));

                            disposer.add(
                                time_input.value.observe(({ value }) =>
                                    method.set_user_time(value),
                                ),
                            );

                            return time_input.element;
                        },
                    },
                    ...this.enemy_types.map(enemy_type => {
                        return {
                            key: NpcType[enemy_type],
                            title: npc_data(enemy_type).simple_name,
                            width: 90,
                            text_align: "right",
                            sortable: true,
                            render_cell(method: HuntMethodModel) {
                                const count = method.enemy_counts.get(enemy_type);
                                return count == undefined ? "" : count.toString();
                            },
                        };
                    }),
                ],
            }),
        );

        this.element.append(table.element);

        this.disposables(
            hunt_method_stores.current.observe(
                async ({ value }) => {
                    try {
                        const hunt_method_store = await value;

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
                    } catch (e) {
                        logger.error("Couldn't load hunt optimizer store.", e);
                    }
                },
                { call_now: true },
            ),
        );

        this.finalize_construction(MethodsForEpisodeView);
    }

    dispose(): void {
        super.dispose();

        if (this.hunt_methods_observer) {
            this.hunt_methods_observer.dispose();
        }
    }
}
