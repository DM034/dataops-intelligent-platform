const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function fetchUsers(token) {
  return request("/api/users", token);
}

export async function updateUserAccess(id, payload, token) {
  return request(`/api/users/${id}`, token, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}

async function request(path, token, options = {}) {
  const response = await fetch(`${apiUrl}${path}`, {
    ...options,
    headers: { Authorization: `Bearer ${token}`, ...(options.headers ?? {}) },
  });
  if (!response.ok) {
    throw new Error(`Erreur API ${response.status}`);
  }
  return response.json();
}
