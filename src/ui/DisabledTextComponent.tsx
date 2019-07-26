import React, { Component, ReactNode } from "react";
import styles from "./DisabledTextComponent.css";

export class DisabledTextComponent extends Component<{ children: string }> {
    render(): ReactNode {
        return <div className={styles.main}>{this.props.children}</div>;
    }
}
