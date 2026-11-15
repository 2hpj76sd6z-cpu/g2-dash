'use strict';
/* G² Dash Touch Studio v0.5.6: reordenar accesos con dos toques, sin arrastre inseguro al manejar. */
(() => {
  const dock = document.querySelector('.bottomDock');
  const settings = document.querySelector('.settingsBody');
  if (!dock || !settings) return;
  const KEY = 'g2dash.dock.v056';
  const buttons = Array.from(dock.querySelectorAll(':scope > button[data-action]'));
  const original = buttons.map(b => b.dataset.action);
  let editing = false, selected = null;
  const get = () => Array.from(dock.querySelectorAll(':scope > button[data-action]'));
  function save() {
    try { localStorage.setItem(KEY, JSON.stringify(get().map(b => b.dataset.action))); } catch (_) {}
  }
  function restore() {
    try {
      const saved = JSON.parse(localStorage.getItem(KEY) || 'null');
      if (!Array.isArray(saved) || saved.length !== original.length ||
          new Set(saved).size !== original.length || !original.every(x => saved.includes(x))) return;
      saved.forEach(name => {
        const button = buttons.find(b => b.dataset.action === name);
        if (button) dock.appendChild(button);
      });
    } catch (_) {}
  }
  const label = document.createElement('label');
  label.textContent = 'Orden de accesos · edición táctil';
  const panel = document.createElement('div');
  panel.className = 'dockEditControls';
  const edit = document.createElement('button');
  edit.type = 'button';
  edit.textContent = 'Editar accesos';
  edit.setAttribute('aria-pressed', 'false');
  const reset = document.createElement('button');
  reset.type = 'button';
  reset.textContent = 'Orden original';
  const help = document.createElement('p');
  help.className = 'hint dockEditHelp';
  help.setAttribute('role','status');
  help.textContent = 'Pulsa Editar, toca dos iconos para intercambiarlos y termina con Listo.';
  panel.append(edit,reset);
  const anchor = settings.querySelector('.settingActions');
  settings.insertBefore(label,anchor || null);
  settings.insertBefore(panel,anchor || null);
  settings.insertBefore(help,anchor || null);
  function clear() {
    selected = null;
    get().forEach(b => b.classList.remove('dockEditSelected'));
  }
  function setEditing(value) {
    editing = value;
    clear();
    dock.classList.toggle('dockEditing', editing);
    edit.textContent = editing ? 'Listo · guardar orden' : 'Editar accesos';
    edit.setAttribute('aria-pressed',String(editing));
    help.textContent = editing
      ? 'MODO EDICIÓN: toca un icono y después otro para cambiar sus posiciones. No se abrirán aplicaciones.'
      : 'Orden guardado en este dispositivo. Puedes volver a editar cuando quieras.';
    if (!editing) save();
  }
  edit.addEventListener('click',() => setEditing(!editing));
  reset.addEventListener('click', () => {
    original.forEach(name => {
      const button = buttons.find(b => b.dataset.action === name);
      if (button) dock.appendChild(button);
    });
    clear(); save();
    help.textContent = 'Restaurado el orden original de los siete accesos.';
  });
  // Capture evita que app.js ejecute el acceso cuando se está editando.
  dock.addEventListener('click', event => {
    if (!editing) return;
    const button = event.target.closest('button[data-action]');
    if (!button || !dock.contains(button)) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    if (!selected) {
      selected = button;
      selected.classList.add('dockEditSelected');
      help.textContent = 'Ahora toca el segundo icono para intercambiar posiciones.';
      return;
    }
    if (selected === button) { clear(); return; }
    const a = selected, b = button;
    const marker = document.createComment('g2-swap');
    dock.replaceChild(marker,a);
    dock.replaceChild(a,b);
    dock.replaceChild(b,marker);
    clear(); save();
    help.textContent = 'Posiciones intercambiadas. Puedes seguir editando o pulsar Listo.';
  },true);
  restore();
})();
