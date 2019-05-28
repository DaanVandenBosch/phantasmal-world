import React from 'react';
import { Object3D } from 'three';
import { entity_selected } from '../actions';
import { Renderer } from '../rendering/Renderer';
import { Area, Quest, VisibleQuestEntity } from '../domain';

interface Props {
    quest?: Quest;
    area?: Area;
    model?: Object3D;
}

export class Area3DComponent extends React.Component<Props> {
    private _renderer: Renderer;

    constructor(props: Props) {
        super(props);

        // _renderer has to be assigned here so that it happens after _on_select is assigned.
        this._renderer = new Renderer({
            on_select: this._on_select
        });
    }

    render() {
        return <div style={{ overflow: 'hidden' }} ref={this._modify_dom} />;
    }

    componentDidMount() {
        window.addEventListener('resize', this._on_resize);
    }

    componentWillUnmount() {
        window.removeEventListener('resize', this._on_resize);
    }

    componentWillReceiveProps({ quest, area, model }: Props) {
        if (model) {
            this._renderer.set_model(model);
        } else {
            this._renderer.set_quest_and_area(quest, area);
        }
    }

    shouldComponentUpdate() {
        return false;
    }

    private _modify_dom = (div: HTMLDivElement) => {
        this._renderer.set_size(div.clientWidth, div.clientHeight);
        div.appendChild(this._renderer.dom_element);
    }

    private _on_select = (entity?: VisibleQuestEntity) => {
        entity_selected(entity);
    }

    private _on_resize = () => {
        const wrapper_div = this._renderer.dom_element.parentNode as HTMLDivElement;
        this._renderer.set_size(wrapper_div.clientWidth, wrapper_div.clientHeight);
    }
}
