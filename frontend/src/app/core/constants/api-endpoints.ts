import { environment } from '../../../environments/environment';

const base = environment.apiBaseUrl;

export const API_ENDPOINTS = {
  tree: `${base}/tree`,
  documents: `${base}/documents`,
  folders: `${base}/folders`,
  uploads: `${base}/uploads`,
  moves: `${base}/moves`,
  items: `${base}/items`,
} as const;
