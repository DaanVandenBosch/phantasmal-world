import { TimePicker } from "antd";
import { observer } from "mobx-react";
import moment, { Moment } from "moment";
import React, { ReactNode, Component } from "react";
import { AutoSizer, Index, SortDirection } from "react-virtualized";
import { Episode, HuntMethod } from "../../domain";
import { EnemyNpcTypes, NpcType } from "../../domain/NpcType";
import { hunt_method_store } from "../../stores/HuntMethodStore";
import { BigTable, Column, ColumnSort } from "../BigTable";
import styles from "./MethodsComponent.css";

@observer
export class MethodsComponent extends Component {
    static columns: Column<HuntMethod>[] = (() => {
        // Standard columns.
        const columns: Column<HuntMethod>[] = [
            {
                key: "name",
                name: "Method",
                width: 250,
                cell_renderer: method => method.name,
                sortable: true,
            },
            {
                key: "episode",
                name: "Ep.",
                width: 34,
                cell_renderer: method => Episode[method.episode],
                sortable: true,
            },
            {
                key: "time",
                name: "Time",
                width: 50,
                cell_renderer: method => <TimeComponent method={method} />,
                class_name: "integrated",
                sortable: true,
            },
        ];

        // One column per enemy type.
        for (const enemy of EnemyNpcTypes) {
            columns.push({
                key: enemy.code,
                name: enemy.name,
                width: 75,
                cell_renderer: method => {
                    const count = method.enemy_counts.get(enemy);
                    return count == null ? "" : count.toString();
                },
                class_name: "number",
                sortable: true,
            });
        }

        return columns;
    })();

    render(): ReactNode {
        const methods = hunt_method_store.methods.current.value;

        return (
            <section className={styles.main}>
                <AutoSizer>
                    {({ width, height }) => (
                        <BigTable<HuntMethod>
                            width={width}
                            height={height}
                            row_count={methods.length}
                            columns={MethodsComponent.columns}
                            fixed_column_count={3}
                            record={this.record}
                            sort={this.sort}
                            update_trigger={hunt_method_store.methods.current.value}
                        />
                    )}
                </AutoSizer>
            </section>
        );
    }

    private record = ({ index }: Index) => {
        return hunt_method_store.methods.current.value[index];
    };

    private sort = (sorts: ColumnSort<HuntMethod>[]) => {
        const methods = hunt_method_store.methods.current.value.slice();

        methods.sort((a, b) => {
            for (const { column, direction } of sorts) {
                let cmp = 0;

                if (column.key === "name") {
                    cmp = a.name.localeCompare(b.name);
                } else if (column.key === "episode") {
                    cmp = a.episode - b.episode;
                } else if (column.key === "time") {
                    cmp = a.time - b.time;
                } else if (column.key) {
                    const type = NpcType.by_code(column.key);

                    if (type) {
                        cmp = (a.enemy_counts.get(type) || 0) - (b.enemy_counts.get(type) || 0);
                    }
                }

                if (cmp !== 0) {
                    return direction === SortDirection.ASC ? cmp : -cmp;
                }
            }

            return 0;
        });

        hunt_method_store.methods.current.value = methods;
    };
}

@observer
class TimeComponent extends React.Component<{ method: HuntMethod }> {
    render(): ReactNode {
        const time = this.props.method.time;
        const hour = Math.floor(time);
        const minute = Math.round(60 * (time - hour));

        return (
            <TimePicker
                className={styles.timepicker}
                value={moment({ hour, minute })}
                format="HH:mm"
                size="small"
                allowClear={false}
                suffixIcon={<span />}
                onChange={this.change}
            />
        );
    }

    private change = (time: Moment) => {
        this.props.method.user_time = time.hour() + time.minute() / 60;
    };
}
