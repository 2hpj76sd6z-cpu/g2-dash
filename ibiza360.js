/* G² Dash v0.5.1 — Ibiza: 12 illustrations, NOT a real 3D model.
 * Same approved 500×314 source frames. Bounded preloading and touch controls.
 */
(()=>{'use strict';
 const modal=document.querySelector('#conceptVeil');if(!modal)return;
 const $=(s)=>modal.querySelector(s);
 const labels=['Frontal','Frontal 3/4 der.','3/4 delantera der.','Lateral derecha','3/4 trasera der.','Trasera 3/4 der.','Trasera','Trasera 3/4 izq.','3/4 trasera izq.','Lateral izquierda','3/4 delantera izq.','Frontal 3/4 izq.'];
 const path=(n)=>'assets/ibiza360/vista_'+String(n).padStart(2,'0')+'.webp';
 const img=$('#conceptHero'),stage=$('#conceptStage'),slider=$('#conceptSlider');
 const grid=$('#conceptThumbs'),auto=$('#conceptAuto'),scale=$('#conceptScale');
 let frame=1,timer=null,press=null;
 const thumbs=[],cache=new Map();
 for(let n=0;n<12;n++){
   const btn=document.createElement('button');
   btn.type='button';btn.className='conceptThumb';
   btn.setAttribute('aria-label',labels[n]+', '+n*30+' grados');
   const pic=document.createElement('img');
   pic.src=path(n);pic.alt='';pic.loading='lazy';pic.decoding='async';pic.width=100;pic.height=63;
   const text=document.createElement('span');text.textContent=n*30+'°';
   btn.append(pic,text);btn.addEventListener('click',()=>{stop();show(n)});
   grid.append(btn);thumbs.push(btn);
 }
 function neighborCache(n){
   const wanted=[(n+1)%12,(n+11)%12];
   for(const key of cache.keys())if(!wanted.includes(key))cache.delete(key);
   for(const key of wanted)if(!cache.has(key)){
     const next=new Image();next.decoding='async';next.src=path(key);cache.set(key,next);
   }
 }
 function show(n){
   frame=(n+12)%12;
   img.src=path(frame);
   img.alt='Vista ilustrativa del Ibiza: '+labels[frame]+', '+frame*30+' grados';
   $('#conceptAngle').textContent=frame*30+'°';
   $('#conceptLabel').textContent=labels[frame];
   slider.value=String(frame);
   thumbs.forEach((b,i)=>b.setAttribute('aria-pressed',String(i===frame)));
   neighborCache(frame);
 }
 function stop(){
   if(timer!==null){clearInterval(timer);timer=null}
   auto.textContent='▶';auto.setAttribute('aria-pressed','false');
   auto.title='Iniciar secuencia de vistas';
 }
 function setScale(expanded){
   stage.dataset.scale=expanded?'large':'native';
   scale.setAttribute('aria-pressed',String(expanded));
   scale.textContent=expanded?'1.3×':'1:1';
   scale.title=expanded?'Volver al tamaño original para mayor nitidez':'Ampliar imagen (puede perder nitidez)';
 }
 function open(){
   stop();modal.hidden=false;show(frame);
   $('#conceptClose').focus();
 }
 function close(){stop();modal.hidden=true;cache.clear();}
 function step(n){stop();show(frame+n);}
 function toggle(){
   if(timer!==null){stop();return}
   if(document.hidden)return;
   auto.textContent='Ⅱ';auto.setAttribute('aria-pressed','true');auto.title='Detener secuencia';
   timer=setInterval(()=>{if(modal.hidden||document.hidden){stop();return}show(frame+1)},1500);
 }
 $('#conceptPrev').addEventListener('click',()=>step(-1));
 $('#conceptNext').addEventListener('click',()=>step(1));
 $('#conceptClose').addEventListener('click',close);
 auto.addEventListener('click',toggle);
 scale.addEventListener('click',()=>setScale(stage.dataset.scale!=='large'));
 slider.addEventListener('input',e=>{stop();show(Number(e.target.value))});
 stage.addEventListener('pointerdown',e=>{
   if(e.pointerType==='mouse' && e.button!==0)return;
   press={id:e.pointerId,x:e.clientX};
   if(stage.setPointerCapture)try{stage.setPointerCapture(e.pointerId)}catch(_){}
 });
 stage.addEventListener('pointerup',e=>{
   if(!press||press.id!==e.pointerId)return;
   const dx=e.clientX-press.x;press=null;
   if(Math.abs(dx)<32)return;
   const amount=Math.min(3,Math.max(1,Math.round(Math.abs(dx)/90)));
   step((dx<0?1:-1)*amount);
 });
 stage.addEventListener('pointercancel',()=>{press=null});
 modal.addEventListener('click',e=>{if(e.target===modal)close()});
 document.addEventListener('visibilitychange',()=>{if(document.hidden)stop()});
 window.addEventListener('blur',stop);
 document.addEventListener('keydown',e=>{
   if(modal.hidden)return;
   if(e.key==='Escape'){close();return}
   if(e.key==='ArrowLeft'){e.preventDefault();step(-1)}
   if(e.key==='ArrowRight'){e.preventDefault();step(1)}
 });
 window.G2Concept={open,close,isOpen:()=>!modal.hidden,getIndex:()=>frame,getScale:()=>stage.dataset.scale,isPlaying:()=>timer!==null};
 setScale(false);show(1);
})();
