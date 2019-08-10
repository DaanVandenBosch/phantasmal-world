import React, { Component, ReactNode } from "react";
import { model_viewer_store } from "../../stores/ModelViewerStore";
import styles from "./AnimationSelectionComponent.css";
import { observer } from "mobx-react";

@observer
export class AnimationSelectionComponent extends Component {
    render(): ReactNode {
        return (
            <section className={styles.main}>
                <ul>
                    {model_viewer_store.animations.map(animation => {
                        const selected =
                            model_viewer_store.animation &&
                            model_viewer_store.animation.player_animation &&
                            model_viewer_store.animation.player_animation.id === animation.id;

                        return (
                            <li
                                key={animation.id}
                                className={selected ? styles.selected : undefined}
                                onClick={() => model_viewer_store.load_animation(animation)}
                            >
                                {animation.name}
                            </li>
                        );
                    })}
                </ul>
            </section>
        );
    }
}
