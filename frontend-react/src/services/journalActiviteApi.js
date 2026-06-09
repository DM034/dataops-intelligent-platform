const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function fetchJournalActivite(token, filters = {}) {
  const query = new URLSearchParams();
  if (filters.niveau) query.set("niveau", filters.niveau);
  if (filters.module) query.set("module", filters.module);
  if (filters.utilisateur) query.set("utilisateur", filters.utilisateur);
  if (filters.dateDebut) query.set("dateDebut", new Date(filters.dateDebut).toISOString());
  if (filters.dateFin) query.set("dateFin", new Date(filters.dateFin).toISOString());
  query.set("page", String(filters.page ?? 0));
  query.set("size", String(filters.size ?? 20));

  const response = await fetch(`${apiUrl}/api/journal-activite?${query}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) {
    throw new Error(`Erreur API ${response.status}`);
  }
  return response.json();
}
