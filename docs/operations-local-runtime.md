# Local packaged runtime

```bash
./scripts/package-frontend.sh
cd backend
./mvnw clean package
java -jar target/markcraft-0.1.0-SNAPSHOT.jar
```

Open `http://127.0.0.1:8080/`. The UI and `/api/v1/vault/**` API share the loopback origin. `server.address=127.0.0.1` keeps the first release off non-loopback interfaces. The vault is external to the jar at `${user.home}/.markcraft/vault` by default; override `markcraft.vault.root` when needed.

Back up by copying the configured vault directory while the app is stopped. Restore by stopping the app, replacing the vault directory contents, and starting the jar again. The vault is authoritative; packaged frontend assets are rebuildable. No database or external service is required.
