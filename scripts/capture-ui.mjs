// G² Dash — captura auténtica de la interfaz publicada (sin imágenes generadas).
// npm install && node scripts/capture-ui.mjs
import { chromium } from 'playwright-core';
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

const base = process.env.G2_DASH_URL || 'http://127.0.0.1:8765/';
const root = path.resolve('docs/capturas/actual');
const browserOpts = {
  headless: true,
  args: ['--no-sandbox', '--disable-dev-shm-usage'],
};
if (process.env.G2_BROWSER) browserOpts.executablePath = process.env.G2_BROWSER;
await mkdir(root, { recursive: true });
const browser = await chromium.launch(browserOpts);
const errors = [];
const page = await browser.newPage({
  viewport: { width: 1280, height: 720 },
  deviceScaleFactor: 1,
  locale: 'es-MX',
  timezoneId: 'America/Mexico_City',
  reducedMotion: 'reduce',
  colorScheme: 'dark',
});
page.on('pageerror', err => errors.push(err.message));

const capture = async (name, selector = null) => {
  const file = path.join(root, `${name}.png`);
  if (selector) {
    const element = page.locator(selector).first();
    await element.waitFor({ state: 'visible' });
    await element.screenshot({ path: file, animations: 'disabled' });
  } else {
    await page.screenshot({ path: file, animations: 'disabled' });
  }
  console.log('Captura:', file);
};
try {
  await page.goto(base, { waitUntil: 'networkidle', timeout: 30000 });
  await page.evaluate(() => localStorage.clear());
  await page.reload({ waitUntil: 'networkidle' });
  await page.locator('#vehiclePhoto').waitFor({ state: 'visible' });
  await page.evaluate(() => document.fonts.ready);
  // Capturar las dos pantallas reales ANTES del tablero: nunca una ilustración.
  await page.waitForFunction(() => document.querySelector('#startup')?.dataset.stage === 'logo', {timeout:2500});
  await capture('00-arranque-seat-logo');
  await page.waitForFunction(() => document.querySelector('#startup')?.dataset.stage === 'greeting', {timeout:4500});
  const actualGreeting = await page.locator('#startupTimeGreeting').textContent();
  const question = await page.locator('.startupGreeting .question').textContent();
  if (!['Buenos Días','Buenas Tardes','Buenas Noches'].includes(actualGreeting) ||
      question!=='¿A dónde iremos hoy?') throw new Error('Saludo de encendido incorrecto');
  await capture('00b-arranque-saludo-doble-g');
  await page.waitForFunction(() => document.querySelector('#startup')?.dataset.stage === 'status', {timeout:4500});
  const splashVersion = await page.locator('.startupStatusVersion').textContent();
  const nextStep = await page.locator('.startupStatusNext p strong').textContent();
  const releaseVersion = (await page.title()).match(/v\d+\.\d+\.\d+/)?.[0];
  if(!releaseVersion || splashVersion!==releaseVersion || !nextStep.trim()) {
    throw new Error('La bienvenida no coincide con la versión del tablero o no indica el siguiente paso');
  }
  await capture('00c-arranque-estado-v052');
  await page.locator('#startup').waitFor({state:'hidden',timeout:8000});

  // Revisión geométrica: no ocultar secciones fuera del área útil.
  // No se confunde un screenshot correcto con una prueba de rendimiento del estéreo.
  const qa = [];
  const inspectViewport = async label => {
    const data = await page.evaluate(() => {
      const names=['.topline','.dashboard','.sidebar','.leftStack','.musicPanel','.mapPanel','.vehiclePanel','.bottomDock','.footer'];
      const boxes={};
      for(const selector of names){
        const el=document.querySelector(selector);
        if(!el)continue;
        const b=el.getBoundingClientRect();
        boxes[selector]={left:+b.left.toFixed(1),top:+b.top.toFixed(1),right:+b.right.toFixed(1),bottom:+b.bottom.toFixed(1),width:+b.width.toFixed(1),height:+b.height.toFixed(1)};
      }
      const actualCar=document.querySelector('#vehiclePhoto');
      return {viewport:{width:innerWidth,height:innerHeight},boxes,carLoaded:Boolean(actualCar?.complete&&actualCar.naturalWidth>0),horizontalOverflow:document.documentElement.scrollWidth>innerWidth+1};
    });
    const problems=[];
    if(data.horizontalOverflow)problems.push('Desplazamiento horizontal');
    if(!data.carLoaded)problems.push('La imagen del Ibiza no cargó');
    for(const [name,b] of Object.entries(data.boxes)){
      if(b.width<34 || b.height<(name==='.footer'?5:20))problems.push(name+' es demasiado pequeño');
      if(b.left < -3 || b.top < -3 || b.right>data.viewport.width+3 || b.bottom>data.viewport.height+3){
        problems.push(name+' queda fuera del viewport');
      }
    }
    qa.push({label,...data,problems});
    console.log('QA pantalla',label,problems.length?'OBSERVACIONES: '+problems.join('; '):'sin desbordamientos');
    if(problems.length)throw new Error('Fallo de geometría en '+label+': '+problems.join('; '));
  };
  await inspectViewport('1280×720');

  await capture('01-inicio-1280x720');
  await capture('02-encabezado', '.topline');
  await capture('03-reloj', '.clockPanel');
  await capture('04-musica', '.musicPanel');
  await capture('05-mapa-ilustrativo', '.mapPanel');
  await capture('06-mi-ibiza', '.vehiclePanel');
  await capture('07-iconos-accesos', '.bottomDock');

  // Centro de apps: navegador solo contiene una maqueta marcada como ejemplo.
  await page.locator('.bottomDock [data-action="apps"]').click();
  if(!await page.locator('#appsVeil').isVisible())throw new Error('No se abrió el panel de apps');
  if(await page.locator('.appsCard').count()!==6)throw new Error('Faltan las seis tarjetas de demostración');
  const demoLabel=await page.locator('#appsInfo').textContent();
  if(!demoLabel.includes('EJEMPLO VISUAL'))throw new Error('Falta advertencia de demostración');
  await capture('21-apps-panel-demo-1280x720');
  await page.locator('#appsSearchInput').fill('Música');
  if(await page.locator('.appsCard').count()!==1)throw new Error('El buscador de apps no filtró');
  await capture('22-apps-buscador-1280x720');
  await page.locator('#appsClose').click();

  await page.locator('[data-action="open-settings"]').first().click();
  await capture('08-personalizacion');

  // v0.5.4: compare exact original dashboard with optional low-effects mode.
  await page.locator('[data-action="close-settings"]').click();
  const actualCar=await page.locator('#vehiclePhoto').getAttribute('src');
  const actualWallpaper=await page.locator('#wallpaper').evaluate(el=>el.style.backgroundImage);
  await capture('41-rendimiento-normal-1280x720');

  await page.locator('[data-action="open-settings"]').first().click();
  await page.locator('[data-performance="light"]').click();
  const selected=await page.locator('body').getAttribute('data-performance');
  const backdrop=await page.locator('.dashboard .glass').first().evaluate(el=>getComputedStyle(el).backdropFilter);
  if(selected!=='light'||backdrop!=='none')throw new Error('Modo ligero no desactiva los filtros de vidrio');
  const savedPerformance=await page.evaluate(()=>JSON.parse(localStorage.getItem('g2dash.v02.preferences')||'{}').performance);
  if(savedPerformance!=='light')throw new Error('La preferencia de rendimiento no se guardó');
  await capture('43-ajustes-rendimiento-ligero');
  await page.locator('[data-action="close-settings"]').click();
  if(await page.locator('#vehiclePhoto').getAttribute('src')!==actualCar
     ||await page.locator('#wallpaper').evaluate(el=>el.style.backgroundImage)!==actualWallpaper)
    throw new Error('Modo ligero alteró la fotografía o el fondo');
  await capture('42-rendimiento-ligero-1280x720');
  await page.locator('[data-action="open-settings"]').first().click();
  await page.locator('[data-performance="normal"]').click();
  if(await page.locator('body').getAttribute('data-performance')!=='normal'
     ||await page.locator('.dashboard .glass').first().evaluate(el=>getComputedStyle(el).backdropFilter)==='none')
    throw new Error('No restauró los desenfoques normales');
  await page.locator('[data-theme="day"]').click();
  await page.locator('[data-action="close-settings"]').click();
  await capture('09-modo-dia-1280x720');

  await page.locator('[data-action="open-settings"]').first().click();
  await page.locator('[data-theme="night"]').click();
  await page.locator('[data-action="close-settings"]').click();
  await page.locator('[data-action="vehicle"]').first().click();
  await capture('10-ibiza-detalle');
  await page.locator('[data-action="car-next"]').click();
  await capture('11-ibiza-trasera');

  // Sample metadata only in the evidence capture, explicitly labeled.
  // NEVER pretend this is an actual playing MediaSession.
  await page.locator('[data-action="close-vehicle"]').click();
  await page.evaluate(() => window.G2MusicUpdate({
    granted:true,
    preview:true,
    title:'Pista de prueba',
    artist:'EJEMPLO — NO REPRODUCCIÓN REAL',
    playing:true,
    positionMs:42000,
    durationMs:186000,
    albumArt:''
  }));
  await capture('12-musica-integracion-prueba', '.musicPanel');
  const label=await page.locator('#musicStatus').textContent();
  const source=await page.locator('#musicSource').textContent();
  const disabled=await page.locator('[data-action="demo-shuffle"]').isDisabled();
  const playIcon=await page.locator('#play .icon use').getAttribute('href');
  if(label!=='EJEMPLO VISUAL' || source!=='NO ES AUDIO REAL' || !disabled || playIcon!=='#i-play') {
    throw new Error('La prueba visual de música no tiene los indicadores correctos');
  }
  await capture('13-musica-detallada-ejemplo', '.musicPanel');
  // Reproductor ampliado: es una VISTA DE PRUEBA, no audio de Android.
  await page.locator('[data-action="music-detail"]').click();
  if(!await page.locator('#musicDetailVeil').isVisible() ||
     await page.locator('#musicDetailSource').textContent()!=='EJEMPLO VISUAL' ||
     !await page.locator('#musicSeek').isDisabled() ||
     !await page.locator('#musicDetailPlay').isDisabled()) {
    throw new Error('El panel musical ampliado no identifica correctamente la simulación');
  }
  await capture('27-musica-ampliada-ejemplo-1280x720');
  await page.locator('#musicDetailClose').click();

  // AirPlay es una integración real solamente en Android: comprobar que la
  // web no simule instalar receptor ni reproducir música.
  await page.locator('[data-action="airplay-setup"]').first().click();
  if(!await page.locator('#airplayVeil').isVisible() ||
     !(await page.locator('#airplayStatusTitle').textContent()).includes('VISTA WEB') ||
     !await page.locator('#airplayLaunch').isHidden() ||
     !await page.locator('#airplayInstall').isHidden()){
     throw new Error('El asistente AirPlay no identifica el modo web de demostración');
  }
  await capture('29-airplay-configuracion-1280x720');
  await page.locator('#airplayClose').click();
  // Recorte real, mismo WebP con la máscara revisada.
  await page.locator('[data-action="car-cutout"]').click();
  // La vista recortada ahora se carga bajo demanda, así que esperar su decodificación antes de capturar.
  await page.locator('#vehiclePhoto').evaluate(img=>img.decode());
  await page.locator('#vehiclePhoto').evaluate(img=>{if(img.naturalWidth<200)throw new Error('SVG Ibiza no cargado');});
  await capture('14-ibiza-mascara-corregida', '.vehiclePanel');
  await page.locator('[data-action="vehicle"]').first().click();
  await capture('15-ibiza-mascara-ampliada');
  await page.locator('[data-action="close-vehicle"]').click();

  // Galería conceptual aprobada: capturas reales y QA táctil/teclado.
  await page.locator('.vehiclePanel [data-action="concept-360"]').click();
  await page.locator('#conceptHero').evaluate(img => img.decode());
  if(await page.locator('#conceptThumbs button').count() !== 12) throw new Error('Faltan miniaturas 12 vistas');
  if(await page.locator('#conceptAngle').textContent() !== '30°') throw new Error('Ángulo inicial incorrecto');
  await capture('17-ibiza-conceptual-12-vistas');
  await page.locator('#conceptNext').click();
  if(await page.locator('#conceptAngle').textContent() !== '60°') throw new Error('Ángulo siguiente falló');
  await page.locator('#conceptHero').evaluate(img => img.decode());
  await capture('18-ibiza-conceptual-60-grados');
  await page.locator('#conceptThumbs button').nth(6).click();
  if(await page.locator('#conceptAngle').textContent() !== '180°') throw new Error('Miniatura trasera falló');
  await page.locator('#conceptHero').evaluate(img => img.decode());
  await capture('19-ibiza-conceptual-trasera-180');

  // Navegación bidireccional entre el Ibiza real y el concepto ilustrado.
  await page.locator('#conceptThumbs button').nth(1).click();
  await page.locator('#conceptHero').evaluate(img=>img.decode());
  if(!await page.locator('#conceptRealPreview img').evaluate(img=>img.complete && img.naturalWidth>200))
    throw new Error('La fotografía auténtica del Ibiza no cargó');
  await capture('35-ibiza-conceptual-y-real');
  await page.locator('#conceptRealPreview').click();
  if(!await page.locator('#conceptVeil').isHidden() || !await page.locator('#vehicleVeil').isVisible())
    throw new Error('No abrió el visor de fotos reales');
  await page.locator('#modalCar').evaluate(img=>img.decode());
  const originalSource=await page.locator('#modalCar').getAttribute('src');
  if(!originalSource.includes('ibiza_frente_original.webp'))throw new Error('No se abrió la fotografía frontal real');
  await capture('36-ibiza-fotografia-autentica');
  await page.locator('#vehicleVeil [data-action="concept-360"]').click();
  if(!await page.locator('#vehicleVeil').isHidden() || !await page.locator('#conceptVeil').isVisible())
    throw new Error('No volvió a las 12 vistas sin superposición');
  if(await page.locator('#conceptAngle').textContent()!=='30°')throw new Error('Se perdió el ángulo 30 grados');
  await capture('37-ibiza-retorno-conceptual');

  // v0.5.1: true native 500×314 dimensions vs optional expanded view.
  await page.locator('#conceptThumbs button').nth(1).click();
  await page.locator('#conceptHero').evaluate(img=>img.decode());
  const size=await page.locator('#conceptHero').evaluate(img=>({w:img.naturalWidth,h:img.naturalHeight,display:img.getBoundingClientRect().width}));
  if(size.w!==500 || size.h!==314 || size.display>505)throw new Error('La imagen no respeta 500x314 sin ampliación');
  await capture('24-ibiza-tamano-original');
  await page.locator('#conceptScale').click();
  const enlarged=await page.locator('#conceptHero').evaluate(img=>img.getBoundingClientRect().width);
  if(enlarged<size.display+50 || await page.locator('#conceptScale').getAttribute('aria-pressed')!=='true')
    throw new Error('El botón 1.3x no amplía correctamente');
  await capture('25-ibiza-modo-ampliado');
  await page.locator('#conceptScale').click();
  if(await page.locator('#conceptScale').getAttribute('aria-pressed')!=='false')throw new Error('No se restauró 1:1');
  const b=await page.locator('#conceptStage').boundingBox();
  await page.mouse.move(b.x+b.width*.70,b.y+b.height*.53);
  await page.mouse.down();
  await page.mouse.move(b.x+b.width*.70-180,b.y+b.height*.53,{steps:5});
  await page.mouse.up();
  if(await page.locator('#conceptAngle').textContent()!=='90°')throw new Error('Deslizar dos pasos no funciona');
  await page.locator('#conceptHero').evaluate(img=>img.decode());
  await capture('26-ibiza-deslizamiento-90');
  await page.locator('#conceptAuto').click();
  if(await page.locator('#conceptAuto').getAttribute('aria-pressed')!=='true')throw new Error('No inicia reproducción automática');
  await page.locator('#conceptClose').click();
  if(await page.evaluate(()=>window.G2Concept.isPlaying()))throw new Error('El recorrido no se detuvo al cerrar');
  if(!await page.locator('#conceptVeil').isHidden()) throw new Error('No cierra la galería');

  // La misma interfaz en pantallas menos espaciosas para evitar cortes.
  await page.setViewportSize({width:1024,height:600});
  await page.reload({waitUntil:'networkidle'});
  await page.locator('#startup').waitFor({state:'hidden',timeout:8000});
  await inspectViewport('1024×600');
  await page.locator('.vehiclePanel [data-action="concept-360"]').click();
  await page.locator('#conceptHero').evaluate(img => img.decode());
  await capture('20-ibiza-conceptual-1024x600');
  await page.locator('#conceptClose').click();
  await capture('14-compacto-1024x600');
  await capture('15-ibiza-1024x600','.vehiclePanel');
  await page.setViewportSize({width:960,height:540});
  await page.reload({waitUntil:'networkidle'});
  await page.locator('#startup').waitFor({state:'hidden',timeout:8000});
  await inspectViewport('960×540');
  await capture('16-compacto-960x540');
  await page.locator('.bottomDock [data-action="apps"]').click();
  const cardBox=await page.locator('.appsModal').boundingBox();
  if(!cardBox||cardBox.x < 0||cardBox.y < 0||
      cardBox.x+cardBox.width>960||cardBox.y+cardBox.height>540)
      throw new Error('El panel Apps no cabe en 960x540');
  await capture('23-apps-compacto-960x540');
  await page.locator('#appsClose').click();
  await page.locator('[data-action="music-detail"]').click();
  const musicBox=await page.locator('.musicDetailModal').boundingBox();
  if(!musicBox||musicBox.x<-2||musicBox.y<-2||
     musicBox.x+musicBox.width>962||musicBox.y+musicBox.height>542) {
    throw new Error('Reproductor ampliado se sale de 960x540');
  }
  await capture('28-musica-ampliada-compacta-960x540');
  await page.locator('#musicDetailClose').click();
  await page.locator('[data-action="airplay-setup"]').first().click();
  const apBox=await page.locator('.airplayModal').boundingBox();
  if(!apBox||apBox.x<-2||apBox.y<-2||
     apBox.x+apBox.width>962||apBox.y+apBox.height>542){
     throw new Error('Asistente AirPlay no cabe en 960x540');
  }
  await capture('30-airplay-compacto-960x540');
  await page.locator('#airplayClose').click();
  await writeFile(path.join(root,'qa-layout.json'),JSON.stringify({source_commit:process.env.G2_COMMIT||'local',checked:qa},null,2)+'\n');

  const meta = {
    project: 'G² Dash',
    note: 'Capturas verificables de HTML/CSS/JS reales; mapa y música ilustrativos; velocidad GPS y OBD no simulados.',
    viewport: '1280x720',
    source_commit: process.env.G2_COMMIT || 'local',
    generated_at: new Date().toISOString(),
    shots: [
      '00-arranque-seat-logo.png', '00b-arranque-saludo-doble-g.png',
      '00c-arranque-estado-v052.png', '01-inicio-1280x720.png', '02-encabezado.png', '03-reloj.png',
      '04-musica.png', '05-mapa-ilustrativo.png', '06-mi-ibiza.png',
      '07-iconos-accesos.png', '08-personalizacion.png',
      '09-modo-dia-1280x720.png', '10-ibiza-detalle.png',
      '41-rendimiento-normal-1280x720.png','42-rendimiento-ligero-1280x720.png','43-ajustes-rendimiento-ligero.png',
      '11-ibiza-trasera.png', '12-musica-integracion-prueba.png',
      '13-musica-detallada-ejemplo.png', '14-compacto-1024x600.png',
      '15-ibiza-1024x600.png', '16-compacto-960x540.png',
      '14-ibiza-mascara-corregida.png', '15-ibiza-mascara-ampliada.png',
      '17-ibiza-conceptual-12-vistas.png','18-ibiza-conceptual-60-grados.png',
      '19-ibiza-conceptual-trasera-180.png','20-ibiza-conceptual-1024x600.png',
      '21-apps-panel-demo-1280x720.png','22-apps-buscador-1280x720.png',
      '23-apps-compacto-960x540.png',
      '27-musica-ampliada-ejemplo-1280x720.png','28-musica-ampliada-compacta-960x540.png',
      '35-ibiza-conceptual-y-real.png','36-ibiza-fotografia-autentica.png','37-ibiza-retorno-conceptual.png',
      '29-airplay-configuracion-1280x720.png','30-airplay-compacto-960x540.png',
       '24-ibiza-tamano-original.png','25-ibiza-modo-ampliado.png','26-ibiza-deslizamiento-90.png'
    ]
  };
  await writeFile(path.join(root, 'meta.json'), JSON.stringify(meta, null, 2) + '\n');
  if (errors.length) throw new Error('Errores JS de la interfaz: ' + errors.join(' | '));
} finally {
  await browser.close();
}
