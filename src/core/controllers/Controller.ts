import { Disposable } from "../observable/Disposable";
import { Disposer } from "../observable/Disposer";

export abstract class Controller implements Disposable {
    private readonly disposer = new Disposer();

    dispose(): void {
        this.disposer.dispose();
    }

    protected disposable<T extends Disposable>(disposable: T): T {
        return this.disposer.add(disposable);
    }

    protected disposables(...disposables: Disposable[]): void {
        this.disposer.add_all(...disposables);
    }
}
