# Local development with XAMPP

The `local` Spring profile is for the XAMPP MariaDB instance on this machine. It
binds the application to `127.0.0.1` and defaults the datasource to
`banhangonline_local` on port `3306`, using XAMPP's `root` account with an empty
password. Override those defaults with `LOCAL_DB_HOST`, `LOCAL_DB_PORT`,
`LOCAL_DB_NAME`, `LOCAL_DB_USERNAME`, and `LOCAL_DB_PASSWORD` when needed.

From the repository root, start the backend with:

```powershell
mvn -f backend/pom.xml -Dspring-boot.run.profiles=local spring-boot:run
```

If `mvn` is not on `PATH`, use the installed Maven command directly:

```powershell
& 'C:\Program Files\apache-maven-3.9.16\bin\mvn.cmd' -f backend/pom.xml -Dspring-boot.run.profiles=local spring-boot:run
```

The local profile defaults to port `8081` so it does not conflict with a
different application already using `8080`. Override it with
`$env:LOCAL_APP_PORT='8082'` in that PowerShell session if needed.

Hibernate uses `validate` and Flyway is disabled, so startup does not create,
alter, or migrate tables. The default profile continues to use the deployment
environment's `DB_*` variables; do not use the `local` profile for deployment.

Store and customer-address map coordinates are nullable `DECIMAL(10,7)` values.
The additive scripts `database/sql/05-store/002_add_store_coordinates.sql` and
`database/sql/02-user/002_add_address_coordinates.sql` are applied manually;
they are not run by Spring. The local `banhangonline_local` database has these
columns. Location selection uses Leaflet 1.9.4, OpenStreetMap tiles, and
Nominatim search. Browser location is requested only after the user chooses
the location button; address queries and selected map coordinates are sent to
OpenStreetMap only when a user searches or selects a map point. Store distance
is shown as a straight-line estimate.

Product image uploads use a local filesystem store in the `local` profile and
are served from `/uploads/`. By default, files live under
`%USERPROFILE%\.banhangonline\uploads` (or `$HOME/.banhangonline/uploads` on
Unix-like systems); override the location with `LOCAL_IMAGE_STORAGE_DIRECTORY`.
The production profile does not enable local uploads. Configure S3 storage for
deployment with the `S3_*` environment variables instead.
