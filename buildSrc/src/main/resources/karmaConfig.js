config.middleware = config.middleware || [];
config.middleware.push('resource-loader');

function ResourceLoaderMiddleware() {
    const fs = require('fs');

    return function (request, response, next) {
        try {
            const content = fs.readFileSync(PROJECT_PATH + '/build/processedResources/js/test' + decodeURI(request.originalUrl));
            response.writeHead(200);
            response.end(content);
        } catch (ignored) {
            try {
                const content = fs.readFileSync(PROJECT_PATH + '/build/processedResources/js/main' + decodeURI(request.originalUrl));
                response.writeHead(200);
                response.end(content);
            } catch (ignored) {
                next();
            }
        }
    }
}

config.plugins.push({
    'middleware:resource-loader': ['factory', ResourceLoaderMiddleware]
});
