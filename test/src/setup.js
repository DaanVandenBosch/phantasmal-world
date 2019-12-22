const Logger = require("js-logger");
require('dotenv').config({ path: ".env.test" })

const log_level = process.env["LOG_LEVEL"] || "WARN";

Logger.useDefaults({
    defaultLevel: Logger[log_level],
});
