import { IdentityTransform, Transform } from "./Transform";
import { Mesh } from "./Mesh";

export class Scene {
    readonly root_node = new Node(this, undefined, new IdentityTransform());

    /**
     * Destroys all GPU objects related to this scene and resets the scene.
     */
    destroy(): void {
        this.traverse(node => {
            node.mesh?.destroy();
            node.mesh?.texture?.destroy();
            node.mesh = undefined;
        }, undefined);

        this.root_node.clear_children();
        this.root_node.transform = new IdentityTransform();
    }

    traverse<T>(f: (node: Node, data: T) => T, data: T): void {
        this.traverse_node(this.root_node, f, data);
    }

    private traverse_node<T>(node: Node, f: (node: Node, data: T) => T, data: T): void {
        const child_data = f(node, data);

        for (const child of node.children) {
            this.traverse_node(child, f, child_data);
        }
    }
}

export class Node {
    private readonly _children: Node[] = [];

    get children(): readonly Node[] {
        return this._children;
    }

    constructor(
        private readonly scene: Scene,
        public mesh: Mesh | undefined,
        public transform: Transform,
    ) {}

    add_child(mesh: Mesh | undefined, transform: Transform): void {
        this._children.push(new Node(this.scene, mesh, transform));
        mesh?.upload();
    }

    clear_children(): void {
        this._children.splice(0);
    }
}
