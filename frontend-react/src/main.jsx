import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import { fetchAlertes, generateAlertes, ignoreAlerte, resolveAlerte } from "./services/alertesApi.js";
import { fetchDashboardGlobal } from "./services/dashboardGlobalApi.js";
import { fetchDecisionIntelligence } from "./services/decisionIntelligenceApi.js";
import { fetchHistorique } from "./services/historiqueApi.js";
import {
  cancelImportJob,
  cleanupImportFiles,
  downloadImportErrors,
  downloadImportReport,
  fetchDatasetVersions,
  fetchImportContracts,
  fetchImportJobs,
  fetchImportObservability,
  fetchImportQualityAlerts,
  importAutoCsv,
  importSalesCsv,
  importStocksCsv,
  previewImportCsv,
  rollbackImportJob,
} from "./services/importApi.js";
import { fetchJournalActivite } from "./services/journalActiviteApi.js";
import { fetchNotifications, markNotificationRead } from "./services/notificationsApi.js";
import { downloadRapport } from "./services/rapportExportApi.js";
import { fetchReglesMetier, updateRegleMetier } from "./services/reglesMetierApi.js";
import { fetchUsers, updateUserAccess } from "./services/usersApi.js";
import "./styles.css";

const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

const roleLabels = {
  ADMIN: "Administrateur",
  DIRECTION: "Direction",
  RESPONSABLE_PRODUCTION: "Responsable production",
  RESPONSABLE_STOCK: "Responsable stock",
  RESPONSABLE_QUALITE: "Responsable qualité",
  RESPONSABLE_ACHAT: "Responsable achat",
  UTILISATEUR_SIMPLE: "Utilisateur simple",
  MANAGER: "Manager",
  ANALYST: "Analyste",
};

const roles = Object.keys(roleLabels);

const pageAccess = {
  governance: ["ADMIN", "DIRECTION", "MANAGER", "ANALYST"],
  dashboardGlobal: ["ADMIN", "DIRECTION", "RESPONSABLE_PRODUCTION", "UTILISATEUR_SIMPLE", "MANAGER", "ANALYST"],
  alertes: ["ADMIN", "DIRECTION", "RESPONSABLE_PRODUCTION", "RESPONSABLE_STOCK", "RESPONSABLE_QUALITE", "RESPONSABLE_ACHAT", "MANAGER", "ANALYST"],
  historique: ["ADMIN", "DIRECTION", "MANAGER", "ANALYST"],
  journalActivite: ["ADMIN", "DIRECTION", "MANAGER", "ANALYST"],
  notifications: ["ADMIN", "DIRECTION", "RESPONSABLE_PRODUCTION", "RESPONSABLE_STOCK", "RESPONSABLE_QUALITE", "RESPONSABLE_ACHAT", "UTILISATEUR_SIMPLE", "MANAGER", "ANALYST"],
  decisionIntelligence: ["ADMIN", "DIRECTION", "MANAGER", "ANALYST"],
  benchmark: ["ADMIN", "DIRECTION", "RESPONSABLE_PRODUCTION", "MANAGER", "ANALYST"],
  recommendations: ["ADMIN", "DIRECTION", "RESPONSABLE_STOCK", "RESPONSABLE_ACHAT", "MANAGER", "ANALYST"],
  imports: ["ADMIN", "DIRECTION", "RESPONSABLE_STOCK", "MANAGER", "ANALYST"],
  quality: ["ADMIN", "DIRECTION", "RESPONSABLE_QUALITE", "MANAGER", "ANALYST"],
  lineage: ["ADMIN", "DIRECTION", "RESPONSABLE_QUALITE", "MANAGER", "ANALYST"],
  businessSettings: ["ADMIN", "DIRECTION", "RESPONSABLE_PRODUCTION", "RESPONSABLE_STOCK", "RESPONSABLE_QUALITE", "RESPONSABLE_ACHAT", "MANAGER", "ANALYST"],
  users: ["ADMIN"],
};

const navItems = [
  { key: "governance", label: "Data Governance" },
  { key: "dashboardGlobal", label: "Dashboard Global" },
  { key: "alertes", label: "Alertes" },
  { key: "historique", label: "Historique" },
  { key: "journalActivite", label: "Journal d’activité" },
  { key: "notifications", label: "Notifications" },
  { key: "decisionIntelligence", label: "Intelligence IA" },
  { key: "benchmark", label: "Benchmark IA" },
  { key: "recommendations", label: "Recommandations" },
  { key: "imports", label: "Import CSV" },
  { key: "quality", label: "Qualité des données" },
  { key: "lineage", label: "Data Lineage" },
  { key: "businessSettings", label: "Paramètres métier" },
  { key: "users", label: "Utilisateurs" },
];

function readStoredUser() {
  const raw = localStorage.getItem("dataops_user_profile");
  if (raw) {
    try {
      return JSON.parse(raw);
    } catch {
      localStorage.removeItem("dataops_user_profile");
    }
  }
  const username = localStorage.getItem("dataops_user");
  return username ? { username, role: "UTILISATEUR_SIMPLE" } : null;
}

function App() {
  const [token, setToken] = useState(localStorage.getItem("dataops_token") ?? "");
  const [user, setUser] = useState(readStoredUser());
  const [page, setPage] = useState("governance");
  const [toasts, setToasts] = useState([]);

  const auth = useMemo(() => ({ token, setToken, user, setUser }), [token, user]);
  const allowedPages = useMemo(() => navItems.filter((item) => canAccess(user?.role, item.key)), [user?.role]);

  useEffect(() => {
    if (token && allowedPages.length && !canAccess(user?.role, page)) {
      setPage(allowedPages[0].key);
    }
  }, [token, user?.role, page, allowedPages]);

  useEffect(() => {
    function handleToast(event) {
      addToast(event.detail?.message ?? "Action effectuée", event.detail?.niveau ?? "SUCCESS");
    }
    window.addEventListener("app-toast", handleToast);
    return () => window.removeEventListener("app-toast", handleToast);
  }, []);

  function addToast(message, niveau = "SUCCESS") {
    const id = crypto.randomUUID();
    setToasts((current) => [...current, { id, message, niveau }]);
    setTimeout(() => setToasts((current) => current.filter((toast) => toast.id !== id)), 4200);
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div>
          <p className="eyebrow">DataOps</p>
          <h1>IA et gouvernance</h1>
        </div>
        <nav>
          <NotificationCenter token={token} onNavigate={setPage} />
          {allowedPages.map((item) => (
            <button key={item.key} className={page === item.key ? "active" : ""} onClick={() => setPage(item.key)}>
              {item.label}
            </button>
          ))}
        </nav>
        <AuthBox auth={auth} />
      </aside>

      <section className="content">
        <ProtectedPage page={page} role={user?.role} token={token}>
          {page === "governance" && <DataGovernanceDashboard token={token} />}
          {page === "dashboardGlobal" && <DashboardGlobal token={token} />}
          {page === "alertes" && <AlertesPage token={token} />}
          {page === "historique" && <HistoriquePage token={token} />}
          {page === "journalActivite" && <JournalActivitePage token={token} />}
          {page === "notifications" && <NotificationsPage token={token} />}
          {page === "decisionIntelligence" && <DecisionIntelligencePage token={token} />}
          {page === "benchmark" && <AiBenchmarkPage token={token} />}
          {page === "recommendations" && <RecommendationsPage token={token} />}
          {page === "imports" && <ImportCsvPage token={token} />}
          {page === "quality" && <DataQualityPage token={token} />}
          {page === "lineage" && <DataLineagePage token={token} />}
          {page === "businessSettings" && <ParametresMetierPage token={token} />}
          {page === "users" && <UserManagementPage token={token} />}
        </ProtectedPage>
        <ToastStack toasts={toasts} />
      </section>
    </main>
  );
}

