import { QuestEntityModel } from "../model/QuestEntityModel";
import { Euler, Intersection, Mesh, Plane, Quaternion, Raycaster, Vector2, Vector3 } from "three";
import { QuestRenderer } from "./QuestRenderer";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { EntityUserData } from "./conversion/entities";
import { QuestNpcModel } from "../model/QuestNpcModel";
import { AreaUserData } from "./conversion/areas";
import { SectionModel } from "../model/SectionModel";
import { Disposable } from "../../core/observable/Disposable";
import { Disposer } from "../../core/observable/Disposer";
import { EntityType, is_npc_type } from "../../core/data_formats/parsing/quest/entities";
import { npc_data } from "../../core/data_formats/parsing/quest/npc_types";
import {
    add_entity_dnd_listener,
    EntityDragEvent,
    remove_entity_dnd_listener,
} from "../gui/entity_dnd";
import { QuestObjectModel } from "../model/QuestObjectModel";
import { AreaModel } from "../model/AreaModel";
import { QuestModel } from "../model/QuestModel";

const ZERO_VECTOR = Object.freeze(new Vector3(0, 0, 0));
const UP_VECTOR = Object.freeze(new Vector3(0, 1, 0));
const DOWN_VECTOR = Object.freeze(new Vector3(0, -1, 0));
const PI2 = 2 * Math.PI;

const raycaster = new Raycaster();

export class QuestEntityControls implements Disposable {
    private readonly disposer = new Disposer();
    private readonly pointer_position = new Vector2(0, 0);
    private readonly pointer_device_position = new Vector2(0, 0);
    private readonly last_pointer_position = new Vector2(0, 0);
    private moved_since_last_pointer_down = false;
    private state: State;
    private _enabled = true;

    /**
     * Whether entity transformations, deletions, etc. are enabled or not.
     */
    set enabled(enabled: boolean) {
        this._enabled = enabled;
        this.state.cancel();
        this.state = new IdleState(this.renderer, this._enabled);
    }

    constructor(private readonly renderer: QuestRenderer) {
        this.disposer.add(quest_editor_store.selected_entity.observe(this.selected_entity_changed));

        renderer.dom_element.addEventListener("keydown", this.keydown);
        renderer.dom_element.addEventListener("mousedown", this.mousedown);
        renderer.dom_element.addEventListener("mousemove", this.mousemove);
        renderer.dom_element.addEventListener("mouseleave", this.mouseleave);
        add_entity_dnd_listener(renderer.dom_element, "dragenter", this.dragenter);
        add_entity_dnd_listener(renderer.dom_element, "dragover", this.dragover);
        add_entity_dnd_listener(renderer.dom_element, "dragleave", this.dragleave);
        add_entity_dnd_listener(renderer.dom_element, "drop", this.drop);

        this.state = new IdleState(renderer, this._enabled);
    }

    dispose = (): void => {
        this.renderer.dom_element.removeEventListener("keydown", this.keydown);
        this.renderer.dom_element.removeEventListener("mousedown", this.mousedown);
        this.renderer.dom_element.removeEventListener("mousemove", this.mousemove);
        document.removeEventListener("mousemove", this.mousemove);
        document.removeEventListener("mouseup", this.mouseup);
        this.renderer.dom_element.removeEventListener("mouseleave", this.mouseleave);
        remove_entity_dnd_listener(this.renderer.dom_element, "dragenter", this.dragenter);
        remove_entity_dnd_listener(this.renderer.dom_element, "dragover", this.dragover);
        remove_entity_dnd_listener(this.renderer.dom_element, "dragleave", this.dragleave);
        remove_entity_dnd_listener(this.renderer.dom_element, "drop", this.drop);
        this.disposer.dispose();
    };

    private selected_entity_changed = ({ value: entity }: { value?: QuestEntityModel }): void => {
        this.state.cancel();

        if (entity) {
            const mesh = this.renderer.get_entity_mesh(entity);

            // Mesh might not be loaded yet.
            if (mesh) {
                this.renderer.mark_selected(mesh);
            } else {
                this.renderer.unmark_selected();
            }
        } else {
            this.renderer.unmark_selected();
        }
    };

    private keydown = (e: KeyboardEvent): void => {
        this.state = this.state.process_event({
            type: EvtType.KeyDown,
            key: e.key,
        });
    };

