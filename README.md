# DataOps Intelligent Platform

Monorepo with:

- `backend-spring`: Spring Boot API with PostgreSQL, JPA, and JWT security
- `frontend-react`: React app built with Vite
- `ai-service`: FastAPI service for AI/data workflows
- `docker-compose.yml`: local orchestration for PostgreSQL and all services

## Run Locally With Docker

```bash
docker compose up --build
```

Run in background:

```bash
docker compose up --build -d
```

Stop everything:

```bash
docker compose down
```

If you previously launched an older branch with different service names, clean old containers once:

```bash
docker compose down --remove-orphans
```

Reset PostgreSQL data:

```bash
docker compose down -v
```

Services:

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- AI service: http://localhost:8000
- PostgreSQL: localhost:5432

Docker Compose services:

- `postgres`
- `backend-spring`
- `frontend-react`
- `ai-service`

## Default Environment

All variables have development defaults in `docker-compose.yml`.

PostgreSQL:

- Database: `dataops`
- User: `dataops`
- Password: `dataops`
- Volume: `postgres-data`

JWT:

- Secret is configured through `JWT_SECRET`
- The default development secret is defined in `docker-compose.yml`

Ports:

- `FRONTEND_PORT=5173`
- `BACKEND_PORT=8080`
- `AI_SERVICE_PORT=8000`
- `POSTGRES_PORT=5432`

Internal service URLs:

- Backend to PostgreSQL: `jdbc:postgresql://postgres:5432/dataops`
- Backend to AI service: `http://ai-service:8000`

## Backend REST API

Public endpoints:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/health`

Authenticated endpoints:

- `/api/users`
- `/api/agencies`
- `/api/products`
- `/api/sales`
- `/api/stock/movements`
- `/api/stock/levels`
- `/api/import/sales`
- `/api/import/stocks`
- `/api/kpi/overview`
- `/api/kpi/sales-by-agency`
- `/api/kpi/sales-by-product`
- `/api/kpi/critical-stocks`
- `/api/kpi/daily-sales`
- `/api/blockchain`
- `/api/blockchain/verify`
- `/api/alerts`

CSV imports expect headers:

- Sales: `date,agencyCode,productCode,quantity,unitPrice`
- Stock: `date,agencyCode,productCode,quantity,type`
