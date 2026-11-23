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
        socket.send(JSON.stringify({id:expected,method:'Runtime.evaluate',params:{expression,returnByValue:true}}));
    });
}
async function verify(expression) {
    for(let i=0;i<35;i++){if(await evaluate(expression))return;await delay(350);}
    throw Error(expression);
}
function capture(filename){
    const activity=execFileSync('adb',['shell','dumpsys','activity','activities'],{encoding:'utf8'});
    if(!activity.split('\n').some(line=>line.includes('mResumedActivity')&&line.includes('com.dobleg.g2dash.prueba22')))throw Error('Dash perdió el primer plano');
    writeFileSync(filename,execFileSync('adb',['exec-out','screencap','-p'],{maxBuffer:6e6}));
}
const click=id=>evaluate("document.getElementById("+JSON.stringify(id)+").click()");
try {
    await verify("document.getElementById('albumArt').naturalWidth>0 && document.getElementById('mapStateTitle').textContent.includes('Gira a la derecha')");
    capture('tablero-compartido.png');
    await click('miniAlbum');
    await verify("document.getElementById('musicDetailVeil').classList.contains('expanded') && document.getElementById('musicDetailCoverImage').naturalWidth>0");
    capture('reproductor-compartido.png');
    await click('musicDetailPlay');
    await verify("document.getElementById('musicDetailMode').textContent==='EN PAUSA'");
    await evaluate("const slider=document.getElementById('musicSeek');slider.value='77000';slider.dispatchEvent(new Event('change'));");
    await verify("document.getElementById('musicDetailCurrent').textContent==='01:17'");
    await click('musicDetailNext');
    await verify("document.getElementById('musicDetailTitle').textContent==='Segunda pista' && !document.querySelector('.mapPanel').classList.contains('hasTurn')");
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
    await evaluate("document.querySelector('.dashRoute').click()");
    await verify("document.querySelector('.dashRoute').textContent==='Trazar ruta'");
    console.log('Portada, instrucciones, pausa, avance, pistas y ruta dentro de Dash verificados.');
} finally {socket.close();}
