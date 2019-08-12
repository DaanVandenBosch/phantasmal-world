import { AssemblyLexer, FloatToken, TokenType } from "./AssemblyLexer";

test("valid floats", () => {
    const lexer = new AssemblyLexer();

    expect((lexer.tokenize_line("808.9")[0] as FloatToken).value).toBeCloseTo(808.9, 4);
    expect((lexer.tokenize_line("-0.9")[0] as FloatToken).value).toBeCloseTo(-0.9, 2);
    expect((lexer.tokenize_line("1e-3")[0] as FloatToken).value).toBeCloseTo(0.001, 4);
    expect((lexer.tokenize_line("-6e2")[0] as FloatToken).value).toBeCloseTo(-600, 3);
});

test("invalid floats", () => {
    const lexer = new AssemblyLexer();

    const tokens1 = lexer.tokenize_line(" 808.9a ");

    expect(tokens1.length).toBe(1);
    expect(tokens1[0].type).toBe(TokenType.InvalidNumber);
    expect(tokens1[0].col).toBe(2);
    expect(tokens1[0].len).toBe(6);

    const tokens2 = lexer.tokenize_line("  -55e ");

    expect(tokens2.length).toBe(1);
    expect(tokens2[0].type).toBe(TokenType.InvalidNumber);
    expect(tokens2[0].col).toBe(3);
    expect(tokens2[0].len).toBe(4);

    const tokens3 = lexer.tokenize_line(".7429");

    expect(tokens3.length).toBe(1);
    expect(tokens3[0].type).toBe(TokenType.InvalidSection);
    expect(tokens3[0].col).toBe(1);
    expect(tokens3[0].len).toBe(5);

    const tokens4 = lexer.tokenize_line("\t\t\t4. test");

    expect(tokens4.length).toBe(2);
    expect(tokens4[0].type).toBe(TokenType.InvalidNumber);
    expect(tokens4[0].col).toBe(4);
    expect(tokens4[0].len).toBe(2);
});
