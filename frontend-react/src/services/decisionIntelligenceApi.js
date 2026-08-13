const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function fetchDecisionIntelligence(token) {
  const response = await fetch(`${apiUrl}/api/ai/decision-intelligence`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) {
    throw new Error(`Erreur API ${response.status}`);
  }
  return response.json();
}

export async function fetchAiModelsStatus(token) {
  const response = await fetch(`${apiUrl}/api/ai/models/status`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) {
    throw new Error(`Erreur API ${response.status}`);
  }
  return response.json();
}
