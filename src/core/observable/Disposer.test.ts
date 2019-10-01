import { Disposer } from "./Disposer";
import { Disposable } from "./Disposable";

test("calling add or add_all should increase length correctly", () => {
    const disposer = new Disposer();
    expect(disposer.length).toBe(0);

    disposer.add(dummy());
    expect(disposer.length).toBe(1);

    disposer.add_all(dummy(), dummy());
    expect(disposer.length).toBe(3);

    disposer.add(dummy());
    expect(disposer.length).toBe(4);

    disposer.add_all(dummy(), dummy());
    expect(disposer.length).toBe(6);
});

test("length should be 0 after calling dispose", () => {
    const disposer = new Disposer();
    disposer.add_all(dummy(), dummy(), dummy());
    expect(disposer.length).toBe(3);

    disposer.dispose();
    expect(disposer.length).toBe(0);
});

test("contained disposables should be disposed when calling dispose", () => {
    let dispose_calls = 0;

    function disposable(): Disposable {
        return {
            dispose(): void {
                dispose_calls++;
            },
        };
    }

    const disposer = new Disposer();
    disposer.add_all(disposable(), disposable(), disposable());
    expect(dispose_calls).toBe(0);

    disposer.dispose();
    expect(dispose_calls).toBe(3);
});

function dummy(): Disposable {
    return {
        dispose(): void {
            // Do nothing.
        },
    };
}
