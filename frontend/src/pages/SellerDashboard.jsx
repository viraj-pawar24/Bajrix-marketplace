import { useEffect, useState } from 'react';
import { api } from '../api/client.js';
import { useSellerAuth } from '../context/SellerAuthContext.jsx';
import AddListingModal from '../components/AddListingModal.jsx';
import { formatMoney } from '../components/Money.jsx';

export default function SellerDashboard() {
  const { activeSeller, selectSeller } = useSellerAuth();

  if (!activeSeller) {
    return <SellerGate onSelect={selectSeller} />;
  }
  return <ListingsPanel />;
}

function SellerGate({ onSelect }) {
  const [sellers, setSellers] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.listSellers().then(setSellers).catch((e) => setError(e.message));
  }, []);

  return (
    <main className="page">
      <div className="seller-gate">
        <h1>Log in as a seller</h1>
        <p>
          There's no real authentication in this demo &mdash; pick which seller account you want to manage. The
          backend still enforces that this identity can only edit its own listings.
        </p>
        {error && <div className="banner error">{error}</div>}
        <div className="seller-select-grid">
          {sellers.map((s) => (
            <div className="seller-select-row" key={s.id} onClick={() => onSelect(s)}>
              <div>
                <div className="name">{s.name}</div>
                <div className="email">{s.contactEmail}</div>
              </div>
              <span className={`badge ${statusClass(s.status)}`}>{s.status}</span>
            </div>
          ))}
        </div>
      </div>
    </main>
  );
}

function statusClass(status) {
  if (status === 'APPROVED') return 'ok';
  if (status === 'PENDING') return 'warn';
  return 'bad';
}