function AuthBox({ auth }) {
  const [username, setUsername] = useState(auth.user?.username || "admin");
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
      localStorage.setItem("dataops_user_profile", JSON.stringify(data.user));
      auth.setToken(data.accessToken);
      auth.setUser(data.user);
      setMessage("Connecté");
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function logout() {
    if (auth.token) {
      try {
        await fetch(`${apiUrl}/api/auth/logout`, {
          method: "POST",
          headers: { Authorization: `Bearer ${auth.token}` },
        });
      } catch {
        // La session locale reste nettoyée même si le backend est indisponible.
      }
    }
    localStorage.removeItem("dataops_token");
    localStorage.removeItem("dataops_user");
    localStorage.removeItem("dataops_user_profile");
    auth.setToken("");
    auth.setUser(null);
    setMessage("Déconnecté");
  }

  if (auth.token) {
    return (
      <div className="auth-card">
        <span>Session</span>
        <strong>{auth.user?.fullName || auth.user?.username}</strong>
        <p>{roleLabels[auth.user?.role] ?? auth.user?.role ?? "Rôle inconnu"}</p>
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

function canAccess(role, pageKey) {
  if (!role) {
    return false;
  }
  return pageAccess[pageKey]?.includes(role) ?? false;
}

function ProtectedPage({ page, role, token, children }) {
  if (!token) {
    return (
      <div className="page">
        <PageHeader title="Connexion requise" description="Connecte-toi pour accéder aux modules de supervision." />
        <State loading={false} error="" token={token} />
      </div>
    );
  }
  if (!canAccess(role, page)) {
    return (
      <div className="page">
        <PageHeader title="Accès refusé" description="Ton rôle ne donne pas accès à cette fonctionnalité." />
        <div className="notice danger">Accès refusé</div>
      </div>
    );
  }
  return children;
}

function NotificationCenter({ token, onNavigate }) {
  const [open, setOpen] = useState(false);
  const [payload, setPayload] = useState({ unreadCount: 0, notifications: [] });

  useEffect(() => {
    if (!token) {
      setPayload({ unreadCount: 0, notifications: [] });
      return;
    }
    load();
    const interval = setInterval(load, 30000);
    return () => clearInterval(interval);
  }, [token]);

  async function load() {
    try {
      setPayload(await fetchNotifications(token));
    } catch {
      setPayload((current) => current);
    }
  }

  async function markRead(id) {
    try {
      const updated = await markNotificationRead(id, token);
      setPayload((current) => {
        const notifications = current.notifications.map((notification) => notification.id === id ? updated : notification);
        return { unreadCount: notifications.filter((notification) => !notification.lu).length, notifications };
      });
    } catch {
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: "Erreur lors de la lecture notification", niveau: "ERROR" } }));
    }
  }

  const recent = payload.notifications.slice(0, 5);

  return (
    <div className="notification-center">
      <button className="notification-button" onClick={() => setOpen((value) => !value)} disabled={!token}>
        <span>Notifications</span>
        {payload.unreadCount > 0 && <strong>{payload.unreadCount}</strong>}
      </button>
      {open && (
        <div className="notification-dropdown">
          <div className="notification-dropdown-header">
            <strong>Récentes</strong>
            <button onClick={() => onNavigate("notifications")}>Tout voir</button>
          </div>
          {recent.length ? recent.map((notification) => (
            <article key={notification.id} className={notification.lu ? "notification-item read" : "notification-item"}>
              <span className={`badge ${notificationTone(notification.niveau)}`}>{notification.niveau}</span>
              <div>
                <strong>{notification.titre}</strong>
                <p>{notification.message}</p>
                <small>{formatCell(notification.dateCreation, "createdAt")}</small>
              </div>
              {!notification.lu && <button onClick={() => markRead(notification.id)}>Lu</button>}
            </article>
          )) : <div className="empty-state">Aucune notification.</div>}
        </div>
      )}
    </div>
  );
}

function ToastStack({ toasts }) {
  return (
    <div className="toast-stack">
      {toasts.map((toast) => (
        <div key={toast.id} className={`toast ${notificationTone(toast.niveau)}`}>
          {toast.message}
        </div>
      ))}
    </div>
  );
}

