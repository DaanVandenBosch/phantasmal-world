import React, { ReactNode, Component } from "react";
import { SkinnedMesh } from "three";
import { get_model_renderer } from "../../rendering/ModelRenderer";

type Props = {
    model?: SkinnedMesh;
};

export class RendererComponent extends Component<Props> {
    private renderer = get_model_renderer();

    render(): ReactNode {
        return <div style={{ overflow: "hidden" }} ref={this.modifyDom} />;
    }

    componentDidMount(): void {
        window.addEventListener("resize", this.onResize);
    }

    componentWillUnmount(): void {
        window.removeEventListener("resize", this.onResize);
    }

    componentWillReceiveProps({ model }: Props): void {
        this.renderer.set_model(model);
    }

    shouldComponentUpdate(): boolean {
        return false;
    }

    private modifyDom = (div: HTMLDivElement | null) => {
        if (div) {
            this.renderer.set_size(div.clientWidth, div.clientHeight);
            div.appendChild(this.renderer.dom_element);
        }
    };

    private onResize = () => {
        const wrapper_div = this.renderer.dom_element.parentNode as HTMLDivElement;
        this.renderer.set_size(wrapper_div.clientWidth, wrapper_div.clientHeight);
    };
}
