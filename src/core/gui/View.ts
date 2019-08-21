import { Disposable } from "../observable/Disposable";

export abstract class View implements Disposable {
    abstract readonly element: HTMLElement;

    get id(): string {
        return this.element.id;
    }

    set id(id: string) {
        this.element.id = id;
    }

    private disposable_list: Disposable[] = [];

    protected disposable<T extends Disposable>(disposable: T): T {
        this.disposable_list.push(disposable);
        return disposable;
    }

    protected disposables(...disposables: Disposable[]): void {
        this.disposable_list.push(...disposables);
    }

    dispose(): void {
        this.element.remove();
        this.disposable_list.splice(0, this.disposable_list.length).forEach(d => d.dispose());
    }
}
