---
name: angular-enterprise-architect
description: Design, review, and scaffold scalable Angular applications using standalone components, signals, lazy-loaded feature slices, typed environments, functional interceptors and guards, and OnPush change detection.
---

# Angular Enterprise Architect

Use this skill for Angular frontend architecture, feature scaffolding, component design, routing, API integration, authentication UI, feature flags, and frontend code review.

## Implementation-strategy workflow

Before changing Angular code, read `AGENTS.md` and `docs/04-implementation-strategy.md`. Resolve the exact frontend `TASK-*` task (for example `TASK-WEB-*`), its dependencies, acceptance criteria, subtasks, and current row in the task-status table. Use the task as the implementation scope.

Then read the approved BRD at `docs/01-business-requirements.md` and the relevant sections of `docs/02-application-development-plan.md`, `docs/03-api-contract-integration-specification.md`, and the referenced files under `contracts/`. The planning documents define architecture and security; OpenAPI, AsyncAPI, and JSON Schema files define wire contracts. Do not invent routes, DTOs, event payloads, permissions, or feature behavior when a task or contract is silent.

Verify approval evidence for all four planning stages and the relevant contracts before implementation; file existence alone is insufficient. Check the implementation-strategy document status and stage gate before implementation. If `docs/04-implementation-strategy.md` is missing, its status is not approved, the task is missing, or a dependency is incomplete, stop at a non-mutating assessment/plan. Do not scaffold code or change task status to `In Progress` while the stage gate is open. If the requested work maps to multiple tasks and the correct task cannot be determined unambiguously, ask for the task ID.

Task lifecycle is recorded in the centralized table in `docs/04-implementation-strategy.md`:

- Set the exact task row to `In Progress` immediately before the first implementation change, and record the Angular skill as owner when appropriate.
- Set it to `Completed` only after all task acceptance criteria, Angular subtasks, relevant contract tests, accessibility checks, and the applicable Definition of Done are satisfied. Completing only the frontend portion does not complete a cross-disciplinary task.
- Use `Blocked` with a concise reason and evidence when approval, dependencies, contracts, or validation prevent progress. Never claim `Completed` when tests or required evidence are missing.
- Update only the matching row, preserve existing notes from other workstreams, and include dates/evidence without rewriting unrelated sections. Re-read the row before updating it to avoid overwriting concurrent progress.

The status update is part of the task workflow, not a substitute for implementation evidence. Keep API models, endpoint constants, auth behavior, and UI behavior aligned with the approved contracts and task acceptance criteria.

## UX design handoff

Before implementing any UI task, read `docs/DESIGN.md` produced by `ux-design-stitch`. The design handoff is required for UI work and must have status `Ready for Angular` for the requested scope. If the file is missing, draft, superseded, or does not cover the task's screens and flow, stop and report the missing UX handoff; do not invent a replacement design in Angular code.

Treat `docs/DESIGN.md` as a read-only UX source of truth during Angular implementation. Map the task's screens, states, interaction behavior, responsive rules, reusable components, visual tokens, and accessibility requirements to the handoff, while using the approved API contracts for transport behavior. If the implementation reveals a UX conflict, return it to `ux-design-stitch`; if it reveals a business, architecture, or wire-contract conflict, return it to the appropriate earlier workflow stage.

## Read-only upstream artifacts

Treat these as immutable reference inputs in every mode, including planning, implementation, review, testing, and status updates:

- `docs/01-business-requirements.md`
- `docs/02-application-development-plan.md`
- `docs/03-api-contract-integration-specification.md`
- every file and subdirectory under `contracts/`

Read them as needed, but never create, edit, delete, rename, reformat, regenerate, or otherwise write to them. Do not fix a requirement, architecture, API, event, or schema discrepancy in place. Report the exact file and issue, then block or return the decision to the appropriate earlier workflow stage. The only planning artifact this skill may update during normal task execution is the matching status row in `docs/04-implementation-strategy.md`, as described above.

## Single-task execution and delegation

