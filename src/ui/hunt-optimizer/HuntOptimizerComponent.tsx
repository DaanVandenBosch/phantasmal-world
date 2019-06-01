import React from "react";
import './HuntOptimizerComponent.css';
import { WantedItemsComponent } from "./WantedItemsComponent";
import { loadItems } from "../../actions/items";
import { loadHuntMethods } from "../../actions/huntMethods";

export class HuntOptimizerComponent extends React.Component {
    componentDidMount() {
        loadItems('ephinea');
        loadHuntMethods('ephinea');
    }

    render() {
        return (
            <section className="ho-HuntOptimizerComponent">
                <WantedItemsComponent />
            </section>
        );
    }
}
