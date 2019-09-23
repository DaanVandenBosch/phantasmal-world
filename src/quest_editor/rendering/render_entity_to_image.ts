import { HemisphereLight, PerspectiveCamera, Scene, Vector3, WebGLRenderer } from "three";
import { EntityType } from "../../core/data_formats/parsing/quest/entities";
import { load_entity_geometry, load_entity_textures } from "../loading/entities";
import { create_entity_type_mesh } from "./conversion/entities";
import { sequential } from "../../core/sequential";

const renderer = new WebGLRenderer({ alpha: true, antialias: true });
renderer.setSize(100, 100);

const light = new HemisphereLight(0xffffff, 0x505050, 1.2);
const scene = new Scene();

const camera = new PerspectiveCamera(30, 1, 10, 1000);
const camera_position = new Vector3(1, 1, 2).normalize();
const camera_dist_factor = 1.3 / Math.tan(((camera.fov / 180) * Math.PI) / 2);

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

        const entity_model = create_entity_type_mesh(entity, geometry, textures);
        scene.add(entity_model);

        const b_sphere = entity_model.geometry.boundingSphere;
        camera.position.copy(camera_position);
        camera.position.multiplyScalar(b_sphere.radius * camera_dist_factor);
        camera.lookAt(b_sphere.center);

        renderer.render(scene, camera);

        return renderer.domElement.toDataURL();
    },
);
