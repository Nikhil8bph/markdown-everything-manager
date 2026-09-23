import { API_ENDPOINTS } from './api-endpoints';

describe('vault API endpoint constants', () => {
  it('keeps all seven contracted operations on the same-origin vault prefix', () => {
    expect(Object.values(API_ENDPOINTS)).toEqual([
      '/api/v1/vault/tree',
      '/api/v1/vault/documents',
      '/api/v1/vault/folders',
      '/api/v1/vault/uploads',
      '/api/v1/vault/moves',
      '/api/v1/vault/items',
    ]);
  });
});
