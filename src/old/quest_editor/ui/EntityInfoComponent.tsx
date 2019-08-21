import { InputNumber } from "antd";
import { autorun, IReactionDisposer } from "mobx";
import { observer } from "mobx-react";
import React, { Component, PureComponent, ReactNode } from "react";
import { Vec3 } from "../../../core/data_formats/vector";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { DisabledTextComponent } from "../../core/ui/DisabledTextComponent";
import styles from "./EntityInfoComponent.css";
import { entity_data, entity_type_to_string } from "../../../core/data_formats/parsing/quest/entities";
import { ObservableQuestEntity, ObservableQuestNpc } from "../domain/observable_quest_entities";

@observer
export class EntityInfoComponent extends Component {
    render(): ReactNode {
        const entity = quest_editor_store.selected_entity;
        let body: ReactNode;

        if (entity) {
            const section_id = entity.section ? entity.section.id : entity.section_id;

            body = (
                <table className={styles.table}>
                    <tbody>
                        <tr>
                            <th>{entity instanceof ObservableQuestNpc ? "NPC" : "Object"}:</th>
                            <td>{entity_data(entity.type).name}</td>
                        </tr>
                        <tr>
                            <th>Section:</th>
                            <td>{section_id}</td>
                        </tr>
                        <tr>
                            <th colSpan={2}>Section position:</th>
                        </tr>
                        <CoordRow entity={entity} position_type="position" coord="x" />
                        <CoordRow entity={entity} position_type="position" coord="y" />
                        <CoordRow entity={entity} position_type="position" coord="z" />
                        <tr>
                            <th colSpan={2}>World position:</th>
                        </tr>
                        <CoordRow entity={entity} position_type="world_position" coord="x" />
                        <CoordRow entity={entity} position_type="world_position" coord="y" />
                        <CoordRow entity={entity} position_type="world_position" coord="z" />
                    </tbody>
                </table>
            );
        } else {
            body = <DisabledTextComponent>No entity selected.</DisabledTextComponent>;
        }

        return (
            <div className={styles.main} tabIndex={-1}>
                {body}
            </div>
        );
    }
}

type CoordProps = {
    entity: ObservableQuestEntity;
    position_type: "position" | "world_position";
    coord: "x" | "y" | "z";
};

class CoordRow extends PureComponent<CoordProps> {
    render(): ReactNode {
        return (
            <tr>
                <th className={styles.coord_label}>{this.props.coord.toUpperCase()}:</th>
                <td>
                    <CoordInput {...this.props} />
                </td>
            </tr>
        );
    }
}

class CoordInput extends Component<CoordProps, { value: number; initial_position: Vec3 }> {
    private disposer?: IReactionDisposer;

    state = { value: 0, initial_position: new Vec3(0, 0, 0) };

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
                className={styles.coord}
                onFocus={this.focus}
                onBlur={this.blur}
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
                name: `${entity_type_to_string(this.props.entity.type)}.${
                    this.props.position_type
                }.${this.props.coord} changed`,
                delay: 50,
            },
        );
    }

    private focus = () => {
        this.setState({ initial_position: this.props.entity.world_position });
    };

    private blur = () => {
        if (!this.state.initial_position.equals(this.props.entity.world_position)) {
            quest_editor_store.push_entity_move_action(
                this.props.entity,
                this.state.initial_position,
                this.props.entity.world_position,
            );
        }
    };

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
