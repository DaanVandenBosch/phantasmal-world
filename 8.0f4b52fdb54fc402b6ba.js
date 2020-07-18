(window.webpackJsonp=window.webpackJsonp||[]).push([[8],{490:function(e,t,i){"use strict";i.d(t,"a",(function(){return r}));var n=i(139),s=i(17),o=(i(491),i(32)),l=i(2);class r extends n.a{constructor(e,t,i,n,s){super(s),this.element=Object(l.s)({className:`${t} core_Input`}),this._value=new o.a(this,e,this.set_value),this.value=this._value,this.input_element=Object(l.m)({className:`${n} core_Input_inner`}),this.input_element.type=i,this.input_element.addEventListener("change",()=>{this._value.set_val(this.get_value(),{silent:!1})}),this.input_element.addEventListener("keydown",e=>{"Enter"===e.key&&this._value.set_val(this.get_value(),{silent:!1})}),s&&s.readonly&&this.set_attr("readOnly",!0),this.element.append(this.input_element)}set_enabled(e){super.set_enabled(e),this.input_element.disabled=!e}set_attr(e,t,i){if(null==t)return;const n=this.input_element,o=i||(e=>e);Object(s.a)(t)?(n[e]=o(t.val),this.disposable(t.observe(({value:t})=>n[e]=o(t)))):n[e]=o(t)}}},491:function(e,t,i){},501:function(e,t,i){"use strict";i.d(t,"a",(function(){return s}));var n=i(490);i(524);class s extends n.a{constructor(e=0,t){if(super(e,"core_NumberInput","number","core_NumberInput_inner",t),this.preferred_label_position="left",t){const{min:e,max:i,step:n,width:s}=t;this.set_attr("min",e,String),this.set_attr("max",i,String),this.input_element.step="any",this.set_attr("step",n,String),null!=s&&(this.element.style.width=`${s}px`)}t&&null!=t.round_to&&t.round_to>=0?this.rounding_factor=Math.pow(10,t.round_to):this.rounding_factor=1,this.set_value(e),this.finalize_construction(s)}get_value(){return parseFloat(this.input_element.value)}set_value(e){this.input_element.valueAsNumber=Math.round(this.rounding_factor*e)/this.rounding_factor}}},524:function(e,t,i){},633:function(e,t,i){"use strict";i.d(t,"a",(function(){return r})),i.d(t,"b",(function(){return a}));var n=i(42),s=i(2),o=i(15);i(634);const l=i(23).a.get("core/gui/Table");var r;!function(e){e[e.Asc=0]="Asc",e[e.Desc=1]="Desc"}(r||(r={}));class a extends n.a{constructor(e){super(e),this.tbody_element=Object(s.u)(),this.element=Object(s.t)({className:"core_Table"}),this.children=[],this.create_row=(e,t)=>{const i=new o.a;let n=0;return[Object(s.A)(...this.columns.map((o,r)=>{const a=o.fixed?Object(s.y)():Object(s.v)();try{const t=o.render_cell(e,i);a.append(t),o.input&&a.classList.add("input"),o.fixed&&(a.classList.add("fixed"),a.style.left=`${n}px`,n+=o.width||0),a.style.width=`${o.width}px`,o.text_align&&(a.style.textAlign=o.text_align),o.tooltip&&(a.title=o.tooltip(e))}catch(e){l.warn(`Error while rendering cell for index ${t}, column ${r}.`,e)}return a})),i]},this.update_footer=()=>{if(!this.footer_row_element)return;const e=this.columns.length;for(let t=0;t<e;t++){const e=this.columns[t];if(e.footer){const i=this.footer_row_element.children[t];i.textContent=e.footer.render_cell(),i.title=e.footer.tooltip?e.footer.tooltip():""}}},this.values=e.values,this.columns=e.columns;const t=[],i=Object(s.z)(),n=Object(s.A)();let c=0,u=!1;n.append(...this.columns.map((e,t)=>{const i=Object(s.y)({data:{index:t.toString()}},Object(s.s)(e.title));return e.fixed&&(i.style.position="sticky",i.style.left=`${c}px`,c+=e.width),i.style.width=`${e.width}px`,e.footer&&(u=!0),i}));const d=e.sort;d&&(n.onmousedown=e=>{if(e.target instanceof HTMLElement){let i=e.target;for(let e=0;e<5&&!i.dataset.index;e++){if(!i.parentElement)return;i=i.parentElement}if(!i.dataset.index)return;const n=parseInt(i.dataset.index,10),s=this.columns[n];if(!s.sortable)return;const o=t.findIndex(e=>e.column===s);if(0===o){const e=t[0];e.direction=e.direction===r.Asc?r.Desc:r.Asc}else-1!==o&&t.splice(o,1),t.unshift({column:s,direction:r.Asc});d(t)}}),i.append(n),this.tbody_element=Object(s.u)(),this.element.append(i,this.tbody_element),u&&(this.footer_row_element=Object(s.A)(),this.element.append(Object(s.x)({},this.footer_row_element)),this.create_footer()),this.disposables(Object(s.d)(this.tbody_element,this.values,this.create_row),this.values.observe(this.update_footer)),this.finalize_construction(a)}create_footer(){const e=[];let t=0;for(let i=0;i<this.columns.length;i++){const n=this.columns[i],o=Object(s.y)();o.style.width=`${n.width}px`,n.fixed&&(o.classList.add("fixed"),o.style.left=`${t}px`,t+=n.width||0),n.footer&&(o.textContent=n.footer.render_cell(),o.title=n.footer.tooltip?n.footer.tooltip():""),n.text_align&&(o.style.textAlign=n.text_align),e.push(o)}this.footer_row_element.append(...e)}}},634:function(e,t,i){},811:function(e,t,i){},812:function(e,t,i){},813:function(e,t,i){},814:function(e,t,i){},897:function(e,t,i){"use strict";i.r(t),i.d(t,"OptimizerView",(function(){return z}));var n=i(2),s=(i(811),i(138)),o=i(15),l=i(501),r=i(139),a=(i(812),i(491),i(145)),c=i(32);class u extends r.a{constructor(e){super(e),this.element=Object(n.s)({className:"core_ComboBox core_Input"}),this.preferred_label_position="left",this.input_element=Object(n.m)(),this.to_label=e.to_label,this._selected=new c.a(this,void 0,this.set_selected),this.selected=this._selected,this.menu=this.disposable(new a.a({items:e.items,to_label:e.to_label,related_element:this.element})),this.menu.element.onmousedown=e=>e.preventDefault(),this.input_element.placeholder=e.placeholder_text||"",this.input_element.onmousedown=()=>{this.menu.visible.set_val(!0,{silent:!1})},this.input_element.onkeydown=e=>{switch(e.key){case"ArrowDown":e.preventDefault(),this.menu.hover_next();break;case"ArrowUp":e.preventDefault(),this.menu.hover_prev();break;case"Enter":this.menu.select_hovered()}};const t=e.filter;if(t){let e="";this.input_element.onkeyup=()=>{this.input_element.value!==e&&(e=this.input_element.value,t(e),(this.menu.visible.val||e)&&this.menu.hover_next())}}this.input_element.onblur=()=>{this.menu.visible.set_val(!1,{silent:!1})};const i=Object(n.k)(n.a.TriangleDown),s=Object(n.k)(n.a.TriangleUp),o=Object(n.s)({className:"core_ComboBox_button"},i,s);o.onmousedown=e=>{e.preventDefault(),this.menu.visible.set_val(!this.menu.visible.val,{silent:!1})},this.element.append(Object(n.s)({className:"core_ComboBox_inner core_Input_inner"},this.input_element,o),this.menu.element),this.disposables(this.menu.visible.observe(({value:e})=>{e&&this.menu.hover_next()}),this.menu.selected.observe(({value:e})=>{this.selected.set_val(e,{silent:!1}),this.input_element.focus()}),Object(n.c)(s,"hidden",this.menu.visible.map(e=>!e)),Object(n.c)(i,"hidden",this.menu.visible)),this.finalize_construction(u)}set_enabled(e){super.set_enabled(e),this.input_element.disabled=!e,this.menu.enabled.val=e}set_selected(e){this.input_element.value=e?this.to_label(e):"",this.menu.selected.val=e}}var d=i(14),h=i(23),_=i(83),m=function(e,t,i,n){return new(i||(i=Promise))((function(s,o){function l(e){try{a(n.next(e))}catch(e){o(e)}}function r(e){try{a(n.throw(e))}catch(e){o(e)}}function a(e){var t;e.done?s(e.value):(t=e.value,t instanceof i?t:new i((function(e){e(t)}))).then(l,r)}a((n=n.apply(e,t||[])).next())}))};const p=h.a.get("hunt_optimizer/gui/WantedItemsView");class b extends _.a{constructor(e){super(),this.hunt_optimizer_stores=e,this.tbody_element=Object(n.u)(),this.store_disposer=this.disposable(new o.a),this.element=Object(n.h)({className:"hunt_optimizer_WantedItemsView"}),this.create_row=e=>{const t=new o.a,i=t.add(new l.a(e.amount.val,{min:0,step:1}));t.add_all(i.value.bind_to(e.amount),i.value.observe(({value:t})=>e.set_amount(t)));const r=t.add(new s.a({icon_left:n.a.Remove}));return t.add(r.onclick.observe(()=>m(this,void 0,void 0,(function*(){return(yield this.hunt_optimizer_stores.current.val).remove_wanted_item(e)})))),[Object(n.A)(Object(n.v)(i.element),Object(n.v)(e.item_type.name),Object(n.v)(r.element)),t]};const t=Object(d.c)(),i=Object(d.c)(),r=this.add(new u({items:i,to_label:e=>e.name,placeholder_text:"Add an item",filter(e){const n=e.toLowerCase();i.val=t.val.filter(e=>e.name.toLowerCase().includes(n))}}));this.element.append(Object(n.j)("Wanted Items"),r.element,Object(n.h)({className:"hunt_optimizer_WantedItemsView_table_wrapper"},Object(n.t)(this.tbody_element))),this.disposables(e.current.observe(({value:e})=>m(this,void 0,void 0,(function*(){try{const s=yield e;this.store_disposer.dispose_all(),this.store_disposer.add_all(Object(n.d)(this.tbody_element,s.wanted_items,this.create_row),r.selected.observe(({value:e})=>{e&&(s.add_wanted_item(e),r.selected.val=void 0)})),t.val=s.huntable_item_types.slice().sort((e,t)=>e.name.localeCompare(t.name)),i.val=t.val}catch(e){p.error("Couldn't load hunt optimizer store.",e)}})),{call_now:!0})),this.finalize_construction(b)}}i(813);var f=i(633),v=i(31),w=i(137),x=(i(814),i(523)),O=function(e,t,i,n){return new(i||(i=Promise))((function(s,o){function l(e){try{a(n.next(e))}catch(e){o(e)}}function r(e){try{a(n.throw(e))}catch(e){o(e)}}function a(e){var t;e.done?s(e.value):(t=e.value,t instanceof i?t:new i((function(e){e(t)}))).then(l,r)}a((n=n.apply(e,t||[])).next())}))};const y=h.a.get("hunt_optimizer/gui/OptimizationResultView");class g extends _.a{constructor(e){super(),this.element=Object(n.h)({className:"hunt_optimizer_OptimizationResultView"},Object(n.j)("Ideal Combination of Methods")),this.disposable(e.current.observe(({value:e})=>O(this,void 0,void 0,(function*(){try{const t=yield e;if(this.disposed)return;this.results_observer&&this.remove_disposable(this.results_observer),this.results_observer=this.disposable(t.result.observe(({value:e})=>this.update_table(e),{call_now:!0}))}catch(e){y.error("Couldn't load hunt optimizer store.",e)}})),{call_now:!0})),this.finalize_construction(g)}update_table(e){this.table&&this.remove(this.table);let t=0,i=x.Duration.fromMillis(0);if(e)for(const n of e.optimal_methods)t+=n.runs,i=i.plus(n.total_time);const s=[{title:"Difficulty",fixed:!0,width:80,render_cell:e=>v.b[e.difficulty],footer:{render_cell:()=>"Totals:"}},{title:"Method",fixed:!0,width:250,render_cell:e=>e.method_name},{title:"Ep.",fixed:!0,width:40,render_cell:e=>w.b[e.method_episode]},{title:"Section ID",fixed:!0,width:90,render_cell(e){const t=Object(n.s)(...e.section_ids.map(e=>Object(n.r)(e,{size:17})));return t.style.display="flex",t}},{title:"Time/Run",width:90,text_align:"center",render_cell:e=>e.method_time.toFormat("hh:mm")},{title:"Runs",width:60,text_align:"right",render_cell:e=>e.runs.toFixed(1),tooltip:e=>e.runs.toString(),footer:{render_cell:()=>t.toFixed(1),tooltip:()=>t.toString()}},{title:"Total Hours",width:60,text_align:"right",render_cell:e=>e.total_time.as("hours").toFixed(1),tooltip:e=>e.total_time.as("hours").toString(),footer:{render_cell:()=>i.as("hours").toFixed(1),tooltip:()=>i.as("hours").toString()}}];if(e)for(const t of e.wanted_items){let i=0;for(const n of e.optimal_methods)i+=n.item_counts.get(t)||0;s.push({title:t.name,width:80,text_align:"right",render_cell(e){const i=e.item_counts.get(t);return i?i.toFixed(2):""},tooltip(e){const i=e.item_counts.get(t);return i?i.toString():""},footer:{render_cell:()=>i.toFixed(2),tooltip:()=>i.toString()}})}this.table=this.add(new f.b({class:"hunt_optimizer_OptimizationResultView_table",values:e?Object(d.c)(void 0,...e.optimal_methods):Object(d.c)(),columns:s})),this.element.append(this.table.element)}}var j=i(82);class z extends j.a{constructor(e){super(),this.element=Object(n.h)({className:"hunt_optimizer_OptimizerView"}),this.element.append(this.add(new b(e)).element,this.add(new g(e)).element),this.finalize_construction(z)}}}}]);