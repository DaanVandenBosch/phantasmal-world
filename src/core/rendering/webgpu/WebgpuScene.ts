import { IdentityTransform, Transform } from "../Transform";
import { WebgpuMesh } from "./WebgpuMesh";

export class WebgpuScene {
    readonly root_node = new WebgpuNode(this, undefined, new IdentityTransform());

    constructor(
        private readonly device: GPUDevice,
        private readonly bind_group_layout: GPUBindGroupLayout,
    ) {}

    /**
     * Destroys all WebGPU objects related to this scene and resets the scene.
     */
    destroy(): void {
        this.traverse(node => {
            // node.mesh?.texture?.delete(this.gl);
            node.mesh?.destroy();
            node.mesh = undefined;
        }, undefined);

        this.root_node.clear_children();
        this.root_node.transform = new IdentityTransform();
    }

    traverse<T>(f: (node: WebgpuNode, data: T) => T, data: T): void {
        this.traverse_node(this.root_node, f, data);
    }

    upload(mesh: WebgpuMesh): void {
        mesh.upload(this.device, this.bind_group_layout);
    }

    private traverse_node<T>(node: WebgpuNode, f: (node: WebgpuNode, data: T) => T, data: T): void {
        const child_data = f(node, data);

        for (const child of node.children) {
            this.traverse_node(child, f, child_data);
        }
    }
}

export class WebgpuNode {
    private readonly _children: WebgpuNode[] = [];

    get children(): readonly WebgpuNode[] {
        return this._children;
    }

    constructor(
        private readonly scene: WebgpuScene,
        public mesh: WebgpuMesh | undefined,
        public transform: Transform,
    ) {}

    add_child(mesh: WebgpuMesh | undefined, transform: Transform): void {
        this._children.push(new WebgpuNode(this.scene, mesh, transform));

        if (mesh) {
            this.scene.upload(mesh);
        }
    }

    clear_children(): void {
        this._children.splice(0);
    }
}
