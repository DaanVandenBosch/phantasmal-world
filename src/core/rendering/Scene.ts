import { Mesh } from "./Mesh";
import { Mat4 } from "../math/linear_algebra";

export class Scene {
    readonly root_node = new SceneNode(undefined, Mat4.identity());

    traverse<T>(f: (node: SceneNode, data: T) => T, data: T): void {
        this.traverse_node(this.root_node, f, data);
    }

    private traverse_node<T>(node: SceneNode, f: (node: SceneNode, data: T) => T, data: T): void {
        const child_data = f(node, data);

        for (const child of node.children) {
            this.traverse_node(child, f, child_data);
        }
    }
}

export class SceneNode {
    private readonly _children: SceneNode[];

    get children(): readonly SceneNode[] {
        return this._children;
    }

    constructor(public mesh: Mesh | undefined, public transform: Mat4, ...children: SceneNode[]) {
        this._children = children;
    }

    add_child(child: SceneNode): void {
        this._children.push(child);
    }

    clear_children(): void {
        this._children.splice(0);
    }
}