    private mousedown = (e: MouseEvent): void => {
        this.process_mouse_event(e);

        this.state = this.state.process_event({
            type: EvtType.MouseDown,
            buttons: e.buttons,
            shift_key: e.shiftKey,
            pointer_device_position: this.pointer_device_position,
            moved_since_last_pointer_down: this.moved_since_last_pointer_down,
            mark_hovered: this.mark_hovered,
        });

        this.renderer.dom_element.removeEventListener("mousemove", this.mousemove);
        document.addEventListener("mousemove", this.mousemove);
        document.addEventListener("mouseup", this.mouseup);
    };

    private mousemove = (e: MouseEvent): void => {
        this.process_mouse_event(e);

        this.state = this.state.process_event({
            type: EvtType.MouseMove,
            buttons: e.buttons,
            shift_key: e.shiftKey,
            pointer_device_position: this.pointer_device_position,
            moved_since_last_pointer_down: this.moved_since_last_pointer_down,
            mark_hovered: this.mark_hovered,
        });
    };

    private mouseup = (e: MouseEvent): void => {
        this.process_mouse_event(e);

        this.state = this.state.process_event({
            type: EvtType.MouseUp,
            buttons: e.buttons,
            shift_key: e.shiftKey,
            pointer_device_position: this.pointer_device_position,
            moved_since_last_pointer_down: this.moved_since_last_pointer_down,
            mark_hovered: this.mark_hovered,
        });

        this.renderer.dom_element.addEventListener("mousemove", this.mousemove);
        document.removeEventListener("mousemove", this.mousemove);
        document.removeEventListener("mouseup", this.mouseup);
    };

    private mouseleave = (e: MouseEvent): void => {
        this.process_mouse_event(e);

        this.state = this.state.process_event({
            type: EvtType.MouseLeave,
            buttons: e.buttons,
            shift_key: e.shiftKey,
            pointer_device_position: this.pointer_device_position,
            moved_since_last_pointer_down: this.moved_since_last_pointer_down,
            mark_hovered: this.mark_hovered,
        });
    };

    private dragenter = (e: EntityDragEvent): void => {
        this.process_mouse_event(e.event);

        this.state = this.state.process_event({
            type: EvtType.EntityDragEnter,
            shift_key: e.event.shiftKey,
            pointer_device_position: this.pointer_device_position,
            entity_type: e.entity_type,
            drag_element: e.drag_element,
            data_transfer: e.event.dataTransfer,
            prevent_default: () => e.event.preventDefault(),
            stop_propagation: () => e.event.stopPropagation(),
        });
    };

    private dragover = (e: EntityDragEvent): void => {
        this.process_mouse_event(e.event);

        this.state = this.state.process_event({
            type: EvtType.EntityDragOver,
            shift_key: e.event.shiftKey,
            pointer_device_position: this.pointer_device_position,
            entity_type: e.entity_type,
            drag_element: e.drag_element,
            data_transfer: e.event.dataTransfer,
            prevent_default: () => e.event.preventDefault(),
            stop_propagation: () => e.event.stopPropagation(),
        });
    };

    private dragleave = (e: EntityDragEvent): void => {
        this.process_mouse_event(e.event);

        this.state = this.state.process_event({
            type: EvtType.EntityDragLeave,
            shift_key: e.event.shiftKey,
            pointer_device_position: this.pointer_device_position,
            entity_type: e.entity_type,
            drag_element: e.drag_element,
            data_transfer: e.event.dataTransfer,
            prevent_default: () => e.event.preventDefault(),
            stop_propagation: () => e.event.stopPropagation(),
        });
    };

    private drop = (e: EntityDragEvent): void => {
        this.process_mouse_event(e.event);
        this.renderer.dom_element.focus();

        this.state = this.state.process_event({
            type: EvtType.EntityDrop,
        });
    };

    private process_mouse_event(e: MouseEvent): void {
        const { left, top } = this.renderer.dom_element.getBoundingClientRect();
        this.pointer_position.set(e.clientX - left, e.clientY - top);
        this.pointer_device_position.copy(this.pointer_position);
        this.renderer.pointer_pos_to_device_coords(this.pointer_device_position);

        if (e.type === "mousedown") {
            this.moved_since_last_pointer_down = false;
        } else if (e.type === "mousemove" || e.type === "mouseup") {
            if (!this.pointer_position.equals(this.last_pointer_position)) {
                this.moved_since_last_pointer_down = true;
            }
        }

        this.last_pointer_position.copy(this.pointer_position);
    }

    private mark_hovered = (mesh?: Mesh): void => {
        this.renderer.mark_hovered(mesh);
    };
}

