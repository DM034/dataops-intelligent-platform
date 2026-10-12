# CSV de démonstration pour le rapport

Ces fichiers utilisent les agences et produits déjà initialisés par `DemoDataInitializer` :

- Agences : `TANA`, `TMM`, `DIE`, `MJN`, `TOL`, `FNR`.
- Produits : `RIZ-MAKALI`, `VAN-BIO`, `CAFE-ROB`, `GIROFLE`, `HUILE-COCO`, `SAVON-ALA`, `MIEL-MADA`, `LITCHI`.

## Fichiers

- `ventes_demo_rapport.csv` : ventes valides pour alimenter KPI, IA, gouvernance et blockchain.
- `stocks_demo_rapport.csv` : mouvements de stock valides, avec quelques sorties sur `VAN-BIO` pour rendre les stocks critiques visibles.
- `ventes_demo_erreurs_qualite.csv` : 100 lignes dont 97 valides et 3 volontairement rejetées :
  - produit manquant ;
  - quantité négative ;
  - prix unitaire aberrant.

## Ordre conseillé pour la démonstration

1. Importer `ventes_demo_rapport.csv`.
2. Importer `stocks_demo_rapport.csv`.
3. Importer `ventes_demo_erreurs_qualite.csv` pour montrer le score qualité et les erreurs ligne par ligne.
4. Capturer ensuite les pages listées dans `docs/demo/plan-captures-rapport.md`.

Si un fichier est importé plusieurs fois dans la même base, les doublons peuvent être rejetés. Pour une capture propre, importer chaque fichier une seule fois ou réinitialiser la base Docker avant la démonstration.
