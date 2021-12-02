function ResourceLoaderMiddleware() {
    const fs = require('fs/promises');
    const cache = new Map;

    return async function (request, response, next) {
        const path = decodeURI(request.originalUrl);
        const isJson = path.slice(-5) === '.json';

        function sendContent(content) {
            if (isJson) {
                response.setHeader('Content-Type', 'application/json');
            }

            response.writeHead(200);

            response.end(content);
        }

        async function tryBasePath(basePath) {
            const content = await fs.readFile(PROJECT_PATH + basePath + path)
            cache.set(path, content);
            sendContent(content);
        }

        const cached = cache.get(path);

        if (cached) {
            sendContent(cached);
            return;
        }

        try {
            await tryBasePath('/build/processedResources/js/test');
        } catch (ignored) {
            try {
                await tryBasePath('/build/processedResources/js/main');
            } catch (ignored) {
                next();
            }
        }
    }
}

config.plugins.push({
    'middleware:resource-loader': ['factory', ResourceLoaderMiddleware]
});

config.middleware = config.middleware || [];
config.middleware.push('resource-loader');
