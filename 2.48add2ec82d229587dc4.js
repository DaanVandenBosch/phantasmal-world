(window.webpackJsonp=window.webpackJsonp||[]).push([[2],{589:function(t,e,i){"use strict";
/*!
 * camera-controls
 * https://github.com/yomotsu/camera-controls
 * (c) 2017 @yomotsu
 * Released under the MIT License.
 */
/*! *****************************************************************************
Copyright (c) Microsoft Corporation. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION ANY IMPLIED
WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
MERCHANTABLITY OR NON-INFRINGEMENT.

See the Apache Version 2.0 License for specific language governing permissions
and limitations under the License.
***************************************************************************** */var o,a=function(t,e){return(a=Object.setPrototypeOf||{__proto__:[]}instanceof Array&&function(t,e){t.__proto__=e}||function(t,e){for(var i in e)e.hasOwnProperty(i)&&(t[i]=e[i])})(t,e)};!function(t){t[t.NONE=0]="NONE",t[t.ROTATE=1]="ROTATE",t[t.TRUCK=2]="TRUCK",t[t.DOLLY=3]="DOLLY",t[t.ZOOM=4]="ZOOM",t[t.TOUCH_ROTATE=5]="TOUCH_ROTATE",t[t.TOUCH_TRUCK=6]="TOUCH_TRUCK",t[t.TOUCH_DOLLY=7]="TOUCH_DOLLY",t[t.TOUCH_ZOOM=8]="TOUCH_ZOOM",t[t.TOUCH_DOLLY_TRUCK=9]="TOUCH_DOLLY_TRUCK",t[t.TOUCH_ZOOM_TRUCK=10]="TOUCH_ZOOM_TRUCK"}(o||(o={}));var r=2*Math.PI,n=Math.PI/2,s=1e-5;function h(t){return Math.abs(t)<s}function c(t,e){return Math.round(t/e)*e}function p(t){return isFinite(t)?t:t<0?-Number.MAX_VALUE:Number.MAX_VALUE}function l(t){return Math.abs(t)<Number.MAX_VALUE?t:t*(1/0)}function d(t){return"TouchEvent"in window&&t instanceof TouchEvent}function u(t,e){if(e.set(0,0),d(t)){for(var i=t,o=0;o<i.touches.length;o++)e.x+=i.touches[o].clientX,e.y+=i.touches[o].clientY;return e.x/=i.touches.length,e.y/=i.touches.length,e}var a=t;return e.set(a.clientX,a.clientY),e}function _(t,e){return!t.isPerspectiveCamera&&(console.warn(e+" is not supported in OrthographicCamera"),!0)}var m,y,v,g,f,E,T,O,C,x,A,w,U,b,z,L,S,D,M=function(){function t(){this._listeners={}}return t.prototype.addEventListener=function(t,e){var i=this._listeners;void 0===i[t]&&(i[t]=[]),-1===i[t].indexOf(e)&&i[t].push(e)},t.prototype.removeEventListener=function(t,e){var i=this._listeners[t];if(void 0!==i){var o=i.indexOf(e);-1!==o&&i.splice(o,1)}},t.prototype.removeAllEventListeners=function(t){t?Array.isArray(this._listeners[t])&&(this._listeners[t].length=0):this._listeners={}},t.prototype.dispatchEvent=function(t){var e=this._listeners[t.type];if(void 0!==e){t.target=this;for(var i=e.slice(0),o=0,a=i.length;o<a;o++)i[o].call(this,t)}},t}(),P=/Mac/.test(navigator.platform),R=Object.freeze(o),F=1/8,V=function(t){function e(e,i){var a=t.call(this)||this;if(a.enabled=!0,a.minPolarAngle=0,a.maxPolarAngle=Math.PI,a.minAzimuthAngle=-1/0,a.maxAzimuthAngle=1/0,a.minDistance=0,a.maxDistance=1/0,a.minZoom=.01,a.maxZoom=1/0,a.dampingFactor=.05,a.draggingDampingFactor=.25,a.azimuthRotateSpeed=1,a.polarRotateSpeed=1,a.dollySpeed=1,a.truckSpeed=2,a.dollyToCursor=!1,a.verticalDragToForward=!1,a.boundaryFriction=0,a.colliderMeshes=[],a._state=o.NONE,a._viewport=null,a._dollyControlAmount=0,a._boundaryEnclosesCamera=!1,a._needsUpdate=!0,a._updatedLastTime=!1,a._camera=e,a._yAxisUpSpace=(new m.Quaternion).setFromUnitVectors(a._camera.up,v),a._yAxisUpSpaceInverse=a._yAxisUpSpace.clone().inverse(),a._state=o.NONE,a._domElement=i,a._target=new m.Vector3,a._targetEnd=a._target.clone(),a._spherical=(new m.Spherical).setFromVector3(E.copy(a._camera.position).applyQuaternion(a._yAxisUpSpace)),a._sphericalEnd=a._spherical.clone(),a._zoom=a._camera.zoom,a._zoomEnd=a._zoom,a._nearPlaneCorners=[new m.Vector3,new m.Vector3,new m.Vector3,new m.Vector3],a._updateNearPlaneCorners(),a._boundary=new m.Box3(new m.Vector3(-1/0,-1/0,-1/0),new m.Vector3(1/0,1/0,1/0)),a._target0=a._target.clone(),a._position0=a._camera.position.clone(),a._zoom0=a._zoom,a._dollyControlAmount=0,a._dollyControlCoord=new m.Vector2,a.mouseButtons={left:o.ROTATE,middle:o.DOLLY,right:o.TRUCK,wheel:a._camera.isPerspectiveCamera?o.DOLLY:a._camera.isOrthographicCamera?o.ZOOM:o.NONE},a.touches={one:o.TOUCH_ROTATE,two:a._camera.isPerspectiveCamera?o.TOUCH_DOLLY_TRUCK:a._camera.isOrthographicCamera?o.TOUCH_ZOOM_TRUCK:o.NONE,three:o.TOUCH_TRUCK},a._domElement){var n=new m.Vector2,s=new m.Vector2,h=new m.Vector2,c=new m.Vector4,p=function(t,e){if(a._camera.isPerspectiveCamera){var i=a._camera,o=E.copy(i.position).sub(a._target),r=i.getEffectiveFOV()*m.Math.DEG2RAD,n=o.length()*Math.tan(.5*r),s=a.truckSpeed*t*n/c.w,h=a.truckSpeed*e*n/c.w;a.verticalDragToForward?(a.truck(s,0,!0),a.forward(-h,!0)):a.truck(s,h,!0)}else if(a._camera.isOrthographicCamera){var p=a._camera;s=t*(p.right-p.left)/p.zoom/c.z,h=e*(p.top-p.bottom)/p.zoom/c.w;a.truck(s,h,!0)}},l=function(t,e){var i=r*a.azimuthRotateSpeed*t/c.w,o=r*a.polarRotateSpeed*e/c.w;a.rotate(i,o,!0)},_=function(t,e,i){var o=Math.pow(.95,-t*a.dollySpeed),r=a._sphericalEnd.radius*o,n=a._sphericalEnd.radius;a.dollyTo(r),a.dollyToCursor&&(a._dollyControlAmount+=a._sphericalEnd.radius-n,a._dollyControlCoord.set(e,i))},y=function(t){var e=Math.pow(.95,t*a.dollySpeed);a.zoomTo(a._zoom*e)},g=function(t){if(a.enabled){t.preventDefault();var e=a._state;switch(t.button){case m.MOUSE.LEFT:a._state=a.mouseButtons.left;break;case m.MOUSE.MIDDLE:a._state=a.mouseButtons.middle;break;case m.MOUSE.RIGHT:a._state=a.mouseButtons.right}e!==a._state&&A(t)}},T=function(t){if(a.enabled){t.preventDefault();var e=a._state;switch(t.touches.length){case 1:a._state=a.touches.one;break;case 2:a._state=a.touches.two;break;case 3:a._state=a.touches.three}e!==a._state&&A(t)}},O=-1,C=function(t){if(a.enabled&&a.mouseButtons.wheel!==o.NONE){if(t.preventDefault(),a.dollyToCursor||a.mouseButtons.wheel===o.ROTATE||a.mouseButtons.wheel===o.TRUCK){var e=performance.now();O-e<1e3&&a._getClientRect(c),O=e}var i=P?-1:-3,r=1===t.deltaMode?t.deltaY/i:t.deltaY/(10*i),n=a.dollyToCursor?(t.clientX-c.x)/c.z*2-1:0,s=a.dollyToCursor?(t.clientY-c.y)/c.w*-2+1:0;switch(a.mouseButtons.wheel){case o.ROTATE:l(t.deltaX,t.deltaY);break;case o.TRUCK:p(t.deltaX,t.deltaY);break;case o.DOLLY:_(-r,n,s);break;case o.ZOOM:y(-r)}a.dispatchEvent({type:"control",originalEvent:t})}},x=function(t){a.enabled&&t.preventDefault()},A=function(t){if(a.enabled){if(t.preventDefault(),u(t,f),a._getClientRect(c),n.copy(f),s.copy(f),d(t)&&t.touches.length>=2){var e=t,i=f.x-e.touches[1].clientX,o=f.y-e.touches[1].clientY,r=Math.sqrt(i*i+o*o);h.set(0,r);var p=.5*(e.touches[0].clientX+e.touches[1].clientX),l=.5*(e.touches[0].clientY+e.touches[1].clientY);s.set(p,l)}document.addEventListener("mousemove",w),document.addEventListener("touchmove",w,{passive:!1}),document.addEventListener("mouseup",U),document.addEventListener("touchend",U),a.dispatchEvent({type:"controlstart",originalEvent:t})}},w=function(t){if(a.enabled){t.preventDefault(),u(t,f);var e=s.x-f.x,i=s.y-f.y;switch(s.copy(f),a._state){case o.ROTATE:case o.TOUCH_ROTATE:l(e,i);break;case o.DOLLY:case o.ZOOM:var r=a.dollyToCursor?(n.x-c.x)/c.z*2-1:0,d=a.dollyToCursor?(n.y-c.y)/c.w*-2+1:0;a._state===o.DOLLY?_(i*F,r,d):y(i*F);break;case o.TOUCH_DOLLY:case o.TOUCH_ZOOM:case o.TOUCH_DOLLY_TRUCK:case o.TOUCH_ZOOM_TRUCK:var m=t,v=f.x-m.touches[1].clientX,g=f.y-m.touches[1].clientY,E=Math.sqrt(v*v+g*g),T=h.y-E;h.set(0,E);r=a.dollyToCursor?(s.x-c.x)/c.z*2-1:0,d=a.dollyToCursor?(s.y-c.y)/c.w*-2+1:0;a._state===o.TOUCH_DOLLY||a._state===o.TOUCH_DOLLY_TRUCK?_(T*F,r,d):y(T*F),a._state!==o.TOUCH_DOLLY_TRUCK&&a._state!==o.TOUCH_ZOOM_TRUCK||p(e,i);break;case o.TRUCK:case o.TOUCH_TRUCK:p(e,i)}a.dispatchEvent({type:"control",originalEvent:t})}},U=function(t){a.enabled&&(a._state=o.NONE,document.removeEventListener("mousemove",w),document.removeEventListener("touchmove",w,{passive:!1}),document.removeEventListener("mouseup",U),document.removeEventListener("touchend",U),a.dispatchEvent({type:"controlend",originalEvent:t}))};a._domElement.addEventListener("mousedown",g),a._domElement.addEventListener("touchstart",T),a._domElement.addEventListener("wheel",C),a._domElement.addEventListener("contextmenu",x),a._removeAllEventListeners=function(){a._domElement.removeEventListener("mousedown",g),a._domElement.removeEventListener("touchstart",T),a._domElement.removeEventListener("wheel",C),a._domElement.removeEventListener("contextmenu",x),document.removeEventListener("mousemove",w),document.removeEventListener("touchmove",w,{passive:!1}),document.removeEventListener("mouseup",U),document.removeEventListener("touchend",U)}}return a.update(0),a}return function(t,e){function i(){this.constructor=t}a(t,e),t.prototype=null===e?Object.create(e):(i.prototype=e.prototype,new i)}(e,t),e.install=function(t){m=t.THREE,y=Object.freeze(new m.Vector3(0,0,0)),v=Object.freeze(new m.Vector3(0,1,0)),g=Object.freeze(new m.Vector3(0,0,1)),f=new m.Vector2,E=new m.Vector3,T=new m.Vector3,O=new m.Vector3,C=new m.Vector3,x=new m.Vector3,A=new m.Spherical,w=new m.Spherical,U=new m.Box3,b=new m.Box3,z=new m.Quaternion,L=new m.Quaternion,S=new m.Matrix4,D=new m.Raycaster},Object.defineProperty(e,"ACTION",{get:function(){return R},enumerable:!0,configurable:!0}),Object.defineProperty(e.prototype,"currentAction",{get:function(){return this._state},enumerable:!0,configurable:!0}),Object.defineProperty(e.prototype,"distance",{get:function(){return this._spherical.radius},set:function(t){this._spherical.radius===t&&this._sphericalEnd.radius===t||(this._spherical.radius=t,this._sphericalEnd.radius=t,this._needsUpdate=!0)},enumerable:!0,configurable:!0}),Object.defineProperty(e.prototype,"azimuthAngle",{get:function(){return this._spherical.theta},set:function(t){this._spherical.theta===t&&this._sphericalEnd.theta===t||(this._spherical.theta=t,this._sphericalEnd.theta=t,this._needsUpdate=!0)},enumerable:!0,configurable:!0}),Object.defineProperty(e.prototype,"polarAngle",{get:function(){return this._spherical.phi},set:function(t){this._spherical.phi===t&&this._sphericalEnd.phi===t||(this._spherical.phi=t,this._sphericalEnd.phi=t,this._needsUpdate=!0)},enumerable:!0,configurable:!0}),Object.defineProperty(e.prototype,"phiSpeed",{set:function(t){console.warn("phiSpeed was renamed. use azimuthRotateSpeed instead"),this.azimuthRotateSpeed=t},enumerable:!0,configurable:!0}),Object.defineProperty(e.prototype,"thetaSpeed",{set:function(t){console.warn("thetaSpeed was renamed. use polarRotateSpeed instead"),this.polarRotateSpeed=t},enumerable:!0,configurable:!0}),Object.defineProperty(e.prototype,"boundaryEnclosesCamera",{get:function(){return this._boundaryEnclosesCamera},set:function(t){this._boundaryEnclosesCamera=t,this._needsUpdate=!0},enumerable:!0,configurable:!0}),e.prototype.rotate=function(t,e,i){void 0===i&&(i=!1),this.rotateTo(this._sphericalEnd.theta+t,this._sphericalEnd.phi+e,i)},e.prototype.rotateTo=function(t,e,i){void 0===i&&(i=!1);var o=m.Math.clamp(t,this.minAzimuthAngle,this.maxAzimuthAngle),a=m.Math.clamp(e,this.minPolarAngle,this.maxPolarAngle);this._sphericalEnd.theta=o,this._sphericalEnd.phi=a,this._sphericalEnd.makeSafe(),i||(this._spherical.theta=this._sphericalEnd.theta,this._spherical.phi=this._sphericalEnd.phi),this._needsUpdate=!0},e.prototype.dolly=function(t,e){void 0===e&&(e=!1),this.dollyTo(this._sphericalEnd.radius-t,e)},e.prototype.dollyTo=function(t,e){void 0===e&&(e=!1),_(this._camera,"dolly")||(this._sphericalEnd.radius=m.Math.clamp(t,this.minDistance,this.maxDistance),e||(this._spherical.radius=this._sphericalEnd.radius),this._needsUpdate=!0)},e.prototype.zoom=function(t,e){void 0===e&&(e=!1),this.zoomTo(this._zoomEnd+t,e)},e.prototype.zoomTo=function(t,e){void 0===e&&(e=!1),this._zoomEnd=m.Math.clamp(t,this.minZoom,this.maxZoom),e||(this._zoom=this._zoomEnd),this._needsUpdate=!0},e.prototype.pan=function(t,e,i){void 0===i&&(i=!1),console.log("`pan` has been renamed to `truck`"),this.truck(t,e,i)},e.prototype.truck=function(t,e,i){void 0===i&&(i=!1),this._camera.updateMatrix(),C.setFromMatrixColumn(this._camera.matrix,0),x.setFromMatrixColumn(this._camera.matrix,1),C.multiplyScalar(t),x.multiplyScalar(-e);var o=E.copy(C).add(x);this._encloseToBoundary(this._targetEnd,o,this.boundaryFriction),i||this._target.copy(this._targetEnd),this._needsUpdate=!0},e.prototype.forward=function(t,e){void 0===e&&(e=!1),E.setFromMatrixColumn(this._camera.matrix,0),E.crossVectors(this._camera.up,E),E.multiplyScalar(t),this._encloseToBoundary(this._targetEnd,E,this.boundaryFriction),e||this._target.copy(this._targetEnd),this._needsUpdate=!0},e.prototype.moveTo=function(t,e,i,o){void 0===o&&(o=!1),this._targetEnd.set(t,e,i),o||this._target.copy(this._targetEnd),this._needsUpdate=!0},e.prototype.fitTo=function(t,e,i){var o=void 0===i?{}:i,a=o.paddingLeft,r=void 0===a?0:a,s=o.paddingRight,p=void 0===s?0:s,l=o.paddingBottom,d=void 0===l?0:l,u=o.paddingTop,_=void 0===u?0:u,m=t.isBox3?U.copy(t):U.setFromObject(t),y=c(this._sphericalEnd.theta,n),f=c(this._sphericalEnd.phi,n);this.rotateTo(y,f,e);var O=E.setFromSpherical(this._sphericalEnd).normalize(),C=z.setFromUnitVectors(O,g);h(Math.abs(O.y)-1)&&C.multiply(L.setFromAxisAngle(v,y));var x=b.makeEmpty();T.copy(m.min).applyQuaternion(C),x.expandByPoint(T),T.copy(m.min).setX(m.max.x).applyQuaternion(C),x.expandByPoint(T),T.copy(m.min).setY(m.max.y).applyQuaternion(C),x.expandByPoint(T),T.copy(m.max).setZ(m.min.z).applyQuaternion(C),x.expandByPoint(T),T.copy(m.min).setZ(m.max.z).applyQuaternion(C),x.expandByPoint(T),T.copy(m.max).setY(m.min.y).applyQuaternion(C),x.expandByPoint(T),T.copy(m.max).setX(m.min.x).applyQuaternion(C),x.expandByPoint(T),T.copy(m.max).applyQuaternion(C),x.expandByPoint(T),C.setFromUnitVectors(g,O),x.min.x-=r,x.min.y-=d,x.max.x+=p,x.max.y+=_;var A=x.getSize(E),w=x.getCenter(T).applyQuaternion(C),S=this._camera.isPerspectiveCamera,D=this._camera.isOrthographicCamera;if(S){var M=this.getDistanceToFit(A.x,A.y,A.z);return this.moveTo(w.x,w.y,w.z,e),void this.dollyTo(M,e)}if(D){var P=this._camera,R=P.right-P.left,F=P.top-P.bottom,V=Math.min(R/A.x,F/A.y);return this.moveTo(w.x,w.y,w.z,e),void this.zoomTo(V,e)}},e.prototype.setLookAt=function(t,e,i,o,a,r,n){void 0===n&&(n=!1);var s=E.set(t,e,i),h=T.set(o,a,r);this._targetEnd.copy(h),this._sphericalEnd.setFromVector3(s.sub(h).applyQuaternion(this._yAxisUpSpace)),this.normalizeRotations(),n||(this._target.copy(this._targetEnd),this._spherical.copy(this._sphericalEnd)),this._needsUpdate=!0},e.prototype.lerpLookAt=function(t,e,i,o,a,r,n,s,h,c,p,l,d,u){void 0===u&&(u=!1);var _=E.set(t,e,i),m=T.set(o,a,r);A.setFromVector3(_.sub(m).applyQuaternion(this._yAxisUpSpace));var y=E.set(c,p,l);this._targetEnd.copy(m).lerp(y,d);var v=T.set(n,s,h);w.setFromVector3(v.sub(y).applyQuaternion(this._yAxisUpSpace));var g=w.theta-A.theta,f=w.phi-A.phi,O=w.radius-A.radius;this._sphericalEnd.set(A.radius+O*d,A.phi+f*d,A.theta+g*d),this.normalizeRotations(),u||(this._target.copy(this._targetEnd),this._spherical.copy(this._sphericalEnd)),this._needsUpdate=!0},e.prototype.setPosition=function(t,e,i,o){void 0===o&&(o=!1),this.setLookAt(t,e,i,this._targetEnd.x,this._targetEnd.y,this._targetEnd.z,o)},e.prototype.setTarget=function(t,e,i,o){void 0===o&&(o=!1);var a=this.getPosition(E);this.setLookAt(a.x,a.y,a.z,t,e,i,o)},e.prototype.setBoundary=function(t){if(!t)return this._boundary.min.set(-1/0,-1/0,-1/0),this._boundary.max.set(1/0,1/0,1/0),void(this._needsUpdate=!0);this._boundary.copy(t),this._boundary.clampPoint(this._targetEnd,this._targetEnd),this._needsUpdate=!0},e.prototype.setViewport=function(t,e,i,o){null!==t?(this._viewport=this._viewport||new m.Vector4,"number"==typeof t?this._viewport.set(t,e,i,o):this._viewport.copy(t)):this._viewport=null},e.prototype.getDistanceToFit=function(t,e,i){if(_(this._camera,"getDistanceToFit"))return this._spherical.radius;var o=this._camera,a=t/e,r=o.getEffectiveFOV()*m.Math.DEG2RAD,n=o.aspect;return.5*(a<n?e:t/n)/Math.tan(.5*r)+.5*i},e.prototype.getTarget=function(t){return(t&&t.isVector3?t:new m.Vector3).copy(this._targetEnd)},e.prototype.getPosition=function(t){return(t&&t.isVector3?t:new m.Vector3).setFromSpherical(this._sphericalEnd).applyQuaternion(this._yAxisUpSpaceInverse).add(this._targetEnd)},e.prototype.normalizeRotations=function(){this._sphericalEnd.theta=this._sphericalEnd.theta%r,this._sphericalEnd.theta<0&&(this._sphericalEnd.theta+=r),this._spherical.theta+=r*Math.round((this._sphericalEnd.theta-this._spherical.theta)/r)},e.prototype.reset=function(t){void 0===t&&(t=!1),this.setLookAt(this._position0.x,this._position0.y,this._position0.z,this._target0.x,this._target0.y,this._target0.z,t),this.zoomTo(this._zoom0,t)},e.prototype.saveState=function(){this._target0.copy(this._target),this._position0.copy(this._camera.position),this._zoom0=this._zoom},e.prototype.updateCameraUp=function(){this._yAxisUpSpace.setFromUnitVectors(this._camera.up,v),this._yAxisUpSpaceInverse.copy(this._yAxisUpSpace).inverse()},e.prototype.update=function(t){var e=this._state===o.NONE?this.dampingFactor:this.draggingDampingFactor,i=1-Math.exp(-e*t*62.5),a=this._sphericalEnd.theta-this._spherical.theta,r=this._sphericalEnd.phi-this._spherical.phi,n=this._sphericalEnd.radius-this._spherical.radius,s=E.subVectors(this._targetEnd,this._target);if(h(a)&&h(r)&&h(n)&&h(s.x)&&h(s.y)&&h(s.z)?(this._spherical.copy(this._sphericalEnd),this._target.copy(this._targetEnd)):(this._spherical.set(this._spherical.radius+n*i,this._spherical.phi+r*i,this._spherical.theta+a*i),this._target.add(s.multiplyScalar(i)),this._needsUpdate=!0),0!==this._dollyControlAmount){if(this._camera.isPerspectiveCamera){var c=this._camera,p=E.setFromSpherical(this._sphericalEnd).applyQuaternion(this._yAxisUpSpaceInverse).normalize().negate(),l=T.copy(p).cross(c.up).normalize();0===l.lengthSq()&&(l.x=1);var d=O.crossVectors(l,p),u=this._sphericalEnd.radius*Math.tan(c.getEffectiveFOV()*m.Math.DEG2RAD*.5),_=(this._sphericalEnd.radius-this._dollyControlAmount-this._sphericalEnd.radius)/this._sphericalEnd.radius,y=E.copy(this._targetEnd).add(l.multiplyScalar(this._dollyControlCoord.x*u*c.aspect)).add(d.multiplyScalar(this._dollyControlCoord.y*u));this._targetEnd.lerp(y,_),this._target.copy(this._targetEnd)}this._dollyControlAmount=0}var v=this._collisionTest();this._spherical.radius=Math.min(this._spherical.radius,v),this._spherical.makeSafe(),this._camera.position.setFromSpherical(this._spherical).applyQuaternion(this._yAxisUpSpaceInverse).add(this._target),this._camera.lookAt(this._target),this._boundaryEnclosesCamera&&this._encloseToBoundary(this._camera.position.copy(this._target),E.setFromSpherical(this._spherical).applyQuaternion(this._yAxisUpSpaceInverse),1);var g=this._zoomEnd-this._zoom;this._zoom+=g*i,this._camera.zoom!==this._zoom&&(h(g)&&(this._zoom=this._zoomEnd),this._camera.zoom=this._zoom,this._camera.updateProjectionMatrix(),this._updateNearPlaneCorners(),this._needsUpdate=!0);var f=this._needsUpdate;return f&&!this._updatedLastTime?(this.dispatchEvent({type:"wake"}),this.dispatchEvent({type:"update"})):f?this.dispatchEvent({type:"update"}):!f&&this._updatedLastTime&&this.dispatchEvent({type:"sleep"}),this._updatedLastTime=f,this._needsUpdate=!1,f},e.prototype.toJSON=function(){return JSON.stringify({enabled:this.enabled,minDistance:this.minDistance,maxDistance:p(this.maxDistance),minZoom:this.minZoom,maxZoom:p(this.maxZoom),minPolarAngle:this.minPolarAngle,maxPolarAngle:p(this.maxPolarAngle),minAzimuthAngle:p(this.minAzimuthAngle),maxAzimuthAngle:p(this.maxAzimuthAngle),dampingFactor:this.dampingFactor,draggingDampingFactor:this.draggingDampingFactor,dollySpeed:this.dollySpeed,truckSpeed:this.truckSpeed,dollyToCursor:this.dollyToCursor,verticalDragToForward:this.verticalDragToForward,target:this._targetEnd.toArray(),position:this._camera.position.toArray(),zoom:this._camera.zoom,target0:this._target0.toArray(),position0:this._position0.toArray(),zoom0:this._zoom0})},e.prototype.fromJSON=function(t,e){void 0===e&&(e=!1);var i=JSON.parse(t),o=E.fromArray(i.position);this.enabled=i.enabled,this.minDistance=i.minDistance,this.maxDistance=l(i.maxDistance),this.minZoom=i.minZoom,this.maxZoom=l(i.maxZoom),this.minPolarAngle=i.minPolarAngle,this.maxPolarAngle=l(i.maxPolarAngle),this.minAzimuthAngle=l(i.minAzimuthAngle),this.maxAzimuthAngle=l(i.maxAzimuthAngle),this.dampingFactor=i.dampingFactor,this.draggingDampingFactor=i.draggingDampingFactor,this.dollySpeed=i.dollySpeed,this.truckSpeed=i.truckSpeed,this.dollyToCursor=i.dollyToCursor,this.verticalDragToForward=i.verticalDragToForward,this._target0.fromArray(i.target0),this._position0.fromArray(i.position0),this._zoom0=i.zoom0,this.moveTo(i.target[0],i.target[1],i.target[2],e),A.setFromVector3(o.sub(this._targetEnd).applyQuaternion(this._yAxisUpSpace)),this.rotateTo(A.theta,A.phi,e),this.zoomTo(i.zoom,e),this._needsUpdate=!0},e.prototype.dispose=function(){this._removeAllEventListeners()},e.prototype._encloseToBoundary=function(t,e,i){var o=e.lengthSq();if(0===o)return t;var a=T.copy(e).add(t),r=this._boundary.clampPoint(a,O).sub(a),n=r.lengthSq();if(0===n)return t.add(e);if(n===o)return t;if(0===i)return t.add(e).add(r);var s=1+i*n/e.dot(r);return t.add(T.copy(e).multiplyScalar(s)).add(r.multiplyScalar(1-i))},e.prototype._updateNearPlaneCorners=function(){if(this._camera.isPerspectiveCamera){var t=(a=this._camera).near,e=a.getEffectiveFOV()*m.Math.DEG2RAD,i=Math.tan(.5*e)*t,o=i*a.aspect;this._nearPlaneCorners[0].set(-o,-i,0),this._nearPlaneCorners[1].set(o,-i,0),this._nearPlaneCorners[2].set(o,i,0),this._nearPlaneCorners[3].set(-o,i,0)}else if(this._camera.isOrthographicCamera){var a,r=1/(a=this._camera).zoom,n=a.left*r,s=a.right*r,h=a.top*r,c=a.bottom*r;this._nearPlaneCorners[0].set(n,h,0),this._nearPlaneCorners[1].set(s,h,0),this._nearPlaneCorners[2].set(s,c,0),this._nearPlaneCorners[3].set(n,c,0)}},e.prototype._collisionTest=function(){var t=1/0;if(!(this.colliderMeshes.length>=1))return t;if(_(this._camera,"_collisionTest"))return t;t=this._spherical.radius;var e=E.setFromSpherical(this._spherical).divideScalar(t);S.lookAt(y,e,this._camera.up);for(var i=0;i<4;i++){var o=T.copy(this._nearPlaneCorners[i]);o.applyMatrix4(S);var a=O.addVectors(this._target,o);D.set(a,e),D.far=t;var r=D.intersectObjects(this.colliderMeshes);0!==r.length&&r[0].distance<t&&(t=r[0].distance)}return t},e.prototype._getClientRect=function(t){var e=this._domElement.getBoundingClientRect();return t.x=e.left,t.y=e.top,this._viewport?(t.x+=this._viewport.x,t.y+=e.height-this._viewport.w-this._viewport.y,t.z=this._viewport.z,t.w=this._viewport.w):(t.z=e.width,t.w=e.height),t},e.prototype._removeAllEventListeners=function(){},e}(M);e.a=V}}]);