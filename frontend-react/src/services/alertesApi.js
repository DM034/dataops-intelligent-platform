const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function fetchAlertes(token) {
  return request("/api/alertes", token);
}

export async function generateAlertes(token) {
  return request("/api/alertes/generate", token, { method: "POST" });
}

export async function resolveAlerte(id, token) {
  return request(`/api/alertes/${id}/resolve`, token, { method: "PATCH" });
}

export async function ignoreAlerte(id, token) {
  return request(`/api/alertes/${id}/ignore`, token, { method: "PATCH" });
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
