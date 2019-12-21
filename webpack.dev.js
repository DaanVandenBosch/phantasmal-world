const common = require("./webpack.common.js");
const path = require("path");
const merge = require("webpack-merge");
const ForkTsCheckerWebpackPlugin = require("fork-ts-checker-webpack-plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const Dotenv = require("dotenv-webpack");

module.exports = merge(common, {
    mode: "development",
    devtool: "eval-source-map",
    module: {
        rules: [
            {
                test: /\.ts$/,
                use: [
                    {
                        loader: "ts-loader",
                        options: {
                            // fork-ts-checker-webpack-plugin does the type checking in a separate process.
                            transpileOnly: true,
                        },
                    },
                ],
                include: path.resolve(__dirname, "src"),
            },
            {
                test: /\.css$/,
                exclude: /node_modules/,
                use: [
                    {
                        loader: MiniCssExtractPlugin.loader,
                        options: {
                            hmr: true,
                        },
                    },
                    {
                        loader: "css-loader",
                        options: {
                            sourceMap: true,
                        },
                    },
                ],
            },
            {
                test: /\.css$/,
                include: /node_modules/,
                use: [
                    MiniCssExtractPlugin.loader,
                    {
                        loader: "css-loader",
                        options: {
                            sourceMap: true,
                        },
                    },
                ],
            },
            {
                test: /\.(gif|jpg|png|svg|ttf)$/,
                use: ["file-loader"],
            },
        ],
    },
    plugins: [
        new Dotenv({
            path: "./.env.dev",
        }),
        new ForkTsCheckerWebpackPlugin(),
        new MiniCssExtractPlugin(),
    ],
});
