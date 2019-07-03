import React, { Component, ReactNode } from "react";
import { model_viewer_store } from "../../stores/ModelViewerStore";
import "./AnimationSelectionComponent.less";

export class AnimationSelectionComponent extends Component {
    render(): ReactNode {
        return (
            <section className="mv-AnimationSelectionComponent">
                <ul>
                    {model_viewer_store.animations.map(animation => (
                        <li
                            key={animation.id}
                            onClick={() => model_viewer_store.load_animation(animation)}
                        >
                            {animation.name}
                        </li>
                    ))}
                </ul>
            </section>
        );
    }
}
