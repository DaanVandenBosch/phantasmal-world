import { Tabs } from "antd";
import React from "react";
import styles from "./HuntOptimizerComponent.css";
import { MethodsComponent } from "./MethodsComponent";
import { OptimizerComponent } from "./OptimizerComponent";

const TabPane = Tabs.TabPane;

export function HuntOptimizerComponent(): JSX.Element {
    return (
        <section className={styles.main}>
            <Tabs type="card">
                <TabPane tab="Optimize" key="optimize">
                    <OptimizerComponent />
                </TabPane>
                <TabPane tab="Methods" key="methods">
                    <MethodsComponent />
                </TabPane>
            </Tabs>
        </section>
    );
}
