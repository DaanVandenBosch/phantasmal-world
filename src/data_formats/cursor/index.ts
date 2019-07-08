// TODO: remove dependency on text-encoding because it is no longer maintained.
import { TextDecoder, TextEncoder } from "text-encoding";

export const ASCII_DECODER = new TextDecoder("ascii");
export const UTF_16BE_DECODER = new TextDecoder("utf-16be");
export const UTF_16LE_DECODER = new TextDecoder("utf-16le");

export const ASCII_ENCODER = new TextEncoder("ascii");
export const UTF_16BE_ENCODER = new TextEncoder("utf-16be");
export const UTF_16LE_ENCODER = new TextEncoder("utf-16le");
