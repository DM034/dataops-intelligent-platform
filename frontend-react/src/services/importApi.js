const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function importSalesCsv(file, token, onProgress) {
  return importCsvJob("/api/import/sales/jobs", file, token, onProgress);
}

export async function importStocksCsv(file, token, onProgress) {
  return importCsvJob("/api/import/stocks/jobs", file, token, onProgress);
}

async function importCsvJob(path, file, token, onProgress) {
  onProgress?.({
    percent: 0,
    status: "PREPARING",
    message: "Préparation du fichier...",
    processedRows: 0,
    totalRows: 0,
  });

  const startedJob = await startJob(path, file, token, onProgress);
  onProgress?.({
    percent: startedJob.progressPercent ?? 0,
    status: startedJob.status,
    message: "Import lancé côté backend",
    processedRows: 0,
    totalRows: startedJob.totalRows ?? 0,
  });

  return pollJob(startedJob.jobId, token, onProgress);
}

function startJob(path, file, token, onProgress) {
  return new Promise((resolve, reject) => {
    const formData = new FormData();
    formData.append("file", file);

    const request = new XMLHttpRequest();
    request.open("POST", `${apiUrl}${path}`);
    request.setRequestHeader("Authorization", `Bearer ${token}`);

    request.upload.onprogress = (event) => {
      if (!event.lengthComputable) {
        return;
      }
      const uploadPercent = Math.round((event.loaded / event.total) * 100);
      onProgress?.({
        percent: Math.min(5, Math.max(1, uploadPercent)),
        status: "UPLOADING",
        message: "Envoi du fichier au backend...",
        processedRows: 0,
        totalRows: 0,
      });
    };

    request.onload = () => {
      if (request.status >= 200 && request.status < 300) {
        try {
          resolve(JSON.parse(request.responseText));
        } catch (error) {
          reject(new Error("Réponse API invalide"));
        }
        return;
      }

      reject(new Error(request.responseText || `Erreur API ${request.status}`));
    };

    request.onerror = () => reject(new Error("Erreur réseau pendant l'import CSV"));
    request.onabort = () => reject(new Error("Import CSV annulé"));
    request.send(formData);
  });
}

function pollJob(jobId, token, onProgress) {
  return new Promise((resolve, reject) => {
    let stopped = false;

    async function tick() {
      if (stopped) {
        return;
      }

      try {
        const response = await fetch(`${apiUrl}/api/import/jobs/${jobId}`, {
          headers: { Authorization: `Bearer ${token}` },
        });

        if (!response.ok) {
          throw new Error(await response.text() || `Erreur API ${response.status}`);
        }

        const progress = await response.json();
        onProgress?.({
          percent: progress.progressPercent ?? 0,
          status: progress.status,
          message: progress.message,
          processedRows: progress.processedRows ?? 0,
          totalRows: progress.totalRows ?? 0,
        });

        if (progress.status === "COMPLETED") {
          stopped = true;
          resolve(progress.result);
          return;
        }

        if (progress.status === "FAILED") {
          stopped = true;
          reject(new Error(progress.message || "Import CSV échoué"));
          return;
        }

        window.setTimeout(tick, 1000);
      } catch (error) {
        stopped = true;
        reject(error);
      }
    }

    tick();
  });
}
