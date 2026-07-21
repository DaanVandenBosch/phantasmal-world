const MonacoWebpackPlugin = require("monaco-editor-webpack-plugin");
const CompressionPlugin = require("compression-webpack-plugin");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const path = require("path");
const zlib = require("zlib");

config.module.rules.push({
    test: /\.(gif|jpg|png|svg|ttf)$/,
    type: 'asset/resource',
    generator: {
        filename: 'fonts/[name][ext][query]'
    }
});

// Only ship Monaco features the quest editor actually uses. Features prefixed
// with "!" are excluded; everything else stays included by default. Audit:
// AsmStore.kt only registers hover, completion, signature help, definition,
// document symbol, and document highlight providers, plus model markers.
// The excluded features below have no registered providers and ship dead UI.
config.plugins.push(
    new MonacoWebpackPlugin({
        languages: [],
        features: [
            "!anchorSelect",
            "!codeAction",
            "!codelens",
            "!colorPicker",
            "!diffEditor",
            "!dropIntoEditor",
            "!fontZoom",
            "!format",
            "!iPadShowKeyboard",
            "!inPlaceReplace",
            "!inlayHints",
            "!inlineCompletions",
            "!inspectTokens",
            "!linkedEditing",
            "!links",
            "!referenceSearch",
            "!rename",
            "!stickyScroll",
            "!toggleHighContrast",
            "!toggleTabFocusMode",
            "!unicodeHighlighter",
            "!unusualLineTerminators",
            "!viewportSemanticTokens",
        ],
    })
);

// Let webpack own index.html: inject <script> tags for every entry chunk
// (including the ones created by splitChunks below), so we don't have to
// hand-maintain the tag list when chunking changes. The source template at
// src/jsMain/resources/index.html has no hardcoded <script src="web.js">.
config.plugins.push(
    new HtmlWebpackPlugin({
        // The source template is shipped as "index.template.html" so it does
        // not collide with webpack's own "index.html" output when Kotlin's
        // jsBrowserDistribution task copies both processedResources and the
        // webpack output into dist/.
        template: path.resolve(__dirname, "kotlin/index.template.html"),
        filename: "index.html",
        inject: "head",
        scriptLoading: "defer",
        minify: config.mode === "production" ? {
            collapseWhitespace: true,
            removeComments: true,
            removeRedundantAttributes: true,
        } : false,
    })
);

// Vendor chunk splitting: pull big third-party libs out of web.js so they
// can be cached independently and download in parallel over HTTP/2. This is
// the canonical webpack pattern for SPAs — total first-load bytes are the
// same, but per-chunk caching means app-only changes don't invalidate the
// ~3 MB of vendor code, and HTTP/2 multiplexes the downloads.
//
// Scope note: we only split *initial* chunks. Touching async chunks would
// hoist code that monaco-editor-webpack-plugin already lazy-loads (worker
// language modes) back into the sync path, inflating first-load rather than
// shrinking it.
config.optimization = Object.assign({}, config.optimization, {
    runtimeChunk: "single",
    splitChunks: {
        chunks: "initial",
        maxInitialRequests: 20,
        cacheGroups: {
            three: {
                test: /[\\/]node_modules[\\/]three[\\/]/,
                name: "vendor-three",
                chunks: "initial",
                priority: 30,
                enforce: true,
            },
            monaco: {
                test: /[\\/]node_modules[\\/]monaco-editor[\\/]/,
                name: "vendor-monaco",
                chunks: "initial",
                priority: 30,
                enforce: true,
            },
            fortawesome: {
                test: /[\\/]node_modules[\\/]@fortawesome[\\/]/,
                name: "vendor-fortawesome",
                chunks: "initial",
                priority: 30,
                enforce: true,
            },
            goldenLayout: {
                test: /[\\/]node_modules[\\/](golden-layout|jquery)[\\/]/,
                name: "vendor-golden-layout",
                chunks: "initial",
                priority: 30,
                enforce: true,
            },
            lpSolver: {
                test: /[\\/]node_modules[\\/]javascript-lp-solver[\\/]/,
                name: "vendor-lp-solver",
                chunks: "initial",
                priority: 30,
                enforce: true,
            },
            vendors: {
                test: /[\\/]node_modules[\\/]/,
                name: "vendor-misc",
                chunks: "initial",
                priority: 10,
                minChunks: 1,
                reuseExistingChunk: true,
            },
            default: {
                minChunks: 2,
                priority: -20,
                chunks: "initial",
                reuseExistingChunk: true,
            },
        },
    },
});

// Pre-compress production assets so the dev/CDN server can serve .br / .gz
// directly via Content-Encoding without re-compressing per request. Industry
// standard for static SPA delivery; zero runtime impact if the server ignores
// them. Only emitted in production builds (mode === "production").
if (config.mode === "production") {
    config.plugins.push(
        new CompressionPlugin({
            filename: "[path][base].br",
            algorithm: "brotliCompress",
            test: /\.(js|css|html|svg|json|wasm)$/,
            compressionOptions: {
                params: {
                    [zlib.constants.BROTLI_PARAM_QUALITY]: 11,
                },
            },
            threshold: 1024,
            minRatio: 0.9,
        }),
        new CompressionPlugin({
            filename: "[path][base].gz",
            algorithm: "gzip",
            test: /\.(js|css|html|svg|json|wasm)$/,
            compressionOptions: { level: 9 },
            threshold: 1024,
            minRatio: 0.9,
        }),
    );
}

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
