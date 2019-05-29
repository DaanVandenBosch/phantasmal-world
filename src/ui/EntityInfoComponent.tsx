import { NumericInput } from '@blueprintjs/core';
import { observer } from 'mobx-react';
import React from 'react';
import { QuestNpc, QuestObject, VisibleQuestEntity } from '../domain';
import './EntityInfoComponent.css';

interface Props {
    entity?: VisibleQuestEntity
}

@observer
export class EntityInfoComponent extends React.Component<Props, any> {
    state = {
        position: {
            x: null,
            y: null,
            z: null,
        },
        sectionPosition: {
            x: null,
            y: null,
            z: null,
        }
    };

    componentWillReceiveProps({ entity }: Props) {
        if (this.props.entity !== entity) {
            this.clearPositionState();
        }
    }

    render() {
        const entity = this.props.entity;

        if (entity) {
            const sectionId = entity.section ? entity.section.id : entity.sectionId;
            let name = null;

            if (entity instanceof QuestObject) {
                name = (
                    <tr>
                        <td>Object: </td><td colSpan={2}>{entity.type.name}</td>
                    </tr>
                );
            } else if (entity instanceof QuestNpc) {
                name = (
                    <tr>
                        <td>NPC: </td><td>{entity.type.name}</td>
                    </tr>
                );
            }

            return (
                <div className="EntityInfoComponent-container">
                    <table className="EntityInfoComponent-table">
                        <tbody>
                            {name}
                            <tr>
                                <td>Section: </td><td>{sectionId}</td>
                            </tr>
                            <tr>
                                <td colSpan={2}>World position: </td>
                            </tr>
                            <tr>
                                <td colSpan={2}>
                                    <table>
                                        <tbody>
                                            {this.coordRow('position', 'x')}
                                            {this.coordRow('position', 'y')}
                                            {this.coordRow('position', 'z')}
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
                                            {this.coordRow('sectionPosition', 'x')}
                                            {this.coordRow('sectionPosition', 'y')}
                                            {this.coordRow('sectionPosition', 'z')}
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

    private coordRow(posType: string, coord: string) {
        if (this.props.entity) {
            const entity = this.props.entity;
            const valueStr = (this.state as any)[posType][coord];
            const value = valueStr
                ? valueStr
                // Do multiplication, rounding, division and || with zero to avoid numbers close to zero flickering between 0 and -0.
                : (Math.round((entity as any)[posType][coord] * 10000) / 10000 || 0).toFixed(4);
            return (
                <tr>
                    <td>{coord.toUpperCase()}: </td>
                    <td>
                        <NumericInput
                            value={value}
                            className="EntityInfoComponent-coord"
                            fill={true}
                            buttonPosition="none"
                            onValueChange={(this.posChange as any)[posType][coord]}
                            onBlur={this.coordInputBlurred} />
                    </td>
                </tr>
            );
        } else {
            return null;
        }
    }

    private posChange = {
        position: {
            x: (value: number, valueStr: string) => {
                this.posChanged('position', 'x', value, valueStr);
            },
            y: (value: number, valueStr: string) => {
                this.posChanged('position', 'y', value, valueStr);
            },
            z: (value: number, valueStr: string) => {
                this.posChanged('position', 'z', value, valueStr);
            }
        },
        sectionPosition: {
            x: (value: number, valueStr: string) => {
                this.posChanged('sectionPosition', 'x', value, valueStr);
            },
            y: (value: number, valueStr: string) => {
                this.posChanged('sectionPosition', 'y', value, valueStr);
            },
            z: (value: number, valueStr: string) => {
                this.posChanged('sectionPosition', 'z', value, valueStr);
            }
        }
    };

    private posChanged(posType: string, coord: string, value: number, valueStr: string) {
        if (!isNaN(value)) {
            const entity = this.props.entity as any;

            if (entity) {
                const v = entity[posType].clone();
                v[coord] = value;
                entity[posType] = v;
            }
        }

        this.setState({
            [posType]: {
                [coord]: valueStr
            }
        });
    }

    private coordInputBlurred = () => {
        this.clearPositionState();
    }

    private clearPositionState() {
        this.setState({
            position: {
                x: null,
                y: null,
                z: null,
            },
            sectionPosition: {
                x: null,
                y: null,
                z: null,
            }
        });
    }
}
