(window.webpackJsonp=window.webpackJsonp||[]).push([[13],{"7CyS":function(t,e,n){"use strict";n.d(e,"a",(function(){return a})),n.d(e,"b",(function(){return r}));var i=n("Womt"),o=n("VwSi");const a=30;function r(t,e){const n=e.interpolation===o.a.Spline?i.InterpolateSmooth:i.InterpolateLinear,r=[];return e.motion_data.forEach((e,s)=>{const _=t.get_bone(s);_&&e.tracks.forEach(({type:t,keyframes:e})=>{const c=[],u=[];for(const n of e)if(c.push(n.frame/a),t===o.b.Rotation){const t=_.evaluation_flags.zxy_rotation_order?"ZXY":"ZYX",e=(new i.Quaternion).setFromEuler(new i.Euler(n.value.x,n.value.y,n.value.z,t));u.push(e.x,e.y,e.z,e.w)}else u.push(n.value.x,n.value.y,n.value.z);if(t===o.b.Rotation)r.push(new i.QuaternionKeyframeTrack(`.bones[${s}].quaternion`,c,u,n));else{const e=t===o.b.Position?`.bones[${s}].position`:`.bones[${s}].scale`;r.push(new i.VectorKeyframeTrack(e,c,u,n))}})}),new i.AnimationClip("Animation",(e.frame_count-1)/a,r).optimize()}},"Sa3/":function(t,e,n){"use strict";n.d(e,"a",(function(){return o}));var i=function(t,e,n,i){return new(n||(n=Promise))((function(o,a){function r(t){try{_(i.next(t))}catch(t){a(t)}}function s(t){try{_(i.throw(t))}catch(t){a(t)}}function _(t){var e;t.done?o(t.value):(e=t.value,e instanceof n?e:new n((function(t){t(e)}))).then(r,s)}_((i=i.apply(t,e||[])).next())}))};function o(t){return i(this,void 0,void 0,(function*(){return new Promise((e,n)=>{const i=new FileReader;i.addEventListener("loadend",()=>{i.result instanceof ArrayBuffer?e(i.result):n(new Error("Couldn't read file."))}),i.readAsArrayBuffer(t)})}))}},tyuj:function(t,e,n){"use strict";n.r(e);var i=n("NRxM"),o=n("bcBH"),a=n("VwSi"),r=n("kwt4"),s=n("9fJw");class _{constructor(t,e){this.id=t,this.name=e}}var c=n("Sa3/"),u=n("ouMO"),h=n("7CyS"),l=n("yub1"),d=n("kcKQ"),m=n("rwco"),f=n("/UnV"),v=n("wtpc"),j=n("nsCy");n.d(e,"Model3DStore",(function(){return w}));var b=function(t,e,n,i){return new(n||(n=Promise))((function(o,a){function r(t){try{_(i.next(t))}catch(t){a(t)}}function s(t){try{_(i.throw(t))}catch(t){a(t)}}function _(t){var e;t.done?o(t.value):(e=t.value,e instanceof n?e:new n((function(t){t(e)}))).then(r,s)}_((i=i.apply(t,e||[])).next())}))};const y=m.c.get("viewer/stores/ModelStore");class w extends d.a{constructor(t){super(),this.asset_loader=t,this._current_model=Object(u.e)(void 0),this._current_nj_data=Object(u.e)(void 0),this._current_textures=Object(u.c)(),this._show_skeleton=Object(u.e)(!1),this._current_animation=Object(u.e)(void 0),this._current_nj_motion=Object(u.e)(void 0),this._animation_playing=Object(u.e)(!0),this._animation_frame_rate=Object(u.e)(h.a),this._animation_frame=Object(u.e)(0),this.models=[s.g,s.h,s.f,s.e,s.k,s.l,s.j,s.i,s.a,s.b,s.d,s.c],this.animations=new Array(572).fill(void 0).map((t,e)=>new _(e,`Animation ${e+1}`)),this.current_model=this._current_model,this.current_nj_data=this._current_nj_data,this.current_textures=this._current_textures,this.show_skeleton=this._show_skeleton,this.current_animation=this._current_animation,this.current_nj_motion=this._current_nj_motion,this.animation_playing=this._animation_playing,this.animation_frame_rate=this._animation_frame_rate,this.animation_frame=this._animation_frame,this.animation_frame_count=this.current_nj_motion.map(t=>t?t.frame_count:0),this.set_current_model=t=>{this._current_model.val=t},this.set_show_skeleton=t=>{this._show_skeleton.val=t},this.set_current_animation=t=>{this._current_animation.val=t},this.set_animation_playing=t=>{this._animation_playing.val=t},this.set_animation_frame_rate=t=>{this._animation_frame_rate.val=t},this.set_animation_frame=t=>{this._animation_frame.val=t},this.load_file=t=>b(this,void 0,void 0,(function*(){try{const e=yield Object(c.a)(t),n=new i.a(e,o.a.Little);if(t.name.endsWith(".nj")){this.set_current_model(void 0),this._current_textures.clear();const t=Object(r.c)(n)[0];this.set_current_nj_data({nj_object:t,bone_count:t.bone_count(),has_skeleton:!0})}else if(t.name.endsWith(".xj")){this.set_current_model(void 0),this._current_textures.clear();const t=Object(r.d)(n)[0];this.set_current_nj_data({nj_object:t,bone_count:0,has_skeleton:!1})}else if(t.name.endsWith(".njm")){this.set_current_animation(void 0),this._current_nj_motion.val=void 0;const t=this.current_nj_data.val;t&&(this.set_animation_playing(!0),this._current_nj_motion.val=Object(a.c)(n,t.bone_count))}else if(t.name.endsWith(".xvm"))this._current_textures.val=Object(l.a)(n).textures;else if(t.name.endsWith(".afs")){const t=Object(f.a)(n),e=[];for(const n of t)e.push(...Object(l.a)(new i.a(n,o.a.Little)).textures);this._current_textures.val=e}else y.error(`Unknown file extension in filename "${t.name}".`)}catch(t){y.error("Couldn't read file.",t)}})),this.load_model=t=>b(this,void 0,void 0,(function*(){if(this.set_current_animation(void 0),t)try{this.set_current_nj_data(void 0);const e=yield this.asset_loader.load_geometry(t);this._current_textures.val=yield this.asset_loader.load_textures(t,Object(v.h)(j.f),Object(v.i)(0,t.body_style_count)),this.set_current_nj_data({nj_object:e,bone_count:t?64:e.bone_count(),has_skeleton:!0})}catch(e){y.error(`Couldn't load model for ${t.name}.`),this._current_nj_data.val=void 0}else this._current_nj_data.val=void 0})),this.load_animation=t=>b(this,void 0,void 0,(function*(){const e=this.current_nj_data.val;if(e&&t)try{this._current_nj_motion.val=yield this.asset_loader.load_animation(t.id,e.bone_count),this.set_animation_playing(!0)}catch(e){y.error(`Couldn't load animation "${t.name}".`),this._current_nj_motion.val=void 0}else this._current_nj_motion.val=void 0})),this.disposables(this.current_model.observe(({value:t})=>this.load_model(t)),this.current_animation.observe(({value:t})=>this.load_animation(t))),this.set_current_model(Object(v.h)(this.models))}set_current_nj_data(t){this._current_nj_data.val=t}}}}]);