type Pick = {
    entity: QuestEntityModel;

    mesh: Mesh;

    /**
     * Vector that points from the grabbing point to the model's origin.
     */
    grab_offset: Vector3;

    /**
     * Vector that points from the grabbing point to the terrain point directly under the model's origin.
     */
    drag_adjust: Vector3;
};

enum EvtType {
    KeyDown,
    MouseDown,
    MouseMove,
    MouseUp,
    MouseLeave,
    EntityDragEnter,
    EntityDragOver,
    EntityDragLeave,
    EntityDrop,
}

type Evt = KeyboardEvt | MouseEvt | EntityDragEvt | EntityDropEvt;

type KeyboardEvt = {
    readonly type: EvtType.KeyDown;
    readonly key: string;
};

type MouseEvt = {
    readonly type: EvtType.MouseDown | EvtType.MouseMove | EvtType.MouseUp | EvtType.MouseLeave;
    readonly buttons: number;
    readonly shift_key: boolean;
    readonly pointer_device_position: Vector2;
    readonly moved_since_last_pointer_down: boolean;
    mark_hovered(entity_mesh?: Mesh): void;
};

type EntityDragEvt = {
    readonly type: EvtType.EntityDragEnter | EvtType.EntityDragOver | EvtType.EntityDragLeave;
    readonly shift_key: boolean;
    readonly pointer_device_position: Vector2;
    readonly entity_type: EntityType;
    readonly drag_element: HTMLElement;
    readonly data_transfer: DataTransfer | null;
    stop_propagation(): void;
    prevent_default(): void;
};

type EntityDropEvt = {
    readonly type: EvtType.EntityDrop;
};

interface State {
    process_event(evt: Evt): State;

    /**
     * The state object should stop doing what it's doing and revert to the idle state as soon as
     * possible.
     */
    cancel(): void;
}

class IdleState implements State {
    constructor(private readonly renderer: QuestRenderer, private readonly enabled: boolean) {}

    process_event(evt: Evt): State {
        switch (evt.type) {
            case EvtType.KeyDown: {
                if (this.enabled) {
                    const entity = quest_editor_store.selected_entity.val;

                    if (entity && evt.key === "Delete") {
                        quest_editor_store.remove_entity(entity);
                    }
                }

                return this;
            }

            case EvtType.MouseDown: {
                const pick = this.pick_entity(evt.pointer_device_position);

                if (pick) {
                    if (evt.buttons === 1) {
                        quest_editor_store.set_selected_entity(pick.entity);

                        if (this.enabled) {
                            return new TranslationState(
                                this.renderer,
                                pick.entity,
                                pick.drag_adjust,
                                pick.grab_offset,
                            );
                        }
                    } else if (evt.buttons === 2) {
                        quest_editor_store.set_selected_entity(pick.entity);

                        if (this.enabled) {
                            return new RotationState(
                                this.renderer,
                                pick.entity,
                                pick.mesh,
                                pick.grab_offset,
                            );
                        }
                    }
                }

                return this;
            }

            case EvtType.MouseMove: {
                // User is hovering.
                const pick_result = this.pick_entity(evt.pointer_device_position);
                evt.mark_hovered(pick_result && pick_result.mesh);
                return this;
            }

            case EvtType.MouseUp: {
                // If the user clicks on nothing, deselect the currently selected entity.
                if (
                    !evt.moved_since_last_pointer_down &&
                    !this.pick_entity(evt.pointer_device_position)
                ) {
                    quest_editor_store.set_selected_entity(undefined);
                }

                return this;
            }

            case EvtType.MouseLeave: {
                evt.mark_hovered(undefined);
                return this;
            }

            case EvtType.EntityDragEnter: {
                if (this.enabled) {
                    const area = quest_editor_store.current_area.val;
                    const quest = quest_editor_store.current_quest.val;

                    if (!area || !quest) return this;

                    return new CreationState(this.renderer, evt, quest, area);
                } else {
                    return this;
                }
            }

            default:
                return this;
        }
    }

    cancel(): void {
        // Do nothing.
    }

