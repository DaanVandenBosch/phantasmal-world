const MonacoWebpackPlugin = require("monaco-editor-webpack-plugin");

config.module.rules.push({
    test: /\.(gif|jpg|png|svg|ttf)$/,
    loader: "file-loader",
});

config.plugins.push(
    new MonacoWebpackPlugin({
        languages: [],
    })
);
