import { InputNumber } from "antd";
import { autorun, IReactionDisposer } from "mobx";
import { observer } from "mobx-react";
import React, { Component, PureComponent, ReactNode } from "react";
import { QuestEntity, QuestNpc, QuestObject } from "../../domain";
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

type CoordProps = {
    entity: QuestEntity;
    position_type: "position" | "section_position";
    coord: "x" | "y" | "z";
};

class CoordRow extends PureComponent<CoordProps> {
    render(): ReactNode {
        return (
            <tr>
                <td>{this.props.coord.toUpperCase()}: </td>
                <td>
                    <CoordInput {...this.props} />
                </td>
            </tr>
        );
    }
}

class CoordInput extends Component<CoordProps, { value: number }> {
    private disposer?: IReactionDisposer;

    state = { value: 0 };

    componentDidMount(): void {
        this.start_observing();
    }

    componentWillUnmount(): void {
        if (this.disposer) this.disposer();
    }

    componentDidUpdate(prev_props: CoordProps): void {
        if (this.props.entity !== prev_props.entity) {
            this.start_observing();
        }
    }

    render(): ReactNode {
        return (
            <InputNumber
                value={this.state.value}
                size="small"
                precision={3}
                className="EntityInfoComponent-coord"
                onChange={this.changed}
            />
        );
    }

    private start_observing(): void {
        if (this.disposer) this.disposer();

        this.disposer = autorun(
            () => {
                this.setState({
                    value: this.props.entity[this.props.position_type][this.props.coord],
                });
            },
            {
                name: `${this.props.entity.type.code}.${this.props.position_type}.${this.props.coord} changed`,
                delay: 50,
            }
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
