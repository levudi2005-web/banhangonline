# PowerDesigner model workspace

The numbered module directories are reserved for native PowerDesigner model
files. Existing-table DDL under `database/sql/` follows local TiDB schema
exports except for the intentionally unchanged chat tables; verify it against
the live schema, then reverse-engineer the DDL to create the MPD and derive the
MLD/MCD. Store the actual exported
PowerDesigner model files here. The SQL and documentation are not native model
files, and no PowerDesigner binary model is generated or implied by these
placeholders.
