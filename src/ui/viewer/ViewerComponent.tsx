import React, { Component, ReactNode } from "react";
import { Tabs } from "antd";
import { ModelViewerComponent } from "./models/ModelViewerComponent";
import "./ViewerComponent.less";
import { TextureViewerComponent } from "./textures/TextureViewerComponent";

export class ViewerComponent extends Component {
    render(): ReactNode {
        return (
            <section className="v-ViewerComponent">
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
