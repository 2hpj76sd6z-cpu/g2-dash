/* G² Dash: 12 approved conceptual images; NOT 3D, no changes to real vehicle photos. */
(()=>{'use strict';
 const modal=document.querySelector('#conceptVeil');if(!modal)return;
 const $=(s)=>modal.querySelector(s);
 const names=['Frontal','Frontal 3/4 der.','3/4 delantera der.','Lateral derecha','3/4 trasera der.','Trasera 3/4 der.','Trasera','Trasera 3/4 izq.','3/4 trasera izq.','Lateral izquierda','3/4 delantera izq.','Frontal 3/4 izq.'];
 const path=(i)=>'assets/ibiza360/vista_'+String(i).padStart(2,'0')+'.webp';
 const img=$('#conceptHero'),slider=$('#conceptSlider'),grid=$('#conceptThumbs'),auto=$('#conceptAuto');
 let frame=1,timer=null,down=null;
 const thumbButtons=[],cached=new Set();
 for(let n=0;n<12;n++){
   const b=document.createElement('button');b.type='button';b.className='conceptThumb';
   b.setAttribute('aria-label',names[n]+', '+n*30+' grados');
   const im=document.createElement('img');im.src=path(n);im.alt='';im.loading='lazy';
   const caption=document.createElement('span');caption.textContent=n*30+'°';
   b.append(im,caption);b.addEventListener('click',()=>{stop();show(n)});
   grid.append(b);thumbButtons.push(b);
 }
 function preload(n){if(cached.has(n))return;const next=new Image();next.src=path(n);cached.add(n)}
 function show(n){
   frame=(n+12)%12;
   img.src=path(frame);img.alt='Vista conceptual del Ibiza: '+names[frame]+', '+frame*30+' grados';
   $('#conceptAngle').textContent=frame*30+'°';$('#conceptLabel').textContent=names[frame];slider.value=frame;
   thumbButtons.forEach((b,i)=>b.setAttribute('aria-pressed',String(i===frame)));
   preload((frame+1)%12);preload((frame+11)%12);
 }
 function stop(){if(timer!==null){clearInterval(timer);timer=null}auto.textContent='▶';auto.setAttribute('aria-pressed','false');auto.title='Iniciar recorrido'}
 function open(){stop();modal.hidden=false;show(frame);$('#conceptClose').focus()}
 function close(){stop();modal.hidden=true}
 function step(d){stop();show(frame+d)}
 function toggle(){if(timer!==null){stop();return}auto.textContent='Ⅱ';auto.setAttribute('aria-pressed','true');auto.title='Detener recorrido';timer=setInterval(()=>{if(modal.hidden||document.hidden){stop();return}show(frame+1)},1250)}
 $('#conceptPrev').addEventListener('click',()=>step(-1));
 $('#conceptNext').addEventListener('click',()=>step(1));
 $('#conceptClose').addEventListener('click',close);
 auto.addEventListener('click',toggle);
 slider.addEventListener('input',e=>{stop();show(Number(e.target.value))});
 $('#conceptStage').addEventListener('pointerdown',e=>{down=e.clientX});
 $('#conceptStage').addEventListener('pointerup',e=>{if(down===null)return;const diff=e.clientX-down;down=null;if(Math.abs(diff)>28)step(diff<0?1:-1)});
 $('#conceptStage').addEventListener('pointercancel',()=>{down=null});
 modal.addEventListener('click',e=>{if(e.target===modal)close()});
 document.addEventListener('visibilitychange',()=>{if(document.hidden)stop()});
 document.addEventListener('keydown',e=>{
  if(modal.hidden)return;
  if(e.key==='Escape')close();
  if(e.key==='ArrowLeft'){e.preventDefault();step(-1)}
  if(e.key==='ArrowRight'){e.preventDefault();step(1)}
 });
 window.G2Concept={open,close,isOpen:()=>!modal.hidden,getIndex:()=>frame};
 show(1);
})();
