const MonacoWebpackPlugin = require("monaco-editor-webpack-plugin");

config.module.rules.push({
    test: /\.(gif|jpg|png|svg|ttf)$/,
    loader: "file-loader",
});

// Optimize Monaco Editor - minimal feature set
config.plugins.push(
    new MonacoWebpackPlugin({
        languages: [], // Don't preload any languages
        features: [
            'coreCommands' // Include only core functionality
        ]
    })
);

// Code splitting optimization - different strategies per environment
config.optimization = config.optimization || {};

// Check if we're in production mode
const isProduction = config.mode === 'production';

if (isProduction) {
    // Production: Conservative code splitting to ensure app stability
    config.optimization.splitChunks = {
        chunks: 'async', // Only split async chunks, keep sync deps in main bundle
        minSize: 200000, // Split chunks over 200KB to avoid over-fragmentation
        maxAsyncRequests: 5,
        cacheGroups: {
            // Only separate the largest dependency (Monaco Editor)
            monaco: {
                test: /[\\/]node_modules[\\/]monaco-editor[\\/]/,
                name: 'monaco',
                chunks: 'async',
                priority: 30,
                enforce: true
            },
            // Disable other splitting to ensure stability
            default: false,
            vendors: false
        }
    };
} else {
    // Development: Minimal splitting for faster builds and simpler debugging
    config.optimization.splitChunks = {
        chunks: 'async',
        minSize: 500000, // Only split chunks over 500KB
        maxAsyncRequests: 3,
        cacheGroups: {
            monaco: {
                test: /[\\/]node_modules[\\/]monaco-editor[\\/]/,
                name: 'monaco',
                chunks: 'async',
                priority: 30,
                enforce: true
            },
            default: false,
            vendors: false
        }
    };
}

// Enable all optimizations
config.optimization.usedExports = true;
config.optimization.sideEffects = false;
// Keep runtime in main bundle to maintain web.js as primary entry point
// config.optimization.runtimeChunk = 'single';

// Performance budget warnings - realistic values for this project
config.performance = {
    maxAssetSize: isProduction ? 2000000 : 10000000, // Production: 2MB, Development: 10MB
    maxEntrypointSize: isProduction ? 6500000 : 40000000, // Production: 6.5MB, Development: 40MB
    hints: 'warning'
};
