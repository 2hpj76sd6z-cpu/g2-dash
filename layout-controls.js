'use strict';
/* G² Dash layout controls: configurable map position and metric order.
   Does not intercept navigation or GPS buttons; edits happen only in settings. */
(() => {
  const settings = document.querySelector('.settingsBody');
  const dashboard = document.querySelector('.dashboard');
  const stats = document.querySelector('.vehicleStats');
  if (!settings || !dashboard || !stats) return;
  // Genuine navigation launches Google's mapping app/browser; artwork is only a preview.
  const map = dashboard.querySelector('.mapPanel');
  if (map && !map.querySelector('.mapsLaunch')) {
    const launch = document.createElement('button');
    launch.type='button';
    launch.className='mapsLaunch';
    launch.dataset.action='navigation';
    launch.textContent='MAPA EN DASH';
    launch.setAttribute('aria-label','Usar Google Maps dentro del DASH');
    const stamp=map.querySelector('.demoStamp');
    map.insertBefore(launch,stamp||null);
  }
  const key = 'g2dash.layout.v058';
  const metrics = Array.from(stats.children);
  if (metrics.length !== 3) return;
  const names = ['Autonomía', 'Velocímetro GPS', 'Voltaje OBD'];
  metrics.forEach((el,i) => {el.dataset.metric = String(i);});
  const initial = {mapSide:'center', mapSize:'wide', metricOrder:[0,1,2]};
  let state = {...initial,metricOrder:[...initial.metricOrder]};
  try {
    const saved = JSON.parse(localStorage.getItem(key) || '{}');
    if (['center','right'].includes(saved.mapSide)) state.mapSide = saved.mapSide;
    if (['wide','balanced'].includes(saved.mapSize)) state.mapSize = saved.mapSize;
    if (Array.isArray(saved.metricOrder) && saved.metricOrder.length === 3 &&
        new Set(saved.metricOrder).size === 3 &&
        saved.metricOrder.every(n => Number.isInteger(n) && n >= 0 && n < 3))
      state.metricOrder = saved.metricOrder.slice();
  } catch (_) {}
  function save() {
    try { localStorage.setItem(key,JSON.stringify(state)); } catch (_) {}
  }
  const container = document.createElement('section');
  container.className = 'layoutEditor';
  container.setAttribute('aria-label','Organización de widgets');
  container.innerHTML = [
    '<h3>Organización del tablero</h3>',
    '<p class="hint">Elige la distribución y acomoda las métricas del Ibiza. Se guarda automáticamente.</p>',
    '<div class="layoutEditorGroup"><strong>Posición del mapa</strong><div class="layoutEditorChoices">',
    '<button type="button" data-map-side="center">Mapa central</button>',
    '<button type="button" data-map-side="right">Mapa a la derecha</button></div></div>',
    '<div class="layoutEditorGroup"><strong>Tamaño del mapa</strong><div class="layoutEditorChoices">',
    '<button type="button" data-map-size="wide">Amplio</button>',
    '<button type="button" data-map-size="balanced">Equilibrado</button></div></div>',
    '<div class="layoutEditorGroup"><strong>Orden de métricas</strong>',
    '<p class="hint">Selecciona una métrica y luego otra para intercambiarlas. Los valores GPS siguen siendo reales cuando el dispositivo los proporciona.</p>',
    '<div class="layoutMetricChoices" role="group" aria-label="Cambiar posición de métricas"></div></div>',
    '<button type="button" class="layoutReset">Restablecer distribución</button>',
    '<p class="hint layoutEditorStatus" role="status">El mapa grande es ilustrativo; toca MAPA EN DASH para usar Google Maps sin salir del tablero.</p>'
  ].join('');
  const first = settings.querySelector('.settingActions');
  settings.insertBefore(container,first || null);
  const metricButtons = container.querySelector('.layoutMetricChoices');
  const status = container.querySelector('.layoutEditorStatus');
  let selection = null;
  function render() {
    dashboard.dataset.mapSide = state.mapSide;
    dashboard.dataset.mapSize = state.mapSize;
    state.metricOrder.forEach(id => stats.appendChild(metrics[id]));
    container.querySelectorAll('[data-map-side]').forEach(btn=>{
      const active=btn.dataset.mapSide===state.mapSide;
      btn.classList.toggle('selected',active);
      btn.setAttribute('aria-pressed',String(active));
    });
    container.querySelectorAll('[data-map-size]').forEach(btn=>{
      const active=btn.dataset.mapSize===state.mapSize;
      btn.classList.toggle('selected',active);
      btn.setAttribute('aria-pressed',String(active));
    });
    metricButtons.replaceChildren();
    state.metricOrder.forEach((id,pos)=>{
      const btn=document.createElement('button');
      btn.type='button';
      btn.dataset.metricId=String(id);
      btn.textContent=(pos+1)+'. '+names[id];
      btn.setAttribute('aria-pressed',String(selection===id));
      if(selection===id)btn.classList.add('selected');
      metricButtons.appendChild(btn);
    });
  }
  container.addEventListener('click',event=>{
    const btn=event.target.closest('button');
    if(!btn||!container.contains(btn))return;
    if(btn.dataset.mapSide){
      state.mapSide=btn.dataset.mapSide;
      selection=null;status.textContent='Posición actualizada. El mapa sigue siendo ilustrativo.';
    } else if(btn.dataset.mapSize){
      state.mapSize=btn.dataset.mapSize;
      selection=null;status.textContent='Tamaño actualizado y guardado.';
    } else if(btn.dataset.metricId!==undefined){
      const id=Number(btn.dataset.metricId);
      if(selection===null){selection=id;status.textContent='Selecciona la métrica con la que deseas intercambiar '+names[id]+'.';}
      else {
        const a=state.metricOrder.indexOf(selection),b=state.metricOrder.indexOf(id);
        if(a>=0&&b>=0)[state.metricOrder[a],state.metricOrder[b]]=[state.metricOrder[b],state.metricOrder[a]];
        selection=null;status.textContent='Nuevo orden de métricas guardado.';
      }
    } else if(btn.classList.contains('layoutReset')){
      state={...initial,metricOrder:[...initial.metricOrder]};
      selection=null;status.textContent='Distribución original restaurada.';
    } else return;
    save();render();
  });
  render();
})();
