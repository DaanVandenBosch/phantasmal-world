(window.webpackJsonp=window.webpackJsonp||[]).push([[10],{488:function(e,t,s){"use strict";s.d(t,"a",(function(){return r}));var i=s(42),n=(s(538),s(139)),o=s(2);class r extends i.a{constructor(e,...t){super(e instanceof i.a?void 0:e),this.element=Object(o.h)({className:"core_ToolBar"}),this.height=33,this.element.style.height=`${this.height}px`,this.children=e instanceof i.a?[e,...t]:t;for(const e of this.children)if(this.disposable(e),e instanceof n.a&&e.label){const t=Object(o.h)({className:"core_ToolBar_group"});"left"===e.preferred_label_position||"top"===e.preferred_label_position?t.append(e.label.element,e.element):t.append(e.element,e.label.element),this.element.append(t)}else this.element.append(e.element);this.finalize_construction(r)}set_enabled(e){super.set_enabled(e);for(const t of this.children)t.enabled.val=e}}},498:function(e,t,s){"use strict";s.d(t,"a",(function(){return n})),s.d(t,"b",(function(){return o}));var i=s(2);function n(e){return new Promise(t=>{var s,n;const o=Object(i.m)({type:"file"});o.accept=null!==(s=null==e?void 0:e.accept)&&void 0!==s?s:"",o.multiple=null!==(n=null==e?void 0:e.multiple)&&void 0!==n&&n,o.onchange=()=>{o.files&&o.files.length?t([...o.files]):t([])},o.click()})}function o(e){return new Promise((t,s)=>{const i=new FileReader;i.addEventListener("loadend",()=>{i.result instanceof ArrayBuffer?t(i.result):s(new Error("Couldn't read file."))}),i.readAsArrayBuffer(e)})}},522:function(e,t,s){"use strict";s.d(t,"a",(function(){return h}));var i=s(141),n=s(2),o=(s(542),s(17)),r=s(32),l=s(14);const c=500,a=500;class h extends i.a{constructor(e){var t;super(e),this.x=0,this.y=0,this.prev_mouse_x=0,this.prev_mouse_y=0,this._title=new r.a(this,"",this.set_title),this._description=new r.a(this,"",this.set_description),this._content=new r.a(this,"",this.set_content),this._ondismiss=Object(l.a)(),this.children=[],this.title=this._title,this.description=this._description,this.content=this._content,this.ondismiss=this._ondismiss,this.mousedown=e=>{this.prev_mouse_x=e.clientX,this.prev_mouse_y=e.clientY,window.addEventListener("mousemove",this.window_mousemove),window.addEventListener("mouseup",this.window_mouseup)},this.window_mousemove=e=>{e.preventDefault(),this.set_position(this.x+e.clientX-this.prev_mouse_x,this.y+e.clientY-this.prev_mouse_y),this.prev_mouse_x=e.clientX,this.prev_mouse_y=e.clientY},this.window_mouseup=e=>{e.preventDefault(),window.removeEventListener("mousemove",this.window_mousemove),window.removeEventListener("mouseup",this.window_mouseup)},this.element=Object(n.q)({className:"core_Dialog",tabIndex:0},this.header_element=Object(n.i)(),this.description_element=Object(n.h)({className:"core_Dialog_description"}),this.content_element=Object(n.h)({className:"core_Dialog_body"}),Object(n.h)({className:"core_Dialog_footer"},...null!==(t=null==e?void 0:e.footer)&&void 0!==t?t:[])),this.element.style.width=`${c}px`,this.element.style.maxHeight=`${a}px`,this.element.addEventListener("keydown",e=>this.keydown(e)),e&&("string"==typeof e.title?this.title.val=e.title:e.title&&this.title.bind_to(e.title),"string"==typeof e.description?this.description.val=e.description:e.description&&this.description.bind_to(e.description),Object(o.a)(e.content)?this.content.bind_to(e.content):null!=e.content&&(this.content.val=e.content)),this.set_position((window.innerWidth-c)/2,(window.innerHeight-a)/2),this.header_element.addEventListener("mousedown",this.mousedown),this.overlay_element=Object(n.h)({className:"core_Dialog_modal_overlay",tabIndex:-1}),this.overlay_element.addEventListener("focus",()=>this.focus()),this.finalize_construction(h)}dispose(){super.dispose(),this.overlay_element.remove()}focus(){(this.first_focusable_child(this.element)||this.element).focus()}first_focusable_child(e){for(const t of e.children)if(t instanceof HTMLElement){if(t.tabIndex>=0)return t;{const e=this.first_focusable_child(t);if(e)return e}}}set_position(e,t){this.x=e,this.y=t,this.element.style.transform=`translate(${Math.floor(e)}px, ${Math.floor(t)}px)`}set_visible(e){e?(document.body.append(this.overlay_element),document.body.append(this.element),this.focus()):(this.overlay_element.remove(),this.element.remove())}set_title(e){this.header_element.textContent=e}set_description(e){""===e?(this.description_element.hidden=!0,this.description_element.textContent=""):(this.description_element.hidden=!1,this.description_element.textContent=e)}set_content(e){this.content_element.textContent="",this.content_element.append(e)}keydown(e){"Escape"===e.key&&this._ondismiss.emit({value:e})}}},528:function(e,t,s){"use strict";s.d(t,"a",(function(){return o}));var i=s(141),n=s(2);class o extends i.a{constructor(e){super(),this.renderer=e,this.element=Object(n.h)({className:"core_RendererWidget"}),this.children=[],this.element.append(e.canvas_element),this.disposable(e),this.finalize_construction(o)}activate(){this.renderer.start_rendering(),super.activate()}deactivate(){super.deactivate(),this.renderer.stop_rendering()}resize(e,t){return super.resize(e,t),this.renderer.set_size(e,t),this}}},538:function(e,t,s){},541:function(e,t,s){"use strict";s.d(t,"a",(function(){return l}));var i=s(14),n=s(138),o=s(498),r=function(e,t,s,i){return new(s||(s=Promise))((function(n,o){function r(e){try{c(i.next(e))}catch(e){o(e)}}function l(e){try{c(i.throw(e))}catch(e){o(e)}}function c(e){var t;e.done?n(e.value):(t=e.value,t instanceof s?t:new s((function(e){e(t)}))).then(r,l)}c((i=i.apply(e,t||[])).next())}))};class l extends n.a{constructor(e){super(e),this._files=Object(i.e)([]),this.files=this._files,this.element.classList.add("core_FileButton"),this.disposables(this.onclick.observe(()=>r(this,void 0,void 0,(function*(){this._files.val=yield Object(o.a)(e)})))),this.finalize_construction(l)}}},542:function(e,t,s){},543:function(e,t,s){"use strict";s.d(t,"a",(function(){return c}));var i=s(522),n=s(138),o=s(17),r=s(2),l=s(14);class c extends i.a{constructor(e){const t=new n.a({text:"Dismiss"});super(Object.assign({footer:[t.element]},e));const s=Object(o.a)(e.result)?e.result:Object(l.e)(e.result);this.problems_message=Object(o.a)(e.problems_message)?e.problems_message:Object(l.e)(e.problems_message),this.error_message=Object(o.a)(e.error_message)?e.error_message:Object(l.e)(e.error_message),this.disposables(t,t.onclick.observe(e=>this._ondismiss.emit(e)),s.observe(({value:e})=>this.result_changed(e),{call_now:!0})),this.finalize_construction(c)}result_changed(e){e?(this.content.val=function(e){const t=Object(r.h)();t.style.overflow="auto",t.style.userSelect="text",t.style.height="100%",t.style.maxHeight="400px";const s=Object(r.B)(...e.problems.map(e=>Object(r.o)(e.ui_message)));return s.style.cursor="text",t.append(s),t}(e),e.success?e.problems.length&&(this.title.val="Problems",this.description.val=this.problems_message.val):(this.title.val="Error",this.description.val=this.error_message.val)):this.content.val=""}}},833:function(e,t,s){"use strict";s.r(t),s.d(t,"TextureView",(function(){return a}));var i=s(2),n=s(541),o=s(488),r=s(528),l=s(82),c=s(543);class a extends l.a{constructor(e,t){super(),this.element=Object(i.h)({className:"viewer_TextureView"}),this.open_file_button=new n.a({icon_left:i.a.File,text:"Open file...",accept:".afs, .xvm"}),this.tool_bar=this.add(new o.a(this.open_file_button)),this.renderer_widget=this.add(new r.a(t)),this.element.append(this.tool_bar.element,this.renderer_widget.element);const s=this.disposable(new c.a({visible:e.result_dialog_visible,result:e.result,problems_message:e.result_problems_message,error_message:e.result_error_message}));this.disposables(this.open_file_button.files.observe(({value:t})=>{t.length&&e.load_file(t[0])}),s.ondismiss.observe(e.dismiss_result_dialog)),this.finalize_construction(a)}resize(e,t){return super.resize(e,t),this.renderer_widget.resize(e,Math.max(0,t-this.tool_bar.height)),this}}}}]);