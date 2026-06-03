from datetime import date as Date
from enum import Enum

import numpy as np
import pandas as pd
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field, model_validator

app = FastAPI(
    title="DataOps AI Service",
    version="0.2.0",
    description="Service FastAPI pour l'analyse intelligente des ventes, des stocks et des alertes DataOps.",
)

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


class AlertLevel(str, Enum):
    low = "faible"
    medium = "moyen"
    critical = "critique"


class SalePoint(BaseModel):
    date: Date = Field(..., description="Date de vente au format YYYY-MM-DD.")
    agencyCode: str = Field(..., min_length=1, description="Code de l'agence.")
    productCode: str = Field(..., min_length=1, description="Code du produit.")
    quantity: int = Field(..., ge=0, description="Quantite vendue.")
    unitPrice: float = Field(..., ge=0, description="Prix unitaire.")


class SalesAnomalyRequest(BaseModel):
    sales: list[SalePoint] = Field(..., min_length=1, description="Historique des ventes a analyser.")
    zscore_threshold: float = Field(2.0, gt=0, description="Seuil absolu de Z-score pour classer une anomalie.")


class SaleAnomalyResult(BaseModel):
    date: Date
    agencyCode: str
    productCode: str
    quantity: int
    unitPrice: float
    revenue: float
    movingAverage7Days: float
    zScore: float
    anomaly: bool
    alertLevel: AlertLevel


class SalesAnomalyResponse(BaseModel):
    count: int
    anomalyCount: int
    zscoreThreshold: float
    results: list[SaleAnomalyResult]


class StockHistoryPoint(BaseModel):
    date: Date = Field(..., description="Date du niveau de stock au format YYYY-MM-DD.")
    stockLevel: int = Field(..., ge=0, description="Niveau de stock observe.")


class StockPredictionRequest(BaseModel):
    productCode: str = Field(..., min_length=1, description="Code du produit.")
    agencyCode: str = Field(..., min_length=1, description="Code de l'agence.")
    currentStock: int = Field(..., ge=0, description="Stock courant.")
    reorderThreshold: int = Field(10, ge=0, description="Seuil de stock critique.")
    history: list[StockHistoryPoint] = Field(..., min_length=2, description="Historique des niveaux de stock.")


class StockPredictionResponse(BaseModel):
    productCode: str
    agencyCode: str
    currentStock: int
    reorderThreshold: int
    averageDailyConsumption: float
    predictedDaysToStockout: int | None
    stockoutDate: Date | None
    alertLevel: AlertLevel
    recommendation: str


class AlertScoreRequest(BaseModel):
    anomalyScore: float = Field(0, ge=0, description="Score ou Z-score d'anomalie.")
    daysToStockout: int | None = Field(None, ge=0, description="Nombre de jours avant rupture prevue.")
    currentStock: int | None = Field(None, ge=0, description="Stock courant.")
    reorderThreshold: int | None = Field(None, ge=0, description="Seuil critique de stock.")
    businessImpact: float = Field(1.0, ge=0, le=3, description="Impact metier de 0 a 3.")

    @model_validator(mode="after")
    def validate_stock_threshold(self) -> "AlertScoreRequest":
        if (self.currentStock is None) != (self.reorderThreshold is None):
            raise ValueError("currentStock et reorderThreshold doivent etre fournis ensemble.")
        return self


class AlertScoreResponse(BaseModel):
    score: float
    alertLevel: AlertLevel
    reasons: list[str]


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


@app.post(
    "/ai/anomalies/sales",
    response_model=SalesAnomalyResponse,
    summary="Detecter les anomalies de ventes",
    description="Calcule le chiffre d'affaires, la moyenne mobile sur 7 jours et le Z-score des ventes.",
)
def detect_sales_anomalies(request: SalesAnomalyRequest) -> SalesAnomalyResponse:
    rows = [sale.model_dump() for sale in request.sales]
    frame = pd.DataFrame(rows)
    frame["date"] = pd.to_datetime(frame["date"])
    frame["revenue"] = frame["quantity"] * frame["unitPrice"]
    frame = frame.sort_values(["productCode", "agencyCode", "date"]).reset_index(drop=True)

    frame["movingAverage7Days"] = (
        frame.groupby(["productCode", "agencyCode"])["quantity"]
        .transform(lambda values: values.rolling(window=7, min_periods=1).mean())
        .astype(float)
    )

    quantity_std = float(frame["quantity"].std(ddof=0))
    quantity_mean = float(frame["quantity"].mean())
    if quantity_std == 0:
        frame["zScore"] = 0.0
    else:
        frame["zScore"] = (frame["quantity"] - quantity_mean) / quantity_std

    frame["anomaly"] = frame["zScore"].abs() >= request.zscore_threshold
    frame["alertLevel"] = frame["zScore"].abs().apply(classify_anomaly)

    results = [
        SaleAnomalyResult(
            date=row.date.date(),
            agencyCode=row.agencyCode,
            productCode=row.productCode,
            quantity=int(row.quantity),
            unitPrice=round(float(row.unitPrice), 2),
            revenue=round(float(row.revenue), 2),
            movingAverage7Days=round(float(row.movingAverage7Days), 2),
            zScore=round(float(row.zScore), 4),
            anomaly=bool(row.anomaly),
            alertLevel=row.alertLevel,
        )
        for row in frame.itertuples(index=False)
    ]

    return SalesAnomalyResponse(
        count=len(results),
        anomalyCount=sum(1 for result in results if result.anomaly),
        zscoreThreshold=request.zscore_threshold,
        results=results,
    )


