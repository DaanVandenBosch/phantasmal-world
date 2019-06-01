import { InputNumber } from 'antd';
import { observer } from 'mobx-react';
import React from 'react';
import { QuestNpc, QuestObject, QuestEntity } from '../../domain';
import './EntityInfoComponent.css';

interface Props {
    entity?: QuestEntity;
}

@observer
export class EntityInfoComponent extends React.Component<Props> {
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
                                            <CoordRow entity={entity} positionType="position" coord="x" />
                                            <CoordRow entity={entity} positionType="position" coord="y" />
                                            <CoordRow entity={entity} positionType="position" coord="z" />
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
                                            <CoordRow entity={entity} positionType="sectionPosition" coord="x" />
                                            <CoordRow entity={entity} positionType="sectionPosition" coord="y" />
                                            <CoordRow entity={entity} positionType="sectionPosition" coord="z" />
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
class CoordRow extends React.Component<{
    entity: QuestEntity,
    positionType: 'position' | 'sectionPosition',
    coord: 'x' | 'y' | 'z'
}> {
    render() {
        const entity = this.props.entity;
        const value = entity[this.props.positionType][this.props.coord];
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
            const posType = this.props.positionType;
            const pos = entity[posType].clone();
            pos[this.props.coord] = value;
            entity[posType] = pos;
        }
    }
}
