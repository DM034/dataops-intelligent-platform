# Diapositive 17 - Preuves de tests

- Tests JUnit/Maven : 8 tests executes, 0 echec.
- Docker : backend, frontend, PostgreSQL et IA lances ensemble.
- Scenario valide : connexion admin, import CSV, rapport qualite/lineage, analyse IA.
- Performance : 1k lignes p50 = 40.755 s ; 10k lignes p50 = 421.666 s.
- Limite identifiee : le traitement ligne par ligne garantit la qualite et la tracabilite, mais ralentit les gros imports.
