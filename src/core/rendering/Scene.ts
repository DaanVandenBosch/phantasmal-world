import { Mesh } from "./Mesh";
import { IdentityTransform, Transform } from "./Transform";
import { GL } from "./VertexFormat";

export class Scene {
    readonly root_node = new Node(undefined, new IdentityTransform());

    constructor(private readonly gl: GL) {}

    /**
     * Creates a new node with `node` as parent. Takes ownership of `mesh`.
     *
     * @param node - The parent node.
     * @param mesh - The new node's mesh.
     * @param transform - The new node's transform.
     */
    add_child(node: Node, mesh: Mesh, transform: Transform): this {
        node.children.push(new Node(mesh, transform));
        mesh.upload(this.gl);
        return this;
    }

    /**
     * Deletes all GL objects related to this scene and resets the scene.
     */
    delete(): void {
        this.traverse(node => {
            node.mesh?.texture?.delete(this.gl);
            node.mesh?.delete(this.gl);
            node.mesh = undefined;
        }, undefined);

        this.root_node.children.splice(0);
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
    readonly children: Node[] = [];

    constructor(public mesh: Mesh | undefined, public transform: Transform) {}
}
