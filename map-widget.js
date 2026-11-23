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
  document.querySelectorAll('[data-action="airplay-setup"]').forEach(btn => {
    if (!btn.closest('.musicPanel,.musicDetailFooter')) return;
    btn.dataset.action='carlink'; btn.title='Conectar con CarLink 2.0'; btn.setAttribute('aria-label',btn.title);
    if (!btn.querySelector('svg')) btn.textContent='Abrir CarLink';
  });
  document.querySelector('.musicPanel .trackRow > .smallAction').dataset.action='carlink';
  const phoneRoute=document.createElement('button');
  phoneRoute.type='button'; phoneRoute.dataset.action='carlink'; phoneRoute.textContent='Ruta del celular';
  phoneRoute.title='Ver la navegación del teléfono en CarLink';
  panel.querySelector('.mapBottom').insertBefore(phoneRoute,gps);
  const footer=document.querySelector('.musicDetailFooter > span');
  if(footer) footer.textContent='Música del teléfono mediante CarLink y sesiones multimedia del estéreo';

  let lastBounds='', frame=0, watch=null, sourceEpoch=0, staleTimer, lastFix=null, firstShow=true;
  let detail='', mapError=false, phone=false;
  function refreshLabel() {
    byId('mapStateTitle').textContent=mapError?'Mapa sin conexión':'Google Maps';
    byId('mapStateDetail').textContent=mapError?'Comprueba internet y toca centrar':detail;
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
    const blocked=document.hidden||!byId('startup').hidden||document.querySelector('.screenveil:not([hidden])');
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
  document.addEventListener('visibilitychange',()=>{if(document.hidden)stopWatch();else if(!firstShow)sourceChanged();schedule();});
  window.G2LiveMap={locate,refreshLabel,sourceChanged,mapError(){mapError=true;refreshLabel();},
    mapLoaded(){mapError=false;refreshLabel();},
    phoneUpdate(valid,speed,label){if(!phone)return;if(valid)fresh(label,speed);else status(label);}
  };
  schedule();
})();
