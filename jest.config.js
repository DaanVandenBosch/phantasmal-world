module.exports = {
    preset: "ts-jest",
    moduleDirectories: ["node_modules"],
    setupFiles: ["./test/src/setup.js"],
    roots: ["./src", "./test"],
    moduleNameMapper: {
        "\\.(css|gif|jpg|png|svg|ttf)$": "<rootDir>/src/__mocks__/static_files.js",
        "^monaco-editor$": "<rootDir>/node_modules/monaco-editor/esm/vs/editor/editor.main.js",
        "^worker-loader!": "<rootDir>/src/__mocks__/webworkers.js",
    },
};
