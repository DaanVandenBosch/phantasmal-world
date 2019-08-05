const common = require("./webpack.common.js");
const antd_theme = require("./antd_theme.js");
const path = require("path");
const merge = require("webpack-merge");
const ForkTsCheckerWebpackPlugin = require("fork-ts-checker-webpack-plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const Dotenv = require("dotenv-webpack");

module.exports = merge(common, {
    mode: "development",
    devtool: "inline-cheap-source-map",
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                use: [{
                    loader: "ts-loader",
                    options: {
                        // fork-ts-checker-webpack-plugin does the type checking in a seperate process.
                        transpileOnly: true,
                    }
                }],
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
                        }
                    },
                    {
                        loader: "css-loader",
                        options: {
                            sourceMap: true,
                            modules: {
                                localIdentName: '[path][name]__[local]',
                            },
                        }
                    },
                ]
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
                        }
                    },
                ]
            },
            {
                test: /\.less$/,
                include: /antd/,
                use: [
                    MiniCssExtractPlugin.loader,
                    {
                        loader: "css-loader",
                        options: {
                            sourceMap: true,
                        }
                    },
                    {
                        loader: "less-loader",
                        options: {
                            sourceMap: true,
                            javascriptEnabled: true,
                            modifyVars: antd_theme,
                        }
                    },
                ]
            },
            {
                test: /\.(png|svg|jpg|gif)$/,
                use: ["file-loader"]
            },
            {
                test: /\.worker\.js$/,
                use: { loader: 'worker-loader' }
            },
        ]
    },
    plugins: [
        new Dotenv({
            path: "./.env.dev",
        }),
        new ForkTsCheckerWebpackPlugin(),
        new MiniCssExtractPlugin(),
    ]
});
