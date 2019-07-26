const path = require("path");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin');
const { ProvidePlugin } = require("webpack");

module.exports = {
    entry: "./src/index.tsx",
    output: {
        path: path.resolve(__dirname, "dist")
    },
    resolve: {
        extensions: [".js", ".ts", ".tsx"]
    },
    plugins: [
        new HtmlWebpackPlugin({
            title: "Phantasmal World"
        }),
        new MonacoWebpackPlugin({
            languages: []
        }),
        new ProvidePlugin({
            React: "react",
            ReactDOM: "react-dom",
            $: "jquery",
            jQuery: "jquery",
        })
    ]
};
