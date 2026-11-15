'use strict';
/* AirPlay real = aplicación receptora externa. G² Dash solo consulta su
   disponibilidad y la MediaSession que publique en este mismo Android. */
(() => {
 const veil=document.getElementById('airplayVeil');
 if(!veil)return;
 const $=id=>document.getElementById(id);
 const status=$('airplayStatus'), title=$('airplayStatusTitle'),
   detail=$('airplayStatusDetail'), launch=$('airplayLaunch'),
   install=$('airplayInstall'), check=$('airplayCheck');
 const receiver='io.github.jqssun.airplay';
 const quickAction=document.querySelector('.musicPanel button[data-action="airplay-setup"]');
 let playing=false, installed=false, supported=false;
 function setShortcutState(state,label,help){
   if(!quickAction)return;
   quickAction.dataset.airplayState=state;
   quickAction.setAttribute('aria-label',label);
   quickAction.title=help;
 }
 setShortcutState('unknown','Configurar AirPlay','Consulta el estado del receptor del iPhone por Wi-Fi.');
 function setInfo(state,head,message,shortcutState=state){
   status.dataset.state=state;
   title.textContent=head;
   detail.textContent=message;
   const shortcut={
     playing:['playing','AirPlay · reproduciendo','Android detectó música AirPlay en reproducción.'],
     ready:['ready','AirPlay · receptor listo','El receptor está instalado; todavía no se detecta una sesión del iPhone.'],
     missing:['missing','AirPlay · falta el receptor','Abre para ver los pasos de instalación del receptor.'],
     unavailable:['unavailable','AirPlay · no disponible en demo','Instala el APK de G² Dash para consultar las apps del estéreo.'],
     error:['error','AirPlay · sin respuesta del estéreo','Abre para revisar el estado del receptor.']
   }[shortcutState]||['unknown','Configurar AirPlay','Consulta el estado del receptor del iPhone por Wi-Fi.'];
   setShortcutState(...shortcut);
 }
 function scan(){
   const bridge=window.G2Android;
   if(!bridge || typeof bridge.getAirPlaySupport!=='function'){
     supported=false;installed=false;
     launch.hidden=true;install.hidden=true;check.disabled=true;
     setInfo('unsupported','VISTA WEB · SIN RECEPTOR','Esta demostración no tiene acceso a las aplicaciones del estéreo. Instala el APK Android para configurar AirPlay.','unavailable');
     return;
   }
   check.disabled=false;
   try{
     const data=JSON.parse(bridge.getAirPlaySupport());
     supported=data.supported===true;
     installed=data.installed===true;
     const version=String(data.androidVersion||'?');
     launch.hidden=!supported||!installed;
     install.hidden=!supported||installed;
     check.hidden=false;
     if(!supported){
       setInfo('unsupported','Android '+version+' · no compatible',
         'Este receptor requiere Android 7.0+. G² Dash conserva música local y Bluetooth como alternativas.','unavailable');
     }else if(playing){
       setInfo('playing','AirPlay · música detectada',
         'Android publicó una sesión AirPlay. Portada y controles aparecen solo si el receptor comparte esos datos.');
     }else if(installed){
       setInfo('ready','Receptor AirPlay instalado · Android '+version,
         'Toca Abrir receptor; después elige su nombre desde la salida AirPlay del iPhone.');
     }else{
       setInfo('unsupported','Falta instalar el receptor AirPlay',
         'Instala AirPlay Receiver & Server (jqssun) desde su página oficial. Requiere Android 7.0+.','missing');
     }
   }catch(_){
     supported=false;installed=false;launch.hidden=true;install.hidden=true;
     setInfo('unsupported','No se pudo consultar Android','Vuelve a abrir G² Dash o actualiza el APK.','error');
   }
 }
 function updateMedia(data){
   if(!data||data.preview)return;
   const pkg=data.package;
   const wasPlaying=playing;
   playing=data.granted===true&&pkg===receiver&&data.playing===true;
   if(playing){
     setShortcutState('playing','AirPlay · reproduciendo','Android detectó música AirPlay en reproducción.');
   }else if(!veil.hidden||wasPlaying){
     scan();
   }
 }
 function open(){veil.hidden=false;scan();$('airplayClose').focus()}
 function close(){veil.hidden=true}
 $('airplayClose').addEventListener('click',close);
 veil.addEventListener('click',e=>{if(e.target===veil)close()});
 document.addEventListener('keydown',e=>{if(e.key==='Escape'&&!veil.hidden){e.stopPropagation();close()}});
 check.addEventListener('click',scan);
 launch.addEventListener('click',()=>{
   if(supported&&installed&&window.G2Android?.openAirPlayReceiver){
     window.G2Android.openAirPlayReceiver();
   }
 });
 install.addEventListener('click',()=>{
   if(supported&&!installed&&window.G2Android?.openAirPlayInstall){
     window.G2Android.openAirPlayInstall();
   }
 });
 window.G2AirPlay={open,close,scan,updateMedia};
})();
