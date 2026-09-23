export interface VaultNode {
  name: string;
  path: string;
  type: 'file' | 'folder';
  updatedAt: string;
  revision: string;
  size?: number;
  children?: VaultNode[];
}

export interface VaultDocument {
  path: string;
  name: string;
  content: string;
  updatedAt: string;
  size: number;
  revision: string;
}

export interface ApiSuccess<T> { success: true; data: T; timestamp: string }
export interface UploadFile { name: string; content: string; expectedRevision: string | null }
export interface UploadResult { path: string; status: 'created' | 'replaced' | 'failed' | 'notAttempted'; revision?: string; error?: { code?: string; message?: string } }

export interface FileSummary extends VaultNode { type: 'file'; folder: string }

export function flattenTree(nodes: VaultNode[]): { files: FileSummary[]; folders: VaultNode[] } {
  const files: FileSummary[] = [];
  const folders: VaultNode[] = [];
  const visit = (items: VaultNode[]) => {
    for (const node of items) {
      if (node.type === 'folder') {
        folders.push(node);
        visit(node.children ?? []);
      } else {
        files.push({ ...node, type: 'file', folder: node.path.split('/').slice(0, -1).join('/') || 'Root' });
      }
    }
  };
  visit(nodes);
  return { files, folders };
}

export function filterTree(nodes: VaultNode[], query: string): VaultNode[] {
  const needle = query.trim().toLowerCase();
  if (!needle) return nodes;
  return nodes.flatMap(node => {
    if (node.name.toLowerCase().includes(needle)) return [node];
    if (node.type !== 'folder') return [];
    const children = filterTree(node.children ?? [], needle);
    return children.length ? [{ ...node, children }] : [];
  });
}
