import { FlatMappedProperty } from "./FlatMappedProperty";
import { SimpleProperty } from "./SimpleProperty";
import { with_disposable } from "../../../../test/src/core/observables/disposable_helpers";

// This is a regression test, it's important that this exact sequence of statements stays the same.
test(`It should emit a change when its direct property dependency changes.`, () => {
    // p is the direct property dependency.
    const p = new SimpleProperty(new SimpleProperty(7));
    const fp = new FlatMappedProperty([p], () => p.val);
    let v: number | undefined;

    with_disposable(
        fp.observe(({ value }) => (v = value)),
        () => {
            expect(v).toBeUndefined();

            p.val.val = 99;

            expect(v).toBe(99);

            p.val = new SimpleProperty(7);

            expect(v).toBe(7);
        },
    );
});
