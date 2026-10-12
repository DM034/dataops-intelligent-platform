# Plan de captures pour le rapport et la présentation

Ce document remplace les captures provisoires du mémoire par des captures réelles de l'application. Les données de référence sont dans `docs/demo/csv`.

## Captures principales du mémoire

| Figure à remplacer | Page de l'application | Données à avoir avant capture | Ce qu'il faut montrer |
| --- | --- | --- | --- |
| Figure 6 - Interface de gouvernance des données | `Data Governance` | Importer `ventes_demo_rapport.csv`, `stocks_demo_rapport.csv`, puis `ventes_demo_erreurs_qualite.csv` | Score qualité global, erreurs, doublons, nombre d'imports, complétude, validité, graphiques qualité/erreurs/imports, flux Data Lineage et catalogue |
| Figure 7 - Interface d'import CSV | `Import CSV` | Garder visible le dernier import, idéalement `ventes_demo_erreurs_qualite.csv` | Barre de progression à 100 %, lignes importées/rejetées, rapport qualité, lineage, boutons rapport/erreurs |
| Figure 8 - Vue fonctionnelle de l'intelligence décisionnelle | `Intelligence IA` | Après imports ventes/stocks | Résumé IA, risques détectés, recommandations et décisions associées |
| Figure D1 - Interface de suivi de la qualité des données | `Qualité des données` | Après import avec erreurs contrôlées | Derniers rapports qualité, score, validité, complétude, erreurs et doublons |
| Figure D2 - Interface de suivi du Data Lineage | `Data Lineage` | Après imports ventes/stocks | Flux `CSV/Excel -> Validation -> ETL -> PostgreSQL -> IA -> Dashboard`, statuts et dates |
| Figure D3 - Administration des utilisateurs | `Utilisateurs` | Connecté avec le compte admin | Liste utilisateurs, rôles, statut actif/inactif, modification de rôle |

## Captures complémentaires utiles

| Usage | Page | Ce qu'il faut montrer |
| --- | --- | --- |
| Diapositive 17 - preuves de tests | `docs/tests/2026-10-08/preuves-tests-T09-T10-T11.docx` ou capture terminal | Résultat JUnit, Docker E2E, performance 1k/10k/50k |
| Défense de l'apport IA | `Benchmark IA` | Comparaison Z-score, IQR, moyenne mobile et méthode recommandée |
| Défense aide à la décision | `Recommandations` | Recommandations classées par priorité, statut et action métier |
| Défense audit technique | `Journal d'activité` | Événements système, filtres, erreurs en priorité |
| Défense traçabilité | endpoint `/api/blockchain/verify` ou page audit blockchain si visible | Chaîne valide et blocs créés après import |

## Bouts de code à capturer pour l'annexe G

### Annexe G.1 - Validation CSV réelle

Ouvrir dans l'IDE :

- `backend-spring/src/main/java/com/example/dataops/service/ImportService.java`
- Capturer autour des lignes `514-565` pour le traitement ventes : lecture CSV, lignes vides ignorées, contrôles obligatoires, doublons, références agence/produit, sauvegarde et bloc blockchain.
- Capturer autour des lignes `827-929` pour les méthodes de validation : colonnes obligatoires, formats, valeurs incohérentes et valeurs aberrantes.

Pourquoi cette capture est pertinente : elle remplace le pseudo-code du rapport par l'implémentation réelle du pipeline d'import.

### Annexe G.2 - Chaînage SHA-256 réel

Ouvrir dans l'IDE :

- `backend-spring/src/main/java/com/example/dataops/service/BlockchainService.java`
- Capturer les lignes `30-44` : création d'un bloc avec `previousHash`, `dataHash` et `currentHash`.
- Capturer les lignes `58-73` : vérification de la continuité de la chaîne.
- Capturer les lignes `98-115` : calcul SHA-256.

Pourquoi cette capture est pertinente : elle montre que la traçabilité n'est pas seulement déclarative, mais vérifiable par recalcul des hashes.

## Ordre de démonstration conseillé

1. Se connecter à l'application.
2. Aller dans `Import CSV`.
3. Importer `ventes_demo_rapport.csv`, puis `stocks_demo_rapport.csv`.
4. Importer `ventes_demo_erreurs_qualite.csv` pour obtenir un cas clair : 97 lignes valides et 3 lignes rejetées.
5. Capturer `Import CSV`.
6. Capturer `Data Governance`.
7. Capturer `Qualité des données`.
8. Capturer `Data Lineage`.
9. Capturer `Intelligence IA`.
10. Capturer `Benchmark IA` et `Recommandations` si la présentation doit renforcer l'aspect MBDS.
11. Capturer les deux fichiers Java pour l'annexe G.

## Point d'attention pour ne pas fragiliser le rapport

La mesure 50k de T11 disponible dans `docs/tests/2026-10-08` est une mesure technique avec rejets qualité. Dans le rapport, il vaut mieux l'écrire comme une mesure de robustesse du pipeline de validation, pas comme un import métier réussi.
