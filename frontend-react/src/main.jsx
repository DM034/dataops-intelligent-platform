import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import "./styles.css";

const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";
const aiServiceUrl = import.meta.env.VITE_AI_SERVICE_URL ?? "http://localhost:8000";

const pages = [
  { id: "dashboard", label: "Dashboard" },
  { id: "agencies", label: "Agences" },
  { id: "products", label: "Produits" },
  { id: "sales", label: "Ventes" },
  { id: "stocks", label: "Stocks" },
  { id: "alerts", label: "Alertes IA" },
  { id: "blockchain", label: "Audit blockchain" },
];

function App() {
  const [token, setToken] = useState(() => localStorage.getItem("dataopsToken") ?? "");
  const [user, setUser] = useState(() => readStoredUser());
  const [activePage, setActivePage] = useState("dashboard");

  const api = useMemo(() => createApiClient(token), [token]);

  function handleLogin(payload) {
    setToken(payload.accessToken);
    setUser(payload.user);
    localStorage.setItem("dataopsToken", payload.accessToken);
    localStorage.setItem("dataopsUser", JSON.stringify(payload.user));
  }

  function handleLogout() {
    setToken("");
    setUser(null);
    localStorage.removeItem("dataopsToken");
    localStorage.removeItem("dataopsUser");
  }

  if (!token) {
    return <LoginPage onLogin={handleLogin} />;
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span>DO</span>
          <div>
            <strong>DataOps</strong>
            <small>Intelligent Platform</small>
          </div>
        </div>
        <nav className="nav-list" aria-label="Navigation principale">
          {pages.map((page) => (
            <button
              key={page.id}
              className={activePage === page.id ? "active" : ""}
              type="button"
              onClick={() => setActivePage(page.id)}
            >
              {page.label}
            </button>
          ))}
        </nav>
        <div className="sidebar-footer">
          <span>{user?.fullName ?? user?.username ?? "Utilisateur"}</span>
          <button type="button" onClick={handleLogout}>Déconnexion</button>
        </div>
      </aside>

      <main className="workspace">
        <Header activePage={activePage} user={user} />
        {activePage === "dashboard" && <DashboardPage api={api} />}
        {activePage === "agencies" && <DataTablePage api={api} title="Agences" endpoint="/api/agencies" columns={agencyColumns} />}
        {activePage === "products" && <DataTablePage api={api} title="Produits" endpoint="/api/products" columns={productColumns} />}
        {activePage === "sales" && <SalesPage api={api} />}
        {activePage === "stocks" && <StocksPage api={api} />}
        {activePage === "alerts" && <AlertsPage api={api} />}
        {activePage === "blockchain" && <BlockchainPage api={api} />}
      </main>
    </div>
  );
}

