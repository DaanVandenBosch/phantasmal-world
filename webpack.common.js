const path = require("path");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const MonacoWebpackPlugin = require("monaco-editor-webpack-plugin");
const { ProvidePlugin } = require("webpack");

module.exports = {
    entry: "./src/index.ts",
    output: {
        path: path.resolve(__dirname, "dist"),
    },
    resolve: {
        extensions: [".js", ".ts"],
    },
    module: {
        rules: [
            {
                test: /\.(gif|jpg|png|svg|ttf)$/,
                loader: "file-loader",
            },
        ],
    },
    plugins: [
        new HtmlWebpackPlugin({
            title: "Phantasmal World",
        }),
        new MonacoWebpackPlugin({
            languages: [],
        }),
        new ProvidePlugin({
            $: "jquery",
            jQuery: "jquery",
        }),
    ],
};
