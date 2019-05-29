import React from 'react';
import { Object3D } from 'three';
import { entitySelected } from '../actions';
import { Renderer } from '../rendering/Renderer';
import { Area, Quest, VisibleQuestEntity } from '../domain';

interface Props {
    quest?: Quest;
    area?: Area;
    model?: Object3D;
}

export class Area3DComponent extends React.Component<Props> {
    private renderer: Renderer;

    constructor(props: Props) {
        super(props);

        // renderer has to be assigned here so that it happens after onSelect is assigned.
        this.renderer = new Renderer({
            onSelect: this.onSelect
        });
    }

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

    private onSelect = (entity?: VisibleQuestEntity) => {
        entitySelected(entity);
    }

    private onResize = () => {
        const wrapperDiv = this.renderer.domElement.parentNode as HTMLDivElement;
        this.renderer.setSize(wrapperDiv.clientWidth, wrapperDiv.clientHeight);
    }
}
