import React from 'react';
import { Area, Quest } from '../../domain';
import { get_quest_renderer } from '../../rendering/QuestRenderer';

type Props = {
    quest?: Quest,
    area?: Area,
}

export class RendererComponent extends React.Component<Props> {
    private renderer = get_quest_renderer();

    render() {
        return <div style={{ overflow: 'hidden' }} ref={this.modifyDom} />;
    }

    componentDidMount() {
        window.addEventListener('resize', this.onResize);
    }

    componentWillUnmount() {
        window.removeEventListener('resize', this.onResize);
    }

    componentWillReceiveProps({ quest, area }: Props) {
        this.renderer.set_quest_and_area(quest, area);
    }

    shouldComponentUpdate() {
        return false;
    }

    private modifyDom = (div: HTMLDivElement | null) => {
        if (div) {
            this.renderer.set_size(div.clientWidth, div.clientHeight);
            div.appendChild(this.renderer.dom_element);
        }
    }

    private onResize = () => {
        const wrapper_div = this.renderer.dom_element.parentNode as HTMLDivElement;
        this.renderer.set_size(wrapper_div.clientWidth, wrapper_div.clientHeight);
    }
}
