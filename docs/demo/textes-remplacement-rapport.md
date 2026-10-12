# Textes courts à insérer avec les captures

## Figure 6 - Interface de gouvernance des données

Cette capture montre la couche de gouvernance appliquée aux données importées. Le tableau de bord affiche le score qualité global, les erreurs détectées, les doublons, le nombre d'imports et les indicateurs de complétude et de validité. Les graphiques permettent de suivre l'évolution de la qualité dans le temps, tandis que le catalogue et le lineage relient les indicateurs aux sources de données exploitées.

## Figure 7 - Interface d'import CSV

Cette capture illustre le traitement d'un fichier CSV depuis l'interface utilisateur. L'application contrôle les colonnes obligatoires, ignore les lignes vides, valide les formats et distingue les lignes acceptées des lignes rejetées. Le résultat de l'import alimente automatiquement les KPI, la qualité des données, le Data Lineage et la blockchain privée.

Exemple à commenter pendant la soutenance : le fichier `ventes_demo_erreurs_qualite.csv` contient 100 lignes métier, dont 97 lignes valides et 3 lignes rejetées. Cela permet de montrer que la plateforme ne se contente pas d'importer des données : elle mesure leur fiabilité avant de les utiliser pour la décision.

## Figure 8 - Vue fonctionnelle de l'intelligence décisionnelle

Cette capture présente la partie analytique de la plateforme. Les ventes et les stocks importés sont exploités pour détecter des risques, comparer des méthodes simples d'analyse et produire des recommandations métier explicables. L'objectif n'est pas de remplacer l'outil opérationnel existant, mais de transformer ses données en informations contrôlées et utilisables pour le pilotage.

## Figure D1 - Suivi de la qualité des données

Cette capture détaille les rapports qualité générés à chaque import. Les métriques de complétude, validité, unicité et cohérence permettent d'expliquer pourquoi un indicateur peut être considéré fiable ou non. Cette vue renforce l'intérêt MBDS du projet : les décisions ne reposent pas seulement sur des données disponibles, mais sur des données évaluées.

## Figure D2 - Data Lineage

Cette capture montre le parcours des données depuis le fichier CSV jusqu'au dashboard : validation, transformation, stockage PostgreSQL, analyse IA et restitution. Le lineage facilite l'audit, car il permet de comprendre d'où vient une information et par quelles étapes elle est passée.

## Figure D3 - Administration des utilisateurs

Cette capture présente la gestion des rôles et des accès. Elle montre que les fonctionnalités sensibles, comme la gouvernance, les exports, les recommandations ou l'administration, sont intégrées dans une logique de contrôle d'accès.

## Annexe G.1 - Validation CSV réelle

Remplacer le pseudo-code par une capture de `ImportService.java`. Les zones importantes sont :

- lignes `514-565` : boucle d'import des ventes, contrôle des lignes vides, validation, sauvegarde et ajout d'un bloc blockchain ;
- lignes `827-929` : validation des colonnes, champs obligatoires, formats, valeurs incohérentes et valeurs aberrantes.

Phrase d'accompagnement :

L'extrait ci-dessus montre que chaque ligne CSV est contrôlée avant persistance. Les lignes valides sont enregistrées, les erreurs restent rattachées à leur numéro de ligne et l'import validé génère une trace blockchain.

## Annexe G.2 - Chaînage SHA-256 réel

Remplacer le pseudo-code par une capture de `BlockchainService.java`. Les zones importantes sont :

- lignes `30-44` : création du bloc et rattachement au hash précédent ;
- lignes `58-73` : vérification de la chaîne ;
- lignes `98-115` : calcul SHA-256.

Phrase d'accompagnement :

L'extrait montre le calcul d'une empreinte SHA-256 à partir des informations du bloc et du hash précédent. Une modification d'un bloc rompt la continuité de la chaîne et peut être détectée par la méthode de vérification.

## Diapositive 17 - Tests

Remplacer les formulations provisoires par :

Les preuves de tests ont été consolidées dans `docs/tests/2026-10-08`. La campagne confirme l'exécution des tests JUnit, le parcours Docker de bout en bout et les mesures de performance sur 1 000, 10 000 et 50 000 lignes. La mesure 50 000 lignes correspond à une épreuve de robustesse du pipeline de validation, avec rejets qualité consignés.
