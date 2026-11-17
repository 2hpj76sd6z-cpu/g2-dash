'use strict';
/* G² Dash 0.4 — web demo + optional Android MediaSession metadata. No remote Wi-Fi protocol or OBD integration. */
(() => {
  const $ = (q) => document.querySelector(q);
  const $$ = (q) => [...document.querySelectorAll(q)];
  const KEY = 'g2dash.v02.preferences';
  const SETTINGS_SCHEMA_VERSION = 1;
  const defaults = { theme:'auto', timeFormat:'24h', wall:'dusk', blur:14, opacity:54, accent:'steel', logo:'apex', widgetTint:'graphite', widgetBlur:8, performance:'normal', customWallpaper:null };
  const allowedSettings = {
    theme:['auto','night','day'],
    timeFormat:['12h','24h'],
    wall:['dusk','blue','gray'],
    accent:['steel','blue','warm'],
    logo:['apex','pulse','orbit'],
    widgetTint:['graphite','blue','green','black'],
    performance:['normal','light']
  };
  function normalizeSettings(candidate) {
    const next={...defaults,...(candidate&&typeof candidate==='object'?candidate:{})};
    Object.keys(allowedSettings).forEach(key=>{
      if(!allowedSettings[key].includes(next[key])) next[key]=defaults[key];
    });
    next.blur=Number.isFinite(Number(next.blur))?Math.max(0,Math.min(30,Number(next.blur))):defaults.blur;
    next.opacity=Number.isFinite(Number(next.opacity))?Math.max(20,Math.min(88,Number(next.opacity))):defaults.opacity;
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
  const widgetTints = {graphite:'19, 25, 34',blue:'17, 32, 47',green:'17, 35, 33',black:'11, 14, 19'};
  const walls={dusk:'radial-gradient(ellipse at 75% 20%, #837068 0%, transparent 39%),linear-gradient(120deg,#3f4d60,#242d3b 48%,#1b2333)',blue:'radial-gradient(ellipse at 75% 25%, #516f90 0%, transparent 39%),linear-gradient(120deg,#243c57,#152438 48%,#1a2432)',gray:'radial-gradient(ellipse at 75% 25%, #6a6970 0%, transparent 39%),linear-gradient(120deg,#353843,#212735 55%,#171d29)'};
  /* Aplicación ligera: mover sliders no repinta el fondo ni las fotos. */
  function applyWidgetSettings(){
    const opacity=Number.isFinite(Number(settings.opacity))?Math.max(20,Math.min(88,Number(settings.opacity))):54;
    const widgetBlur=Number.isFinite(Number(settings.widgetBlur))?Math.max(0,Math.min(16,Number(settings.widgetBlur))):8;
    const tint=Object.prototype.hasOwnProperty.call(widgetTints,settings.widgetTint)?settings.widgetTint:'graphite';
    document.documentElement.style.setProperty('--glassOpacity',opacity/100);
    document.documentElement.style.setProperty('--widgetTintRgb',widgetTints[tint]);
    document.documentElement.style.setProperty('--widgetBlur',widgetBlur+'px');
    $('#opacity').value=opacity;$('#opacityValue').textContent=`${opacity}%`;
    $('#widgetBlur').value=widgetBlur;$('#widgetBlurValue').textContent=`${widgetBlur} px`;
    $$('[data-tint]').forEach(b=>{const chosen=b.dataset.tint===tint;b.classList.toggle('selected',chosen);b.setAttribute('aria-pressed',String(chosen));});
  }
  function applyPerformanceMode(){
    const mode=settings.performance==='light'?'light':'normal';
    document.body.dataset.performance=mode;
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
    applyPerformanceMode();
    applyClockFormat();
    $('#blur').value=settings.blur;$('#blurValue').textContent=`${settings.blur} px`;
    $$('[data-accent]').forEach(b=>b.classList.toggle('selected',b.dataset.accent===settings.accent));
    $$('[data-wall]').forEach(b=>b.classList.toggle('selected',b.dataset.wall===settings.wall));
    const activeLogo=allowedSettings.logo.includes(settings.logo)?settings.logo:'apex';
    $('#brandLogo').dataset.logo=activeLogo;
    $$('.logoChoices button[data-logo]').forEach(b=>{const selected=b.dataset.logo===activeLogo;b.classList.toggle('selected',selected);b.setAttribute('aria-pressed',String(selected));});
    $('#wallpaper').style.backgroundImage=settings.customWallpaper?`url(${JSON.stringify(settings.customWallpaper)})`:walls[settings.wall]||walls.dusk;
    applyTheme(false);
  }
  function photoChange(index){
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
    if(window.G2Android)window.G2Android.openMap(term);
    else openMap();
  }
  function updateMapDestinationState(){
    const hasDestination=Boolean($('#destination').value.trim());
    const panel=$('.mapPanel');
    panel.dataset.destination=hasDestination?'ready':'empty';
    $('#mapStateTitle').textContent=hasDestination?'Destino preparado':'Mapa de demostración';
    $('#mapStateDetail').textContent=hasDestination?'Google Maps se mostrará dentro del DASH':'Toca para usar Google Maps dentro del DASH';
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
    'io.github.jqssun.airplay':'AIRPLAY · IPHONE',
    'com.spotify.music':'SPOTIFY',
    'com.google.android.apps.youtube.music':'YOUTUBE MUSIC',
    'com.apple.android.music':'APPLE MUSIC',
    'com.aspiro.tidal':'TIDAL',
    'deezer.android.app':'DEEZER',
    'org.videolan.vlc':'VLC',
    'com.maxmpz.audioplayer':'POWERAMP',
    'in.krosbits.musicolet':'MUSICOLET'
  };
  function musicText(value, fallback, max=160) {
    if (typeof value !== 'string' || !value.trim()) return fallback;
    return value.trim().slice(0,max);
  }
  window.G2MusicUpdate = function(data) {
    if (!data || typeof data !== 'object') return;
    const preview = data.preview === true;
    const granted = data.granted === true;
    const active = granted && musicText(data.title,'') !== '' &&
      data.title !== 'Sin música activa' && data.title !== 'Conecta tu música';
    const playing = !preview && active && data.playing === true;
    const permission = $('#musicPermission');
    permission.hidden = !window.G2Android || granted;
    $('#songName').textContent = preview ? musicText(data.title,'Pista de prueba') :
      (active ? musicText(data.title,'Sin título') : (granted?'Sin música activa':'Conecta tu música'));
    $('#songArtist').textContent = preview ? 'EJEMPLO VISUAL · NO REPRODUCE AUDIO' :
      (active ? musicText(data.artist,'Artista desconocido') :
       (granted?'Abre una app de música':'Autoriza música en Ajustes'));
    const art = $('#albumArt');
    if (active && !preview && typeof data.albumArt === 'string' &&
        data.albumArt.startsWith('data:image/jpeg;base64,') && data.albumArt.length < 400000) {
      art.src = data.albumArt; art.hidden = false;
    } else {
      art.hidden = true; art.removeAttribute('src');
    }
    const duration = active || preview ? Math.max(0,Number(data.durationMs)||0) : 0;
    const position = Math.min(duration,Math.max(0,Number(data.positionMs)||0));
    $('#musicCurrent').textContent=mmss(position);
    $('#musicDuration').textContent=mmss(duration);
    $('#trackProgress').style.width=duration > 0 ? Math.min(100,position/duration*100)+'%' : '0%';
    $('#play .icon use').setAttribute('href',playing?'#i-pause':'#i-play');
    $('#play').setAttribute('aria-label',playing?'Pausar música':'Reproducir música');
    const status = preview?'EJEMPLO VISUAL' : !granted?'Permiso pendiente' : !active?'Sin reproducción' : playing?'Reproduciendo':'En pausa';
    $('#musicStatus').textContent=status;
    $('#musicSource').textContent=preview?'NO ES AUDIO REAL' : (active ? (musicApps[data.package]||'ANDROID AUDIO') : (granted?'ANDROID':'SIN CONECTAR'));
    $('#deviceStatus').textContent = preview ? 'VISTA DE PRUEBA' : (granted?'ANDROID · MÚSICA':'ANDROID · v0.5.3');
    $('.musicPanel').dataset.media=preview?'preview':(!granted?'permission':(!active?'idle':(playing?'playing':'paused')));
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
      case 'gps':activateGps();break;
      case 'navigation':launchNavigation();break;
      case 'zoom-in':case 'zoom-out':zoom=Math.max(.8,Math.min(1.8,zoom+(action==='zoom-in'?.1:-.1)));$('#mapArtwork').style.transform=`scale(${zoom})`;showToast('Zoom ilustrativo: no sustituye navegación real');break;
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
      case 'phone':case 'bluetooth':case 'radio':case 'music':if(window.G2Android)window.G2Android.openTarget(action);else showToast('Acceso pendiente de integración nativa en Android');break;
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
  $$('[data-performance]').forEach(button=>button.addEventListener('click',()=>{
    settings.performance=button.dataset.performance==='light'?'light':'normal';
    applyPerformanceMode();save();
  }));
  $$('.logoChoices button[data-logo]').forEach(button=>button.addEventListener('click',()=>{settings.logo=button.dataset.logo;applySettings();save()}));
  $('#blur').addEventListener('input',(e)=>{settings.blur=Number(e.target.value);document.documentElement.style.setProperty('--wallBlur',`${settings.blur}px`);$('#blurValue').textContent=`${settings.blur} px`;save()});
  $('#opacity').addEventListener('input',(e)=>{settings.opacity=Number(e.target.value);applyWidgetSettings();save()});
  $('#widgetBlur').addEventListener('input',(e)=>{settings.widgetBlur=Number(e.target.value);applyWidgetSettings();save()});
  $('#chooseCar').addEventListener('change',event=>{
    const file=event.target.files?.[0];if(!file)return;
    if(!['image/png','image/webp'].includes(file.type)){showToast('Selecciona un PNG o WebP');return}
    if(file.size>8000000){showToast('Selecciona una imagen menor a 8 MB');return}
    const reader=new FileReader();
    reader.onload=()=>{
      const img=new Image();
      img.onload=()=>{
        const ratio=Math.min(1,900/img.naturalWidth,500/img.naturalHeight);
        const canvas=document.createElement('canvas');
        canvas.width=Math.round(img.naturalWidth*ratio);
        canvas.height=Math.round(img.naturalHeight*ratio);
        canvas.getContext('2d').drawImage(img,0,0,canvas.width,canvas.height);
        settings.personalCar=canvas.toDataURL('image/png');
        photoChange(2);save();showToast('Imagen del Ibiza aplicada sobre fondo transparente');
      };
      img.onerror=()=>showToast('No se pudo abrir la imagen');
      img.src=reader.result;
    };
    reader.readAsDataURL(file);
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
  $('#destination').addEventListener('keydown',(event)=>{if(event.key==='Enter'){event.preventDefault();launchNavigation()}});
  document.addEventListener('keydown',(event)=>{if(event.key==='Escape'){closeScreen('#settingsVeil');closeScreen('#vehicleVeil');window.G2Concept?.close();window.G2Apps?.close();window.G2MusicDetail?.close();window.G2AirPlay?.close()}});
  if(window.G2Android){const tag=$('#deviceStatus');if(tag)tag.textContent='ANDROID · v0.5.5';$('#musicPermission').hidden=false;$('#homeConfig').hidden=false;}
  photoChange(0);updateMapDestinationState();applySettings();updateClock();setInterval(updateClock,20000);
})();
