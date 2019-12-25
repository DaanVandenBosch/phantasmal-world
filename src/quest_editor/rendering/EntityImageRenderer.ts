import { HemisphereLight, PerspectiveCamera, Scene, Vector3 } from "three";
import { EntityType } from "../../core/data_formats/parsing/quest/entities";
import { create_entity_type_mesh } from "./conversion/entities";
import { sequential } from "../../core/sequential";
import { EntityAssetLoader } from "../loading/EntityAssetLoader";
import { Disposable } from "../../core/observable/Disposable";
import { DisposableThreeRenderer } from "../../core/rendering/Renderer";
import { LoadingCache } from "../loading/LoadingCache";
import { DisposablePromise } from "../../core/DisposablePromise";

const light = new HemisphereLight(0xffffff, 0x505050, 1.2);
const scene = new Scene();

const camera = new PerspectiveCamera(30, 1, 10, 1000);
const camera_position = new Vector3(1, 1, 2).normalize();
const camera_dist_factor = 1.3 / Math.tan(((camera.fov / 180) * Math.PI) / 2);

export class EntityImageRenderer implements Disposable {
    private renderer: DisposableThreeRenderer;
    private readonly cache = new LoadingCache<EntityType, string>();

    constructor(
        private readonly entity_asset_loader: EntityAssetLoader,
        create_three_renderer: () => DisposableThreeRenderer,
    ) {
        this.renderer = create_three_renderer();
        this.renderer.setSize(100, 100);
    }

    dispose(): void {
        this.renderer.dispose();
        this.cache.dispose();
    }

    async render(entity: EntityType): Promise<string> {
        return this.cache.get_or_set(entity, () => this.render_to_image(entity));
    }

    private render_to_image = sequential(
        (entity: EntityType): DisposablePromise<string> =>
            this.entity_asset_loader.load_geometry(entity).then(geometry =>
                this.entity_asset_loader.load_textures(entity).then(textures => {
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
                }),
            ),
    );
}
