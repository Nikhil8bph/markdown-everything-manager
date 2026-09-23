---
name: task-browser-verification
description: Verify one implementation task's user flow in a real browser against approved acceptance criteria, UX handoff, and API contracts. Capture reproducible failures and evidence; use for task-scoped UI verification, not feature implementation or contract changes.
---

# Task Browser Verification

## Scope and prerequisites

Read `AGENTS.md`, the selected `TASK-*` in `docs/04-implementation-strategy.md`, relevant approved requirements/contracts, and the scope's `Ready for Angular` handoff in `docs/DESIGN.md`. Identify the target URL, test environment, acceptance criteria, and permitted test accounts/data. Handle one task per run; do not invent behavior or implement fixes during a verification-only request.

If approval gates, a task, or a required design scope are missing, inspect safely but report verification as blocked. Keep upstream documents, contracts, and the design handoff unchanged.

## Browser and environment discovery

Discover available browser tools and their schemas before use. Prefer the repository's configured browser automation capability (such as Playwright or Chrome DevTools); use a documented installed alternative when needed. Do not guess tool names or claim a browser pass from an HTTP request alone. If no usable browser exists, report the limitation and any static/API evidence separately.

Reuse a running intended dev/test server where possible. If startup is needed and authorized, read repository commands and configuration; track processes started by this run for cleanup. Never stop unrelated processes or reset shared databases. Use isolated fixtures and existing test accounts. Production writes, real purchases, messages, and destructive actions require explicit authorization for those effects; use a sandbox when available.

## Verification procedure

1. Build a concise scenario list mapping each acceptance criterion to setup, action, and observable result.
2. Run the primary flow and relevant loading, empty, validation, error, permission, and retry states. Use controlled mocks only where needed and label mocked evidence; it does not prove backend integration.
3. Check actual browser network responses against the approved method/path/status and payload behavior. Correlate with backend or persisted state when permitted and required by the task. A success toast alone is insufficient proof of a write.
4. Inspect console errors and failed requests, separating pre-existing noise from task regressions. Check keyboard access, accessible names, focus, and approved responsive sizes for changed screens. Screenshots alone do not establish accessibility compliance.
5. Capture minimal reproducible steps and expected/actual results. Sanitize screenshots, traces, URLs, and logs before saving; exclude tokens and private data. Store requested persistent evidence under `docs/verification/<TASK-ID>/`.
6. Clean up only test data/resources created by this run where authorized, close owned sessions, and report any leftovers.

## Evidence and status

Report each scenario as `Pass`, `Fail`, or `Blocked`, with evidence, environment/browser, mocked dependencies, and reproduction steps for failures. Distinguish checks run from checks proposed. Name the owning stage for requirement, design, or contract conflicts.

If this run owns the task status, re-read and update only that row with verification evidence. Set `Completed` only if all task acceptance criteria and the full DoD are satisfied; a browser pass does not complete unfinished backend or infrastructure work. Preserve unrelated notes. Delegated verification reports to its coordinating parent, which owns the status transition.
