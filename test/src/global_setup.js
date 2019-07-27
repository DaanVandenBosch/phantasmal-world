const Logger = require("js-logger");
require('dotenv').config({ path: ".env.test" })

module.exports = async () => {
    const log_level = process.env["LOG_LEVEL"] || "OFF";

    console.log(`Log level: ${log_level}`);

    Logger.useDefaults({
        defaultLevel: Logger[log_level],
    });
};
