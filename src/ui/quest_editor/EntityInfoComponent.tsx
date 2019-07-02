import { InputNumber } from "antd";
import { observer } from "mobx-react";
import React, { ReactNode, Component } from "react";
import { QuestNpc, QuestObject, QuestEntity } from "../../domain";
import "./EntityInfoComponent.css";

export type Props = {
    entity?: QuestEntity;
};

@observer
export class EntityInfoComponent extends Component<Props> {
    render(): ReactNode {
        const entity = this.props.entity;

        if (entity) {
            const section_id = entity.section ? entity.section.id : entity.section_id;
            let name = null;

            if (entity instanceof QuestObject) {
                name = (
                    <tr>
                        <td>Object: </td>
                        <td colSpan={2}>{entity.type.name}</td>
                    </tr>
                );
            } else if (entity instanceof QuestNpc) {
                name = (
                    <tr>
                        <td>NPC: </td>
                        <td>{entity.type.name}</td>
                    </tr>
                );
            }

            return (
                <div className="EntityInfoComponent-container">
                    <table className="EntityInfoComponent-table">
                        <tbody>
                            {name}
                            <tr>
                                <td>Section: </td>
                                <td>{section_id}</td>
                            </tr>
                            <tr>
                                <td colSpan={2}>World position: </td>
                            </tr>
                            <tr>
                                <td colSpan={2}>
                                    <table>
                                        <tbody>
                                            <CoordRow
                                                entity={entity}
                                                position_type="position"
                                                coord="x"
                                            />
                                            <CoordRow
                                                entity={entity}
                                                position_type="position"
                                                coord="y"
                                            />
                                            <CoordRow
                                                entity={entity}
                                                position_type="position"
                                                coord="z"
                                            />
                                        </tbody>
                                    </table>
                                </td>
                            </tr>
                            <tr>
                                <td colSpan={2}>Section position: </td>
                            </tr>
                            <tr>
                                <td colSpan={2}>
                                    <table>
                                        <tbody>
                                            <CoordRow
                                                entity={entity}
                                                position_type="section_position"
                                                coord="x"
                                            />
                                            <CoordRow
                                                entity={entity}
                                                position_type="section_position"
                                                coord="y"
                                            />
                                            <CoordRow
                                                entity={entity}
                                                position_type="section_position"
                                                coord="z"
                                            />
                                        </tbody>
                                    </table>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            );
        } else {
            return <div className="EntityInfoComponent-container" />;
        }
    }
}

@observer
class CoordRow extends Component<{
    entity: QuestEntity;
    position_type: "position" | "section_position";
    coord: "x" | "y" | "z";
}> {
    render(): ReactNode {
        const entity = this.props.entity;
        const value = entity[this.props.position_type][this.props.coord];
        return (
            <tr>
                <td>{this.props.coord.toUpperCase()}: </td>
                <td>
                    <InputNumber
                        value={value}
                        size="small"
                        precision={3}
                        className="EntityInfoComponent-coord"
                        onChange={this.changed}
                    />
                </td>
            </tr>
        );
    }

    private changed = (value?: number) => {
        if (value != null) {
            const entity = this.props.entity;
            const pos_type = this.props.position_type;
            const pos = entity[pos_type].clone();
            pos[this.props.coord] = value;
            entity[pos_type] = pos;
        }
    };
}