- One agent run owns exactly one `TASK-*` from `docs/04-implementation-strategy.md`. Read dependencies for context, but do not implement sibling tasks in the same run or update multiple task rows.
- If the request spans multiple tasks, identify the first dependency-ready task and ask the user to split the remainder, or coordinate separate task runs. Do not silently widen scope.
- When subagent delegation is available and authorized, prefer one isolated subagent per task. Give it the exact task ID, acceptance criteria, relevant source paths, and validation expectations. Do not run agents concurrently against the same files or status row.
- The coordinating parent owns the task-status row. A delegated subagent reports changes, tests, and blockers but does not independently change that row; the parent sets `In Progress`, `Blocked`, or `Completed` after verifying the report. An agent working directly on the task follows the normal lifecycle rules above.

## Reuse, SOLID, and clean code

- Search existing Angular workspace code, shared components, services, interceptors, models, generated transport types, and test utilities before creating anything new. Extend or compose an existing abstraction when it already satisfies the task; do not duplicate behavior under a new name.
- Keep components, services, state, and utilities focused on one responsibility. Prefer composition and narrow typed interfaces over god components, global mutable state, inheritance-heavy designs, or speculative frameworks.
- Keep feature code open to extension through configuration/typed strategies where variation is real, substitutable through stable contracts, and independent of concrete infrastructure. Do not force abstractions for one implementation.
- Keep view models separate from transport models when their responsibilities differ; centralize cross-cutting auth, correlation, error, ETag, and idempotency behavior in the existing platform services.
- Use clear names, small cohesive functions, explicit state transitions, no dead code or magic values, and tests that protect observable behavior. Refactor duplication only within the current task boundary.

## Application structure

Use the repository’s approved topology and installed Angular version. The layout, routes, and flags below are illustrative; do not introduce example features absent from the contracts. Verify CLI output against the required four-file component naming convention, since generator defaults may differ by version.

Prefer a standalone, domain-oriented structure:

```text
src/
├── environments/
│   ├── environment.ts
│   ├── environment.development.ts
│   └── environment.prod.ts
└── app/
    ├── core/
    │   ├── guards/
    │   ├── interceptors/
    │   ├── services/
    │   └── constants/
    ├── shared/
    │   ├── components/
    │   ├── directives/
    │   ├── pipes/
    │   └── models/
    ├── layout/
    │   ├── header/
    │   ├── footer/
    │   ├── sidebar/
    │   └── main-layout/
    └── features/
        └── <feature-name>/
            ├── components/
            ├── pages/
            ├── services/
            ├── models/
            └── <feature>.routes.ts
```

- `core` contains singleton application infrastructure only.
- `shared` contains reusable, domain-neutral UI and utilities.
- `layout` contains the application shell.
- `features` contain lazy-loaded domain slices and their feature-specific data access.
- Keep smart/container route pages separate from reusable presentational components.
- Do not create a global shared module that becomes a dumping ground for feature logic.

## Component file boundaries

Create every new component and route page as a four-file unit with colocated implementation, template, styles, and tests:

```text
<component-name>/
├── <component-name>.component.ts
├── <component-name>.component.html
├── <component-name>.component.scss
└── <component-name>.component.spec.ts
```

- Keep component metadata, signals, dependency injection, and presentation orchestration in `.component.ts`.
- Keep markup and Angular template control flow in `.component.html`; do not use inline templates.
- Keep component-scoped styles in `.component.scss`; do not use inline styles or place feature-specific styling in global stylesheets.
- Keep component behavior, input/output, accessibility, and state-rendering tests in the colocated `.component.spec.ts`.
- Colocate `.spec.ts` files for services, guards, interceptors, directives, pipes, and other executable units. Pure interfaces and type-only model files do not need empty placeholder specs.
- Split files by responsibility instead of using oversized components. A separate HTML/SCSS file is not a substitute for extracting reusable UI or moving workflows into feature services/state.

