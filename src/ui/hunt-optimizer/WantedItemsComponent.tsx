import { Button, InputNumber, Select, Table } from "antd";
import { observer } from "mobx-react";
import React from "react";
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
                <h2>Wanted Items</h2>
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
                <Table
                    className="ho-WantedItemsComponent-table"
                    size="small"
                    dataSource={huntOptimizerStore.wantedItems}
                    rowKey={wanted => wanted.item.name}
                    pagination={false}
                >
                    <Table.Column<WantedItem>
                        title="Amount"
                        dataIndex="amount"
                        render={(_, wanted) => (
                            <WantedAmountCell wantedItem={wanted} />
                        )}
                    />
                    <Table.Column title="Item" dataIndex="item.name" />
                    <Table.Column<WantedItem>
                        render={(_, wanted) => (
                            <Button type="link" icon="delete" onClick={this.removeWanted(wanted)} />
                        )}
                    />
                </Table>
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
}

@observer
class WantedAmountCell extends React.Component<{ wantedItem: WantedItem }> {
    render() {
        const wanted = this.props.wantedItem;

        return (
            <InputNumber
                min={1}
                value={wanted.amount}
                onChange={this.wantedAmountChanged}
            />
        );
    }

    private wantedAmountChanged = (value?: number) => {
        if (value && value >= 1) {
            this.props.wantedItem.amount = value;
        }
    }
}
