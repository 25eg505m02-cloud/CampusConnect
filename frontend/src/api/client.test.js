import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import { resolveApiBaseUrl } from './baseUrl.js';

test('production API requests use the same-origin API proxy', () => {
  assert.equal(
    resolveApiBaseUrl(true, 'https://campusconnect-smah.onrender.com'),
    '/api/v1',
  );
});

test('development API requests can use the configured backend URL', () => {
  assert.equal(
    resolveApiBaseUrl(false, 'http://localhost:8080/api/v1'),
    'http://localhost:8080/api/v1',
  );
  assert.equal(resolveApiBaseUrl(false, undefined), '/api/v1');
});

test('Vercel proxies versioned API requests before serving client-side routes', async () => {
  const config = JSON.parse(await readFile(new URL('../../vercel.json', import.meta.url), 'utf8'));
  assert.deepEqual(config.rewrites[0], {
    source: '/api/v1/:path*',
    destination: 'https://campusconnect-smah.onrender.com/api/v1/:path*',
  });
  assert.equal(config.rewrites.at(-1).destination, '/index.html');
});
