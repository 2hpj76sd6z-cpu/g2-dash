'use strict';
/* G² Dash 0.4 — web demo + optional Android MediaSession metadata. No remote Wi-Fi protocol or OBD integration. */
(() => {
  const $ = (q) => document.querySelector(q);
  const $$ = (q) => [...document.querySelectorAll(q)];
  const KEY = 'g2dash.v02.preferences';
  const SETTINGS_SCHEMA_VERSION = 1;
  const defaults = { theme:'auto', timeFormat:'24h', wall:'dusk', blur:14, opacity:54, transparentOpacity:3.5, accent:'steel', logo:'apex', widgetTint:'green', windowTint:'green', windowFinish:'smoked', windowSmoke:78, windowClear:25, widgetStyle:'smoked', widgetBlur:8, performance:'normal', customWallpaper:null };
  const opacityControls = {
    smoked: { key: 'opacity', min: 20, max: 88, step: 1 },
    transparent: { key: 'transparentOpacity', min: 0, max: 12, step: 0.5 }
  };
  const opacityFormat = new Intl.NumberFormat('es-MX', { maximumFractionDigits: 1 });
  const tones = ['graphite', 'blue', 'green', 'warm', 'black', 'violet', 'wine', 'teal', 'amber'];
  const allowedSettings = {
    theme:['auto','night','day'],
    timeFormat:['12h','24h'],
    wall:['dusk','blue','gray'],
    accent:['steel','blue','warm'],
    logo:['apex','pulse','orbit'],
    widgetTint:tones,
    windowFinish:['smoked','clear'],
    windowTint:['auto', ...tones],
    widgetStyle:['smoked','transparent'],
    performance:['normal','light']
  };
  function normalizeSettings(candidate) {
    const next={...defaults,...(candidate&&typeof candidate==='object'?candidate:{})};
    Object.keys(allowedSettings).forEach(key=>{
      if(!allowedSettings[key].includes(next[key])) next[key]=defaults[key];
    });
    next.blur=Number.isFinite(Number(next.blur))?Math.max(0,Math.min(30,Number(next.blur))):defaults.blur;
    for (const { key, min, max, step } of Object.values(opacityControls)) {
      const value = Number.parseFloat(next[key]);
      next[key] = Number.isFinite(value)
        ? Math.max(min, Math.min(max, Math.round(value / step) * step))
        : defaults[key];
    }
    next.widgetBlur=Number.isFinite(Number(next.widgetBlur))?Math.max(0,Math.min(16,Number(next.widgetBlur))):defaults.widgetBlur;
    if(next.customWallpaper!==null && typeof next.customWallpaper!=='string') next.customWallpaper=null;
    return next;
  }
  const pictures = [
    {path:'assets/ibiza_frente_sin_fondo.webp',name:'Frontal — recorte transparente',alt:'SEAT Ibiza blanco aislado, vista frontal de tres cuartos, con placa protegida',cutout:true},
    {path:'assets/ibiza_trasera_sin_fondo.png',name:'Trasera — recorte transparente',alt:'SEAT Ibiza blanco aislado, vista trasera de tres cuartos, con placa protegida',cutout:true},
    {path:'assets/ibiza_frente_original.webp',name:'Frontal — foto original',alt:'Fotografía frontal real del SEAT Ibiza blanco con boca negra y placa protegida',personalSource:true},
    {path:'assets/ibiza_trasera_original.webp',name:'Trasera — foto original',alt:'Fotografía trasera real del SEAT Ibiza blanco con placa protegida'},
  ];
  let settings = {...defaults};
  try {settings=normalizeSettings(JSON.parse(localStorage.getItem(KEY)||'{}'))} catch(_){}
  let currentPhoto=0, zoom=1, toastTimer=null;
  function save(){
    try {
      settings=normalizeSettings(settings);
      localStorage.setItem(KEY,JSON.stringify({...settings,schemaVersion:SETTINGS_SCHEMA_VERSION}));
    } catch(_) {showToast('No fue posible guardar los ajustes en este navegador')}
  }
  function showToast(message){const el=$('#toast');if(!el)return;el.textContent=message;el.classList.add('show');clearTimeout(toastTimer);toastTimer=setTimeout(()=>el.classList.remove('show'),3200)}
  function updateClock(){
    const now = new Date();
    const format12h=settings.timeFormat==='12h';
    const timeParts=new Intl.DateTimeFormat('es-MX',{hour:'2-digit',minute:'2-digit',hour12:format12h}).formatToParts(now);
    $('#clock').textContent=timeParts.filter(part=>part.type!=='dayPeriod').map(part=>part.value).join('').trim();
    const period=timeParts.find(part=>part.type==='dayPeriod')?.value||'';
    $('#clockPeriod').textContent=period.replace(/[\s.]/g,'').toLocaleUpperCase('es-MX');
    $('#clockPeriod').hidden=!format12h;
    const localDate = [now.getFullYear(),String(now.getMonth()+1).padStart(2,'0'),String(now.getDate()).padStart(2,'0')].join('-');
    $('#clock').dateTime=localDate+'T'+String(now.getHours()).padStart(2,'0')+':'+String(now.getMinutes()).padStart(2,'0');
    const fullDate = new Intl.DateTimeFormat('es-MX',{weekday:'long',day:'numeric',month:'long',year:'numeric'}).format(now);
    const compactDate = new Intl.DateTimeFormat('es-MX',{weekday:'short',day:'numeric',month:'short'}).format(now);
    $('#dateLong').textContent=compactDate;
    $('#dateLong').dateTime=localDate;
    $('#dateLong').title=fullDate.charAt(0).toUpperCase()+fullDate.slice(1);
    $('#dateShort').dateTime=localDate;
    $('#dateShort').textContent=new Intl.DateTimeFormat('es-MX',{weekday:'short',day:'numeric',month:'short',year:'numeric'}).format(now);
    if(settings.theme==='auto')applyTheme(false);
  }
  function applyTheme(persist=true){
    const hour=new Date().getHours();
    document.body.dataset.mode=settings.theme==='auto'?(hour>=7&&hour<19?'day':'night'):settings.theme;
    $$('[data-theme]').forEach(btn=>btn.classList.toggle('selected',btn.dataset.theme===settings.theme));
    if(persist)save();
  }
  const accents = {steel:'#b7cce8',blue:'#84bbff',warm:'#dfc1a0'};
  const walls={dusk:'radial-gradient(ellipse at 75% 20%, #837068 0%, transparent 39%),linear-gradient(120deg,#3f4d60,#242d3b 48%,#1b2333)',blue:'radial-gradient(ellipse at 75% 25%, #516f90 0%, transparent 39%),linear-gradient(120deg,#243c57,#152438 48%,#1a2432)',gray:'radial-gradient(ellipse at 75% 25%, #6a6970 0%, transparent 39%),linear-gradient(120deg,#353843,#212735 55%,#171d29)'};
  function markToneSelection(attribute, selected){
    $$(`[${attribute}]`).forEach(button=>{
      const active=button.getAttribute(attribute)===selected;
      button.classList.toggle('selected',active);
      button.setAttribute('aria-pressed',String(active));
    });
  }
  function applyWindowTone(){
    const finish=settings.windowFinish==='clear'?'clear':'smoked';
    const val=Number(settings[finish==='clear'?'windowClear':'windowSmoke']);
    const alpha=Number.isFinite(val)?Math.max(5,Math.min(95,val)):(finish==='clear'?25:78);
    document.body.dataset.windowFinish=finish;
    document.documentElement.style.setProperty('--windowAlpha',alpha/100);
    $('#windowFinish').value=finish;$('#windowAlpha').value=alpha;$('#windowAlphaValue').textContent=alpha+'%';
    const tone=settings.windowTint;
    document.body.dataset.windowTint=tone;
    const rootStyle=document.documentElement.style;
    if(tone==='auto'){
      rootStyle.removeProperty('--windowTintRgb');
      rootStyle.removeProperty('--windowTintDayRgb');
    }else{
      rootStyle.setProperty('--windowTintRgb',`var(--tone-${tone})`);
      rootStyle.setProperty('--windowTintDayRgb',`var(--tone-${tone}-day)`);
    }
    markToneSelection('data-window-tone',tone);
  }
  function applyWidgetOpacity() {
    const { key, min, max, step } = opacityControls[settings.widgetStyle];
    const slider = $('#opacity');
    const value = settings[key];
    slider.min = min;
    slider.max = max;
    slider.step = step;
    slider.value = value;
    const formatted = opacityFormat.format(value) + '%';
    slider.setAttribute('aria-valuetext', formatted);
    $('#opacityValue').textContent = formatted;

    const rootStyle = document.documentElement.style;
    rootStyle.setProperty('--glassOpacity', settings.opacity / 100);
    rootStyle.setProperty('--transparentOpacity', settings.transparentOpacity / 100);
  }
  /* Aplicación ligera: mover sliders no repinta el fondo ni las fotos. */
  function applyWidgetSettings(){
    const widgetBlur=Number.isFinite(Number(settings.widgetBlur))?Math.max(0,Math.min(16,Number(settings.widgetBlur))):8;
    const tint=settings.widgetTint;
    document.body.dataset.widgetTint=tint;
    const widgetStyle=allowedSettings.widgetStyle.includes(settings.widgetStyle)?settings.widgetStyle:'smoked';
    document.body.dataset.widgetStyle=widgetStyle;
    applyWidgetOpacity();
    document.documentElement.style.setProperty('--widgetTintRgb',`var(--tone-${tint})`);
    document.documentElement.style.setProperty('--widgetEdgeRgb',`var(--tone-${tint}-edge)`);
    document.documentElement.style.setProperty('--widgetBlur',widgetBlur+'px');
    $('#widgetBlur').value=widgetBlur;$('#widgetBlurValue').textContent=`${widgetBlur} px`;
    markToneSelection('data-tint',tint);
    $$('[data-widget-style]').forEach(b=>{const chosen=b.dataset.widgetStyle===widgetStyle;b.classList.toggle('selected',chosen);b.setAttribute('aria-pressed',String(chosen));});
    const styleNote=$('#widgetStyleNote');
    if(styleNote)styleNote.textContent=widgetStyle==='transparent'
      ? 'Transparente: ajusta el relleno de 0 a 12 %. El contorno conserva el tono elegido.'
      : 'Ahumado: mantiene el vidrio oscuro y el desenfoque actual.';
  }
  function applyPerformanceMode(){
    const mode=settings.performance==='light'?'light':'normal';
    document.body.dataset.performance=mode;
    $$('.dashboard .glass, .sidebar.glass, .bottomDock button').forEach(el=>{
      if(mode==='light'){
        el.style.setProperty('backdrop-filter','none','important');
        el.style.setProperty('-webkit-backdrop-filter','none','important');
      } else {
        el.style.removeProperty('backdrop-filter');
        el.style.removeProperty('-webkit-backdrop-filter');
      }
    });
    $$('[data-performance]').forEach(button=>{
      const selected=button.dataset.performance===mode;
      button.classList.toggle('selected',selected);
      button.setAttribute('aria-pressed',String(selected));
    });
    const note=$('#performanceNote');
    if(note)note.textContent=mode==='light'
      ? 'Ligero: elimina desenfoques de fondo y vidrio; conserva tus fotos, colores y preferencias para el modo normal.'
      : 'Visual normal: conserva los desenfoques y los ajustes elegidos.';
  }
  function applyClockFormat(){
    const format=settings.timeFormat==='12h'?'12h':'24h';
    settings.timeFormat=format;
    $$('[data-time-format]').forEach(button=>{
      const selected=button.dataset.timeFormat===format;
      button.classList.toggle('selected',selected);
      button.setAttribute('aria-pressed',String(selected));
    });
  }
  function applySettings(){
    document.documentElement.style.setProperty('--accent',accents[settings.accent]||accents.steel);
    document.documentElement.style.setProperty('--wallBlur',`${Number(settings.blur)||0}px`);
    applyWidgetSettings();
    applyWindowTone();
    applyPerformanceMode();
    applyClockFormat();
    $('#blur').value=settings.blur;$('#blurValue').textContent=`${settings.blur} px`;
    $$('[data-accent]').forEach(b=>b.classList.toggle('selected',b.dataset.accent===settings.accent));
    markToneSelection('data-wall', settings.customWallpaper ? null : settings.wall);
    const activeLogo=allowedSettings.logo.includes(settings.logo)?settings.logo:'apex';
    $('#brandLogo').dataset.logo=activeLogo;
    $$('.logoChoices button[data-logo]').forEach(b=>{const selected=b.dataset.logo===activeLogo;b.classList.toggle('selected',selected);b.setAttribute('aria-pressed',String(selected));});
    $('#wallpaper').style.backgroundImage=settings.customWallpaper?`url(${JSON.stringify(settings.customWallpaper)})`:walls[settings.wall]||walls.dusk;
    applyTheme(false);
  }
  const profileKey='g2dash.vehicle.v1';
  let profile={name:'',photos:[]};
  try {
    const item=JSON.parse(localStorage.getItem(profileKey)||'null');
    if(item && typeof item.name==='string' && Array.isArray(item.photos)) profile={name:item.name.slice(0,32),photos:item.photos.filter(p=>typeof p==='string' && /^data:image[/](webp|jpeg|png);base64,/.test(p)).slice(0,4)};
  } catch (_) {}
  function saveProfile(next){
    try{localStorage.setItem(profileKey,JSON.stringify(next));profile=next;photoChange(0);return true;}
    catch(_){showToast('Almacenamiento lleno. Prueba con menos fotos.');return false;}
  }
  function photoChange(index){
    const custom=profile.photos.length>0;
    const name=profile.name||(custom?'Mi vehículo':'Mi Ibiza');
    $('.vehHeading strong').textContent=name;
    $('.vehHeading small').textContent=custom?'Mis fotografías':'SEAT 6J · 5 puertas';
    $('.vehiclePanel').setAttribute('aria-label',name);
    $('.vehicleModal header h2').textContent=name;
    $('#carName').value=profile.name;
    document.querySelectorAll('.vehiclePanel .conceptEntry,.vehicleModal .conceptToGallery').forEach(el=>el.hidden=custom);
    $('.vehicleModal header p').textContent=custom?'Mis fotografías':'Dos recortes transparentes y dos fotografías originales';
    $('.vehicleModal .disclaimer').hidden=custom;
    $('.photoDots span').textContent=custom?(profile.photos.length+' fotos'):'2 recortes + 2 fotos originales';
    for(let i=0;i<4;i++){
      const dot=$('#carDot'+i);
      dot.hidden=custom && i>=profile.photos.length;
      dot.setAttribute('aria-label',custom?'Ver foto '+(i+1):['Ver recorte frontal','Ver recorte trasero','Ver foto frontal','Ver foto trasera'][i]);
      dot.title=dot.getAttribute('aria-label');
    }
    if(custom){
      currentPhoto=(index+profile.photos.length)%profile.photos.length;
      const caption='Foto '+(currentPhoto+1)+' de '+profile.photos.length;
      for(const img of [$('#vehiclePhoto'),$('#modalCar')]){ img.src=profile.photos[currentPhoto];img.alt=name+' · '+caption; }
      $('#vehicleView').setAttribute('aria-label','Ver fotos de '+name);
      $('#vehicleView').classList.add('isCutout');$('.modalPhoto').classList.add('isCutout');
      $('#carChip').textContent=caption;$('#modalLabel').textContent=caption;
      for(let i=0;i<4;i++)$('#carDot'+i).classList.toggle('on',i===currentPhoto);
      return;
    }
    $('#vehicleView').setAttribute('aria-label','Ver fotos de '+name);
    currentPhoto=(index+pictures.length)%pictures.length;
    const pic=pictures[currentPhoto];
    const isPersonal=Boolean(pic.personalSource&&settings.personalCar);
    const path=isPersonal?settings.personalCar:pic.path;
    const isCutout=Boolean(pic.cutout||isPersonal);
    $('#vehiclePhoto').src=path;$('#modalCar').src=path;
    $('#vehiclePhoto').alt=isPersonal?'Imagen personal del SEAT Ibiza':pic.alt;
    $('#modalCar').alt=isPersonal?'Imagen personal del SEAT Ibiza':pic.alt;
    $('#vehicleView').classList.toggle('isCutout',isCutout);
    $('.modalPhoto').classList.toggle('isCutout',isCutout);
    $('#carChip').textContent=isPersonal?'IMAGEN PERSONAL':pic.cutout?(currentPhoto===0?'RECORTE TRANSPARENTE · FRENTE':'RECORTE TRANSPARENTE · TRASERA'):(currentPhoto===2?'FRENTE · FOTO ORIGINAL':'TRASERA · FOTO ORIGINAL');
    $('#modalLabel').textContent=isPersonal?'Imagen personal — guardada en este dispositivo':pic.name;
    document.querySelectorAll('#carDot0, #carDot1, #carDot2, #carDot3').forEach((b,i)=>b.classList.toggle('on',i===currentPhoto));
  }
  // Native Android Activity lifecycle, not a vehicle ignition sensor.
  // The third argument is ONLY for clearly labeled WEB screenshot tests.
  window.G2AndroidLifecycle = function(phase, count=0, preview=false) {
    if(!window.G2Android && !preview)return;
    const allowed=['cold_start','resume','home','restored','web'];
    if(!allowed.includes(phase))return;
    updateClock();
    const simulated=preview===true || !window.G2Android;
    const names={
      cold_start:'INICIO DE LA APP',
      resume:'SESIÓN REANUDADA',
      home:'REGRESO A G² DASH',
      restored:'ACTIVIDAD RECONSTRUIDA',
      web:'VISTA WEB · SIN EVENTO ANDROID'
    };
    const label=$('#lifecycleMode'),details=$('#lifecycleDetail');
    const time=new Intl.DateTimeFormat('es-MX',{hour:'2-digit',minute:'2-digit',hour12:false}).format(new Date());
    if(label)label.textContent=(simulated && phase!=='web'?'SIMULACIÓN WEB · ':'')+names[phase];
    if(details)details.textContent=phase==='web'
      ? 'Este navegador no informa eventos Android de inicio o suspensión del estéreo.'
      : (simulated ? 'Prueba visual, no un evento Android real. ' : 'Evento de la app, NO de la llave del coche. ')
        +'Hora '+time+' · Reanudaciones de la sesión: '+Math.max(0,Number(count)||0);
    document.body.dataset.lifecycle=phase;
    const splash=$('#startup');
    if((phase==='resume'||phase==='restored') && splash && !splash.hidden) {
      $('#startupSkip')?.click(); // Return straight to the dashboard.
    }
  };
  function openScreen(selector){$(selector).hidden=false}
  function closeScreen(selector){$(selector).hidden=true}
  function openMap(){
    const term=$('#destination').value.trim();
    const url=term?`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(term)}`:'https://www.google.com/maps/';
    const w=window.open(url,'_blank','noopener,noreferrer');
    if(!w)showToast('Permite ventanas nuevas para abrir Google Maps');
  }
  function launchNavigation(){
    const term=$('#destination').value.trim();
    if(window.G2LiveMap)window.G2Android.openMap('');
    else if(window.G2Android)window.G2Android.openMap(term);
    else openMap();
  }
  function nativeMapsAvailable(){
    try{return Boolean(window.G2Android&&typeof window.G2Android.hasNativeMaps==='function'&&window.G2Android.hasNativeMaps())}catch(_){return false}
  }
  function updateMapDestinationState(){
    const hasDestination=Boolean($('#destination').value.trim());
    const panel=$('.mapPanel');
    panel.dataset.destination=hasDestination?'ready':'empty';
    if(window.G2LiveMap){window.G2LiveMap.refreshLabel();return;}
    $('#mapStateTitle').textContent=hasDestination?'Destino preparado':'Mapa de demostración';
    const detail=$('#mapStateDetail');
    if(detail)detail.textContent=window.G2Android
      ? (nativeMapsAvailable()?'Google Maps nativo dentro del DASH':'Mapa web de respaldo dentro del DASH')
      : (hasDestination?'Google Maps se mostrará dentro del DASH':'Toca para usar Google Maps dentro del DASH');
    const apiStatus=$('#mapApiStatus');
    if(apiStatus){
      apiStatus.dataset.state=window.G2Android?(nativeMapsAvailable()?'ready':'fallback'):'web';
      apiStatus.textContent=window.G2Android
        ? (nativeMapsAvailable()?'Google Maps nativo listo: búsqueda, gestos y zoom permanecen dentro del DASH.':'Modo web de respaldo: configura GOOGLE_MAPS_API_KEY para activar el mapa nativo.')
        : 'Vista web de demostración: la clave de Google Maps se configura únicamente en el APK Android.';
    }
  }
  let gpsRequest = 0;
  function activateGps(){
    const request = ++gpsRequest;
    $('#speed').innerHTML='— <small>km/h</small>';
    if(!navigator.geolocation){$('#speedLabel').textContent='GPS no disponible';showToast('GPS no disponible en este navegador');return}
    if(!window.isSecureContext){$('#speedLabel').textContent='GPS requiere HTTPS';showToast('Para usar GPS, abre la prueba mediante HTTPS');return}
    $('#speedLabel').textContent='Buscando señal…';
    navigator.geolocation.getCurrentPosition((position)=>{
      if(request!==gpsRequest)return;
      const speed=position.coords.speed;
      if(Number.isFinite(speed)&&speed>=0){$('#speed').innerHTML=`${Math.round(speed*3.6)} <small>km/h</small>`;$('#speedLabel').textContent='Lectura puntual GPS'}
      else{$('#speed').innerHTML='— <small>km/h</small>';$('#speedLabel').textContent='Sin velocidad GPS'}
      showToast('Lectura puntual del GPS; el mapa sigue siendo ilustrativo.');
    },(err)=>{
      if(request!==gpsRequest)return;
      $('#speed').innerHTML='— <small>km/h</small>';
      const errors={1:['Sin permiso GPS','Permiso de ubicación denegado'],2:['Señal GPS ausente','No se obtuvo ubicación'],3:['GPS sin respuesta','No se pudo obtener ubicación a tiempo']};
      const [label,message]=errors[err.code]||['GPS no disponible','No se pudo obtener ubicación'];
      $('#speedLabel').textContent=label;showToast(message);
    },{enableHighAccuracy:true,timeout:10000,maximumAge:2000});
  }
  // Android invokes this using a local and isolated WebView origin.
  // On GitHub Pages this API only receives clearly marked visual test data.
  function mmss(ms) {
    const s = Math.max(0, Math.floor((Number(ms)||0) / 1000));
    return String(Math.floor(s/60)).padStart(2,'0')+':'+String(s%60).padStart(2,'0');
  }
  // Etiquetas locales: nunca se descargan metadatos de servicios externos.
  const musicApps = {
    'com.syu.carlink':'CARLINK 2.0',
    'io.github.jqssun.airplay':'AIRPLAY · IPHONE',
    'com.spotify.music':'SPOTIFY',
    'com.google.android.apps.youtube.music':'YOUTUBE MUSIC',
    'com.apple.android.music':'APPLE MUSIC',
    'com.aspiro.tidal':'TIDAL',
    'deezer.android.app':'DEEZER',
    'org.videolan.vlc':'VLC',
    'com.maxmpz.audioplayer':'POWERAMP',
    'in.krosbits.musicolet':'MUSICOLET',
    'com.amazon.mp3':'AMAZON MUSIC',
    'com.samsung.android.app.music':'SAMSUNG MUSIC',
    'com.sec.android.app.music':'SAMSUNG MUSIC',
    'com.pandora.android':'PANDORA',
    'com.soundcloud.android':'SOUNDCLOUD'
  };
  function musicText(value, fallback, max=160) {
    if (typeof value !== 'string' || !value.trim()) return fallback;
    return value.trim().slice(0,max);
  }
  const miniSeek=$('#miniSeek');
  let scrubbing=false;
  miniSeek.addEventListener('pointerdown',()=>{scrubbing=true;});
  miniSeek.addEventListener('pointerup',()=>{scrubbing=false;});
  miniSeek.addEventListener('pointercancel',()=>{scrubbing=false;});
  miniSeek.addEventListener('input',()=>{$('#musicCurrent').textContent=mmss(+miniSeek.value);});
  miniSeek.addEventListener('change',()=>{scrubbing=false;if(!miniSeek.disabled)window.G2Android?.musicSeek(+miniSeek.value);});
  window.G2MusicUpdate = function(data) {
    if (!data || typeof data !== 'object') return;
    const preview = data.preview === true;
    const granted = data.granted === true;
    const active = granted && musicText(data.title,'') !== '' &&
      data.title !== 'Sin música activa' && data.title !== 'Conecta tu música';
    const playing = !preview && active && data.playing === true;
    const permission = $('#musicPermission');
    const source = $('#songSource');
    permission.hidden = !window.G2Android || granted;
    $('#songName').textContent = preview ? musicText(data.title,'Pista de prueba') :
      (active ? musicText(data.title,'Sin título') : (granted?'Sin música activa':'Conecta tu música'));
    $('#songArtist').textContent = preview ? 'EJEMPLO VISUAL · NO REPRODUCE AUDIO' :
      (active ? musicText(data.artist,'Artista desconocido') :
       (granted?'Reproduce música en tu iPhone':'Autoriza música en Ajustes'));
    if(source){
      const sourceName=preview?'EJEMPLO VISUAL':active?(data.carLink?'CARLINK 2.0':musicApps[data.package]||'ANDROID AUDIO'):'MÚSICA LOCAL';
      source.textContent=sourceName;
      source.dataset.source=String(data.package||'').toLowerCase();
    }
    const art = $('#albumArt');
    if (active && !preview && typeof data.albumArt === 'string' &&
        data.albumArt.startsWith('data:image/jpeg;base64,') && data.albumArt.length < 400000) {
      if (art.getAttribute('src') !== data.albumArt) art.src = data.albumArt;
      art.hidden = false;
    } else {
      art.hidden = true; art.removeAttribute('src');
    }
    const duration = active || preview ? Math.max(0,Number(data.durationMs)||0) : 0;
    const position = Math.min(duration,Math.max(0,Number(data.positionMs)||0));
    const seekable=active && !preview && data.canSeek===true && duration>0 && Boolean(window.G2Android);
    miniSeek.hidden=!seekable; miniSeek.disabled=!seekable;
    $('#trackProgress').parentElement.hidden=seekable;
    if (!scrubbing || !seekable) {
      miniSeek.max=duration||1; miniSeek.value=position;
      $('#musicCurrent').textContent=mmss(position);
    }
    $('#musicDuration').textContent=mmss(duration);
    $('#trackProgress').style.width=duration > 0 ? Math.min(100,position/duration*100)+'%' : '0%';
    $('#play .icon use').setAttribute('href',playing?'#i-pause':'#i-play');
    $('#play').setAttribute('aria-label',playing?'Pausar música':'Reproducir música');
    const status = preview?'EJEMPLO VISUAL' : !granted?'Permiso pendiente' : !active?'Sin reproducción' : playing?'Reproduciendo':'En pausa';
    $('#musicStatus').textContent=status;
    $('#musicSource').textContent=preview?'NO ES AUDIO REAL' : (active ? (data.carLink?'CARLINK 2.0':musicApps[data.package]||'ANDROID AUDIO') : (granted?'ANDROID':'SIN CONECTAR'));
    $('#deviceStatus').textContent = preview ? 'VISTA DE PRUEBA' : (granted?'ANDROID · MÚSICA':'ANDROID · v0.5.8');
    $('.musicPanel').dataset.media=preview?'preview':(!granted?'permission':(!active?'idle':(playing?'playing':'paused')));
    if (window.G2Android) {
      $('.musicControls [data-action="demo-prev"]').disabled=!active || data.canPrevious===false;
      $('.musicControls [data-action="demo-next"]').disabled=!active || data.canNext===false;
      $('#play').disabled=active && data.canPlay===false;
    }
    window.G2LiveMap?.sharedTurn(data.turn,data.turnSource);
    window.G2MusicDetail?.update(data);
    window.G2AirPlay?.updateMedia(data);
  };
  function mediaAction(action) {
    if (!window.G2Android) {
      if(action==='toggle')demoPlay();
      else showToast('Música de demostración: sin reproducción real');
      return;
    }
    window.G2Android.musicControl(action);
  }
  function demoPlay(){
    // El prototipo web no reproduce audio: no modificar el icono de pausa.
    showToast('Demo visual: instala el APK y conecta una app musical de Android');
  }
  function handleAction(action){
    switch(action){
      case 'home':closeScreen('#settingsVeil');closeScreen('#vehicleVeil');window.G2Concept?.close();window.G2Apps?.close();window.G2MusicDetail?.close();window.G2AirPlay?.close();break;
      case 'open-settings':openScreen('#settingsVeil');break;
      case 'intro-preview':closeScreen('#settingsVeil');window.G2ShowIntro?.();break;
      case 'home-settings':
        if(window.G2Android?.openHomeSettings)window.G2Android.openHomeSettings();
        else showToast('Disponible únicamente en el APK Android');
        break;
      case 'close-settings':closeScreen('#settingsVeil');break;
      case 'vehicle':window.G2Concept?.close();openScreen('#vehicleVeil');break;
      case 'vehicle-real':window.G2Concept?.close();photoChange(2);openScreen('#vehicleVeil');break;
      case 'concept-360':closeScreen('#vehicleVeil');window.G2Concept?.open();break;
      case 'close-vehicle':closeScreen('#vehicleVeil');break;
      case 'car-front-cutout':photoChange(0);break;
      case 'car-rear-cutout':photoChange(1);break;
      case 'car-front':photoChange(2);break;
      case 'car-rear':photoChange(3);break;
      case 'car-cutout':photoChange(0);break;
      case 'car-prev':photoChange(currentPhoto-1);break;
      case 'car-next':photoChange(currentPhoto+1);break;
      case 'music':window.G2MusicDetail?.open(true);break;
      case 'choose-carlink':window.G2Android?.openTarget('choose-carlink');break;
      case 'choose-gps':window.G2Android?.chooseLocationSource();break;
      case 'gps':if(window.G2LiveMap)window.G2LiveMap.locate();else activateGps();break;
      case 'navigation':launchNavigation();break;
      case 'zoom-in':case 'zoom-out':{const delta=action==='zoom-in'?.75:-.75;if(window.G2Android?.mapZoom){window.G2Android.mapZoom(delta);showToast('Zoom de Google Maps aplicado');}else{zoom=Math.max(.8,Math.min(1.8,zoom+(action==='zoom-in'?.1:-.1)));$('#mapArtwork').style.transform=`scale(${zoom})`;showToast('Zoom ilustrativo: no sustituye navegación real');}break;}
      case 'music-access':if(window.G2Android)window.G2Android.openMusicAccess();else showToast('Solo en Android');break;
      case 'music-detail':window.G2MusicDetail?.open();break;
      case 'airplay-setup':window.G2AirPlay?.open();break;
      case 'demo-play':mediaAction('toggle');break;
      case 'demo-prev':mediaAction('previous');break;
      case 'demo-next':mediaAction('next');break;
      case 'demo-shuffle':case 'demo-repeat':showToast('Aleatorio y repetir: integración pendiente');break;
      case 'reset-settings':settings={...defaults};applySettings();updateClock();save();showToast('Ajustes restablecidos');break;
      case 'fullscreen':if(document.fullscreenElement){document.exitFullscreen?.()}else{document.documentElement.requestFullscreen?.().catch(()=>showToast('Pantalla completa no disponible'))}break;
      case 'apps':window.G2Apps?.open();break;
      case 'phone':case 'bluetooth':case 'radio':if(window.G2Android)window.G2Android.openTarget(action);else showToast('Acceso pendiente de integración nativa en Android');break;
      default:showToast('Función en desarrollo');
    }
  }
  document.addEventListener('click',(event)=>{
    const trigger=event.target.closest('[data-action]');
    if(trigger){event.preventDefault();handleAction(trigger.dataset.action)}
    if(event.target.matches('.screenveil'))event.target.hidden=true;
  });
  $$('[data-theme]').forEach(button=>button.addEventListener('click',()=>{settings.theme=button.dataset.theme;applyTheme();}));
  $$('[data-time-format]').forEach(button=>button.addEventListener('click',()=>{
    settings.timeFormat=button.dataset.timeFormat;
    applyClockFormat();updateClock();save();
  }));
  $$('[data-wall]').forEach(button=>button.addEventListener('click',()=>{settings.wall=button.dataset.wall;settings.customWallpaper=null;applySettings();save()}));
  $$('[data-accent]').forEach(button=>button.addEventListener('click',()=>{settings.accent=button.dataset.accent;applySettings();save()}));
  $$('[data-tint]').forEach(button=>button.addEventListener('click',()=>{settings.widgetTint=button.dataset.tint;applyWidgetSettings();save()}));
  $$('[data-window-tone]').forEach(button=>button.addEventListener('click',()=>{
    settings.windowTint=button.dataset.windowTone;
    applyWindowTone();
    save();
  }));
  $$('[data-widget-style]').forEach(button=>button.addEventListener('click',()=>{settings.widgetStyle=button.dataset.widgetStyle;applyWidgetSettings();save()}));
  $$('[data-performance]').forEach(button=>button.addEventListener('click',()=>{
    settings.performance=button.dataset.performance==='light'?'light':'normal';
    applyPerformanceMode();save();
  }));
  $$('.logoChoices button[data-logo]').forEach(button=>button.addEventListener('click',()=>{settings.logo=button.dataset.logo;applySettings();save()}));
  $('#blur').addEventListener('input',(e)=>{settings.blur=Number(e.target.value);document.documentElement.style.setProperty('--wallBlur',`${settings.blur}px`);$('#blurValue').textContent=`${settings.blur} px`;save()});
  $('#opacity').addEventListener('input', event => {
    const { key } = opacityControls[settings.widgetStyle];
    settings[key] = event.target.valueAsNumber;
    applyWidgetOpacity();
  });
  $('#opacity').addEventListener('change', save);
  $('#windowFinish').addEventListener('change',e=>{settings.windowFinish=e.target.value;applyWindowTone();save();});
  $('#windowAlpha').addEventListener('input',e=>{settings[settings.windowFinish==='clear'?'windowClear':'windowSmoke']=Number(e.target.value);applyWindowTone();save();});
  $('#widgetBlur').addEventListener('input',(e)=>{settings.widgetBlur=Number(e.target.value);applyWidgetSettings();save()});
  $('#carName').addEventListener('change',e=>saveProfile({...profile,name:e.target.value.trim().slice(0,32)}));
  let uploadId=0;
  $('#restoreIbiza').addEventListener('click',()=>{uploadId++;if(saveProfile({name:'',photos:[]}))showToast('Ibiza original seleccionado');});
  $('#chooseCar').addEventListener('change',async event=>{
    const files=Array.from(event.target.files||[]);event.target.value='';const id=++uploadId;
    if(!files.length)return;
    if(files.length>4||files.some(f=>f.size>16*1024*1024)){showToast('Selecciona hasta 4 imágenes de menos de 16 MB cada una');return;}
    try{
      const photos=[];
      for(const file of files){
        const url=URL.createObjectURL(file);
        try{
          const img=new Image();
          await new Promise((resolve,reject)=>{img.onload=resolve;img.onerror=reject;img.src=url;});
          const ratio=Math.min(1,880/img.naturalWidth,580/img.naturalHeight);
          const canvas=document.createElement('canvas');canvas.width=Math.max(1,Math.round(img.naturalWidth*ratio));canvas.height=Math.max(1,Math.round(img.naturalHeight*ratio));
          canvas.getContext('2d').drawImage(img,0,0,canvas.width,canvas.height);
          photos.push(canvas.toDataURL('image/webp',.78));
        }finally{URL.revokeObjectURL(url);}
      }
      if(id===uploadId && saveProfile({...profile,photos}))showToast('Fotos del vehículo guardadas');
    }catch(_){if(id===uploadId)showToast('No se pudo leer la imagen. Usa JPG, PNG o WebP.');}
  });
  $('#chooseWall').addEventListener('change',(event)=>{
    const file=event.target.files?.[0];if(!file)return;
    if(!file.type.startsWith('image/')){showToast('Elige una imagen compatible');return}
    if(file.size>12e6){showToast('Elige una imagen menor a 12 MB');return}
    const reader=new FileReader();reader.onload=()=>{
      const img=new Image();img.onload=()=>{
        const ratio=Math.min(1,1500/img.width,900/img.height);
        const canvas=document.createElement('canvas');canvas.width=Math.round(img.width*ratio);canvas.height=Math.round(img.height*ratio);
        canvas.getContext('2d').drawImage(img,0,0,canvas.width,canvas.height);
        settings.customWallpaper=canvas.toDataURL('image/jpeg',.72);
        applySettings();save();showToast('Fondo aplicado en este dispositivo');
      };img.src=reader.result;
    };reader.readAsDataURL(file);
  });
  $('#destination').addEventListener('input',updateMapDestinationState);
  $('#destination').addEventListener('keydown',(event)=>{if(event.key==='Enter'){event.preventDefault();if(window.G2LiveMap)window.G2LiveMap.startRoute();else launchNavigation()}});
  document.addEventListener('keydown',(event)=>{if(event.key==='Escape'){closeScreen('#settingsVeil');closeScreen('#vehicleVeil');window.G2Concept?.close();window.G2Apps?.close();window.G2MusicDetail?.close();window.G2AirPlay?.close()}});
  if(window.G2Android){$('#musicSource').textContent='ANDROID';const tag=$('#deviceStatus');if(tag)tag.textContent='ANDROID · v0.5.8';$('#musicPermission').hidden=false;$('#homeConfig').hidden=false;}
  photoChange(0);updateMapDestinationState();applySettings();updateClock();setInterval(updateClock,20000);
})();
