import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/constants/api-endpoints';
import { ApiSuccess, UploadFile, UploadResult, VaultDocument, VaultNode } from '../models/vault.model';

@Injectable({ providedIn: 'root' })
export class VaultReadService {
  private readonly http = inject(HttpClient);

  tree() {
    return this.http.get<ApiSuccess<VaultNode[]>>(API_ENDPOINTS.tree).pipe(map(response => response.data));
  }

  document(path: string) {
    return this.http.get<ApiSuccess<VaultDocument>>(API_ENDPOINTS.documents, { params: { path } })
      .pipe(map(response => response.data));
  }

  saveDocument(path: string, content: string, revision: string) {
    return this.http.put<ApiSuccess<VaultDocument>>(API_ENDPOINTS.documents, { content }, {
      params: { path }, headers: { 'If-Match': `"${revision}"` },
    }).pipe(map(response => response.data));
  }

  createDocument(path: string, content: string) {
    return this.http.put<ApiSuccess<VaultDocument>>(API_ENDPOINTS.documents, { content }, {
      params: { path }, headers: { 'If-None-Match': '*' },
    }).pipe(map(response => response.data));
  }

  createFolder(path: string) {
    return this.http.post<ApiSuccess<VaultNode>>(API_ENDPOINTS.folders, { path }).pipe(map(response => response.data));
  }

  moveItem(from: string, to: string, revision: string) {
    return this.http.post<ApiSuccess<VaultNode>>(API_ENDPOINTS.moves, { from, to }, {
      headers: { 'If-Match': `"${revision}"` },
    }).pipe(map(response => response.data));
  }

  deleteItem(path: string, revision: string) {
    return this.http.delete<void>(API_ENDPOINTS.items, { params: { path }, headers: { 'If-Match': `"${revision}"` } });
  }

  upload(folder: string, files: UploadFile[]) {
    const payload = files.map(({ name, content, expectedRevision }) => ({ name, content, expectedRevision }));
    return this.http.post<ApiSuccess<UploadResult[]>>(API_ENDPOINTS.uploads, { folder, files: payload }, { observe: 'response' }).pipe(
      map(response => response.body?.data ?? []));
  }
}
