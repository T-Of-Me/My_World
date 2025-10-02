const socket = io();
const username = '<%= user.username %>';
const avatar = '/images/default-avatar.png';
const room = 'General'; // Default room
let currentRoom = room;

// Emit user connected event
socket.emit('user connected', { username, avatar });

// Listen for messages from the server
socket.on('chat message', (data) => {
  const messagesDiv = document.getElementById('chat-messages');
  let messageContent = `<p><strong>${data.username}:</strong> ${data.text}</p>`;
  if (data.file) {
    messageContent += `<img src="${data.file}" alt="Uploaded image" style="max-width: 200px;">`;
  }
  const messageElement = document.createElement('div');
  messageElement.classList.add('message', data.username === username ? 'sent' : 'received');
  messageElement.innerHTML = `
    <img src="/images/default-avatar.png" alt="${data.username}" class="avatar">
    ${messageContent}
  `;
  messagesDiv.appendChild(messageElement);
  messagesDiv.scrollTop = messagesDiv.scrollHeight;
});

// Message submit
document.getElementById('chat-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const msg = document.getElementById('msg').value;
  socket.emit('chat message', { text: msg }, currentRoom);
  document.getElementById('msg').value = '';
});
