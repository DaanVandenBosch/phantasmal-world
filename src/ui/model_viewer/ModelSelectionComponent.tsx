import { List } from "antd";
import React, { Component, ReactNode } from "react";
import { model_viewer_store } from "../../stores/ModelViewerStore";
import "./ModelSelectionComponent.less";

export class ModelSelectionComponent extends Component {
    render(): ReactNode {
        return (
            <section className="mv-ModelSelectionComponent">
                <List
                    itemLayout="horizontal"
                    dataSource={model_viewer_store.models}
                    size="small"
                    renderItem={model => {
                        const selected = model_viewer_store.current_player_model === model;

                        return (
                            <List.Item onClick={() => model_viewer_store.load_model(model)}>
                                <List.Item.Meta
                                    title={
                                        <span
                                            className={
                                                "mv-ModelSelectionComponent-model" +
                                                (selected ? " selected" : "")
                                            }
                                        >
                                            {model.name}
                                        </span>
                                    }
                                />
                            </List.Item>
                        );
                    }}
                />
            </section>
        );
    }
}
