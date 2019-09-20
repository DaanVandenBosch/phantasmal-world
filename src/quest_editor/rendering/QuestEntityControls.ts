import { QuestEntityModel } from "../model/QuestEntityModel";
import { Intersection, Mesh, MeshLambertMaterial, Plane, Raycaster, Vector2, Vector3 } from "three";
import { Vec3 } from "../../core/data_formats/vector";
import { QuestRenderer } from "./QuestRenderer";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { ColorType, EntityUserData, NPC_COLORS, OBJECT_COLORS } from "./conversion/entities";
import { QuestNpcModel } from "../model/QuestNpcModel";
import { AreaUserData } from "./conversion/areas";
import { SectionModel } from "../model/SectionModel";
import { Disposable } from "../../core/observable/Disposable";
import { Disposer } from "../../core/observable/Disposer";
import { is_npc_type } from "../../core/data_formats/parsing/quest/entities";
import { npc_data } from "../../core/data_formats/parsing/quest/npc_types";
import {
    add_entity_dnd_listener,
    EntityDragEvent,
    remove_entity_dnd_listener,
} from "../gui/entity_dnd";

const DOWN_VECTOR = new Vector3(0, -1, 0);

type Highlighted = {
    entity: QuestEntityModel;
    mesh: Mesh;
};

enum PickMode {
    Creating,
    Transforming,
}

type Pick = {
    mode: PickMode;

    initial_section?: SectionModel;

    initial_position: Vec3;

    /**
     * Vector that points from the grabbing point to the model's origin.
     */
    grab_offset: Vector3;

    /**
     * Vector that points from the grabbing point to the terrain point directly under the model's origin.
     */
    drag_adjust: Vector3;

    /**
     * Distance to terrain.
     */
    drag_y: number;
};

type PickResult = Pick & {
    entity: QuestEntityModel;
    mesh: Mesh;
};

export class QuestEntityControls implements Disposable {
    private raycaster = new Raycaster();
    private selected?: Highlighted;
    private hovered?: Highlighted;
    /**
     * Iff defined, the user is transforming the selected entity.
     */
    private pick?: Pick;
    private last_pointer_position = new Vector2(0, 0);
    private moved_since_last_mouse_down = false;
    private disposer = new Disposer();

    constructor(private renderer: QuestRenderer) {
        this.disposer.add(
            quest_editor_store.selected_entity.observe(({ value: entity }) => {
                if (!this.selected || this.selected.entity !== entity) {
                    this.stop_transforming();

                    if (entity) {
                        // Mesh might not be loaded yet.
                        this.try_highlight(entity);
                    } else {
                        this.deselect();
                    }
                }
            }),
        );

        renderer.dom_element.addEventListener("mousedown", this.mousedown);
        renderer.dom_element.addEventListener("mousemove", this.mousemove);
        renderer.dom_element.addEventListener("mouseup", this.mouseup);
        add_entity_dnd_listener(renderer.dom_element, "dragenter", this.dragenter);
        add_entity_dnd_listener(renderer.dom_element, "dragover", this.dragover);
        add_entity_dnd_listener(renderer.dom_element, "dragleave", this.dragleave);
        add_entity_dnd_listener(renderer.dom_element, "drop", this.drop);
    }

    dispose(): void {
        this.renderer.dom_element.removeEventListener("mousedown", this.mousedown);
        this.renderer.dom_element.removeEventListener("mousemove", this.mousemove);
        this.renderer.dom_element.removeEventListener("mouseup", this.mouseup);
        remove_entity_dnd_listener(this.renderer.dom_element, "dragenter", this.dragenter);
        remove_entity_dnd_listener(this.renderer.dom_element, "dragover", this.dragover);
        remove_entity_dnd_listener(this.renderer.dom_element, "dragleave", this.dragleave);
        remove_entity_dnd_listener(this.renderer.dom_element, "drop", this.drop);
        this.disposer.dispose();
    }

    /**
     * Highlights the selected entity if its mesh has been loaded.
     */
    try_highlight = (entity: QuestEntityModel) => {
        const mesh = this.renderer.get_entity_mesh(entity);

        if (mesh) {
            this.select({ entity, mesh });
        } else {
            if (this.selected) {
                set_color(this.selected, ColorType.Normal);
            }

            this.selected = undefined;
        }
    };