function ImportCsvPage({ token }) {
  const [lastResult, setLastResult] = useState(null);
  const [jobs, setJobs] = useState([]);
  const [jobsError, setJobsError] = useState("");
  const [versions, setVersions] = useState([]);
  const [contracts, setContracts] = useState([]);
  const [observability, setObservability] = useState(null);
  const [qualityAlerts, setQualityAlerts] = useState([]);
  const [governanceError, setGovernanceError] = useState("");

  async function loadJobs() {
    try {
      setJobs(await fetchImportJobs(token));
      setJobsError("");
    } catch (error) {
      setJobsError(error.message);
    }
  }

  async function loadImportGovernance() {
    try {
      const [nextVersions, nextContracts, nextObservability, nextAlerts] = await Promise.all([
        fetchDatasetVersions(token),
        fetchImportContracts(token),
        fetchImportObservability(token),
        fetchImportQualityAlerts(token),
      ]);
      setVersions(nextVersions);
      setContracts(nextContracts);
      setObservability(nextObservability);
      setQualityAlerts(nextAlerts);
      setGovernanceError("");
    } catch (error) {
      setGovernanceError(error.message);
    }
  }

  useEffect(() => {
    loadJobs();
    loadImportGovernance();
    const timer = window.setInterval(loadJobs, 3000);
    return () => window.clearInterval(timer);
  }, [token]);

  async function cancelJob(jobId) {
    try {
      await cancelImportJob(jobId, token);
      await loadJobs();
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: "Annulation demandée", niveau: "WARNING" } }));
    } catch (error) {
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: error.message, niveau: "ERROR" } }));
    }
  }

  async function downloadErrors(jobId) {
    try {
      await downloadImportErrors(jobId, token);
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: "Fichier erreurs téléchargé", niveau: "SUCCESS" } }));
    } catch (error) {
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: error.message, niveau: "ERROR" } }));
    }
  }

  async function downloadReport(jobId) {
    try {
      await downloadImportReport(jobId, token);
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: "Rapport import téléchargé", niveau: "SUCCESS" } }));
    } catch (error) {
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: error.message, niveau: "ERROR" } }));
    }
  }

  async function cleanupFiles() {
    try {
      const count = await cleanupImportFiles(token, 7);
      await loadJobs();
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: `${count} fichier(s) nettoyé(s)`, niveau: "SUCCESS" } }));
    } catch (error) {
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: error.message, niveau: "ERROR" } }));
    }
  }

  async function rollbackJob(jobId) {
    try {
      const result = await rollbackImportJob(jobId, token);
      await Promise.all([loadJobs(), loadImportGovernance()]);
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: `${result.deletedRows} ligne(s) annulée(s)`, niveau: "SUCCESS" } }));
    } catch (error) {
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: error.message, niveau: "ERROR" } }));
    }
  }

  return (
    <div className="page">
      <PageHeader
        title="Import CSV"
        description="Charge les fichiers ventes et stocks pour alimenter les KPI, la qualité des données, le lineage et la blockchain privée."
      />

      <div className="notice">
        Pour la démo, commence par les fichiers légers. Pour le benchmark MBDS, utilise ensuite les fichiers de 2,5M lignes.
      </div>

      <div className="split-grid">
        <ImportPanel
          title="Import automatique"
          description="Détecte automatiquement le type de fichier, prévalide les colonnes, puis lance l’import."
          acceptLabel="Fichier ventes ou stocks"
          onPreview={(file) => previewImportCsv(file, token)}
          onImport={(file, onProgress, mode) => importAutoCsv(file, token, onProgress, mode)}
          onResult={(result) => {
            setLastResult({ type: "Auto", result });
            loadJobs();
          }}
        />
        <ImportPanel
          title="Importer les ventes"
          description="Format attendu : date, agencyCode, productCode, quantity, unitPrice"
          acceptLabel="sales_demo_50_000.csv ou sales_2_500_000.csv"
          onPreview={(file) => previewImportCsv(file, token)}
          onImport={(file, onProgress, mode) => importSalesCsv(file, token, onProgress, mode)}
          onResult={(result) => {
            setLastResult({ type: "Ventes", result });
            loadJobs();
          }}
        />
        <ImportPanel
          title="Importer les stocks"
          description="Format attendu : date, agencyCode, productCode, quantity, type"
          acceptLabel="stocks_demo_20_000.csv ou stocks_700_000.csv"
          onPreview={(file) => previewImportCsv(file, token)}
          onImport={(file, onProgress, mode) => importStocksCsv(file, token, onProgress, mode)}
          onResult={(result) => {
            setLastResult({ type: "Stocks", result });
            loadJobs();
          }}
        />
      </div>

      {lastResult && (
        <section className="result-panel">
          <div className="result-header">
            <div>
              <p className="eyebrow">Dernier import</p>
              <h3>{lastResult.type}</h3>
            </div>
            <span className="badge success">Terminé</span>
          </div>
          <div className="metric-grid compact">
            <Metric label="Lignes importées" value={lastResult.result.importedRows} tone="strong" />
            <Metric label="Lignes rejetées" value={lastResult.result.skippedRows} />
            <Metric label="Rapport qualité" value={lastResult.result.dataQualityReportId ?? "-"} />
            <Metric label="Lineage" value={lastResult.result.dataLineageId ?? "-"} />
          </div>
          <DataTable
            rows={(lastResult.result.errors ?? []).slice(0, 20)}
            empty="Aucune erreur retournée par le backend."
            columns={[
              ["line", "Ligne"],
              ["message", "Erreur"],
            ]}
          />
          {(lastResult.result.errors?.length ?? 0) > 20 && (
            <div className="notice">Affichage limité aux premières erreurs. Le fichier complet est disponible dans l’historique d’import.</div>
          )}
        </section>
      )}

      <section className="result-panel">
        <div className="result-header">
          <div>
            <p className="eyebrow">Suivi technique</p>
            <h3>Historique des imports</h3>
          </div>
          <button className="secondary" type="button" onClick={loadJobs}>Actualiser</button>
          <button className="secondary" type="button" onClick={cleanupFiles}>Nettoyer fichiers &gt; 7j</button>
        </div>
        {jobsError && <div className="notice danger">{jobsError}</div>}
        <DataTable
          rows={jobs}
          empty="Aucun import lancé pour l’instant."
          columns={[
            ["fileName", "Fichier"],
            ["type", "Type"],
            ["mode", "Mode"],
            ["status", "Statut"],
            ["progressPercent", "%"],
            ["processedRows", "Traitées"],
            ["totalRows", "Total"],
            ["importedRows", "Importées"],
            ["skippedRows", "Rejetées"],
            ["rowsPerSecond", "Lignes/s"],
            ["estimatedRemainingSeconds", "ETA"],
            ["actions", "Actions"],
          ]}
          renderCell={(row, key) => {
            if (key === "progressPercent") return `${row.progressPercent ?? 0}%`;
            if (key === "estimatedRemainingSeconds") return row.estimatedRemainingSeconds == null ? "-" : formatDuration(row.estimatedRemainingSeconds);
            if (key === "actions") {
              const running = ["STARTING", "COUNTING", "RUNNING", "CANCELLING"].includes(row.status);
              const canRollback = row.status === "COMPLETED" && (row.importedRows ?? 0) > 0;
              return (
                <div className="table-actions">
                  {running && <button type="button" className="secondary" onClick={() => cancelJob(row.jobId)}>Annuler</button>}
                  <button type="button" className="secondary" onClick={() => downloadReport(row.jobId)}>Rapport</button>
                  {row.errorDownloadUrl && <button type="button" className="secondary" onClick={() => downloadErrors(row.jobId)}>Erreurs CSV</button>}
                  {canRollback && <button type="button" className="secondary" onClick={() => rollbackJob(row.jobId)}>Rollback</button>}
                </div>
              );
            }
            return undefined;
          }}
        />
      </section>

      <section className="result-panel">
        <div className="result-header">
          <div>
            <p className="eyebrow">DataOps</p>
            <h3>Observabilité des imports</h3>
          </div>
          <button className="secondary" type="button" onClick={loadImportGovernance}>Actualiser</button>
        </div>
        {governanceError && <div className="notice danger">{governanceError}</div>}
        <div className="metric-grid compact">
          <Metric label="Jobs" value={observability?.totalJobs ?? "-"} />
          <Metric label="Terminés" value={observability?.completedJobs ?? "-"} tone="strong" />
          <Metric label="En cours" value={observability?.runningJobs ?? "-"} />
          <Metric label="Échecs" value={observability?.failedJobs ?? "-"} />
          <Metric label="Vitesse moyenne" value={observability ? `${observability.averageRowsPerSecond} lignes/s` : "-"} />
          <Metric label="Alertes qualité" value={qualityAlerts.length} />
        </div>
        {(observability?.warnings?.length ?? 0) > 0 && (
          <div className="notice danger">{observability.warnings.join(" | ")}</div>
        )}
        <DataTable
          rows={qualityAlerts}
          empty="Aucune alerte qualité générée."
          columns={[
            ["severity", "Sévérité"],
            ["type", "Type"],
            ["message", "Message"],
            ["importJobId", "Import"],
          ]}
          renderCell={(row, key) => {
            if (key === "severity") return <span className={`badge ${row.severity?.toLowerCase()}`}>{row.severity}</span>;
            return undefined;
          }}
        />
      </section>

      <section className="result-panel">
        <div className="result-header">
          <div>
            <p className="eyebrow">Contrats de données</p>
            <h3>Schémas acceptés par l’import</h3>
          </div>
        </div>
        <DataTable
          rows={contracts}
          empty="Aucun contrat de données disponible."
          columns={[
            ["type", "Dataset"],
            ["schemaVersion", "Version"],
            ["requiredColumns", "Colonnes obligatoires"],
            ["businessRules", "Règles métier"],
          ]}
          renderCell={(row, key) => {
            if (key === "requiredColumns") return row.requiredColumns?.join(", ");
            if (key === "businessRules") return row.businessRules?.join(" | ");
            return undefined;
          }}
        />
      </section>

      <section className="result-panel">
        <div className="result-header">
          <div>
            <p className="eyebrow">Versioning dataset</p>
            <h3>Fichiers importés et hash source</h3>
          </div>
        </div>
        <DataTable
          rows={versions}
          empty="Aucune version dataset pour l’instant."
          columns={[
            ["datasetName", "Dataset"],
            ["sourceFileName", "Fichier"],
            ["schemaVersion", "Contrat"],
            ["qualityScore", "Score"],
            ["sourceFileHash", "SHA-256"],
            ["status", "Statut"],
            ["createdAt", "Créé le"],
          ]}
          renderCell={(row, key) => {
            if (key === "sourceFileHash") return row.sourceFileHash ? `${row.sourceFileHash.slice(0, 16)}...` : "-";
            return undefined;
          }}
        />
      </section>
    </div>
  );
}

