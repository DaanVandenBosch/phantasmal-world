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
    }
};
