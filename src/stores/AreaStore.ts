import { Object3D } from "three";
import { parse_area_collision_geometry } from "../data_formats/parsing/area_collision_geometry";
import { parse_area_geometry } from "../data_formats/parsing/area_geometry";
import { Area, AreaVariant, Section } from "../domain";
import { area_collision_geometry_to_object_3d } from "../rendering/areas";
import { get_area_collision_data, get_area_render_data } from "./binary_assets";
import { Endianness } from "../data_formats";
import { ArrayBufferCursor } from "../data_formats/cursor/ArrayBufferCursor";

function area(id: number, name: string, order: number, variants: number): Area {
    const area = new Area(id, name, order, []);
    const varis = Array(variants)
        .fill(null)
        .map((_, i) => new AreaVariant(i, area));
    area.area_variants.splice(0, 0, ...varis);
    return area;
}

const sections_cache: Map<string, Promise<Section[]>> = new Map();
const render_geometry_cache: Map<string, Promise<Object3D>> = new Map();
const collision_geometry_cache: Map<string, Promise<Object3D>> = new Map();

class AreaStore {
    areas: Area[][];

    constructor() {
        // The IDs match the PSO IDs for areas.
        this.areas = [];
        let order = 0;
        this.areas[1] = [
            area(0, "Pioneer II", order++, 1),
            area(1, "Forest 1", order++, 1),
            area(2, "Forest 2", order++, 1),
            area(11, "Under the Dome", order++, 1),
            area(3, "Cave 1", order++, 6),
            area(4, "Cave 2", order++, 5),
            area(5, "Cave 3", order++, 6),
            area(12, "Underground Channel", order++, 1),
            area(6, "Mine 1", order++, 6),
            area(7, "Mine 2", order++, 6),
            area(13, "Monitor Room", order++, 1),
            area(8, "Ruins 1", order++, 5),
            area(9, "Ruins 2", order++, 5),
            area(10, "Ruins 3", order++, 5),
            area(14, "Dark Falz", order++, 1),
            area(15, "BA Ruins", order++, 3),
            area(16, "BA Spaceship", order++, 3),
            area(17, "Lobby", order++, 15),
        ];
        order = 0;
        this.areas[2] = [
            area(0, "Lab", order++, 1),
            area(1, "VR Temple Alpha", order++, 3),
            area(2, "VR Temple Beta", order++, 3),
            area(14, "VR Temple Final", order++, 1),
            area(3, "VR Spaceship Alpha", order++, 3),
            area(4, "VR Spaceship Beta", order++, 3),
            area(15, "VR Spaceship Final", order++, 1),
            area(5, "Central Control Area", order++, 1),
            area(6, "Jungle Area East", order++, 1),
            area(7, "Jungle Area North", order++, 1),
            area(8, "Mountain Area", order++, 3),
            area(9, "Seaside Area", order++, 1),
            area(12, "Cliffs of Gal Da Val", order++, 1),
            area(10, "Seabed Upper Levels", order++, 3),
            area(11, "Seabed Lower Levels", order++, 3),
            area(13, "Test Subject Disposal Area", order++, 1),
            area(16, "Seaside Area at Night", order++, 1),
            area(17, "Control Tower", order++, 5),
        ];
        order = 0;
        this.areas[4] = [
            area(0, "Pioneer II (Ep. IV)", order++, 1),
            area(1, "Crater Route 1", order++, 1),
            area(2, "Crater Route 2", order++, 1),
            area(3, "Crater Route 3", order++, 1),
            area(4, "Crater Route 4", order++, 1),
            area(5, "Crater Interior", order++, 1),
            area(6, "Subterranean Desert 1", order++, 3),
            area(7, "Subterranean Desert 2", order++, 3),
            area(8, "Subterranean Desert 3", order++, 3),
            area(9, "Meteor Impact Site", order++, 1),
        ];
    }

    get_variant(episode: number, area_id: number, variant_id: number): AreaVariant {
        if (episode !== 1 && episode !== 2 && episode !== 4)
            throw new Error(`Expected episode to be 1, 2 or 4, got ${episode}.`);

        const area = this.areas[episode].find(a => a.id === area_id);
        if (!area) throw new Error(`Area id ${area_id} for episode ${episode} is invalid.`);

        const area_variant = area.area_variants[variant_id];
        if (!area_variant)
            throw new Error(
                `Area variant id ${variant_id} for area ${area_id} of episode ${episode} is invalid.`
            );

        return area_variant;
    }

    async get_area_sections(
        episode: number,
        area_id: number,
        area_variant: number
    ): Promise<Section[]> {
        const sections = sections_cache.get(`${episode}-${area_id}-${area_variant}`);

        if (sections) {
            return sections;
        } else {
            return this.get_area_sections_and_render_geometry(episode, area_id, area_variant).then(
                ({ sections }) => sections
            );
        }
    }

    async get_area_render_geometry(
        episode: number,
        area_id: number,
        area_variant: number
    ): Promise<Object3D> {
        const object_3d = render_geometry_cache.get(`${episode}-${area_id}-${area_variant}`);

        if (object_3d) {
            return object_3d;
        } else {
            return this.get_area_sections_and_render_geometry(episode, area_id, area_variant).then(
                ({ object_3d }) => object_3d
            );
        }
    }

    async get_area_collision_geometry(
        episode: number,
        area_id: number,
        area_variant: number
    ): Promise<Object3D> {
        const object_3d = collision_geometry_cache.get(`${episode}-${area_id}-${area_variant}`);

        if (object_3d) {
            return object_3d;
        } else {
            const object_3d = get_area_collision_data(episode, area_id, area_variant).then(buffer =>
                area_collision_geometry_to_object_3d(
                    parse_area_collision_geometry(new ArrayBufferCursor(buffer, Endianness.Little))
                )
            );
            collision_geometry_cache.set(`${area_id}-${area_variant}`, object_3d);
            return object_3d;
        }
    }

    private get_area_sections_and_render_geometry(
        episode: number,
        area_id: number,
        area_variant: number
    ): Promise<{ sections: Section[]; object_3d: Object3D }> {
        const promise = get_area_render_data(episode, area_id, area_variant).then(
            parse_area_geometry
        );

        const sections = new Promise<Section[]>((resolve, reject) => {
            promise.then(({ sections }) => resolve(sections)).catch(reject);
        });
        const object_3d = new Promise<Object3D>((resolve, reject) => {
            promise.then(({ object_3d }) => resolve(object_3d)).catch(reject);
        });

        sections_cache.set(`${episode}-${area_id}-${area_variant}`, sections);
        render_geometry_cache.set(`${episode}-${area_id}-${area_variant}`, object_3d);

        return promise;
    }
}

export const area_store = new AreaStore();
