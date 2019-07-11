import React, { Component, ReactNode } from "react";
import { Renderer } from "../rendering/Renderer";
import "./RendererComponent.less";
import { Camera } from "three";

export class RendererComponent extends Component<{
    renderer: Renderer<Camera>;
    className?: string;
}> {
    render(): ReactNode {
        let className = "RendererComponent";
        if (this.props.className) className += " " + this.props.className;

        return <div className={className} ref={this.modifyDom} />;
    }

    componentDidMount(): void {
        window.addEventListener("resize", this.onResize);
    }

    componentWillUnmount(): void {
        window.removeEventListener("resize", this.onResize);
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
