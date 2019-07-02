import { Button, InputNumber, Popover } from "antd";
import { observer } from "mobx-react";
import React, { ReactNode, Component } from "react";
import { AutoSizer, Column, Table, TableCellRenderer } from "react-virtualized";
import { hunt_optimizer_store, WantedItem } from "../../stores/HuntOptimizerStore";
import { item_type_stores } from "../../stores/ItemTypeStore";
import { BigSelect } from "../BigSelect";
import "./WantedItemsComponent.less";

@observer
export class WantedItemsComponent extends Component {
    state = {
        help_visible: false,
    };

    render(): ReactNode {
        // Make sure render is called on updates.
        hunt_optimizer_store.wanted_items.slice(0, 0);

        return (
            <section className="ho-WantedItemsComponent">
                <h3>
                    Wanted Items
                    <Popover
                        content={<Help />}
                        trigger="click"
                        visible={this.state.help_visible}
                        onVisibleChange={this.on_help_visible_change}
                    >
                        <Button icon="info-circle" type="link" />
                    </Popover>
                </h3>
                <div className="ho-WantedItemsComponent-top-bar">
                    <BigSelect
                        placeholder="Add an item"
                        value={undefined}
                        style={{ width: 200 }}
                        options={hunt_optimizer_store.huntable_item_types.map(itemType => ({
                            label: itemType.name,
                            value: itemType.id,
                        }))}
                        onChange={this.add_wanted}
                    />
                    <Button onClick={hunt_optimizer_store.optimize} style={{ marginLeft: 10 }}>
                        Optimize
                    </Button>
                </div>
                <div className="ho-WantedItemsComponent-table">
                    <AutoSizer>
                        {({ width, height }) => (
                            <Table
                                width={width}
                                height={height}
                                headerHeight={30}
                                rowHeight={30}
                                rowCount={hunt_optimizer_store.wanted_items.length}
                                rowGetter={({ index }) => hunt_optimizer_store.wanted_items[index]}
                                noRowsRenderer={this.no_rows_renderer}
                            >
                                <Column
                                    label="Amount"
                                    dataKey="amount"
                                    width={70}
                                    cellRenderer={({ rowData }) => (
                                        <WantedAmountCell wantedItem={rowData} />
                                    )}
                                />
                                <Column
                                    label="Item"
                                    dataKey="item"
                                    width={150}
                                    flexGrow={1}
                                    cellDataGetter={({ rowData }) =>
                                        (rowData as WantedItem).item_type.name
                                    }
                                />
                                <Column
                                    dataKey="remove"
                                    width={30}
                                    cellRenderer={this.table_remove_cell_renderer}
                                />
                            </Table>
                        )}
                    </AutoSizer>
                </div>
            </section>
        );
    }

    private add_wanted = (selected: any) => {
        if (selected) {
            let added = hunt_optimizer_store.wanted_items.find(
                w => w.item_type.id === selected.value
            );

            if (!added) {
                const item_type = item_type_stores.current.value.get_by_id(selected.value)!;
                hunt_optimizer_store.wanted_items.push(new WantedItem(item_type, 1));
            }
        }
    };

    private remove_wanted = (wanted: WantedItem) => () => {
        const i = hunt_optimizer_store.wanted_items.findIndex(w => w === wanted);

        if (i !== -1) {
            hunt_optimizer_store.wanted_items.splice(i, 1);
        }
    };

    private table_remove_cell_renderer: TableCellRenderer = ({ rowData }) => {
        return <Button type="link" icon="delete" onClick={this.remove_wanted(rowData)} />;
    };

    private no_rows_renderer = () => {
        return (
            <div className="ho-WantedItemsComponent-no-rows">
                <p>
                    Add some items with the above drop down and click "Optimize" to see the result
                    on the right.
                </p>
            </div>
        );
    };

    private on_help_visible_change = (visible: boolean) => {
        this.setState({ helpVisible: visible });
    };
}

function Help(): JSX.Element {
    return (
        <div className="ho-WantedItemsComponent-help">
            <p>
                Add some items with the drop down and click "Optimize" to see the optimal
                combination of hunt methods on the right.
            </p>
            <p>
                At the moment a method is simply a quest run-through. Partial quest run-throughs are
                coming. View the list of methods on the "Methods" tab. Each method takes a certain
                amount of time, which affects the optimization result. Make sure the times are
                correct for you.
            </p>
            <p>Only enemy drops are considered. Box drops are coming.</p>
            <p>
                The optimal result is calculated using linear optimization. The optimizer takes rare
                enemies and the fact that pan arms can be split in two into account.
            </p>
        </div>
    );
}

@observer
class WantedAmountCell extends Component<{ wantedItem: WantedItem }> {
    render(): ReactNode {
        const wanted = this.props.wantedItem;

        return (
            <InputNumber
                min={0}
                max={10}
                value={wanted.amount}
                onChange={this.wanted_amount_changed}
                size="small"
                style={{ width: "100%" }}
            />
        );
    }

    private wanted_amount_changed = (value?: number) => {
        if (value != null && value >= 0) {
            this.props.wantedItem.amount = value;
        }
    };
}
