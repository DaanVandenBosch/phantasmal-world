import React from "react";
import './HuntOptimizerComponent.css';
import { WantedItemsComponent } from "./WantedItemsComponent";
import { OptimizationResultComponent } from "./OptimizationResultComponent";

export function HuntOptimizerComponent() {
    return (
        <section className="ho-HuntOptimizerComponent">
            <WantedItemsComponent />
            <OptimizationResultComponent />
        </section>
    );
}
