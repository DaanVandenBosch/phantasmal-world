import { Widget, WidgetOptions } from "./Widget";

export type ControlOptions = WidgetOptions;

/**
 * Represents all widgets that allow for user interaction such as buttons, text inputs, combo boxes,
 * etc.
 */
export abstract class Control extends Widget {}