@app.post(
    "/ai/stock/predict",
    response_model=StockPredictionResponse,
    summary="Prevoir une rupture de stock",
    description="Estime la consommation quotidienne moyenne et la date probable de rupture.",
)
def predict_stockout(request: StockPredictionRequest) -> StockPredictionResponse:
    history = sorted(request.history, key=lambda item: item.date)
    levels = np.array([point.stockLevel for point in history], dtype=float)
    drops = np.maximum(levels[:-1] - levels[1:], 0)
    average_daily_consumption = round(float(drops.mean()), 2) if len(drops) else 0.0

    if average_daily_consumption <= 0:
        days_to_stockout = None
        stockout_date = None
        alert_level = AlertLevel.low
        recommendation = "Stock stable ou en hausse. Continuer la surveillance."
    else:
        days_to_stockout = int(np.floor(request.currentStock / average_daily_consumption))
        stockout_date = pd.Timestamp.today().normalize() + pd.Timedelta(days=days_to_stockout)
        alert_level = classify_stockout(days_to_stockout, request.currentStock, request.reorderThreshold)
        recommendation = build_stock_recommendation(alert_level)

    return StockPredictionResponse(
        productCode=request.productCode,
        agencyCode=request.agencyCode,
        currentStock=request.currentStock,
        reorderThreshold=request.reorderThreshold,
        averageDailyConsumption=average_daily_consumption,
        predictedDaysToStockout=days_to_stockout,
        stockoutDate=stockout_date.date() if stockout_date is not None else None,
        alertLevel=alert_level,
        recommendation=recommendation,
    )


@app.post(
    "/ai/alerts/score",
    response_model=AlertScoreResponse,
    summary="Classifier une alerte",
    description="Combine anomalie, risque de rupture et impact metier pour retourner faible, moyen ou critique.",
)
def score_alert(request: AlertScoreRequest) -> AlertScoreResponse:
    score = min(request.anomalyScore / 3, 2.5) + request.businessImpact
    reasons: list[str] = []

    if request.anomalyScore >= 3:
        reasons.append("Anomalie de vente forte.")
    elif request.anomalyScore >= 2:
        reasons.append("Anomalie de vente moderee.")

    if request.daysToStockout is not None:
        if request.daysToStockout <= 3:
            score += 3
            reasons.append("Rupture de stock probable sous 3 jours.")
        elif request.daysToStockout <= 7:
            score += 2
            reasons.append("Risque de rupture sous 7 jours.")
        elif request.daysToStockout <= 14:
            score += 1
            reasons.append("Risque de rupture a surveiller sous 14 jours.")

    if request.currentStock is not None and request.reorderThreshold is not None:
        if request.currentStock <= request.reorderThreshold:
            score += 2
            reasons.append("Stock courant inferieur ou egal au seuil critique.")
        elif request.currentStock <= request.reorderThreshold * 1.5:
            score += 1
            reasons.append("Stock courant proche du seuil critique.")

    if not reasons:
        reasons.append("Aucun signal critique detecte.")

    score = round(float(score), 2)
    return AlertScoreResponse(score=score, alertLevel=classify_score(score), reasons=reasons)


def classify_anomaly(zscore: float) -> AlertLevel:
    absolute_score = abs(float(zscore))
    if absolute_score >= 3:
        return AlertLevel.critical
    if absolute_score >= 2:
        return AlertLevel.medium
    return AlertLevel.low


def classify_stockout(days_to_stockout: int, current_stock: int, reorder_threshold: int) -> AlertLevel:
    if current_stock <= reorder_threshold or days_to_stockout <= 3:
        return AlertLevel.critical
    if days_to_stockout <= 7:
        return AlertLevel.medium
    return AlertLevel.low


def classify_score(score: float) -> AlertLevel:
    if score >= 6:
        return AlertLevel.critical
    if score >= 3:
        return AlertLevel.medium
    return AlertLevel.low


def build_stock_recommendation(alert_level: AlertLevel) -> str:
    recommendations = {
        AlertLevel.critical: "Reapprovisionner immediatement et verifier les ventes recentes.",
        AlertLevel.medium: "Planifier un reapprovisionnement prioritaire.",
        AlertLevel.low: "Surveiller le stock dans le cycle normal.",
    }
    return recommendations[alert_level]
