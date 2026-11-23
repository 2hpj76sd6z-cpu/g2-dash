// Read the actual app WebView over the emulator's local ADB forward.
const targets = await (await fetch('http://127.0.0.1:9222/json')).json();
const dashboard = targets.find(t => t.url.includes('appassets.androidplatform.net/assets/www/index.html'));
if (!dashboard) throw Error('No se cargÃ³ la pÃ¡gina local del launcher');
const ws = new WebSocket(dashboard.webSocketDebuggerUrl);
await new Promise((resolve,reject)=>{ws.onopen=resolve;ws.onerror=reject;});
const expression = `(() => {
 const get = s => document.querySelector(s);
 const rect = s => get(s).getBoundingClientRect();
 const clock=rect('.clockPanel'), date=rect('.calendarRow'), time=rect('.clockDisplay'), map=rect('#liveMapSlot');
 return {version:navigator.userAgent,screen:[innerWidth,innerHeight], ibiza:get('#vehiclePhoto').naturalWidth,
   mapWidth:map.width,mapHeight:map.height,live:Boolean(window.G2LiveMap),splash:get('#startup').hidden,
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
 if(!data?.ibiza || !data.splash || !data.live || !data.centered || data.layoutButtons<5 ||
    data.mapWidth<50 || data.mapHeight<50)throw Error('FallÃ³ el arranque o la geometrÃ­a del tablero en Android');
} finally {ws.close();}
