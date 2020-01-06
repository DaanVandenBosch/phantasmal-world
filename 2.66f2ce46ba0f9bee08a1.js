(window.webpackJsonp=window.webpackJsonp||[]).push([[2],{"/Wax":function(t,e,i){"use strict";i.d(e,"a",(function(){return n}));var s=i("9dYx");class n extends s.a{constructor(t,e,i=0,s=t.size-i){if(i<0||i>t.size)throw new Error(`Offset ${i} is out of bounds.`);if(s<0||i+s>t.size)throw new Error(`Size ${s} is out of bounds.`);super(e,i),this.buffer=t,this._size=s}get size(){return this._size}set size(t){t>this._size?this.ensure_size(t-this.position):this._size=t}get backing_buffer(){return this.buffer.backing_buffer}get dv(){return this.buffer.view}take(t){this.check_size("size",t,t);const e=this.offset+this.position,i=new n(this.buffer,this.endianness,e,t);return this._position+=t,i}ensure_size(t,e=this.position){const i=e+t-this._size;i>0&&(this._size+=i,this.buffer.size<this.offset+this._size&&(this.buffer.size=this.offset+this._size))}}},"0Xme":function(t,e,i){"use strict";i.d(e,"a",(function(){return a}));var s=i("sDu+"),n=i("jJhE"),r=(i("x1yY"),i("4VsB")),o=i("FXpb");class a extends s.a{constructor(t,e,i,s,n){super(n),this.element=Object(o.s)({className:`${e} core_Input`}),this._value=new r.a(this,t,this.set_value),this.value=this._value,this.input_element=Object(o.m)({className:`${s} core_Input_inner`}),this.input_element.type=i,this.input_element.addEventListener("change",()=>{this._value.set_val(this.get_value(),{silent:!1})}),n&&n.readonly&&this.set_attr("readOnly",!0),this.element.append(this.input_element)}set_enabled(t){super.set_enabled(t),this.input_element.disabled=!t}set_attr(t,e,i){if(null==e)return;const s=this.input_element,r=i||(t=>t);Object(n.a)(e)?(s[t]=r(e.val),this.disposable(e.observe(({value:e})=>s[t]=r(e)))):s[t]=r(e)}}},"488w":function(t,e,i){"use strict";i.d(e,"b",(function(){return l})),i.d(e,"a",(function(){return d}));var s=i("Womt"),n=i("tRdk"),r=i("kwt4"),o=i("vM2b");const a=new s.Vector3(0,1,0),u=new s.Vector2(0,0),h=new s.Vector3(0,0,0),c=new s.Quaternion(0,0,0,1),_=new s.Vector3(1,1,1);function l(t,e){new p(e).to_geometry_builder(t)}function d(t){return new p(new o.a).create_buffer_geometry(t)}class f{constructor(){this.vertices_stack=[]}put(t){this.vertices_stack.push(t)}get(t){const e=[];for(let i=this.vertices_stack.length-1;i>=0;i--){const s=this.vertices_stack[i][t];s&&e.push(s)}return e}}class p{constructor(t){this.vertices=new f,this.bone_id=0,this.builder=t}to_geometry_builder(t){this.object_to_geometry(t,void 0,new s.Matrix4)}create_buffer_geometry(t){return this.to_geometry_builder(t),this.builder.build()}object_to_geometry(t,e,i){const{no_translate:r,no_rotate:o,no_scale:a,hidden:u,break_child_trace:l,zxy_rotation_order:d,skip:f}=t.evaluation_flags,{position:p,rotation:b,scale:g}=t,w=new s.Euler(b.x,b.y,b.z,d?"ZXY":"ZYX"),m=(new s.Matrix4).compose(r?h:Object(n.a)(p),o?c:(new s.Quaternion).setFromEuler(w),a?_:Object(n.a)(g)).premultiply(i);let v;if(f?v=e:(v=new s.Bone,v.name=this.bone_id.toString(),v.position.set(p.x,p.y,p.z),v.setRotationFromEuler(w),v.scale.set(g.x,g.y,g.z),this.builder.add_bone(v),e&&e.add(v)),t.model&&!u&&this.model_to_geometry(t.model,m),this.bone_id++,!l)for(const e of t.children)this.object_to_geometry(e,v,m)}model_to_geometry(t,e){Object(r.b)(t)?this.njcm_model_to_geometry(t,e):this.xj_model_to_geometry(t,e)}njcm_model_to_geometry(t,e){const i=(new s.Matrix3).getNormalMatrix(e),r=t.vertices.map(t=>{const r=Object(n.a)(t.position),o=t.normal?Object(n.a)(t.normal):new s.Vector3(0,1,0);return r.applyMatrix4(e),o.applyMatrix3(i),{bone_id:this.bone_id,position:r,normal:o,bone_weight:t.bone_weight,bone_weight_status:t.bone_weight_status,calc_continue:t.calc_continue}});this.vertices.put(r);for(const e of t.meshes){const t=this.builder.index_count;for(let t=0;t<e.vertices.length;++t){const i=e.vertices[t],s=this.vertices.get(i.index);if(s.length){const n=s[0],r=n.normal||i.normal||a,o=this.builder.vertex_count;this.builder.add_vertex(n.position,r,e.has_tex_coords?i.tex_coords:u),t>=2&&(t%2==(e.clockwise_winding?1:0)?(this.builder.add_index(o-2),this.builder.add_index(o-1),this.builder.add_index(o)):(this.builder.add_index(o-2),this.builder.add_index(o),this.builder.add_index(o-1)));const h=[[0,0],[0,0],[0,0],[0,0]];for(let t=s.length-1;t>=0;t--){const e=s[t];h[e.bone_weight_status]=[e.bone_id,e.bone_weight]}const c=h.reduce((t,[,e])=>t+e,0);for(const[t,e]of h)this.builder.add_bone_weight(t,c>0?e/c:e)}}this.builder.add_group(t,this.builder.index_count-t,e.texture_id)}}xj_model_to_geometry(t,e){const i=this.builder.vertex_count,r=(new s.Matrix3).getNormalMatrix(e);for(const{position:i,normal:o,uv:a}of t.vertices){const t=Object(n.a)(i).applyMatrix4(e),h=(o?Object(n.a)(o):new s.Vector3(0,1,0)).applyMatrix3(r),c=a||u;this.builder.add_vertex(t,h,c)}let o;for(const e of t.meshes){const t=this.builder.index_count;let s=!1;for(let t=2;t<e.indices.length;++t){const n=i+e.indices[t-2],r=i+e.indices[t-1],o=i+e.indices[t],a=this.builder.get_position(n),u=this.builder.get_position(r),h=this.builder.get_position(o),c=this.builder.get_normal(n),_=this.builder.get_normal(r),l=this.builder.get_normal(o),d=u.clone().sub(a).cross(h.clone().sub(a));s&&d.negate(),(d.dot(c)<0?1:0)+(d.dot(_)<0?1:0)+(d.dot(l)<0?1:0)>=2&&(s=!s),s?(this.builder.add_index(r),this.builder.add_index(n),this.builder.add_index(o)):(this.builder.add_index(n),this.builder.add_index(r),this.builder.add_index(o)),s=!s}null!=e.material_properties.texture_id&&(o=e.material_properties.texture_id),this.builder.add_group(t,this.builder.index_count-t,o)}}}},"6cU9":function(t,e,i){"use strict";i.d(e,"a",(function(){return a}));var s=i("ouMO"),n=i("KVVN"),r=i("iPoI"),o=function(t,e,i,s){return new(i||(i=Promise))((function(n,r){function o(t){try{u(s.next(t))}catch(t){r(t)}}function a(t){try{u(s.throw(t))}catch(t){r(t)}}function u(t){var e;t.done?n(t.value):(e=t.value,e instanceof i?e:new i((function(t){t(e)}))).then(o,a)}u((s=s.apply(t,e||[])).next())}))};class a extends n.a{constructor(t){super(t),this._files=Object(s.e)([]),this.files=this._files,this.element.classList.add("core_FileButton"),this.disposables(this.onclick.observe(()=>o(this,void 0,void 0,(function*(){this._files.val=yield Object(r.a)(t)})))),this.finalize_construction()}}},"9Ykw":function(t,e,i){"use strict";i.d(e,"a",(function(){return a})),i.d(e,"b",(function(){return u}));var s=i("BAAx"),n=i("rwco"),r=i("5cb6");const o=n.a.get("core/data_formats/parsing/iff");function a(t,e=!1){return h(t,e,[],(t,e,i)=>({type:e,data:t.take(i)}))}function u(t,e=!1){return h(t,e,[],(t,e,i)=>({type:e,size:i}))}function h(t,e,i,n){const a=Object(s.b)(o);let u=!1;for(;t.bytes_left>=8;){const s=t.u32(),o=t.position,h=t.u32();if(h>t.bytes_left){u=!0,e||a.add_problem(0===i.length?r.b.Error:r.b.Warning,"Invalid IFF format.",`Size ${h} was too large (only ${t.bytes_left} bytes left) at position ${o}.`);break}i.push(n(t,s,h))}return u&&0===i.length?a.failure():a.success(i)}},"9dYx":function(t,e,i){"use strict";var s=i("bcBH");class n{constructor(t,e){this._position=0,this.endianness=t,this.offset=e}get position(){return this._position}get endianness(){return this.little_endian?s.a.Little:s.a.Big}set endianness(t){this.little_endian=t===s.a.Little}get bytes_left(){return this.size-this.position}seek(t){return this.seek_start(this.position+t)}seek_start(t){if(t<0||t>this.size)throw new Error(`Offset ${t} is out of bounds.`);return this._position=t,this}seek_end(t){if(t<0||t>this.size)throw new Error(`Offset ${t} is out of bounds.`);return this._position=this.size-t,this}u8(){return this.u8_at(this._position++)}u8_at(t){return this.check_offset(t,1),this.dv.getUint8(this.offset+t)}u16(){const t=this.u16_at(this.position);return this._position+=2,t}u16_at(t){return this.check_offset(t,2),this.dv.getUint16(this.offset+t,this.little_endian)}u32(){const t=this.u32_at(this.position);return this._position+=4,t}u32_at(t){return this.check_offset(t,4),this.dv.getUint32(this.offset+t,this.little_endian)}i8(){return this.i8_at(this._position++)}i8_at(t){return this.check_offset(t,1),this.dv.getInt8(this.offset+t)}i16(){const t=this.i16_at(this.position);return this._position+=2,t}i16_at(t){return this.check_offset(t,2),this.dv.getInt16(this.offset+t,this.little_endian)}i32(){const t=this.i32_at(this.position);return this._position+=4,t}i32_at(t){return this.check_offset(t,4),this.dv.getInt32(this.offset+t,this.little_endian)}f32(){const t=this.f32_at(this.position);return this._position+=4,t}f32_at(t){return this.check_offset(t,4),this.dv.getFloat32(this.offset+t,this.little_endian)}u8_array(t){this.check_size("n",t,t);const e=[];for(let i=0;i<t;++i)e.push(this.dv.getUint8(this.offset+this._position++));return e}u16_array(t){this.check_size("n",t,2*t);const e=[];for(let i=0;i<t;++i)e.push(this.dv.getUint16(this.offset+this.position,this.little_endian)),this._position+=2;return e}u32_array(t){this.check_size("n",t,4*t);const e=[];for(let i=0;i<t;++i)e.push(this.dv.getUint32(this.offset+this.position,this.little_endian)),this._position+=4;return e}i32_array(t){this.check_size("n",t,4*t);const e=[];for(let i=0;i<t;++i)e.push(this.dv.getInt32(this.offset+this.position,this.little_endian)),this._position+=4;return e}vec2_f32(){return{x:this.f32(),y:this.f32()}}vec3_f32(){return{x:this.f32(),y:this.f32(),z:this.f32()}}string_ascii(t,e,i){const s=[];for(let n=0;n<t;n++){const r=this.u8();if(e&&0===r){i&&this.seek(t-n-1);break}s.push(r)}return String.fromCodePoint(...s)}string_utf16(t,e,i){const s=[],n=Math.floor(t/2);for(let t=0;t<n;t++){const r=this.u16();if(e&&0===r){i&&this.seek(2*(n-t-1));break}s.push(r)}return String.fromCodePoint(...s)}string_ascii_at(t,e,i){const s=[];for(let n=0;n<e;n++){const e=this.u8_at(t+n);if(i&&0===e)break;s.push(e)}return String.fromCodePoint(...s)}string_utf16_at(t,e,i){const s=[],n=Math.floor(e/2);for(let e=0;e<n;e++){const n=this.u16_at(t+2*e);if(i&&0===n)break;s.push(n)}return String.fromCodePoint(...s)}array_buffer(t=this.size-this.position){this.check_size("size",t,t);const e=this.backing_buffer.slice(this.offset+this.position,this.offset+this.position+t);return this._position+=t,e}copy_to_uint8_array(t,e=this.size-this.position){return this.check_size("size",e,e),t.set(new Uint8Array(this.backing_buffer,this.offset+this.position,e)),this._position+=e,this}check_size(t,e,i){if(i<0||i>this.size-this.position)throw new Error(`${t} ${e} is out of bounds.`)}check_offset(t,e){if(t<0||t+e>this.size)throw new Error(`Offset ${t} is out of bounds.`)}}i.d(e,"a",(function(){return r}));class r extends n{write_u8(t){return this.write_u8_at(this.position,t),this._position+=1,this}write_u16(t){return this.write_u16_at(this.position,t),this._position+=2,this}write_u32(t){return this.write_u32_at(this.position,t),this._position+=4,this}write_i8(t){return this.write_i8_at(this.position,t),this._position+=1,this}write_i16(t){return this.write_i16_at(this.position,t),this._position+=2,this}write_i32(t){return this.write_i32_at(this.position,t),this._position+=4,this}write_f32(t){return this.write_f32_at(this.position,t),this._position+=4,this}write_u8_array(t){return this.write_u8_array_at(this.position,t),this._position+=t.length,this}write_u16_array(t){return this.write_u16_array_at(this.position,t),this._position+=2*t.length,this}write_u32_array(t){return this.write_u32_array_at(this.position,t),this._position+=4*t.length,this}write_i32_array(t){return this.write_i32_array_at(this.position,t),this._position+=4*t.length,this}write_vec2_f32(t){return this.write_vec2_f32_at(this.position,t),this._position+=8,this}write_vec3_f32(t){return this.write_vec3_f32_at(this.position,t),this._position+=12,this}write_cursor(t){const e=t.size-t.position;return this.ensure_size(e),t.copy_to_uint8_array(new Uint8Array(this.backing_buffer,this.offset+this.position,e),e),this._position+=e,this}write_string_ascii(t,e){return this.write_string_ascii_at(this.position,t,e),this._position+=e,this}write_string_utf16(t,e){return this.write_string_utf16_at(this.position,t,e),this._position+=e,this}write_u8_at(t,e){return this.ensure_size(1,t),this.dv.setUint8(t,e),this}write_u16_at(t,e){return this.ensure_size(2,t),this.dv.setUint16(t,e,this.little_endian),this}write_u32_at(t,e){return this.ensure_size(4,t),this.dv.setUint32(t,e,this.little_endian),this}write_i8_at(t,e){return this.ensure_size(1,t),this.dv.setInt8(t,e),this}write_i16_at(t,e){return this.ensure_size(2,t),this.dv.setInt16(t,e,this.little_endian),this}write_i32_at(t,e){return this.ensure_size(4,t),this.dv.setInt32(t,e,this.little_endian),this}write_f32_at(t,e){return this.ensure_size(4,t),this.dv.setFloat32(t,e,this.little_endian),this}write_u8_array_at(t,e){return this.ensure_size(e.length,t),new Uint8Array(this.backing_buffer,this.offset+t).set(new Uint8Array(e)),this}write_u16_array_at(t,e){this.ensure_size(2*e.length,t);const i=e.length;for(let s=0;s<i;s++)this.write_u16_at(t+2*s,e[s]);return this}write_u32_array_at(t,e){this.ensure_size(4*e.length,t);const i=e.length;for(let s=0;s<i;s++)this.write_u32_at(t+4*s,e[s]);return this}write_i32_array_at(t,e){this.ensure_size(4*e.length,t);const i=e.length;for(let s=0;s<i;s++)this.write_i32_at(t+4*s,e[s]);return this}write_vec2_f32_at(t,e){return this.ensure_size(8,t),this.dv.setFloat32(t,e.x,this.little_endian),this.dv.setFloat32(t+4,e.y,this.little_endian),this}write_vec3_f32_at(t,e){return this.ensure_size(12,t),this.dv.setFloat32(t,e.x,this.little_endian),this.dv.setFloat32(t+4,e.y,this.little_endian),this.dv.setFloat32(t+8,e.z,this.little_endian),this}write_string_ascii_at(t,e,i){this.ensure_size(i,t);const s=Math.min(i,e.length);for(let i=0;i<s;i++)this.write_u8_at(t+i,e.codePointAt(i));const n=i-s;for(let e=0;e<n;e++)this.write_u8_at(t+s+e,0);return this}write_string_utf16_at(t,e,i){this.ensure_size(i,t);const s=Math.floor(i/2),n=Math.min(s,e.length);for(let i=0;i<n;i++)this.write_u16_at(t+2*i,e.codePointAt(i));const r=s-n;for(let e=0;e<r;e++)this.write_u16_at(t+2*n+2*e,0);return this}ensure_size(t,e=this.position){const i=this.size-e;if(t>i)throw new Error(`${t} Bytes required but only ${i} available.`)}}},BAAx:function(t,e,i){"use strict";function s(t,e){return{success:!0,value:t,problems:null!=e?e:[]}}function n(t){return{success:!1,problems:null!=t?t:[]}}function r(t){if(t.success)return t.value;throw new Error(t.problems.join("\n"))}function o(t){return new a(t)}i.d(e,"c",(function(){return s})),i.d(e,"a",(function(){return n})),i.d(e,"d",(function(){return r})),i.d(e,"b",(function(){return o}));class a{constructor(t){this.logger=t,this.problems=[]}add_problem(t,e,i,s){return this.logger.log(t,i,s),this.problems.push({severity:t,ui_message:e}),this}add_result(t){return this.problems.push(...t.problems),this}success(t){return s(t,this.problems)}failure(){return n(this.problems)}}},Eqai:function(t,e,i){"use strict";i.d(e,"a",(function(){return a})),i.d(e,"b",(function(){return u}));var s=i("Womt");const n=new s.MeshLambertMaterial({color:65280,side:s.DoubleSide}),r=new s.MeshLambertMaterial({color:16711935,side:s.DoubleSide}),o=new s.MeshLambertMaterial({skinning:!0,color:16711935,side:s.DoubleSide});function a(t,e,i=r){return h(t,e,i,s.Mesh)}function u(t,e,i=o){return h(t,e,i,s.SkinnedMesh)}function h(t,e,i,r){const{created_by_geometry_builder:o,normalized_material_indices:a,bones:u}=t.userData;let h;if(Array.isArray(e))if(o){h=[n];for(const[t,s]of a.entries())s>0&&(h[s]=e[t]||i)}else h=e;else h=e||i;const c=new r(t,h);return o&&u.length&&c instanceof s.SkinnedMesh&&(c.add(u[0]),c.bind(new s.Skeleton(u))),c}},Ftn7:function(t,e,i){"use strict";i.d(e,"a",(function(){return r}));var s=i("pVCM"),n=i("Womt");s.a.install({THREE:Object.assign(Object.assign({},n),{MOUSE:Object.assign(Object.assign({},n.MOUSE),{LEFT:n.MOUSE.RIGHT,RIGHT:n.MOUSE.LEFT})})});class r{constructor(t){this._debug=!1,this.scene=new n.Scene,this.light_holder=new n.Group,this.render_scheduled=!1,this.animation_frame_handle=void 0,this.light=new n.HemisphereLight(16777215,5263440,1),this.controls_clock=new n.Clock,this.size=new n.Vector2(0,0),this.schedule_render=()=>{this.render_scheduled=!0},this.on_mouse_down=t=>{t.currentTarget&&t.currentTarget.focus()},this.call_render=()=>{const t=this.controls.update(this.controls_clock.getDelta()),e=this.render_scheduled||t;this.render_scheduled=!1,e&&this.render(),this.animation_frame_handle=requestAnimationFrame(this.call_render)},this.renderer=t,this.renderer.domElement.tabIndex=0,this.renderer.domElement.addEventListener("mousedown",this.on_mouse_down),this.renderer.domElement.style.outline="none",this.scene.background=new n.Color(1579032),this.light_holder.add(this.light),this.scene.add(this.light_holder)}get debug(){return this._debug}set debug(t){this._debug=t}get canvas_element(){return this.renderer.domElement}set_size(t,e){this.size.set(t,e),this.renderer.setSize(t,e),this.schedule_render()}pointer_pos_to_device_coords(t){t.set(t.x/this.size.width*2-1,t.y/this.size.height*-2+1)}start_rendering(){null==this.animation_frame_handle&&(this.schedule_render(),this.animation_frame_handle=requestAnimationFrame(this.call_render))}stop_rendering(){null!=this.animation_frame_handle&&(cancelAnimationFrame(this.animation_frame_handle),this.animation_frame_handle=void 0)}reset_camera(t,e){this.controls.setLookAt(t.x,t.y,t.z,e.x,e.y,e.z)}dispose(){this.renderer.dispose(),this.controls.dispose()}init_camera_controls(){this.controls=new s.a(this.camera,this.renderer.domElement),this.controls.dampingFactor=1,this.controls.draggingDampingFactor=1}render(){this.renderer.render(this.scene,this.camera)}}},IMNf:function(t,e,i){"use strict";i.d(e,"a",(function(){return o}));var s=i("/Wax"),n=i("tjEv");const r=i("rwco").a.get("core/data_formats/compression/prs/decompress");function o(t){const e=new a(t);for(;;)if(1===e.read_flag_bit())e.copy_u8();else{let t,i;if(0===e.read_flag_bit())t=e.read_flag_bit()<<1,t|=e.read_flag_bit(),t+=2,i=e.read_u8()-256;else{if(i=e.read_u16(),0===i)break;t=7&i,i>>>=3,0===t?(t=e.read_u8(),t+=1):t+=2,i-=8192}e.offset_copy(i,t)}return e.dst.seek_start(0)}class a{constructor(t){this.src=t,this.dst=new s.a(new n.a(Math.floor(1.5*t.size)),t.endianness),this.flags=0,this.flag_bits_left=0}read_flag_bit(){0===this.flag_bits_left&&(this.flags=this.read_u8(),this.flag_bits_left=8);const t=1&this.flags;return this.flags>>>=1,this.flag_bits_left-=1,t}copy_u8(){this.dst.write_u8(this.read_u8())}read_u8(){return this.src.u8()}read_u16(){return this.src.u16()}offset_copy(t,e){(t<-8192||t>0)&&r.error(`offset was ${t}, should be between -8192 and 0.`),(e<1||e>256)&&r.error(`length was ${e}, should be between 1 and 256.`);const i=Math.min(-t,e);this.dst.seek(t);const s=this.dst.take(i);this.dst.seek(-t-i);for(let t=0;t<Math.floor(e/i);++t)this.dst.write_cursor(s),s.seek_start(0);this.dst.write_cursor(s.take(e%i))}}},NRxM:function(t,e,i){"use strict";i.d(e,"a",(function(){return n}));var s=i("9dYx");class n extends s.a{constructor(t,e,i=0,s=t.byteLength-i){super(e,i),this._size=s,this.backing_buffer=t,this.dv=new DataView(t,0,t.byteLength)}get size(){return this._size}set size(t){if(t>this.backing_buffer.byteLength-this.offset)throw new Error(`Size ${t} is out of bounds.`);this._size=t}take(t){const e=this.offset+this.position,i=new n(this.backing_buffer,this.endianness,e,t);return this._position+=t,i}}},PE7g:function(t,e,i){"use strict";i.d(e,"a",(function(){return n}));var s=i("0Xme");i("rY5A");class n extends s.a{constructor(t=0,e){if(super(t,"core_NumberInput","number","core_NumberInput_inner",e),this.preferred_label_position="left",e){const{min:t,max:i,step:s,width:n}=e;this.set_attr("min",t,String),this.set_attr("max",i,String),this.input_element.step="any",this.set_attr("step",s,String),null!=n&&(this.element.style.width=`${n}px`)}e&&null!=e.round_to&&e.round_to>=0?this.rounding_factor=Math.pow(10,e.round_to):this.rounding_factor=1,this.set_value(t),this.finalize_construction()}get_value(){return parseFloat(this.input_element.value)}set_value(t){this.input_element.valueAsNumber=Math.round(this.rounding_factor*t)/this.rounding_factor}}},bcBH:function(t,e,i){"use strict";var s;i.d(e,"a",(function(){return s})),function(t){t[t.Little=0]="Little",t[t.Big=1]="Big"}(s||(s={}))},iPoI:function(t,e,i){"use strict";i.d(e,"a",(function(){return n})),i.d(e,"b",(function(){return r}));var s=i("FXpb");function n(t){return new Promise(e=>{var i,n,r,o;const a=Object(s.m)({type:"file"});a.accept=null!=(n=null===(i=t)||void 0===i?void 0:i.accept)?n:"",a.multiple=null!=(o=null===(r=t)||void 0===r?void 0:r.multiple)&&o,a.onchange=()=>{a.files&&a.files.length?e([...a.files]):e([])},a.click()})}function r(t){return new Promise((e,i)=>{const s=new FileReader;s.addEventListener("loadend",()=>{s.result instanceof ArrayBuffer?e(s.result):i(new Error("Couldn't read file."))}),s.readAsArrayBuffer(t)})}},iR5r:function(t,e,i){"use strict";i.d(e,"a",(function(){return n})),i.d(e,"b",(function(){return r}));var s=i("Womt");function n(t){return t.textures.map(r)}function r(t){let e,i;switch(t.format[1]){case 6:e=s.RGBA_S3TC_DXT1_Format,i=t.width*t.height/2;break;case 7:e=s.RGBA_S3TC_DXT3_Format,i=t.width*t.height;break;default:throw new Error(`Format ${t.format.join(", ")} not supported.`)}const n=new s.CompressedTexture([{data:new Uint8Array(t.data,0,i),width:t.width,height:t.height}],t.width,t.height,e);return n.minFilter=s.LinearFilter,n.wrapS=s.MirroredRepeatWrapping,n.wrapT=s.MirroredRepeatWrapping,n.needsUpdate=!0,n}},kwt4:function(t,e,i){"use strict";var s=i("9Ykw"),n=i("rwco");const r=n.a.get("core/data_formats/parsing/ninja/njcm");var o;function a(t,e){const i=t.u32(),s=t.u32(),n=t.vec3_f32(),r=t.f32(),a=[],h=[];if(i){t.seek_start(i);for(const i of u(t,e,!0))if(i.type===o.Vertex)for(const t of i.vertices)a[t.index]={position:t.position,normal:t.normal,bone_weight:t.bone_weight,bone_weight_status:t.bone_weight_status,calc_continue:t.calc_continue}}if(s){t.seek_start(s);let i=void 0;for(const s of u(t,e,!1))if(s.type===o.Tiny)i=s.texture_id;else if(s.type===o.Strip){for(const t of s.triangle_strips)t.texture_id=i;h.push(...s.triangle_strips)}}return{type:"njcm",vertices:a,meshes:h,collision_sphere_center:n,collision_sphere_radius:r}}function u(t,e,i){const s=[];let n=!0;for(;n;){const a=t.u8(),_=t.u8(),l=t.position;let d=0;if(0===a)s.push({type:o.Null,type_id:a});else if(1<=a&&a<=3)s.push({type:o.Bits,type_id:a});else if(4===a){const i=_,r=t.position;s.push({type:o.CachePolygonList,type_id:a,cache_index:i,offset:r}),e[i]=r,n=!1}else if(5===a){const n=_,r=e[n];null!=r&&(t.seek_start(r),s.push(...u(t,e,i))),s.push({type:o.DrawPolygonList,type_id:a,cache_index:n})}else if(8<=a&&a<=9){d=2;const e=t.u16();s.push({type:o.Tiny,type_id:a,flip_u:0!=(128&a),flip_v:0!=(64&a),clamp_u:0!=(32&a),clamp_v:0!=(16&a),mipmap_d_adjust:15&a,filter_mode:e>>>14,super_sample:0!=(64&e),texture_id:8191&e})}else 17<=a&&a<=31?(d=2+2*t.u16(),s.push({type:o.Material,type_id:a})):32<=a&&a<=50?(d=2+4*t.u16(),s.push({type:o.Vertex,type_id:a,vertices:h(t,a,_)})):56<=a&&a<=58?(d=2+2*t.u16(),s.push({type:o.Volume,type_id:a})):64<=a&&a<=75?(d=2+2*t.u16(),s.push({type:o.Strip,type_id:a,triangle_strips:c(t,a,_)})):255===a?(d=i?2:0,s.push({type:o.End,type_id:a}),n=!1):(d=2+2*t.u16(),s.push({type:o.Unknown,type_id:a}),r.warning(`Unknown chunk type ${a} at offset ${l}.`));t.seek_start(l+d)}return s}function h(t,e,i){if(e<32||e>50)return r.warning(`Unknown vertex chunk type ${e}.`),[];const s=3&i,n=0!=(128&i),o=t.u16(),a=t.u16(),u=[];for(let i=0;i<a;++i){const r={index:o+i,position:t.vec3_f32(),bone_weight:1,bone_weight_status:s,calc_continue:n};if(32===e)t.seek(4);else if(33===e)t.seek(4),r.normal=t.vec3_f32(),t.seek(4);else if(35<=e&&e<=40)37===e?(r.index=o+t.u16(),r.bone_weight=t.u16()/255):t.seek(4);else if(41<=e&&e<=47)r.normal=t.vec3_f32(),e>=42&&(44===e?(r.index=o+t.u16(),r.bone_weight=t.u16()/255):t.seek(4));else if(48<=e&&e<=50){const i=t.u32();r.normal={x:(i>>20&1023)/1023,y:(i>>10&1023)/1023,z:(1023&i)/1023},e>=49&&t.seek(4)}u.push(r)}return u}function c(t,e,i){const s={ignore_light:0!=(1&i),ignore_specular:0!=(2&i),ignore_ambient:0!=(4&i),use_alpha:0!=(8&i),double_side:0!=(16&i),flat_shading:0!=(32&i),environment_mapping:0!=(64&i)},n=t.u16(),r=n>>>14,o=16383&n;let a=!1,u=!1,h=!1,c=!1;switch(e){case 64:break;case 65:case 66:a=!0;break;case 67:h=!0;break;case 68:case 69:a=!0,h=!0;break;case 70:u=!0;break;case 71:case 72:a=!0,u=!0;break;case 73:break;case 74:case 75:c=!0;break;default:throw new Error(`Unexpected chunk type ID: ${e}.`)}const _=[];for(let e=0;e<o;++e){const e=t.i16(),i=e<1,n=Math.abs(e),o=[];for(let e=0;e<n;++e){const i={index:t.u16()};o.push(i),a&&(i.tex_coords={x:t.u16()/255,y:t.u16()/255}),u&&t.seek(4),h&&(i.normal={x:t.u16()/255,y:t.u16()/255,z:t.u16()/255}),c&&t.seek(8),e>=2&&t.seek(2*r)}_.push(Object.assign(Object.assign({},s),{clockwise_winding:i,has_tex_coords:a,has_normal:h,vertices:o}))}return _}!function(t){t[t.Unknown=0]="Unknown",t[t.Null=1]="Null",t[t.Bits=2]="Bits",t[t.CachePolygonList=3]="CachePolygonList",t[t.DrawPolygonList=4]="DrawPolygonList",t[t.Tiny=5]="Tiny",t[t.Material=6]="Material",t[t.Vertex=7]="Vertex",t[t.Volume=8]="Volume",t[t.Strip=9]="Strip",t[t.End=10]="End"}(o||(o={}));const _=n.a.get("core/data_formats/parsing/ninja/xj");function l(t){t.seek(4);const e=t.u32(),i=t.u32(),s=t.u32(),n=t.u32(),r=t.u32(),o=t.u32(),a={type:"xj",vertices:[],meshes:[],collision_sphere_position:t.vec3_f32(),collision_sphere_radius:t.f32()};return i>=1&&(i>1&&_.warning(`Vertex info count of ${i} was larger than expected.`),a.vertices.push(...function(t,e){t.seek_start(e);const i=t.u16();t.seek(2);const s=t.u32(),n=t.u32(),r=t.u32(),o=[];for(let e=0;e<r;++e){t.seek_start(s+e*n);const r=t.vec3_f32();let a,u;switch(i){case 3:a=t.vec3_f32(),u=t.vec2_f32();break;case 4:break;case 5:t.seek(4),u=t.vec2_f32();break;case 6:a=t.vec3_f32();break;case 7:a=t.vec3_f32(),u=t.vec2_f32();break;default:_.warning(`Unknown vertex type ${i} with size ${n}.`)}o.push({position:r,normal:a,uv:u})}return o}(t,e))),a.meshes.push(...d(t,s,n)),a.meshes.push(...d(t,r,o)),a}function d(t,e,i){const s=[];for(let n=0;n<i;++n){t.seek_start(e+20*n);const i=t.u32(),r=t.u32(),o=t.u32(),a=t.u32(),u=f(t,i,r);t.seek_start(o);const h=t.u16_array(a);s.push({material_properties:u,indices:h})}return s}function f(t,e,i){const s={};for(let n=0;n<i;++n){switch(t.seek_start(e+16*n),t.u32()){case 2:s.alpha_src=t.u32(),s.alpha_dst=t.u32();break;case 3:s.texture_id=t.u32();break;case 5:s.diffuse_r=t.u8(),s.diffuse_g=t.u8(),s.diffuse_b=t.u8(),s.diffuse_a=t.u8()}}return s}var p=i("BAAx");i.d(e,"a",(function(){return b})),i.d(e,"b",(function(){return w})),i.d(e,"c",(function(){return v})),i.d(e,"d",(function(){return y})),i.d(e,"e",(function(){return k}));const b=2*Math.PI/65535,g=1296255566;function w(t){return"njcm"===t.type}class m{constructor(t,e,i,s,n,r){this.bone_cache=new Map,this._bone_count=-1,this.evaluation_flags=t,this.model=e,this.position=i,this.rotation=s,this.scale=n,this._children=r,this.children=this._children}bone_count(){if(-1===this._bone_count){const t=[0];this.get_bone_internal(this,Number.MAX_SAFE_INTEGER,t),this._bone_count=t[0]}return this._bone_count}get_bone(t){let e=this.bone_cache.get(t);return void 0===e&&(e=this.get_bone_internal(this,t,[0]),this.bone_cache.set(t,e||null)),e||void 0}add_child(t){this._bone_count=-1,this.bone_cache.clear(),this._children.push(t)}get_bone_internal(t,e,i){if(!t.evaluation_flags.skip){const s=i[0]++;if(this.bone_cache.set(s,t),s===e)return t}if(!t.evaluation_flags.break_child_trace)for(const s of t.children){const t=this.get_bone_internal(s,e,i);if(t)return t}}}function v(t){return x(t,a,[])}function y(t){return x(t,l,void 0)}function k(t){return z(t,l,void 0)}function x(t,e,i){const n=Object(s.a)(t);if(!n.success)return n;const r=n.value.filter(t=>t.type===g),o=[];for(const t of r)o.push(...z(t.data,e,i));return Object(p.c)(o,n.problems)}function z(t,e,i){const s=t.u32(),n=0!=(1&s),r=0!=(2&s),o=0!=(4&s),a=0!=(8&s),u=0!=(16&s),h=0!=(32&s),c=0!=(64&s),_=0!=(128&s),l=t.u32(),d=t.vec3_f32(),f={x:t.i32()*b,y:t.i32()*b,z:t.i32()*b},p=t.vec3_f32(),g=t.u32(),w=t.u32();let v,y,k;return l&&(t.seek_start(l),v=e(t,i)),g?(t.seek_start(g),y=z(t,e,i)):y=[],w?(t.seek_start(w),k=z(t,e,i)):k=[],[new m({no_translate:n,no_rotate:r,no_scale:o,hidden:a,break_child_trace:u,zxy_rotation_order:h,skip:c,shape_skip:_},v,d,f,p,y),...k]}},mKaR:function(t,e,i){"use strict";i.d(e,"a",(function(){return n}));var s=i("Tpfs");class n{constructor(){this.disposer=new s.a}dispose(){this.disposer.dispose()}disposable(t){return this.disposer.add(t)}disposables(...t){this.disposer.add_all(...t)}}},oyid:function(t,e,i){"use strict";i.d(e,"a",(function(){return o}));var s=i("sDu+"),n=i("4VsB"),r=i("FXpb");class o extends s.a{constructor(t=!1,e){super(e),this.element=Object(r.m)({className:"core_CheckBox"}),this.preferred_label_position="right",this._checked=new n.a(this,t,this.set_checked),this.checked=this._checked,this.set_checked(t),this.element.type="checkbox",this.element.onchange=()=>this._checked.set_val(this.element.checked,{silent:!1}),this.finalize_construction()}set_enabled(t){super.set_enabled(t),this.element.disabled=!t}set_checked(t){this.element.checked=t}}},tRdk:function(t,e,i){"use strict";i.d(e,"a",(function(){return n}));var s=i("Womt");function n(t){return new s.Vector3(t.x,t.y,t.z)}},tjEv:function(t,e,i){"use strict";i.d(e,"a",(function(){return s}));class s{constructor(t=8192){this._size=0,this._buffer=new ArrayBuffer(t),this._data_view=new DataView(this._buffer)}get size(){return this._size}set size(t){if(t<0)throw new Error("Size should be non-negative.");this.ensure_capacity(t),this._size=t}get capacity(){return this._buffer.byteLength}get backing_buffer(){return this._buffer}get view(){return this._data_view}sub_view(t,e){return new DataView(this._buffer,t,e)}ensure_capacity(t){if(t>this.capacity){let e=this.capacity||t;do{e*=2}while(e<t);const i=new ArrayBuffer(e);new Uint8Array(i).set(new Uint8Array(this._buffer,0,this.size)),this._buffer=i,this._data_view=new DataView(this._buffer)}}}},vM2b:function(t,e,i){"use strict";i.d(e,"a",(function(){return n}));var s=i("Womt");class n{constructor(){this.positions=[],this.normals=[],this.uvs=[],this.indices=[],this.bones=[],this.bone_indices=[],this.bone_weights=[],this.groups=[],this.material_indices=new Set([-1])}get vertex_count(){return this.positions.length/3}get index_count(){return this.indices.length}get_position(t){return new s.Vector3(this.positions[3*t],this.positions[3*t+1],this.positions[3*t+2])}get_normal(t){return new s.Vector3(this.normals[3*t],this.normals[3*t+1],this.normals[3*t+2])}add_vertex(t,e,i){this.positions.push(t.x,t.y,t.z),this.normals.push(e.x,e.y,e.z),this.uvs.push(i.x,i.y)}add_index(t){this.indices.push(t)}add_bone(t){this.bones.push(t)}add_bone_weight(t,e){this.bone_indices.push(t),this.bone_weights.push(e)}add_group(t,e,i){const s=this.groups[this.groups.length-1],n=null==i?-1:i;s&&s.material_index===n?s.size+=e:(this.groups.push({offset:t,size:e,material_index:n}),this.material_indices.add(n))}build(){const t=new s.BufferGeometry,e=t.userData;e.created_by_geometry_builder=!0,t.setAttribute("position",new s.Float32BufferAttribute(this.positions,3)),t.setAttribute("normal",new s.Float32BufferAttribute(this.normals,3)),t.setAttribute("uv",new s.Float32BufferAttribute(this.uvs,2)),t.setIndex(new s.Uint16BufferAttribute(this.indices,1)),this.bone_indices.length&&this.bones.length?(t.setAttribute("skinIndex",new s.Uint16BufferAttribute(this.bone_indices,4)),t.setAttribute("skinWeight",new s.Float32BufferAttribute(this.bone_weights,4)),e.bones=this.bones):e.bones=[];const i=new Map;let n=0;for(const t of[...this.material_indices].sort((t,e)=>t-e))i.set(t,n++);for(const e of this.groups)t.addGroup(e.offset,e.size,i.get(e.material_index));return e.normalized_material_indices=i,t.computeBoundingSphere(),t.computeBoundingBox(),t}}},w6HN:function(t,e,i){"use strict";i.d(e,"a",(function(){return o}));var s=i("TRid"),n=(i("IQIM"),i("sDu+")),r=i("FXpb");class o extends s.a{constructor(t,...e){super(t instanceof s.a?void 0:t),this.element=Object(r.h)({className:"core_ToolBar"}),this.height=33,this.element.style.height=`${this.height}px`,this.children=t instanceof s.a?[t,...e]:e;for(const t of this.children)if(this.disposable(t),t instanceof n.a&&t.label){const e=Object(r.h)({className:"core_ToolBar_group"});"left"===t.preferred_label_position||"top"===t.preferred_label_position?e.append(t.label.element,t.element):e.append(t.element,t.label.element),this.element.append(e)}else this.element.append(t.element);this.finalize_construction()}set_enabled(t){super.set_enabled(t);for(const e of this.children)e.enabled.val=t}}},yub1:function(t,e,i){"use strict";i.d(e,"a",(function(){return c})),i.d(e,"b",(function(){return _}));var s=i("9Ykw"),n=i("rwco"),r=i("BAAx"),o=i("5cb6");const a=n.a.get("core/data_formats/parsing/ninja/texture"),u=1213027928,h=1414682200;function c(t){const e=Object(s.b)(t,!0);return t.seek_start(0),e.success&&null!=e.value.find(t=>t.type===u||t.type===h)}function _(t){const e=Object(s.a)(t);if(!e.success)return e;const i=Object(r.b)(a);i.add_result(e);const n=e.value,c=n.find(t=>t.type===u),_=c&&function(t){return{texture_count:t.u16()}}(c.data),l=n.filter(t=>t.type===h).map(t=>(function(t){const e=t.u32(),i=t.u32(),s=t.u32(),n=t.u16(),r=t.u16(),o=t.u32();return t.seek(36),{id:s,format:[e,i],width:n,height:r,size:o,data:t.array_buffer(o)}})(t.data));return _||0!==l.length?(_&&_.texture_count!==l.length&&i.add_problem(o.b.Warning,"Corrupted XVM file.",`Found ${l.length} textures instead of ${_.texture_count} as defined in the header.`),i.success({textures:l})):(i.add_problem(o.b.Error,"Corrupted XVM file.","No header and no XVRT chunks found."),i.failure())}}}]);