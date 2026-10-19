/**
 * Room Inventory Controller
 */

let allRooms = [];
let currentRoomViewMode = 'grid'; // 'grid' or 'table'

async function renderRooms() {
  const container = document.getElementById('main-content');
  container.innerHTML = `
    <div class="view-header">
      <div class="view-title">
        <h2>Room Inventory Management</h2>
        <p>Live inventory, rate tiers, room statuses, and availability</p>
      </div>
      <div class="view-actions">
        <button class="btn btn-secondary btn-sm" onclick="toggleRoomViewMode()">
          <i class="fa-solid fa-list"></i> Switch View
        </button>
        <button class="btn btn-primary" onclick="openAddRoomModal()">
          <i class="fa-solid fa-plus"></i> Add New Room
        </button>
      </div>
    </div>

    <!-- Filters -->
    <div class="filter-bar">
      <div class="search-input">
        <input type="text" id="room-search" class="form-control" placeholder="Search room number..." oninput="filterRooms()">
      </div>
      <select id="room-filter-status" class="form-control" style="max-width: 180px;" onchange="filterRooms()">
        <option value="ALL">All Statuses</option>
        <option value="AVAILABLE">AVAILABLE</option>
        <option value="OCCUPIED">OCCUPIED</option>
        <option value="RESERVED">RESERVED</option>
        <option value="MAINTENANCE">MAINTENANCE</option>
      </select>
      <select id="room-filter-type" class="form-control" style="max-width: 180px;" onchange="filterRooms()">
        <option value="ALL">All Types</option>
        <option value="Single">Single</option>
        <option value="Double">Double</option>
        <option value="Deluxe">Deluxe</option>
        <option value="Suite">Suite</option>
      </select>
    </div>

    <div id="rooms-container">
      <div class="loading-spinner"><i class="fa-solid fa-spinner fa-spin"></i> Loading room inventory...</div>
    </div>
  `;

  await loadRooms();
}

async function loadRooms() {
  try {
    const data = await API.get('/rooms');
    allRooms = data.rooms || [];
    filterRooms();
  } catch (err) {
    document.getElementById('rooms-container').innerHTML = `
      <div class="alert alert-danger">Failed to load rooms: ${err.message}</div>
    `;
  }
}

function filterRooms() {
  const search = document.getElementById('room-search')?.value.toLowerCase() || '';
  const status = document.getElementById('room-filter-status')?.value || 'ALL';
  const type = document.getElementById('room-filter-type')?.value || 'ALL';

  const filtered = allRooms.filter(r => {
    const matchSearch = r.room_number.toLowerCase().includes(search);
    const matchStatus = status === 'ALL' || r.status === status;
    const matchType = type === 'ALL' || r.room_type === type;
    return matchSearch && matchStatus && matchType;
  });

  renderRoomList(filtered);
}

function toggleRoomViewMode() {
  currentRoomViewMode = currentRoomViewMode === 'grid' ? 'table' : 'grid';
  filterRooms();
}