function ImportPanel({ title, description, acceptLabel, onPreview, onImport, onResult }) {
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [mode, setMode] = useState("PARTIAL_IMPORT");
  const [preview, setPreview] = useState(null);
  const [progress, setProgress] = useState({
    percent: 0,
    status: "IDLE",
    message: "",
    processedRows: 0,
    totalRows: 0,
    importedRows: 0,
    skippedRows: 0,
    rowsPerSecond: 0,
    estimatedRemainingSeconds: null,
  });

  async function submit(event) {
    event.preventDefault();
    if (!file) {
      setMessage("Choisis d'abord un fichier CSV.");
      return;
    }
    setLoading(true);
    setProgress({ percent: 0, status: "PREPARING", message: "Préparation du fichier...", processedRows: 0, totalRows: 0, importedRows: 0, skippedRows: 0, rowsPerSecond: 0, estimatedRemainingSeconds: null });
    setMessage("Import en cours...");
    try {
      const result = await onImport(file, setProgress, mode);
      onResult(result);
      setProgress((current) => ({ ...current, percent: 100, status: "COMPLETED", message: "Import terminé" }));
      setMessage(`${result.importedRows} lignes importées, ${result.skippedRows} lignes rejetées.`);
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: "Import CSV terminé", niveau: "SUCCESS" } }));
    } catch (error) {
      setMessage(error.message);
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: "Erreur import CSV", niveau: "ERROR" } }));
    } finally {
      setLoading(false);
    }
  }

  function chooseFile(event) {
    setFile(event.target.files?.[0] ?? null);
    setMessage("");
    setPreview(null);
    setProgress({ percent: 0, status: "IDLE", message: "", processedRows: 0, totalRows: 0, importedRows: 0, skippedRows: 0, rowsPerSecond: 0, estimatedRemainingSeconds: null });
  }

  async function previewFile() {
    if (!file || !onPreview) {
      return;
    }
    try {
      const result = await onPreview(file);
      setPreview(result);
      setMessage(`${result.message} - score qualité ${result.qualityScore ?? 0}%`);
    } catch (error) {
      setMessage(error.message);
    }
  }

  return (
    <form className="import-card" onSubmit={submit}>
      <div>
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
      <label className="file-picker">
        <span>Fichier CSV</span>
        <input type="file" accept=".csv,text/csv" onChange={chooseFile} />
      </label>
      <div className="file-summary">
        {file ? (
          <>
            <strong>{file.name}</strong>
            <span>{formatBytes(file.size)}</span>
          </>
        ) : (
          <span>{acceptLabel}</span>
        )}
      </div>
      <label className="file-picker">
        <span>Mode d'import</span>
        <select value={mode} onChange={(event) => setMode(event.target.value)}>
          <option value="PARTIAL_IMPORT">Partiel : importer les lignes valides</option>
          <option value="STRICT_IMPORT">Strict : arrêter à la première erreur</option>
        </select>
      </label>
      <button className="secondary" type="button" disabled={!file || loading} onClick={previewFile}>Prévalider</button>
      {preview && (
        <div className={preview.status === "INVALID" ? "notice danger" : "notice"}>
          <strong>{preview.status}</strong> - Type {preview.detectedType} - Score qualité {preview.qualityScore ?? 0}%
          <div>
            {Number(preview.validRows ?? 0).toLocaleString("fr-FR")} lignes valides,
            {" "}{Number(preview.errorRows ?? 0).toLocaleString("fr-FR")} lignes problématiques sur
            {" "}{Number(preview.totalRows ?? 0).toLocaleString("fr-FR")} lignes.
          </div>
          {(preview.missingColumns?.length ?? 0) > 0 && <div>Colonnes manquantes : {preview.missingColumns.join(", ")}</div>}
          {(preview.sampleErrors?.length ?? 0) > 0 && (
            <div>
              {preview.sampleErrors.length} exemple(s) d’erreurs :
              <ul className="compact-list">
                {preview.sampleErrors.slice(0, 5).map((error) => (
                  <li key={`${error.line}-${error.message}`}>Ligne {error.line} : {error.message}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
      {(loading || progress.percent > 0) && (
        <div className="import-progress" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow={progress.percent}>
          <div className="import-progress-header">
            <strong>{progress.status === "COMPLETED" ? "Import terminé" : "Progression réelle backend"}</strong>
            <span>{progress.percent}%</span>
          </div>
          <div className="progress-track">
            <div className="progress-fill" style={{ width: `${progress.percent}%` }} />
          </div>
          <div className="progress-details">
            <span>{progress.message || "Traitement en cours..."}</span>
            {progress.totalRows > 0 && (
              <span>{progress.processedRows.toLocaleString("fr-FR")} / {progress.totalRows.toLocaleString("fr-FR")} lignes</span>
            )}
          </div>
          <div className="progress-details muted">
            <span>{(progress.rowsPerSecond ?? 0).toLocaleString("fr-FR")} lignes/s</span>
            <span>ETA {progress.estimatedRemainingSeconds == null ? "-" : formatDuration(progress.estimatedRemainingSeconds)}</span>
          </div>
        </div>
      )}
      <button type="submit" disabled={loading || !file}>{loading ? "Import..." : "Importer"}</button>
      {message && <div className={message.includes("Erreur") ? "notice danger" : "notice"}>{message}</div>}
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
  const [message, setMessage] = useState("");
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
      <ExportButtons typeRapport="TABLEAU_BORD_GLOBAL" token={token} onMessage={setMessage} />
      <div className="toolbar">
        <span className="toolbar-title">Exports rapides</span>
        <ExportButtons typeRapport="SIMULATION_WHAT_IF" token={token} onMessage={setMessage} compact label="Simulation" />
        <ExportButtons typeRapport="NON_CONFORMITES" token={token} onMessage={setMessage} compact label="Non-conformités" />
        <ExportButtons typeRapport="STOCKS_CRITIQUES" token={token} onMessage={setMessage} compact label="Stocks critiques" />
      </div>
      {message && <div className="notice">{message}</div>}

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
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: `${response.createdCount} alerte(s) générée(s).`, niveau: "SUCCESS" } }));
    } catch (requestError) {
      setMessage(requestError.message);
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: requestError.message, niveau: "ERROR" } }));
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
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: "Action effectuée avec succès", niveau: "SUCCESS" } }));
    } catch (requestError) {
      setMessage(requestError.message);
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: requestError.message, niveau: "ERROR" } }));
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
      <ExportButtons typeRapport="ALERTES_ACTIVES" token={token} onMessage={setMessage} />

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

