import React, { useEffect, useMemo, useState } from 'react';
import { RefreshCw, Search } from 'lucide-react';
import { api, ApiError } from './api.js';
import { availableActions, filterEvents, positivePrice, positiveWhole, sameData } from './eventModel.js';
import DataTable from './DataTable.jsx';

const money = (value) => value == null ? '-' : Number(value).toFixed(2);
const text = (value) => value == null ? '-' : String(value).replaceAll('_', ' ');

function MarketData({ detail }) {
  const [tab, setTab] = useState('Options');
  const options = detail.marketOptions ?? [];
  const orders = options.flatMap((option) => [...(option.buyOrders ?? []), ...(option.sellOrders ?? [])]);
  const tables = {
    Options: {
      rows: options, key: (row) => row.optionNumber,
      columns: [
        { label: 'Option', value: (row) => row.name },
        { label: 'Shares', value: (row) => row.issuedShares },
        { label: 'Value', value: (row) => money(row.currentValue) },
        { label: 'Last', value: (row) => money(row.lastPrice) },
        { label: 'Bid', value: (row) => money(row.bestBid) },
        { label: 'Ask', value: (row) => money(row.bestAsk) },
      ],
    },
    Orders: {
      rows: orders, key: (row) => row.orderId,
      columns: [
        { label: 'Option', value: (row) => row.optionNumber },
        { label: 'Side', value: (row) => row.side },
        { label: 'User', value: (row) => row.userName },
        { label: 'Remaining', value: (row) => row.remainingQuantity },
        { label: 'Limit', value: (row) => money(row.limitPrice) },
        { label: 'Status', value: (row) => text(row.status) },
      ],
    },
    Executions: {
      rows: detail.executionsNewestFirst ?? [], key: (row) => row.executionId,
      columns: [
        { label: 'Option', value: (row) => row.optionNumber },
        { label: 'Buyer', value: (row) => row.buyerName },
        { label: 'Seller', value: (row) => row.minted ? 'Minted' : row.sellerName },
        { label: 'Quantity', value: (row) => row.quantity },
        { label: 'Price', value: (row) => money(row.unitPrice) },
      ],
    },
    Trades: {
      rows: detail.tradesNewestFirst ?? [], key: (row) => row.tradeNumber,
      columns: [
        { label: 'Option', value: (row) => row.optionName },
        { label: 'Quantity', value: (row) => row.quantity },
        { label: 'Cost', value: (row) => money(row.shareCost) },
        { label: 'Commission', value: (row) => money(row.commission) },
        { label: 'Paid', value: (row) => money(row.totalPaid) },
      ],
    },
    Participants: {
      rows: detail.participantPositions ?? [], key: (row) => row.userName,
      columns: [
        { label: 'User', value: (row) => row.userName },
        { label: 'Shares', value: (row) => row.totalShares },
        { label: 'Paid', value: (row) => money(row.totalAmountPaid) },
        { label: 'Commission', value: (row) => money(row.totalCommissionPaid) },
      ],
    },
  };
  const selected = tables[tab];
  return (
    <section className="market-data" aria-label="Market data">
      <div className="sub-tabs" role="tablist" aria-label="Market data views">
        {Object.keys(tables).map((name) => <button key={name} type="button" role="tab"
          aria-selected={tab === name} className={tab === name ? 'selected' : ''}
          onClick={() => setTab(name)}>{name}</button>)}
      </div>
      <DataTable columns={selected.columns} rows={selected.rows} rowKey={selected.key}
        empty={`No ${tab.toLowerCase()} yet`} />
    </section>
  );
}

