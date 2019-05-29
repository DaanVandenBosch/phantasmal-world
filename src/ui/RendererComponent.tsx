import React from 'react';
import { Object3D } from 'three';
import { Area, Quest } from '../domain';
import { Renderer } from '../rendering/Renderer';

interface Props {
    quest?: Quest;
    area?: Area;
    model?: Object3D;
}

export class RendererComponent extends React.Component<Props> {
    private renderer = new Renderer();

    render() {
        return <div style={{ overflow: 'hidden' }} ref={this.modifyDom} />;
    }

    componentDidMount() {
        window.addEventListener('resize', this.onResize);
    }

    componentWillUnmount() {
        window.removeEventListener('resize', this.onResize);
    }

    componentWillReceiveProps({ quest, area, model }: Props) {
        if (model) {
            this.renderer.setModel(model);
        } else {
            this.renderer.setQuestAndArea(quest, area);
        }
    }

    shouldComponentUpdate() {
        return false;
    }

    private modifyDom = (div: HTMLDivElement) => {
        this.renderer.setSize(div.clientWidth, div.clientHeight);
        div.appendChild(this.renderer.domElement);
    }

    private onResize = () => {
        const wrapperDiv = this.renderer.domElement.parentNode as HTMLDivElement;
        this.renderer.setSize(wrapperDiv.clientWidth, wrapperDiv.clientHeight);
    }
}
