import { enum_values } from "./enums";

enum Test {
    TestA,
    TestB,
    TestC,
}

enum TestString {
    TestA,
    TestB,
    TestC,
}

test("enum_values of integer enum", () => {
    const values = enum_values(Test);

    expect(values.length).toBe(3);
    expect(values[0]).toBe(Test.TestA);
    expect(values[1]).toBe(Test.TestB);
    expect(values[2]).toBe(Test.TestC);
});

test("enum_values of string enum", () => {
    const values = enum_values(TestString);

    expect(values.length).toBe(3);
    expect(values[0]).toBe(TestString.TestA);
    expect(values[1]).toBe(TestString.TestB);
    expect(values[2]).toBe(TestString.TestC);
});
