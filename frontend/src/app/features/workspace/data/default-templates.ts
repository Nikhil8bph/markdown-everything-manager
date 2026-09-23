export interface TemplateItem {
  id: string;
  title: string;
  description: string;
  icon: string;
  content: string;
}

export const DEFAULT_TEMPLATES: TemplateItem[] = [
  {
    id: 'okf-concept',
    title: 'OKF v0.2 Knowledge Concept',
    description: 'Canonical concept document conforming to Open Knowledge Format v0.2',
    icon: 'psychology',
    content: `---
type: concept
title: "Distributed Cache Architecture"
description: "Core architectural specification for multi-region knowledge caching"
status: stable
tags:
  - architecture
  - caching
  - okf-v0.2
sources:
  - https://openknowledgeformat.com/specs/v0.2
verified:
  by: lead-architect
  at: "2026-09-22"
stale_after: "2027-01-01"
---

# Distributed Cache Architecture

## Overview
A distributed cache pools RAM across multiple commodity servers to accelerate reads and reduce backend load.

## Architectural Principles
1. **Low Latency**: Sub-millisecond P99 response time for cached keys.
2. **Consistent Hashing**: Minimizes key remapping during cluster resize.
3. **Partition Tolerance**: Graceful failover on node isolation.

### Verification Flow
\`\`\`typescript
interface CacheLookup<T> {
  key: string;
  ttlSeconds: number;
  value: T;
}
\`\`\`

- [x] Regional failover tested
- [x] Eviction policies tuned (LRU / LFU)
- [ ] Multi-tenant quota enforcement
`
  },
  {
    id: 'okf-guide',
    title: 'OKF v0.2 Technical Guide',
    description: 'Step-by-step instructional guide using the supported OKF v0.2 fields',
    icon: 'school',
    content: `---
type: guide
title: "Markdown Studio Formatting Guide"
description: "Comprehensive tutorial for editing and viewing OKF v0.2 Markdown"
status: stable
tags:
  - guide
  - markdown
  - tutorial
sources:
  - https://github.com/GoogleCloudPlatform/knowledge-catalog
---

# Markdown Studio Formatting Guide ✍️

A modern, high-performance **Markdown Editor & Real-time Viewer** powered by \`ngx-markdown\`.

## ⚡ Quick Start & Features

- **Two Clean Modes**: Instant switching between **Edit** and **View** mode with \`Ctrl+E\`.
- **Interactive Checklists**: Click checkboxes right inside the preview pane.
- **Code Syntax Highlighting**: Powered by Prism.js with copy buttons.
- **Table Generator**: Build clean tabular data effortlessly.
- **Local File Upload**: Ingest any local \`.md\` file with automated OKF v0.2 conformance.

---

## 💻 Code Blocks with Syntax Highlighting

Here is an Angular 21 TypeScript component:

\`\`\`typescript
import { Component, signal } from '@angular/core';

@Component({
  selector: 'app-counter',
  template: \`
    <button (click)="count.update(n => n + 1)">Count: {{ count() }}</button>
  \`
})
export class Counter {
  readonly count = signal(0);
}
\`\`\`

---

## 📊 Structured Tables

| Feature | Support | Engine | Status |
| :--- | :---: | :---: | :--- |
| **OKF v0.2 fields** | Supported subset | Frontmatter validation | ✅ Enforced |
| **Live Preview** | Full | \`ngx-markdown\` | ✅ Active |
| **Upload Support** | Multi-file | Drag & Drop | ✅ Enabled |
`
  },
  {
    id: 'okf-api',
    title: 'OKF v0.2 API Reference',
    description: 'RESTful API endpoint documentation using OKF v0.2 fields',
    icon: 'api',
    content: `---
type: reference
title: "Filesystem Storage API"
description: "Specification for backend folder & markdown document endpoints"
status: stable
tags:
  - api
  - backend
  - okf-v0.2
resource: "/api/v1/vault"
---

# Filesystem Storage API Reference

Base URL: \`/api/v1/vault\`

## Endpoints

### 1. Retrieve Vault Tree
\`GET /api/v1/vault/tree\`

Returns recursive folder and \`.md\` file tree.

### 2. Upload Markdown Files
\`POST /api/v1/vault/uploads\`

Accepts multiple \`.md\` files and normalizes the supported OKF v0.2 fields before storage.

#### Request Body
\`\`\`json
{
  "folder": "Documentation",
  "files": [
    {
      "name": "spec.md",
      "content": "---\\ntype: concept\\n---\\n# Spec",
      "expectedRevision": null
    }
  ]
}
\`\`\`
`
  },
  {
    id: 'okf-task',
    title: 'OKF v0.2 Task & Roadmap',
    description: 'Sprint action items and milestone tracker in OKF v0.2 format',
    icon: 'task_alt',
    content: `---
type: task
title: "Sprint Execution Plan"
description: "Sprint tasks, milestones, and deliverables"
status: draft
tags:
  - sprint
  - delivery
  - okf-v0.2
---

# Sprint Execution Plan 📋

## Objectives
1. Maintain the supported Open Knowledge Format (OKF) v0.2 fields.
2. Implement seamless file upload and drag-and-drop ingestion.
3. Validate 2-mode switching (Edit & View).

## Action Items
- [x] Implement \`parseOkf\` and \`ensureOkfFormat\`
- [x] Add multi-file upload button and drag-and-drop zone
- [x] Render OKF v0.2 metadata card in View mode
- [x] Add OKF v0.2 toolbar inspector in Edit mode
- [ ] Review document trust signals
`
  }
];
