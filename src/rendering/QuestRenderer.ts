import { Intersection, Mesh, MeshLambertMaterial, Object3D, Plane, Raycaster, Vector2, Vector3 } from "three";
import { Area, Quest, QuestEntity, QuestNpc, QuestObject, Section, Vec3 } from "../domain";
import { area_store } from "../stores/AreaStore";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { NPC_COLOR, NPC_HOVER_COLOR, NPC_SELECTED_COLOR, OBJECT_COLOR, OBJECT_HOVER_COLOR, OBJECT_SELECTED_COLOR } from "./entities";
import { Renderer } from "./Renderer";

let renderer: QuestRenderer | undefined;

export function get_quest_renderer(): QuestRenderer {
    if (!renderer) renderer = new QuestRenderer();
    return renderer;
}

interface PickEntityResult {
    object: Mesh;
    entity: QuestEntity;
    grab_offset: Vector3;
    drag_adjust: Vector3;
    drag_y: number;
    manipulating: boolean;
}

export class QuestRenderer extends Renderer {
    private raycaster = new Raycaster();

    private quest?: Quest;
    private quest_entities_loaded = false;
    private area?: Area;
    private objs: Map<number, QuestObject[]> = new Map(); // Objs grouped by area id
    private npcs: Map<number, QuestNpc[]> = new Map(); // Npcs grouped by area id

    private collision_geometry = new Object3D();
    private render_geometry = new Object3D();
    private obj_geometry = new Object3D();
    private npc_geometry = new Object3D();

    private hovered_data?: PickEntityResult;
    private selected_data?: PickEntityResult;

    constructor() {
        super();

        this.renderer.domElement.addEventListener(
            'mousedown',
            this.on_mouse_down
        );
        this.renderer.domElement.addEventListener(
            'mouseup',
            this.on_mouse_up
        );
        this.renderer.domElement.addEventListener(
            'mousemove',
            this.on_mouse_move
        );

        this.scene.add(this.obj_geometry);
        this.scene.add(this.npc_geometry);
    }

    set_quest_and_area(quest?: Quest, area?: Area) {
        let update = false;

        if (this.area !== area) {
            this.area = area;
            update = true;
        }

        if (this.quest !== quest) {
            this.quest = quest;

            this.objs.clear();
            this.npcs.clear();

            if (quest) {
                for (const obj of quest.objects) {
                    const array = this.objs.get(obj.area_id) || [];
                    array.push(obj);
                    this.objs.set(obj.area_id, array);
                }

                for (const npc of quest.npcs) {
                    const array = this.npcs.get(npc.area_id) || [];
                    array.push(npc);
                    this.npcs.set(npc.area_id, array);
                }
            }

            update = true;
        }

        if (update) {
            this.update_geometry();
        }
    }

    protected render() {
        this.controls.update();
        this.add_loaded_entities();
        this.renderer.render(this.scene, this.camera);
    }

    private async update_geometry() {
        this.scene.remove(this.obj_geometry);
        this.scene.remove(this.npc_geometry);
        this.obj_geometry = new Object3D();
        this.npc_geometry = new Object3D();
        this.scene.add(this.obj_geometry);
        this.scene.add(this.npc_geometry);
        this.quest_entities_loaded = false;

        this.scene.remove(this.collision_geometry);

        if (this.quest && this.area) {
            const episode = this.quest.episode;
            const area_id = this.area.id;
            const variant = this.quest.area_variants.find(v => v.area.id === area_id);
            const variant_id = (variant && variant.id) || 0;

            const collision_geometry = await area_store.get_area_collision_geometry(episode, area_id, variant_id);

            if (this.quest && this.area) {
                this.scene.remove(this.collision_geometry);

                this.reset_camera(new Vector3(0, 800, 700), new Vector3(0, 0, 0));

                this.collision_geometry = collision_geometry;
                this.scene.add(collision_geometry);
            }

            const render_geometry = await area_store.get_area_render_geometry(episode, area_id, variant_id);

            if (this.quest && this.area) {
                this.render_geometry = render_geometry;
            }
        }
    }

