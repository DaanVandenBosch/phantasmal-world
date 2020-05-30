use std::io::{Read, Write, Cursor};
use wasm_bindgen::prelude::*;
use ages_prs::{LegacyPrsEncoder, LegacyPrsDecoder};

// When the `wee_alloc` feature is enabled, use `wee_alloc` as the global
// allocator.
#[cfg(feature = "wee_alloc")]
#[global_allocator]
static ALLOC: wee_alloc::WeeAlloc = wee_alloc::WeeAlloc::INIT;

#[wasm_bindgen]
pub fn compress(data: Vec<u8>) -> Vec<u8> {
    let mut enc = LegacyPrsEncoder::new(Vec::new());
    enc.write_all(&data).unwrap();
    return enc.into_inner().unwrap();
}

#[wasm_bindgen]
pub fn decompress(data: Vec<u8>) -> Vec<u8> {
    let mut dec = LegacyPrsDecoder::new(Cursor::new(&data));
    let mut result = Vec::new();
    dec.read_to_end(&mut result).unwrap();
    return result;
}
