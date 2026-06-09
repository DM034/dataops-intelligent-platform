const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function fetchHistorique(token, filters = {}) {
  const query = new URLSearchParams();
  if (filters.module) query.set("module", filters.module);
  if (filters.utilisateur) query.set("utilisateur", filters.utilisateur);
  if (filters.dateDebut) query.set("dateDebut", new Date(filters.dateDebut).toISOString());
  if (filters.dateFin) query.set("dateFin", new Date(filters.dateFin).toISOString());
  if (filters.action) query.set("action", filters.action);
  const path = query.toString() ? `/api/historique/search?${query}` : "/api/historique";
  const response = await fetch(`${apiUrl}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) {
    throw new Error(`Erreur API ${response.status}`);
  }
  return response.json();
}
