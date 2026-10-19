/**
 * Staff & Payroll Management Controller (Admin Only)
 */

let allEmployees = [];

async function renderEmployeesView() {
  const container = document.getElementById('main-content');
  container.innerHTML = `
    <div class="view-header">
      <div class="view-title">
        <h2>Hotel Staff & Payroll Management</h2>
        <p>Admin-restricted employee profiles, departments, and compensation details</p>
      </div>
      <div class="view-actions">
        <button class="btn btn-primary" onclick="openAddEmployeeModal()">
          <i class="fa-solid fa-user-tie"></i> Add Staff Member
        </button>
      </div>
    </div>

    <!-- Filters -->
    <div class="filter-bar">
      <div class="search-input">
        <input type="text" id="employee-search" class="form-control" placeholder="Search by name, phone, or email..." oninput="filterEmployees()">
      </div>
      <select id="employee-filter-status" class="form-control" style="max-width: 180px;" onchange="filterEmployees()">
        <option value="ALL">All Statuses</option>
        <option value="ACTIVE">ACTIVE</option>
        <option value="ON_LEAVE">ON_LEAVE</option>
        <option value="TERMINATED">TERMINATED</option>
      </select>
    </div>

    <div id="employees-container">
      <div class="loading-spinner"><i class="fa-solid fa-spinner fa-spin"></i> Loading employee directory...</div>
    </div>
  `;

  await loadEmployees();
}

async function loadEmployees() {
  try {
    const data = await API.get('/employees');
    allEmployees = data.employees || [];
    filterEmployees();
  } catch (err) {
    document.getElementById('employees-container').innerHTML = `
      <div class="alert alert-danger">Failed to load staff list: ${err.message}</div>
    `;
  }
}

function filterEmployees() {
  const search = document.getElementById('employee-search')?.value.toLowerCase() || '';
  const status = document.getElementById('employee-filter-status')?.value || 'ALL';

  const filtered = allEmployees.filter(e => {
    const matchSearch = e.full_name.toLowerCase().includes(search) ||
                        e.phone.toLowerCase().includes(search) ||
                        (e.email && e.email.toLowerCase().includes(search)) ||
                        e.designation.toLowerCase().includes(search);
    const matchStatus = status === 'ALL' || e.status === status;
    return matchSearch && matchStatus;
  });

  renderEmployeeList(filtered);
}

function renderEmployeeList(employees) {
  const container = document.getElementById('employees-container');
  if (!container) return;

  if (employees.length === 0) {
    container.innerHTML = `
      <div class="card" style="text-align: center; padding: 40px;">
        <i class="fa-solid fa-id-badge" style="font-size: 32px; color: var(--text-dim); margin-bottom: 12px;"></i>
        <p style="color: var(--text-muted);">No staff members found matching criteria.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = `
    <div class="card">
      <div class="table-responsive">
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Full Name</th>
              <th>Gender</th>
              <th>Phone</th>
              <th>Email</th>
              <th>Designation</th>
              <th>Monthly Salary</th>
              <th>Joining Date</th>
              <th>Status</th>
              <th class="text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            ${employees.map(e => `
              <tr>
                <td>#${e.employee_id}</td>
                <td><strong>${e.full_name}</strong></td>
                <td>${e.gender}</td>
                <td>${e.phone}</td>
                <td>${e.email || '--'}</td>
                <td><span class="badge badge-accent">${e.designation}</span></td>
                <td class="font-weight-bold text-success">${formatCurrency(e.salary)}</td>
                <td>${formatDate(e.joining_date)}</td>
                <td><span class="badge badge-${e.status.toLowerCase() === 'active' ? 'available' : 'maintenance'}">${e.status}</span></td>
                <td class="text-right">
                  <button class="btn btn-secondary btn-sm" onclick="openEditEmployeeModal(${e.employee_id})"><i class="fa-solid fa-pen"></i></button>
                  <button class="btn btn-outline-danger btn-sm" onclick="deleteEmployee(${e.employee_id})"><i class="fa-solid fa-trash"></i></button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

function openAddEmployeeModal() {
  document.getElementById('employee-modal-title').innerHTML = '<i class="fa-solid fa-id-badge"></i> Add Hotel Staff';
  document.getElementById('employee-form-id').value = '';
  document.getElementById('emp-name').value = '';
  document.getElementById('emp-gender').value = 'Male';
  document.getElementById('emp-phone').value = '';
  document.getElementById('emp-email').value = '';
  document.getElementById('emp-designation').value = '';
  document.getElementById('emp-salary').value = '';
  document.getElementById('emp-joindate').value = new Date().toISOString().split('T')[0];
  document.getElementById('emp-status').value = 'ACTIVE';
  document.getElementById('emp-address').value = '';
  openModal('employee-modal');
}

function openEditEmployeeModal(employeeId) {
  const e = allEmployees.find(item => item.employee_id === employeeId);
  if (!e) return;

  document.getElementById('employee-modal-title').innerHTML = `<i class="fa-solid fa-pen"></i> Edit Staff Member — ${e.full_name}`;
  document.getElementById('employee-form-id').value = e.employee_id;
  document.getElementById('emp-name').value = e.full_name;
  document.getElementById('emp-gender').value = e.gender;
  document.getElementById('emp-phone').value = e.phone;
  document.getElementById('emp-email').value = e.email || '';
  document.getElementById('emp-designation').value = e.designation;
  document.getElementById('emp-salary').value = e.salary;
  document.getElementById('emp-joindate').value = e.joining_date ? e.joining_date.split('T')[0] : '';
  document.getElementById('emp-status').value = e.status;
  document.getElementById('emp-address').value = e.address || '';
  openModal('employee-modal');
}

async function deleteEmployee(employeeId) {
  const e = allEmployees.find(item => item.employee_id === employeeId);
  if (!confirm(`Are you sure you want to remove ${e?.full_name || employeeId} from staff?`)) return;

  try {
    const res = await API.delete(`/employees/${employeeId}`);
    showToast(res.message, 'success');
    await loadEmployees();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// Form submit event
document.addEventListener('DOMContentLoaded', () => {
  const empForm = document.getElementById('employee-form');
  if (empForm) {
    empForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const id = document.getElementById('employee-form-id').value;
      const payload = {
        fullName: document.getElementById('emp-name').value,
        gender: document.getElementById('emp-gender').value,
        phone: document.getElementById('emp-phone').value,
        email: document.getElementById('emp-email').value,
        designation: document.getElementById('emp-designation').value,
        salary: document.getElementById('emp-salary').value,
        joiningDate: document.getElementById('emp-joindate').value,
        status: document.getElementById('emp-status').value,
        address: document.getElementById('emp-address').value,
      };

      try {
        if (id) {
          const res = await API.put(`/employees/${id}`, payload);
          showToast(res.message, 'success');
        } else {
          const res = await API.post('/employees', payload);
          showToast(res.message, 'success');
        }
        closeModal('employee-modal');
        await loadEmployees();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }
});
