module.exports = {
    preset: "ts-jest",
    testEnvironment: "node",
    moduleDirectories: ["node_modules"],
    setupFiles: ["./test/src/setup.js"],
    moduleNameMapper: {
        "\\.(css|gif|jpg|png|svg|ttf)$": "<rootDir>/src/__mocks__/static_files.js",
    },
};
