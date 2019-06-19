import { Button, InputNumber, Popover } from "antd";
import { observer } from "mobx-react";
import React from "react";
import { AutoSizer, Column, Table, TableCellRenderer } from "react-virtualized";
import { huntOptimizerStore, WantedItem } from "../../stores/HuntOptimizerStore";
import { itemKindStores } from "../../stores/ItemKindStore";
import { BigSelect } from "../BigSelect";
import './WantedItemsComponent.less';

@observer
export class WantedItemsComponent extends React.Component {
    state = {
        helpVisible: false
    }

    render() {
        // Make sure render is called on updates.
        huntOptimizerStore.wantedItems.slice(0, 0);

        return (
            <section className="ho-WantedItemsComponent">
                <h3>
                    Wanted Items
                    <Popover
                        content={<Help />}
                        trigger="click"
                        visible={this.state.helpVisible}
                        onVisibleChange={this.onHelpVisibleChange}
                    >
                        <Button icon="info-circle" type="link" />
                    </Popover>
                </h3>
                <div className="ho-WantedItemsComponent-top-bar">
                    <BigSelect
                        placeholder="Add an item"
                        value={undefined}
                        style={{ width: 200 }}
                        options={huntOptimizerStore.huntableItems.map(itemKind => ({
                            label: itemKind.name,
                            value: itemKind.id
                        }))}
                        onChange={this.addWanted}
                    />
                    <Button
                        onClick={huntOptimizerStore.optimize}
                        style={{ marginLeft: 10 }}
                    >
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
                                rowCount={huntOptimizerStore.wantedItems.length}
                                rowGetter={({ index }) => huntOptimizerStore.wantedItems[index]}
                                noRowsRenderer={this.noRowsRenderer}
                            >
                                <Column
                                    label="Amount"
                                    dataKey="amount"
                                    width={70}
                                    cellRenderer={({ rowData }) =>
                                        <WantedAmountCell wantedItem={rowData} />
                                    }
                                />
                                <Column
                                    label="Item"
                                    dataKey="item"
                                    width={150}
                                    flexGrow={1}
                                    cellDataGetter={({ rowData }) =>
                                        (rowData as WantedItem).itemKind.name
                                    }
                                />
                                <Column
                                    dataKey="remove"
                                    width={30}
                                    cellRenderer={this.tableRemoveCellRenderer}
                                />
                            </Table>
                        )}
                    </AutoSizer>
                </div>
            </section>
        );
    }

    private addWanted = (selected: any) => {
        if (selected) {
            let added = huntOptimizerStore.wantedItems.find(w => w.itemKind.id === selected.value);

            if (!added) {
                const itemKind = itemKindStores.current.value.getById(selected.value)!;
                huntOptimizerStore.wantedItems.push(new WantedItem(itemKind, 1));
            }
        }
    }

    private removeWanted = (wanted: WantedItem) => () => {
        const i = huntOptimizerStore.wantedItems.findIndex(w => w === wanted);

        if (i !== -1) {
            huntOptimizerStore.wantedItems.splice(i, 1);
        }
    }

    private tableRemoveCellRenderer: TableCellRenderer = ({ rowData }) => {
        return <Button type="link" icon="delete" onClick={this.removeWanted(rowData)} />;
    }

    private noRowsRenderer = () => {
        return (
            <div className="ho-WantedItemsComponent-no-rows">
                <p>
                    Add some items with the above drop down and click "Optimize" to see the result on the right.
                </p>
            </div>
        );
    }

    private onHelpVisibleChange = (visible: boolean) => {
        this.setState({ helpVisible: visible });
    }
}

function Help() {
    return (
        <div className="ho-WantedItemsComponent-help">
            <p>
                Add some items with the drop down and click "Optimize" to see the optimal set of method/difficulty/section ID combinations on the right.
            </p>
            <p>
                At the moment a method is simply a quest run-through. Partial quest run-throughs are coming. View the list of methods on the "Methods" tab. Each method takes a certain amount of time, which affects the optimization result. Make sure the times are correct for you (at the moment times can't be changed, but this feature is coming).
            </p>
            <p>
                Only enemy drops are considered. Box drops are coming.
            </p>
            <p>
                The optimal result is calculated using linear optimization. The optimizer takes rare enemies and the fact that pan arms can be split in two into account.
            </p>
        </div>
    )
}

@observer
class WantedAmountCell extends React.Component<{ wantedItem: WantedItem }> {
    render() {
        const wanted = this.props.wantedItem;

        return (
            <InputNumber
                min={0}
                max={10}
                value={wanted.amount}
                onChange={this.wantedAmountChanged}
                size="small"
                style={{ width: '100%' }}
            />
        );
    }

    private wantedAmountChanged = (value?: number) => {
        if (value != null && value >= 0) {
            this.props.wantedItem.amount = value;
        }
    }
}
