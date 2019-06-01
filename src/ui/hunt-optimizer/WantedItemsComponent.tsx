import { Button, InputNumber, Select, Table } from "antd";
import { observable } from "mobx";
import { observer } from "mobx-react";
import React from "react";
import { Item } from "../../domain";
import { itemStore } from "../../stores/ItemStore";
import './WantedItemsComponent.css';

@observer
export class WantedItemsComponent extends React.Component {
    @observable
    private wantedItems: Array<WantedItem> = [];

    render() {
        // Make sure render is called on updates.
        this.wantedItems.slice(0, 0);

        return (
            <section className="ho-WantedItemsComponent">
                <h2>Wanted Items</h2>
                <Select
                    value={undefined}
                    showSearch
                    placeholder="Add an item"
                    optionFilterProp="children"
                    style={{ width: 200 }}
                    filterOption
                    onChange={this.addWanted}
                >
                    {itemStore.items.map(item => (
                        <Select.Option key={item.name}>
                            {item.name}
                        </Select.Option>
                    ))}
                </Select>
                <Table
                    className="ho-WantedItemsComponent-table"
                    size="small"
                    dataSource={this.wantedItems}
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
        let added = this.wantedItems.find(w => w.item.name === itemName);

        if (!added) {
            const item = itemStore.items.find(i => i.name === itemName)!;
            this.wantedItems.push(new WantedItem(item, 1));
        }
    };

    private removeWanted = (wanted: WantedItem) => () => {
        const i = this.wantedItems.findIndex(w => w === wanted);

        if (i !== -1) {
            this.wantedItems.splice(i, 1);
        }
    };
}

class WantedItem {
    @observable item: Item;
    @observable amount: number;

    constructor(item: Item, amount: number) {
        this.item = item;
        this.amount = amount;
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
