import { initialize_application } from "./index";
import { LogHandler, LogManager } from "../core/Logger";
import { FileSystemHttpClient } from "../../test/src/core/FileSystemHttpClient";
import { timeout } from "../../test/src/utils";
import { StubThreeRenderer } from "../../test/src/core/rendering/StubThreeRenderer";
import { Random } from "../core/Random";
import { Severity } from "../core/Severity";

for (const path of [undefined, "/viewer", "/quest_editor", "/hunt_optimizer"]) {
    const with_path = path == undefined ? "without specific path" : `with path ${path}`;

    test(`Initialization and shutdown ${with_path} should succeed without throwing or logging errors.`, async () => {
        const logged_errors: string[] = [];

        const handler: LogHandler = ({ severity, message }) => {
            if (severity >= Severity.Error) {
                logged_errors.push(message);
            }
        };

        return LogManager.with_default_handler(handler, async () => {
            if (path != undefined) {
                window.location.hash = path;
            }

            const app = initialize_application(
                new FileSystemHttpClient(),
                new Random(() => 0.27),
                () => new StubThreeRenderer(),
            );

            expect(app).toBeDefined();
            expect(logged_errors).toEqual([]);

            await timeout(1000);

            app.dispose();

            expect(logged_errors).toEqual([]);
        });
    });
}
