import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import { resolveApiBaseUrl } from './baseUrl.js';

test('production API requests use the embedded Render API URL', () => {
  assert.equal(
    resolveApiBaseUrl(
      true,
      'http://localhost:8080/api/v1',
      'https://campusconnect-smah.onrender.com/api/v1',
    ),
    'https://campusconnect-smah.onrender.com/api/v1',
  );
});

test('development API requests can use the configured backend URL', () => {
  assert.equal(
    resolveApiBaseUrl(false, 'http://localhost:8080/api/v1'),
    'http://localhost:8080/api/v1',
  );
  assert.equal(resolveApiBaseUrl(false, undefined), '/api/v1');
});

test('Vercel serves client-side routes from the SPA entry point', async () => {
  const config = JSON.parse(await readFile(new URL('../../vercel.json', import.meta.url), 'utf8'));
  assert.deepEqual(config.rewrites, [
    { source: '/(.*)', destination: '/index.html' },
  ]);
});
