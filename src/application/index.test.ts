import { initialize_application } from "./index";
import { FileSystemHttpClient } from "../../test/src/core/FileSystemHttpClient";
import { pw_test, timeout } from "../../test/src/utils";
import { Random } from "../core/Random";
import { Severity } from "../core/Severity";
import { StubClock } from "../../test/src/core/StubClock";
import { STUB_RENDERER } from "../../test/src/core/rendering/StubRenderer";

for (const path of [undefined, "/viewer", "/quest_editor", "/hunt_optimizer"]) {
    const with_path = path == undefined ? "without specific path" : `with path ${path}`;

    test(
        `Initialization and shutdown ${with_path} should succeed without throwing or logging errors.`,
        pw_test({ max_log_severity: Severity.Warning }, async disposer => {
            if (path != undefined) {
                window.location.hash = path;
            }

            const app = disposer.add(
                initialize_application(
                    new FileSystemHttpClient(),
                    new Random(() => 0.27),
                    new StubClock(new Date("2020-01-01T15:40:20Z")),
                    () => STUB_RENDERER,
                ),
            );

            expect(app).toBeDefined();

            await timeout(1000);
        }),
    );
}
