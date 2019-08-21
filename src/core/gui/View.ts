import { Disposable } from "../observable/Disposable";
import { Disposer } from "../observable/Disposer";

export abstract class View implements Disposable {
    abstract readonly element: HTMLElement;

    get id(): string {
        return this.element.id;
    }

    set id(id: string) {
        this.element.id = id;
    }

    private disposer = new Disposer();

    protected disposable<T extends Disposable>(disposable: T): T {
        return this.disposer.add(disposable);
    }

    protected disposables(...disposables: Disposable[]): void {
        this.disposer.add_all(...disposables);
    }

    dispose(): void {
        this.element.remove();
        this.disposer.dispose();
    }
}
