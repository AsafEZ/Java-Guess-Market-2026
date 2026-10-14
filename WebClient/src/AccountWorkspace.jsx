import React, { useEffect, useState } from 'react';
import { api, ApiError } from './api.js';
import { depositAmount, formatTime, selectedPosition } from './accountModel.js';
import { sameData } from './eventModel.js';
import DataTable from './DataTable.jsx';

const money = (value) => Number(value ?? 0).toFixed(2);
const text = (value) => String(value ?? '-').replaceAll('_', ' ');

export default function AccountWorkspace({ userName, onSessionExpired }) {
  const [users, setUsers] = useState([]);
  const [account, setAccount] = useState(null);
  const [history, setHistory] = useState([]);
  const [tab, setTab] = useState('Positions');
  const [positionId, setPositionId] = useState(null);
  const [amount, setAmount] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    let active = true;
    let timer;
    async function poll() {
      try {
        const [nextUsers, nextAccount, nextHistory] = await Promise.all([
          api.users(), api.account(), api.history(),
        ]);
        if (!active) return;
        setUsers((previous) => sameData(previous, nextUsers) ? previous : nextUsers);
        setAccount((previous) => sameData(previous, nextAccount) ? previous : nextAccount);
        setHistory((previous) => sameData(previous, nextHistory) ? previous : nextHistory);
        setError('');
      } catch (failure) {
        if (!active) return;
        if (failure instanceof ApiError && failure.status === 401) onSessionExpired();
        else setError(failure.message);
      } finally {
        if (active) timer = window.setTimeout(poll, 1000);
      }
    }
    poll();
    return () => { active = false; window.clearTimeout(timer); };
  }, [onSessionExpired]);

  async function addFunds(event) {
    event.preventDefault();
    const parsed = depositAmount(amount);
    if (!parsed) { setError('Enter a positive amount.'); return; }
    setBusy(true);
    setError('');
    setNotice('');
    try {
      await api.deposit(parsed);
      const [nextUsers, nextAccount, nextHistory] = await Promise.all([
        api.users(), api.account(), api.history(),
      ]);
      setUsers(nextUsers);
      setAccount(nextAccount);
      setHistory(nextHistory);
      setAmount('');
      setNotice('Funds added.');
    } catch (failure) {
      setError(failure.message);
    } finally {
      setBusy(false);
    }
  }

  const positions = account?.positions ?? [];
  const position = selectedPosition(positions, positionId);
  return (
    <section className="account-screen" aria-label="Account">
      <div className="screen-heading"><div><h1>Account</h1><span>{userName}</span></div></div>
      {error && <div className="inline-message error" role="alert">{error}</div>}
      {notice && <div className="inline-message success" role="status">{notice}</div>}
      <div className="master-detail account-layout">
        <aside className="user-master" aria-label="Users">
          <div className="pane-title"><h2>Users</h2><span>{users.length}</span></div>
          <DataTable columns={[
            { label: 'User', value: (row) => <span className={row.name === userName ? 'self-name' : ''}>{row.name}</span> },
            { label: 'Balance', value: (row) => money(row.balance) },
            { label: 'Market maker', value: (row) => row.marketMaker ? 'Yes' : 'No' },
          ]} rows={users} rowKey={(row) => row.name} empty="No users yet" />
        </aside>
        <div className="account-detail">
          <div className="pane-title"><h2>My account</h2></div>
          {!account ? <p className="empty-state">Loading account...</p> : <>
            <div className="account-summary">
              <div><span>Current balance</span><strong>{money(account.balance)}</strong></div>
              <div><span>Status</span><strong>{text(account.status)}</strong></div>
              <div><span>Market maker events</span><strong>{account.marketMakerEventIds?.length ?? 0}</strong></div>
            </div>
            <form className="deposit-form" onSubmit={addFunds}>
              <label htmlFor="deposit-amount">Add funds</label>
              <input id="deposit-amount" value={amount} inputMode="decimal" placeholder="Amount"
                onChange={(change) => setAmount(change.target.value)} />
              <button type="submit" className="button primary" disabled={busy}>
                {busy ? 'Adding...' : 'Add funds'}
              </button>
            </form>
            <div className="sub-tabs account-tabs" role="tablist" aria-label="Account views">
              {['Positions', 'History'].map((name) => <button key={name} role="tab" type="button"
                aria-selected={tab === name} className={tab === name ? 'selected' : ''}
                onClick={() => setTab(name)}>{name}</button>)}
            </div>
            {tab === 'Positions' ? <>
              <div className="table-scroll">
                <table className="data-table positions-table">
                  <thead><tr><th>Event</th><th>Status</th><th>Shares</th><th>Paid</th></tr></thead>
                  <tbody>
                    {positions.length === 0 && <tr><td colSpan="4" className="empty-cell">No event positions</td></tr>}
                    {positions.map((item) => <tr key={item.eventId} className={positionId === item.eventId ? 'selected-row' : ''}>
                      <td><button type="button" className="table-link" onClick={() => setPositionId(item.eventId)}>{item.eventName}</button></td>
                      <td>{text(item.eventStatus)}</td><td>{item.totalShares}</td><td>{money(item.totalAmountPaid)}</td>
                    </tr>)}
                  </tbody>
                </table>
              </div>
              {position && <div className="position-detail">
                <h3>{position.eventName}</h3>
                <p>{text(position.tradingMethod)} | {text(position.eventStatus)}
                  {position.winningOptionName ? ` | Winner: ${position.winningOptionName}` : ''}</p>
                <div className="position-totals"><span>Shares: {position.totalShares}</span>
                  <span>Paid: {money(position.totalAmountPaid)}</span>
                  <span>Commission: {money(position.totalCommissionPaid)}</span></div>
                <h4>Options</h4>
                <DataTable columns={[
                  { label: 'Option', value: (row) => row.name },
                  { label: 'Shares', value: (row) => row.shares },
                  { label: 'Paid', value: (row) => money(row.amountPaid) },
                  { label: 'Commission', value: (row) => money(row.commissionPaid) },
                ]} rows={position.options ?? []} rowKey={(row) => row.optionNumber} empty="No option positions" />
                <h4>Trades</h4>
                <DataTable columns={[
                  { label: 'Option', value: (row) => row.optionName },
                  { label: 'Quantity', value: (row) => row.quantity },
                  { label: 'Cost', value: (row) => money(row.shareCost) },
                  { label: 'Commission', value: (row) => money(row.commission) },
                  { label: 'Paid', value: (row) => money(row.totalPaid) },
                ]} rows={position.tradesNewestFirst ?? []} rowKey={(row) => row.tradeNumber} empty="No trades for this event" />
              </div>}
            </> : <DataTable columns={[
              { label: 'Time', value: (row) => formatTime(row.occurredAt) },
              { label: 'Action', value: (row) => text(row.action) },
              { label: 'Event', value: (row) => row.eventName ?? '-' },
              { label: 'Amount', value: (row) => money(row.amount) },
              { label: 'Commission', value: (row) => money(row.commission) },
              { label: 'Balance after', value: (row) => money(row.balanceAfter) },
            ]} rows={history} rowKey={(row) => row.id} empty="No account activity yet" />}
          </>}
        </div>
      </div>
    </section>
  );
}
