'use strict';
(() => {
  const dash=document.querySelector('.dashboard'), settings=document.querySelector('.settingsBody');
  const cards={clock:'.clockPanel',music:'.musicPanel',map:'.mapPanel',vehicle:'.vehiclePanel'};
  const titles={clock:'Reloj',music:'Música',map:'Mapa',vehicle:'Vehículo'};
  const keys=Object.keys(cards), cols=12, rows=8, storage='g2dash.widgetGrid.v1';
  const fresh=()=>({clock:{x:0,y:0,w:3,h:3,on:true},music:{x:0,y:3,w:3,h:5,on:true},map:{x:3,y:0,w:6,h:8,on:true},vehicle:{x:9,y:0,w:3,h:8,on:true}});
  const copy=x=>JSON.parse(JSON.stringify(x));
  const inside=r=>r.x>=0&&r.y>=0&&r.w>=2&&r.h>=2&&r.x+r.w<=cols&&r.y+r.h<=rows;
  const overlap=(a,b)=>a.x<b.x+b.w&&b.x<a.x+a.w&&a.y<b.y+b.h&&b.y<a.y+a.h;
  function valid(s) {
    return s && keys.every(k=>s[k]&&typeof s[k].on==='boolean'&&['x','y','w','h'].every(p=>Number.isInteger(s[k][p]))&&inside(s[k])) &&
      keys.some(k=>s[k].on) && !keys.some((k,i)=>s[k].on&&keys.slice(i+1).some(j=>s[j].on&&overlap(s[k],s[j])));
  }
  let state=fresh(),selected='map',gesture=null;
  try{
    const saved=JSON.parse(localStorage.getItem(storage));
    if(valid(saved)) state=saved;
    else {
      const old=JSON.parse(localStorage.getItem('g2dash.layout.v058'));
      if(old?.mapSize==='balanced'){state.map.w=5;state.vehicle.x=8;state.vehicle.w=4;}
      if(old?.mapSide==='right'){state.vehicle.x=3;state.map.x=3+state.vehicle.w;}
    }
  }catch(_){}
  const board=document.createElement('div');
  board.className='widgetBoard';dash.appendChild(board);
  for(const k of keys){const el=dash.querySelector(cards[k]);el.dataset.card=k;board.appendChild(el);}
  dash.querySelector('.leftStack')?.remove();
  dash.classList.add('editableDashboard');
  const editor=document.createElement('section');editor.className='layoutEditor';
  editor.innerHTML='<h3>Editar widgets</h3><div class="widgetChoices"></div><div class="gridPreview" aria-label="Distribución del tablero"></div><div class="editTools"><label for="tileSelect">Widget</label><select id="tileSelect"></select><button type="button" class="layoutReset">Restablecer</button></div><div class="gridSteps"></div><p class="hint layoutEditorStatus" role="status">Arrastra un recuadro para moverlo. Arrastra su esquina inferior derecha para cambiar el ancho y el alto.</p>';
  settings.insertBefore(editor,settings.firstChild);
  const preview=editor.querySelector('.gridPreview'),status=editor.querySelector('.layoutEditorStatus'),select=editor.querySelector('select');
  const choice=editor.querySelector('.widgetChoices');
  for(const k of keys){
    const label=document.createElement('label'),check=document.createElement('input');check.type='checkbox';check.dataset.show=k;
    label.append(check,document.createTextNode(titles[k]));choice.appendChild(label);
    const tile=document.createElement('button');tile.type='button';tile.dataset.tile=k;tile.textContent=titles[k];
    const handle=document.createElement('span');handle.className='resizeGrip';handle.textContent='↘';handle.title='Cambiar tamaño';
    tile.appendChild(handle);preview.appendChild(tile);
    const option=document.createElement('option');option.value=k;option.textContent=titles[k];select.appendChild(option);
  }
  const moves={left:['Mover a la izquierda','←','x',-1],right:['Mover a la derecha','→','x',1],up:['Subir','↑','y',-1],down:['Bajar','↓','y',1],narrow:['Reducir ancho','Ancho −','w',-1],wide:['Aumentar ancho','Ancho +','w',1],short:['Reducir alto','Alto −','h',-1],tall:['Aumentar alto','Alto +','h',1]};
  for(const [id,[name,text]] of Object.entries(moves)){const b=document.createElement('button');b.type='button';b.dataset.step=id;b.title=name;b.setAttribute('aria-label',name);b.textContent=text;editor.querySelector('.gridSteps').appendChild(b);}
  function render(){
    if(!state[selected].on)selected=keys.find(k=>state[k].on);
    for(const k of keys){
      const r=state[k],tile=preview.querySelector('[data-tile="'+k+'"]'),el=board.querySelector(cards[k]);
      for(const target of [tile,el]){target.hidden=!r.on;target.style.gridColumn=(1+r.x)+' / span '+r.w;target.style.gridRow=(1+r.y)+' / span '+r.h;}
      tile.setAttribute('aria-pressed',String(k===selected));
      choice.querySelector('[data-show="'+k+'"]').checked=r.on;
      select.querySelector('[value="'+k+'"]').disabled=!r.on;
    }
    select.value=selected;
    window.dispatchEvent(new Event('g2-layout'));
  }
  function persist(){try{localStorage.setItem(storage,JSON.stringify(state));}catch(_){status.textContent='Memoria llena. Los cambios solo durarán esta sesión.';}}
  // Preserve other tiles where possible. When space is tight, reflow them into free cells.
  function propose(base,k,rect){
    if(!inside(rect))return null;
    const next=copy(base);next[k]={...rect,on:true};
    const placed=[next[k]], others=keys.filter(j=>j!==k&&base[j].on);
    others.sort((a,b)=>Number(overlap(base[a],rect))-Number(overlap(base[b],rect)) || base[b].w*base[b].h-base[a].w*base[a].h);
    for(const j of others){
      const r=base[j];let best=null,cost=Infinity;
      for(let w=r.w;w>=2;w--)for(let h=r.h;h>=2;h--){
        const loss=(r.w*r.h-w*h)*100;
        if(loss>cost)continue;
        for(let y=0;y<=rows-h;y++)for(let x=0;x<=cols-w;x++){
          const c={x,y,w,h,on:true},score=loss+Math.abs(x-r.x)+Math.abs(y-r.y);
          if(score<cost&&!placed.some(p=>overlap(p,c))){cost=score;best=c;}
        }
      }
      if(!best)return null;
      next[j]=best;placed.push(best);
    }
    return next;
  }
  function adjust(base,r){
    const next=propose(base,selected,r);
    if(!next){status.textContent='No cabe en esa posición. Reduce el tamaño u oculta otro widget.';return false;}
    state=next;render();status.textContent=titles[selected]+' · ancho '+r.w+'/12 · alto '+r.h+'/8';return true;
  }
  select.onchange=()=>{selected=select.value;render();};
  choice.onchange=e=>{
    const k=e.target.dataset.show;if(!k)return;
    if(e.target.checked){selected=k;if(!adjust(state,{...state[k],on:true})){render();return;}}
    else if(keys.filter(j=>state[j].on).length>1){state[k].on=false;render();}
    else {e.target.checked=true;status.textContent='Deja al menos un widget visible.';return;}
    persist();
  };
  editor.querySelector('.layoutReset').onclick=()=>{state=fresh();selected='map';render();persist();status.textContent='Distribución original restaurada.';};
  editor.querySelector('.gridSteps').onclick=e=>{
    const move=moves[e.target.dataset.step];if(!move)return;
    const r={...state[selected]};r[move[2]]+=move[3];
    if(adjust(state,r))persist();
  };
  preview.onpointerdown=e=>{
    const tile=e.target.closest('[data-tile]');if(!tile)return;
    selected=tile.dataset.tile;render();const b=preview.getBoundingClientRect();
    gesture={base:copy(state),resize:e.target.classList.contains('resizeGrip'),x:e.clientX,y:e.clientY,w:b.width,h:b.height,id:e.pointerId};
    preview.setPointerCapture(e.pointerId);e.preventDefault();
  };
  preview.onpointermove=e=>{
    if(!gesture||e.pointerId!==gesture.id)return;
    const g=gesture,r={...g.base[selected]},dx=Math.round((e.clientX-g.x)*cols/g.w),dy=Math.round((e.clientY-g.y)*rows/g.h);
    if(g.resize){r.w=Math.max(2,Math.min(cols-r.x,r.w+dx));r.h=Math.max(2,Math.min(rows-r.y,r.h+dy));}
    else {r.x=Math.max(0,Math.min(cols-r.w,r.x+dx));r.y=Math.max(0,Math.min(rows-r.h,r.y+dy));}
    adjust(g.base,r);
  };
  preview.onpointerup=()=>{if(gesture)persist();gesture=null;};
  preview.onpointercancel=()=>{if(gesture){state=gesture.base;render();}gesture=null;};
  preview.onclick=e=>{const tile=e.target.closest('[data-tile]');if(tile){selected=tile.dataset.tile;render();}};
  function sizeCards(){
    for(const k of keys){const el=board.querySelector(cards[k]);if(el.hidden)continue;
      el.classList.toggle('shortCard',el.clientHeight<200);
      el.classList.toggle('thinCard',el.clientWidth<170);
      el.classList.toggle('tinyCard',el.clientHeight<120||el.clientWidth<115);
    }
  }
  if(window.ResizeObserver){const observer=new ResizeObserver(sizeCards);for(const el of board.children)observer.observe(el);}
  window.addEventListener('resize',sizeCards);
  render();sizeCards();
})();
