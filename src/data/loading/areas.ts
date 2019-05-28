import { Object3D } from 'three';
import { Section } from '../../domain';
import { get_area_render_data, get_area_collision_data } from './assets';
import { parse_c_rel, parse_n_rel } from '../parsing/geometry';

//
// Caches
//
const sections_cache: Map<string, Promise<Section[]>> = new Map();
const render_geometry_cache: Map<string, Promise<Object3D>> = new Map();
const collision_geometry_cache: Map<string, Promise<Object3D>> = new Map();

export function get_area_sections(
    episode: number,
    area_id: number,
    area_variant: number
): Promise<Section[]> {
    const sections = sections_cache.get(`${episode}-${area_id}-${area_variant}`);

    if (sections) {
        return sections;
    } else {
        return get_area_sections_and_render_geometry(
            episode, area_id, area_variant).then(({sections}) => sections);
    }
}

export function get_area_render_geometry(
    episode: number,
    area_id: number,
    area_variant: number
): Promise<Object3D> {
    const object_3d = render_geometry_cache.get(`${episode}-${area_id}-${area_variant}`);

    if (object_3d) {
        return object_3d;
    } else {
        return get_area_sections_and_render_geometry(
            episode, area_id, area_variant).then(({object_3d}) => object_3d);
    }
}

export function get_area_collision_geometry(
    episode: number,
    area_id: number,
    area_variant: number
): Promise<Object3D> {
    const object_3d = collision_geometry_cache.get(`${episode}-${area_id}-${area_variant}`);

    if (object_3d) {
        return object_3d;
    } else {
        const object_3d = get_area_collision_data(
            episode, area_id, area_variant).then(parse_c_rel);
        collision_geometry_cache.set(`${area_id}-${area_variant}`, object_3d);
        return object_3d;
    }
}

function get_area_sections_and_render_geometry(
    episode: number,
    area_id: number,
    area_variant: number
): Promise<{ sections: Section[], object_3d: Object3D }> {
    const promise = get_area_render_data(
        episode, area_id, area_variant).then(parse_n_rel);

    const sections = new Promise<Section[]>((resolve, reject) => {
        promise.then(({sections}) => resolve(sections)).catch(reject);
    });
    const object_3d = new Promise<Object3D>((resolve, reject) => {
        promise.then(({object_3d}) => resolve(object_3d)).catch(reject);
    });

    sections_cache.set(`${episode}-${area_id}-${area_variant}`, sections);
    render_geometry_cache.set(`${episode}-${area_id}-${area_variant}`, object_3d);

    return promise;
}
