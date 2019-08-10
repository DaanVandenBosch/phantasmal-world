import { ValueSet } from "./ValueSet";

test("empty", () => {
    const vs = new ValueSet();

    expect(vs.size()).toBe(0);
});

test("get", () => {
    const vs = new ValueSet().set_interval(10, 13).union(new ValueSet().set_interval(20, 22));

    expect(vs.size()).toBe(7);
    expect(vs.get(0)).toBe(10);
    expect(vs.get(1)).toBe(11);
    expect(vs.get(2)).toBe(12);
    expect(vs.get(3)).toBe(13);
    expect(vs.get(4)).toBe(20);
    expect(vs.get(5)).toBe(21);
    expect(vs.get(6)).toBe(22);
});

test("has", () => {
    const vs = new ValueSet().set_interval(-20, 13).union(new ValueSet().set_interval(20, 22));

    expect(vs.size()).toBe(37);
    expect(vs.has(-9001)).toBe(false);
    expect(vs.has(-21)).toBe(false);
    expect(vs.has(-20)).toBe(true);
    expect(vs.has(13)).toBe(true);
    expect(vs.has(14)).toBe(false);
    expect(vs.has(19)).toBe(false);
    expect(vs.has(20)).toBe(true);
    expect(vs.has(22)).toBe(true);
    expect(vs.has(23)).toBe(false);
    expect(vs.has(9001)).toBe(false);
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
        .union(new ValueSet().set_interval(10, 12))
        .union(new ValueSet().set_interval(14, 16));

    expect(a.size()).toBe(6);
    expect([...a]).toEqual([10, 11, 12, 14, 15, 16]);

    a.union(new ValueSet().set_interval(13, 13));

    expect(a.size()).toBe(7);
    expect([...a]).toEqual([10, 11, 12, 13, 14, 15, 16]);

    a.union(new ValueSet().set_interval(1, 2));

    expect(a.size()).toBe(9);
    expect([...a]).toEqual([1, 2, 10, 11, 12, 13, 14, 15, 16]);

    a.union(new ValueSet().set_interval(30, 32));

    expect(a.size()).toBe(12);
    expect([...a]).toEqual([1, 2, 10, 11, 12, 13, 14, 15, 16, 30, 31, 32]);

    a.union(new ValueSet().set_interval(20, 21));

    expect(a.size()).toBe(14);
    expect([...a]).toEqual([1, 2, 10, 11, 12, 13, 14, 15, 16, 20, 21, 30, 31, 32]);
});
