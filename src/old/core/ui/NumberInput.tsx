import * as React from "react";
import { Component, ReactNode, FocusEvent } from "react";
import { InputNumber } from "antd";

export class NumberInput extends Component<{
    value: number;
    min?: number;
    max?: number;
    on_change?: (new_value?: number) => void;
    on_blur?: (e: FocusEvent<HTMLInputElement>) => void;
}> {
    render(): ReactNode {
        return (
            <InputNumber
                value={this.props.value}
                min={this.props.min}
                max={this.props.max}
                onChange={this.props.on_change}
                onBlur={this.props.on_blur}
                size="small"
            />
        );
    }
}
