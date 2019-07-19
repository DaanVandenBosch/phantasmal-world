import React, { Component, ReactNode } from "react";
import { Renderer } from "../rendering/Renderer";
import "./RendererComponent.less";
import { Camera } from "three";

type Props = {
    renderer: Renderer<Camera>;
    debug?: boolean;
    className?: string;
    on_will_unmount?: () => void;
};

export class RendererComponent extends Component<Props> {
    render(): ReactNode {
        let className = "RendererComponent";
        if (this.props.className) className += " " + this.props.className;

        return <div className={className} ref={this.modifyDom} />;
    }

    componentWillReceiveProps(props: Props): void {
        this.props.renderer.debug = !!props.debug;
    }

    componentDidMount(): void {
        window.addEventListener("resize", this.onResize);
    }

    componentWillUnmount(): void {
        window.removeEventListener("resize", this.onResize);
        this.props.on_will_unmount && this.props.on_will_unmount();
    }

    shouldComponentUpdate(): boolean {
        return false;
    }

    private modifyDom = (div: HTMLDivElement | null) => {
        if (div) {
            this.props.renderer.set_size(div.clientWidth, div.clientHeight);
            div.appendChild(this.props.renderer.dom_element);
        }
    };

    private onResize = () => {
        const wrapper_div = this.props.renderer.dom_element.parentNode as HTMLDivElement;
        this.props.renderer.set_size(wrapper_div.clientWidth, wrapper_div.clientHeight);
    };
}
