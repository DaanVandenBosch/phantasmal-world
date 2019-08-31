import { Widget, WidgetOptions } from "./Widget";

export type ControlOptions = WidgetOptions;

export abstract class Control<E extends HTMLElement = HTMLElement> extends Widget<E> {}
