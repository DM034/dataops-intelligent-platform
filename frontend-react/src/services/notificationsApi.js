const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function fetchNotifications(token) {
  return request("/api/notifications", token);
}

export async function markNotificationRead(id, token) {
  return request(`/api/notifications/${id}/read`, token, { method: "PATCH" });
}

export async function createNotification(payload, token) {
  return request("/api/notifications", token, {
    method: "POST",
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
