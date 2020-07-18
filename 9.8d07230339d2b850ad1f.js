(window.webpackJsonp=window.webpackJsonp||[]).push([[9],{488:function(e,t,s){"use strict";s.d(t,"a",(function(){return r}));var n=s(42),i=(s(538),s(139)),o=s(2);class r extends n.a{constructor(e,...t){super(e instanceof n.a?void 0:e),this.element=Object(o.h)({className:"core_ToolBar"}),this.height=33,this.element.style.height=`${this.height}px`,this.children=e instanceof n.a?[e,...t]:t;for(const e of this.children)if(this.disposable(e),e instanceof i.a&&e.label){const t=Object(o.h)({className:"core_ToolBar_group"});"left"===e.preferred_label_position||"top"===e.preferred_label_position?t.append(e.label.element,e.element):t.append(e.element,e.label.element),this.element.append(t)}else this.element.append(e.element);this.finalize_construction(r)}set_enabled(e){super.set_enabled(e);for(const t of this.children)t.enabled.val=e}}},490:function(e,t,s){"use strict";s.d(t,"a",(function(){return a}));var n=s(139),i=s(17),o=(s(491),s(32)),r=s(2);class a extends n.a{constructor(e,t,s,n,i){super(i),this.element=Object(r.s)({className:`${t} core_Input`}),this._value=new o.a(this,e,this.set_value),this.value=this._value,this.input_element=Object(r.m)({className:`${n} core_Input_inner`}),this.input_element.type=s,this.input_element.addEventListener("change",()=>{this._value.set_val(this.get_value(),{silent:!1})}),this.input_element.addEventListener("keydown",e=>{"Enter"===e.key&&this._value.set_val(this.get_value(),{silent:!1})}),i&&i.readonly&&this.set_attr("readOnly",!0),this.element.append(this.input_element)}set_enabled(e){super.set_enabled(e),this.input_element.disabled=!e}set_attr(e,t,s){if(null==t)return;const n=this.input_element,o=s||(e=>e);Object(i.a)(t)?(n[e]=o(t.val),this.disposable(t.observe(({value:t})=>n[e]=o(t)))):n[e]=o(t)}}},491:function(e,t,s){},498:function(e,t,s){"use strict";s.d(t,"a",(function(){return i})),s.d(t,"b",(function(){return o}));var n=s(2);function i(e){return new Promise(t=>{var s,i;const o=Object(n.m)({type:"file"});o.accept=null!==(s=null==e?void 0:e.accept)&&void 0!==s?s:"",o.multiple=null!==(i=null==e?void 0:e.multiple)&&void 0!==i&&i,o.onchange=()=>{o.files&&o.files.length?t([...o.files]):t([])},o.click()})}function o(e){return new Promise((t,s)=>{const n=new FileReader;n.addEventListener("loadend",()=>{n.result instanceof ArrayBuffer?t(n.result):s(new Error("Couldn't read file."))}),n.readAsArrayBuffer(e)})}},501:function(e,t,s){"use strict";s.d(t,"a",(function(){return i}));var n=s(490);s(524);class i extends n.a{constructor(e=0,t){if(super(e,"core_NumberInput","number","core_NumberInput_inner",t),this.preferred_label_position="left",t){const{min:e,max:s,step:n,width:i}=t;this.set_attr("min",e,String),this.set_attr("max",s,String),this.input_element.step="any",this.set_attr("step",n,String),null!=i&&(this.element.style.width=`${i}px`)}t&&null!=t.round_to&&t.round_to>=0?this.rounding_factor=Math.pow(10,t.round_to):this.rounding_factor=1,this.set_value(e),this.finalize_construction(i)}get_value(){return parseFloat(this.input_element.value)}set_value(e){this.input_element.valueAsNumber=Math.round(this.rounding_factor*e)/this.rounding_factor}}},522:function(e,t,s){"use strict";s.d(t,"a",(function(){return h}));var n=s(141),i=s(2),o=(s(542),s(17)),r=s(32),a=s(14);const l=500,c=500;class h extends n.a{constructor(e){var t;super(e),this.x=0,this.y=0,this.prev_mouse_x=0,this.prev_mouse_y=0,this._title=new r.a(this,"",this.set_title),this._description=new r.a(this,"",this.set_description),this._content=new r.a(this,"",this.set_content),this._ondismiss=Object(a.a)(),this.children=[],this.title=this._title,this.description=this._description,this.content=this._content,this.ondismiss=this._ondismiss,this.mousedown=e=>{this.prev_mouse_x=e.clientX,this.prev_mouse_y=e.clientY,window.addEventListener("mousemove",this.window_mousemove),window.addEventListener("mouseup",this.window_mouseup)},this.window_mousemove=e=>{e.preventDefault(),this.set_position(this.x+e.clientX-this.prev_mouse_x,this.y+e.clientY-this.prev_mouse_y),this.prev_mouse_x=e.clientX,this.prev_mouse_y=e.clientY},this.window_mouseup=e=>{e.preventDefault(),window.removeEventListener("mousemove",this.window_mousemove),window.removeEventListener("mouseup",this.window_mouseup)},this.element=Object(i.q)({className:"core_Dialog",tabIndex:0},this.header_element=Object(i.i)(),this.description_element=Object(i.h)({className:"core_Dialog_description"}),this.content_element=Object(i.h)({className:"core_Dialog_body"}),Object(i.h)({className:"core_Dialog_footer"},...null!==(t=null==e?void 0:e.footer)&&void 0!==t?t:[])),this.element.style.width=`${l}px`,this.element.style.maxHeight=`${c}px`,this.element.addEventListener("keydown",e=>this.keydown(e)),e&&("string"==typeof e.title?this.title.val=e.title:e.title&&this.title.bind_to(e.title),"string"==typeof e.description?this.description.val=e.description:e.description&&this.description.bind_to(e.description),Object(o.a)(e.content)?this.content.bind_to(e.content):null!=e.content&&(this.content.val=e.content)),this.set_position((window.innerWidth-l)/2,(window.innerHeight-c)/2),this.header_element.addEventListener("mousedown",this.mousedown),this.overlay_element=Object(i.h)({className:"core_Dialog_modal_overlay",tabIndex:-1}),this.overlay_element.addEventListener("focus",()=>this.focus()),this.finalize_construction(h)}dispose(){super.dispose(),this.overlay_element.remove()}focus(){(this.first_focusable_child(this.element)||this.element).focus()}first_focusable_child(e){for(const t of e.children)if(t instanceof HTMLElement){if(t.tabIndex>=0)return t;{const e=this.first_focusable_child(t);if(e)return e}}}set_position(e,t){this.x=e,this.y=t,this.element.style.transform=`translate(${Math.floor(e)}px, ${Math.floor(t)}px)`}set_visible(e){e?(document.body.append(this.overlay_element),document.body.append(this.element),this.focus()):(this.overlay_element.remove(),this.element.remove())}set_title(e){this.header_element.textContent=e}set_description(e){""===e?(this.description_element.hidden=!0,this.description_element.textContent=""):(this.description_element.hidden=!1,this.description_element.textContent=e)}set_content(e){this.content_element.textContent="",this.content_element.append(e)}keydown(e){"Escape"===e.key&&this._ondismiss.emit({value:e})}}},524:function(e,t,s){},538:function(e,t,s){},541:function(e,t,s){"use strict";s.d(t,"a",(function(){return a}));var n=s(14),i=s(138),o=s(498),r=function(e,t,s,n){return new(s||(s=Promise))((function(i,o){function r(e){try{l(n.next(e))}catch(e){o(e)}}function a(e){try{l(n.throw(e))}catch(e){o(e)}}function l(e){var t;e.done?i(e.value):(t=e.value,t instanceof s?t:new s((function(e){e(t)}))).then(r,a)}l((n=n.apply(e,t||[])).next())}))};class a extends i.a{constructor(e){super(e),this._files=Object(n.e)([]),this.files=this._files,this.element.classList.add("core_FileButton"),this.disposables(this.onclick.observe(()=>r(this,void 0,void 0,(function*(){this._files.val=yield Object(o.a)(e)})))),this.finalize_construction(a)}}},542:function(e,t,s){},543:function(e,t,s){"use strict";s.d(t,"a",(function(){return l}));var n=s(522),i=s(138),o=s(17),r=s(2),a=s(14);class l extends n.a{constructor(e){const t=new i.a({text:"Dismiss"});super(Object.assign({footer:[t.element]},e));const s=Object(o.a)(e.result)?e.result:Object(a.e)(e.result);this.problems_message=Object(o.a)(e.problems_message)?e.problems_message:Object(a.e)(e.problems_message),this.error_message=Object(o.a)(e.error_message)?e.error_message:Object(a.e)(e.error_message),this.disposables(t,t.onclick.observe(e=>this._ondismiss.emit(e)),s.observe(({value:e})=>this.result_changed(e),{call_now:!0})),this.finalize_construction(l)}result_changed(e){e?(this.content.val=function(e){const t=Object(r.h)();t.style.overflow="auto",t.style.userSelect="text",t.style.height="100%",t.style.maxHeight="400px";const s=Object(r.B)(...e.problems.map(e=>Object(r.o)(e.ui_message)));return s.style.cursor="text",t.append(s),t}(e),e.success?e.problems.length&&(this.title.val="Problems",this.description.val=this.problems_message.val):(this.title.val="Error",this.description.val=this.error_message.val)):this.content.val=""}}},546:function(e,t,s){"use strict";s.d(t,"a",(function(){return o})),s.d(t,"b",(function(){return r}));var n=s(136),i=s(544);const o=30;function r(e,t){const s=t.interpolation===i.a.Spline?n.InterpolateSmooth:n.InterpolateLinear,r=[];return t.motion_data.forEach((t,a)=>{const l=e.get_bone(a);l&&t.tracks.forEach(({type:e,keyframes:t})=>{const c=[],h=[];for(const s of t)if(c.push(s.frame/o),e===i.b.Rotation){const e=l.evaluation_flags.zxy_rotation_order?"ZXY":"ZYX",t=(new n.Quaternion).setFromEuler(new n.Euler(s.value.x,s.value.y,s.value.z,e));h.push(t.x,t.y,t.z,t.w)}else h.push(s.value.x,s.value.y,s.value.z);if(e===i.b.Rotation)r.push(new n.QuaternionKeyframeTrack(`.bones[${a}].quaternion`,c,h,s));else{const t=e===i.b.Position?`.bones[${a}].position`:`.bones[${a}].scale`;r.push(new n.VectorKeyframeTrack(t,c,h,s))}})}),new n.AnimationClip("Animation",(t.frame_count-1)/o,r).optimize()}},578:function(e,t,s){"use strict";s.d(t,"a",(function(){return r}));var n=s(139),i=s(32),o=s(2);class r extends n.a{constructor(e=!1,t){super(t),this.element=Object(o.m)({className:"core_CheckBox"}),this.preferred_label_position="right",this._checked=new i.a(this,e,this.set_checked),this.checked=this._checked,this.set_checked(e),this.element.type="checkbox",this.element.onchange=()=>this._checked.set_val(this.element.checked,{silent:!1}),this.finalize_construction(r)}set_enabled(e){super.set_enabled(e),this.element.disabled=!e}set_checked(e){this.element.checked=e}}},827:function(e,t,s){"use strict";s.r(t),s.d(t,"ModelToolBarView",(function(){return d}));var n=s(488),i=s(541),o=s(578),r=s(501),a=s(546),l=s(144),c=s(2),h=s(83),u=s(543);class d extends h.a{constructor(e){super();const t=new i.a({icon_left:c.a.File,text:"Open file...",accept:".afs, .nj, .njm, .xj, .xvm"}),s=new o.a(!1,{label:"Show skeleton"}),h=new o.a(!0,{label:"Play animation"}),_=new r.a(a.a,{label:"Frame rate:",min:1,max:240,step:1}),m=new r.a(1,{label:"Frame:",min:1,max:e.animation_frame_count,step:1}),p=new l.a(e.animation_frame_count_label);this.toolbar=this.add(new n.a(t,s,h,_,m,p));const f=this.disposable(new u.a({visible:e.result_dialog_visible,result:e.result,problems_message:e.result_problems_message,error_message:e.result_error_message}));this.disposables(t.files.observe(({value:t})=>{t.length&&e.load_file(t[0])}),s.checked.observe(({value:t})=>e.set_show_skeleton(t)),f.ondismiss.observe(e.dismiss_result_dialog));const v=e.animation_controls_enabled;this.disposables(h.enabled.bind_to(v),h.checked.bind_to(e.animation_playing),h.checked.observe(({value:t})=>e.set_animation_playing(t)),_.enabled.bind_to(v),_.value.observe(({value:t})=>e.set_animation_frame_rate(t)),m.enabled.bind_to(v),m.value.bind_to(e.animation_frame),m.value.observe(({value:t})=>e.set_animation_frame(t)),p.enabled.bind_to(v)),this.finalize_construction(d)}get element(){return this.toolbar.element}get height(){return this.toolbar.height}}}}]);