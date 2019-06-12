import { observer } from "mobx-react";
import React from "react";
import { AutoSizer, Index } from "react-virtualized";
import { HuntMethod } from "../../domain";
import { EnemyNpcTypes } from "../../domain/NpcType";
import { huntMethodStore } from "../../stores/HuntMethodStore";
import "./MethodsComponent.css";
import { DataTable, Column } from "../dataTable";

@observer
export class MethodsComponent extends React.Component {
    static columns: Array<Column<HuntMethod>> = (() => {
        // Standard columns.
        const columns: Column<HuntMethod>[] = [
            {
                name: 'Method',
                width: 250,
                cellValue: (method) => method.name,
            },
            {
                name: 'Hours',
                width: 50,
                cellValue: (method) => method.time.toString(),
            },
        ];

        // One column per enemy type.
        for (const enemy of EnemyNpcTypes) {
            columns.push({
                name: enemy.name,
                width: 75,
                cellValue: (method) => {
                    const count = method.enemyCounts.get(enemy);
                    return count == null ? '' : count.toString();
                },
                className: 'number',
            });
        }

        return columns;
    })();

    render() {
        const methods = huntMethodStore.methods.current.value;

        return (
            <section className="ho-MethodsComponent">
                <AutoSizer>
                    {({ width, height }) => (
                        <DataTable<HuntMethod>
                            width={width}
                            height={height}
                            rowCount={methods.length}
                            columns={MethodsComponent.columns}
                            fixedColumnCount={2}
                            record={this.record}
                        />
                    )}
                </AutoSizer>
            </section>
        );
    }

    private record = ({ index }: Index) => {
        return huntMethodStore.methods.current.value[index];
    }
}