    private add_loaded_entities() {
        if (this.quest && this.area && !this.quest_entities_loaded) {
            let loaded = true;

            for (const object of this.quest.objects) {
                if (object.area_id === this.area.id) {
                    if (object.object_3d) {
                        this.obj_geometry.add(object.object_3d);
                    } else {
                        loaded = false;
                    }
                }
            }

            for (const npc of this.quest.npcs) {
                if (npc.area_id === this.area.id) {
                    if (npc.object_3d) {
                        this.npc_geometry.add(npc.object_3d);
                    } else {
                        loaded = false;
                    }
                }
            }

            this.quest_entities_loaded = loaded;
        }
    }

    private on_mouse_down = (e: MouseEvent) => {
        const old_selected_data = this.selected_data;
        const data = this.pick_entity(
            this.pointer_pos_to_device_coords(e)
        );

        // Did we pick a different object than the previously hovered over 3D object?
        if (this.hovered_data && (!data || data.object !== this.hovered_data.object)) {
            (this.hovered_data.object.material as MeshLambertMaterial).color.set(
                this.get_color(this.hovered_data.entity, 'normal')
            );
        }

        // Did we pick a different object than the previously selected 3D object?
        if (this.selected_data && (!data || data.object !== this.selected_data.object)) {
            (this.selected_data.object.material as MeshLambertMaterial).color.set(
                this.get_color(this.selected_data.entity, 'normal')
            );
            this.selected_data.manipulating = false;
        }

        if (data) {
            // User selected an entity.
            (data.object.material as MeshLambertMaterial).color.set(
                this.get_color(data.entity, 'selected')
            );
            data.manipulating = true;
            this.hovered_data = data;
            this.selected_data = data;
            this.controls.enabled = false;
        } else {
            // User clicked on terrain or outside of area.
            this.hovered_data = undefined;
            this.selected_data = undefined;
            this.controls.enabled = true;
        }

        const selection_changed = old_selected_data && data
            ? old_selected_data.object !== data.object
            : old_selected_data !== data;

        if (selection_changed) {
            quest_editor_store.set_selected_entity(data && data.entity);
        }
    }

    private on_mouse_up = () => {
        if (this.selected_data) {
            this.selected_data.manipulating = false;
            this.controls.enabled = true;
        }
    }

    private on_mouse_move = (e: MouseEvent) => {
        const pointer_pos = this.pointer_pos_to_device_coords(e);

        if (this.selected_data && this.selected_data.manipulating) {
            if (e.buttons === 1) {
                // User is dragging a selected entity.
                const data = this.selected_data;

                if (e.shiftKey) {
                    // Vertical movement.
                    // We intersect with a plane that's oriented toward the camera and that's coplanar with the point where the entity was grabbed.
                    this.raycaster.setFromCamera(pointer_pos, this.camera);
                    const ray = this.raycaster.ray;
                    const negative_world_dir = this.camera.getWorldDirection(new Vector3()).negate();
                    const plane = new Plane().setFromNormalAndCoplanarPoint(
                        new Vector3(negative_world_dir.x, 0, negative_world_dir.z).normalize(),
                        data.object.position.sub(data.grab_offset)
                    );
                    const intersection_point = new Vector3();

                    if (ray.intersectPlane(plane, intersection_point)) {
                        const y = intersection_point.y + data.grab_offset.y;
                        const y_delta = y - data.entity.position.y;
                        data.drag_y += y_delta;
                        data.drag_adjust.y -= y_delta;
                        data.entity.position = new Vec3(
                            data.entity.position.x,
                            y,
                            data.entity.position.z
                        );
                    }
                } else {
                    // Horizontal movement accross terrain.
                    // Cast ray adjusted for dragging entities.
                    const { intersection, section } = this.pick_terrain(pointer_pos, data);

                    if (intersection) {
                        data.entity.position = new Vec3(
                            intersection.point.x,
                            intersection.point.y + data.drag_y,
                            intersection.point.z
                        );
                        data.entity.section = section;
                    } else {
                        // If the cursor is not over any terrain, we translate the entity accross the horizontal plane in which the entity's origin lies.
                        this.raycaster.setFromCamera(pointer_pos, this.camera);
                        const ray = this.raycaster.ray;
                        // ray.origin.add(data.dragAdjust);
                        const plane = new Plane(
                            new Vector3(0, 1, 0),
                            -data.entity.position.y + data.grab_offset.y);
                        const intersection_point = new Vector3();

                        if (ray.intersectPlane(plane, intersection_point)) {
                            data.entity.position = new Vec3(
                                intersection_point.x + data.grab_offset.x,
                                data.entity.position.y,
                                intersection_point.z + data.grab_offset.z
                            );
                        }
                    }
                }
            }
        } else {
            // User is hovering.
            const old_data = this.hovered_data;
            const data = this.pick_entity(pointer_pos);

            if (old_data && (!data || data.object !== old_data.object)) {
                if (!this.selected_data || old_data.object !== this.selected_data.object) {
                    (old_data.object.material as MeshLambertMaterial).color.set(
                        this.get_color(old_data.entity, 'normal')
                    );
                }

                this.hovered_data = undefined;
            }

            if (data && (!old_data || data.object !== old_data.object)) {
                if (!this.selected_data || data.object !== this.selected_data.object) {
                    (data.object.material as MeshLambertMaterial).color.set(
                        this.get_color(data.entity, 'hover')
                    );
                }

                this.hovered_data = data;
            }
        }
    }

