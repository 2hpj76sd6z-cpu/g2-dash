'use strict';
/* Música ampliada: representa exclusivamente la MediaSession local que publica Android.
   No inicia reproducción web ni escucha metadatos de un teléfono por Wi-Fi. */
(() => {
 const veil=document.getElementById('musicDetailVeil');
 if(!veil)return;
 const $=id=>document.getElementById(id);
 const title=$('musicDetailTitle'), artist=$('musicDetailArtist'), album=$('musicDetailAlbum');
 const source=$('musicDetailSource'), mode=$('musicDetailMode');
 const coverFrame=$('musicDetailCover'), cover=$('musicDetailCoverImage'), seek=$('musicSeek');
 const miniAlbum=$('miniAlbum');
 const expand=$('musicDetailExpand');
 const current=$('musicDetailCurrent'), total=$('musicDetailTotal');
 const status=$('musicDetailHint'), action=$('musicDetailPlay');
 const perm=$('musicDetailPermission'), prev=$('musicDetailPrev'), next=$('musicDetailNext');
 let last=null, canSeek=false, duration=0;
 function mmss(ms){
   const seconds=Math.floor(Math.max(0,ms||0)/1000);
   return String(Math.floor(seconds/60)).padStart(2,'0')+':'+String(seconds%60).padStart(2,'0');
 }
 function update(data){
   if(!data || typeof data!=='object')return;
   last=data;
   const preview=data.preview===true;
   const granted=data.granted===true;
   const active=granted&&typeof data.title==='string'&&
     !['','Sin música activa','Conecta tu música'].includes(data.title);
   const playing=active&&!preview&&data.playing===true;
   const native=Boolean(window.G2Android);
   duration=(active||preview)&&Number.isFinite(+data.durationMs)?Math.min(24*60*60*1000,Math.max(0,+data.durationMs)):0;
   const pos=Math.min(duration,Math.max(0,Number(data.positionMs)||0));
   const appNames={'com.syu.carlink':'CARLINK 2.0','io.github.jqssun.airplay':'AIRPLAY · IPHONE','com.spotify.music':'SPOTIFY','com.google.android.apps.youtube.music':'YOUTUBE MUSIC',
     'com.apple.android.music':'APPLE MUSIC','org.videolan.vlc':'VLC','com.maxmpz.audioplayer':'POWERAMP',
     'com.aspiro.tidal':'TIDAL','deezer.android.app':'DEEZER',
     'com.amazon.mp3':'AMAZON MUSIC','com.samsung.android.app.music':'SAMSUNG MUSIC',
     'com.sec.android.app.music':'SAMSUNG MUSIC','com.pandora.android':'PANDORA',
     'com.soundcloud.android':'SOUNDCLOUD'};
   const sourceName=preview?'EJEMPLO VISUAL':active?(data.carLink?'CARLINK 2.0':appNames[data.package]||'ANDROID AUDIO'):'MÚSICA LOCAL';
   title.textContent=preview?'Pista de prueba':active?String(data.title||'Sin título').slice(0,160):granted?'Sin música activa':'Conecta tu música';
   artist.textContent=preview?'EJEMPLO · NO ES AUDIO REAL':active?String(data.artist||'Artista desconocido').slice(0,160):granted?'Abre el reproductor en este Android':'Autoriza el acceso a música en Android';
   album.textContent=preview?'ÁLBUM DE PRUEBA':active?String(data.album||'Álbum no disponible').slice(0,160):'ÁLBUM NO DISPONIBLE';
   source.textContent=sourceName;
   source.dataset.source=String(data.package||'').toLowerCase();
   mode.textContent=preview?'SIN REPRODUCCIÓN':playing?'REPRODUCIENDO':active?'EN PAUSA':granted?'SIN SESIÓN':'SIN PERMISO';
   const validCover=active&&!preview&&typeof data.albumArt==='string'&&
     data.albumArt.startsWith('data:image/jpeg;base64,')&&data.albumArt.length<400000;
   if(validCover){if(cover.src!==data.albumArt)cover.src=data.albumArt;cover.hidden=false}
   else{cover.hidden=true;cover.removeAttribute('src')}
   const canPlay=active&&native&&!preview&&data.canPlay!==false;
   prev.disabled=!active||!native||preview||data.canPrevious===false;
   next.disabled=!active||!native||preview||data.canNext===false;
   action.disabled=!canPlay;
   action.textContent=playing?'Ⅱ':'▶';
   action.setAttribute('aria-label',playing?'Pausar':'Reproducir');
   canSeek=active&&native&&!preview&&data.canSeek===true&&duration>0;
   seek.disabled=!canSeek;
   seek.max=String(Math.max(1,Math.floor(duration)));
   if(!seek.matches(':active'))seek.value=String(Math.floor(pos));
   seek.style.setProperty('--seek-progress',duration?((pos/duration)*100).toFixed(2)+'%':'0%');
   current.textContent=mmss(pos);total.textContent=mmss(duration);
   perm.hidden=!native||granted;
   status.textContent=preview?'Vista de integración: no se reproduce sonido real':
     !native?'El reproductor real se conecta desde el APK Android':
     !granted?'Autoriza el acceso multimedia desde este estéreo':
     !active?'Abre una app de música en el estéreo':
     !canSeek?'La app de música no permite cambiar el punto de reproducción':
     'Controlando el reproductor activo de este Android';
 }
 function setExpanded(expanded){
   veil.classList.toggle('expanded',Boolean(expanded));
   expand.setAttribute('aria-expanded',String(Boolean(expanded)));
   expand.setAttribute('aria-label',expanded?'Restaurar tamaño del reproductor':'Expandir reproductor a pantalla completa');
   expand.title=expanded?'Restaurar tamaño':'Pantalla completa';
 }
 function open(expanded=false){veil.hidden=false;setExpanded(expanded);update(last||{granted:false});$('musicDetailClose').focus()}
 function close(){setExpanded(false);veil.hidden=true}
 function ctl(k){
   if(!window.G2Android||!last||last.preview===true)return;
   try{window.G2Android.musicControl(k)}catch(_){status.textContent='No se pudo enviar el control al reproductor'}
 }
 $('musicDetailClose').addEventListener('click',close);
 expand.addEventListener('click',()=>setExpanded(!veil.classList.contains('expanded')));
 coverFrame.addEventListener('click',()=>setExpanded(!veil.classList.contains('expanded')));
 coverFrame.setAttribute('role','button');
 coverFrame.setAttribute('tabindex','0');
 coverFrame.addEventListener('keydown',e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();setExpanded(!veil.classList.contains('expanded'))}});
 if(miniAlbum){
   miniAlbum.addEventListener('click',()=>open(true));
   miniAlbum.addEventListener('keydown',e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();open(true)}});
 }
 veil.addEventListener('click',e=>{if(e.target===veil)close()});
 document.addEventListener('keydown',e=>{if(e.key==='Escape'&&!veil.hidden){e.stopPropagation();close()}});
 prev.addEventListener('click',()=>ctl('previous'));
 next.addEventListener('click',()=>ctl('next'));
 action.addEventListener('click',()=>ctl('toggle'));
 perm.addEventListener('click',()=>{if(window.G2Android)window.G2Android.openMusicAccess()});
 seek.addEventListener('input',()=>{
   if(!canSeek)return;
   current.textContent=mmss(+seek.value);
   seek.style.setProperty('--seek-progress',duration?((+seek.value/duration)*100).toFixed(2)+'%':'0%');
 });
 seek.addEventListener('change',()=>{
   if(!canSeek||!window.G2Android?.musicSeek)return;
   const ms=Math.round(Math.min(duration,Math.max(0,+seek.value)));
   try{window.G2Android.musicSeek(ms)}catch(_){status.textContent='No se pudo adelantar la canción'}
 });
 window.G2MusicDetail={open,close,update};
})();
