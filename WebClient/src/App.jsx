import React, { useEffect, useState } from 'react';
import { api, ApiError } from './api.js';
import EventWorkspace from './EventWorkspace.jsx';
import AccountWorkspace from './AccountWorkspace.jsx';

export default function App() {
  const [user, setUser] = useState(null);
  const [name, setName] = useState('');
  const [checking, setChecking] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [view, setView] = useState('Events');

  useEffect(() => {
    let active = true;
    api.session()
      .then((session) => { if (active) setUser(session); })
      .catch((failure) => {
        if (active && (!(failure instanceof ApiError) || failure.status !== 401)) {
          setError(failure.message);
        }
      })
      .finally(() => { if (active) setChecking(false); });
    return () => { active = false; };
  }, []);

  async function signIn(event) {
    event.preventDefault();
    const entered = name.trim();
    if (!entered) {
      setError('Enter a user name.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      setUser(await api.login(entered));
      setName('');
    } catch (failure) {
      setError(failure instanceof ApiError && failure.code === 'DUPLICATE_USER_NAME'
        ? 'This user name is already in use. Choose another name.' : failure.message);
    } finally {
      setBusy(false);
    }
  }

  async function signOut() {
    setBusy(true);
    setError('');
    try {
      await api.logout();
      setUser(null);
    } catch (failure) {
      setError(failure.message);
    } finally {
      setBusy(false);
    }
  }

  if (checking) {
    return <div className="boot-state" role="status">Connecting to Guess Market...</div>;
  }

  if (!user) {
    return (
      <main className="login-screen">
        <div className="login-layout">
          <header className="login-brand">
            <span className="brand-mark" aria-hidden="true">GM</span>
            <h1>Guess Market</h1>
          </header>
          <form className="login-form" onSubmit={signIn}>
            <h2>Sign in</h2>
            <label htmlFor="user-name">User name</label>
            <input id="user-name" autoFocus autoComplete="off" value={name}
              onChange={(event) => setName(event.target.value)} />
            <button className="button primary" type="submit" disabled={busy}>
              {busy ? 'Signing in...' : 'Sign in'}
            </button>
            {error && <p className="form-error" role="alert">{error}</p>}
          </form>
        </div>
      </main>
    );
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <span className="brand-mark" aria-hidden="true">GM</span>
        <strong>Guess Market</strong>
        <span className="header-spacer" />
        <span className="current-user">{user.name}</span>
        <button className="button subtle" type="button" onClick={signOut} disabled={busy}>
          Sign out
        </button>
      </header>
      <nav className="main-tabs" aria-label="Main views">
        {['Events', 'Account'].map((item) => <button key={item} type="button"
          aria-current={view === item ? 'page' : undefined}
          onClick={() => setView(item)}>{item}</button>)}
      </nav>
      <main className="workspace">
        {error && <p className="form-error" role="alert">{error}</p>}
        {view === 'Events' ? <EventWorkspace userName={user.name}
          onSessionExpired={() => setUser(null)} />
          : <AccountWorkspace userName={user.name} onSessionExpired={() => setUser(null)} />}
      </main>
    </div>
  );
}
