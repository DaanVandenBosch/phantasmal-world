export async function load_array_buffer(url: string): Promise<ArrayBuffer> {
    const base_url = process.env.PUBLIC_URL;
    const response = await fetch(base_url + url);
    return response.arrayBuffer();
}
