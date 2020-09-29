const fs = require("fs");

require("dotenv").config({
    path: fs.existsSync(".env.test.local") ? ".env.test.local" : ".env.test",
});

// For GoldenLayout.
window.$ = require("jquery");