function JournalActivitePage({ token }) {
  const [payload, setPayload] = useState({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  const [filters, setFilters] = useState({ niveau: "", module: "", dateDebut: "", dateFin: "", utilisateur: "", page: 0, size: 20 });
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!token) {
      setPayload({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
      return;
    }
    load();
  }, [token, filters.page]);

  async function load(nextFilters = filters) {
    setLoading(true);
    setError("");
    try {
      setPayload(await fetchJournalActivite(token, nextFilters));
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  function updateFilter(patch) {
    setFilters((current) => ({ ...current, ...patch, page: 0 }));
  }

  function submitFilters() {
    const nextFilters = { ...filters, page: 0 };
    setFilters(nextFilters);
    load(nextFilters);
  }

  function changePage(delta) {
    setFilters((current) => ({ ...current, page: Math.max(0, current.page + delta) }));
  }

  return (
    <div className="page">
      <PageHeader title="Journal d’activité" description="Suivi des événements techniques et fonctionnels importants." onRefresh={() => load()} />
      <State loading={loading} error={error} token={token} />

      <div className="toolbar">
        <FilterSelect label="Niveau" value={filters.niveau} options={["ERROR", "WARNING", "INFO"]} onChange={(value) => updateFilter({ niveau: value })} />
        <FilterSelect label="Module" value={filters.module} options={["AUTH", "SYSTEME", "UTILISATEURS", "PRODUITS", "VENTES", "STOCK", "RAPPORTS", "ALERTES", "IA"]} onChange={(value) => updateFilter({ module: value })} />
        <label>
          Utilisateur
          <input value={filters.utilisateur} onChange={(event) => updateFilter({ utilisateur: event.target.value })} placeholder="admin" />
        </label>
        <label>
          Date début
          <input type="datetime-local" value={filters.dateDebut} onChange={(event) => updateFilter({ dateDebut: event.target.value })} />
        </label>
        <label>
          Date fin
          <input type="datetime-local" value={filters.dateFin} onChange={(event) => updateFilter({ dateFin: event.target.value })} />
        </label>
        <button onClick={submitFilters} disabled={!token}>Filtrer</button>
      </div>

      <JournalActiviteTable rows={payload.content} onSelect={setSelected} />

      <div className="pagination">
        <button onClick={() => changePage(-1)} disabled={payload.page <= 0}>Précédent</button>
        <span>Page {payload.totalPages ? payload.page + 1 : 0} / {payload.totalPages}</span>
        <button onClick={() => changePage(1)} disabled={payload.page + 1 >= payload.totalPages}>Suivant</button>
        <span>{payload.totalElements} événement(s)</span>
      </div>

      {selected && (
        <section className="detail-panel">
          <h3>Détail événement</h3>
          <p><strong>Niveau :</strong> {selected.niveau}</p>
          <p><strong>Type :</strong> {selected.typeEvenement}</p>
          <p><strong>Module :</strong> {selected.module}</p>
          <p><strong>Utilisateur :</strong> {selected.utilisateur}</p>
          <p><strong>Message :</strong> {selected.message}</p>
          <p><strong>Détails :</strong> {selected.details ?? "-"}</p>
          <p><strong>Référence :</strong> {selected.referenceObjet ?? "-"}</p>
        </section>
      )}
    </div>
  );
}

function NotificationsPage({ token }) {
  const [payload, setPayload] = useState({ unreadCount: 0, notifications: [] });
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!token) {
      setPayload({ unreadCount: 0, notifications: [] });
      return;
    }
    load();
  }, [token]);

  async function load() {
    setLoading(true);
    setError("");
    try {
      setPayload(await fetchNotifications(token));
    } catch (requestError) {
      setError(requestError.message);
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: requestError.message, niveau: "ERROR" } }));
    } finally {
      setLoading(false);
    }
  }

  async function markRead(id) {
    try {
      const updated = await markNotificationRead(id, token);
      setPayload((current) => {
        const notifications = current.notifications.map((notification) => notification.id === id ? updated : notification);
        return { unreadCount: notifications.filter((notification) => !notification.lu).length, notifications };
      });
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: "Notification marquée comme lue", niveau: "SUCCESS" } }));
    } catch (requestError) {
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: requestError.message, niveau: "ERROR" } }));
    }
  }

  return (
    <div className="page">
      <PageHeader title="Notifications" description="Centre de suivi des événements importants et actions utilisateur." onRefresh={load} />
      <State loading={loading} error={error} token={token} />

      <div className="metric-grid">
        <Metric label="Non lues" value={payload.unreadCount} tone="strong" />
        <Metric label="Total" value={payload.notifications.length} />
        <Metric label="Critiques" value={payload.notifications.filter((notification) => notification.niveau === "CRITICAL").length} />
      </div>

      <NotificationsTable rows={payload.notifications} onRead={markRead} onSelect={setSelected} />

      {selected && (
        <section className="detail-panel">
          <h3>Détail notification</h3>
          <p><strong>Titre :</strong> {selected.titre}</p>
          <p><strong>Type :</strong> {selected.type}</p>
          <p><strong>Niveau :</strong> {selected.niveau}</p>
          <p><strong>Message :</strong> {selected.message}</p>
          <p><strong>Lien :</strong> {selected.lienAction ?? "-"}</p>
          <p><strong>Statut :</strong> {selected.lu ? "Lue" : "Non lue"}</p>
        </section>
      )}
    </div>
  );
}

