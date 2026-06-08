import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

function App() {
  const [token, setToken] = useState(localStorage.getItem("dataops_token") ?? "");
  const [user, setUser] = useState(localStorage.getItem("dataops_user") ?? "");
  const [page, setPage] = useState("services");

  const auth = useMemo(() => ({ token, setToken, user, setUser }), [token, user]);

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div>
          <p className="eyebrow">DataOps</p>
          <h1>IA et gouvernance</h1>
        </div>
        <nav>
          <button className={page === "services" ? "active" : ""} onClick={() => setPage("services")}>
            État des services
          </button>
          <button className={page === "benchmark" ? "active" : ""} onClick={() => setPage("benchmark")}>
            Benchmark IA
          </button>
          <button className={page === "recommendations" ? "active" : ""} onClick={() => setPage("recommendations")}>
            Recommandations
          </button>
          <button className={page === "quality" ? "active" : ""} onClick={() => setPage("quality")}>
            Qualité des données
          </button>
          <button className={page === "lineage" ? "active" : ""} onClick={() => setPage("lineage")}>
            Data Lineage
          </button>
        </nav>
        <AuthBox auth={auth} />
      </aside>

      <section className="content">
        {page === "services" && <ServicesStatusPage />}
        {page === "benchmark" && <AiBenchmarkPage token={token} />}
        {page === "recommendations" && <RecommendationsPage token={token} />}
        {page === "quality" && <DataQualityPage token={token} />}
        {page === "lineage" && <DataLineagePage token={token} />}
      </section>
    </main>
  );
}

function ServicesStatusPage() {
  const [backend, setBackend] = useState(null);
  const [dependencies, setDependencies] = useState(null);
  const [error, setError] = useState("");
  const [refreshIndex, setRefreshIndex] = useState(0);

  useEffect(() => {
    setError("");
    Promise.all([
      fetch(`${apiUrl}/api/health`).then((response) => {
        if (!response.ok) {
          throw new Error(`Backend indisponible (${response.status})`);
        }
        return response.json();
      }),
      fetch(`${apiUrl}/api/health/dependencies`).then((response) => {
        if (!response.ok) {
          throw new Error(`Dépendances indisponibles (${response.status})`);
        }
        return response.json();
      }),
    ])
      .then(([backendHealth, dependencyHealth]) => {
        setBackend(backendHealth);
        setDependencies(dependencyHealth);
      })
      .catch((requestError) => {
        setBackend(null);
        setDependencies(null);
        setError(requestError.message);
      });
  }, [refreshIndex]);

  const dependencyMap = Object.fromEntries((dependencies?.dependencies ?? []).map((dependency) => [dependency.name, dependency]));
  const rows = [
    serviceRow("Backend", backend ? "OK" : "KO", backend?.service ?? error),
    serviceRow("Base de données", dependencyMap.postgres?.status ?? "KO", dependencyMap.postgres?.details),
    serviceRow("Service IA", dependencyMap["ai-service"]?.status ?? "KO", dependencyMap["ai-service"]?.details),
    serviceRow("Blockchain", dependencyMap.blockchain?.status ?? "KO", dependencyMap.blockchain?.details),
  ];

  return (
    <div className="page">
      <PageHeader
        title="État des services"
        description="Vue de santé des services indépendants du système distribué local."
        onRefresh={() => setRefreshIndex((value) => value + 1)}
      />
      {error && <div className="notice danger">{error}</div>}

      <div className="metric-grid">
        {rows.map((row) => (
          <article className={row.status === "OK" ? "metric ok" : "metric ko"} key={row.name}>
            <span>{row.name}</span>
            <strong>{row.status}</strong>
          </article>
        ))}
      </div>

      <DataTable
        rows={rows}
        empty="Aucun statut disponible."
        columns={[
          ["name", "Service"],
          ["status", "État"],
          ["details", "Détails"],
        ]}
      />
    </div>
  );
}

