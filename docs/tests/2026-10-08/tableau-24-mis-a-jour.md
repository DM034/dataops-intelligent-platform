# Tableau 24 - Tests et validation

| ID | Test | Preuve | Resultat | Statut |
|---|---|---|---|---|
| T09 | Tests unitaires backend JUnit/Maven | `t09-maven-test-final.log` et `maven-surefire-reports/` | 8 tests executes, 0 echec, 0 erreur | Valide |
| T10 | Parcours Docker connexion -> import -> analyse | `t10-docker-workflow.json`, `t10-docker-ps.txt` | Login 200, import 5/5, analyse IA 200, benchmark IA 200 | Valide |
| T11 | Performance import CSV | `t11-performance-summary.json`, `t11-selected-raw.json` | 1k p50 40.755s, 10k p50 421.666s, 50k technique 1522.006s | Valide avec limite identifiee |
