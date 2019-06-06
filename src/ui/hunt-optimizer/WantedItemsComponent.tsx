import { Button, InputNumber, Select } from "antd";
import { observer } from "mobx-react";
import React from "react";
import { AutoSizer, Column, Table, TableCellRenderer } from "react-virtualized";
import { huntOptimizerStore, WantedItem } from "../../stores/HuntOptimizerStore";
import { itemStore } from "../../stores/ItemStore";
import './WantedItemsComponent.css';

@observer
export class WantedItemsComponent extends React.Component {
    render() {
        // Make sure render is called on updates.
        huntOptimizerStore.wantedItems.slice(0, 0);

        return (
            <section className="ho-WantedItemsComponent">
                <h3>Wanted Items</h3>
                <div>
                    <Select
                        value={undefined}
                        showSearch
                        placeholder="Add an item"
                        optionFilterProp="children"
                        style={{ width: 200 }}
                        filterOption
                        onChange={this.addWanted}
                    >
                        {itemStore.items.current.value.map(item => (
                            <Select.Option key={item.name}>
                                {item.name}
                            </Select.Option>
                        ))}
                    </Select>
                    <Button onClick={huntOptimizerStore.optimize}>Optimize</Button>
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
                                    cellDataGetter={({ rowData }) => rowData.item.name}
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

    private addWanted = (itemName: string) => {
        let added = huntOptimizerStore.wantedItems.find(w => w.item.name === itemName);

        if (!added) {
            const item = itemStore.items.current.value.find(i => i.name === itemName)!;
            huntOptimizerStore.wantedItems.push(new WantedItem(item, 1));
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
}

@observer
class WantedAmountCell extends React.Component<{ wantedItem: WantedItem }> {
    render() {
        const wanted = this.props.wantedItem;

        return (
            <InputNumber
                min={1}
                max={10}
                value={wanted.amount}
                onChange={this.wantedAmountChanged}
                size="small"
                style={{ width: '100%' }}
            />
        );
    }

    private wantedAmountChanged = (value?: number) => {
        if (value && value >= 1) {
            this.props.wantedItem.amount = value;
        }
    }
}