When touching an existing inline component within the current task scope, move its template and styles into the four-file layout if doing so is safe and task-relevant. Do not perform unrelated repository-wide conversion.

## Standalone and modern Angular defaults

- Use standalone components, directives, and pipes for new code.
- Use `ChangeDetectionStrategy.OnPush` on every component unless there is a documented reason not to.
- Prefer `inject()` over constructor injection in new services, guards, interceptors, and components.
- Prefer signal inputs and outputs (`input()`, `input.required()`, `output()`, and `model()`) over decorator-based inputs and outputs in new components.
- Use built-in control flow (`@if`, `@for`, and `@defer`) instead of legacy structural directives in new templates.
- Track repeated items by stable identity: `@for (item of items; track item.id)`.
- Keep templates declarative. Move expensive or derived logic into computed signals or pipes.
- Avoid direct subscriptions in components. Prefer the `async` pipe, `take(1)`, or `takeUntilDestroyed()`.

## Environment and API configuration

Keep API base URLs, third-party public identifiers, feature flags, and environment-specific toggles in environment files. Do not hardcode URLs inside components or services.

Example:

```typescript
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api/v1',
  authUrl: 'http://localhost:8080/api/v1/auth',
  features: {
    enableAnalytics: false,
    enableBetaDashboard: true,
    enableExportPdf: true,
    mockData: true
  }
} as const;
```

Centralize endpoint construction in a typed constant:

```typescript
import { environment } from '../../../environments/environment';

export const API_ENDPOINTS = {
  auth: {
    login: `${environment.authUrl}/login`,
    register: `${environment.authUrl}/register`,
    refresh: `${environment.authUrl}/refresh-token`,
    logout: `${environment.authUrl}/logout`
  },
  users: {
    base: `${environment.apiBaseUrl}/users`,
    byId: (id: string) => `${environment.apiBaseUrl}/users/${id}`
  }
} as const;
```

Never commit private secrets to environment files. Public browser configuration is not a secret; server credentials must not be placed in the frontend bundle.

## Angular CLI scaffolding

Use precise CLI commands and place generated files in the intended domain directory:

```bash
ng g c features/<feature-name>/pages/<page-name> --standalone --change-detection=OnPush --style=scss --inline-template=false --inline-style=false --skip-tests=false
ng g c features/<feature-name>/components/<component-name> --standalone --change-detection=OnPush --style=scss --inline-template=false --inline-style=false --skip-tests=false
ng g c shared/components/<component-name> --standalone --change-detection=OnPush --style=scss --inline-template=false --inline-style=false --skip-tests=false
ng g c layout/<component-name> --standalone --change-detection=OnPush --style=scss --inline-template=false --inline-style=false --skip-tests=false
ng g s core/services/<service-name>/<service-name>
ng g s features/<feature-name>/services/<service-name>
ng g interceptor core/interceptors/auth --functional
ng g interceptor core/interceptors/error-handler --functional
ng g guard core/guards/auth --functional
ng g guard core/guards/role --functional
ng g guard core/guards/feature-toggle --functional
ng g directive shared/directives/feature-toggle --standalone
ng g pipe shared/pipes/<pipe-name> --standalone
ng g interface features/<feature-name>/models/<model-name>.model
```

Do not generate modules or NgModule-based components for a standalone application unless the existing codebase requires them.

## Routing and lazy loading

Keep feature routes close to their feature and lazy-load them from the application route configuration. Protect routes with functional guards and keep authorization decisions in a dedicated policy/service rather than scattering role checks through templates.

Use route-level providers for feature-specific dependencies when they do not need application-wide lifetime. Use resolvers sparingly; prefer loading state in the feature page when it makes the flow easier to retry and test.

## Signals and feature toggles

Use signals for local UI state and derived state. Keep server state in the chosen data-access/cache layer and avoid duplicating the same state in unrelated services.

Feature keys should be typed from the environment configuration:

```typescript
export type FeatureKey = keyof typeof environment.features;
```

A feature-toggle service can expose both synchronous checks and computed signals:

