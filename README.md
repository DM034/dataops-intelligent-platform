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

Services:

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- AI service: http://localhost:8000
- PostgreSQL: localhost:5432

## Default Environment

PostgreSQL:

- Database: `dataops`
- User: `dataops`
- Password: `dataops`

JWT:

- Secret is configured through `JWT_SECRET`
- The default development secret is defined in `docker-compose.yml`

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
- `/api/imports/sales`
- `/api/imports/stock`
- `/api/kpis/overview`
- `/api/audit-chain/blocks`
- `/api/audit-chain/events`
- `/api/audit-chain/validate`
- `/api/alerts`

CSV imports expect headers:

- Sales: `agencyCode,sku,quantity,unitPrice,saleDate,reference`
- Stock: `agencyCode,sku,type,quantity,movementDate,reason`
