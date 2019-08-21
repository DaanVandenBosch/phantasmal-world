const data_view = new DataView(new ArrayBuffer(4));

export function reinterpret_i32_as_f32(i32: number): number {
    data_view.setInt32(0, i32);
    return data_view.getFloat32(0);
}

export function reinterpret_f32_as_i32(f32: number): number {
    data_view.setFloat32(0, f32);
    return data_view.getInt32(0);
}
