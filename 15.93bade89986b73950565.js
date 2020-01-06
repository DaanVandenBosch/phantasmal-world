(window.webpackJsonp=window.webpackJsonp||[]).push([[15],{ZV42:function(e,t,o){"use strict";o.r(t),o.d(t,"CharacterClassAssetLoader",(function(){return l}));var s=o("kwt4"),a=o("NRxM"),n=o("bcBH"),i=o("VwSi"),c=o("9fJw"),r=o("yub1"),d=o("/UnV"),h=o("nsCy"),_=o("BAAx"),u=function(e,t,o,s){return new(o||(o=Promise))((function(a,n){function i(e){try{r(s.next(e))}catch(e){n(e)}}function c(e){try{r(s.throw(e))}catch(e){n(e)}}function r(e){var t;e.done?a(e.value):(t=e.value,t instanceof o?t:new o((function(e){e(t)}))).then(i,c)}r((s=s.apply(e,t||[])).next())}))};class l{constructor(e){this.http_client=e,this.nj_object_cache=new Map,this.xvr_texture_cache=new Map,this.nj_motion_cache=new Map}dispose(){for(const e of this.nj_object_cache.values())e.dispose();for(const e of this.nj_motion_cache.values())e.dispose();this.nj_object_cache.clear(),this.nj_motion_cache.clear()}load_geometry(e){let t=this.nj_object_cache.get(e.name);return t||(t=this.load_all_nj_objects(e),this.nj_object_cache.set(e.name,t)),t}load_all_nj_objects(e){const t=b(e,h.e.Viridia,0);return this.load_body_part_geometry(e.name,"Body").then(o=>{if(!o)throw new Error(`Couldn't load body for player class ${e.name}.`);return this.load_body_part_geometry(e.name,"Head",0).then(s=>{if(!s)return o;let a=1+t.body.length;return this.shift_texture_ids(s,a),this.add_to_bone(o,s,59),0===e.hair_style_count?o:this.load_body_part_geometry(e.name,"Hair",0).then(n=>n?(a+=t.head.length,this.shift_texture_ids(n,a),this.add_to_bone(s,n,0),e.hair_styles_with_accessory.has(0)?this.load_body_part_geometry(e.name,"Accessory",0).then(e=>(e&&(a+=t.hair.length,this.shift_texture_ids(e,a),this.add_to_bone(n,e,0)),o)):o):o)})})}load_body_part_geometry(e,t,o){return this.http_client.get(function(e,t,o){return`/player/${e}${t}${null==o?"":o}.nj`}(e,t,o)).array_buffer().then(e=>Object(_.d)(Object(s.c)(new a.a(e,n.a.Little)))[0])}shift_texture_ids(e,t){if(e.model)for(const o of e.model.meshes)null!=o.texture_id&&(o.texture_id+=t);for(const o of e.children)this.shift_texture_ids(o,t)}add_to_bone(e,t,o){const s=e.get_bone(o);s&&(s.evaluation_flags.hidden=!1,s.evaluation_flags.break_child_trace=!1,s.add_child(t))}load_textures(e,t,o){return u(this,void 0,void 0,(function*(){let s=this.xvr_texture_cache.get(e.name);s||(s=this.http_client.get(`/player/${e.name}Tex.afs`).array_buffer().then(e=>{const t=Object(d.a)(new a.a(e,n.a.Little)),o=[];if(t.success)for(const e of t.value){const t=Object(r.b)(new a.a(e,n.a.Little));t.success&&o.push(...t.value.textures)}return o}));const i=yield s,c=b(e,t,o);return[c.section_id,...c.body,...c.head,...c.hair,...c.accessories].map(e=>null==e?void 0:i[e])}))}load_animation(e,t){let o=this.nj_motion_cache.get(e);return o||(o=this.http_client.get(`/player/animation/animation_${e.toString().padStart(3,"0")}.njm`).array_buffer().then(e=>Object(i.c)(new a.a(e,n.a.Little),t))),o}}function b(e,t,o){switch(e){case c.g:{const e=3*o;return{section_id:t+126,body:[e,e+1,e+2,o+108],head:[54,55],hair:[94,95],accessories:[]}}case c.h:{const e=13*o;return{section_id:t+299,body:[e+13,e,e+1,e+2,e+3,277,o+281],head:[235,239],hair:[260,259],accessories:[]}}case c.f:{const e=5*o;return{section_id:t+275,body:[e,e+1,e+2,o+250],head:[e+3,e+4],hair:[],accessories:[]}}case c.e:{const e=5*o;return{section_id:t+375,body:[e,e+1,e+2],head:[e+3,e+4],hair:[],accessories:[]}}case c.k:{const e=7*o;return{section_id:t+197,body:[e+4,e+5,e+6,o+179],head:[126,127],hair:[166,167],accessories:[void 0,void 0,e+2]}}case c.l:{const e=16*o;return{section_id:t+322,body:[e+15,e+1,e],head:[288],hair:[308,309],accessories:[void 0,void 0,e+8]}}case c.j:{const e=5*o;return{section_id:t+300,body:[e,e+1,e+2,e+3,o+275],head:[e+4],hair:[],accessories:[]}}case c.i:{const e=5*o;return{section_id:t+375,body:[o+350,e,e+1,e+2],head:[e+3],hair:[e+4],accessories:[]}}case c.a:{const e=0===o?0:15*o+2;return{section_id:t+310,body:[e+12,e+13,e+14,e],head:[276,272],hair:[void 0,296,297],accessories:[e+4]}}case c.b:{const e=16*o;return{section_id:t+326,body:[e,e+2,e+1,322],head:[288],hair:[void 0,void 0,308],accessories:[e+3,e+4]}}case c.d:{const e=17*o;return{section_id:t+344,body:[e+4,340,e,e+5],head:[306,310],hair:[void 0,void 0,330],accessories:[e+6,e+16,330]}}case c.c:{const e=26*o;return{section_id:t+505,body:[e+1,e,e+2,501],head:[472,468],hair:[void 0,void 0,492],accessories:[e+12,e+13]}}default:throw new Error(`No textures for character class ${e.name}.`)}}}}]);