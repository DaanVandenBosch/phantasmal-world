const MonacoWebpackPlugin = require("monaco-editor-webpack-plugin");

config.module.rules.push({
    test: /\.(gif|jpg|png|svg|ttf)$/,
    type: 'asset/resource',
    generator: {
        filename: 'fonts/[name][ext][query]'
    }
});

config.plugins.push(
    new MonacoWebpackPlugin({
        languages: [],
    })
);