    private pointer_pos_to_device_coords(e: MouseEvent) {
        const coords = new Vector2();
        this.renderer.getSize(coords);
        coords.width = e.offsetX / coords.width * 2 - 1;
        coords.height = e.offsetY / coords.height * -2 + 1;
        return coords;
    }

    /**
     * @param pointer_pos - pointer coordinates in normalized device space
     */
    private pick_entity(pointer_pos: Vector2): PickEntityResult | undefined {
        // Find the nearest object and NPC under the pointer.
        this.raycaster.setFromCamera(pointer_pos, this.camera);
        const [nearest_object] = this.raycaster.intersectObjects(
            this.obj_geometry.children
        );
        const [nearest_npc] = this.raycaster.intersectObjects(
            this.npc_geometry.children
        );

        if (!nearest_object && !nearest_npc) {
            return;
        }

        const object_dist = nearest_object ? nearest_object.distance : Infinity;
        const npc_dist = nearest_npc ? nearest_npc.distance : Infinity;
        const intersection = object_dist < npc_dist ? nearest_object : nearest_npc;

        const entity = intersection.object.userData.entity;
        // Vector that points from the grabbing point to the model's origin.
        const grab_offset = intersection.object.position
            .clone()
            .sub(intersection.point);
        // Vector that points from the grabbing point to the terrain point directly under the model's origin.
        const drag_adjust = grab_offset.clone();
        // Distance to terrain.
        let drag_y = 0;

        // Find vertical distance to terrain.
        this.raycaster.set(
            intersection.object.position, new Vector3(0, -1, 0)
        );
        const [terrain] = this.raycaster.intersectObjects(
            this.collision_geometry.children, true
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
            manipulating: false
        };
    }

    /**
     * @param pointer_pos - pointer coordinates in normalized device space
     */
    private pick_terrain(
        pointer_pos: Vector2,
        data: PickEntityResult
    ): {
        intersection?: Intersection,
        section?: Section
    } {
        this.raycaster.setFromCamera(pointer_pos, this.camera);
        this.raycaster.ray.origin.add(data.drag_adjust);
        const terrains = this.raycaster.intersectObjects(
            this.collision_geometry.children, true);

        // Don't allow entities to be placed on very steep terrain.
        // E.g. walls.
        // TODO: make use of the flags field in the collision data.
        for (const terrain of terrains) {
            if (terrain.face!.normal.y > 0.75) {
                // Find section ID.
                this.raycaster.set(
                    terrain.point.clone().setY(1000), new Vector3(0, -1, 0));
                const render_terrains = this.raycaster
                    .intersectObjects(this.render_geometry.children, true)
                    .filter(rt => rt.object.userData.section.id >= 0);

                return {
                    intersection: terrain,
                    section: render_terrains[0] && render_terrains[0].object.userData.section
                };
            }
        }

        return {};
    }

    private get_color(entity: QuestEntity, type: 'normal' | 'hover' | 'selected') {
        const is_npc = entity instanceof QuestNpc;

        switch (type) {
            default:
            case 'normal': return is_npc ? NPC_COLOR : OBJECT_COLOR;
            case 'hover': return is_npc ? NPC_HOVER_COLOR : OBJECT_HOVER_COLOR;
            case 'selected': return is_npc ? NPC_SELECTED_COLOR : OBJECT_SELECTED_COLOR;
        }
    }
}
