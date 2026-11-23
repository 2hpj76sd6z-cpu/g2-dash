// Read the actual app WebView over the emulator's local ADB forward.
const targets = await (await fetch('http://127.0.0.1:9222/json')).json();
console.log('Páginas del emulador', targets.map(t => {try{return new URL(t.url).origin}catch{return ''}}));
const dashboard = targets.find(t => t.url.includes('appassets.androidplatform.net/assets/www/index.html'));
if (!dashboard) throw Error('No se cargÃƒÂ³ la pÃƒÂ¡gina local del launcher');
const ws = new WebSocket(dashboard.webSocketDebuggerUrl);
await new Promise((resolve,reject)=>{ws.onopen=resolve;ws.onerror=reject;});
const expression = `(() => {
 const get = s => document.querySelector(s);
 const rect = s => get(s).getBoundingClientRect();
 const clock=rect('.clockPanel'), date=rect('.calendarRow'), time=rect('.clockDisplay'), map=rect('#liveMapSlot');
 return {version:navigator.userAgent,screen:[innerWidth,innerHeight], ibiza:get('#vehiclePhoto').naturalWidth,
   phone:get('#speedLabel').textContent,speed:get('#speed').textContent,settings:document.querySelectorAll('[data-action=open-settings]').length,metrics:document.querySelectorAll('.vehicleStats>div').length,mapWidth:map.width,mapHeight:map.height,live:Boolean(window.G2LiveMap),splash:get('#startup').hidden,
   centered:date.top>=time.bottom-1 && date.bottom<=clock.bottom+1 && time.top>=clock.top-1,
   layoutButtons:document.querySelectorAll('.layoutEditor button').length};
})()`;
const result = new Promise((resolve,reject)=>{
 const timeout=setTimeout(()=>reject(Error('Tiempo de espera de la interfaz')),10000);
 ws.onmessage=e=>{const value=JSON.parse(e.data);if(value.id===1){clearTimeout(timeout);resolve(value.result);}};
});
ws.send(JSON.stringify({id:1,method:'Runtime.evaluate',params:{expression,returnByValue:true}}));
try {
 const response=await result;
 const data=response?.result?.value;
 console.log(data||response);
 if(data?.phone!=='GPS del celular' || !data.speed.includes('18') || data.settings!==1 || data.metrics!==1 || !data?.ibiza || !data.splash || !data.live || !data.centered || data.layoutButtons<5 ||
    data.mapWidth<50 || data.mapHeight<50)throw Error('FallÃƒÂ³ el arranque o la geometrÃƒÂ­a del tablero en Android');
} finally {ws.close();}

for (const t of targets.filter(t => t !== dashboard && /^https:/.test(t.url))) {
  const remote=new WebSocket(t.webSocketDebuggerUrl);
  await new Promise((resolve,reject)=>{remote.onopen=resolve;remote.onerror=reject;});
  try {
    const report=new Promise((resolve,reject)=>{
      const timer=setTimeout(()=>reject(Error('Tiempo de espera del mapa web')),5000);
      remote.onmessage=e=>{const m=JSON.parse(e.data);if(m.id===2){clearTimeout(timer);resolve(m.result);}};
    });
    remote.send(JSON.stringify({id:2,method:'Runtime.evaluate',params:{returnByValue:true,
      expression:"({state:document.readyState,canvases:document.querySelectorAll('canvas').length,images:document.images.length,forms:document.forms.length,characters:document.body?.innerText.length,title:document.title.slice(0,100)})"}}));
    console.log('Estado de la página pública de Maps', (await report)?.result?.value);
  } finally {remote.close();}
}
