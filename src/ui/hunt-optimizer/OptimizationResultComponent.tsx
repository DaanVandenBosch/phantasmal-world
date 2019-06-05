import { Table } from "antd";
import { observer } from "mobx-react";
import React from "react";
import { Item } from "../../domain";
import { huntOptimizerStore, OptimizationResult } from "../../stores/HuntOptimizerStore";

@observer
export class OptimizationResultComponent extends React.Component {
    render() {
        const items = new Set<Item>();

        for (const r of huntOptimizerStore.result) {
            for (const i of r.itemCounts.keys()) {
                items.add(i);
            }
        }

        return (
            <section>
                <h2>Optimization Result</h2>
                <Table
                    dataSource={huntOptimizerStore.result}
                    pagination={false}
                    rowKey={(_, index) => index.toString()}
                    size="small"
                    scroll={{ x: true, y: true }}
                >
                    <Table.Column title="Difficulty" dataIndex="difficulty" />
                    <Table.Column title="Method" dataIndex="methodName" />
                    <Table.Column title="Section ID" dataIndex="sectionId" />
                    <Table.Column title="Hours/Run" dataIndex="methodTime" render={this.fixed1} />
                    <Table.Column title="Runs" dataIndex="runs" render={this.fixed1} />
                    <Table.Column title="Total Hours" dataIndex="totalTime" render={this.fixed1} />
                    {[...items].map(item =>
                        <Table.Column<OptimizationResult>
                            title={item.name}
                            key={item.name}
                            render={(_, result) => {
                                const count = result.itemCounts.get(item);
                                return count && count.toFixed(2);
                            }}
                        />
                    )}
                </Table>
            </section>
        );
    }

    private fixed1(time: number): string {
        return time.toFixed(1);
    }
}
