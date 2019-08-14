import * as React from "react";
import { ChangeEvent, Component, FocusEvent, ReactNode } from "react";
import { Input } from "antd";

export class TextInput extends Component<{
    value: string;
    max_length: number;
    on_change: (e: ChangeEvent<HTMLInputElement>) => void;
    on_blur?: (e: FocusEvent<HTMLInputElement>) => void;
}> {
    render(): ReactNode {
        return (
            <Input
                value={this.props.value}
                maxLength={this.props.max_length}
                onChange={this.props.on_change}
                onBlur={this.props.on_blur}
                size="small"
            />
        );
    }
}
