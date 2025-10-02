const socket = io({
    transports: ['websocket'],
    upgrade: false
});
console.log('Socket connected on admin page');

const chatRoomSelect = document.getElementById('chat-room-select');
const joinRoomBtn = document.getElementById('join-room-btn');
const leaveRoomBtn = document.getElementById('leave-room-btn');
const adminChatMessages = document.getElementById('admin-chat-messages');
const adminChatForm = document.getElementById('admin-chat-form');
const adminMessageInput = document.getElementById('admin-message-input');

let currentRoom = null;

joinRoomBtn.addEventListener('click', () => {
    const selectedRoom = chatRoomSelect.value;
    if (selectedRoom) {
      socket.emit('admin join', selectedRoom);
      currentRoom = selectedRoom;
      console.log('Admin attempting to join room:', selectedRoom);
      adminChatMessages.innerHTML = ''; // Clear previous messages
    }
  });

leaveRoomBtn.addEventListener('click', () => {
    if (currentRoom) {
        socket.emit('admin leave', currentRoom);
        currentRoom = null;
        console.log('Admin left the room');
        adminChatMessages.innerHTML = '';
    }
});

adminChatForm.addEventListener('submit', (e) => {
    e.preventDefault();
    const message = adminMessageInput.value.trim();
    if (message && currentRoom) {
        socket.emit('chat message', { text: message }, currentRoom);
        adminMessageInput.value = '';
    }
});

socket.on('chat message', (data) => {
    console.log('Received chat message:', data);
    if (currentRoom === data.room) {
      const messageElement = document.createElement('div');
      messageElement.innerHTML = `
        <div class="message ${data.isAdmin ? 'admin-message' : ''}">
          <img src="/images/default-avatar.png" alt="${data.username}" class="avatar">
          <div class="message-content">
            <p><strong>${data.username}${data.isAdmin ? ' (Admin)' : ''}:</strong> ${data.text}</p>
            <span class="timestamp">${new Date(data.timestamp).toLocaleString()}</span>
          </div>
        </div>
      `;
      adminChatMessages.appendChild(messageElement);
      adminChatMessages.scrollTop = adminChatMessages.scrollHeight;
    }
});

socket.on('user joined', (username, room) => {
    if (currentRoom === room) {
        const messageElement = document.createElement('div');
        messageElement.innerHTML = `<em>${username} has joined the room</em>`;
        adminChatMessages.appendChild(messageElement);
    }
});

socket.on('user left', (username, room) => {
    if (currentRoom === room) {
        const messageElement = document.createElement('div');
        messageElement.innerHTML = `<em>${username} has left the room</em>`;
        adminChatMessages.appendChild(messageElement);
    }
});