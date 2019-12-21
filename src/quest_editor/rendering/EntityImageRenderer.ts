import { HemisphereLight, PerspectiveCamera, Scene, Vector3, WebGLRenderer } from "three";
import { EntityType } from "../../core/data_formats/parsing/quest/entities";
import { create_entity_type_mesh } from "./conversion/entities";
import { sequential } from "../../core/sequential";
import { EntityAssetLoader } from "../loading/EntityAssetLoader";

const light = new HemisphereLight(0xffffff, 0x505050, 1.2);
const scene = new Scene();

const camera = new PerspectiveCamera(30, 1, 10, 1000);
const camera_position = new Vector3(1, 1, 2).normalize();
const camera_dist_factor = 1.3 / Math.tan(((camera.fov / 180) * Math.PI) / 2);

export class EntityImageRenderer {
    private renderer = new WebGLRenderer({ alpha: true, antialias: true });
    private readonly cache: Map<EntityType, Promise<string>> = new Map();

    constructor(private readonly entity_asset_loader: EntityAssetLoader) {
        this.renderer.setSize(100, 100);
    }

    async render(entity: EntityType): Promise<string> {
        let url = this.cache.get(entity);

        if (!url) {
            url = this.render_to_image(entity);
            this.cache.set(entity, url);
        }

        return url;
    }

    private render_to_image = sequential(
        async (entity: EntityType): Promise<string> => {
            const geometry = await this.entity_asset_loader.load_geometry(entity);
            const textures = await this.entity_asset_loader.load_textures(entity);

            scene.remove(...scene.children);
            scene.add(light);

            const entity_model = create_entity_type_mesh(entity, geometry, textures);
            scene.add(entity_model);

            const b_sphere = entity_model.geometry.boundingSphere;
            camera.position.copy(camera_position);
            camera.position.multiplyScalar(b_sphere.radius * camera_dist_factor);
            camera.lookAt(b_sphere.center);

            this.renderer.render(scene, camera);

            return this.renderer.domElement.toDataURL();
        },
    );
}
