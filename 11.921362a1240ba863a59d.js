(window.webpackJsonp=window.webpackJsonp||[]).push([[11],{490:function(e,t,n){"use strict";n.d(t,"a",(function(){return l}));var s=n(139),i=n(17),o=(n(491),n(32)),r=n(2);class l extends s.a{constructor(e,t,n,s,i){super(i),this.element=Object(r.s)({className:`${t} core_Input`}),this._value=new o.a(this,e,this.set_value),this.value=this._value,this.input_element=Object(r.m)({className:`${s} core_Input_inner`}),this.input_element.type=n,this.input_element.addEventListener("change",()=>{this._value.set_val(this.get_value(),{silent:!1})}),this.input_element.addEventListener("keydown",e=>{"Enter"===e.key&&this._value.set_val(this.get_value(),{silent:!1})}),i&&i.readonly&&this.set_attr("readOnly",!0),this.element.append(this.input_element)}set_enabled(e){super.set_enabled(e),this.input_element.disabled=!e}set_attr(e,t,n){if(null==t)return;const s=this.input_element,o=n||(e=>e);Object(i.a)(t)?(s[e]=o(t.val),this.disposable(t.observe(({value:t})=>s[e]=o(t)))):s[e]=o(t)}}},491:function(e,t,n){},633:function(e,t,n){"use strict";n.d(t,"a",(function(){return l})),n.d(t,"b",(function(){return c}));var s=n(42),i=n(2),o=n(15);n(634);const r=n(23).a.get("core/gui/Table");var l;!function(e){e[e.Asc=0]="Asc",e[e.Desc=1]="Desc"}(l||(l={}));class c extends s.a{constructor(e){super(e),this.tbody_element=Object(i.u)(),this.element=Object(i.t)({className:"core_Table"}),this.children=[],this.create_row=(e,t)=>{const n=new o.a;let s=0;return[Object(i.A)(...this.columns.map((o,l)=>{const c=o.fixed?Object(i.y)():Object(i.v)();try{const t=o.render_cell(e,n);c.append(t),o.input&&c.classList.add("input"),o.fixed&&(c.classList.add("fixed"),c.style.left=`${s}px`,s+=o.width||0),c.style.width=`${o.width}px`,o.text_align&&(c.style.textAlign=o.text_align),o.tooltip&&(c.title=o.tooltip(e))}catch(e){r.warn(`Error while rendering cell for index ${t}, column ${l}.`,e)}return c})),n]},this.update_footer=()=>{if(!this.footer_row_element)return;const e=this.columns.length;for(let t=0;t<e;t++){const e=this.columns[t];if(e.footer){const n=this.footer_row_element.children[t];n.textContent=e.footer.render_cell(),n.title=e.footer.tooltip?e.footer.tooltip():""}}},this.values=e.values,this.columns=e.columns;const t=[],n=Object(i.z)(),s=Object(i.A)();let a=0,u=!1;s.append(...this.columns.map((e,t)=>{const n=Object(i.y)({data:{index:t.toString()}},Object(i.s)(e.title));return e.fixed&&(n.style.position="sticky",n.style.left=`${a}px`,a+=e.width),n.style.width=`${e.width}px`,e.footer&&(u=!0),n}));const d=e.sort;d&&(s.onmousedown=e=>{if(e.target instanceof HTMLElement){let n=e.target;for(let e=0;e<5&&!n.dataset.index;e++){if(!n.parentElement)return;n=n.parentElement}if(!n.dataset.index)return;const s=parseInt(n.dataset.index,10),i=this.columns[s];if(!i.sortable)return;const o=t.findIndex(e=>e.column===i);if(0===o){const e=t[0];e.direction=e.direction===l.Asc?l.Desc:l.Asc}else-1!==o&&t.splice(o,1),t.unshift({column:i,direction:l.Asc});d(t)}}),n.append(s),this.tbody_element=Object(i.u)(),this.element.append(n,this.tbody_element),u&&(this.footer_row_element=Object(i.A)(),this.element.append(Object(i.x)({},this.footer_row_element)),this.create_footer()),this.disposables(Object(i.d)(this.tbody_element,this.values,this.create_row),this.values.observe(this.update_footer)),this.finalize_construction(c)}create_footer(){const e=[];let t=0;for(let n=0;n<this.columns.length;n++){const s=this.columns[n],o=Object(i.y)();o.style.width=`${s.width}px`,s.fixed&&(o.classList.add("fixed"),o.style.left=`${t}px`,t+=s.width||0),s.footer&&(o.textContent=s.footer.render_cell(),o.title=s.footer.tooltip?s.footer.tooltip():""),s.text_align&&(o.style.textAlign=s.text_align),e.push(o)}this.footer_row_element.append(...e)}}},634:function(e,t,n){},815:function(e,t,n){},816:function(e,t,n){},898:function(e,t,n){"use strict";n.r(t),n.d(t,"MethodsView",(function(){return b}));var s=n(549),i=n(137),o=n(142),r=(n(815),n(490)),l=n(523);n(816);class c extends r.a{constructor(e=l.Duration.fromMillis(0),t){super(e,"core_DurationInput","text","core_DurationInput_inner",t),this.preferred_label_position="left",this.input_element.pattern="(60|[0-5][0-9]):(60|[0-5][0-9])",this.set_value(e),this.finalize_construction(c)}get_value(){const e=this.input_element.value;if(this.input_element.validity.valid)return l.Duration.fromObject({hours:parseInt(e.slice(0,2),10),minutes:parseInt(e.slice(3),10)});{const t=e.indexOf(":");return-1===t?l.Duration.fromObject({minutes:parseInt(e,10)}):l.Duration.fromObject({hours:parseInt(e.slice(0,t),10),minutes:parseInt(e.slice(t+1),10)})}}set_value(e){this.input_element.value=e.toFormat("hh:mm")}}var a=n(633),u=n(14),d=n(23),h=n(2),p=n(82),_=function(e,t,n,s){return new(n||(n=Promise))((function(i,o){function r(e){try{c(s.next(e))}catch(e){o(e)}}function l(e){try{c(s.throw(e))}catch(e){o(e)}}function c(e){var t;e.done?i(e.value):(t=e.value,t instanceof n?t:new n((function(e){e(t)}))).then(r,l)}c((s=s.apply(e,t||[])).next())}))};const m=d.a.get("hunt_optimizer/gui/MethodsForEpisodeView");class f extends p.a{constructor(e,t){super(),this.element=Object(h.h)({className:"hunt_optimizer_MethodsForEpisodeView"}),this.episode=t,this.enemy_types=o.a.filter(e=>Object(o.d)(e).episode===this.episode);const n=Object(u.c)(),s=this.add(new a.b({class:"hunt_optimizer_MethodsForEpisodeView_table",values:n,sort:e=>{n.sort((t,n)=>{for(const{column:s,direction:i}of e){let e=0;switch(s.key){case"method":e=t.name.localeCompare(n.name);break;case"time":e=t.time.val.as("minutes")-n.time.val.as("minutes");break;default:{const i=o.c[s.key];i&&(e=(t.enemy_counts.get(i)||0)-(n.enemy_counts.get(i)||0))}}if(0!==e)return i===a.a.Asc?e:-e}return 0})},columns:[{key:"method",title:"Method",fixed:!0,width:250,sortable:!0,render_cell:e=>e.name},{key:"time",title:"Time",fixed:!0,width:60,input:!0,sortable:!0,render_cell(e,t){const n=t.add(new c(e.time.val));return t.add(n.value.observe(({value:t})=>e.set_user_time(t))),n.element}},...this.enemy_types.map(e=>({key:o.c[e],title:Object(o.d)(e).simple_name,width:90,text_align:"right",sortable:!0,render_cell(t){const n=t.enemy_counts.get(e);return null==n?"":n.toString()}}))]}));this.element.append(s.element),this.disposables(e.current.observe(({value:e})=>_(this,void 0,void 0,(function*(){try{const t=yield e;this.hunt_methods_observer&&this.hunt_methods_observer.dispose(),this.hunt_methods_observer=t.methods.observe(({value:e})=>{n.val=e.filter(e=>e.episode===this.episode)},{call_now:!0})}catch(e){m.error("Couldn't load hunt optimizer store.",e)}})),{call_now:!0})),this.finalize_construction(f)}dispose(){super.dispose(),this.hunt_methods_observer&&this.hunt_methods_observer.dispose()}}var v=function(e,t,n,s){return new(n||(n=Promise))((function(i,o){function r(e){try{c(s.next(e))}catch(e){o(e)}}function l(e){try{c(s.throw(e))}catch(e){o(e)}}function c(e){var t;e.done?i(e.value):(t=e.value,t instanceof n?t:new n((function(e){e(t)}))).then(r,l)}c((s=s.apply(e,t||[])).next())}))};class b extends s.a{constructor(e,t){super(e,{class:"hunt_optimizer_MethodsView",tabs:[{title:"Episode I",key:"episode_1",path:"/methods/episode_1",create_view:()=>v(this,void 0,void 0,(function*(){return new f(t,i.b.I)}))},{title:"Episode II",key:"episode_2",path:"/methods/episode_2",create_view:()=>v(this,void 0,void 0,(function*(){return new f(t,i.b.II)}))},{title:"Episode IV",key:"episode_4",path:"/methods/episode_4",create_view:()=>v(this,void 0,void 0,(function*(){return new f(t,i.b.IV)}))}]}),this.finalize_construction(b)}}}}]);