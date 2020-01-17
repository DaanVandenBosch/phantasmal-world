import { NavigationController } from "./NavigationController";
import { GuiStore } from "../../core/stores/GuiStore";
import { StubClock } from "../../../test/src/core/StubClock";

test("Internet time should be calculated correctly.", () => {
    for (const [time, beats] of [
        ["00:00:00", 41],
        ["13:10:12", 590],
        ["22:59:59", 999],
        ["23:00:00", 0],
        ["23:59:59", 41],
    ]) {
        const ctrl = new NavigationController(
            new GuiStore(),
            new StubClock(new Date(`2020-01-01T${time}Z`)),
        );

        expect(ctrl.internet_time.val).toBe(`@${beats}`);
    }
});
