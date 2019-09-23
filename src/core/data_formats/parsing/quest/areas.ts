import { Episode } from "./Episode";

export type Area = {
    readonly id: number;
    readonly name: string;
    readonly order: number;
    readonly area_variants: AreaVariant[];
};

export type AreaVariant = {
    readonly id: number;
    readonly area: Area;
};

export function get_areas_for_episode(episode: Episode): Area[] {
    return AREAS[episode];
}

export function get_area_variant(
    episode: Episode,
    area_id: number,
    variant_id: number,
): AreaVariant {
    const area = AREAS[episode].find(area => area.id === area_id);
    if (!area) throw new Error(`No area with id ${area_id}.`);

    const variant = area.area_variants[variant_id];
    if (!variant) throw new Error(`No area variant with id ${variant_id}.`);

    return variant;
}

const AREAS: { [episode: number]: Area[] } = [];

function create_area(id: number, name: string, order: number, variants: number): Area {
    const area: Area = { id, name, order, area_variants: [] };

    for (let id = 0; id < variants; id++) {
        area.area_variants.push(Object.freeze({ id, area }));
    }

    return Object.freeze(area);
}

// The IDs match the PSO IDs for areas.
let order = 0;
AREAS[Episode.I] = [
    create_area(0, "Pioneer II", order++, 1),
    create_area(1, "Forest 1", order++, 1),
    create_area(2, "Forest 2", order++, 1),
    create_area(11, "Under the Dome", order++, 1),
    create_area(3, "Cave 1", order++, 6),
    create_area(4, "Cave 2", order++, 5),
    create_area(5, "Cave 3", order++, 6),
    create_area(12, "Underground Channel", order++, 1),
    create_area(6, "Mine 1", order++, 6),
    create_area(7, "Mine 2", order++, 6),
    create_area(13, "Monitor Room", order++, 1),
    create_area(8, "Ruins 1", order++, 5),
    create_area(9, "Ruins 2", order++, 5),
    create_area(10, "Ruins 3", order++, 5),
    create_area(14, "Dark Falz", order++, 1),
    // TODO:
    // create_area(15, "BA Ruins", order++, 3),
    // create_area(16, "BA Spaceship", order++, 3),
    // create_area(17, "Lobby", order++, 15),
];
order = 0;
AREAS[Episode.II] = [
    create_area(0, "Lab", order++, 1),
    create_area(1, "VR Temple Alpha", order++, 3),
    create_area(2, "VR Temple Beta", order++, 3),
    create_area(14, "VR Temple Final", order++, 1),
    create_area(3, "VR Spaceship Alpha", order++, 3),
    create_area(4, "VR Spaceship Beta", order++, 3),
    create_area(15, "VR Spaceship Final", order++, 1),
    create_area(5, "Central Control Area", order++, 1),
    create_area(6, "Jungle Area East", order++, 1),
    create_area(7, "Jungle Area North", order++, 1),
    create_area(8, "Mountain Area", order++, 3),
    create_area(9, "Seaside Area", order++, 1),
    create_area(12, "Cliffs of Gal Da Val", order++, 1),
    create_area(10, "Seabed Upper Levels", order++, 3),
    create_area(11, "Seabed Lower Levels", order++, 3),
    create_area(13, "Test Subject Disposal Area", order++, 1),
    create_area(16, "Seaside Area at Night", order++, 2),
    create_area(17, "Control Tower", order++, 5),
];
order = 0;
AREAS[Episode.IV] = [
    create_area(0, "Pioneer II (Ep. IV)", order++, 1),
    create_area(1, "Crater Route 1", order++, 1),
    create_area(2, "Crater Route 2", order++, 1),
    create_area(3, "Crater Route 3", order++, 1),
    create_area(4, "Crater Route 4", order++, 1),
    create_area(5, "Crater Interior", order++, 1),
    create_area(6, "Subterranean Desert 1", order++, 3),
    create_area(7, "Subterranean Desert 2", order++, 3),
    create_area(8, "Subterranean Desert 3", order++, 3),
    create_area(9, "Meteor Impact Site", order++, 1),
];

Object.freeze(AREAS);
