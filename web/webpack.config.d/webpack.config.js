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

// Suppress benign ResizeObserver warnings in dev overlay.
if (config.devServer) {
    config.devServer.client = config.devServer.client || {};
    config.devServer.client.overlay = {
        runtimeErrors: (error) => {
            if (error && error.message &&
                error.message.includes('ResizeObserver loop')) {
                return false;
            }
            return true;
        },
    };
}