function renderRoomList(rooms) {
  const container = document.getElementById('rooms-container');
  if (!container) return;

  if (rooms.length === 0) {
    container.innerHTML = `
      <div class="card" style="text-align: center; padding: 40px;">
        <i class="fa-solid fa-bed" style="font-size: 32px; color: var(--text-dim); margin-bottom: 12px;"></i>
        <p style="color: var(--text-muted);">No rooms matching the selected criteria.</p>
      </div>
    `;
    return;
  }

  if (currentRoomViewMode === 'grid') {
    container.innerHTML = `
      <div class="room-grid">
        ${rooms.map(r => `
          <div class="room-card">
            <div>
              <div class="room-card-header">
                <div>
                  <span class="room-number">Room ${r.room_number}</span>
                  <div class="room-type">${r.room_type} (Floor ${r.floor})</div>
                </div>
                <span class="badge badge-${r.status.toLowerCase()}">${r.status}</span>
              </div>
              <div class="room-price">${formatCurrency(r.price_per_night)} <small style="font-size: 11px; color: var(--text-muted);">/ night</small></div>
              <p style="font-size: 12px; color: var(--text-dim); min-height: 36px;">${r.description || 'Standard hotel room'}</p>
            </div>
            <div class="room-actions">
              <button class="btn btn-secondary btn-sm" onclick="openEditRoomModal(${r.room_id})">
                <i class="fa-solid fa-pen-to-square"></i> Edit
              </button>
              <button class="btn btn-outline-danger btn-sm" onclick="deleteRoom(${r.room_id})">
                <i class="fa-solid fa-trash"></i>
              </button>
            </div>
          </div>
        `).join('')}
      </div>
    `;
  } else {
    container.innerHTML = `
      <div class="card">
        <div class="table-responsive">
          <table class="table">
            <thead>
              <tr>
                <th>Room #</th>
                <th>Type</th>
                <th>Floor</th>
                <th>Price / Night</th>
                <th>Status</th>
                <th>Description</th>
                <th class="text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              ${rooms.map(r => `
                <tr>
                  <td><strong>Room ${r.room_number}</strong></td>
                  <td>${r.room_type}</td>
                  <td>Floor ${r.floor}</td>
                  <td>${formatCurrency(r.price_per_night)}</td>
                  <td><span class="badge badge-${r.status.toLowerCase()}">${r.status}</span></td>
                  <td><small style="color: var(--text-muted);">${r.description || '--'}</small></td>
                  <td class="text-right">
                    <button class="btn btn-secondary btn-sm" onclick="openEditRoomModal(${r.room_id})"><i class="fa-solid fa-pen"></i></button>
                    <button class="btn btn-outline-danger btn-sm" onclick="deleteRoom(${r.room_id})"><i class="fa-solid fa-trash"></i></button>
                  </td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      </div>
    `;
  }
}

function openAddRoomModal() {
  document.getElementById('room-modal-title').innerHTML = '<i class="fa-solid fa-plus"></i> Add New Room';
  document.getElementById('room-form-id').value = '';
  document.getElementById('room-form-number').value = '';
  document.getElementById('room-form-type').value = 'Single';
  document.getElementById('room-form-floor').value = '1';
  document.getElementById('room-form-price').value = '';
  document.getElementById('room-form-status').value = 'AVAILABLE';
  document.getElementById('room-form-desc').value = '';
  openModal('room-modal');
}

function openEditRoomModal(roomId) {
  const room = allRooms.find(r => r.room_id === roomId);
  if (!room) return;

  document.getElementById('room-modal-title').innerHTML = `<i class="fa-solid fa-pen-to-square"></i> Edit Room ${room.room_number}`;
  document.getElementById('room-form-id').value = room.room_id;
  document.getElementById('room-form-number').value = room.room_number;
  document.getElementById('room-form-type').value = room.room_type;
  document.getElementById('room-form-floor').value = room.floor;
  document.getElementById('room-form-price').value = room.price_per_night;
  document.getElementById('room-form-status').value = room.status;
  document.getElementById('room-form-desc').value = room.description || '';
  openModal('room-modal');
}

async function deleteRoom(roomId) {
  const room = allRooms.find(r => r.room_id === roomId);
  const confirmMsg = `Are you sure you want to delete Room ${room?.room_number || roomId}?`;
  if (!confirm(confirmMsg)) return;

  try {
    const res = await API.delete(`/rooms/${roomId}`);
    showToast(res.message, 'success');
    await loadRooms();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// Form submit event
document.addEventListener('DOMContentLoaded', () => {
  const roomForm = document.getElementById('room-form');
  if (roomForm) {
    roomForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const id = document.getElementById('room-form-id').value;
      const payload = {
        roomNumber: document.getElementById('room-form-number').value,
        roomType: document.getElementById('room-form-type').value,
        floor: document.getElementById('room-form-floor').value,
        pricePerNight: document.getElementById('room-form-price').value,
        status: document.getElementById('room-form-status').value,
        description: document.getElementById('room-form-desc').value,
      };

      try {
        if (id) {
          const res = await API.put(`/rooms/${id}`, payload);
          showToast(res.message, 'success');
        } else {
          const res = await API.post('/rooms', payload);
          showToast(res.message, 'success');
        }
        closeModal('room-modal');
        await loadRooms();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }
});