    /**
     * @param pointer_position - pointer coordinates in normalized device space
     */
    private pick_entity(pointer_position: Vector2): Pick | undefined {
        // Find the nearest object and NPC under the pointer.
        raycaster.setFromCamera(pointer_position, this.renderer.camera);
        const [intersection] = raycaster.intersectObjects(this.renderer.entity_models.children);

        if (!intersection) {
            return undefined;
        }

        const entity = (intersection.object.userData as EntityUserData).entity;
        const grab_offset = intersection.object.position.clone().sub(intersection.point);
        const drag_adjust = grab_offset.clone();

        // Find vertical distance to the ground.
        raycaster.set(intersection.object.position, DOWN_VECTOR);
        const [collision_geom_intersection] = raycaster.intersectObjects(
            this.renderer.collision_geometry.children,
            true,
        );

        if (collision_geom_intersection) {
            drag_adjust.y -= collision_geom_intersection.distance;
        }

        return {
            mesh: intersection.object as Mesh,
            entity,
            grab_offset,
            drag_adjust,
        };
    }
}

class TranslationState implements State {
    private readonly initial_section: SectionModel | undefined;
    private readonly initial_position: Vector3;
    private cancelled = false;

    constructor(
        private readonly renderer: QuestRenderer,
        private readonly entity: QuestEntityModel,
        private readonly drag_adjust: Vector3,
        private readonly grab_offset: Vector3,
    ) {
        this.initial_section = entity.section.val;
        this.initial_position = entity.world_position.val;
        this.renderer.controls.enabled = false;
    }

    process_event(evt: Evt): State {
        switch (evt.type) {
            case EvtType.MouseMove: {
                if (this.cancelled) {
                    return new IdleState(this.renderer, true);
                }

                if (evt.moved_since_last_pointer_down) {
                    translate_entity(
                        this.renderer,
                        this.entity,
                        this.drag_adjust,
                        this.grab_offset,
                        evt.pointer_device_position,
                        evt.shift_key,
                    );
                }

                return this;
            }

            case EvtType.MouseUp: {
                this.renderer.controls.enabled = true;

                if (!this.cancelled && evt.moved_since_last_pointer_down) {
                    quest_editor_store.translate_entity(
                        this.entity,
                        this.initial_section,
                        this.entity.section.val,
                        this.initial_position,
                        this.entity.world_position.val,
                        true,
                    );
                }

                return new IdleState(this.renderer, true);
            }

            default:
                return this.cancelled ? new IdleState(this.renderer, true) : this;
        }
    }

    cancel(): void {
        this.cancelled = true;
        this.renderer.controls.enabled = true;

        if (this.initial_section) {
            this.entity.set_section(this.initial_section);
        }

        this.entity.set_world_position(this.initial_position);
    }
}

// TODO: make entities rotatable with right mouse button.
class RotationState implements State {
    private readonly initial_rotation: Euler;
    private readonly grab_point: Vector3;
    private cancelled = false;

    constructor(
        private readonly renderer: QuestRenderer,
        private readonly entity: QuestEntityModel,
        private readonly mesh: Mesh,
        grab_offset: Vector3,
    ) {
        this.initial_rotation = entity.world_rotation.val;
        this.grab_point = entity.world_position.val.clone().sub(grab_offset);
        this.renderer.controls.enabled = false;
    }

    process_event(evt: Evt): State {
        switch (evt.type) {
            case EvtType.MouseMove: {
                if (this.cancelled) {
                    return new IdleState(this.renderer, true);
                }

                if (evt.moved_since_last_pointer_down) {
                    rotate_entity(
                        this.renderer,
                        this.entity,
                        this.mesh.quaternion,
                        this.initial_rotation,
                        this.grab_point,
                        evt.pointer_device_position,
                    );
                }

                return this;
            }

            case EvtType.MouseUp: {
                this.renderer.controls.enabled = true;

                if (!this.cancelled && evt.moved_since_last_pointer_down) {
                    quest_editor_store.rotate_entity(
                        this.entity,
                        this.initial_rotation,
                        this.entity.world_rotation.val,
                        true,
                    );
                }

                return new IdleState(this.renderer, true);
            }

            default:
                return this.cancelled ? new IdleState(this.renderer, true) : this;
        }
    }

    cancel(): void {
        this.cancelled = true;
        this.renderer.controls.enabled = true;

        this.entity.set_world_rotation(this.initial_rotation);
    }
}

class CreationState implements State {
    private readonly renderer: QuestRenderer;
    private readonly entity: QuestEntityModel;
    private readonly drag_adjust = new Vector3(0, 0, 0);
    private cancelled = false;

