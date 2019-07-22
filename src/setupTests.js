import Logger from "js-logger";

Logger.useDefaults({
    defaultLevel: Logger[process.env["REACT_APP_LOG_LEVEL"] || "OFF"],
});