    private mousedown = (e: MouseEvent) => {
        this.process_event(e);
        this.stop_transforming();

        const new_pick = this.pick_entity(this.renderer.pointer_pos_to_device_coords(e));

        if (new_pick) {
            // Disable camera controls while the user is transforming an entity.
            this.renderer.controls.enabled = false;
            this.pick = new_pick;
            this.select(new_pick);
        } else {
            this.renderer.controls.enabled = true;
            this.pick = undefined;
        }

        this.renderer.schedule_render();
    };

    private mousemove = (e: MouseEvent) => {
        this.process_event(e);

        const pointer_device_pos = this.renderer.pointer_pos_to_device_coords(e);

        if (this.selected && this.pick) {
            if (this.moved_since_last_mouse_down) {
                if (e.buttons === 1) {
                    // User is transforming selected entity.
                    // User is dragging selected entity.
                    if (e.shiftKey) {
                        // Vertical movement.
                        this.translate_vertically(this.selected, this.pick, pointer_device_pos);
                    } else {
                        // Horizontal movement across terrain.
                        this.translate_horizontally(this.selected, this.pick, pointer_device_pos);
                    }
                }

                this.renderer.schedule_render();
            }
        } else {
            // User is hovering.
            const new_pick = this.pick_entity(pointer_device_pos);

            if (this.mark_hovered(new_pick)) {
                this.renderer.schedule_render();
            }
        }
    };

    private mouseup = (e: MouseEvent) => {
        this.process_event(e);

        if (!this.moved_since_last_mouse_down && !this.pick) {
            // If the user clicks on nothing, deselect the currently selected entity.
            this.deselect();
        }

        this.stop_transforming();
        // Enable camera controls again after transforming an entity.
        this.renderer.controls.enabled = true;

        this.renderer.schedule_render();
    };

    private dragenter = (e: EntityDragEvent) => {
        const area = quest_editor_store.current_area.val;
        if (!area) return;

        const pointer_position = this.renderer.pointer_pos_to_device_coords(e.event);
        const { intersection, section } = this.pick_terrain(pointer_position, new Vector3());

        let position: Vec3 | undefined;

        if (intersection) {
            position = new Vec3(intersection.point.x, intersection.point.y, intersection.point.z);
        } else {
            // If the cursor is not over any terrain, we translate the entity across the horizontal plane in which the origin lies.
            this.raycaster.setFromCamera(pointer_position, this.renderer.camera);
            const ray = this.raycaster.ray;
            const plane = new Plane(new Vector3(0, 1, 0), 0);
            const intersection_point = new Vector3();

            if (ray.intersectPlane(plane, intersection_point)) {
                position = new Vec3(intersection_point.x, 0, intersection_point.z);
            }
        }

        const quest = quest_editor_store.current_quest.val;

        if (quest && position) {
            if (is_npc_type(e.entity_type)) {
                const data = npc_data(e.entity_type);

                if (data.pso_type_id != undefined && data.pso_roaming != undefined) {
                    e.drag_element.style.display = "none";

                    if (e.event.dataTransfer) {
                        e.event.dataTransfer.dropEffect = "copy";
                    }

                    const npc = new QuestNpcModel(
                        e.entity_type,
                        data.pso_type_id,
                        0,
                        0,
                        data.pso_roaming,
                        section ? section.area_variant.area.id : area.id,
                        section ? section.id : 0,
                        new Vec3(0, 0, 0),
                        new Vec3(0, 0, 0),
                        new Vec3(1, 1, 1),
                        // TODO: do the following values make sense?
                        [[0, 0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0], [0, 0, 0, 0]],
                    );
                    npc.set_world_position(position);
                    quest.add_npc(npc);

                    quest_editor_store.set_selected_entity(npc);

                    this.pick = {
                        mode: PickMode.Creating,
                        initial_section: section,
                        initial_position: position,
                        grab_offset: new Vector3(0, 0, 0),
                        drag_adjust: new Vector3(0, 0, 0),
                        drag_y: 0,
                    };
                }
            }
        }
    };