    constructor(renderer: QuestRenderer, evt: EntityDragEvt, quest: QuestModel, area: AreaModel) {
        this.renderer = renderer;

        evt.drag_element.style.display = "none";

        if (evt.data_transfer) {
            evt.data_transfer.dropEffect = "copy";
        }

        if (is_npc_type(evt.entity_type)) {
            const data = npc_data(evt.entity_type);

            this.entity = new QuestNpcModel(
                evt.entity_type,
                data.pso_type_id!,
                0,
                0,
                data.pso_roaming!,
                area.id,
                0,
                new Vector3(0, 0, 0),
                new Euler(0, 0, 0, "ZXY"),
                new Vector3(1, 1, 1),
                // TODO: do the following values make sense?
                [
                    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                    [0, 0, 0, 0, 0, 0],
                    [0, 0, 0, 0],
                ],
            );
        } else {
            this.entity = new QuestObjectModel(
                evt.entity_type,
                0,
                0,
                area.id,
                0,
                new Vector3(0, 0, 0),
                new Euler(0, 0, 0, "ZXY"),
                // TODO: which default properties?
                new Map(),
                // TODO: do the following values make sense?
                [
                    [0, 0, 0, 0, 0, 0],
                    [0, 0],
                ],
            );
        }

        translate_entity_horizontally(
            this.renderer,
            this.entity,
            this.drag_adjust,
            ZERO_VECTOR,
            evt.pointer_device_position,
        );
        quest.add_entity(this.entity);

        quest_editor_store.set_selected_entity(this.entity);
    }

    process_event(evt: Evt): State {
        switch (evt.type) {
            case EvtType.EntityDragOver: {
                if (this.cancelled) {
                    evt.drag_element.style.display = "flex";

                    if (evt.data_transfer) {
                        evt.data_transfer.dropEffect = "none";
                    }

                    return new IdleState(this.renderer, true);
                }

                evt.stop_propagation();
                evt.prevent_default();

                if (evt.data_transfer) {
                    evt.data_transfer.dropEffect = "copy";
                }

                // Only translation is possible while dragging in a new entity.
                translate_entity(
                    this.renderer,
                    this.entity,
                    this.drag_adjust,
                    ZERO_VECTOR,
                    evt.pointer_device_position,
                    evt.shift_key,
                );

                return this;
            }

            case EvtType.EntityDragLeave: {
                evt.drag_element.style.display = "flex";

                const quest = quest_editor_store.current_quest.val;

                if (quest) {
                    quest.remove_entity(this.entity);
                }

                return new IdleState(this.renderer, true);
            }

            case EvtType.EntityDrop: {
                if (!this.cancelled) {
                    quest_editor_store.push_create_entity_action(this.entity);
                }

                return new IdleState(this.renderer, true);
            }

            default:
                return this;
        }
    }

    cancel(): void {
        this.cancelled = true;

        const quest = quest_editor_store.current_quest.val;

        if (quest) {
            quest.remove_entity(this.entity);
        }
    }
}

function translate_entity(
    renderer: QuestRenderer,
    entity: QuestEntityModel,
    drag_adjust: Vector3,
    grab_offset: Vector3,
    pointer_device_position: Vector2,
    vertically: boolean,
): void {
    if (vertically) {
        translate_entity_vertically(
            renderer,
            entity,
            drag_adjust,
            grab_offset,
            pointer_device_position,
        );
    } else {
        translate_entity_horizontally(
            renderer,
            entity,
            drag_adjust,
            grab_offset,
            pointer_device_position,
        );
    }
}

/**
 * If the drag-adjusted pointer is over the ground, translate an entity horizontally across the
 * ground. Otherwise translate the entity over the horizontal plane that intersects its origin.
 */
const translate_entity_horizontally = (() => {
    const plane = new Plane();
    const pointer_pos_on_plane = new Vector3();

    return (
        renderer: QuestRenderer,
        entity: QuestEntityModel,
        drag_adjust: Vector3,
        grab_offset: Vector3,
        pointer_device_position: Vector2,
    ): void => {
        // Cast ray adjusted for dragging entities.
        const { intersection, section } = pick_ground(
            renderer,
            pointer_device_position,
            drag_adjust,
        );

        if (intersection) {
            if (section) {
                const old_rotation = entity.world_rotation.val;

                entity.set_section(section);

                // Make sure entity's world-relative orientation stays constant when translating manually.
                entity.set_world_rotation(old_rotation);
            }

            entity.set_world_position(
                new Vector3(
                    intersection.point.x,
                    intersection.point.y + grab_offset.y - drag_adjust.y,
                    intersection.point.z,
                ),
            );
        } else {
            // If the pointer is not over the ground, we translate the entity across the horizontal
            // plane in which the entity's origin lies.
            raycaster.setFromCamera(pointer_device_position, renderer.camera);
            plane.set(UP_VECTOR, -entity.world_position.val.y + grab_offset.y);

            if (raycaster.ray.intersectPlane(plane, pointer_pos_on_plane)) {
                entity.set_world_position(
                    new Vector3(
                        pointer_pos_on_plane.x + grab_offset.x,
                        entity.world_position.val.y,
                        pointer_pos_on_plane.z + grab_offset.z,
                    ),
                );
            }
        }
    };
})();