function AuthBox({ auth }) {
  const [username, setUsername] = useState(auth.user || "admin");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");

  async function login(event) {
    event.preventDefault();
    setMessage("Connexion...");
    try {
      const response = await fetch(`${apiUrl}/api/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });
      if (!response.ok) {
        throw new Error("Identifiants invalides");
      }
      const data = await response.json();
      localStorage.setItem("dataops_token", data.accessToken);
      localStorage.setItem("dataops_user", data.user.username);
      auth.setToken(data.accessToken);
      auth.setUser(data.user.username);
      setMessage("Connecté");
    } catch (error) {
      setMessage(error.message);
    }
  }

  function logout() {
    localStorage.removeItem("dataops_token");
    localStorage.removeItem("dataops_user");
    auth.setToken("");
    auth.setUser("");
    setMessage("Déconnecté");
  }

  if (auth.token) {
    return (
      <div className="auth-card">
        <span>Session</span>
        <strong>{auth.user}</strong>
        <button onClick={logout}>Déconnexion</button>
      </div>
    );
  }

  return (
    <form className="auth-card" onSubmit={login}>
      <label>
        Utilisateur
        <input value={username} onChange={(event) => setUsername(event.target.value)} />
      </label>
      <label>
        Mot de passe
        <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
      </label>
      <button type="submit">Connexion</button>
      {message && <p>{message}</p>}
    </form>
  );
}

function DataQualityPage({ token }) {
  const { data, loading, error, refresh } = useProtectedFetch("/api/data-quality/reports", token);
  const latest = data?.[0];

  return (
    <div className="page">
      <PageHeader
        title="Qualité des données"
        description="Scores générés automatiquement après chaque import CSV ventes ou stocks."
        onRefresh={refresh}
      />
      <State loading={loading} error={error} token={token} />

      <div className="metric-grid">
        <Metric label="Score global" value={latest ? `${latest.globalScore}%` : "-"} tone="strong" />
        <Metric label="Erreurs détectées" value={latest?.errorRows ?? "-"} />
        <Metric label="Complétude" value={latest ? `${latest.completenessRate}%` : "-"} />
        <Metric label="Validité" value={latest ? `${latest.validityRate}%` : "-"} />
        <Metric label="Unicité" value={latest ? `${latest.uniquenessRate}%` : "-"} />
        <Metric label="Cohérence" value={latest ? `${latest.consistencyRate}%` : "-"} />
      </div>

      <DataTable
        rows={data}
        empty="Aucun rapport qualité pour l’instant. Lance un import CSV pour générer les premières données."
        columns={[
          ["importFileId", "Import"],
          ["totalRows", "Lignes"],
          ["validRows", "Valides"],
          ["errorRows", "Erreurs"],
          ["globalScore", "Score"],
          ["createdAt", "Créé le"],
        ]}
      />
    </div>
  );
}

function AiBenchmarkPage({ token }) {
  const { data, loading, error, refresh } = useProtectedFetch("/api/ai/benchmark/anomalies", token, null);
  const rows = data
    ? [
        buildBenchmarkRow("Z-score", data.zscore),
        buildBenchmarkRow("IQR", data.iqr),
        buildBenchmarkRow("Moyenne mobile 7 jours", data.movingAverage),
      ]
    : [];

  return (
    <div className="page">
      <PageHeader
        title="Benchmark IA"
        description="Comparaison simple des méthodes statistiques de détection d’anomalies sur les ventes."
        onRefresh={refresh}
      />
      <State loading={loading} error={error} token={token} />

      <div className="metric-grid">
        <Metric label="Méthode recommandée" value={formatMethod(data?.recommendedMethod)} tone="strong" />
        <Metric label="Méthodes comparées" value={data ? 3 : "-"} />
        <Metric label="Anomalies Z-score" value={data?.zscore?.anomalyCount ?? "-"} />
        <Metric label="Anomalies IQR" value={data?.iqr?.anomalyCount ?? "-"} />
      </div>

      <DataTable
        rows={rows}
        empty="Aucun benchmark chargé. Vérifie que des ventes existent puis actualise."
        columns={[
          ["method", "Méthode"],
          ["anomalyCount", "Anomalies"],
          ["executionTimeMs", "Temps"],
          ["interpretation", "Interprétation"],
        ]}
      />
    </div>
  );
}

function RecommendationsPage({ token }) {
  const { data, setData, loading, error, refresh } = useProtectedFetch("/api/recommendations", token);
  const [agencyFilter, setAgencyFilter] = useState("");
  const [productFilter, setProductFilter] = useState("");
  const [message, setMessage] = useState("");

  const agencies = uniqueValues(data, "agencyName");
  const products = uniqueValues(data, "productName");
  const filtered = data.filter((recommendation) => {
    const agencyMatch = !agencyFilter || recommendation.agencyName === agencyFilter;
    const productMatch = !productFilter || recommendation.productName === productFilter;
    return agencyMatch && productMatch;
  });

  async function generateRecommendations() {
    setMessage("Génération en cours...");
    try {
      const response = await protectedRequest("/api/recommendations/generate", token, { method: "POST" });
      setData(response.recommendations ?? []);
      setMessage(`${response.createdCount} nouvelle(s) recommandation(s) générée(s).`);
    } catch (requestError) {
      setMessage(requestError.message);
    }
  }

  async function updateStatus(id, status) {
    try {
      const updated = await protectedRequest(`/api/recommendations/${id}/status`, token, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status }),
      });
      setData((rows) => rows.map((row) => (row.id === id ? updated : row)));
    } catch (requestError) {
      setMessage(requestError.message);
    }
  }

  return (
    <div className="page">
      <PageHeader
        title="Recommandations"
        description="Actions métier générées depuis les alertes IA, stocks critiques, prédictions de rupture et anomalies de ventes."
        onRefresh={refresh}
      />
      <State loading={loading} error={error} token={token} />

      <div className="toolbar">
        <label>
          Agence
          <select value={agencyFilter} onChange={(event) => setAgencyFilter(event.target.value)}>
            <option value="">Toutes</option>
            {agencies.map((agency) => (
              <option key={agency} value={agency}>
                {agency}
              </option>
            ))}
          </select>
        </label>
        <label>
          Produit
          <select value={productFilter} onChange={(event) => setProductFilter(event.target.value)}>
            <option value="">Tous</option>
            {products.map((product) => (
              <option key={product} value={product}>
                {product}
              </option>
            ))}
          </select>
        </label>
        <button onClick={generateRecommendations} disabled={!token}>
          Générer
        </button>
      </div>
      {message && <div className="notice">{message}</div>}

      <RecommendationsTable rows={filtered} onStatusChange={updateStatus} />
    </div>
  );
}

function DataLineagePage({ token }) {
  const { data, loading, error, refresh } = useProtectedFetch("/api/data-lineage", token);

  return (
    <div className="page">
      <PageHeader
        title="Data Lineage"
        description="Parcours des données depuis le fichier importé jusqu’aux vues dashboard."
        onRefresh={refresh}
      />
      <State loading={loading} error={error} token={token} />

      <div className="flow">
        {["CSV/Excel", "Validation", "ETL", "PostgreSQL", "Dashboard"].map((step, index) => (
          <React.Fragment key={step}>
            <div className="flow-step">{step}</div>
            {index < 4 && <div className="flow-arrow">→</div>}
          </React.Fragment>
        ))}
      </div>

      <DataTable
        rows={data}
        empty="Aucune trace lineage pour l’instant."
        columns={[
          ["sourceName", "Source"],
          ["sourceType", "Type"],
          ["validationStep", "Validation"],
          ["transformationStep", "Transformation"],
          ["storageStep", "Stockage"],
          ["dashboardStep", "Dashboard"],
          ["status", "Statut"],
        ]}
      />
    </div>
  );
}

function PageHeader({ title, description, onRefresh }) {
  return (
    <header className="page-header">
      <div>
        <p className="eyebrow">Module gouvernance</p>
        <h2>{title}</h2>
        <p>{description}</p>
      </div>
      <button onClick={onRefresh}>Actualiser</button>
    </header>
  );
}

function Metric({ label, value, tone }) {
  return (
    <article className={tone === "strong" ? "metric strong" : "metric"}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function DataTable({ rows, columns, empty }) {
  if (!rows?.length) {
    return <div className="empty-state">{empty}</div>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map(([, label]) => (
              <th key={label}>{label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              {columns.map(([key]) => (
                <td key={key}>{formatCell(row[key], key)}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function RecommendationsTable({ rows, onStatusChange }) {
  if (!rows?.length) {
    return <div className="empty-state">Aucune recommandation pour ces filtres.</div>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Type</th>
            <th>Gravité</th>
            <th>Message</th>
            <th>Action suggérée</th>
            <th>Agence</th>
            <th>Produit</th>
            <th>Statut</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((recommendation) => (
            <tr key={recommendation.id}>
              <td>{formatRecommendationType(recommendation.type)}</td>
              <td>
                <span className={`badge ${recommendation.severity?.toLowerCase()}`}>{recommendation.severity}</span>
              </td>
              <td>{recommendation.message}</td>
              <td>{recommendation.suggestedAction}</td>
              <td>{recommendation.agencyName ?? "-"}</td>
              <td>{recommendation.productName ?? "-"}</td>
              <td>
                <select value={recommendation.status} onChange={(event) => onStatusChange(recommendation.id, event.target.value)}>
                  {["NEW", "IN_PROGRESS", "DONE", "IGNORED"].map((status) => (
                    <option key={status} value={status}>
                      {formatStatus(status)}
                    </option>
                  ))}
                </select>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function State({ loading, error, token }) {
  if (!token) {
    return <div className="notice">Connecte-toi pour charger les endpoints protégés du backend.</div>;
  }
  if (loading) {
    return <div className="notice">Chargement des données...</div>;
  }
  if (error) {
    return <div className="notice danger">{error}</div>;
  }
  return null;
}

function useProtectedFetch(path, token, initialValue = []) {
  const [data, setData] = useState(initialValue);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [refreshIndex, setRefreshIndex] = useState(0);

  useEffect(() => {
    if (!token) {
      setData(initialValue);
      return;
    }

    setLoading(true);
    setError("");
    protectedRequest(path, token)
      .then(setData)
      .catch((fetchError) => setError(fetchError.message))
      .finally(() => setLoading(false));
  }, [path, token, refreshIndex]);

  return { data, setData, loading, error, refresh: () => setRefreshIndex((value) => value + 1) };
}

async function protectedRequest(path, token, options = {}) {
  const headers = {
    Authorization: `Bearer ${token}`,
    ...(options.headers ?? {}),
  };
  const response = await fetch(`${apiUrl}${path}`, { ...options, headers });
  if (!response.ok) {
    throw new Error(`Erreur API ${response.status}`);
  }
  return response.json();
}

function formatCell(value, key) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  if (key.endsWith("At") || key === "importDate") {
    return new Intl.DateTimeFormat("fr-FR", { dateStyle: "short", timeStyle: "short" }).format(new Date(value));
  }
  if (["globalScore", "completenessRate", "validityRate", "uniquenessRate", "consistencyRate"].includes(key)) {
    return `${value}%`;
  }
  if (key === "executionTimeMs") {
    return `${value} ms`;
  }
  return String(value);
}

function buildBenchmarkRow(method, result) {
  return {
    id: method,
    method,
    anomalyCount: result?.anomalyCount ?? 0,
    executionTimeMs: result?.executionTimeMs ?? 0,
    interpretation: benchmarkInterpretation(method),
  };
}

function benchmarkInterpretation(method) {
  if (method === "Z-score") {
    return "Simple si la distribution est proche d’une moyenne stable.";
  }
  if (method === "IQR") {
    return "Robuste aux valeurs extrêmes, facile à expliquer avec les quartiles.";
  }
  return "Utile pour les séries temporelles et les changements récents.";
}

function formatMethod(method) {
  const labels = {
    Z_SCORE: "Z-score",
    IQR: "IQR",
    MOVING_AVERAGE: "Moyenne mobile",
  };
  return labels[method] ?? "-";
}

function uniqueValues(rows, key) {
  return [...new Set(rows.map((row) => row[key]).filter(Boolean))].sort();
}

function formatRecommendationType(type) {
  const labels = {
    AI_ALERT: "Alerte IA",
    STOCKOUT_RISK: "Rupture prévue",
    CRITICAL_STOCK: "Stock critique",
    SALES_ANOMALY: "Anomalie ventes",
    DORMANT_STOCK: "Stock dormant",
  };
  return labels[type] ?? type;
}

function formatStatus(status) {
  const labels = {
    NEW: "Nouveau",
    IN_PROGRESS: "En cours",
    DONE: "Terminé",
    IGNORED: "Ignoré",
  };
  return labels[status] ?? status;
}

function serviceRow(name, status, details) {
  return {
    id: name,
    name,
    status,
    details: details || "-",
  };
}

createRoot(document.getElementById("root")).render(<App />);
