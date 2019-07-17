import { runInAction } from "mobx";
import { Intersection, Mesh, MeshLambertMaterial, Plane, Raycaster, Vector2, Vector3 } from "three";
import { Vec3 } from "../data_formats/vector";
import { QuestEntity, QuestNpc, Section } from "../domain";
import { quest_editor_store } from "../stores/QuestEditorStore";
import {
    EntityUserData,
    NPC_COLOR,
    NPC_HIGHLIGHTED_COLOR,
    NPC_SELECTED_COLOR,
    OBJECT_COLOR,
    OBJECT_HIGHLIGHTED_COLOR,
    OBJECT_SELECTED_COLOR,
} from "./conversion/entities";
import { QuestRenderer } from "./QuestRenderer";
import { AreaUserData } from "./conversion/areas";

type Pick = {
    object: Mesh;
    entity: QuestEntity;
    grab_offset: Vector3;
    drag_adjust: Vector3;
    drag_y: number;
};

enum ColorType {
    Normal,
    Highlighted,
    Selected,
}

export class EntityControls {
    private raycaster = new Raycaster();
    private selected?: Pick;
    private highlighted?: Pick;
    private transforming = false;
    private last_pointer_position = new Vector2(0, 0);
    private moved_since_last_mouse_down = false;

    constructor(private renderer: QuestRenderer) {}

    on_mouse_down = (e: MouseEvent) => {
        this.process_event(e);

        const new_pick = this.pick_entity(this.renderer.pointer_pos_to_device_coords(e));

        if (new_pick) {
            this.transforming = new_pick != null;
            // Disable camera controls while the user is transforming an entity.
            this.renderer.controls.enabled = !this.transforming;

            this.select(new_pick);
            quest_editor_store.set_selected_entity(new_pick.entity);
        }

        this.renderer.schedule_render();
    };

    on_mouse_up = (e: MouseEvent) => {
        this.process_event(e);

        // If the user clicks on nothing, deselect the currently selected entity.
        if (!this.moved_since_last_mouse_down && !this.transforming) {
            this.deselect();
            quest_editor_store.set_selected_entity(undefined);
        }

        this.transforming = false;
        // Enable camera controls again after transforming an entity.
        this.renderer.controls.enabled = !this.transforming;

        this.renderer.schedule_render();
    };

