'use strict';
/* G² Dash Apps: solo lista apps reales en Android. Web = ejemplo inequívoco. */
(() => {
 const veil=document.getElementById('appsVeil');
 const list=document.getElementById('appsGrid');
 const search=document.getElementById('appsSearchInput');
 const count=document.getElementById('appsCount');
 const info=document.getElementById('appsInfo');
 const reload=document.getElementById('appsReload');
 const filterBtns=[...document.querySelectorAll('[data-apps-filter]')];
 if(!veil||!list||!search||!count||!info||!reload)return;
 const FAVORITES_KEY='g2dash.apps.favorites.v1';
 let apps=[],onlyFavorites=false,isAndroid=false,focusBack=null;
 let favs=[];
 try{
   const val=JSON.parse(localStorage.getItem(FAVORITES_KEY)||'[]');
   if(Array.isArray(val))favs=val.filter(x=>typeof x==='string').slice(0,100);
 }catch(_){}
 function persist(){try{localStorage.setItem(FAVORITES_KEY,JSON.stringify(favs))}catch(_){}}
 const sample=[
  {name:'Navegación',symbol:'🧭'}, {name:'Música',symbol:'♫'},
  {name:'Archivos',symbol:'▤'}, {name:'Radio',symbol:'◉'},
  {name:'Teléfono',symbol:'☎'}, {name:'Cámara',symbol:'◎'}
 ];
 function element(tag,className,text){
  const node=document.createElement(tag);
  if(className)node.className=className;
  if(text!==undefined)node.textContent=text;
  return node;
 }
 function empty(title,description){
  const box=element('div','appsEmpty');
  const ico=element('span','');ico.textContent='◈';ico.style.fontSize='34px';ico.style.color='#61d993';
  box.append(ico,element('strong','',title),element('span','',description));
  list.append(box);
 }
 function render(){
  list.textContent='';
  const q=search.value.trim().toLocaleLowerCase('es');
  const visible=apps.filter(a=>(!onlyFavorites||favs.includes(a.id)) &&
     (!q||a.name.toLocaleLowerCase('es').includes(q)));
  count.textContent=isAndroid?visible.length+' de '+apps.length+' aplicaciones':'6 accesos de muestra';
  filterBtns.forEach(b=>{const active=(b.dataset.appsFilter==='favorites')===onlyFavorites;
     b.classList.toggle('selected',active);b.setAttribute('aria-pressed',String(active));});
  if(!isAndroid){info.textContent='EJEMPLO VISUAL · NO SON APPS INSTALADAS';info.classList.add('appsDemo')}
  else{info.classList.remove('appsDemo');info.textContent='Aplicaciones instaladas en este estéreo'}
  if(!visible.length){
    empty(onlyFavorites?'Sin favoritos todavía':'No hay coincidencias',
      isAndroid?'Prueba otra búsqueda o marca una app con la estrella.':'La lista real solo está disponible al ejecutar el APK Android.');
    return;
  }
  const frag=document.createDocumentFragment();
  for(const a of visible){
    const item=element('div','appsCard');
    item.setAttribute('role','button');item.tabIndex=0;
    item.title=isAndroid?'Abrir '+a.name:a.name+' · ejemplo visual';
    const icon=element('div','appsIcon');
    if(isAndroid&&typeof a.icon==='string'&&/^data:image\/png;base64,[A-Za-z0-9+/=]+$/.test(a.icon)){
      const img=document.createElement('img');img.src=a.icon;img.alt='';img.loading='lazy';icon.append(img);
    }else icon.textContent=a.symbol||'◈';
    item.append(icon,element('span','appsName',a.name));
    if(isAndroid){
      const star=element('button','appsStar'+(favs.includes(a.id)?' on':''),favs.includes(a.id)?'★':'☆');
      star.type='button';star.title=favs.includes(a.id)?'Quitar de favoritos':'Agregar a favoritos';
      star.setAttribute('aria-label',star.title);
      star.addEventListener('click',ev=>{ev.stopPropagation();
        favs=favs.includes(a.id)?favs.filter(i=>i!==a.id):[...favs,a.id].slice(0,100);
        persist();render();
      });
      item.append(star);
      const launch=()=>{
        try{window.G2Android.launchInstalledApp(a.packageName,a.activityName)}
        catch(_){info.textContent='No se pudo abrir esta aplicación';}
      };
      item.addEventListener('click',launch);
      item.addEventListener('keydown',e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();launch()}});
    }
    frag.append(item);
  }
  list.append(frag);
 }
 function load(){
   isAndroid=Boolean(window.G2Android&&typeof window.G2Android.getInstalledApps==='function');
   if(!isAndroid){apps=sample.filter(a=>a.name.toLocaleLowerCase('es').includes(search.value.trim().toLocaleLowerCase('es')));onlyFavorites=false;render();return}
   info.textContent='Consultando aplicaciones instaladas…';
   try{
      const raw=JSON.parse(window.G2Android.getInstalledApps());
      if(!Array.isArray(raw))throw new Error('No es una lista');
      apps=raw.filter(a=>a&&typeof a.name==='string'&&typeof a.packageName==='string'
         &&typeof a.activityName==='string'&&a.packageName.length<200)
         .map(a=>({name:a.name,packageName:a.packageName,activityName:a.activityName,
          id:a.packageName+'/'+a.activityName,icon:a.icon||''}));
      apps.sort((a,b)=>a.name.localeCompare(b.name,'es',{sensitivity:'base'}));
      render();
   }catch(_){apps=[];list.textContent='';empty('No se pudo consultar Android',
       'Revisa que tengas instalada la versión actual de G² Dash.');count.textContent='0 aplicaciones'}
 }
 function open(){
  focusBack=document.activeElement;
  veil.hidden=false;search.value='';onlyFavorites=false;load();search.focus();
 }
 function close(){veil.hidden=true;if(focusBack&&focusBack.focus)focusBack.focus()}
 document.getElementById('appsClose').addEventListener('click',close);
 reload.addEventListener('click',load);
 search.addEventListener('input',render);
 filterBtns.forEach(b=>b.addEventListener('click',()=>{
   if(!isAndroid&&b.dataset.appsFilter==='favorites')return;
   onlyFavorites=b.dataset.appsFilter==='favorites';render();
 }));
 veil.addEventListener('click',e=>{if(e.target===veil)close()});
 document.addEventListener('keydown',e=>{if(e.key==='Escape'&&!veil.hidden){e.stopPropagation();close()}});
 window.G2Apps={open,close};
})();
