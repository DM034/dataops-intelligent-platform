# DataOps Intelligent Platform

DataOps Intelligent Platform est une plateforme full-stack de pilotage des ventes, stocks, agences et produits. Le projet combine un backend Spring Boot, un dashboard React, un service IA FastAPI et une base PostgreSQL afin de centraliser les operations metier, calculer des KPI, detecter des risques et tracer les actions sensibles dans une blockchain privee simple.

## Architecture

```text
dataops-intelligent-platform/
  backend-spring/      API REST Spring Boot, securite JWT, JPA, PostgreSQL
  frontend-react/      Dashboard React avec Vite
  ai-service/          Service FastAPI pour analyses ventes/stocks
  docker-compose.yml   Orchestration locale des services
  README.md            Documentation projet
```

Flux principal :

```text
Utilisateur -> Frontend React -> Backend Spring -> PostgreSQL
                                      |
                                      v
                                AI service FastAPI
                                      |
                                      v
                              Alertes + Audit blockchain
```

## Technologies

- Java 21, Spring Boot, Spring Security JWT, Spring Data JPA
- PostgreSQL 16
- React 18, Vite, Recharts
- Python 3.12, FastAPI, Pandas, NumPy
- Docker Compose
- SHA-256 pour la blockchain privee

## Fonctionnalites

- Authentification JWT avec inscription et connexion
- Gestion des utilisateurs, agences, produits, ventes et mouvements de stock
- Import CSV des ventes et stocks avec erreurs ligne par ligne
- Gouvernance des donnees : catalogue, qualite, lineage et audit des imports
- Dashboard KPI : chiffre d'affaires, ventes, stocks, top produits, ventes journalieres
- Detection d'anomalies de ventes par Z-score
- Moyenne mobile sur 7 jours
- Prediction simple de rupture de stock
- Classification des alertes : `faible`, `moyen`, `critique`
- Creation d'alertes en base
- Audit blockchain privee des operations sensibles
- Verification d'integrite de la chaine d'audit
- Documentation Swagger/OpenAPI exposee par Springdoc

## Installation

Prerequis :

- Docker et Docker Compose
- Git
- Optionnel pour developpement local : Java 21, Maven, Node.js, Python 3.12

Cloner le projet :

```bash
git clone https://github.com/DM034/dataops-intelligent-platform.git
cd dataops-intelligent-platform
```

## Lancement Avec Docker

Commande principale attendue :

```bash
docker compose up --build
```

Lancer en arriere-plan :

```bash
docker compose up --build -d
```

Arreter les services :

```bash
docker compose down
```

Nettoyer les anciens conteneurs de branches precedentes :

```bash
docker compose down --remove-orphans
```

Reinitialiser aussi les donnees PostgreSQL :

```bash
docker compose down -v
```

URLs locales :

- Frontend : http://localhost:5173
- Backend : http://localhost:8080
- AI service : http://localhost:8000
- PostgreSQL : `localhost:5432`

Services Docker Compose :

- `postgres`
- `backend-spring`
- `frontend-react`
- `ai-service`

## Configuration

Les variables ont des valeurs de developpement par defaut dans `docker-compose.yml`.

PostgreSQL :

- `POSTGRES_DB=dataops`
- `POSTGRES_USER=dataops`
- `POSTGRES_PASSWORD=dataops`
- Volume : `postgres-data`

Ports :

- `FRONTEND_PORT=5173`
- `BACKEND_PORT=8080`
- `AI_SERVICE_PORT=8000`
- `POSTGRES_PORT=5432`

Backend :

- `JWT_SECRET=change-this-development-secret-change-this`
- `AI_SERVICE_URL=http://ai-service:8000`
- `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/dataops`

## Format Des Fichiers CSV

Import ventes : `POST /api/import/sales`

Colonnes obligatoires :

```csv
date,agencyCode,productCode,quantity,unitPrice
2026-05-01,DEMO-TNR,DEMO-RIZ-25,12,28.50
```

Import stocks : `POST /api/import/stocks`

Colonnes obligatoires :

```csv
date,agencyCode,productCode,quantity,type
2026-05-01,DEMO-TNR,DEMO-RIZ-25,50,IN
```

