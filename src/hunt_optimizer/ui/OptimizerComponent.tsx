import React from "react";
import { OptimizationResultComponent } from "./OptimizationResultComponent";
import styles from "./OptimizerComponent.css";
import { WantedItemsComponent } from "./WantedItemsComponent";

export function OptimizerComponent(): JSX.Element {
    return (
        <section className={styles.main}>
            <WantedItemsComponent />
            <OptimizationResultComponent />
        </section>
    );
}
