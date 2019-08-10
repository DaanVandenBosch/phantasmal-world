import { Tabs } from "antd";
import React, { Component, ReactNode } from "react";
import { ModelViewerComponent } from "./models/ModelViewerComponent";
import { TextureViewerComponent } from "./textures/TextureViewerComponent";
import styles from "./ViewerComponent.css";

export class ViewerComponent extends Component {
    render(): ReactNode {
        return (
            <section className={styles.main}>
                <Tabs type="card">
                    <Tabs.TabPane tab="Models" key="models">
                        <ModelViewerComponent />
                    </Tabs.TabPane>
                    <Tabs.TabPane tab="Textures" key="textures">
                        <TextureViewerComponent />
                    </Tabs.TabPane>
                </Tabs>
            </section>
        );
    }
}
