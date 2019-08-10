import { List } from "antd";
import { observer } from "mobx-react";
import React, { Component, ReactNode } from "react";
import { model_viewer_store } from "../../stores/ModelViewerStore";
import styles from "./ModelSelectionComponent.css";

@observer
export class ModelSelectionComponent extends Component {
    render(): ReactNode {
        // Make sure we trigger mobx.
        const current = model_viewer_store.current_player_model;

        return (
            <section className={styles.main}>
                <List
                    itemLayout="horizontal"
                    dataSource={model_viewer_store.models}
                    size="small"
                    renderItem={model => {
                        const selected = current === model;

                        return (
                            <List.Item onClick={() => model_viewer_store.load_model(model)}>
                                <List.Item.Meta
                                    title={
                                        <span
                                            className={
                                                styles.model +
                                                (selected ? ` ${styles.selected}` : "")
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