const translate_entity_vertically = (() => {
    const plane_normal = new Vector3();
    const plane = new Plane();
    const pointer_pos_on_plane = new Vector3();
    const grab_point = new Vector3();

    return (
        renderer: QuestRenderer,
        entity: QuestEntityModel,
        drag_adjust: Vector3,
        grab_offset: Vector3,
        pointer_device_position: Vector2,
    ): void => {
        // Intersect with a plane that's oriented towards the camera and that's coplanar with the
        // point where the entity was grabbed.
        raycaster.setFromCamera(pointer_device_position, renderer.camera);

        renderer.camera.getWorldDirection(plane_normal);
        plane_normal.negate();
        plane_normal.y = 0;
        plane_normal.normalize();

        grab_point.set(
            entity.world_position.val.x,
            entity.world_position.val.y,
            entity.world_position.val.z,
        );
        grab_point.sub(grab_offset);
        plane.setFromNormalAndCoplanarPoint(plane_normal, grab_point);

        if (raycaster.ray.intersectPlane(plane, pointer_pos_on_plane)) {
            const y = pointer_pos_on_plane.y + grab_offset.y;
            const y_delta = y - entity.world_position.val.y;
            drag_adjust.y -= y_delta;
            entity.set_world_position(
                new Vector3(entity.world_position.val.x, y, entity.world_position.val.z),
            );
        }
    };
})();

const rotate_entity = (() => {
    const plane_normal = new Vector3();
    const plane = new Plane();
    const pointer_pos_on_plane = new Vector3();
    const y_intersect = new Vector3();
    const axis_to_grab = new Vector3();
    const axis_to_pointer = new Vector3();

    return (
        renderer: QuestRenderer,
        entity: QuestEntityModel,
        rotation: Quaternion,
        initial_rotation: Euler,
        grab_point: Vector3,
        pointer_device_position: Vector2,
    ): void => {
        // Intersect with a plane that's oriented along the entity's y-axis and that's coplanar with
        // the point where the entity was grabbed.
        plane_normal.copy(UP_VECTOR);
        plane_normal.applyQuaternion(rotation);

        plane.setFromNormalAndCoplanarPoint(plane_normal, grab_point);

        raycaster.setFromCamera(pointer_device_position, renderer.camera);

        if (raycaster.ray.intersectPlane(plane, pointer_pos_on_plane)) {
            plane.projectPoint(entity.world_position.val, y_intersect);

            // Calculate vector from the entity's y-axis to the original grab point.
            axis_to_grab.subVectors(y_intersect, grab_point);

            // Calculate vector from the entity's y-axis to the new pointer position.
            axis_to_pointer.subVectors(y_intersect, pointer_pos_on_plane);

            // Calculate the angle between the two vectors and rotate the entity around its y-axis
            // by that angle.
            const cos = axis_to_grab.dot(axis_to_pointer);
            const sin = plane_normal.dot(axis_to_grab.cross(axis_to_pointer));
            const angle = Math.atan2(sin, cos);

            entity.set_world_rotation(
                new Euler(
                    initial_rotation.x,
                    (initial_rotation.y + angle) % PI2,
                    initial_rotation.z,
                    "ZXY",
                ),
            );
        }
    };
})();

/**
 * @param renderer
 * @param pointer_pos - pointer coordinates in normalized device space
 * @param drag_adjust - vector from origin of entity to grabbing point
 */
function pick_ground(
    renderer: QuestRenderer,
    pointer_pos: Vector2,
    drag_adjust: Vector3,
): {
    intersection?: Intersection;
    section?: SectionModel;
} {
    raycaster.setFromCamera(pointer_pos, renderer.camera);
    raycaster.ray.origin.add(drag_adjust);
    const intersections = raycaster.intersectObjects(renderer.collision_geometry.children, true);

    // Don't allow entities to be placed on very steep terrain.
    // E.g. walls.
    // TODO: make use of the flags field in the collision data.
    for (const intersection of intersections) {
        if (intersection.face!.normal.y > 0.75) {
            return {
                intersection,
                section: (intersection.object.userData as AreaUserData).section,
            };
        }
    }

    return {};
}
