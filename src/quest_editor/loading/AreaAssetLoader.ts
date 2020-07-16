import { Object3D } from "three";
import { Endianness } from "../../core/data_formats/block/Endianness";
import { ArrayBufferCursor } from "../../core/data_formats/block/cursor/ArrayBufferCursor";
import {
    CollisionObject,
    parse_area_collision_geometry,
} from "../../core/data_formats/parsing/area_collision_geometry";
import { parse_area_geometry, RenderObject } from "../../core/data_formats/parsing/area_geometry";
import { LoadingCache } from "./LoadingCache";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { SectionModel } from "../model/SectionModel";
import {
    area_collision_geometry_to_object_3d,
    area_geometry_to_sections_and_object_3d,
} from "../rendering/conversion/areas";
import { AreaVariantModel } from "../model/AreaVariantModel";
import { HttpClient } from "../../core/HttpClient";
import { Disposable } from "../../core/observable/Disposable";
import { DisposablePromise } from "../../core/DisposablePromise";
import { Disposer } from "../../core/observable/Disposer";

export class AreaAssetLoader implements Disposable {
    private readonly disposer = new Disposer();
    private readonly render_object_cache = this.disposer.add(
        new LoadingCache<string, RenderObject>(),
    );
    private readonly collision_object_cache = this.disposer.add(
        new LoadingCache<string, CollisionObject>(),
    );
    private readonly area_sections_cache = this.disposer.add(
        new LoadingCache<string, SectionModel[]>(),
    );

    constructor(private readonly http_client: HttpClient) {}

    dispose(): void {
        this.disposer.dispose();
    }

    load_sections(
        episode: Episode,
        area_variant: AreaVariantModel,
    ): DisposablePromise<SectionModel[]> {
        const key = `${episode}-${area_variant.area.id}-${area_variant.id}`;

        return this.area_sections_cache.get_or_set(key, () =>
            this.render_object_cache
                .get_or_set(key, () => this.load_render_object(episode, area_variant))
                .then(
                    render_object =>
                        area_geometry_to_sections_and_object_3d(render_object, area_variant)[0],
                ),
        );
    }

    load_render_geometry(
        episode: Episode,
        area_variant: AreaVariantModel,
    ): DisposablePromise<Object3D> {
        const key = `${episode}-${area_variant.area.id}-${area_variant.id}`;

        return this.render_object_cache
            .get_or_set(key, () => this.load_render_object(episode, area_variant))
            .then(
                render_object =>
                    // Do not cache this call, multiple renderers need their own copy of the data.
                    area_geometry_to_sections_and_object_3d(render_object, area_variant)[1],
            );
    }

    load_collision_geometry(
        episode: Episode,
        area_variant: AreaVariantModel,
    ): DisposablePromise<Object3D> {
        const key = `${episode}-${area_variant.area.id}-${area_variant.id}`;

        return this.collision_object_cache
            .get_or_set(key, () =>
                this.get_area_asset(episode, area_variant, "collision").then(buffer =>
                    parse_area_collision_geometry(new ArrayBufferCursor(buffer, Endianness.Little)),
                ),
            )
            .then(collision_object =>
                // Do not cache this call, multiple renderers need their own copy of the data.
                area_collision_geometry_to_object_3d(collision_object),
            );
    }

    private load_render_object(
        episode: Episode,
        area_variant: AreaVariantModel,
    ): DisposablePromise<RenderObject> {
        return this.get_area_asset(episode, area_variant, "render").then(buffer =>
            parse_area_geometry(new ArrayBufferCursor(buffer, Endianness.Little)),
        );
    }

    private get_area_asset(
        episode: Episode,
        area_variant: AreaVariantModel,
        type: "render" | "collision",
    ): DisposablePromise<ArrayBuffer> {
        const base_url = area_version_to_base_url(episode, area_variant);
        const suffix = type === "render" ? "n.rel" : "c.rel";
        return this.http_client.get(base_url + suffix).array_buffer();
    }
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

function area_version_to_base_url(episode: Episode, area_variant: AreaVariantModel): string {
    let area_id = area_variant.area.id;
    let area_variant_id = area_variant.id;

    // Exception for Seaside area at night, variant 1.
    // Phantasmal World 4 and Lost heart breaker use this to have two tower maps.
    if (area_id === 16 && area_variant_id === 1) {
        area_id = 17;
        area_variant_id = 1;
    }

    const episode_base_names = area_base_names[episode - 1];

    if (0 <= area_id && area_id < episode_base_names.length) {
        const [base_name, variants] = episode_base_names[area_id];

        if (0 <= area_variant_id && area_variant_id < variants) {
            let variant: string;

            if (variants === 1) {
                variant = "";
            } else {
                variant = String(area_variant_id);
                variant = variant.padStart(2, "0");
            }

            return `/maps/map_${base_name}${variant}`;
        } else {
            throw new Error(
                `Unknown variant ${area_variant_id} of area ${area_id} in episode ${episode}.`,
            );
        }
    } else {
        throw new Error(`Unknown episode ${episode} area ${area_id}.`);
    }
}
