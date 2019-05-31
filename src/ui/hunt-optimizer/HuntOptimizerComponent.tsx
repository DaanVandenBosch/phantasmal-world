import { Select, Table, Button } from "antd";
import { observable } from "mobx";
import { observer } from "mobx-react";
import React from "react";
import { loadItems } from "../../actions/items";
import { Item } from "../../domain";
import { itemStore } from "../../stores/ItemStore";
import './HuntOptimizerComponent.css';

export function HuntOptimizerComponent() {
    return (
        <section className="HuntOptimizerComponent">
            <WantedItemsComponent />
        </section>
    );
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
class WantedItemsComponent extends React.Component {
    @observable private wantedItems: Array<WantedItem> = [];

    componentDidMount() {
        loadItems('ephinea');
    }

    render() {
        // Make sure render is called on updates.
        this.wantedItems.slice(0, 0);

        return (
            <section className="HuntOptimizerComponent-wanted-items">
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
                    size="small"
                    dataSource={this.wantedItems}
                    rowKey={wanted => wanted.item.name}
                    pagination={false}
                >
                    <Table.Column title="Amount" dataIndex="amount" />
                    <Table.Column title="Item" dataIndex="item.name" />
                    <Table.Column
                        render={() => (
                            <Button type="link" icon="delete" />
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
    }
}