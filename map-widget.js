'use strict';
(() => {
  const clock = document.querySelector('.clockPanel');
  function fitClock() {
    const height = clock.clientHeight - clock.querySelector('.calendarRow').offsetHeight - 17;
    const width = clock.clientWidth - (document.querySelector('#clockPeriod').hidden ? 16 : 40);
    clock.style.setProperty('--fit-clock', Math.max(18, Math.min(90, height, width / 3.2)) + 'px');
  }
  if (window.ResizeObserver) new ResizeObserver(fitClock).observe(clock);
  new MutationObserver(fitClock).observe(document.querySelector('#clockPeriod'), {attributes:true, attributeFilter:['hidden']});
  window.addEventListener('resize',fitClock);
  fitClock();

  const bridge = window.G2Android;
  if (!bridge || typeof bridge.mapBounds !== 'function') return;
  const byId = id => document.getElementById(id);
  document.body.dataset.liveMap = 'true';
  byId('connections').hidden = false;
  const slot = byId('liveMapSlot');
  slot.hidden = false;
  const panel = document.querySelector('.mapPanel');
  panel.setAttribute('aria-label','Google Maps y búsqueda de destinos');
  document.querySelector('.mapPanel .mapsLaunch')?.remove();
  const gps = panel.querySelector('[data-action="gps"]');
  gps.setAttribute('aria-label','Centrar mi ubicación'); gps.title = 'Centrar mi ubicación';
  byId('mapApiStatus').textContent = 'Google Maps interactivo dentro del tablero. En Fuente de ubicación puedes elegir el GPS del celular o el del estéreo.';
  const disclaimer=byId('mapApiStatus').parentElement;
  for (const node of disclaimer.childNodes) if(node.nodeType===3) node.textContent=node.textContent.replace('El mapa es ilustrativo. ','');
  for (const control of document.querySelectorAll('.musicPanel [data-action="airplay-setup"],.musicPanel .smallAction,.musicPanel .musicExpand,.musicDetailFooter .airplayShortcut')) control.hidden=true;
  document.querySelector('.musicDetailFooter > span').textContent='Audio del teléfono compartido con este estéreo';
  const destination=byId('destination');
  const route=document.createElement('button');
  route.type='button';
  route.className='dashRoute';
  route.textContent='Trazar ruta';
  panel.querySelector('.mapBottom').insertBefore(route,gps);
  let routeActive=false, turn='', turnSource='';
  function stopRoute(){
    routeActive=false;
    route.textContent='Trazar ruta';
    bridge.endDashRoute();
    refreshLabel();
  }
  function startRoute(){
    if(!destination.value.trim()){destination.focus();return;}
    bridge.routeInDash(destination.value.trim());
    routeActive=true;
    route.textContent='Cerrar ruta';
    refreshLabel();
  }
  route.addEventListener('click',()=>{if(routeActive)stopRoute();else startRoute();});
  const go=panel.querySelector('.searchGo');
  go.removeAttribute('data-action');
  go.title='Trazar ruta al destino';
  go.setAttribute('aria-label',go.title);
  go.addEventListener('click',startRoute);
  destination.addEventListener('input',()=>{if(routeActive)stopRoute();});
  const turns=byId('shareTurns');
  turns.checked=bridge.sharedTurnsEnabled?.()===true;
  turns.addEventListener('change',()=>{
    bridge.setSharedTurns?.(turns.checked);
    if(!turns.checked){turn='';turnSource='';refreshLabel();}
  });
  let lastBounds='', frame=0, watch=null, sourceEpoch=0, staleTimer, lastFix=null, firstShow=true;
  let detail='', mapError=false, phone=false;
  function refreshLabel() {
    const sharedInstruction=routeActive?'':turn;
    panel.classList.toggle('hasTurn',Boolean(sharedInstruction));
    byId('mapStateTitle').textContent=sharedInstruction?sharedInstruction:mapError?'Mapa pendiente':routeActive?'Ruta en Dash':'Google Maps';
    byId('mapStateDetail').textContent=sharedInstruction?turnSource:mapError?'Toca centrar para reintentar':detail;
  }
  function status(text, kmh=-1) {
    detail=text;
    byId('speed').textContent=(Number.isFinite(kmh)&&kmh>=0?Math.round(kmh):'—')+' km/h';
    byId('speedLabel').textContent=text;
    refreshLabel();
  }
  function fresh(text, kmh) {
    status(text,kmh);
    clearTimeout(staleTimer);
    staleTimer=setTimeout(()=>{lastFix=null;status(phone?'Sin señal reciente del celular':'Sin señal reciente del estéreo');},30000);
  }
  function stopWatch() {
    sourceEpoch++;
    if(watch!==null){navigator.geolocation?.clearWatch(watch);watch=null;}
    clearTimeout(staleTimer);
    lastFix=null;
  }
  function sourceChanged() {
    stopWatch();
    phone=bridge.usesPhoneGps();
    status(phone?'Esperando GPS del celular':'Buscando GPS del estéreo');
    if(phone || document.hidden) return;
    if(!navigator.geolocation){status('GPS del estéreo no disponible');return;}
    const token=sourceEpoch;
    watch=navigator.geolocation.watchPosition(pos=>{
      if(token!==sourceEpoch) return;
      const center=!lastFix;
      lastFix=pos.coords;
      fresh('GPS del estéreo',pos.coords.speed==null?-1:pos.coords.speed*3.6);
      bridge.centerFix(pos.coords.latitude,pos.coords.longitude,center);
    },err=>{if(token===sourceEpoch){lastFix=null;status(err.code===1?'Permite ubicación · toca centrar':'Sin señal GPS del estéreo');}},
      {enableHighAccuracy:true,maximumAge:5000,timeout:15000});
  }
  function locate(){
    if(mapError) {mapError=false;bridge.retryWidgetMap();refreshLabel();}
    if(phone){bridge.recenterPhone();return;}
    if(lastFix) bridge.centerFix(lastFix.latitude,lastFix.longitude,true);
    else sourceChanged();
  }
  function measure() {
    frame=0;
    const blocked=panel.hidden||document.hidden||!byId('startup').hidden||document.querySelector('.screenveil:not([hidden])');
    const b=slot.getBoundingClientRect();
    const coords=blocked?[0,0,0,0]:[b.left/innerWidth,b.top/innerHeight,b.width/innerWidth,b.height/innerHeight];
    const key=coords.join('/');
    if(key!==lastBounds){lastBounds=key;bridge.mapBounds(...coords);}
    if(!blocked&&firstShow){firstShow=false;sourceChanged();}
  }
  const schedule=()=>{if(!frame)frame=requestAnimationFrame(measure);};
  new MutationObserver(schedule).observe(document.body,{subtree:true,attributes:true,
      attributeFilter:['hidden','data-map-side','data-map-size']});
  if(window.ResizeObserver)new ResizeObserver(schedule).observe(slot);
  window.addEventListener('resize',schedule);
  window.addEventListener('g2-layout',schedule);
  document.addEventListener('visibilitychange',()=>{if(document.hidden)stopWatch();else if(!firstShow)sourceChanged();schedule();});
  window.G2LiveMap={startRoute,sharedTurn(text,source){turn=String(text||'');turnSource=String(source||'');refreshLabel();},locate,refreshLabel,sourceChanged,mapError(){mapError=true;refreshLabel();},
    mapLoaded(){mapError=false;refreshLabel();},
    phoneUpdate(valid,speed,label){if(!phone)return;if(valid)fresh(label,speed);else status(label);}
  };
  schedule();
})();