function LoginPage({ onLogin }) {
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({
    username: "",
    email: "",
    fullName: "",
    password: "",
    role: "ADMIN",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setLoading(true);
    setError("");

    const payload = mode === "login"
      ? { username: form.username, password: form.password }
      : form;

    try {
      const response = await fetch(`${apiUrl}/api/auth/${mode}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!response.ok) {
        throw new Error("Identifiants invalides ou compte déjà existant.");
      }
      onLogin(await response.json());
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function updateField(event) {
    setForm({ ...form, [event.target.name]: event.target.value });
  }

  return (
    <main className="login-screen">
      <section className="login-panel">
        <p className="eyebrow">DataOps Intelligent Platform</p>
        <h1>Connexion dashboard</h1>
        <div className="segmented">
          <button className={mode === "login" ? "selected" : ""} type="button" onClick={() => setMode("login")}>Login</button>
          <button className={mode === "register" ? "selected" : ""} type="button" onClick={() => setMode("register")}>Créer un compte</button>
        </div>
        <form className="form-grid" onSubmit={submit}>
          <label>
            <span>Nom utilisateur</span>
            <input name="username" value={form.username} onChange={updateField} required />
          </label>
          {mode === "register" && (
            <>
              <label>
                <span>Email</span>
                <input name="email" type="email" value={form.email} onChange={updateField} required />
              </label>
              <label>
                <span>Nom complet</span>
                <input name="fullName" value={form.fullName} onChange={updateField} required />
              </label>
              <label>
                <span>Rôle</span>
                <select name="role" value={form.role} onChange={updateField}>
                  <option value="ADMIN">ADMIN</option>
                  <option value="MANAGER">MANAGER</option>
                  <option value="ANALYST">ANALYST</option>
                </select>
              </label>
            </>
          )}
          <label>
            <span>Mot de passe</span>
            <input name="password" type="password" value={form.password} onChange={updateField} required minLength={8} />
          </label>
          {error && <p className="error-text">{error}</p>}
          <button className="primary-action" type="submit" disabled={loading}>
            {loading ? "Connexion..." : mode === "login" ? "Se connecter" : "Créer et entrer"}
          </button>
        </form>
      </section>
    </main>
  );
}

function Header({ activePage, user }) {
  const page = pages.find((item) => item.id === activePage);
  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">Espace opérationnel</p>
        <h1>{page?.label ?? "Dashboard"}</h1>
      </div>
      <div className="user-pill">
        <span>{user?.role ?? "USER"}</span>
      </div>
    </header>
  );
}

function DashboardPage({ api }) {
  const { data: overview, loading, error, refresh } = useApiResource(api, "/api/kpi/overview");
  const cards = [
    ["Chiffre d'affaires", formatMoney(overview?.totalRevenue)],
    ["Total ventes", formatNumber(overview?.totalSales)],
    ["Stock total", formatNumber(overview?.totalStock)],
    ["Stocks critiques", formatNumber(overview?.criticalStockProducts)],
  ];

  return (
    <PageState loading={loading} error={error} onRetry={refresh}>
      <section className="metric-grid">
        {cards.map(([label, value]) => <MetricCard key={label} label={label} value={value} />)}
      </section>
      <section className="dashboard-grid">
        <ChartPanel title="Évolution journalière">
          <ResponsiveContainer width="100%" height={260}>
            <AreaChart data={overview?.dailySales ?? []}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Area type="monotone" dataKey="revenue" stroke="#176b61" fill="#d5eee9" />
            </AreaChart>
          </ResponsiveContainer>
        </ChartPanel>
        <ChartPanel title="Ventes par agence">
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={overview?.salesByAgency ?? []}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="label" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="value" fill="#2f6fed" />
            </BarChart>
          </ResponsiveContainer>
        </ChartPanel>
        <ChartPanel title="Top produits">
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={overview?.topProducts ?? []}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="label" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="value" fill="#c46a2b" />
            </BarChart>
          </ResponsiveContainer>
        </ChartPanel>
      </section>
    </PageState>
  );
}

function DataTablePage({ api, title, endpoint, columns }) {
  const { data, loading, error, refresh } = useApiResource(api, endpoint, []);
  return (
    <PageState loading={loading} error={error} onRetry={refresh}>
      <section className="panel">
        <PanelHeader title={title} count={data?.length ?? 0} />
        <DataTable rows={data ?? []} columns={columns} />
      </section>
    </PageState>
  );
}

function SalesPage({ api }) {
  const { data: sales, loading, error, refresh } = useApiResource(api, "/api/sales", []);
  return (
    <PageState loading={loading} error={error} onRetry={refresh}>
      <section className="panel">
        <PanelHeader title="Tableau des ventes" count={sales?.length ?? 0} />
        <DataTable rows={sales ?? []} columns={saleColumns} />
      </section>
    </PageState>
  );
}

function StocksPage({ api }) {
  const levels = useApiResource(api, "/api/stock/levels", []);
  const movements = useApiResource(api, "/api/stock/movements", []);
  const critical = useApiResource(api, "/api/kpi/critical-stocks", []);

  return (
    <PageState loading={levels.loading || movements.loading || critical.loading} error={levels.error || movements.error || critical.error} onRetry={() => { levels.refresh(); movements.refresh(); critical.refresh(); }}>
      <section className="dashboard-grid">
        <section className="panel">
          <PanelHeader title="Stock critique" count={critical.data?.length ?? 0} />
          <DataTable rows={critical.data ?? []} columns={stockLevelColumns} />
        </section>
        <section className="panel">
          <PanelHeader title="Niveaux de stock" count={levels.data?.length ?? 0} />
          <DataTable rows={levels.data ?? []} columns={stockLevelColumns} />
        </section>
      </section>
      <section className="panel">
        <PanelHeader title="Mouvements de stock" count={movements.data?.length ?? 0} />
        <DataTable rows={movements.data ?? []} columns={stockMovementColumns} />
      </section>
    </PageState>
  );
}

function AlertsPage({ api }) {
  const alerts = useApiResource(api, "/api/alerts?activeOnly=false", []);
  const aiSales = useManualResource(api, "/api/ai/sales-anomalies");
  const aiStock = useManualResource(api, "/api/ai/stock-predictions");

  return (
    <PageState loading={alerts.loading} error={alerts.error} onRetry={alerts.refresh}>
      <section className="action-row">
        <button type="button" onClick={aiSales.run} disabled={aiSales.loading}>Analyser ventes</button>
        <button type="button" onClick={aiStock.run} disabled={aiStock.loading}>Prédire stocks</button>
      </section>
      {(aiSales.data || aiStock.data) && (
        <section className="metric-grid">
          <MetricCard label="Alertes ventes créées" value={formatNumber(aiSales.data?.createdAlertCount)} />
          <MetricCard label="Alertes stock créées" value={formatNumber(aiStock.data?.createdAlertCount)} />
          <MetricCard label="Stocks analysés" value={formatNumber(aiStock.data?.analyzedCount)} />
        </section>
      )}
      {(aiSales.error || aiStock.error) && <p className="error-text">{aiSales.error || aiStock.error}</p>}
      <section className="panel">
        <PanelHeader title="Alertes IA" count={alerts.data?.length ?? 0} />
        <DataTable rows={alerts.data ?? []} columns={alertColumns} />
      </section>
    </PageState>
  );
}

function BlockchainPage({ api }) {
  const blocks = useApiResource(api, "/api/blockchain", []);
  const verification = useManualResource(api, "/api/blockchain/verify");

  return (
    <PageState loading={blocks.loading} error={blocks.error} onRetry={blocks.refresh}>
      <section className="action-row">
        <button type="button" onClick={verification.run} disabled={verification.loading}>Vérifier la chaîne</button>
        {verification.data && (
          <span className={verification.data.valid ? "status-ok" : "status-bad"}>
            {verification.data.valid ? "Chaîne valide" : "Chaîne invalide"}
          </span>
        )}
      </section>
      {verification.data?.message && <p className="muted">{verification.data.message}</p>}
      {verification.error && <p className="error-text">{verification.error}</p>}
      <section className="panel">
        <PanelHeader title="Blocs blockchain" count={blocks.data?.length ?? 0} />
        <DataTable rows={blocks.data ?? []} columns={blockchainColumns} />
      </section>
    </PageState>
  );
}

function MetricCard({ label, value }) {
  return (
    <article className="metric-card">
      <span>{label}</span>
      <strong>{value ?? "0"}</strong>
    </article>
  );
}

function ChartPanel({ title, children }) {
  return (
    <section className="panel chart-panel">
      <PanelHeader title={title} />
      {children}
    </section>
  );
}

function PanelHeader({ title, count }) {
  return (
    <div className="panel-header">
      <h2>{title}</h2>
      {typeof count === "number" && <span>{count}</span>}
    </div>
  );
}

function PageState({ loading, error, onRetry, children }) {
  if (loading) {
    return <section className="panel state-panel">Chargement...</section>;
  }
  if (error) {
    return (
      <section className="panel state-panel">
        <p>{error}</p>
        <button type="button" onClick={onRetry}>Réessayer</button>
      </section>
    );
  }
  return <>{children}</>;
}

function DataTable({ rows, columns }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((column) => <th key={column.key}>{column.label}</th>)}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr><td colSpan={columns.length}>Aucune donnée</td></tr>
          ) : rows.map((row, index) => (
            <tr key={row.id ?? `${index}-${columns[0]?.key}`}>
              {columns.map((column) => (
                <td key={column.key}>{column.render ? column.render(row) : formatCell(row[column.key])}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function useApiResource(api, endpoint, fallback = null) {
  const [data, setData] = useState(fallback);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [nonce, setNonce] = useState(0);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");
    api(endpoint)
      .then((result) => mounted && setData(result))
      .catch((err) => mounted && setError(err.message))
      .finally(() => mounted && setLoading(false));
    return () => {
      mounted = false;
    };
  }, [api, endpoint, nonce]);

  return { data, loading, error, refresh: () => setNonce((value) => value + 1) };
}

function useManualResource(api, endpoint) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function run() {
    setLoading(true);
    setError("");
    try {
      setData(await api(endpoint));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return { data, loading, error, run };
}

function createApiClient(token) {
  return async function api(endpoint, options = {}) {
    const response = await fetch(`${apiUrl}${endpoint}`, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
        ...(options.headers ?? {}),
      },
    });
    if (!response.ok) {
      throw new Error(`API ${response.status} sur ${endpoint}`);
    }
    if (response.status === 204) {
      return null;
    }
    return response.json();
  };
}

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem("dataopsUser"));
  } catch {
    return null;
  }
}

function formatMoney(value) {
  return new Intl.NumberFormat("fr-FR", { style: "currency", currency: "EUR" }).format(Number(value ?? 0));
}

function formatNumber(value) {
  return new Intl.NumberFormat("fr-FR").format(Number(value ?? 0));
}

function formatCell(value) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  if (typeof value === "boolean") {
    return value ? "Oui" : "Non";
  }
  return String(value);
}

const agencyColumns = [
  { key: "code", label: "Code" },
  { key: "name", label: "Nom" },
  { key: "city", label: "Ville" },
  { key: "active", label: "Active", render: (row) => row.active ? "Oui" : "Non" },
];

const productColumns = [
  { key: "sku", label: "SKU" },
  { key: "name", label: "Produit" },
  { key: "category", label: "Catégorie" },
  { key: "unitPrice", label: "Prix", render: (row) => formatMoney(row.unitPrice) },
  { key: "active", label: "Actif", render: (row) => row.active ? "Oui" : "Non" },
];

const saleColumns = [
  { key: "saleDate", label: "Date" },
  { key: "agencyName", label: "Agence" },
  { key: "productName", label: "Produit" },
  { key: "quantity", label: "Quantité" },
  { key: "unitPrice", label: "Prix", render: (row) => formatMoney(row.unitPrice) },
  { key: "totalAmount", label: "Total", render: (row) => formatMoney(row.totalAmount) },
  { key: "reference", label: "Référence" },
];

const stockLevelColumns = [
  { key: "productName", label: "Produit" },
  { key: "agencyName", label: "Agence" },
  { key: "quantity", label: "Quantité" },
];

const stockMovementColumns = [
  { key: "movementDate", label: "Date" },
  { key: "agencyName", label: "Agence" },
  { key: "productName", label: "Produit" },
  { key: "type", label: "Type" },
  { key: "quantity", label: "Quantité" },
  { key: "reason", label: "Raison" },
];

const alertColumns = [
  { key: "severity", label: "Sévérité" },
  { key: "title", label: "Titre" },
  { key: "message", label: "Message" },
  { key: "resolved", label: "Résolue", render: (row) => row.resolved ? "Oui" : "Non" },
  { key: "createdAt", label: "Créée le" },
];

const blockchainColumns = [
  { key: "id", label: "ID" },
  { key: "timestamp", label: "Timestamp" },
  { key: "action", label: "Action" },
  { key: "entityType", label: "Entité" },
  { key: "entityId", label: "Entity ID" },
  { key: "currentHash", label: "Hash", render: (row) => `${row.currentHash ?? ""}`.slice(0, 18) },
];

createRoot(document.getElementById("root")).render(<App />);
