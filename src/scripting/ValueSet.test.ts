import { ValueSet } from "./ValueSet";

test("empty", () => {
    const vs = new ValueSet();

    expect(vs.size()).toBe(0);
});

test("set_value", () => {
    const vs = new ValueSet();
    vs.set_value(100);
    vs.set_value(4);
    vs.set_value(24324);

    expect(vs.size()).toBe(1);
    expect([...vs]).toEqual([24324]);
});

test("union", () => {
    const c = new ValueSet()
        .union(new ValueSet().set_value(21))
        .union(new ValueSet().set_value(4968));

    expect(c.size()).toBe(2);
    expect([...c]).toEqual([21, 4968]);
});

test("union of intervals", () => {
    const a = new ValueSet()
        .union(new ValueSet().set_interval(10, 13))
        .union(new ValueSet().set_interval(14, 17));

    expect(a.size()).toBe(6);
    expect([...a]).toEqual([10, 11, 12, 14, 15, 16]);

    a.union(new ValueSet().set_interval(13, 14));

    expect(a.size()).toBe(7);
    expect([...a]).toEqual([10, 11, 12, 13, 14, 15, 16]);

    a.union(new ValueSet().set_interval(1, 3));

    expect(a.size()).toBe(9);
    expect([...a]).toEqual([1, 2, 10, 11, 12, 13, 14, 15, 16]);

    a.union(new ValueSet().set_interval(30, 33));

    expect(a.size()).toBe(12);
    expect([...a]).toEqual([1, 2, 10, 11, 12, 13, 14, 15, 16, 30, 31, 32]);

    a.union(new ValueSet().set_interval(20, 22));

    expect(a.size()).toBe(14);
    expect([...a]).toEqual([1, 2, 10, 11, 12, 13, 14, 15, 16, 20, 21, 30, 31, 32]);
});
