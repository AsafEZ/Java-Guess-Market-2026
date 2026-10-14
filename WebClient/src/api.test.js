import assert from 'node:assert/strict';
import { afterEach, test } from 'node:test';
import { api, ApiError } from './api.js';

const originalFetch = globalThis.fetch;
afterEach(() => { globalThis.fetch = originalFetch; });

test('login uses the existing API path and JSON contract', async () => {
  let called;
  globalThis.fetch = async (url, options) => {
    called = { url, options };
    return { ok: true, json: async () => ({ name: 'Asaf' }) };
  };
  assert.deepEqual(await api.login('Asaf'), { name: 'Asaf' });
  assert.equal(called.url, '/Server/api/login');
  assert.equal(called.options.credentials, 'same-origin');
  assert.equal(called.options.method, 'POST');
  assert.deepEqual(JSON.parse(called.options.body), { userName: 'Asaf' });
});

test('server validation errors reach the UI with their code', async () => {
  globalThis.fetch = async () => ({
    ok: false,
    status: 409,
    json: async () => ({ errorCode: 'DUPLICATE_USER_NAME', message: 'In use.' }),
  });
  await assert.rejects(api.login('Asaf'), (error) =>
    error instanceof ApiError && error.status === 409
      && error.code === 'DUPLICATE_USER_NAME' && error.message === 'In use.');
});

test('network failure is distinguishable from an invalid input', async () => {
  globalThis.fetch = async () => { throw new TypeError('offline'); };
  await assert.rejects(api.session(), (error) =>
    error instanceof ApiError && error.code === 'SERVER_UNAVAILABLE');
});
