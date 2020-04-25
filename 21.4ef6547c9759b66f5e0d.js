(window.webpackJsonp=window.webpackJsonp||[]).push([[21],{555:function(t,e,i){"use strict";i.d(e,"a",(function(){return a}));var s=i(146),n=i(142),o=(i(593),i(2));const r=28;class a extends n.a{constructor(t,e){super(e),this.gui_store=t,this.tabs=[],this.bar_element=Object(o.h)({className:"core_TabContainer_Bar"}),this.panes_element=Object(o.h)({className:"core_TabContainer_Panes"}),this.element=Object(o.h)({className:"core_TabContainer"}),this.bar_mousedown=t=>{if(t.target instanceof HTMLElement){const e=t.target.dataset.key;e&&this.activate_key(e)}},this.bar_element.onmousedown=this.bar_mousedown;for(const t of e.tabs){const e=Object(o.s)({className:"core_TabContainer_Tab",data:{key:t.key}},t.title);this.bar_element.append(e);const i=this.disposable(new s.a(t.create_view)),n=Object.assign(Object.assign({},t),{tab_element:e,lazy_view:i});this.tabs.push(n),this.panes_element.append(i.element)}this.element.append(this.bar_element,this.panes_element),this.finalize_construction()}get children(){return this.tabs.flatMap(t=>t.lazy_view.children)}resize(t,e){super.resize(t,e),this.bar_element.style.width=`${t}px`,this.bar_element.style.height=`${r}px`;const i=e-r;this.panes_element.style.width=`${t}px`,this.panes_element.style.height=`${i}px`;for(const e of this.tabs)e.lazy_view.resize(t,i);return this}activate(){if(this.active_tab)this.activate_tab(this.active_tab);else{let t;for(const e of this.tabs)null!=e.path&&this.gui_store.path.val.startsWith(e.path)&&(t=e);t?this.activate_tab(t):this.tabs.length&&this.activate_tab(this.tabs[0])}}activate_key(t){for(const e of this.tabs)if(e.key===t){this.activate_tab(e);break}}activate_tab(t){this.active_tab!==t&&(this.active_tab&&(this.active_tab.tab_element.classList.remove("active"),this.active_tab.lazy_view.visible.val=!1,this.active_tab.lazy_view.deactivate()),this.active_tab=t,t.tab_element.classList.add("active"),t.lazy_view.visible.val=!0),null!=t.path&&(this.gui_store.set_path_prefix(t.path),t.lazy_view.activate())}}},586:function(t,e,i){"use strict";i.d(e,"a",(function(){return a}));var s=i(31),n=i(23),o=function(t,e,i,s){return new(i||(i=Promise))((function(n,o){function r(t){try{c(s.next(t))}catch(t){o(t)}}function a(t){try{c(s.throw(t))}catch(t){o(t)}}function c(t){var e;t.done?n(t.value):(e=t.value,e instanceof i?e:new i((function(t){t(e)}))).then(r,a)}c((s=s.apply(t,e||[])).next())}))};const r=n.a.get("core/persistence/Persister");class a{persist_for_server(t,e,i){this.persist(this.server_key(t,e),i)}persist(t,e){try{localStorage.setItem(t,JSON.stringify(e))}catch(e){r.error(`Couldn't persist ${t}.`,e)}}load_for_server(t,e){return o(this,void 0,void 0,(function*(){return this.load(this.server_key(t,e))}))}load(t){return o(this,void 0,void 0,(function*(){try{const e=localStorage.getItem(t);return e&&JSON.parse(e)}catch(e){return void r.error(`Couldn't load ${t}.`,e)}}))}server_key(t,e){let i=e+".";switch(t){case s.g.Ephinea:i+="Ephinea";break;default:throw new Error(`Server ${s.g[t]} not supported.`)}return i}}},593:function(t,e,i){},902:function(t,e,i){"use strict";i.r(e),i.d(e,"initialize_hunt_optimizer",(function(){return A}));var s=i(555),n=i(82),o=function(t,e,i,s){return new(i||(i=Promise))((function(n,o){function r(t){try{c(s.next(t))}catch(t){o(t)}}function a(t){try{c(s.throw(t))}catch(t){o(t)}}function c(t){var e;t.done?n(t.value):(e=t.value,e instanceof i?e:new i((function(t){t(e)}))).then(r,a)}c((s=s.apply(t,e||[])).next())}))};class r extends n.a{constructor(t,e,n){super(),this.tab_container=this.add(new s.a(t,{class:"hunt_optimizer_HuntOptimizerView",tabs:[{title:"Optimize",key:"optimize",path:"/optimize",create_view:()=>o(this,void 0,void 0,(function*(){return new((yield i.e(10).then(i.bind(null,904))).OptimizerView)(e)}))},{title:"Methods",key:"methods",path:"/methods",create_view:()=>o(this,void 0,void 0,(function*(){return new((yield i.e(14).then(i.bind(null,907))).MethodsView)(t,n)}))},{title:"Help",key:"help",path:"/help",create_view:()=>o(this,void 0,void 0,(function*(){return new((yield i.e(22).then(i.bind(null,900))).HelpView)}))}]})),this.finalize_construction()}get element(){return this.tab_container.element}resize(t,e){super.resize(t,e),this.tab_container.resize(t,e)}}var a=i(31),c=i(140);class h{constructor(t,e,i,s){if(this.id=t,this.name=e,this.episode=i,this.enemy_counts=s,!t)throw new Error("id is required.");if(!e)throw new Error("name is required.");if(!s)throw new Error("enemyCounts is required.")}}var u=i(14),_=i(531);class d{constructor(t,e,i,s){if(!t)throw new Error("id is required.");if(!_.Duration.isDuration(s))throw new Error("default_time must a valid duration.");if(!e)throw new Error("name is required.");if(!i)throw new Error("quest is required.");this.id=t,this.name=e,this.episode=i.episode,this.quest=i,this.enemy_counts=i.enemy_counts,this.default_time=s,this._user_time=Object(u.e)(void 0),this.user_time=this._user_time,this.time=this.user_time.map(t=>null!=t?t:this.default_time)}set_user_time(t){return this._user_time.val=t,this}}var l=i(59),m=i(104),f=i(23),p=function(t,e,i,s){return new(i||(i=Promise))((function(n,o){function r(t){try{c(s.next(t))}catch(t){o(t)}}function a(t){try{c(s.throw(t))}catch(t){o(t)}}function c(t){var e;t.done?n(t.value):(e=t.value,e instanceof i?e:new i((function(t){t(e)}))).then(r,a)}c((s=s.apply(t,e||[])).next())}))};const v=f.a.get("hunt_optimizer/stores/HuntMethodStore"),w=_.Duration.fromObject({minutes:30}),y=_.Duration.fromObject({minutes:45}),b=_.Duration.fromObject({minutes:45});function g(t,e,i){return new m.a(e,function(t,e){return i=>p(this,void 0,void 0,(function*(){const s=yield t.get(`/quests.${a.g[i].toLowerCase()}.json`).json(),n=[];for(const t of s){let e=0;const i=new Map;for(const[s,n]of Object.entries(t.enemy_counts)){const t=c.c[s];t?(i.set(t,n),e+=n):v.error(`No NpcType found for code ${s}.`)}switch(t.id){case 31:case 34:case 1305:case 1306:case 1307:case 313:case 314:continue}n.push(new d(`q${t.id}`,t.name,new h(t.id,t.name,t.episode,i),/^\d-\d.*/.test(t.name)?y:e>400?b:w))}return yield e.load_method_user_times(n,i),new z(e,i,n)}))}(t,i))}class z extends l.a{constructor(t,e,i){super(),this.methods=Object(u.c)(t=>[t.user_time],...i),this.disposables(this.methods.observe_list(()=>t.persist_method_user_times(this.methods.val,e)))}}var O=i(797),x=i.n(O);class j{constructor(t,e){this.item_type=t,this._amount=Object(u.e)(e),this.amount=this._amount}set_amount(t){return this._amount.val=t,this}}class k{constructor(t,e){this.wanted_items=t,this.optimal_methods=e}}class M{constructor(t,e,i,s,n,o,r){this.difficulty=t,this.section_ids=e,this.method_name=i,this.method_episode=s,this.method_time=n,this.runs=o,this.item_counts=r,this.total_time=_.Duration.fromMillis(o*n.as("milliseconds"))}}var P=function(t,e,i,s){return new(i||(i=Promise))((function(n,o){function r(t){try{c(s.next(t))}catch(t){o(t)}}function a(t){try{c(s.throw(t))}catch(t){o(t)}}function c(t){var e;t.done?n(t.value):(e=t.value,e instanceof i?e:new i((function(t){t(e)}))).then(r,a)}c((s=s.apply(t,e||[])).next())}))};function $(t,e,i,s,n){return new m.a(t,function(t,e,i,s){return n=>P(this,void 0,void 0,(function*(){return new E(t,n,yield e.get(n),yield i.get(n),yield s.get(n))}))}(e,i,s,n))}class E extends l.a{constructor(t,e,i,s,n){super(),this.hunt_optimizer_persister=t,this.server=e,this.item_drop_store=s,this._wanted_items=Object(u.c)(t=>[t.amount]),this.wanted_items=this._wanted_items,this.optimize=(t,e)=>{if(!t.length)return;const i=new Set(t.filter(t=>t.amount.val>0).map(t=>t.item_type)),s=this.item_drop_store.enemy_drops,n={};for(const t of this.wanted_items.val)n[t.item_type.name]={min:t.amount.val};const o={},r=new Map;for(const t of e){const e=new Map;for(const[i,s]of t.enemy_counts.entries()){const t=e.get(i)||0,n=Object(c.d)(i);if(null==n.rare_type)e.set(i,t+s);else{let o,r;n.rare_type===c.c.Kondrieu?(o=1-a.c,r=a.c):(o=1-a.d,r=a.d),e.set(i,t+s*o),e.set(n.rare_type,(e.get(n.rare_type)||0)+s*r)}}const n=[e],h=e.get(c.c.PanArms);if(h){const t=new Map(e);t.delete(c.c.PanArms),t.set(c.c.Migium,h),t.set(c.c.Hidoom,h),n.push(t)}const u=e.get(c.c.PanArms2);if(u){const t=new Map(e);t.delete(c.c.PanArms2),t.set(c.c.Migium2,u),t.set(c.c.Hidoom2,u),n.push(t)}for(let e=0;e<n.length;e++){const c=n[e],h=1===e;for(const e of a.a)for(const n of a.f){const a={time:t.time.val.as("hours")};let u=!1;for(const[t,o]of c.entries()){const r=s.get_drop(e,n,t);if(r&&i.has(r.item_type)){const t=a[r.item_type.name]||0;a[r.item_type.name]=t+o*r.rate,u=!0}}if(u){const i=this.full_method_name(e,n,t,h);o[i]=a,r.set(i,{method:t,difficulty:e,section_id:n,split_pan_arms:h})}}}}const h=x.a.Solve({optimize:"time",opType:"min",constraints:n,variables:o});if(!h.feasible)return;const u=[];for(const[t,e]of Object.entries(h)){const s=r.get(t);if(s){const{method:n,difficulty:r,section_id:c,split_pan_arms:h}=s,_=e,d=o[t],l=new Map;for(const[t,e]of Object.entries(d))for(const s of i)if(t===s.name){l.set(s,_*e);break}const m=[];for(const t of a.f){let e=!0;if(t!==c){const i=o[this.full_method_name(r,t,n,h)];if(i){for(const t of Object.keys(d))if(d[t]!==i[t]){e=!1;break}}else e=!1}e&&m.push(t)}u.push(new M(r,m,n.name+(h?" (Split Pan Arms)":""),n.episode,n.time.val,_,l))}}return new k([...i],u)},this.initialize_persistence=()=>P(this,void 0,void 0,(function*(){this._wanted_items.val=yield this.hunt_optimizer_persister.load_wanted_items(this.server),this.disposable(this._wanted_items.observe(({value:t})=>{this.hunt_optimizer_persister.persist_wanted_items(this.server,t)}))})),this.huntable_item_types=i.item_types.filter(t=>s.enemy_drops.get_drops_for_item_type(t.id).length),this.result=Object(u.d)(this.optimize,this.wanted_items,n.methods),this.initialize_persistence()}add_wanted_item(t){this._wanted_items.val.find(e=>e.item_type===t)||this._wanted_items.push(new j(t,1))}remove_wanted_item(t){this._wanted_items.remove(t)}full_method_name(t,e,i,s){let n=`${t}\t${e}\t${i.id}`;return s&&(n+="\tspa"),n}}var S=i(586),T=function(t,e,i,s){return new(i||(i=Promise))((function(n,o){function r(t){try{c(s.next(t))}catch(t){o(t)}}function a(t){try{c(s.throw(t))}catch(t){o(t)}}function c(t){var e;t.done?n(t.value):(e=t.value,e instanceof i?e:new i((function(t){t(e)}))).then(r,a)}c((s=s.apply(t,e||[])).next())}))};const q="HuntMethodStore.methodUserTimes";class H extends S.a{persist_method_user_times(t,e){const i={};for(const e of t)null!=e.user_time.val&&(i[e.id]=e.user_time.val.as("hours"));this.persist_for_server(e,q,i)}load_method_user_times(t,e){return T(this,void 0,void 0,(function*(){const i=yield this.load_for_server(e,q);if(i)for(const e of t){const t=i[e.id];e.set_user_time(null==t?void 0:_.Duration.fromObject({hours:t}))}}))}}var C=function(t,e,i,s){return new(i||(i=Promise))((function(n,o){function r(t){try{c(s.next(t))}catch(t){o(t)}}function a(t){try{c(s.throw(t))}catch(t){o(t)}}function c(t){var e;t.done?n(t.value):(e=t.value,e instanceof i?e:new i((function(t){t(e)}))).then(r,a)}c((s=s.apply(t,e||[])).next())}))};const N="HuntOptimizerStore.wantedItems";class D extends S.a{constructor(t){super(),this.item_type_stores=t}persist_wanted_items(t,e){this.persist_for_server(t,N,e.map(({item_type:t,amount:e})=>({itemTypeId:t.id,amount:e.val})))}load_wanted_items(t){return C(this,void 0,void 0,(function*(){const e=yield this.item_type_stores.get(t),i=yield this.load_for_server(t,N),s=[];if(i)for(const{itemTypeId:t,itemKindId:n,amount:o}of i){const i=null!=t?e.get_by_id(t):e.get_by_id(n);i&&s.push(new j(i,o))}return s}))}}var I=i(15);function A(t,e,i,s){const n=new I.a,o=n.add(g(t,e,new H)),a=n.add($(e,new D(i),i,s,o));return{view:n.add(new r(e,a,o)),dispose(){n.dispose()}}}}}]);