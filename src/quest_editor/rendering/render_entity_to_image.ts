import { HemisphereLight, PerspectiveCamera, Scene, WebGLRenderer } from "three";
import { EntityType } from "../../core/data_formats/parsing/quest/entities";
import { load_entity_geometry, load_entity_textures } from "../loading/entities";
import { create_entity_type_mesh } from "./conversion/entities";
import { sequential } from "../../core/sequential";

const renderer = new WebGLRenderer({ alpha: true, antialias: true });
renderer.setSize(100, 100);

const camera = new PerspectiveCamera(60, 1, 10, 1000);
const light = new HemisphereLight(0xffffff, 0x505050, 1.2);
const scene = new Scene();

const cache: Map<EntityType, Promise<string>> = new Map();

export async function render_entity_to_image(entity: EntityType): Promise<string> {
    let url = cache.get(entity);

    if (!url) {
        url = render(entity);
        cache.set(entity, url);
    }

    return url;
}

const render = sequential(
    async (entity: EntityType): Promise<string> => {
        const geometry = await load_entity_geometry(entity);
        const textures = await load_entity_textures(entity);

        scene.remove(...scene.children);
        scene.add(light);
        scene.add(create_entity_type_mesh(entity, geometry, textures));

        camera.position.set(10, 25, 20);
        camera.lookAt(0, 10, 0);

        renderer.render(scene, camera);

        return renderer.domElement.toDataURL();
    },
);
