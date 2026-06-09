const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function fetchDashboardGlobal(token) {
  const response = await fetch(`${apiUrl}/api/dashboard/global`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) {
    throw new Error(`Erreur API ${response.status}`);
  }
  return response.json();
}
