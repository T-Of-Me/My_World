// Make markdown possible for students to be descriptive 
const escapeQuotes = (content) => {
  return content
    .replaceAll(`"`, '&quot;')
    .replaceAll(`'`, '&#39;')
}

const escapeHtml = (content) => {
  return content
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;');
}

const createImg = (match, altText, src) =>{
  return `<img alt="${escapeQuotes(altText)}" src="${escapeQuotes(src)}"></img>`
}

const createLink = (match, href, text) =>{
  return `<a href="${escapeQuotes(href)}">${escapeHtml(text)}</a>`
}

const referPage = (match, src) =>{
  return `<iframe src="${escapeQuotes(src)}"></iframe>`
}

const strong = (match, strong) => {
  return `<strong>${escapeHtml(strong)}</strong>`;
}

const markdown = (content) => {
  // Prevent XSS
  content = escapeHtml(content);
  return content
    .replace(/!\[([^]*?)\]\(([^]*?)\)/g, createImg)
    .replace(/&\[([^]*?)\]\(([^]*?)\)/g, referPage)
    .replace(/\[(.*?)\]\(([^]*?)\)/g, createLink)
    .replace(/\*\*(.*?)\*\*/g, strong)
    .replace(/  $/mg, `<br>`);
}


// Get and add complain
const urlParams = new URLSearchParams(window.location.search);
const student = urlParams.get('student');

if (student) {
    document.getElementById('student').textContent = student;
} else {
    document.getElementById('student').textContent = 'No student found in URL.';
}

const complain = urlParams.get('complain');
if (complain) {
    document.getElementById('complain').innerHTML = markdown(complain);
} else {
    document.getElementById('complain').textContent = 'No complaint found in URL.';
}
