# MarkCraft

MarkCraft is a localhost Markdown workspace. It provides a browser based Angular editor and preview, with a Spring Boot API that stores Markdown files in a folder vault on the machine running the backend. The packaged application needs no database or external service.

## Features

- Organize Markdown documents and folders in a vault; create, rename, delete, and upload files.
- Edit Markdown with autosave, rendered preview, formatting tools, find and replace, templates, and table of contents.
- Inspect and edit the supported OKF frontmatter fields.
- Export Markdown or rendered HTML and adjust workspace preferences.
- Keep vault paths bounded to the configured vault directory.

## Requirements

- Java 21
- Node.js 22.12 or newer and npm

## Run in development

Start the backend from the repository root:

```bash
cd backend
./mvnw spring-boot:run
```

In another terminal, start the Angular development server:

```bash
cd frontend
npm ci
npm start
```

Open the local URL printed by Angular (normally `http://127.0.0.1:4200`). The development proxy forwards `/api/v1/vault` requests to the backend at `http://127.0.0.1:8080`.

## Build and run the packaged app

From the repository root:

```bash
./scripts/package-frontend.sh
cd backend
./mvnw clean package
java -jar target/markcraft-0.1.0-SNAPSHOT.jar
```

Open <http://127.0.0.1:8080/>. Spring Boot serves both the UI and API from the same local origin. The backend binds to `127.0.0.1` by default.

## Vault storage

The default vault is `${user.home}/.markcraft/vault`. It is separate from the application and starts empty. To use another directory, pass a Spring property when starting the jar:

```bash
java -Dmarkcraft.vault.root=/path/to/vault -jar target/markcraft-0.1.0-SNAPSHOT.jar
```

The vault contains the Markdown files and folders you create or upload. Back it up by copying the configured vault directory while MarkCraft is stopped; restore by replacing its contents while the app is stopped.

## Project layout

- `frontend/` — Angular application and development proxy.
- `backend/` — Spring Boot application, Maven Wrapper, and packaged frontend assets.
- `contracts/` — OpenAPI and JSON Schema contracts.
- `docs/` — requirements, architecture, implementation strategy, design, and operations documentation.
- `scripts/package-frontend.sh` — builds Angular and copies the browser output into the backend resources.

See [docs/operations-local-runtime.md](docs/operations-local-runtime.md) for local runtime and backup notes. The API contract is in [contracts/openapi/v1.yaml](contracts/openapi/v1.yaml).
