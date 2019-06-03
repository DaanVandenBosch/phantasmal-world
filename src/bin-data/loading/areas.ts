import { Object3D } from 'three';
import { Section } from '../../domain';
import { getAreaRenderData, getAreaCollisionData } from './binaryAssets';
import { parseCRel, parseNRel } from '../parsing/geometry';

//
// Caches
//
const sectionsCache: Map<string, Promise<Section[]>> = new Map();
const renderGeometryCache: Map<string, Promise<Object3D>> = new Map();
const collisionGeometryCache: Map<string, Promise<Object3D>> = new Map();

export function getAreaSections(
    episode: number,
    areaId: number,
    areaVariant: number
): Promise<Section[]> {
    const sections = sectionsCache.get(`${episode}-${areaId}-${areaVariant}`);

    if (sections) {
        return sections;
    } else {
        return getAreaSectionsAndRenderGeometry(
            episode, areaId, areaVariant).then(({sections}) => sections);
    }
}

export function getAreaRenderGeometry(
    episode: number,
    areaId: number,
    areaVariant: number
): Promise<Object3D> {
    const object3d = renderGeometryCache.get(`${episode}-${areaId}-${areaVariant}`);

    if (object3d) {
        return object3d;
    } else {
        return getAreaSectionsAndRenderGeometry(
            episode, areaId, areaVariant).then(({object3d}) => object3d);
    }
}

export function getAreaCollisionGeometry(
    episode: number,
    areaId: number,
    areaVariant: number
): Promise<Object3D> {
    const object3d = collisionGeometryCache.get(`${episode}-${areaId}-${areaVariant}`);

    if (object3d) {
        return object3d;
    } else {
        const object3d = getAreaCollisionData(
            episode, areaId, areaVariant).then(parseCRel);
        collisionGeometryCache.set(`${areaId}-${areaVariant}`, object3d);
        return object3d;
    }
}

function getAreaSectionsAndRenderGeometry(
    episode: number,
    areaId: number,
    areaVariant: number
): Promise<{ sections: Section[], object3d: Object3D }> {
    const promise = getAreaRenderData(
        episode, areaId, areaVariant).then(parseNRel);

    const sections = new Promise<Section[]>((resolve, reject) => {
        promise.then(({sections}) => resolve(sections)).catch(reject);
    });
    const object3d = new Promise<Object3D>((resolve, reject) => {
        promise.then(({object3d}) => resolve(object3d)).catch(reject);
    });

    sectionsCache.set(`${episode}-${areaId}-${areaVariant}`, sections);
    renderGeometryCache.set(`${episode}-${areaId}-${areaVariant}`, object3d);

    return promise;
}
