const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function importSalesCsv(file, token, onProgress) {
  return importCsv("/api/import/sales", file, token, onProgress);
}

export async function importStocksCsv(file, token, onProgress) {
  return importCsv("/api/import/stocks", file, token, onProgress);
}

function importCsv(path, file, token, onProgress) {
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
      const percent = Math.min(99, Math.round((event.loaded / event.total) * 100));
      onProgress?.(percent);
    };

    request.onload = () => {
      if (request.status >= 200 && request.status < 300) {
        onProgress?.(100);
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