```typescript
@Injectable({ providedIn: 'root' })
export class FeatureToggleService {
  private readonly flags = signal<Record<FeatureKey, boolean>>({ ...environment.features });

  isEnabled(feature: FeatureKey): boolean {
    return !!this.flags()[feature];
  }

  isEnabledSignal(feature: FeatureKey) {
    return computed(() => !!this.flags()[feature]);
  }

  setFeature(feature: FeatureKey, enabled: boolean): void {
    this.flags.update(current => ({ ...current, [feature]: enabled }));
  }
}
```

Use feature toggles consistently in route guards, component rendering, and service behavior. Treat client-side feature flags as presentation controls, not security authorization.

## Functional HTTP interceptors

Register interceptors with `provideHttpClient(withInterceptors([...]))`.

Authentication interceptor:

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();

  if (!token || !auth.isTrustedApiUrl(req.url) || req.headers.has('Authorization')) {
    return next(req);
  }

  return next(req.clone({
    headers: req.headers.set('Authorization', `Bearer ${token}`)
  }));
};
```

`isTrustedApiUrl` must resolve relative URLs against the configured application origin and compare the exact approved API origin and path boundary. Do not use string-prefix hostname checks. Exclude third-party and signed object-storage URLs; test these exclusions and SSR URL resolution when applicable. Reuse the existing URL policy helper.

An error interceptor should preserve the original error for callers while translating the API error envelope into a user-facing notification. Avoid showing sensitive server details directly to users.

For refresh-token handling, prevent concurrent refresh requests from creating multiple token rotations. Queue or share the refresh request and retry the original requests only after a successful refresh. On refresh failure, clear credentials and navigate to login.

## Functional guards

Use functional guards for authentication, role access, feature availability, and unsaved changes. A guard should return a boolean, `UrlTree`, or observable/promise of those values; it should not perform unrelated data mutation.

Example feature guard shape:

```typescript
export const featureToggleGuard = (
  feature: FeatureKey,
  redirectTo = '/'
): CanActivateFn => {
  return () => {
    const featureService = inject(FeatureToggleService);
    const router = inject(Router);

    return featureService.isEnabled(feature)
      ? true
      : router.parseUrl(redirectTo);
  };
};
```

Route guards improve navigation behavior but are not a security boundary. The backend must enforce authorization independently.

## Component design

- Page components coordinate route state, loading, errors, and feature services.
- Presentational components receive data and emit user intent; they should not know API URLs or authentication storage details.
- Prefer typed models and discriminated unions for complex UI states.
- Expose loading, empty, error, and retry states explicitly.
- Use accessible semantic HTML, labels, keyboard interactions, and focus management for dialogs and navigation.
- Avoid putting business workflows into templates or shared UI components.

## API models and response handling

Define reusable interfaces for the backend response envelope and pagination metadata. Keep API DTOs separate from view models when the UI needs a different shape.

Normalize API errors in one place so feature services do not each parse status codes and message formats independently. Preserve correlation/request IDs when the backend provides them.

## Review checklist

- Every new component/page has separate `.component.ts`, `.component.html`, `.component.scss`, and `.component.spec.ts` files; inline templates/styles and skipped component tests are absent.
- Tests for other executable Angular units are colocated as `.spec.ts` files; empty tests are not created for type-only models.
- Feature routes are lazy-loaded and feature code is not placed in `core`.
- New components are standalone and use `OnPush`.
- Templates use modern control flow and stable tracking.
- API URLs are sourced from environment configuration and endpoint constants.
- Authentication, error, and loading behavior is handled through functional interceptors.
- Guards are functional and do not replace backend authorization.
- Signals are used for local/derived UI state without creating duplicate server state.
- Subscriptions are managed with `async`, `take(1)`, or `takeUntilDestroyed()`.
- Feature flags are typed and are not treated as security controls.
- Passwords, private keys, and server credentials are absent from the client bundle.
- Components are accessible and expose loading, empty, error, and retry states.
