# Database framework

This directory is a proposed, manually applied TiDB schema framework. It is
not a migration runner and none of these SQL files have been executed.

Source-of-truth flow:

```text
PowerDesigner MCD → MLD → MPD → reviewed SQL → manual TiDB deployment
```

Until PowerDesigner models are created and reviewed, the SQL is a design
baseline, not an assertion that the production schema exists. Do not apply the
combined file to a database that may already contain tables.

- `powerdesigner/` contains placeholders for future native PowerDesigner
  models; no `.cdm`, `.ldm`, or `.pdm` file is fabricated.
- `sql/` contains module DDL, the existing auth reference-data seed, and the
  dependency-ordered combined bootstrap file.
- Design decisions and current Java/Flyway alignment are recorded in
  `docs/database/`.

The AI directory is reserved, but AI tables are omitted because the current
application has no AI persistence architecture. No sample customer, order,
product, or credential data is included.
