import * as React from "react";
import { Component, ReactNode } from "react";
import { observer } from "mobx-react";
import {
    object_data,
    OBJECT_TYPES,
    ObjectType,
} from "../../core/data_formats/parsing/quest/object_types";

const drag_helper = document.createElement("div");
drag_helper.id = "drag_helper";
drag_helper.style.width = "100px";
drag_helper.style.height = "100px";
drag_helper.style.position = "fixed";
drag_helper.style.top = "-200px";
document.body.append(drag_helper);

@observer
export class AddObjectComponent extends Component {
    render(): ReactNode {
        return (
            <div>
                {OBJECT_TYPES.map(type => (
                    <ObjectComponent key={type} object_type={type} />
                ))}
            </div>
        );
    }
}

class ObjectComponent extends Component<{ object_type: ObjectType }> {
    render(): ReactNode {
        return (
            <div
                style={{
                    width: 100,
                    height: 100,
                    color: "black",
                    backgroundColor: "#ffff00",
                }}
            >
                {object_data(this.props.object_type).name}
            </div>
        );
    }
}
