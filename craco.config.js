const CracoAntDesignPlugin = require("craco-antd");
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin');
const webpack = require("webpack")

module.exports = {
    plugins: [
        { plugin: CracoAntDesignPlugin },
        {
            plugin: new MonacoWebpackPlugin({
                languages: []
            })
        }
    ],
    eslint: {
        mode: "file"
    },
    webpack: {
        configure: config => {
            // golden-layout config.
            config.plugins.push(new webpack.ProvidePlugin({
                React: "react",
                ReactDOM: "react-dom",
                $: "jquery",
                jQuery: "jquery",
            }));

            // worker-loader config.
            config.module.rules.push({
                test: /\.worker\.js$/,
                use: { loader: 'worker-loader' }
            });

            // Work-around until create-react-app uses webpack-dev-server 4.
            // See https://github.com/webpack/webpack/issues/6642
            config.output.globalObject = "this";

            return config;
        }
    }
};
