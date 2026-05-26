import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";
const aiServiceUrl = import.meta.env.VITE_AI_SERVICE_URL ?? "http://localhost:8000";

function App() {
  const [backendHealth, setBackendHealth] = useState("checking");
  const [aiHealth, setAiHealth] = useState("checking");

  useEffect(() => {
    fetch(`${apiUrl}/api/health`)
      .then((response) => response.json())
      .then((data) => setBackendHealth(data.status))
      .catch(() => setBackendHealth("offline"));

    fetch(`${aiServiceUrl}/health`)
      .then((response) => response.json())
      .then((data) => setAiHealth(data.status))
      .catch(() => setAiHealth("offline"));
  }, []);

  return (
    <main className="app">
      <section className="panel">
        <p className="eyebrow">DataOps Intelligent Platform</p>
        <h1>Operational data, backend APIs, and AI workflows in one monorepo.</h1>
        <div className="status-grid">
          <StatusCard label="Spring backend" value={backendHealth} />
          <StatusCard label="FastAPI AI service" value={aiHealth} />
        </div>
      </section>
    </main>
  );
}

function StatusCard({ label, value }) {
  return (
    <article className="status-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

createRoot(document.getElementById("root")).render(<App />);

