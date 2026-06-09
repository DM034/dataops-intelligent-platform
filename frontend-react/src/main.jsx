import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import { fetchAlertes, generateAlertes, ignoreAlerte, resolveAlerte } from "./services/alertesApi.js";
import { fetchDashboardGlobal } from "./services/dashboardGlobalApi.js";
import { fetchHistorique } from "./services/historiqueApi.js";
import "./styles.css";

const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

function App() {
  const [token, setToken] = useState(localStorage.getItem("dataops_token") ?? "");
  const [user, setUser] = useState(localStorage.getItem("dataops_user") ?? "");
  const [page, setPage] = useState("governance");

  const auth = useMemo(() => ({ token, setToken, user, setUser }), [token, user]);

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div>
          <p className="eyebrow">DataOps</p>
          <h1>IA et gouvernance</h1>
        </div>
        <nav>
          <button className={page === "governance" ? "active" : ""} onClick={() => setPage("governance")}>
            Data Governance
          </button>
          <button className={page === "dashboardGlobal" ? "active" : ""} onClick={() => setPage("dashboardGlobal")}>
            Dashboard Global
          </button>
          <button className={page === "alertes" ? "active" : ""} onClick={() => setPage("alertes")}>
            Alertes
          </button>
          <button className={page === "historique" ? "active" : ""} onClick={() => setPage("historique")}>
            Historique
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
        {page === "governance" && <DataGovernanceDashboard token={token} />}
        {page === "dashboardGlobal" && <DashboardGlobal token={token} />}
        {page === "alertes" && <AlertesPage token={token} />}
        {page === "historique" && <HistoriquePage token={token} />}
        {page === "benchmark" && <AiBenchmarkPage token={token} />}
        {page === "recommendations" && <RecommendationsPage token={token} />}
        {page === "quality" && <DataQualityPage token={token} />}
        {page === "lineage" && <DataLineagePage token={token} />}
      </section>
    </main>
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

function DataGovernanceDashboard({ token }) {
  const { data, loading, error, refresh } = useProtectedFetch("/api/governance/dashboard", token, null);
  const qualityHistory = data?.qualityHistory ?? [];
  const imports = data?.imports ?? [];

  return (
    <div className="page">
      <PageHeader
        title="Data Governance Dashboard"
        description="Suivi de la qualité, provenance, traçabilité et gouvernance des données importées."
        onRefresh={refresh}
      />
      <State loading={loading} error={error} token={token} />

      <div className="metric-grid">
        <Metric label="Score qualité global" value={data ? `${data.globalQualityScore}%` : "-"} tone="strong" />
        <Metric label="Erreurs" value={data?.errorCount ?? "-"} />
        <Metric label="Doublons" value={data?.duplicateCount ?? "-"} />
        <Metric label="Imports" value={data?.importCount ?? "-"} />
        <Metric label="Complétude" value={data ? `${data.completenessRate}%` : "-"} />
        <Metric label="Validité" value={data ? `${data.validityRate}%` : "-"} />
      </div>

      <div className="chart-grid">
        <MiniChart title="Évolution qualité" rows={qualityHistory} valueKey="qualityScore" suffix="%" />
        <MiniChart title="Évolution erreurs" rows={qualityHistory} valueKey="invalidRecords" />
        <MiniChart title="Évolution imports" rows={imports} valueKey="successRows" />
      </div>

      <section className="split-grid">
        <div>
          <h3>Data Lineage</h3>
          <div className="flow vertical-flow">
            {["CSV/Excel", "Validation", "ETL", "PostgreSQL", "IA", "Dashboard"].map((step, index) => (
              <React.Fragment key={step}>
                <div className="flow-step">{step}</div>
                {index < 5 && <div className="flow-arrow">↓</div>}
              </React.Fragment>
            ))}
          </div>
        </div>
        <div>
          <h3>Catalogue des données</h3>
          <DataTable
            rows={data?.catalog ?? []}
            empty="Aucune entrée catalogue."
            columns={[
              ["name", "Nom"],
              ["sourceType", "Source"],
              ["owner", "Owner"],
              ["refreshFrequency", "Refresh"],
            ]}
          />
        </div>
      </section>

      <DataTable
        rows={imports}
        empty="Aucun audit d’import."
        columns={[
          ["fileName", "Fichier"],
          ["importedBy", "Importé par"],
          ["totalRows", "Lignes"],
          ["successRows", "Succès"],
          ["failedRows", "Échecs"],
          ["status", "Statut"],
          ["importDate", "Date"],
        ]}
      />
    </div>
  );
}

function DashboardGlobal({ token }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [refreshIndex, setRefreshIndex] = useState(0);

  useEffect(() => {
    if (!token) {
      setData(null);
      return;
    }

    setLoading(true);
    setError("");
    fetchDashboardGlobal(token)
      .then(setData)
      .catch((requestError) => setError(requestError.message))
      .finally(() => setLoading(false));
  }, [token, refreshIndex]);

  const kpis = data?.kpis;

  return (
    <div className="page">
      <PageHeader
        title="DashboardGlobal"
        description="Vue décisionnelle production, stocks, qualité, achats et alertes pour la supervision industrielle."
        onRefresh={() => setRefreshIndex((value) => value + 1)}
      />
      <State loading={loading} error={error} token={token} />

      <div className="metric-grid">
        <Metric label="Ordres de production" value={kpis?.totalProductionOrders ?? "-"} tone="strong" />
        <Metric label="Ordres en retard" value={kpis?.delayedProductionOrders ?? "-"} />
        <Metric label="Non-conformité" value={kpis ? `${kpis.nonConformityRate}%` : "-"} />
        <Metric label="Stocks critiques" value={kpis?.criticalStockProducts ?? "-"} />
        <Metric label="Achats recommandés" value={kpis?.recommendedPurchases ?? "-"} />
        <Metric label="Alertes actives" value={kpis?.activeAlerts ?? "-"} />
      </div>

      <div className="chart-grid">
        <MiniChart title="Non-conformités par période" rows={data?.nonConformitiesTrend ?? []} valueKey="value" />
        <MiniChart title="Ordres par statut" rows={data?.productionOrdersByStatus ?? []} labelKey="status" valueKey="value" />
        <MiniChart title="Stock par catégorie" rows={data?.stockByProductCategory ?? []} labelKey="category" valueKey="stockLevel" />
      </div>

      <DataTable
        rows={data?.recentAlerts ?? []}
        empty="Aucune alerte active récente."
        columns={[
          ["severity", "Gravité"],
          ["title", "Titre"],
          ["message", "Message"],
          ["createdAt", "Créée le"],
        ]}
      />

      {data?.dataMode && <div className="notice">Mode données : {data.dataMode}</div>}
    </div>
  );
}

function AlertesPage({ token }) {
  const [payload, setPayload] = useState({ summary: { criticalCount: 0, warningCount: 0, activeCount: 0 }, alertes: [] });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [selected, setSelected] = useState(null);
  const [filters, setFilters] = useState({ type: "", sourceModule: "", niveauCriticite: "", statut: "" });

  useEffect(() => {
    if (!token) {
      setPayload({ summary: { criticalCount: 0, warningCount: 0, activeCount: 0 }, alertes: [] });
      return;
    }
    loadAlertes();
  }, [token]);

  async function loadAlertes() {
    setLoading(true);
    setError("");
    try {
      setPayload(await fetchAlertes(token));
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  async function generate() {
    setMessage("Génération en cours...");
    try {
      const response = await generateAlertes(token);
      setPayload({ summary: computeAlertesSummary(response.alertes), alertes: response.alertes });
      setMessage(`${response.createdCount} alerte(s) générée(s).`);
    } catch (requestError) {
      setMessage(requestError.message);
    }
  }

  async function updateAlerte(id, action) {
    try {
      const updated = action === "resolve" ? await resolveAlerte(id, token) : await ignoreAlerte(id, token);
      setPayload((current) => {
        const alertes = current.alertes.map((alerte) => (alerte.id === id ? updated : alerte));
        return { summary: computeAlertesSummary(alertes), alertes };
      });
      setSelected(updated);
    } catch (requestError) {
      setMessage(requestError.message);
    }
  }

  const filtered = payload.alertes.filter((alerte) =>
    (!filters.type || alerte.type === filters.type)
    && (!filters.sourceModule || alerte.sourceModule === filters.sourceModule)
    && (!filters.niveauCriticite || alerte.niveauCriticite === filters.niveauCriticite)
    && (!filters.statut || alerte.statut === filters.statut)
  );

  return (
    <div className="page">
      <PageHeader
        title="Alertes"
        description="Détection intelligente des situations critiques production, stock, qualité et achats."
        onRefresh={loadAlertes}
      />
      <State loading={loading} error={error} token={token} />

      <div className="metric-grid">
        <Metric label="Critiques" value={payload.summary.criticalCount} tone="strong" />
        <Metric label="Warning" value={payload.summary.warningCount} />
        <Metric label="Actives" value={payload.summary.activeCount} />
      </div>

      <div className="toolbar">
        <FilterSelect label="Type" value={filters.type} options={uniqueValues(payload.alertes, "type")} onChange={(value) => setFilters({ ...filters, type: value })} />
        <FilterSelect label="Module" value={filters.sourceModule} options={uniqueValues(payload.alertes, "sourceModule")} onChange={(value) => setFilters({ ...filters, sourceModule: value })} />
        <FilterSelect label="Criticité" value={filters.niveauCriticite} options={uniqueValues(payload.alertes, "niveauCriticite")} onChange={(value) => setFilters({ ...filters, niveauCriticite: value })} />
        <FilterSelect label="Statut" value={filters.statut} options={uniqueValues(payload.alertes, "statut")} onChange={(value) => setFilters({ ...filters, statut: value })} />
        <button onClick={generate} disabled={!token}>Générer</button>
      </div>
      {message && <div className="notice">{message}</div>}

      <AlertesTable rows={filtered} onResolve={(id) => updateAlerte(id, "resolve")} onIgnore={(id) => updateAlerte(id, "ignore")} onSelect={setSelected} />

      {selected && (
        <section className="detail-panel">
          <h3>Détail alerte</h3>
          <p><strong>Référence :</strong> {selected.referenceObjet}</p>
          <p><strong>Message :</strong> {selected.message}</p>
          <p><strong>Module :</strong> {selected.sourceModule}</p>
          <p><strong>Statut :</strong> {selected.statut}</p>
        </section>
      )}
    </div>
  );
}

function HistoriquePage({ token }) {
  const [rows, setRows] = useState([]);
  const [filters, setFilters] = useState({ module: "", utilisateur: "", dateDebut: "", dateFin: "", action: "" });
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!token) {
      setRows([]);
      return;
    }
    load();
  }, [token]);

  async function load() {
    setLoading(true);
    setError("");
    try {
      setRows(await fetchHistorique(token, filters));
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  function exportCsv() {
    const header = ["dateAction", "utilisateurNom", "action", "module", "description", "ancienneValeur", "nouvelleValeur", "referenceObjet", "adresseIp"];
    const csv = [
      header.join(","),
      ...rows.map((row) => header.map((key) => csvCell(row[key])).join(",")),
    ].join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "historique-actions.csv";
    link.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div className="page">
      <PageHeader title="Historique" description="Traçabilité des actions utilisateur et décisions métier." onRefresh={load} />
      <State loading={loading} error={error} token={token} />

      <div className="toolbar">
        <FilterSelect label="Module" value={filters.module} options={["PRODUCTION", "STOCK", "QUALITE", "ACHAT", "SIMULATION", "DASHBOARD"]} onChange={(value) => setFilters({ ...filters, module: value })} />
        <label>
          Utilisateur
          <input value={filters.utilisateur} onChange={(event) => setFilters({ ...filters, utilisateur: event.target.value })} placeholder="Nom ou id" />
        </label>
        <label>
          Date début
          <input type="datetime-local" value={filters.dateDebut} onChange={(event) => setFilters({ ...filters, dateDebut: event.target.value })} />
        </label>
        <label>
          Date fin
          <input type="datetime-local" value={filters.dateFin} onChange={(event) => setFilters({ ...filters, dateFin: event.target.value })} />
        </label>
        <label>
          Action
          <input value={filters.action} onChange={(event) => setFilters({ ...filters, action: event.target.value })} placeholder="RESOLUTION_ALERTE" />
        </label>
        <button onClick={load} disabled={!token}>Filtrer</button>
        <button onClick={exportCsv} disabled={!rows.length}>Export CSV</button>
      </div>

      <HistoriqueTable rows={rows} onSelect={setSelected} />

      {selected && (
        <section className="detail-panel">
          <h3>Détail action</h3>
          <p><strong>Utilisateur :</strong> {selected.utilisateurNom}</p>
          <p><strong>Action :</strong> {selected.action}</p>
          <p><strong>Module :</strong> {selected.module}</p>
          <p><strong>Description :</strong> {selected.description}</p>
          <p><strong>Ancienne valeur :</strong> {selected.ancienneValeur ?? "-"}</p>
          <p><strong>Nouvelle valeur :</strong> {selected.nouvelleValeur ?? "-"}</p>
          <p><strong>Référence :</strong> {selected.referenceObjet ?? "-"}</p>
          <p><strong>Adresse IP :</strong> {selected.adresseIp ?? "-"}</p>
        </section>
      )}
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

function MiniChart({ title, rows, valueKey, labelKey = "period", suffix = "" }) {
  const chartRows = rows.slice(0, 10).reverse();
  const values = chartRows.map((row) => Number(row[valueKey] ?? 0));
  const max = Math.max(...values, 1);

  return (
    <article className="mini-chart">
      <h3>{title}</h3>
      <div className="bars">
        {chartRows.map((row, index) => {
          const value = Number(row[valueKey] ?? 0);
          return (
          <div className="bar-wrap" key={`${title}-${index}`}>
            <div className="bar" style={{ height: `${Math.max((value / max) * 100, 4)}%` }} />
            <small>{row[labelKey] ?? ""}</small>
            <span>
              {value}
              {suffix}
            </span>
          </div>
          );
        })}
      </div>
    </article>
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

function AlertesTable({ rows, onResolve, onIgnore, onSelect }) {
  if (!rows?.length) {
    return <div className="empty-state">Aucune alerte pour ces filtres.</div>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Type</th>
            <th>Criticité</th>
            <th>Module</th>
            <th>Message</th>
            <th>Statut</th>
            <th>Créée le</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((alerte) => (
            <tr key={alerte.id}>
              <td>{formatAlerteType(alerte.type)}</td>
              <td><span className={`badge ${alerte.niveauCriticite?.toLowerCase()}`}>{alerte.niveauCriticite}</span></td>
              <td>{alerte.sourceModule}</td>
              <td>{alerte.message}</td>
              <td>{formatAlerteStatut(alerte.statut)}</td>
              <td>{formatCell(alerte.dateCreation, "createdAt")}</td>
              <td>
                <div className="row-actions">
                  <button onClick={() => onSelect(alerte)}>Voir détail</button>
                  <button onClick={() => onResolve(alerte.id)} disabled={alerte.statut !== "ACTIVE"}>Résoudre</button>
                  <button onClick={() => onIgnore(alerte.id)} disabled={alerte.statut !== "ACTIVE"}>Ignorer</button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function HistoriqueTable({ rows, onSelect }) {
  if (!rows?.length) {
    return <div className="empty-state">Aucune action trouvée pour ces filtres.</div>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Date</th>
            <th>Utilisateur</th>
            <th>Action</th>
            <th>Module</th>
            <th>Description</th>
            <th>Référence</th>
            <th>Détail</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              <td>{formatCell(row.dateAction, "createdAt")}</td>
              <td>{row.utilisateurNom}</td>
              <td>{row.action}</td>
              <td>{row.module}</td>
              <td>{row.description}</td>
              <td>{row.referenceObjet ?? "-"}</td>
              <td><button onClick={() => onSelect(row)}>Voir détail</button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function FilterSelect({ label, value, options, onChange }) {
  return (
    <label>
      {label}
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="">Tous</option>
        {options.map((option) => (
          <option key={option} value={option}>{option}</option>
        ))}
      </select>
    </label>
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
  if (["globalScore", "qualityScore", "completenessRate", "validityRate", "uniquenessRate", "consistencyRate"].includes(key)) {
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

function computeAlertesSummary(alertes) {
  return {
    criticalCount: alertes.filter((alerte) => alerte.niveauCriticite === "CRITICAL").length,
    warningCount: alertes.filter((alerte) => alerte.niveauCriticite === "WARNING").length,
    activeCount: alertes.filter((alerte) => alerte.statut === "ACTIVE").length,
  };
}

function formatAlerteType(type) {
  const labels = {
    STOCK_CRITIQUE: "Stock critique",
    RETARD_PRODUCTION: "Retard production",
    SURCHARGE_RESSOURCE: "Surcharge ressource",
    NON_CONFORMITE_ELEVEE: "Non-conformité élevée",
    ACHAT_URGENT: "Achat urgent",
    FOURNISSEUR_MOINS_PERFORMANT: "Fournisseur moins performant",
  };
  return labels[type] ?? type;
}

function formatAlerteStatut(statut) {
  const labels = {
    ACTIVE: "Active",
    RESOLUE: "Résolue",
    IGNOREE: "Ignorée",
  };
  return labels[statut] ?? statut;
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

function csvCell(value) {
  const text = value === null || value === undefined ? "" : String(value);
  return `"${text.replaceAll('"', '""')}"`;
}

createRoot(document.getElementById("root")).render(<App />);
