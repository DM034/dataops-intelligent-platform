from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

app = FastAPI(title="DataOps AI Service", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class InsightRequest(BaseModel):
    dataset_name: str
    objective: str


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "ai-service"}


@app.post("/insights")
def generate_insights(request: InsightRequest) -> dict[str, str]:
    return {
        "dataset_name": request.dataset_name,
        "objective": request.objective,
        "summary": "AI insight pipeline placeholder is ready.",
    }
