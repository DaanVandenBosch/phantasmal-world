import { QuestRenderer } from "./QuestRenderer";
import { Intersection, Raycaster, Vector2, Vector3 } from "three";
import { SectionModel } from "../model/SectionModel";
import { AreaUserData } from "./conversion/areas";

const raycaster = new Raycaster();

/**
 * @param renderer
 * @param pointer_pos - pointer coordinates in normalized device space
 * @param drag_adjust - vector from origin of entity to grabbing point
 */
export function pick_ground(
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
