const CracoAntDesignPlugin = require("craco-antd");
const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin');

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
