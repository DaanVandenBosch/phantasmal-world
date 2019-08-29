import { Widget } from "./Widget";

export abstract class Control<E extends HTMLElement = HTMLElement> extends Widget<E> {}
