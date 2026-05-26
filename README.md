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

