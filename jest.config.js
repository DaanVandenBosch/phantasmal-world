module.exports = {
    preset: 'ts-jest',
    testEnvironment: 'node',
    moduleDirectories: ["node_modules"],
    globalSetup: "./test/src/global_setup.js",
};