    on_mouse_move = (e: MouseEvent) => {
        this.process_event(e);

        const pointer_device_pos = this.renderer.pointer_pos_to_device_coords(e);

        if (this.selected && this.transforming) {
            if (e.buttons === 1) {
                // User is dragging a selected entity.
                if (e.shiftKey) {
                    // Vertical movement.
                    this.translate_vertically(this.selected, pointer_device_pos);
                } else {
                    // Horizontal movement accross terrain.
                    this.translate_horizontally(this.selected, pointer_device_pos);
                }
            }

            this.renderer.schedule_render();
        } else {
            // User is hovering.
            const new_pick = this.pick_entity(pointer_device_pos);

            if (this.highlight(new_pick)) {
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
    private highlight(pick?: Pick): boolean {
        let render_required = false;

        if (!this.selected || !picks_equal(pick, this.selected)) {
            if (!picks_equal(pick, this.highlighted)) {
                this.unhighlight();

                if (pick) {
                    set_color(pick, ColorType.Highlighted);
                }

                render_required = true;
            }

            this.highlighted = pick;
        }

        return render_required;
    }

    private unhighlight(): void {
        if (this.highlighted) {
            set_color(this.highlighted, ColorType.Normal);
            this.highlighted = undefined;
        }
    }

    private select(pick: Pick): void {
        this.unhighlight();

        if (!picks_equal(pick, this.selected)) {
            if (this.selected) {
                set_color(this.selected, ColorType.Normal);
            }

            set_color(pick, ColorType.Selected);
        }

        this.selected = pick;
    }

    private deselect(): void {
        if (this.selected) {
            set_color(this.selected, ColorType.Normal);
        }

        this.selected = undefined;
    }

    private translate_vertically(pick: Pick, pointer_position: Vector2): void {
        // We intersect with a plane that's oriented toward the camera and that's coplanar with the point where the entity was grabbed.
        this.raycaster.setFromCamera(pointer_position, this.renderer.camera);
        const ray = this.raycaster.ray;

        const negative_world_dir = this.renderer.camera.getWorldDirection(new Vector3()).negate();
        const plane = new Plane().setFromNormalAndCoplanarPoint(
            new Vector3(negative_world_dir.x, 0, negative_world_dir.z).normalize(),
            pick.object.position.sub(pick.grab_offset)
        );

        const intersection_point = new Vector3();

        if (ray.intersectPlane(plane, intersection_point)) {
            const y = intersection_point.y + pick.grab_offset.y;
            const y_delta = y - pick.entity.position.y;
            pick.drag_y += y_delta;
            pick.drag_adjust.y -= y_delta;
            pick.entity.position = new Vec3(pick.entity.position.x, y, pick.entity.position.z);
        }
    }

    private translate_horizontally(pick: Pick, pointer_position: Vector2): void {
        // Cast ray adjusted for dragging entities.
        const { intersection, section } = this.pick_terrain(pointer_position, pick);

        if (intersection) {
            runInAction(() => {
                pick.entity.position = new Vec3(
                    intersection.point.x,
                    intersection.point.y + pick.drag_y,
                    intersection.point.z
                );
                pick.entity.section = section;
            });
        } else {
            // If the cursor is not over any terrain, we translate the entity accross the horizontal plane in which the entity's origin lies.
            this.raycaster.setFromCamera(pointer_position, this.renderer.camera);
            const ray = this.raycaster.ray;
            // ray.origin.add(data.dragAdjust);
            const plane = new Plane(
                new Vector3(0, 1, 0),
                -pick.entity.position.y + pick.grab_offset.y
            );
            const intersection_point = new Vector3();

            if (ray.intersectPlane(plane, intersection_point)) {
                pick.entity.position = new Vec3(
                    intersection_point.x + pick.grab_offset.x,
                    pick.entity.position.y,
                    intersection_point.z + pick.grab_offset.z
                );
            }
        }
    }

    /**
     * @param pointer_position pointer coordinates in normalized device space
     */
    private pick_entity(pointer_position: Vector2): Pick | undefined {
        // Find the nearest object and NPC under the pointer.
        this.raycaster.setFromCamera(pointer_position, this.renderer.camera);
        const [nearest_object] = this.raycaster.intersectObjects(
            this.renderer.obj_geometry.children
        );
        const [nearest_npc] = this.raycaster.intersectObjects(this.renderer.npc_geometry.children);

        if (!nearest_object && !nearest_npc) {
            return undefined;
        }

        const object_dist = nearest_object ? nearest_object.distance : Infinity;
        const npc_dist = nearest_npc ? nearest_npc.distance : Infinity;
        const intersection = object_dist < npc_dist ? nearest_object : nearest_npc;

        const entity = (intersection.object.userData as EntityUserData).entity;
        // Vector that points from the grabbing point to the model's origin.
        const grab_offset = intersection.object.position.clone().sub(intersection.point);
        // Vector that points from the grabbing point to the terrain point directly under the model's origin.
        const drag_adjust = grab_offset.clone();
        // Distance to terrain.
        let drag_y = 0;

        // Find vertical distance to terrain.
        this.raycaster.set(intersection.object.position, new Vector3(0, -1, 0));
        const [terrain] = this.raycaster.intersectObjects(
            this.renderer.collision_geometry.children,
            true
        );

        if (terrain) {
            drag_adjust.sub(new Vector3(0, terrain.distance, 0));
            drag_y += terrain.distance;
        }

        return {
            object: intersection.object as Mesh,
            entity,
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
        const terrains = this.raycaster.intersectObjects(
            this.renderer.collision_geometry.children,
            true
        );

        // Don't allow entities to be placed on very steep terrain.
        // E.g. walls.
        // TODO: make use of the flags field in the collision data.
        for (const terrain of terrains) {
            if (terrain.face!.normal.y > 0.75) {
                // Find section ID.
                this.raycaster.set(terrain.point.clone().setY(1000), new Vector3(0, -1, 0));
                const render_terrains = this.raycaster
                    .intersectObjects(this.renderer.render_geometry.children, true)
                    .filter(rt => (rt.object.userData as AreaUserData).section.id >= 0);

                return {
                    intersection: terrain,
                    section:
                        render_terrains[0] &&
                        (render_terrains[0].object.userData as AreaUserData).section,
                };
            }
        }

        return {};
    }
}

function set_color(pick: Pick, type: ColorType): void {
    const color = get_color(pick.entity, type);

    for (const material of pick.object.material as MeshLambertMaterial[]) {
        if (type === ColorType.Normal && material.map) {
            material.color.set(0xffffff);
        } else {
            material.color.set(color);
        }
    }
}

function picks_equal(a?: Pick, b?: Pick): boolean {
    return a && b ? a.entity === b.entity : a === b;
}

function get_color(entity: QuestEntity, type: ColorType): number {
    const is_npc = entity instanceof QuestNpc;

    switch (type) {
        default:
        case ColorType.Normal:
            return is_npc ? NPC_COLOR : OBJECT_COLOR;
        case ColorType.Highlighted:
            return is_npc ? NPC_HIGHLIGHTED_COLOR : OBJECT_HIGHLIGHTED_COLOR;
        case ColorType.Selected:
            return is_npc ? NPC_SELECTED_COLOR : OBJECT_SELECTED_COLOR;
    }
}
