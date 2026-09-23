import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { VaultReadService } from './vault-read.service';
import { API_ENDPOINTS } from '../../../core/constants/api-endpoints';

describe('VaultReadService', () => {
  let service: VaultReadService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(VaultReadService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('unwraps the tree envelope', () => {
    let result: unknown;
    service.tree().subscribe(value => result = value);
    http.expectOne(API_ENDPOINTS.tree).flush({ success: true, data: [], timestamp: '2026-09-23T00:00:00Z' });
    expect(result).toEqual([]);
  });

  it('encodes a nested document path as a query parameter', () => {
    let result: unknown;
    service.document('Notes/a & b.md').subscribe(value => result = value);
    const request = http.expectOne(req => req.url === API_ENDPOINTS.documents);
    expect(request.request.params.get('path')).toBe('Notes/a & b.md');
    request.flush({ success: true, data: { path: 'Notes/a & b.md', name: 'a & b.md', content: '# Hi', updatedAt: '', size: 4, revision: 'abc' }, timestamp: '' });
    expect(result).toMatchObject({ content: '# Hi' });
  });

  it('sends a conditional PUT with the complete Markdown buffer', () => {
    service.saveDocument('Notes/a.md', '# New', 'a'.repeat(64)).subscribe();
    const request = http.expectOne(req => req.method === 'PUT' && req.url === API_ENDPOINTS.documents);
    expect(request.request.params.get('path')).toBe('Notes/a.md');
    expect(request.request.headers.get('If-Match')).toBe(`"${'a'.repeat(64)}"`);
    expect(request.request.body).toEqual({ content: '# New' });
    request.flush({ success: true, data: { path: 'Notes/a.md', name: 'a.md', content: '# New', updatedAt: '', size: 5, revision: 'b'.repeat(64) }, timestamp: '' });
  });

  it('uses create-only precondition for a new document', () => {
    service.createDocument('Notes/new.md', '# New').subscribe();
    const request = http.expectOne(req => req.method === 'PUT' && req.url === API_ENDPOINTS.documents);
    expect(request.request.headers.get('If-None-Match')).toBe('*');
    expect(request.request.headers.has('If-Match')).toBe(false);
    expect(request.request.params.get('path')).toBe('Notes/new.md');
    request.flush({ success: true, data: { path: 'Notes/new.md', name: 'new.md', content: '# New', updatedAt: '', size: 5, revision: 'a'.repeat(64) }, timestamp: '' });
  });

  it('omits UI-only properties from the contracted upload body', () => {
    service.upload('', [{ name: 'a.md', content: '# A', expectedRevision: null }]).subscribe();
    const request = http.expectOne(req => req.method === 'POST' && req.url === API_ENDPOINTS.uploads);
    expect(request.request.body).toEqual({ folder: '', files: [{ name: 'a.md', content: '# A', expectedRevision: null }] });
    request.flush({ success: true, data: [], timestamp: '' });
  });
});
