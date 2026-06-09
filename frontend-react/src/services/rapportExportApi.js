const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function downloadRapport(typeRapport, format, token, filters = {}) {
  const query = new URLSearchParams({ typeRapport });
  if (filters.dateDebut) query.set("dateDebut", filters.dateDebut);
  if (filters.dateFin) query.set("dateFin", filters.dateFin);
  if (filters.module) query.set("module", filters.module);
  if (filters.statut) query.set("statut", filters.statut);

  const response = await fetch(`${apiUrl}/api/rapports/export/${format}?${query}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) {
    throw new Error(`Erreur export ${response.status}`);
  }

  const blob = await response.blob();
  const disposition = response.headers.get("Content-Disposition") ?? "";
  const match = disposition.match(/filename="?([^"]+)"?/);
  const fileName = match?.[1] ?? `${typeRapport.toLowerCase()}.${format === "excel" ? "xlsx" : "pdf"}`;
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}