function DecisionIntelligencePage({ token }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!token) {
      setData(null);
      return;
    }
    load();
  }, [token]);

  async function load() {
    setLoading(true);
    setError("");
    try {
      setData(await fetchDecisionIntelligence(token));
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  const modules = data?.modules ?? {};
  const moduleRows = decisionModuleRows(modules);
  const topRisks = data?.risks?.slice(0, 8) ?? [];
  const recommendations = data?.recommendations?.slice(0, 8) ?? [];

  return (
    <div className="page">
      <PageHeader
        title="Intelligence IA"
        description="Pilotage avance des donnees, ventes, stocks, risques, scenarios et recommandations explicables."
        onRefresh={load}
      />
      <State loading={loading} error={error} token={token} />

      <div className="metric-grid">
        <Metric label="Modules avances" value={data?.summary?.moduleCount ?? 15} tone="strong" />
        <Metric label="Lignes ventes" value={data?.summary?.salesRows ?? "-"} />
        <Metric label="Lignes stocks" value={data?.summary?.stockRows ?? "-"} />
        <Metric label="Risque global" value={data?.summary?.globalRiskLevel ?? "-"} />
      </div>

      <section className="intelligence-grid">
        <article className="detail-panel">
          <h3>Qualite decisionnelle</h3>
          <div className="score-line">
            <strong>{modules.weightedQualityScore?.globalScore ?? "-"}%</strong>
            <span>Score global pondere</span>
          </div>
          <ul className="compact-list">
            <li>Completude : {modules.weightedQualityScore?.completenessRate ?? "-"}%</li>
            <li>Validite : {modules.weightedQualityScore?.validityRate ?? "-"}%</li>
            <li>Unicite : {modules.weightedQualityScore?.uniquenessRate ?? "-"}%</li>
            <li>Coherence : {modules.weightedQualityScore?.consistencyRate ?? "-"}%</li>
            <li>Fraicheur : {modules.weightedQualityScore?.freshnessRate ?? "-"}%</li>
          </ul>
        </article>

        <article className="detail-panel">
          <h3>What-If</h3>
          <div className="score-line">
            <strong>{modules.whatIfSimulation?.supplierDelayImpact ?? "-"}</strong>
            <span>Impact delai fournisseur</span>
          </div>
          <ul className="compact-list">
            <li>CA projete : {formatCurrency(modules.whatIfSimulation?.projectedRevenue)}</li>
            <li>Unites projetees : {modules.whatIfSimulation?.projectedUnits ?? "-"}</li>
            <li>Rupture potentielle : {modules.whatIfSimulation?.shortageUnits ?? "-"} unites</li>
          </ul>
        </article>

        <article className="detail-panel">
          <h3>Observabilite IA</h3>
          <div className="score-line">
            <strong>{data?.modelStatus?.status ?? "-"}</strong>
            <span>FastAPI models</span>
          </div>
          <ul className="compact-list">
            {(data?.modelStatus?.models ?? []).slice(0, 5).map((model) => (
              <li key={model.name}>{model.name} : {model.status}</li>
            ))}
          </ul>
        </article>
      </section>

      <section className="detail-panel">
        <h3>Les 15 fonctionnalites IA/DataOps ajoutees</h3>
        <div className="feature-grid">
          {moduleRows.map((module) => (
            <article key={module.key} className="feature-tile">
              <span>{module.index}</span>
              <strong>{module.label}</strong>
              <p>{module.summary}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="split-grid">
        <div>
          <h3>Risques prioritaires</h3>
          <DataTable
            rows={topRisks.map((risk, index) => ({ id: `${risk.agencyCode}-${risk.productCode}-${index}`, ...risk }))}
            empty="Aucun risque prioritaire."
            columns={[
              ["level", "Niveau"],
              ["agencyCode", "Agence"],
              ["productCode", "Produit"],
              ["riskScore", "Score"],
            ]}
          />
        </div>
        <div>
          <h3>Recommandations explicables</h3>
          <DataTable
            rows={recommendations.map((recommendation, index) => ({ id: `${recommendation.type}-${index}`, ...recommendation }))}
            empty="Aucune recommandation IA."
            columns={[
              ["priority", "Priorite"],
              ["type", "Type"],
              ["message", "Message"],
              ["explanation", "Explication"],
            ]}
          />
        </div>
      </section>
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
  const [filters, setFilters] = useState({ moduleSource: "", priority: "", status: "" });
  const [selected, setSelected] = useState(null);
  const [message, setMessage] = useState("");

  const filtered = data
    .filter((recommendation) =>
      (!filters.moduleSource || recommendation.moduleSource === filters.moduleSource)
      && (!filters.priority || recommendation.priority === filters.priority)
      && (!filters.status || recommendation.status === filters.status)
    )
    .sort((left, right) => priorityRank(right.priority) - priorityRank(left.priority));

  async function generateRecommendations() {
    setMessage("Génération en cours...");
    try {
      const response = await protectedRequest("/api/recommendations/generate", token, { method: "POST" });
      setData(response.recommendations ?? []);
      setMessage(`${response.createdCount} nouvelle(s) recommandation(s) générée(s).`);
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: `${response.createdCount} recommandation(s) générée(s).`, niveau: "SUCCESS" } }));
    } catch (requestError) {
      setMessage(requestError.message);
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: requestError.message, niveau: "ERROR" } }));
    }
  }

  async function decideRecommendation(id, action) {
    try {
      const updated = await protectedRequest(`/api/recommendations/${id}/${action}`, token, { method: "PUT" });
      setData((rows) => rows.map((row) => (row.id === id ? updated : row)));
      setSelected(updated);
      setMessage(action === "validate" ? "Recommandation validée." : "Recommandation rejetée.");
      window.dispatchEvent(new CustomEvent("app-toast", {
        detail: {
          message: action === "validate" ? "Décision validée" : "Recommandation rejetée",
          niveau: action === "validate" ? "SUCCESS" : "WARNING",
        },
      }));
    } catch (requestError) {
      setMessage(requestError.message);
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: requestError.message, niveau: "ERROR" } }));
    }
  }

  return (
    <div className="page">
      <PageHeader
        title="Recommandations"
        description="Synthèse des recommandations issues du planning, des simulations, de la qualité, des achats et des alertes intelligentes."
        onRefresh={refresh}
      />
      <State loading={loading} error={error} token={token} />
      <ExportButtons typeRapport="ACHATS_RECOMMANDES" token={token} onMessage={setMessage} />

      <div className="toolbar">
        <FilterSelect label="Module" value={filters.moduleSource} options={uniqueValues(data, "moduleSource")} onChange={(value) => setFilters({ ...filters, moduleSource: value })} />
        <FilterSelect label="Priorité" value={filters.priority} options={["CRITIQUE", "HAUTE", "MOYENNE", "FAIBLE"]} onChange={(value) => setFilters({ ...filters, priority: value })} />
        <FilterSelect label="Statut" value={filters.status} options={["PROPOSEE", "EN_ATTENTE", "VALIDEE", "REJETEE"]} onChange={(value) => setFilters({ ...filters, status: value })} />
        <button onClick={generateRecommendations} disabled={!token}>
          Générer
        </button>
      </div>
      {message && <div className="notice">{message}</div>}

      <RecommendationsTable
        rows={filtered}
        onValidate={(id) => decideRecommendation(id, "validate")}
        onReject={(id) => decideRecommendation(id, "reject")}
        onSelect={setSelected}
      />

      {selected && (
        <section className="detail-panel">
          <h3>Détail recommandation</h3>
          <p><strong>Type :</strong> {formatRecommendationType(selected.type)}</p>
          <p><strong>Module :</strong> {selected.moduleSource}</p>
          <p><strong>Priorité :</strong> {selected.priority}</p>
          <p><strong>Description :</strong> {selected.description}</p>
          <p><strong>Action :</strong> {selected.suggestedAction}</p>
          <p><strong>Impact estimé :</strong> {selected.estimatedImpact}</p>
          <p><strong>Statut :</strong> {formatStatus(selected.status)}</p>
        </section>
      )}
    </div>
  );
}

