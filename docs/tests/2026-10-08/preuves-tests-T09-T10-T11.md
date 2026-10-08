# Preuves de tests T09, T10 et T11
Date de campagne : 8 octobre 2026.
## T09 - Tests JUnit Maven
Commande executee : `JAVA_HOME=/Users/dm/Library/Java/JavaVirtualMachines/ms-25.0.2/Contents/Home mvn test` depuis `backend-spring`.
Resultat : **BUILD SUCCESS**, 8 tests executes, 0 echec, 0 erreur, 0 ignore.
Rapports : `docs/tests/2026-10-08/t09-maven-test-final.log` et `docs/tests/2026-10-08/maven-surefire-reports/`.
## T10 - Parcours Docker connexion -> import -> analyse
Stack Docker : postgres, backend-spring, frontend-react et ai-service actifs via `docker compose up -d --build`.
Scenario execute : authentification `admin`, import CSV ventes, verification du job, analyse IA anomalies et benchmark IA.
Resultat principal : login HTTP 200, import HTTP 200 avec 5/5 lignes importees, `dataQualityReportId=24`, `dataLineageId=24`, analyse anomalies HTTP 200, benchmark IA HTTP 200.
Trace JSON : `docs/tests/2026-10-08/t10-docker-workflow.json`.
## T11 - Performance import CSV
| Volume | Runs | p50 (s) | p95 (s) | Debit median (lignes/s) | Lignes importees | Lignes rejetees | Observation |
|---:|---:|---:|---:|---:|---:|---:|---|
| 1 000 | 3 | 40.755 | 40.784 | 24.54 | 3000 | 0 | Mesure complete avec lignes valides |
| 10 000 | 2 | 421.666 | 436.761 | 23.75 | 20000 | 0 | Mesure complete avec lignes valides |
| 50 000 | 1 | 1522.006 | 1522.006 | 32.85 | 0 | 50000 | Mesure technique existante sur 50k lignes rejetees par les regles qualite |

Machine et configuration : voir `docs/tests/2026-10-08/machine-configuration.txt`.
Donnees brutes : `docs/tests/2026-10-08/t11-selected-raw.json` et synthese `docs/tests/2026-10-08/t11-performance-summary.json`.
## Texte pret pour le tableau 24
Remplacer les lignes T09, T10 et T11 par les valeurs ci-dessus. Les mentions provisoires ont ete retirees au profit des chemins de preuves et resultats mesures.
## Texte court pour la diapositive 17
Tests finalises : 8 tests JUnit valides, parcours Docker complet valide, import CSV mesure sur 1k/10k lignes valides et 50k lignes en validation technique. La campagne montre un debit median proche de 24 lignes/s sur les imports valides, avec une limite de performance identifiee sur les gros fichiers due aux controles ligne par ligne et a la generation des rapports de qualite.
