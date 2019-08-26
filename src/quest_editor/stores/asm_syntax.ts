import { languages } from "monaco-editor";

export const ASM_SYNTAX: languages.IMonarchLanguage = {
    defaultToken: "invalid",

    tokenizer: {
        root: [
            // Strings.
            [/"([^"\\]|\\.)*$/, "string.invalid"], // Unterminated string.
            [/"/, { token: "string.quote", bracket: "@open", next: "@string" }],

            // Registers.
            [/r\d+/, "predefined"],

            // Labels.
            [/[^\s]+:/, "tag"],

            // Numbers.
            [/0x[0-9a-fA-F]+/, "number.hex"],
            [/-?\d+(\.\d+)?(e-?\d+)?/, "number.float"],
            [/-?[0-9]+/, "number"],

            // Section markers.
            [/\.[^\s]+/, "keyword"],

            // Identifiers.
            [/[a-z][a-z0-9_=<>!]*/, "identifier"],

            // Whitespace.
            [/[ \t\r\n]+/, "white"],
            // [/\/\*/, "comment", "@comment"],
            [/\/\/.*$/, "comment"],

            // Delimiters.
            [/,/, "delimiter"],
        ],

        // comment: [
        //     [/[^/*]+/, "comment"],
        //     [/\/\*/, "comment", "@push"], // Nested comment.
        //     [/\*\//, "comment", "@pop"],
        //     [/[/*]/, "comment"],
        // ],

        string: [
            [/[^\\"]+/, "string"],
            [/\\(?:[n\\"])/, "string.escape"],
            [/\\./, "string.escape.invalid"],
            [/"/, { token: "string.quote", bracket: "@close", next: "@pop" }],
        ],
    },
};
