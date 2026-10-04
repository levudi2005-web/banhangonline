# TiDB SQL bootstrap files

Each numbered module file defines tables only; the auth reference-data seed is
separate. `tidb_full_schema.sql` is the combined, dependency-ordered copy of
these definitions. Review against the PowerDesigner MPD and the target
database before manually applying any file. There are no `DROP` statements,
no `IF NOT EXISTS` masking, and no automatic execution from Spring Boot.

The top-level numbering is a responsibility catalog, not the order in which
all modules can be created. For example, categories must be created before
products because products reference categories.
