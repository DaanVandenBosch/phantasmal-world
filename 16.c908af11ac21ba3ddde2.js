(window.webpackJsonp=window.webpackJsonp||[]).push([[16],{"0Xme":function(e,t,i){"use strict";i.d(t,"a",(function(){return r}));var s=i("sDu+"),n=i("jJhE"),o=(i("x1yY"),i("4VsB")),l=i("FXpb");class r extends s.a{constructor(e,t,i,s,n){super(n),this.element=Object(l.s)({className:`${t} core_Input`}),this._value=new o.a(this,e,this.set_value),this.value=this._value,this.input_element=Object(l.m)({className:`${s} core_Input_inner`}),this.input_element.type=i,this.input_element.addEventListener("change",()=>{this._value.set_val(this.get_value(),{silent:!1})}),this.input_element.addEventListener("keydown",e=>{"Enter"===e.key&&this._value.set_val(this.get_value(),{silent:!1})}),n&&n.readonly&&this.set_attr("readOnly",!0),this.element.append(this.input_element)}set_enabled(e){super.set_enabled(e),this.input_element.disabled=!e}set_attr(e,t,i){if(null==t)return;const s=this.input_element,o=i||(e=>e);Object(n.a)(t)?(s[e]=o(t.val),this.disposable(t.observe(({value:t})=>s[e]=o(t)))):s[e]=o(t)}}},FNQV:function(e,t,i){"use strict";i.r(t);var s=i("FXpb"),n=(i("XfQH"),i("KVVN")),o=i("Tpfs"),l=i("PE7g"),r=i("sDu+"),a=(i("rlVZ"),i("x1yY"),i("LSoc")),c=i("4VsB");class u extends r.a{constructor(e){super(e),this.element=Object(s.s)({className:"core_ComboBox core_Input"}),this.preferred_label_position="left",this.input_element=Object(s.m)(),this.to_label=e.to_label,this._selected=new c.a(this,void 0,this.set_selected),this.selected=this._selected,this.menu=this.disposable(new a.a({items:e.items,to_label:e.to_label,related_element:this.element})),this.menu.element.onmousedown=e=>e.preventDefault(),this.input_element.placeholder=e.placeholder_text||"",this.input_element.onmousedown=()=>{this.menu.visible.set_val(!0,{silent:!1})},this.input_element.onkeydown=e=>{switch(e.key){case"ArrowDown":e.preventDefault(),this.menu.hover_next();break;case"ArrowUp":e.preventDefault(),this.menu.hover_prev();break;case"Enter":this.menu.select_hovered()}};const t=e.filter;if(t){let e="";this.input_element.onkeyup=()=>{this.input_element.value!==e&&(e=this.input_element.value,t(e),(this.menu.visible.val||e)&&this.menu.hover_next())}}this.input_element.onblur=()=>{this.menu.visible.set_val(!1,{silent:!1})};const i=Object(s.k)(s.a.TriangleDown),n=Object(s.k)(s.a.TriangleUp),o=Object(s.s)({className:"core_ComboBox_button"},i,n);o.onmousedown=e=>{e.preventDefault(),this.menu.visible.set_val(!this.menu.visible.val,{silent:!1})},this.element.append(Object(s.s)({className:"core_ComboBox_inner core_Input_inner"},this.input_element,o),this.menu.element),this.disposables(this.menu.visible.observe(({value:e})=>{e&&this.menu.hover_next()}),this.menu.selected.observe(({value:e})=>{this.selected.set_val(e,{silent:!1}),this.input_element.focus()}),Object(s.c)(n,"hidden",this.menu.visible.map(e=>!e)),Object(s.c)(i,"hidden",this.menu.visible)),this.finalize_construction()}set_selected(e){this.input_element.value=e?this.to_label(e):"",this.menu.selected.val=e}}var h=i("ouMO"),d=i("rwco"),_=i("6Fug"),m=function(e,t,i,s){return new(i||(i=Promise))((function(n,o){function l(e){try{a(s.next(e))}catch(e){o(e)}}function r(e){try{a(s.throw(e))}catch(e){o(e)}}function a(e){var t;e.done?n(e.value):(t=e.value,t instanceof i?t:new i((function(e){e(t)}))).then(l,r)}a((s=s.apply(e,t||[])).next())}))};const p=d.a.get("hunt_optimizer/gui/WantedItemsView");class b extends _.a{constructor(e){super(),this.hunt_optimizer_stores=e,this.tbody_element=Object(s.u)(),this.store_disposer=this.disposable(new o.a),this.element=Object(s.h)({className:"hunt_optimizer_WantedItemsView"}),this.create_row=e=>{const t=new o.a,i=t.add(new l.a(e.amount.val,{min:0,step:1}));t.add_all(i.value.bind_to(e.amount),i.value.observe(({value:t})=>e.set_amount(t)));const r=t.add(new n.a({icon_left:s.a.Remove}));return t.add(r.onclick.observe(()=>m(this,void 0,void 0,(function*(){return(yield this.hunt_optimizer_stores.current.val).remove_wanted_item(e)})))),[Object(s.A)(Object(s.v)(i.element),Object(s.v)(e.item_type.name),Object(s.v)(r.element)),t]};const t=Object(h.c)(),i=Object(h.c)(),r=this.add(new u({items:i,to_label:e=>e.name,placeholder_text:"Add an item",filter(e){const s=e.toLowerCase();i.val=t.val.filter(e=>e.name.toLowerCase().includes(s))}}));this.element.append(Object(s.j)("Wanted Items"),r.element,Object(s.h)({className:"hunt_optimizer_WantedItemsView_table_wrapper"},Object(s.t)(this.tbody_element))),this.disposables(e.current.observe(({value:e})=>m(this,void 0,void 0,(function*(){try{const n=yield e;this.store_disposer.dispose_all(),this.store_disposer.add_all(Object(s.d)(this.tbody_element,n.wanted_items,this.create_row),r.selected.observe(({value:e})=>{e&&(n.add_wanted_item(e),r.selected.val=void 0)})),t.val=n.huntable_item_types.slice().sort((e,t)=>e.name.localeCompare(t.name)),i.val=t.val}catch(e){p.error("Couldn't load hunt optimizer store.",e)}})),{call_now:!0})),this.finalize_construction()}}i("l8iQ");var f=i("yar8"),v=i("nsCy"),w=i("xs7i"),x=(i("a0xr"),i("ExVU")),y=function(e,t,i,s){return new(i||(i=Promise))((function(n,o){function l(e){try{a(s.next(e))}catch(e){o(e)}}function r(e){try{a(s.throw(e))}catch(e){o(e)}}function a(e){var t;e.done?n(e.value):(t=e.value,t instanceof i?t:new i((function(e){e(t)}))).then(l,r)}a((s=s.apply(e,t||[])).next())}))};const O=d.a.get("hunt_optimizer/gui/OptimizationResultView");class g extends _.a{constructor(e){super(),this.element=Object(s.h)({className:"hunt_optimizer_OptimizationResultView"},Object(s.j)("Ideal Combination of Methods")),this.disposable(e.current.observe(({value:e})=>y(this,void 0,void 0,(function*(){try{const t=yield e;if(this.disposed)return;this.results_observer&&this.remove_disposable(this.results_observer),this.results_observer=this.disposable(t.result.observe(({value:e})=>this.update_table(e),{call_now:!0}))}catch(e){O.error("Couldn't load hunt optimizer store.",e)}})),{call_now:!0})),this.finalize_construction()}update_table(e){this.table&&this.remove(this.table);let t=0,i=x.Duration.fromMillis(0);if(e)for(const s of e.optimal_methods)t+=s.runs,i=i.plus(s.total_time);const n=[{title:"Difficulty",fixed:!0,width:80,render_cell:e=>v.b[e.difficulty],footer:{render_cell:()=>"Totals:"}},{title:"Method",fixed:!0,width:250,render_cell:e=>e.method_name},{title:"Ep.",fixed:!0,width:40,render_cell:e=>w.b[e.method_episode]},{title:"Section ID",fixed:!0,width:90,render_cell(e){const t=Object(s.s)(...e.section_ids.map(e=>Object(s.r)(e,{size:17})));return t.style.display="flex",t}},{title:"Time/Run",width:90,text_align:"center",render_cell:e=>e.method_time.toFormat("hh:mm")},{title:"Runs",width:60,text_align:"right",render_cell:e=>e.runs.toFixed(1),tooltip:e=>e.runs.toString(),footer:{render_cell:()=>t.toFixed(1),tooltip:()=>t.toString()}},{title:"Total Hours",width:60,text_align:"right",render_cell:e=>e.total_time.as("hours").toFixed(1),tooltip:e=>e.total_time.as("hours").toString(),footer:{render_cell:()=>i.as("hours").toFixed(1),tooltip:()=>i.as("hours").toString()}}];if(e)for(const t of e.wanted_items){let i=0;for(const s of e.optimal_methods)i+=s.item_counts.get(t)||0;n.push({title:t.name,width:80,text_align:"right",render_cell(e){const i=e.item_counts.get(t);return i?i.toFixed(2):""},tooltip(e){const i=e.item_counts.get(t);return i?i.toString():""},footer:{render_cell:()=>i.toFixed(2),tooltip:()=>i.toString()}})}this.table=this.add(new f.b({class:"hunt_optimizer_OptimizationResultView_table",values:e?Object(h.c)(void 0,...e.optimal_methods):Object(h.c)(),columns:n})),this.element.append(this.table.element)}}var j=i("zsIO");i.d(t,"OptimizerView",(function(){return z}));class z extends j.a{constructor(e){super(),this.element=Object(s.h)({className:"hunt_optimizer_OptimizerView"}),this.element.append(this.add(new b(e)).element,this.add(new g(e)).element),this.finalize_construction()}}},PE7g:function(e,t,i){"use strict";i.d(t,"a",(function(){return n}));var s=i("0Xme");i("rY5A");class n extends s.a{constructor(e=0,t){if(super(e,"core_NumberInput","number","core_NumberInput_inner",t),this.preferred_label_position="left",t){const{min:e,max:i,step:s,width:n}=t;this.set_attr("min",e,String),this.set_attr("max",i,String),this.input_element.step="any",this.set_attr("step",s,String),null!=n&&(this.element.style.width=`${n}px`)}t&&null!=t.round_to&&t.round_to>=0?this.rounding_factor=Math.pow(10,t.round_to):this.rounding_factor=1,this.set_value(e),this.finalize_construction()}get_value(){return parseFloat(this.input_element.value)}set_value(e){this.input_element.valueAsNumber=Math.round(this.rounding_factor*e)/this.rounding_factor}}},yar8:function(e,t,i){"use strict";i.d(t,"a",(function(){return r})),i.d(t,"b",(function(){return a}));var s=i("TRid"),n=i("FXpb"),o=i("Tpfs");i("/KYB");const l=i("rwco").a.get("core/gui/Table");var r;!function(e){e[e.Asc=0]="Asc",e[e.Desc=1]="Desc"}(r||(r={}));class a extends s.a{constructor(e){super(e),this.tbody_element=Object(n.u)(),this.element=Object(n.t)({className:"core_Table"}),this.children=[],this.create_row=(e,t)=>{const i=new o.a;let s=0;return[Object(n.A)(...this.columns.map((o,r)=>{const a=o.fixed?Object(n.y)():Object(n.v)();try{const t=o.render_cell(e,i);a.append(t),o.input&&a.classList.add("input"),o.fixed&&(a.classList.add("fixed"),a.style.left=`${s}px`,s+=o.width||0),a.style.width=`${o.width}px`,o.text_align&&(a.style.textAlign=o.text_align),o.tooltip&&(a.title=o.tooltip(e))}catch(e){l.warn(`Error while rendering cell for index ${t}, column ${r}.`,e)}return a})),i]},this.update_footer=()=>{if(!this.footer_row_element)return;const e=this.columns.length;for(let t=0;t<e;t++){const e=this.columns[t];if(e.footer){const i=this.footer_row_element.children[t];i.textContent=e.footer.render_cell(),i.title=e.footer.tooltip?e.footer.tooltip():""}}},this.values=e.values,this.columns=e.columns;const t=[],i=Object(n.z)(),s=Object(n.A)();let a=0,c=!1;s.append(...this.columns.map((e,t)=>{const i=Object(n.y)({data:{index:t.toString()}},Object(n.s)(e.title));return e.fixed&&(i.style.position="sticky",i.style.left=`${a}px`,a+=e.width),i.style.width=`${e.width}px`,e.footer&&(c=!0),i}));const u=e.sort;u&&(s.onmousedown=e=>{if(e.target instanceof HTMLElement){let i=e.target;for(let e=0;e<5&&!i.dataset.index;e++){if(!i.parentElement)return;i=i.parentElement}if(!i.dataset.index)return;const s=parseInt(i.dataset.index,10),n=this.columns[s];if(!n.sortable)return;const o=t.findIndex(e=>e.column===n);if(0===o){const e=t[0];e.direction=e.direction===r.Asc?r.Desc:r.Asc}else-1!==o&&t.splice(o,1),t.unshift({column:n,direction:r.Asc});u(t)}}),i.append(s),this.tbody_element=Object(n.u)(),this.element.append(i,this.tbody_element),c&&(this.footer_row_element=Object(n.A)(),this.element.append(Object(n.x)({},this.footer_row_element)),this.create_footer()),this.disposables(Object(n.d)(this.tbody_element,this.values,this.create_row),this.values.observe(this.update_footer)),this.finalize_construction()}create_footer(){const e=[];let t=0;for(let i=0;i<this.columns.length;i++){const s=this.columns[i],o=Object(n.y)();o.style.width=`${s.width}px`,s.fixed&&(o.classList.add("fixed"),o.style.left=`${t}px`,t+=s.width||0),s.footer&&(o.textContent=s.footer.render_cell(),o.title=s.footer.tooltip?s.footer.tooltip():""),s.text_align&&(o.style.textAlign=s.text_align),e.push(o)}this.footer_row_element.append(...e)}}}}]);