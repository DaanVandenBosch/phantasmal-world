import { autorun } from "mobx";
import { Intersection, Mesh, MeshLambertMaterial, Plane, Raycaster, Vector2, Vector3 } from "three";
import { Vec3 } from "../data_formats/vector";
import { QuestEntity, QuestNpc, QuestObject, Section } from "../domain";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { AreaUserData } from "./conversion/areas";
import { ColorType, EntityUserData, NPC_COLORS, OBJECT_COLORS } from "./conversion/entities";
import { QuestRenderer } from "./QuestRenderer";

const DOWN_VECTOR = new Vector3(0, -1, 0);

type Highlighted = {
    entity: QuestEntity;
    mesh: Mesh;
};

type Pick = {
    initial_position: Vec3;
    grab_offset: Vector3;
    drag_adjust: Vector3;
    drag_y: number;
};

type PickResult = Pick & {
    entity: QuestEntity;
    mesh: Mesh;
};

export class QuestEntityControls {
    private raycaster = new Raycaster();
    private selected?: Highlighted;
    private hovered?: Highlighted;
    /**
     * Iff defined, the user is transforming the selected entity.
     */
    private pick?: Pick;
    private last_pointer_position = new Vector2(0, 0);
    private moved_since_last_mouse_down = false;

    constructor(private renderer: QuestRenderer) {
        autorun(() => {
            const entity = quest_editor_store.selected_entity;

            if (!this.selected || this.selected.entity !== entity) {
                this.stop_transforming();

                if (entity) {
                    // Mesh might not be loaded yet.
                    this.try_highlight_selected();
                } else {
                    this.deselect();
                }
            }
        });
    }

    /**
     * Highlights the selected entity if its mesh has been loaded.
     */
    try_highlight_selected = () => {
        const entity = quest_editor_store.selected_entity!;
        const mesh = this.renderer.get_entity_mesh(entity);

        if (mesh) {
            this.select({ entity, mesh });
        }
    };

    on_mouse_down = (e: MouseEvent) => {
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

    on_mouse_up = (e: MouseEvent) => {
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

    on_mouse_move = (e: MouseEvent) => {
        this.process_event(e);

        const pointer_device_pos = this.renderer.pointer_pos_to_device_coords(e);

        if (this.selected && this.pick) {
            if (this.moved_since_last_mouse_down) {
                if (e.buttons === 1) {
                    // User is tranforming selected entity.
                    // User is dragging selected entity.
                    if (e.shiftKey) {
                        // Vertical movement.
                        this.translate_vertically(this.selected, this.pick, pointer_device_pos);
                    } else {
                        // Horizontal movement accross terrain.
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
        pointer_position: Vector2
    ): void {
        // We intersect with a plane that's oriented toward the camera and that's coplanar with the point where the entity was grabbed.
        this.raycaster.setFromCamera(pointer_position, this.renderer.camera);
        const ray = this.raycaster.ray;

        const negative_world_dir = this.renderer.camera.getWorldDirection(new Vector3()).negate();
        const plane = new Plane().setFromNormalAndCoplanarPoint(
            new Vector3(negative_world_dir.x, 0, negative_world_dir.z).normalize(),
            selection.mesh.position.sub(pick.grab_offset)
        );

        const intersection_point = new Vector3();

        if (ray.intersectPlane(plane, intersection_point)) {
            const y = intersection_point.y + pick.grab_offset.y;
            const y_delta = y - selection.entity.position.y;
            pick.drag_y += y_delta;
            pick.drag_adjust.y -= y_delta;
            selection.entity.position = new Vec3(
                selection.entity.position.x,
                y,
                selection.entity.position.z
            );
        }
    }

    private translate_horizontally(
        selection: Highlighted,
        pick: Pick,
        pointer_position: Vector2
    ): void {
        // Cast ray adjusted for dragging entities.
        const { intersection, section } = this.pick_terrain(pointer_position, pick);

        if (intersection) {
            selection.entity.set_position_and_section(
                new Vec3(
                    intersection.point.x,
                    intersection.point.y + pick.drag_y,
                    intersection.point.z
                ),
                section
            );
        } else {
            // If the cursor is not over any terrain, we translate the entity accross the horizontal plane in which the entity's origin lies.
            this.raycaster.setFromCamera(pointer_position, this.renderer.camera);
            const ray = this.raycaster.ray;
            // ray.origin.add(data.dragAdjust);
            const plane = new Plane(
                new Vector3(0, 1, 0),
                -selection.entity.position.y + pick.grab_offset.y
            );
            const intersection_point = new Vector3();

            if (ray.intersectPlane(plane, intersection_point)) {
                selection.entity.position = new Vec3(
                    intersection_point.x + pick.grab_offset.x,
                    selection.entity.position.y,
                    intersection_point.z + pick.grab_offset.z
                );
            }
        }
    }

    private stop_transforming = () => {
        if (this.moved_since_last_mouse_down && this.selected && this.pick) {
            const entity = this.selected.entity;
            const initial_position = this.pick.initial_position;
            const new_position = entity.position;
            const entity_type =
                entity instanceof QuestNpc ? entity.type.name : (entity as QuestObject).type.name;

            quest_editor_store.undo.push_action(
                `Move ${entity_type}`,
                () => {
                    entity.position = initial_position;
                    quest_editor_store.set_selected_entity(entity);
                },
                () => {
                    entity.position = new_position;
                    quest_editor_store.set_selected_entity(entity);
                }
            );
        }

        this.pick = undefined;
    };

    /**
     * @param pointer_position pointer coordinates in normalized device space
     */
    private pick_entity(pointer_position: Vector2): PickResult | undefined {
        // Find the nearest object and NPC under the pointer.
        this.raycaster.setFromCamera(pointer_position, this.renderer.camera);
        const [intersection] = this.raycaster.intersectObjects(
            this.renderer.entity_models.children
        );

        if (!intersection) {
            return undefined;
        }

        const entity = (intersection.object.userData as EntityUserData).entity;
        // Vector that points from the grabbing point to the model's origin.
        const grab_offset = intersection.object.position.clone().sub(intersection.point);
        // Vector that points from the grabbing point to the terrain point directly under the model's origin.
        const drag_adjust = grab_offset.clone();
        // Distance to terrain.
        let drag_y = 0;

        // Find vertical distance to terrain.
        this.raycaster.set(intersection.object.position, DOWN_VECTOR);
        const [collision_geom_intersection] = this.raycaster.intersectObjects(
            this.renderer.collision_geometry.children,
            true
        );

        if (collision_geom_intersection) {
            drag_adjust.y -= collision_geom_intersection.distance;
            drag_y += collision_geom_intersection.distance;
        }

        return {
            mesh: intersection.object as Mesh,
            entity,
            initial_position: entity.position,
            grab_offset,
            drag_adjust,
            drag_y,
        };
    }

    /**
     * @param pointer_pos - pointer coordinates in normalized device space
     */
    private pick_terrain(
        pointer_pos: Vector2,
        data: Pick
    ): {
        intersection?: Intersection;
        section?: Section;
    } {
        this.raycaster.setFromCamera(pointer_pos, this.renderer.camera);
        this.raycaster.ray.origin.add(data.drag_adjust);
        const intersections = this.raycaster.intersectObjects(
            this.renderer.collision_geometry.children,
            true
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
    const color = entity instanceof QuestNpc ? NPC_COLORS[type] : OBJECT_COLORS[type];

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