function ListingsPanel() {
  const { activeSeller, selectSeller } = useSellerAuth();
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [drafts, setDrafts] = useState({}); // listingId -> { price, stock, minOrderQty }
  const [savingId, setSavingId] = useState(null);

  function load() {
    setLoading(true);
    setError(null);
    api
      .myListings(activeSeller.id)
      .then((data) => {
        setListings(data);
        setDrafts(Object.fromEntries(data.map((l) => [l.id, draftFrom(l)])));
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }

  useEffect(load, [activeSeller.id]);

  function draftFrom(l) {
    return { price: String(l.price), stock: String(l.stock), minOrderQty: String(l.minOrderQty) };
  }

  function updateDraft(id, field, value) {
    setDrafts((prev) => ({ ...prev, [id]: { ...prev[id], [field]: value } }));
  }

  async function saveRow(listing) {
    const draft = drafts[listing.id];
    setSavingId(listing.id);
    setError(null);
    try {
      const updated = await api.updateListing(activeSeller.id, listing.id, {
        price: Number(draft.price),
        stock: Number(draft.stock),
        minOrderQty: Number(draft.minOrderQty),
        version: listing.version,
      });
      setListings((prev) => prev.map((l) => (l.id === updated.id ? updated : l)));
      setDrafts((prev) => ({ ...prev, [updated.id]: draftFrom(updated) }));
    } catch (err) {
      if (err.status === 409) {
        setError(
          `"${listing.sellerName ? '' : ''}${listingLabel(listing)}" was changed elsewhere (maybe another tab). Reloading the latest data.`
        );
        load();
      } else {
        setError(err.message);
      }
    } finally {
      setSavingId(null);
    }
  }

  async function toggleActive(listing, nextActive) {
    setSavingId(listing.id);
    setError(null);
    try {
      const updated = await api.updateListing(activeSeller.id, listing.id, { active: nextActive, version: listing.version });
      setListings((prev) => prev.map((l) => (l.id === updated.id ? updated : l)));
      setDrafts((prev) => ({ ...prev, [updated.id]: draftFrom(updated) }));
    } catch (err) {
      setError(err.message);
    } finally {
      setSavingId(null);
    }
  }

  function listingLabel(l) {
    return l.productName || `listing #${l.id}`;
  }

  return (
    <main className="page">
      <div className="active-seller-bar">
        <span className={`badge ${statusClass(activeSeller.status)}`}>{activeSeller.status}</span>
        <span>
          Managing listings as <strong>{activeSeller.name}</strong>
        </span>
        <button className="switch-link" onClick={() => selectSeller(null)}>
          Switch seller
        </button>
      </div>

      <div className="page-header">
        <div>
          <h1>Your listings</h1>
          <p>Update price, stock and minimum order quantity, or stop selling a product.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>
          + List a product
        </button>
      </div>

      {activeSeller.status !== 'APPROVED' && (
        <div className="banner error">
          Your seller account is <strong>{activeSeller.status}</strong>. You can still manage listings here, but
          buyers won't see them until your account is approved.
        </div>
      )}

      {error && <div className="banner error">{error}</div>}

      {loading ? (
        <p>Loading&hellip;</p>
      ) : listings.length === 0 ? (
        <div className="empty-state">
          <h3>You haven't listed anything yet</h3>
          <p>Add your first product to start selling on BajriX.</p>
        </div>
      ) : (
        <div className="dashboard-table-wrap">
          <table className="dashboard-table">
            <thead>
              <tr>
                <th>Product</th>
                <th>Price (&#8377;)</th>
                <th>Stock</th>
                <th>Min. order</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {listings.map((l) => {
                const draft = drafts[l.id] ?? draftFrom(l);
                const dirty =
                  draft.price !== String(l.price) ||
                  draft.stock !== String(l.stock) ||
                  draft.minOrderQty !== String(l.minOrderQty);
                return (
                  <tr key={l.id} style={{ opacity: l.active ? 1 : 0.55 }}>
                    <td>
                      <strong>{listingLabel(l)}</strong>
                      <div style={{ fontSize: 12, color: 'var(--ink-faint)' }}>{formatMoney(l.price)} currently</div>
                    </td>
                    <td>
                      <input
                        type="number"
                        min="0.01"
                        step="0.01"
                        value={draft.price}
                        disabled={!l.active}
                        onChange={(e) => updateDraft(l.id, 'price', e.target.value)}
                      />
                    </td>
                    <td>
                      <input
                        type="number"
                        min="0"
                        step="1"
                        value={draft.stock}
                        disabled={!l.active}
                        onChange={(e) => updateDraft(l.id, 'stock', e.target.value)}
                      />
                    </td>
                    <td>
                      <input
                        type="number"
                        min="1"
                        step="1"
                        value={draft.minOrderQty}
                        disabled={!l.active}
                        onChange={(e) => updateDraft(l.id, 'minOrderQty', e.target.value)}
                      />
                    </td>
                    <td>
                      {!l.active ? (
                        <span className="badge muted">Stopped</span>
                      ) : l.orderable ? (
                        <span className="badge ok">Active</span>
                      ) : (
                        <span className="badge warn">Below MOQ</span>
                      )}
                    </td>
                    <td>
                      <div className="row-actions">
                        {l.active && (
                          <>
                            <button
                              className="btn btn-secondary btn-sm"
                              disabled={!dirty || savingId === l.id}
                              onClick={() => saveRow(l)}
                            >
                              {savingId === l.id ? 'Saving…' : 'Save'}
                            </button>
                            <button
                              className="btn btn-danger btn-sm"
                              disabled={savingId === l.id}
                              onClick={() => toggleActive(l, false)}
                            >
                              Stop selling
                            </button>
                          </>
                        )}
                        {!l.active && (
                          <button
                            className="btn btn-secondary btn-sm"
                            disabled={savingId === l.id}
                            onClick={() => toggleActive(l, true)}
                          >
                            Resume selling
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {showAddModal && (
        <AddListingModal
          sellerId={activeSeller.id}
          onClose={() => setShowAddModal(false)}
          onCreated={() => {
            setShowAddModal(false);
            load();
          }}
        />
      )}
    </main>
  );
}