Valeurs de `type` :

- `IN` : entree de stock
- `OUT` : sortie de stock
- `ADJUSTMENT` : ajustement

Contraintes d'import :

- Les colonnes obligatoires sont validees.
- Les lignes vides sont ignorees.
- Les lignes invalides sont retournees avec leur numero et leur message d'erreur.
- Les lignes valides sont enregistrees.
- Chaque import valide peut alimenter l'audit blockchain selon le backend.

## Roles et Acces

Le backend utilise JWT et les autorites Spring Security `ROLE_*`.

Roles metier :

- `ADMIN` : acces complet, gestion des utilisateurs.
- `DIRECTION` : dashboard, KPI, rapports, simulations, gouvernance.
- `RESPONSABLE_PRODUCTION` : ventes, production, simulations et alertes production.
- `RESPONSABLE_STOCK` : stocks, alertes stock et achats recommandes.
- `RESPONSABLE_QUALITE` : qualite des donnees et alertes qualite.
- `RESPONSABLE_ACHAT` : recommandations d'achat, produits et alertes achat.
- `UTILISATEUR_SIMPLE` : lecture limitee.

Le frontend masque les menus non autorises et affiche `Acces refuse` si une page est appelee sans droit. La page `Utilisateurs` est reservee au role `ADMIN`.

## Parametres Metier

Le module `Parametres metier` centralise les seuils configurables utilises par les modules stock, production, qualite, achat et simulation.

Regles initialisees par defaut :

- `STOCK_CRITIQUE_SEUIL`
- `FOURNISSEUR_DELAI_MIN`
- `QUALITE_NON_CONFORMITE_MAX`
- `PRODUCTION_CAPACITE_MAX_ATELIER`
- `PRODUCTION_RETARD_SEUIL`
- `SIMULATION_PRIORITE_CLIENT_COEF`
- `SIMULATION_COUT_RETARD_ESTIME`
- `ACHAT_ALERTE_SEUIL`

## Endpoints Principaux

Endpoints publics :

- `GET /api/health`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- Swagger UI : `GET /swagger-ui/index.html`

Endpoints authentifies :

