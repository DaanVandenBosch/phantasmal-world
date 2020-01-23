import { IdentityTransform, Transform } from "../Transform";
import { GL } from "../VertexFormat";
import { WebglMesh } from "./WebglMesh";

export class WebglScene {
    readonly root_node = new WebglNode(this, undefined, new IdentityTransform());

    constructor(private readonly gl: GL) {}

    /**
     * Deletes all GL objects related to this scene and resets the scene.
     */
    delete(): void {
        this.traverse(node => {
            node.mesh?.texture?.delete(this.gl);
            node.mesh?.delete(this.gl);
            node.mesh = undefined;
        }, undefined);

        this.root_node.clear_children();
        this.root_node.transform = new IdentityTransform();
    }

    traverse<T>(f: (node: WebglNode, data: T) => T, data: T): void {
        this.traverse_node(this.root_node, f, data);
    }

    upload(mesh: WebglMesh): void {
        mesh.upload(this.gl);
    }

    private traverse_node<T>(node: WebglNode, f: (node: WebglNode, data: T) => T, data: T): void {
        const child_data = f(node, data);

        for (const child of node.children) {
            this.traverse_node(child, f, child_data);
        }
    }
}

class WebglNode {
    private readonly _children: WebglNode[] = [];

    get children(): readonly WebglNode[] {
        return this._children;
    }

    constructor(
        private readonly scene: WebglScene,
        public mesh: WebglMesh | undefined,
        public transform: Transform,
    ) {}

    add_child(mesh: WebglMesh | undefined, transform: Transform): void {
        this._children.push(new WebglNode(this.scene, mesh, transform));

        if (mesh) {
            this.scene.upload(mesh);
        }
    }

    clear_children(): void {
        this._children.splice(0);
    }
}