    private dragover = (e: EntityDragEvent) => {
        if (!quest_editor_store.current_area.val) return;

        if (this.pick && this.pick.mode === PickMode.Creating) {
            e.event.stopPropagation();
            e.event.preventDefault();

            if (e.event.dataTransfer) {
                e.event.dataTransfer.dropEffect = "copy";
            }

            if (this.selected) {
                const pointer_device_pos = this.renderer.pointer_pos_to_device_coords(e.event);

                if (e.event.shiftKey) {
                    // Vertical movement.
                    this.translate_vertically(this.selected, this.pick, pointer_device_pos);
                } else {
                    // Horizontal movement across terrain.
                    this.translate_horizontally(this.selected, this.pick, pointer_device_pos);
                }

                this.renderer.schedule_render();
            }
        }
    };

    private dragleave = (e: EntityDragEvent) => {
        if (!quest_editor_store.current_area.val) return;

        e.drag_element.style.display = "flex";

        const quest = quest_editor_store.current_quest.val;

        if (quest && this.selected && this.pick && this.pick.mode === PickMode.Creating) {
            quest.remove_entity(this.selected.entity);
        }
    };

    private drop = () => {
        // TODO: push onto undo stack.
        this.pick = undefined;
    };

    private process_event(e: MouseEvent): void {
        if (e.type === "mousedown") {
            this.moved_since_last_mouse_down = false;
        } else {
            if (
                e.offsetX !== this.last_pointer_position.x ||
                e.offsetY !== this.last_pointer_position.y
            ) {
                this.moved_since_last_mouse_down = true;
            }
        }

        this.last_pointer_position.set(e.offsetX, e.offsetY);
    }

    /**
     * @returns true if a render is required.
     */
    private mark_hovered(selection?: Highlighted): boolean {
        let render_required = false;

        if (!this.selected || !selection_equals(selection, this.selected)) {
            if (!selection_equals(selection, this.hovered)) {
                if (this.hovered) {
                    set_color(this.hovered, ColorType.Normal);
                    this.hovered = undefined;
                }

                if (selection) {
                    set_color(selection, ColorType.Hovered);
                }

                render_required = true;
            }

            this.hovered = selection;
        }

        return render_required;
    }

    private select(selection: Highlighted): void {
        if (selection_equals(selection, this.hovered)) {
            this.hovered = undefined;
        }

        if (!selection_equals(selection, this.selected)) {
            if (this.selected) {
                set_color(this.selected, ColorType.Normal);
            }

            set_color(selection, ColorType.Selected);

            this.selected = selection;
            quest_editor_store.set_selected_entity(selection.entity);
        } else {
            this.selected = selection;
        }
    }

    private deselect(): void {
        if (this.selected) {
            set_color(this.selected, ColorType.Normal);
        }

        this.selected = undefined;
        quest_editor_store.set_selected_entity(undefined);
    }

    private translate_vertically(
        selection: Highlighted,
        pick: Pick,
        pointer_position: Vector2,
    ): void {
        // We intersect with a plane that's oriented toward the camera and that's coplanar with the point where the entity was grabbed.
        this.raycaster.setFromCamera(pointer_position, this.renderer.camera);
        const ray = this.raycaster.ray;

        const negative_world_dir = this.renderer.camera.getWorldDirection(new Vector3()).negate();
        const plane = new Plane().setFromNormalAndCoplanarPoint(
            new Vector3(negative_world_dir.x, 0, negative_world_dir.z).normalize(),
            selection.mesh.position.sub(pick.grab_offset),
        );

        const intersection_point = new Vector3();

        if (ray.intersectPlane(plane, intersection_point)) {
            const y = intersection_point.y + pick.grab_offset.y;
            const y_delta = y - selection.entity.world_position.val.y;
            pick.drag_y += y_delta;
            pick.drag_adjust.y -= y_delta;
            selection.entity.set_world_position(
                new Vec3(
                    selection.entity.world_position.val.x,
                    y,
                    selection.entity.world_position.val.z,
                ),
            );
        }
    }