- `GET /api/users`
- `GET /api/agencies`
- `POST /api/agencies`
- `PUT /api/agencies/{id}`
- `DELETE /api/agencies/{id}`
- `GET /api/products`
- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`
- `GET /api/sales`
- `POST /api/sales`
- `PUT /api/sales/{id}`
- `DELETE /api/sales/{id}`
- `GET /api/stock/movements`
- `POST /api/stock/movements`
- `PUT /api/stock/movements/{id}`
- `DELETE /api/stock/movements/{id}`
- `GET /api/stock/levels`
- `POST /api/import/sales`
- `POST /api/import/stocks`
- `GET /api/alerts`
- `PATCH /api/alerts/{id}/resolve`

Parametres metier :

- `GET /api/regles-metier`
- `GET /api/regles-metier?module=STOCK`
- `GET /api/regles-metier/{code}`
- `PUT /api/regles-metier/{code}`

Journal d'activite :

- `GET /api/journal-activite`
- `GET /api/journal-activite?niveau=ERROR&module=SYSTEME&page=0&size=20`
- `GET /api/journal-activite/{id}`

Recommandations :

- `GET /api/recommendations`
- `GET /api/recommendations/{id}`
- `POST /api/recommendations/generate`
- `PUT /api/recommendations/{id}/validate`
- `PUT /api/recommendations/{id}/reject`
- Statuts de decision : `PROPOSEE`, `VALIDEE`, `REJETEE`, `EN_ATTENTE`

KPI :

- `GET /api/kpi/overview`
- `GET /api/kpi/sales-by-agency`
- `GET /api/kpi/sales-by-product`
- `GET /api/kpi/critical-stocks`
- `GET /api/kpi/daily-sales`
- `GET /api/dashboard/global`

IA via backend :

- `GET /api/ai/sales-anomalies`
- `GET /api/ai/stock-predictions`

Blockchain :

- `GET /api/blockchain`
- `POST /api/blockchain`
- `GET /api/blockchain/verify`

Exports rapports :

- `GET /api/rapports/export/pdf?typeRapport=TABLEAU_BORD_GLOBAL`
- `GET /api/rapports/export/excel?typeRapport=ALERTES_ACTIVES`
- Parametres optionnels : `dateDebut`, `dateFin`, `module`, `statut`
- Types disponibles : `TABLEAU_BORD_GLOBAL`, `ALERTES_ACTIVES`, `SIMULATION_WHAT_IF`, `ACHATS_RECOMMANDES`, `NON_CONFORMITES`, `STOCKS_CRITIQUES`

Gouvernance des donnees :

- `GET /api/catalog`
- `GET /api/catalog/{id}`
- `POST /api/catalog`
- `PUT /api/catalog/{id}`
- `DELETE /api/catalog/{id}`
- `GET /api/data-quality`
- `GET /api/data-quality/latest`
- `GET /api/data-quality/history`
- `GET /api/lineage`
- `GET /api/lineage/{id}`
- `GET /api/import-audit`
- `GET /api/import-audit/{id}`
- `GET /api/governance/dashboard`

Dashboard decisionnel global :

```json
{
  "kpis": {
    "totalProductionOrders": 128,
    "delayedProductionOrders": 14,
    "nonConformityRate": 6.00,
    "criticalStockProducts": 3,
    "recommendedPurchases": 5,
    "activeAlerts": 2
  },
  "nonConformitiesTrend": [
    { "period": "Jan", "value": 6 }
  ],
  "productionOrdersByStatus": [
    { "status": "EN_COURS", "value": 41 }
  ],
  "stockByProductCategory": [
    { "category": "Produits finis", "stockLevel": 185 }
  ],
  "recentAlerts": [],
  "dataMode": "MIXED_REAL_AND_MOCKED_AGGREGATES"
}
```

Alertes intelligentes industrielles :

- `GET /api/alertes`
- `GET /api/alertes/active`
- `GET /api/alertes/{id}`
- `PATCH /api/alertes/{id}/resolve`
- `PATCH /api/alertes/{id}/ignore`
- `POST /api/alertes/generate`

Exemple de reponse :

```json
{
  "summary": {
    "criticalCount": 2,
    "warningCount": 3,
    "activeCount": 5
  },
  "alertes": [
    {
      "id": 1,
      "type": "STOCK_CRITIQUE",
      "niveauCriticite": "CRITICAL",
      "message": "Stock critique pour Produit A / Agence TANA : 2 unite(s).",
      "sourceModule": "STOCK",
      "statut": "ACTIVE",
      "dateCreation": "2026-06-09T08:00:00Z",
      "dateResolution": null,
      "referenceObjet": "Produit A|Agence TANA"
    }
  ]
}
```

Historique et tracabilite :

- `GET /api/historique`
- `GET /api/historique/{id}`
- `GET /api/historique/search?module=STOCK&utilisateur=admin&dateDebut=2026-06-01T00:00:00Z&dateFin=2026-06-09T23:59:59Z&action=RESOLUTION_ALERTE`

Exemple de reponse :

```json
[
  {
    "id": 1,
    "utilisateurId": "admin",
    "utilisateurNom": "admin",
    "action": "RESOLUTION_ALERTE",
    "module": "STOCK",
    "description": "Resolution de l'alerte STOCK_CRITIQUE",
    "ancienneValeur": "ACTIVE",
    "nouvelleValeur": "RESOLUE",
    "dateAction": "2026-06-09T08:00:00Z",
    "referenceObjet": "Produit A|Agence TANA",
    "adresseIp": "127.0.0.1"
  }
]
```

AI service direct :

- `GET /health`
- `POST /ai/anomalies/sales`
- `POST /ai/stock/predict`
- `POST /ai/alerts/score`

## Module IA

Le module `ai-service` est un service FastAPI independant. Il recoit des donnees JSON envoyees par le backend Spring ou par un client direct.

Analyses disponibles :

- Detection d'anomalies de ventes avec Z-score.
- Calcul de moyenne mobile sur 7 jours.
- Prediction simple du nombre de jours avant rupture de stock.
- Classification des alertes selon le risque.

Exemple de payload anomalies :

```json
{
  "sales": [
    {
      "date": "2026-05-01",
      "agencyCode": "DEMO-TNR",
      "productCode": "DEMO-RIZ-25",
      "quantity": 12,
      "unitPrice": 28.5
    }
  ],
  "zscore_threshold": 2.0
}
```

Le backend peut appeler le service IA, recevoir les resultats, puis creer des alertes en base afin de les afficher dans le dashboard.

## Blockchain Privee

Le backend inclut une blockchain privee simple pour tracer les operations sensibles.

Chaque bloc contient :

- `id`
- `timestamp`
- `action`
- `entityType`
- `entityId`
- `userId`
- `dataHash`
- `previousHash`
- `currentHash`

Le hash est calcule en SHA-256 avec les donnees du bloc et le hash precedent. La verification de chaine controle :

- que chaque `previousHash` correspond au bloc precedent ;
- que chaque `currentHash` est encore valide ;
- que la chaine n'a pas ete modifiee manuellement.

Endpoint de verification :

```bash
GET /api/blockchain/verify
```

Reponse attendue :

```json
{
  "valid": true,
  "message": "Private audit chain is valid"
}
```

Pour chaque import CSV, le module de gouvernance cree aussi un bloc blockchain global de type `IMPORT_GOVERNANCE`. Le bloc contient un hash des donnees d'import : nom du fichier, date d'import, nombre total de lignes et score qualite. Cette trace permet de verifier que l'historique des imports et les indicateurs qualite n'ont pas ete alteres manuellement.

## Gouvernance Des Donnees

Le module de gouvernance couvre quatre axes :

- Catalogue : referentiel des sources `VENTES`, `STOCKS`, `PRODUITS`, `AGENCES`, avec owner, frequence de rafraichissement et description.
- Data Quality : calcul automatique de completude, validite, unicite, coherence et score qualite pondere.
- Data Lineage : suivi du cycle `CSV/Excel -> Validation -> ETL -> PostgreSQL -> IA -> Dashboard`.
- Audit des imports : fichier, utilisateur, date, lignes traitees, lignes valides, lignes echouees et statut.

Le dashboard React `Data Governance Dashboard` affiche le score qualite global, les erreurs, doublons, nombre d'imports, historiques qualite/erreurs/imports, catalogue et lineage.

## Scenario De Demonstration

1. Lancer la plateforme :

```bash
docker compose up --build
```

2. Ouvrir le dashboard :

```text
http://localhost:5173
```

3. Creer un compte ou se connecter avec un compte existant.

4. Tester le dashboard global :

- consulter le chiffre d'affaires total ;
- visualiser les ventes journalieres ;
- comparer les ventes par agence ;
- consulter les top produits.

5. Tester les modules metier :

- afficher les agences ;
- afficher les produits ;
- consulter le tableau des ventes ;
- verifier les stocks et stocks critiques.

6. Importer des donnees CSV :

- importer un fichier ventes avec `date,agencyCode,productCode,quantity,unitPrice` ;
- importer un fichier stock avec `date,agencyCode,productCode,quantity,type` ;
- verifier les erreurs ligne par ligne si certaines lignes sont invalides.
- ouvrir `Data Governance Dashboard` pour consulter le score qualite, le catalogue, le lineage et l'audit d'import.

7. Tester l'IA :

- ouvrir la page `Alertes IA` ;
- lancer l'analyse des anomalies de ventes ;
- lancer la prediction de rupture de stock ;
- verifier les alertes creees en base.

8. Tester la blockchain :

- ouvrir la page `Audit blockchain` ;
- consulter les blocs ;
- lancer la verification de chaine ;
- confirmer que la chaine est valide.

## Developpement Local

Backend Spring :

```bash
cd backend-spring
mvn test
mvn spring-boot:run
```

Frontend React :

```bash
cd frontend-react
npm install
npm run dev
```

AI service :

```bash
cd ai-service
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

## Notes

- Les endpoints backend, sauf auth et health, necessitent un header `Authorization: Bearer <token>`.
- Le frontend utilise `VITE_API_URL` et `VITE_AI_SERVICE_URL` au build.
- En environnement de production, changer `JWT_SECRET`, les mots de passe PostgreSQL et la strategie CORS.
