(window.webpackJsonp=window.webpackJsonp||[]).push([[6],{"4/Uj":function(t,i,e){"use strict";e.r(i);var s=e("ouMO"),r=e("yub1"),n=e("Sa3/"),o=e("NRxM"),h=e("bcBH"),u=e("kcKQ"),a=e("rwco"),_=e("wtpc"),f=e("iR5r");const c=a.c.get("core/data_formats/parsing/afs"),d=5457473;e.d(i,"TextureStore",(function(){return w}));var l=function(t,i,e,s){return new(e||(e=Promise))((function(r,n){function o(t){try{u(s.next(t))}catch(t){n(t)}}function h(t){try{u(s.throw(t))}catch(t){n(t)}}function u(t){var i;t.done?r(t.value):(i=t.value,i instanceof e?i:new e((function(t){t(i)}))).then(o,h)}u((s=s.apply(t,i||[])).next())}))};const p=a.c.get("viewer/stores/TextureStore");class w extends u.a{constructor(){super(...arguments),this._textures=Object(s.c)(),this.textures=this._textures,this.load_file=t=>l(this,void 0,void 0,(function*(){try{const i=Object(_.f)(t.name).toLowerCase(),e=yield Object(n.a)(t);if("xvm"===i){const t=Object(r.a)(new o.a(e,h.a.Little));this._textures.splice(0,1/0,...t.textures.map(t=>{let i=void 0;try{i=Object(f.b)(t)}catch(t){p.error("Couldn't convert XVR texture.",t)}return{texture:i,width:t.width,height:t.height}}))}else if("afs"===i){const t=function(t){if(t.u32()!==d)return c.error("Not an AFS archive."),[];const i=t.u16();t.seek(2);const e=[];for(let s=0;s<i;s++){const i=t.u32(),s=t.u32();e.push({offset:i,size:s})}const s=[];for(const{offset:i,size:r}of e)t.seek_start(i),s.push(t.array_buffer(r));return s}(new o.a(e,h.a.Little)),i=[];for(const e of t){const t=Object(r.a)(new o.a(e,h.a.Little));for(const e of t.textures){let t=void 0;try{t=Object(f.b)(e)}catch(t){p.error("Couldn't convert XVR texture.",t)}i.push({texture:t,width:e.width,height:e.height})}}this._textures.splice(0,1/0,...i)}}catch(t){p.error("Couldn't read file.",t)}}))}}},"9Ykw":function(t,i,e){"use strict";function s(t){const i=[];for(;t.bytes_left;){const e=t.u32(),s=t.u32();if(s>t.bytes_left)break;i.push({type:e,data:t.take(s)})}return i}e.d(i,"a",(function(){return s}))},"9dYx":function(t,i,e){"use strict";var s=e("bcBH");class r{constructor(t,i){this._position=0,this.endianness=t,this.offset=i}get position(){return this._position}get endianness(){return this.little_endian?s.a.Little:s.a.Big}set endianness(t){this.little_endian=t===s.a.Little}get bytes_left(){return this.size-this.position}seek(t){return this.seek_start(this.position+t)}seek_start(t){if(t<0||t>this.size)throw new Error(`Offset ${t} is out of bounds.`);return this._position=t,this}seek_end(t){if(t<0||t>this.size)throw new Error(`Offset ${t} is out of bounds.`);return this._position=this.size-t,this}u8(){return this.u8_at(this._position++)}u8_at(t){return this.check_offset(t,1),this.dv.getUint8(this.offset+t)}u16(){const t=this.u16_at(this.position);return this._position+=2,t}u16_at(t){return this.check_offset(t,2),this.dv.getUint16(this.offset+t,this.little_endian)}u32(){const t=this.u32_at(this.position);return this._position+=4,t}u32_at(t){return this.check_offset(t,4),this.dv.getUint32(this.offset+t,this.little_endian)}i8(){return this.i8_at(this._position++)}i8_at(t){return this.check_offset(t,1),this.dv.getInt8(this.offset+t)}i16(){const t=this.i16_at(this.position);return this._position+=2,t}i16_at(t){return this.check_offset(t,2),this.dv.getInt16(this.offset+t,this.little_endian)}i32(){const t=this.i32_at(this.position);return this._position+=4,t}i32_at(t){return this.check_offset(t,4),this.dv.getInt32(this.offset+t,this.little_endian)}f32(){const t=this.f32_at(this.position);return this._position+=4,t}f32_at(t){return this.check_offset(t,4),this.dv.getFloat32(this.offset+t,this.little_endian)}u8_array(t){this.check_size("n",t,t);const i=[];for(let e=0;e<t;++e)i.push(this.dv.getUint8(this.offset+this._position++));return i}u16_array(t){this.check_size("n",t,2*t);const i=[];for(let e=0;e<t;++e)i.push(this.dv.getUint16(this.offset+this.position,this.little_endian)),this._position+=2;return i}u32_array(t){this.check_size("n",t,4*t);const i=[];for(let e=0;e<t;++e)i.push(this.dv.getUint32(this.offset+this.position,this.little_endian)),this._position+=4;return i}i32_array(t){this.check_size("n",t,4*t);const i=[];for(let e=0;e<t;++e)i.push(this.dv.getInt32(this.offset+this.position,this.little_endian)),this._position+=4;return i}vec2_f32(){return{x:this.f32(),y:this.f32()}}vec3_f32(){return{x:this.f32(),y:this.f32(),z:this.f32()}}string_ascii(t,i,e){const s=[];for(let r=0;r<t;r++){const n=this.u8();if(i&&0===n){e&&this.seek(t-r-1);break}s.push(n)}return String.fromCodePoint(...s)}string_utf16(t,i,e){const s=[],r=Math.floor(t/2);for(let t=0;t<r;t++){const n=this.u16();if(i&&0===n){e&&this.seek(2*(r-t-1));break}s.push(n)}return String.fromCodePoint(...s)}string_ascii_at(t,i,e){const s=[];for(let r=0;r<i;r++){const i=this.u8_at(t+r);if(e&&0===i)break;s.push(i)}return String.fromCodePoint(...s)}string_utf16_at(t,i,e){const s=[],r=Math.floor(i/2);for(let i=0;i<r;i++){const r=this.u16_at(t+2*i);if(e&&0===r)break;s.push(r)}return String.fromCodePoint(...s)}array_buffer(t=this.size-this.position){this.check_size("size",t,t);const i=this.backing_buffer.slice(this.offset+this.position,this.offset+this.position+t);return this._position+=t,i}copy_to_uint8_array(t,i=this.size-this.position){return this.check_size("size",i,i),t.set(new Uint8Array(this.backing_buffer,this.offset+this.position,i)),this._position+=i,this}check_size(t,i,e){if(e<0||e>this.size-this.position)throw new Error(`${t} ${i} is out of bounds.`)}check_offset(t,i){if(t<0||t+i>this.size)throw new Error(`Offset ${t} is out of bounds.`)}}e.d(i,"a",(function(){return n}));class n extends r{write_u8(t){return this.write_u8_at(this.position,t),this._position+=1,this}write_u16(t){return this.write_u16_at(this.position,t),this._position+=2,this}write_u32(t){return this.write_u32_at(this.position,t),this._position+=4,this}write_i8(t){return this.write_i8_at(this.position,t),this._position+=1,this}write_i16(t){return this.write_i16_at(this.position,t),this._position+=2,this}write_i32(t){return this.write_i32_at(this.position,t),this._position+=4,this}write_f32(t){return this.write_f32_at(this.position,t),this._position+=4,this}write_u8_array(t){return this.write_u8_array_at(this.position,t),this._position+=t.length,this}write_u16_array(t){return this.write_u16_array_at(this.position,t),this._position+=2*t.length,this}write_u32_array(t){return this.write_u32_array_at(this.position,t),this._position+=4*t.length,this}write_i32_array(t){return this.write_i32_array_at(this.position,t),this._position+=4*t.length,this}write_vec2_f32(t){return this.write_vec2_f32_at(this.position,t),this._position+=8,this}write_vec3_f32(t){return this.write_vec3_f32_at(this.position,t),this._position+=12,this}write_cursor(t){const i=t.size-t.position;return this.ensure_size(i),t.copy_to_uint8_array(new Uint8Array(this.backing_buffer,this.offset+this.position,i),i),this._position+=i,this}write_string_ascii(t,i){return this.write_string_ascii_at(this.position,t,i),this._position+=i,this}write_string_utf16(t,i){return this.write_string_utf16_at(this.position,t,i),this._position+=i,this}write_u8_at(t,i){return this.ensure_size(1,t),this.dv.setUint8(t,i),this}write_u16_at(t,i){return this.ensure_size(2,t),this.dv.setUint16(t,i,this.little_endian),this}write_u32_at(t,i){return this.ensure_size(4,t),this.dv.setUint32(t,i,this.little_endian),this}write_i8_at(t,i){return this.ensure_size(1,t),this.dv.setInt8(t,i),this}write_i16_at(t,i){return this.ensure_size(2,t),this.dv.setInt16(t,i,this.little_endian),this}write_i32_at(t,i){return this.ensure_size(4,t),this.dv.setInt32(t,i,this.little_endian),this}write_f32_at(t,i){return this.ensure_size(4,t),this.dv.setFloat32(t,i,this.little_endian),this}write_u8_array_at(t,i){return this.ensure_size(i.length,t),new Uint8Array(this.backing_buffer,this.offset+t).set(new Uint8Array(i)),this}write_u16_array_at(t,i){this.ensure_size(2*i.length,t);const e=i.length;for(let s=0;s<e;s++)this.write_u16_at(t+2*s,i[s]);return this}write_u32_array_at(t,i){this.ensure_size(4*i.length,t);const e=i.length;for(let s=0;s<e;s++)this.write_u32_at(t+4*s,i[s]);return this}write_i32_array_at(t,i){this.ensure_size(4*i.length,t);const e=i.length;for(let s=0;s<e;s++)this.write_i32_at(t+4*s,i[s]);return this}write_vec2_f32_at(t,i){return this.ensure_size(8,t),this.dv.setFloat32(t,i.x,this.little_endian),this.dv.setFloat32(t+4,i.y,this.little_endian),this}write_vec3_f32_at(t,i){return this.ensure_size(12,t),this.dv.setFloat32(t,i.x,this.little_endian),this.dv.setFloat32(t+4,i.y,this.little_endian),this.dv.setFloat32(t+8,i.z,this.little_endian),this}write_string_ascii_at(t,i,e){this.ensure_size(e,t);const s=Math.min(e,i.length);for(let e=0;e<s;e++)this.write_u8_at(t+e,i.codePointAt(e));const r=e-s;for(let i=0;i<r;i++)this.write_u8_at(t+s+i,0);return this}write_string_utf16_at(t,i,e){this.ensure_size(e,t);const s=Math.floor(e/2),r=Math.min(s,i.length);for(let e=0;e<r;e++)this.write_u16_at(t+2*e,i.codePointAt(e));const n=s-r;for(let i=0;i<n;i++)this.write_u16_at(t+2*r+2*i,0);return this}ensure_size(t,i=this.position){const e=this.size-i;if(t>e)throw new Error(`${t} Bytes required but only ${e} available.`)}}},NRxM:function(t,i,e){"use strict";e.d(i,"a",(function(){return r}));var s=e("9dYx");class r extends s.a{constructor(t,i,e=0,s=t.byteLength-e){super(i,e),this._size=s,this.backing_buffer=t,this.dv=new DataView(t,0,t.byteLength)}get size(){return this._size}set size(t){if(t>this.backing_buffer.byteLength-this.offset)throw new Error(`Size ${t} is out of bounds.`);this._size=t}take(t){const i=this.offset+this.position,e=new r(this.backing_buffer,this.endianness,i,t);return this._position+=t,e}}},"Sa3/":function(t,i,e){"use strict";e.d(i,"a",(function(){return r}));var s=function(t,i,e,s){return new(e||(e=Promise))((function(r,n){function o(t){try{u(s.next(t))}catch(t){n(t)}}function h(t){try{u(s.throw(t))}catch(t){n(t)}}function u(t){var i;t.done?r(t.value):(i=t.value,i instanceof e?i:new e((function(t){t(i)}))).then(o,h)}u((s=s.apply(t,i||[])).next())}))};function r(t){return s(this,void 0,void 0,(function*(){return new Promise((i,e)=>{const s=new FileReader;s.addEventListener("loadend",()=>{s.result instanceof ArrayBuffer?i(s.result):e(new Error("Couldn't read file."))}),s.readAsArrayBuffer(t)})}))}},bcBH:function(t,i,e){"use strict";var s;e.d(i,"a",(function(){return s})),function(t){t[t.Little=0]="Little",t[t.Big=1]="Big"}(s||(s={}))},iR5r:function(t,i,e){"use strict";e.d(i,"a",(function(){return r})),e.d(i,"b",(function(){return n}));var s=e("Womt");function r(t){return t.textures.map(n)}function n(t){let i,e;switch(t.format[1]){case 6:i=s.RGBA_S3TC_DXT1_Format,e=t.width*t.height/2;break;case 7:i=s.RGBA_S3TC_DXT3_Format,e=t.width*t.height;break;default:throw new Error(`Format ${t.format.join(", ")} not supported.`)}const r=new s.CompressedTexture([{data:new Uint8Array(t.data,0,e),width:t.width,height:t.height}],t.width,t.height,i);return r.minFilter=s.LinearFilter,r.wrapS=s.MirroredRepeatWrapping,r.wrapT=s.MirroredRepeatWrapping,r.needsUpdate=!0,r}},yub1:function(t,i,e){"use strict";e.d(i,"a",(function(){return h}));var s=e("9Ykw");const r=e("rwco").c.get("core/data_formats/parsing/ninja/texture"),n=1213027928,o=1414682200;function h(t){const i=Object(s.a)(t),e=i.find(t=>t.type===n),h=e&&function(t){return{texture_count:t.u16()}}(e.data),u=i.filter(t=>t.type===o).map(t=>(function(t){const i=t.u32(),e=t.u32(),s=t.u32(),r=t.u16(),n=t.u16(),o=t.u32();return t.seek(36),{id:s,format:[i,e],width:r,height:n,size:o,data:t.array_buffer(o)}})(t.data));return h&&h.texture_count!==u.length&&r.warn(`Found ${u.length} textures instead of ${h.texture_count} as defined in the header.`),{textures:u}}}}]);