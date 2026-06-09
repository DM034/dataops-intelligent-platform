const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function fetchReglesMetier(token, module = "") {
  const query = module ? `?module=${encodeURIComponent(module)}` : "";
  return request(`/api/regles-metier${query}`, token);
}

export async function updateRegleMetier(code, payload, token) {
  return request(`/api/regles-metier/${encodeURIComponent(code)}`, token, {
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
