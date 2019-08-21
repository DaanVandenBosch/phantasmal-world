import * as React from "react";
import { ChangeEvent, Component, FocusEvent, ReactNode } from "react";
import { Input } from "antd";

export class TextArea extends Component<{
    value: string;
    max_length: number;
    rows: number;
    on_change?: (e: ChangeEvent<HTMLTextAreaElement>) => void;
    on_blur?: (e: FocusEvent<HTMLTextAreaElement>) => void;
}> {
    render(): ReactNode {
        return (
            <Input.TextArea
                value={this.props.value}
                maxLength={this.props.max_length}
                rows={this.props.rows}
                onChange={this.props.on_change}
                onBlur={this.props.on_blur}
            />
        );
    }
}
