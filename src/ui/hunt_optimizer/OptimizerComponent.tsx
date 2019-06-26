import React from "react";
import { WantedItemsComponent } from "./WantedItemsComponent";
import { OptimizationResultComponent } from "./OptimizationResultComponent";
import "./OptimizerComponent.css";

export function OptimizerComponent() {
    return (
        <section className="ho-OptimizerComponent">
            <WantedItemsComponent />
            <OptimizationResultComponent />
        </section>
    );
}