function EventActions({ detail, userName, onAction, busy }) {
  const event = detail.summary;
  const allowed = availableActions(event, userName);
  const options = event.options ?? [];
  const [option, setOption] = useState(null);
  const [quantity, setQuantity] = useState('1');
  const [side, setSide] = useState('BUY');
  const [price, setPrice] = useState('');
  const [formError, setFormError] = useState('');
  const optionNumber = Number(option ?? options[0]?.optionNumber);

  function run(event, action) {
    event.preventDefault();
    setFormError('');
    const count = positiveWhole(quantity);
    if (!count) { setFormError('Enter a positive whole quantity.'); return; }
    if (!Number.isInteger(optionNumber)) { setFormError('Choose an option.'); return; }
    if (allowed.purchase) onAction(() => api.purchase(detail.summary.eventId, optionNumber, count));
    if (allowed.order) {
      const limit = positivePrice(price);
      if (!limit) { setFormError('Enter a positive limit price.'); return; }
      onAction(() => api.order(detail.summary.eventId, optionNumber, side, count, limit));
    }
  }

  if (!Object.values(allowed).some(Boolean)) return null;
  return (
    <section className="event-actions" aria-label="Event actions">
      {allowed.open && <button type="button" className="button primary" disabled={busy}
        onClick={() => onAction(() => api.open(event.eventId))}>Open event</button>}
      {(allowed.purchase || allowed.order) && <form className="action-form" onSubmit={(submitEvent) => run(submitEvent)}>
        <label>Option
          <select value={optionNumber || ''} onChange={(change) => setOption(change.target.value)}>
            {options.map((item) => <option key={item.optionNumber} value={item.optionNumber}>{item.name}</option>)}
          </select>
        </label>
        {allowed.order && <label>Side
          <select value={side} onChange={(change) => setSide(change.target.value)}>
            <option value="BUY">Buy</option><option value="SELL">Sell</option>
          </select>
        </label>}
        <label>Quantity<input inputMode="numeric" value={quantity}
          onChange={(change) => setQuantity(change.target.value)} /></label>
        {allowed.order && <label>Limit price<input inputMode="decimal" value={price}
          onChange={(change) => setPrice(change.target.value)} /></label>}
        <button className="button primary" type="submit" disabled={busy}>
          {allowed.purchase ? 'Buy shares' : 'Submit order'}
        </button>
      </form>}
      {allowed.close && <div className="close-form">
        <label>Winner
          <select value={optionNumber || ''} onChange={(change) => setOption(change.target.value)}>
            {options.map((item) => <option key={item.optionNumber} value={item.optionNumber}>{item.name}</option>)}
          </select>
        </label>
        <button className="button outline" type="button" disabled={busy} onClick={() => {
          if (Number.isInteger(optionNumber) && window.confirm('Close this event with the selected winner?')) {
            onAction(() => api.close(event.eventId, optionNumber));
          }
        }}>Close event</button>
      </div>}
      {formError && <p className="form-error" role="alert">{formError}</p>}
    </section>
  );
}

