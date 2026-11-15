'use strict';
/* G² Dash 0.2 — local demonstrator. No telemetry, OBD or media integration is implied. */
(() => {
  const $ = (q) => document.querySelector(q);
  const $$ = (q) => [...document.querySelectorAll(q)];
  const KEY = 'g2dash.v02.preferences';
  const defaults = { theme:'auto', wall:'dusk', blur:14, opacity:68, accent:'steel', logo:'apex', customWallpaper:null };
  const pictures = [
    {path:'assets/ibiza_frente_original.webp',name:'Frontal — fotografía real'},
    {path:'assets/ibiza_trasera_original.webp',name:'Trasera — fotografía real'},
  ];
  let settings = {...defaults};
  try {settings={...defaults,...JSON.parse(localStorage.getItem(KEY)||'{}')}} catch(_){}
  let currentPhoto=0, isDemoPlaying=false, zoom=1, toastTimer=null;
  function save(){try{localStorage.setItem(KEY,JSON.stringify(settings))}catch(_){showToast('No fue posible guardar los ajustes en este navegador')}}
  function showToast(message){const el=$('#toast');if(!el)return;el.textContent=message;el.classList.add('show');clearTimeout(toastTimer);toastTimer=setTimeout(()=>el.classList.remove('show'),3200)}
  function updateClock(){
    const now = new Date();
    $('#clock').textContent = new Intl.DateTimeFormat('es-MX',{hour:'2-digit',minute:'2-digit',hour12:false}).format(now);
    const localDate = [now.getFullYear(),String(now.getMonth()+1).padStart(2,'0'),String(now.getDate()).padStart(2,'0')].join('-');
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
  function applySettings(){
    document.documentElement.style.setProperty('--accent',accents[settings.accent]||accents.steel);
    document.documentElement.style.setProperty('--wallBlur',`${Number(settings.blur)||0}px`);
    document.documentElement.style.setProperty('--glassOpacity',Math.min(.94,Math.max(.28,(Number(settings.opacity)||68)/100)));
    $('#blur').value=settings.blur;$('#blurValue').textContent=`${settings.blur} px`;
    $('#opacity').value=settings.opacity;$('#opacityValue').textContent=`${settings.opacity}%`;
    $$('[data-accent]').forEach(b=>b.classList.toggle('selected',b.dataset.accent===settings.accent));
    $('[data-wall]').forEach(b=>b.classList.toggle('selected',b.dataset.wall===settings.wall));
    const activeLogo=['apex','pulse','orbit'].includes(settings.logo)?settings.logo:'apex';
    $('#brandLogo').dataset.logo=activeLogo;
    $('.logoChoices button[data-logo]').forEach(b=>{const selected=b.dataset.logo===activeLogo;b.classList.toggle('selected',selected);b.setAttribute('aria-pressed',String(selected));});
    $('#wallpaper').style.backgroundImage=settings.customWallpaper?`url(${JSON.stringify(settings.customWallpaper)})`:walls[settings.wall]||walls.dusk;
    applyTheme(false);
  }
  function photoChange(index){
    currentPhoto=(index+pictures.length)%pictures.length;
    const pic=pictures[currentPhoto];
    $('#vehiclePhoto').src=pic.path;$('#modalCar').src=pic.path;
    $('#carChip').textContent=`Foto ${currentPhoto===0?'frontal':'trasera'} · placa protegida`;
    $('#modalLabel').textContent=pic.name;
    $$('#carDot0, #carDot1').forEach((b,i)=>b.classList.toggle('on',i===currentPhoto));
  }
  function openScreen(selector){$(selector).hidden=false}
  function closeScreen(selector){$(selector).hidden=true}
  function openMap(){
    const term=$('#destination').value.trim();
    const url=term?`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(term)}`:'https://www.google.com/maps/';
    const w=window.open(url,'_blank','noopener,noreferrer');
    if(!w)showToast('Permite ventanas nuevas para abrir Google Maps');
  }
  function activateGps(){
    if(!navigator.geolocation){showToast('GPS no disponible en este navegador');return}
    if(!window.isSecureContext){showToast('Para usar GPS, abre la prueba mediante HTTPS');return}
    $('#speedLabel').textContent='Buscando señal GPS…';
    navigator.geolocation.getCurrentPosition((position)=>{
      const speed=position.coords.speed;
      if(Number.isFinite(speed)&&speed>=0){$('#speed').innerHTML=`${Math.round(speed*3.6)} <small>km/h</small>`;$('#speedLabel').textContent='Velocidad GPS estimada'}
      else{$('#speed').innerHTML='— <small>km/h</small>';$('#speedLabel').textContent='Sin velocidad GPS disponible'}
      showToast('Ubicación obtenida. El mapa sigue siendo ilustrativo.');
    },(err)=>{$('#speedLabel').textContent='Velocidad GPS';showToast(err.code===1?'Permiso de ubicación denegado':'No se pudo obtener ubicación')},{enableHighAccuracy:true,timeout:10000,maximumAge:2000});
  }
  function demoPlay(){
    isDemoPlaying=!isDemoPlaying;
    $('#play .icon use').setAttribute('href',isDemoPlaying?'#i-pause':'#i-play');
    showToast(isDemoPlaying?'Vista demo: no reproduce audio real':'Vista demo: control detenido');
  }
  function handleAction(action){
    switch(action){
      case 'home':closeScreen('#settingsVeil');closeScreen('#vehicleVeil');break;
      case 'open-settings':openScreen('#settingsVeil');break;
      case 'close-settings':closeScreen('#settingsVeil');break;
      case 'vehicle':openScreen('#vehicleVeil');break;
      case 'close-vehicle':closeScreen('#vehicleVeil');break;
      case 'car-front':photoChange(0);break;
      case 'car-rear':photoChange(1);break;
      case 'car-prev':photoChange(currentPhoto-1);break;
      case 'car-next':photoChange(currentPhoto+1);break;
      case 'gps':activateGps();break;
      case 'navigation':openMap();break;
      case 'zoom-in':case 'zoom-out':zoom=Math.max(.8,Math.min(1.8,zoom+(action==='zoom-in'?.1:-.1)));$('#mapArtwork').style.transform=`scale(${zoom})`;showToast('Zoom ilustrativo: no sustituye navegación real');break;
      case 'demo-play':demoPlay();break;
      case 'demo-shuffle':case 'demo-prev':case 'demo-next':case 'demo-repeat':showToast('Control musical de demostración; integración Android pendiente');break;
      case 'reset-settings':settings={...defaults};applySettings();save();showToast('Ajustes restablecidos');break;
      case 'fullscreen':if(document.fullscreenElement){document.exitFullscreen?.()}else{document.documentElement.requestFullscreen?.().catch(()=>showToast('Pantalla completa no disponible'))}break;
      case 'phone':case 'apps':case 'bluetooth':case 'radio':case 'music':showToast('Acceso pendiente de integración nativa en Android');break;
      default:showToast('Función en desarrollo');
    }
  }
  document.addEventListener('click',(event)=>{
    const trigger=event.target.closest('[data-action]');
    if(trigger){event.preventDefault();handleAction(trigger.dataset.action)}
    if(event.target.matches('.screenveil'))event.target.hidden=true;
  });
  $$('[data-theme]').forEach(button=>button.addEventListener('click',()=>{settings.theme=button.dataset.theme;applyTheme();}));
  $$('[data-wall]').forEach(button=>button.addEventListener('click',()=>{settings.wall=button.dataset.wall;settings.customWallpaper=null;applySettings();save()}));
  $('[data-accent]').forEach(button=>button.addEventListener('click',()=>{settings.accent=button.dataset.accent;applySettings();save()}));
  $('.logoChoices button[data-logo]').forEach(button=>button.addEventListener('click',()=>{settings.logo=button.dataset.logo;applySettings();save()}));
  $('#blur').addEventListener('input',(e)=>{settings.blur=Number(e.target.value);document.documentElement.style.setProperty('--wallBlur',`${settings.blur}px`);$('#blurValue').textContent=`${settings.blur} px`;save()});
  $('#opacity').addEventListener('input',(e)=>{settings.opacity=Number(e.target.value);document.documentElement.style.setProperty('--glassOpacity',settings.opacity/100);$('#opacityValue').textContent=`${settings.opacity}%`;save()});
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
  $('#destination').addEventListener('keydown',(event)=>{if(event.key==='Enter'){event.preventDefault();openMap()}});
  document.addEventListener('keydown',(event)=>{if(event.key==='Escape'){closeScreen('#settingsVeil');closeScreen('#vehicleVeil')}});
  photoChange(0);applySettings();updateClock();setInterval(updateClock,20000);
})();
