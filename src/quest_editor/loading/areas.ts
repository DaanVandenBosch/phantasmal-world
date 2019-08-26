import { Object3D } from "three";
import { Endianness } from "../../core/data_formats/Endianness";
import { ArrayBufferCursor } from "../../core/data_formats/cursor/ArrayBufferCursor";
import { parse_area_collision_geometry } from "../../core/data_formats/parsing/area_collision_geometry";
import { parse_area_geometry } from "../../core/data_formats/parsing/area_geometry";
import { load_array_buffer } from "../../core/loading";
import { LoadingCache } from "./LoadingCache";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { SectionModel } from "../model/SectionModel";
import {
    area_collision_geometry_to_object_3d,
    area_geometry_to_sections_and_object_3d,
} from "../rendering/conversion/areas";

const render_geometry_cache = new LoadingCache<
    string,
    { geometry: Promise<Object3D>; sections: Promise<SectionModel[]> }
>();
const collision_geometry_cache = new LoadingCache<string, Promise<Object3D>>();

export async function load_area_sections(
    episode: Episode,
    area_id: number,
    area_variant: number,
): Promise<SectionModel[]> {
    return render_geometry_cache.get_or_set(`${episode}-${area_id}-${area_variant}`, () =>
        load_area_sections_and_render_geometry(episode, area_id, area_variant),
    ).sections;
}

export async function load_area_render_geometry(
    episode: Episode,
    area_id: number,
    area_variant: number,
): Promise<Object3D> {
    return render_geometry_cache.get_or_set(`${episode}-${area_id}-${area_variant}`, () =>
        load_area_sections_and_render_geometry(episode, area_id, area_variant),
    ).geometry;
}

export async function load_area_collision_geometry(
    episode: Episode,
    area_id: number,
    area_variant: number,
): Promise<Object3D> {
    return collision_geometry_cache.get_or_set(`${episode}-${area_id}-${area_variant}`, () =>
        get_area_asset(episode, area_id, area_variant, "collision").then(buffer =>
            area_collision_geometry_to_object_3d(
                parse_area_collision_geometry(new ArrayBufferCursor(buffer, Endianness.Little)),
            ),
        ),
    );
}

function load_area_sections_and_render_geometry(
    episode: Episode,
    area_id: number,
    area_variant: number,
): { geometry: Promise<Object3D>; sections: Promise<SectionModel[]> } {
    const promise = get_area_asset(episode, area_id, area_variant, "render").then(buffer =>
        area_geometry_to_sections_and_object_3d(
            parse_area_geometry(new ArrayBufferCursor(buffer, Endianness.Little)),
        ),
    );

    return {
        geometry: promise.then(([, object_3d]) => object_3d),
        sections: promise.then(([sections]) => sections),
    };
}

const area_base_names = [
    [
        ["city00_00", 1],
        ["forest01", 1],
        ["forest02", 1],
        ["cave01_", 6],
        ["cave02_", 5],
        ["cave03_", 6],
        ["machine01_", 6],
        ["machine02_", 6],
        ["ancient01_", 5],
        ["ancient02_", 5],
        ["ancient03_", 5],
        ["boss01", 1],
        ["boss02", 1],
        ["boss03", 1],
        ["darkfalz00", 1],
    ],
    [
        ["labo00_00", 1],
        ["ruins01_", 3],
        ["ruins02_", 3],
        ["space01_", 3],
        ["space02_", 3],
        ["jungle01_00", 1],
        ["jungle02_00", 1],
        ["jungle03_00", 1],
        ["jungle04_", 3],
        ["jungle05_00", 1],
        ["seabed01_", 3],
        ["seabed02_", 3],
        ["boss05", 1],
        ["boss06", 1],
        ["boss07", 1],
        ["boss08", 1],
        ["jungle06_00", 1],
        ["jungle07_", 5],
    ],
    [
        // Don't remove this empty array, see usage of area_base_names in area_version_to_base_url.
    ],
    [
        ["city02_00", 1],
        ["wilds01_00", 1],
        ["wilds01_01", 1],
        ["wilds01_02", 1],
        ["wilds01_03", 1],
        ["crater01_00", 1],
        ["desert01_", 3],
        ["desert02_", 3],
        ["desert03_", 3],
        ["boss09_00", 1],
    ],
];

async function get_area_asset(
    episode: Episode,
    area_id: number,
    area_variant: number,
    type: "render" | "collision",
): Promise<ArrayBuffer> {
    const base_url = area_version_to_base_url(episode, area_id, area_variant);
    const suffix = type === "render" ? "n.rel" : "c.rel";
    return load_array_buffer(base_url + suffix);
}

function area_version_to_base_url(episode: Episode, area_id: number, area_variant: number): string {
    // Exception for Seaside area at night variant 1.
    // Phantasmal World 4 and Lost heart breaker use this to have two tower maps.
    if (area_id === 16 && area_variant === 1) {
        area_id = 17;
        area_variant = 1;
    }

    const episode_base_names = area_base_names[episode - 1];

    if (0 <= area_id && area_id < episode_base_names.length) {
        const [base_name, variants] = episode_base_names[area_id];

        if (0 <= area_variant && area_variant < variants) {
            let variant: string;

            if (variants === 1) {
                variant = "";
            } else {
                variant = String(area_variant);
                while (variant.length < 2) variant = "0" + variant;
            }

            return `/maps/map_${base_name}${variant}`;
        } else {
            throw new Error(
                `Unknown variant ${area_variant} of area ${area_id} in episode ${episode}.`,
            );
        }
    } else {
        throw new Error(`Unknown episode ${episode} area ${area_id}.`);
    }
}