export default function EventWorkspace({ userName, onSessionExpired }) {
  const [events, setEvents] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [search, setSearch] = useState('');
  const [method, setMethod] = useState('');
  const [status, setStatus] = useState('');
  const [commission, setCommission] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const visible = useMemo(() => filterEvents(events, { search, method, status, commission }),
    [events, search, method, status, commission]);

  useEffect(() => {
    let active = true;
    let timer;
    async function poll() {
      try {
        const [nextEvents, nextDetail] = await Promise.all([
          api.events(), selectedId == null ? Promise.resolve(null) : api.event(selectedId),
        ]);
        if (!active) return;
        setEvents((previous) => sameData(previous, nextEvents) ? previous : nextEvents);
        setDetail((previous) => sameData(previous, nextDetail) ? previous : nextDetail);
        setError('');
      } catch (failure) {
        if (!active) return;
        if (failure instanceof ApiError && failure.status === 401) onSessionExpired();
        else setError(failure.message);
      } finally {
        if (active) {
          setLoading(false);
          timer = window.setTimeout(poll, 1000);
        }
      }
    }
    poll();
    return () => { active = false; window.clearTimeout(timer); };
  }, [selectedId, onSessionExpired]);

  async function act(operation) {
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const result = await operation();
      setDetail(result.event);
      const nextEvents = await api.events();
      setEvents((previous) => sameData(previous, nextEvents) ? previous : nextEvents);
      setNotice('Event updated.');
    } catch (failure) {
      setError(failure.message);
    } finally {
      setBusy(false);
    }
  }

  const selected = visible.find((event) => event.eventId === selectedId);
  const shownDetail = detail?.summary?.eventId === selectedId ? detail : null;
  return (
    <section className="events-screen" aria-label="Events">
      <div className="screen-heading"><div><h1>Events</h1><span>{events.length} total</span></div>
        <button className="icon-button" title="Refresh events" aria-label="Refresh events" type="button"
          onClick={async () => {
            try { const next = await api.events(); setEvents(next); setError(''); }
            catch (failure) { setError(failure.message); }
          }}><RefreshCw size={18} /></button></div>
      {error && <div className="inline-message error" role="alert">{error}</div>}
      {notice && <div className="inline-message success" role="status">{notice}</div>}
      <div className="master-detail">
        <div className="event-master">
          <div className="filters">
            <label className="search-field"><Search size={17} aria-hidden="true" />
              <input aria-label="Search events" placeholder="Search events" value={search}
                onChange={(change) => setSearch(change.target.value)} /></label>
            <div className="filter-row">
              <select aria-label="Method" value={method} onChange={(change) => setMethod(change.target.value)}>
                <option value="">All methods</option><option value="LMSR">LMSR</option><option value="ORDER_BOOK">Order book</option>
              </select>
              <select aria-label="Status" value={status} onChange={(change) => setStatus(change.target.value)}>
                <option value="">All statuses</option><option value="NOT_STARTED">Not started</option>
                <option value="ACTIVE">Active</option><option value="CLOSED">Closed</option>
              </select>
              <select aria-label="Commission" value={commission} onChange={(change) => setCommission(change.target.value)}>
                <option value="">All commissions</option><option value="ON_PURCHASE">On purchase</option>
                <option value="ON_CLOSE">On close</option>
              </select>
            </div>
          </div>
          <div className="event-list-heading"><span>Event</span><span>Method</span><span>Status</span></div>
          <div className="event-list" role="listbox" aria-label="Events">
            {loading && <p className="empty-state">Loading events...</p>}
            {!loading && visible.length === 0 && <p className="empty-state">No events to show.</p>}
            {visible.map((event) => <button type="button" role="option" key={event.eventId}
              aria-selected={selectedId === event.eventId}
              className={`event-row ${selectedId === event.eventId ? 'selected' : ''}`}
              onClick={() => { setSelectedId(event.eventId); setDetail(null); setNotice(''); }}>
              <span className="event-name">{event.name}<small>{event.marketMakerName}</small></span>
              <span>{text(event.tradingMethod)}</span><span className={`status-text ${event.status?.toLowerCase()}`}>{text(event.status)}</span>
            </button>)}
          </div>
        </div>
        <div className="event-detail">
          {!selected && <div className="empty-detail">Select an event to see its market.</div>}
          {selected && <>
            <div className="detail-heading"><div><h2>{selected.name}</h2>
              <span className="detail-status">{text(selected.status)} | {text(selected.tradingMethod)}</span></div></div>
            <p className="event-description">{selected.description}</p>
            <dl className="event-metrics">
              <div><dt>Market maker</dt><dd>{selected.marketMakerName}</dd></div>
              <div><dt>Event balance</dt><dd>{money(selected.accountBalance)}</dd></div>
              <div><dt>Commission</dt><dd>{selected.commissionPercentage}% | {text(selected.commissionType)}</dd></div>
              {shownDetail?.winningOptionName && <div><dt>Winner</dt><dd>{shownDetail.winningOptionName}</dd></div>}
            </dl>
            {shownDetail ? <>
              <EventActions key={selectedId} detail={shownDetail} userName={userName} busy={busy} onAction={act} />
              <MarketData detail={shownDetail} />
            </> : <p className="empty-state">Loading market details...</p>}
          </>}
        </div>
      </div>
    </section>
  );
}
