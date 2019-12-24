const common = require("./webpack.common.js");
const path = require("path");
const merge = require("webpack-merge");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const Dotenv = require("dotenv-webpack");
const { CleanWebpackPlugin } = require("clean-webpack-plugin");
const CopyWebpackPlugin = require("copy-webpack-plugin");

module.exports = merge(common, {
    mode: "production",
    output: {
        filename: "[name].[contenthash].js",
    },
    optimization: {
        moduleIds: "hashed",
        runtimeChunk: "single",
        splitChunks: {
            cacheGroups: {
                styles: {
                    name: "style",
                    test: /\.css$/,
                    chunks: "all",
                    enforce: true,
                },
                vendor: {
                    test: /node_modules/,
                    name: "vendors",
                    chunks: "all",
                },
            },
        },
    },
    module: {
        rules: [
            {
                test: /\.ts$/,
                loader: "ts-loader",
                include: path.resolve(__dirname, "src"),
            },
            {
                test: /\.css$/,
                use: [MiniCssExtractPlugin.loader, "css-loader"],
            },
        ],
    },
    plugins: [
        new CleanWebpackPlugin(),
        new Dotenv({
            path: "./.env.prod",
        }),
        new MiniCssExtractPlugin({
            ignoreOrder: true,
            filename: "[name].[contenthash].css",
        }),
        new CopyWebpackPlugin([
            {
                from: path.resolve(__dirname, "assets"),
                to: path.resolve(__dirname, "dist/assets"),
            },
        ]),
    ],
});
