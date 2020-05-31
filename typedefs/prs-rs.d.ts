/**
 * @file This file is needed because prs-rs is an optional module and typescript will complain if code refers to an undeclared module.
 */

declare module "prs-rs" {
    /**
     * @param {Uint8Array} data
     * @returns {Uint8Array}
     */
    export function compress(data: Uint8Array): Uint8Array;
    /**
     * @param {Uint8Array} data
     * @returns {Uint8Array}
     */
    export function decompress(data: Uint8Array): Uint8Array;
}
