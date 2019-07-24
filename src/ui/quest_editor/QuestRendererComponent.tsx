import { observer } from "mobx-react";
import React, { Component, ReactNode } from "react";
import { AutoSizer } from "react-virtualized";
import { get_quest_renderer } from "../../rendering/QuestRenderer";
import { quest_editor_store } from "../../stores/QuestEditorStore";
import { RendererComponent } from "../RendererComponent";

@observer
export class QuestRendererComponent extends Component {
    render(): ReactNode {
        const debug = quest_editor_store.debug;

        return (
            <AutoSizer>
                {({ width, height }) => (
                    <RendererComponent
                        renderer={get_quest_renderer()}
                        width={width}
                        height={height}
                        debug={debug}
                    />
                )}
            </AutoSizer>
        );
    }
}
