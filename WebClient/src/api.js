const API_BASE = '/Server/api';

export class ApiError extends Error {
  constructor(status, code, message) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

export async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`${API_BASE}/${path}`, {
      credentials: 'same-origin',
      ...options,
      headers: {
        Accept: 'application/json',
        ...(options.body ? { 'Content-Type': 'application/json; charset=UTF-8' } : {}),
        ...options.headers,
      },
    });
  } catch {
    throw new ApiError(0, 'SERVER_UNAVAILABLE',
      'Cannot reach Guess Market. Check that Tomcat is running on port 8080.');
  }

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(response.status, data?.errorCode ?? 'HTTP_ERROR',
      data?.message ?? `Server returned HTTP ${response.status}.`);
  }
  return data;
}

function post(path, body) {
  return request(path, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export const api = {
  session: () => request('login'),
  login: (userName) => post('login', { userName }),
  logout: () => request('session/logout', { method: 'POST' }),
  events: () => request('events'),
  event: (id) => request(`event?id=${encodeURIComponent(id)}`),
  users: () => request('users'),
  account: () => request('account'),
  history: () => request('account/history'),
  deposit: (amount) => post('account/deposit', { amount }),
  open: (eventId) => post('event/open', { eventId }),
  purchase: (eventId, optionNumber, quantity) =>
    post('event/purchase', { eventId, optionNumber, quantity }),
  order: (eventId, optionNumber, side, quantity, limitPrice) =>
    post('event/order', { eventId, optionNumber, side, quantity, limitPrice }),
  close: (eventId, winningOptionNumber) =>
    post('event/close', { eventId, winningOptionNumber }),
};
