const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function importSalesCsv(file, token) {
  return importCsv("/api/import/sales", file, token);
}

export async function importStocksCsv(file, token) {
  return importCsv("/api/import/stocks", file, token);
}

async function importCsv(path, file, token) {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${apiUrl}${path}`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: formData,
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Erreur API ${response.status}`);
  }

  return response.json();
}
