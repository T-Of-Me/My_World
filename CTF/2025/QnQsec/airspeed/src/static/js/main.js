document.addEventListener('DOMContentLoaded',()=>{
  const links=[...document.querySelectorAll('.nav-link')];
  const path=location.pathname;
  links.forEach(l=>{ if(l.getAttribute('href')===path){ l.classList.add('active'); }});
});