function ParametresMetierPage({ token }) {
  const [moduleFilter, setModuleFilter] = useState("");
  const [rules, setRules] = useState([]);
  const [draftValues, setDraftValues] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!token) {
      setRules([]);
      return;
    }
    loadRules();
  }, [token, moduleFilter]);

  async function loadRules() {
    setLoading(true);
    setError("");
    try {
      const data = await fetchReglesMetier(token, moduleFilter);
      setRules(data);
      setDraftValues(Object.fromEntries(data.map((rule) => [rule.code, rule.valeur])));
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  async function saveRule(rule, patch = {}) {
    setMessage("Mise à jour en cours...");
    try {
      const updated = await updateRegleMetier(rule.code, {
        valeur: patch.valeur ?? draftValues[rule.code] ?? rule.valeur,
        actif: patch.actif ?? rule.actif,
      }, token);
      setRules((rows) => rows.map((row) => (row.code === updated.code ? updated : row)));
      setDraftValues((values) => ({ ...values, [updated.code]: updated.valeur }));
      setMessage("Règle métier mise à jour.");
    } catch (requestError) {
      setMessage(requestError.message);
    }
  }

  return (
    <div className="page">
      <PageHeader
        title="Paramètres métier"
        description="Configuration centralisée des seuils utilisés par les modules stock, production, qualité, achat et simulation."
        onRefresh={loadRules}
      />
      <State loading={loading} error={error} token={token} />

      <div className="toolbar">
        <FilterSelect
          label="Module"
          value={moduleFilter}
          options={["STOCK", "PRODUCTION", "QUALITE", "ACHAT", "SIMULATION"]}
          onChange={setModuleFilter}
        />
      </div>
      {message && <div className="notice">{message}</div>}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Règle</th>
              <th>Module</th>
              <th>Valeur</th>
              <th>Type</th>
              <th>Statut</th>
              <th>Modification</th>
            </tr>
          </thead>
          <tbody>
            {rules.map((rule) => (
              <tr key={rule.code}>
                <td>
                  <strong>{rule.libelle}</strong>
                  <small>{rule.code}</small>
                  <small>{rule.description}</small>
                </td>
                <td>{rule.module}</td>
                <td>
                  <div className="value-editor">
                    <input
                      value={draftValues[rule.code] ?? rule.valeur}
                      onChange={(event) => setDraftValues((values) => ({ ...values, [rule.code]: event.target.value }))}
                    />
                    <span>{rule.unite}</span>
                  </div>
                </td>
                <td>{rule.typeValeur}</td>
                <td>
                  <button className={rule.actif ? "" : "secondary"} onClick={() => saveRule(rule, { actif: !rule.actif })}>
                    {rule.actif ? "Actif" : "Inactif"}
                  </button>
                </td>
                <td>
                  <div className="row-actions">
                    <button onClick={() => saveRule(rule)}>Enregistrer</button>
                    <small>{formatCell(rule.dateModification, "createdAt")}</small>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!rules.length && !loading && <div className="empty-state">Aucune règle métier disponible.</div>}
    </div>
  );
}

function UserManagementPage({ token }) {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!token) {
      setUsers([]);
      return;
    }
    loadUsers();
  }, [token]);

  async function loadUsers() {
    setLoading(true);
    setError("");
    try {
      setUsers(await fetchUsers(token));
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  async function updateAccess(user, patch) {
    setMessage("Mise à jour en cours...");
    try {
      const updated = await updateUserAccess(user.id, {
        email: user.email,
        fullName: user.fullName,
        role: patch.role ?? user.role,
        active: patch.active ?? user.active,
      }, token);
      setUsers((rows) => rows.map((row) => (row.id === updated.id ? updated : row)));
      setMessage("Utilisateur mis à jour.");
    } catch (requestError) {
      setMessage(requestError.message);
    }
  }

  return (
    <div className="page">
      <PageHeader
        title="Gestion utilisateurs"
        description="Administration simple des rôles et statuts d’accès."
        onRefresh={loadUsers}
      />
      <State loading={loading} error={error} token={token} />
      {message && <div className="notice">{message}</div>}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Utilisateur</th>
              <th>Email</th>
              <th>Rôle</th>
              <th>Statut</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>
                  <strong>{user.fullName}</strong>
                  <small>{user.username}</small>
                </td>
                <td>{user.email}</td>
                <td>
                  <select value={user.role} onChange={(event) => updateAccess(user, { role: event.target.value })}>
                    {roles.map((role) => (
                      <option key={role} value={role}>
                        {roleLabels[role]}
                      </option>
                    ))}
                  </select>
                </td>
                <td>
                  <select value={user.active ? "ACTIVE" : "INACTIVE"} onChange={(event) => updateAccess(user, { active: event.target.value === "ACTIVE" })}>
                    <option value="ACTIVE">Actif</option>
                    <option value="INACTIVE">Inactif</option>
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!users.length && !loading && <div className="empty-state">Aucun utilisateur trouvé.</div>}
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
      {onRefresh && <button onClick={onRefresh}>Actualiser</button>}
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

function DataTable({ rows, columns, empty, renderCell }) {
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
          {rows.map((row, index) => (
            <tr key={row.id ?? row.jobId ?? index}>
              {columns.map(([key]) => {
                const customValue = renderCell?.(row, key);
                return <td key={key}>{customValue ?? formatCell(row[key], key)}</td>;
              })}
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

function RecommendationsTable({ rows, onValidate, onReject, onSelect }) {
  if (!rows?.length) {
    return <div className="empty-state">Aucune recommandation pour ces filtres.</div>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Type</th>
            <th>Module</th>
            <th>Priorité</th>
            <th>Description</th>
            <th>Impact estimé</th>
            <th>Statut</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((recommendation) => (
            <tr key={recommendation.id} className={recommendation.priority === "CRITIQUE" ? "error-row" : ""}>
              <td>{formatRecommendationType(recommendation.type)}</td>
              <td>{recommendation.moduleSource}</td>
              <td>
                <span className={`badge ${priorityTone(recommendation.priority)}`}>{recommendation.priority}</span>
              </td>
              <td>{recommendation.description || recommendation.message}</td>
              <td>{recommendation.estimatedImpact ?? "-"}</td>
              <td>{formatStatus(recommendation.status)}</td>
              <td>
                <div className="row-actions">
                  <button onClick={() => onSelect(recommendation)}>Détail</button>
                  <button onClick={() => onValidate(recommendation.id)} disabled={recommendation.status === "VALIDEE"}>Valider</button>
                  <button onClick={() => onReject(recommendation.id)} disabled={recommendation.status === "REJETEE"}>Rejeter</button>
                </div>
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

function JournalActiviteTable({ rows, onSelect }) {
  if (!rows?.length) {
    return <div className="empty-state">Aucun événement trouvé pour ces filtres.</div>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Niveau</th>
            <th>Date</th>
            <th>Type</th>
            <th>Module</th>
            <th>Message</th>
            <th>Utilisateur</th>
            <th>Détail</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id} className={row.niveau === "ERROR" ? "error-row" : ""}>
              <td><span className={`badge ${row.niveau?.toLowerCase()}`}>{row.niveau}</span></td>
              <td>{formatCell(row.dateEvenement, "createdAt")}</td>
              <td>{row.typeEvenement}</td>
              <td>{row.module}</td>
              <td>{row.message}</td>
              <td>{row.utilisateur}</td>
              <td><button onClick={() => onSelect(row)}>Voir détail</button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function NotificationsTable({ rows, onRead, onSelect }) {
  if (!rows?.length) {
    return <div className="empty-state">Aucune notification.</div>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Niveau</th>
            <th>Titre</th>
            <th>Type</th>
            <th>Message</th>
            <th>Date</th>
            <th>Statut</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((notification) => (
            <tr key={notification.id} className={notification.lu ? "" : "unread-row"}>
              <td><span className={`badge ${notificationTone(notification.niveau)}`}>{notification.niveau}</span></td>
              <td>{notification.titre}</td>
              <td>{notification.type}</td>
              <td>{notification.message}</td>
              <td>{formatCell(notification.dateCreation, "createdAt")}</td>
              <td>{notification.lu ? "Lue" : "Non lue"}</td>
              <td>
                <div className="row-actions">
                  <button onClick={() => onSelect(notification)}>Détail</button>
                  <button onClick={() => onRead(notification.id)} disabled={notification.lu}>Marquer lue</button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function ExportButtons({ typeRapport, token, onMessage, compact = false, label = "" }) {
  async function exportReport(format) {
    onMessage?.("Export en cours...");
    try {
      await downloadRapport(typeRapport, format, token);
      onMessage?.(`Export ${format === "pdf" ? "PDF" : "Excel"} généré.`);
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: `Rapport ${format === "pdf" ? "PDF" : "Excel"} exporté`, niveau: "SUCCESS" } }));
    } catch (requestError) {
      onMessage?.(requestError.message);
      window.dispatchEvent(new CustomEvent("app-toast", { detail: { message: requestError.message, niveau: "ERROR" } }));
    }
  }

  return (
    <div className={compact ? "export-buttons compact" : "export-buttons"}>
      {label && <span>{label}</span>}
      <button onClick={() => exportReport("pdf")} disabled={!token}>Exporter PDF</button>
      <button onClick={() => exportReport("excel")} disabled={!token}>Exporter Excel</button>
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

function formatCurrency(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return "-";
  }
  return new Intl.NumberFormat("fr-FR", { style: "currency", currency: "MGA", maximumFractionDigits: 0 }).format(Number(value));
}

function formatBytes(value) {
  if (!value) {
    return "0 o";
  }
  const units = ["o", "Ko", "Mo", "Go"];
  const index = Math.min(Math.floor(Math.log(value) / Math.log(1024)), units.length - 1);
  const size = value / 1024 ** index;
  return `${size.toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
}

function formatDuration(seconds) {
  if (seconds === null || seconds === undefined || Number.isNaN(Number(seconds))) {
    return "-";
  }
  const total = Math.max(0, Math.round(Number(seconds)));
  const minutes = Math.floor(total / 60);
  const rest = total % 60;
  if (minutes <= 0) {
    return `${rest}s`;
  }
  return `${minutes}m ${rest}s`;
}

function decisionModuleRows(modules) {
  const definitions = [
    ["dataProfiling", "Profiling automatique", `${modules.dataProfiling?.rowCount ?? 0} lignes analysees`],
    ["sourceTrustScoring", "Score de confiance source", `${modules.sourceTrustScoring?.sources?.length ?? 0} source(s) scorees`],
    ["multiSourceReconciliation", "Rapprochement multi-source", modules.multiSourceReconciliation?.status ?? "-"],
    ["dataDriftDetection", "Detection de drift", modules.dataDriftDetection?.status ?? "-"],
    ["configurableQualityRules", "Regles qualite configurables", `${modules.configurableQualityRules?.failedRuleCount ?? 0} regle(s) en echec`],
    ["weightedQualityScore", "Score qualite pondere", `${modules.weightedQualityScore?.globalScore ?? "-"}%`],
    ["freshnessMonitoring", "Fraicheur des donnees", modules.freshnessMonitoring?.status ?? "-"],
    ["dataObservability", "Observabilite data", `${modules.dataObservability?.incidentCount ?? 0} incident(s)`],
    ["dataQualityIncidents", "Incidents qualite", `${modules.dataQualityIncidents?.length ?? 0} incident(s)`],
    ["certifiedKpis", "KPI certifies", modules.certifiedKpis?.certificationLevel ?? "-"],
    ["kpiReconciliation", "Comparaison KPI Async/DataOps", `${modules.kpiReconciliation?.comparisons?.length ?? 0} KPI compare(s)`],
    ["agencyDataSla", "SLA qualite agence", `${modules.agencyDataSla?.length ?? 0} agence(s)`],
    ["demandForecasting", "Prevision de demande", `${modules.demandForecasting?.length ?? 0} prevision(s)`],
    ["stockoutProbability", "Probabilite de rupture", `${modules.stockoutProbability?.length ?? 0} risque(s)`],
    ["optimizedReplenishment", "Reapprovisionnement optimise", `${modules.optimizedReplenishment?.length ?? 0} action(s)`],
  ];
  return definitions.map(([key, label, summary], index) => ({ key, label, summary, index: index + 1 }));
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
    OPTIMISATION_PLANNING: "Optimisation planning",
    SIMULATION_WHAT_IF: "Simulation What-If",
    PREDICTION_NON_CONFORMITE: "Prédiction qualité",
    OPTIMISATION_ACHATS: "Optimisation achats",
    ALERTE_INTELLIGENTE: "Alerte intelligente",
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
    PROPOSEE: "Proposée",
    VALIDEE: "Validée",
    REJETEE: "Rejetée",
    EN_ATTENTE: "En attente",
    NEW: "Nouveau",
    IN_PROGRESS: "En cours",
    DONE: "Terminé",
    IGNORED: "Ignoré",
  };
  return labels[status] ?? status;
}

function priorityRank(priority) {
  return { CRITIQUE: 4, HAUTE: 3, MOYENNE: 2, FAIBLE: 1 }[priority] ?? 0;
}

function priorityTone(priority) {
  if (priority === "CRITIQUE") return "critical";
  if (priority === "HAUTE") return "warning";
  if (priority === "MOYENNE") return "info";
  return "low";
}

function notificationTone(niveau) {
  if (niveau === "CRITICAL" || niveau === "ERROR") return "critical";
  if (niveau === "WARNING") return "warning";
  if (niveau === "SUCCESS") return "success";
  return "info";
}

function csvCell(value) {
  const text = value === null || value === undefined ? "" : String(value);
  return `"${text.replaceAll('"', '""')}"`;
}

createRoot(document.getElementById("root")).render(<App />);