    private translate_horizontally(
        selection: Highlighted,
        pick: Pick,
        pointer_position: Vector2,
    ): void {
        // Cast ray adjusted for dragging entities.
        const { intersection, section } = this.pick_terrain(pointer_position, pick.drag_adjust);

        if (intersection) {
            selection.entity.set_world_position(
                new Vec3(
                    intersection.point.x,
                    intersection.point.y + pick.drag_y,
                    intersection.point.z,
                ),
            );

            if (section) {
                selection.entity.set_section(section);
            }
        } else {
            // If the cursor is not over any terrain, we translate the entity across the horizontal plane in which the entity's origin lies.
            this.raycaster.setFromCamera(pointer_position, this.renderer.camera);
            const ray = this.raycaster.ray;
            // ray.origin.add(data.dragAdjust);
            const plane = new Plane(
                new Vector3(0, 1, 0),
                -selection.entity.world_position.val.y + pick.grab_offset.y,
            );
            const intersection_point = new Vector3();

            if (ray.intersectPlane(plane, intersection_point)) {
                selection.entity.set_world_position(
                    new Vec3(
                        intersection_point.x + pick.grab_offset.x,
                        selection.entity.world_position.val.y,
                        intersection_point.z + pick.grab_offset.z,
                    ),
                );
            }
        }
    }

    private stop_transforming = () => {
        if (
            this.moved_since_last_mouse_down &&
            this.selected &&
            this.pick &&
            this.pick.mode === PickMode.Transforming
        ) {
            const entity = this.selected.entity;
            quest_editor_store.push_translate_entity_action(
                entity,
                this.pick.initial_section,
                entity.section.val,
                this.pick.initial_position,
                entity.world_position.val,
                true,
            );
        }

        this.pick = undefined;
    };

    /**
     * @param pointer_position - pointer coordinates in normalized device space
     */
    private pick_entity(pointer_position: Vector2): PickResult | undefined {
        // Find the nearest object and NPC under the pointer.
        this.raycaster.setFromCamera(pointer_position, this.renderer.camera);
        const [intersection] = this.raycaster.intersectObjects(
            this.renderer.entity_models.children,
        );

        if (!intersection) {
            return undefined;
        }

        const entity = (intersection.object.userData as EntityUserData).entity;
        const grab_offset = intersection.object.position.clone().sub(intersection.point);
        const drag_adjust = grab_offset.clone();
        let drag_y = 0;

        // Find vertical distance to terrain.
        this.raycaster.set(intersection.object.position, DOWN_VECTOR);
        const [collision_geom_intersection] = this.raycaster.intersectObjects(
            this.renderer.collision_geometry.children,
            true,
        );

        if (collision_geom_intersection) {
            drag_adjust.y -= collision_geom_intersection.distance;
            drag_y += collision_geom_intersection.distance;
        }

        return {
            mode: PickMode.Transforming,
            mesh: intersection.object as Mesh,
            entity,
            initial_section: entity.section.val,
            initial_position: entity.world_position.val,
            grab_offset,
            drag_adjust,
            drag_y,
        };
    }

    /**
     * @param pointer_pos - pointer coordinates in normalized device space
     * @param drag_adjust - vector from origin of entity to grabbing point
     */
    private pick_terrain(
        pointer_pos: Vector2,
        drag_adjust: Vector3,
    ): {
        intersection?: Intersection;
        section?: SectionModel;
    } {
        this.raycaster.setFromCamera(pointer_pos, this.renderer.camera);
        this.raycaster.ray.origin.add(drag_adjust);
        const intersections = this.raycaster.intersectObjects(
            this.renderer.collision_geometry.children,
            true,
        );

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
}

function set_color({ entity, mesh }: Highlighted, type: ColorType): void {
    const color = entity instanceof QuestNpcModel ? NPC_COLORS[type] : OBJECT_COLORS[type];

    if (mesh) {
        if (Array.isArray(mesh.material)) {
            for (const mat of mesh.material as MeshLambertMaterial[]) {
                if (type === ColorType.Normal && mat.map) {
                    mat.color.set(0xffffff);
                } else {
                    mat.color.set(color);
                }
            }
        } else {
            (mesh.material as MeshLambertMaterial).color.set(color);
        }
    }
}

function selection_equals(a?: Highlighted, b?: Highlighted): boolean {
    return a && b ? a.entity === b.entity : a === b;
}
