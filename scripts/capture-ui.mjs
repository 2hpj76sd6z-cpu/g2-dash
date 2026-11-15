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
  await page.waitForTimeout(350);

  await capture('01-inicio-1280x720');
  await capture('02-encabezado', '.topline');
  await capture('03-reloj', '.clockPanel');
  await capture('04-musica', '.musicPanel');
  await capture('05-mapa-ilustrativo', '.mapPanel');
  await capture('06-mi-ibiza', '.vehiclePanel');
  await capture('07-iconos-accesos', '.bottomDock');

  await page.locator('[data-action="open-settings"]').first().click();
  await capture('08-personalizacion');
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

  const meta = {
    project: 'G² Dash',
    note: 'Capturas verificables de HTML/CSS/JS reales; mapa y música ilustrativos; velocidad GPS y OBD no simulados.',
    viewport: '1280x720',
    source_commit: process.env.G2_COMMIT || 'local',
    generated_at: new Date().toISOString(),
    shots: [
      '01-inicio-1280x720.png', '02-encabezado.png', '03-reloj.png',
      '04-musica.png', '05-mapa-ilustrativo.png', '06-mi-ibiza.png',
      '07-iconos-accesos.png', '08-personalizacion.png',
      '09-modo-dia-1280x720.png', '10-ibiza-detalle.png',
      '11-ibiza-trasera.png'
    ]
  };
  await writeFile(path.join(root, 'meta.json'), JSON.stringify(meta, null, 2) + '\n');
  if (errors.length) throw new Error('Errores JS de la interfaz: ' + errors.join(' | '));
} finally {
  await browser.close();
}
