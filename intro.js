'use strict';
/* G² Dash — bienvenida al INICIAR la aplicación. No afirma detectar la ignición. */
(() => {
  const splash=document.getElementById('startup');
  const greeting=document.getElementById('startupTimeGreeting');
  const skip=document.getElementById('startupSkip');
  if(!splash||!greeting||!skip)return;
  let jobs=[];
  let sequence=0;
  const clearJobs=()=>{jobs.forEach(clearTimeout);jobs=[];};
  function greetingByHour(){
    const hour=new Date().getHours();
    if(hour>=6 && hour<12)return 'Buenos Días';
    if(hour>=12 && hour<19)return 'Buenas Tardes';
    return 'Buenas Noches';
  }
  function finish(token){
    if(token!==sequence)return;
    clearJobs();
    splash.dataset.stage='end';
    splash.classList.add('is-exiting');
    jobs.push(setTimeout(()=>{
      if(token!==sequence)return;
      splash.hidden=true;
      splash.setAttribute('aria-hidden','true');
      splash.classList.remove('is-exiting');
    },window.matchMedia?.('(prefers-reduced-motion: reduce)').matches?40:530));
  }
  function play(){
    const token=++sequence;
    clearJobs();
    greeting.textContent=greetingByHour();
    splash.hidden=false;
    splash.removeAttribute('aria-hidden');
    splash.classList.remove('is-exiting');
    splash.dataset.stage='logo';
    jobs.push(setTimeout(()=>{if(token===sequence)splash.dataset.stage='greeting';},1750));
    jobs.push(setTimeout(()=>finish(token),4100));
  }
  skip.addEventListener('click',()=>finish(sequence));
  window.G2ShowIntro=play;
  play();
})();
