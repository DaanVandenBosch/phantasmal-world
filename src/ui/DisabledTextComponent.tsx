import React, { Component, ReactNode } from "react";
import "./DisabledTextComponent.less";

export class DisabledTextComponent extends Component<{ children: string }> {
    render(): ReactNode {
        return <div className="DisabledTextComponent">{this.props.children}</div>;
    }
}
