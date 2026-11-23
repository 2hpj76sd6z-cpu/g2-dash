import { execFileSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';
import { setTimeout as delay } from 'node:timers/promises';

const targets = () => fetch('http://127.0.0.1:9222/json').then(r=>r.json());
const dashboard = (await targets()).find(t=>t.url.includes('appassets.androidplatform.net'));
const socket = new WebSocket(dashboard.webSocketDebuggerUrl);
await new Promise((ok,fail)=>{socket.onopen=ok; socket.onerror=fail;});
let id=0;
function evaluate(expression) {
    return new Promise((resolve,reject)=>{
        const expected=++id;
        const timer=setTimeout(()=>reject(Error(expression)),6000);
        socket.onmessage=e=>{
            const r=JSON.parse(e.data);
            if(r.id!==expected)return;
            clearTimeout(timer);
            if(r.result?.exceptionDetails)reject(Error(JSON.stringify(r.result.exceptionDetails)));
            else resolve(r.result?.result?.value);
        };
        socket.send(JSON.stringify({id:expected,method:'Runtime.evaluate',params:{expression,returnByValue:true,awaitPromise:true}}));
    });
}
async function verify(expression) {
    for(let i=0;i<35;i++){if(await evaluate(expression))return;await delay(350);}
    throw Error(expression);
}
function capture(filename){
    const activity=execFileSync('adb',['shell','dumpsys','activity','activities'],{encoding:'utf8'});
    if(!activity.split('\n').some(line=>line.includes('mResumedActivity')&&line.includes('com.dobleg.g2dash.prueba24')))throw Error('Dash perdió el primer plano');
    writeFileSync(filename,execFileSync('adb',['exec-out','screencap','-p'],{maxBuffer:6e6}));
}
const click=id=>evaluate("document.getElementById("+JSON.stringify(id)+").click()");
try {
    await verify("document.getElementById('albumArt').naturalWidth>0 && document.getElementById('mapStateTitle').textContent.includes('Gira a la derecha')");
    capture('tablero-compartido.png');
    const point=await evaluate("(()=>{const r=document.getElementById('miniAlbum').getBoundingClientRect();return [Math.round(r.x+r.width/2),Math.round(r.y+r.height/2)];})()");
    execFileSync('adb',['shell','input','tap',...point.map(String)]);
    await verify("document.getElementById('musicDetailVeil').classList.contains('expanded') && document.getElementById('musicDetailCoverImage').naturalWidth>0");
    await verify("['musicDetailPlay','musicDetailCover','musicDetailClose'].every(id=>{const target=document.getElementById(id);const r=target.getBoundingClientRect();const hit=document.elementFromPoint(r.left+r.width/2,r.top+r.height/2);return r.width>25 && r.height>25 && r.left>=0 && r.top>=0 && r.bottom<=innerHeight+1 && r.right<=innerWidth+1 && target.contains(hit);})");
    await verify("document.getElementById('musicDetailCover').getBoundingClientRect().height >= innerHeight*0.32");

    await click('musicDetailPlay');
    await verify("document.getElementById('musicDetailMode').textContent==='EN PAUSA'");
    await evaluate("const slider=document.getElementById('musicSeek');slider.value='77000';slider.dispatchEvent(new Event('change'));");
    await verify("document.getElementById('musicDetailCurrent').textContent==='01:17'");
    // Read the actual dashboard WebView compositor after seeking has settled.
    await evaluate("new Promise(ok=>requestAnimationFrame(()=>requestAnimationFrame(ok)))");
    const photo=await new Promise((resolve,reject)=>{
        const expected=++id;
        const timer=setTimeout(()=>reject(Error('Captura del reproductor')),8000);
        socket.onmessage=e=>{
            const result=JSON.parse(e.data);
            if(result.id!==expected)return;
            clearTimeout(timer);
            if(!result.result?.data)reject(Error('Captura vacía'));else resolve(result.result.data);
        };
        socket.send(JSON.stringify({id:expected,method:'Page.captureScreenshot',params:{format:'png',fromSurface:true}}));
    });
    writeFileSync('reproductor-compartido.png',Buffer.from(photo,'base64'));
    await click('musicDetailNext');
    await verify("document.getElementById('musicDetailTitle').textContent==='Segunda pista' && !document.querySelector('.mapPanel').classList.contains('hasTurn')");
    await verify("document.getElementById('musicDetailCoverImage').hidden && document.getElementById('albumArt').hidden");
    await click('musicDetailPrev');
    await verify("document.getElementById('musicDetailTitle').textContent==='Primera pista'");
    await click('musicDetailPlay');
    await verify("document.getElementById('musicDetailMode').textContent==='REPRODUCIENDO'");
    await click('musicDetailClose');
    await evaluate("document.getElementById('destination').value='Chapultepec'; document.querySelector('.dashRoute').click()");
    let routed=false;
    for(let i=0;i<40;i++){
        routed=(await targets()).some(t=>t.url.includes('/maps/dir/') && !t.url.includes('appassets'));
        if(routed)break;
        await delay(350);
    }
    if(!routed)throw Error('No se abrió la ruta dentro del WebView de Dash');
    await delay(9000);
    capture('ruta-en-dash.png');
    await evaluate("document.querySelector('.dashRoute').click()");
    await verify("document.querySelector('.dashRoute').textContent==='Trazar ruta'");
    // Remove listener access: selected receiver's public MediaBrowser must still expose controls.
    execFileSync('adb',['shell','cmd','notification','disallow_listener','com.dobleg.g2dash.prueba24/com.dobleg.g2dash.G2MediaListener']);
    await verify("document.getElementById('albumArt').hidden && document.getElementById('songName').textContent==='Primera pista'");
    await click('play');
    await verify("document.querySelector('.musicPanel').dataset.media==='paused'");
    execFileSync('adb',['shell','am','force-stop','com.dobleg.testreceiver']);
    execFileSync('adb',['shell','am','broadcast','-a','com.syu.bt.music.id3','-p','com.dobleg.g2dash.prueba24','--es','title','Camino','--es','artist','Prueba']);
    await verify("document.getElementById('songName').textContent==='Camino' && document.getElementById('songSource').textContent==='BLUETOOTH'");
    await verify("document.getElementById('albumArt').hidden && !document.getElementById('albumArt').hasAttribute('src') && !document.getElementById('musicDetailCoverImage').hasAttribute('src')");
    console.log('Portada, instrucciones, pausa, avance, pistas y ruta dentro de Dash verificados.');
} finally {socket.close();}
