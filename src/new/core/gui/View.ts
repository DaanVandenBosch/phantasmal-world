import { Disposable } from "./Disposable";

export abstract class View implements Disposable {
    abstract readonly element: HTMLElement;

    private disposables: Disposable[] = [];

    protected disposable<T extends Disposable>(disposable: T): T {
        this.disposables.push(disposable);
        return disposable;
    }

    dispose(): void {
        this.element.remove();
        this.disposables.forEach(d => d.dispose());
        this.disposables.splice(0, this.disposables.length);
    }
}
