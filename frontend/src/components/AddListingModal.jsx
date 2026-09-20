import { useEffect, useState } from 'react';
import { api } from '../api/client.js';

/**
 * Two-step "add listing" flow: find (or create) a catalogue product, then
 * set the seller's price/stock/MOQ for it. Kept as one modal since for a
 * seller adding a brand-new product these two steps happen back to back.
 */
export default function AddListingModal({ sellerId, onClose, onCreated }) {
  const [mode, setMode] = useState('search'); // 'search' | 'new-product'
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [newProduct, setNewProduct] = useState({ name: '', brand: '', category: '', unit: '' });
  const [form, setForm] = useState({ price: '', stock: '', minOrderQty: '1' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (mode !== 'search' || !query.trim()) {
      setResults([]);
      return;
    }
    let cancelled = false;
    const t = setTimeout(() => {
      api.searchProducts({ q: query, size: 6 }).then((data) => {
        if (!cancelled) setResults(data.content);
      });
    }, 250);
    return () => {
      cancelled = true;
      clearTimeout(t);
    };
  }, [query, mode]);

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      let productId = selectedProduct?.id;

      if (mode === 'new-product') {
        if (!newProduct.name.trim() || !newProduct.category.trim()) {
          throw new Error('Product name and category are required.');
        }
        const created = await api.createProduct(newProduct);
        productId = created.id;
      }
      if (!productId) {
        throw new Error('Choose a product first.');
      }

      const payload = {
        productId,
        price: Number(form.price),
        stock: Number(form.stock),
        minOrderQty: Number(form.minOrderQty),
      };
      const listing = await api.createListing(sellerId, payload);
      onCreated(listing);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  const readyToPriceIt = (mode === 'search' && selectedProduct) || mode === 'new-product';

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal">
        <h2>List a product</h2>
        <p className="sub">Find an existing catalogue product, or add a new one if it doesn't exist yet.</p>

        {error && <div className="banner error">{error}</div>}

        <form onSubmit={handleSubmit}>
          {!selectedProduct && mode === 'search' && (
            <div className="form-field">
              <label htmlFor="product-search">Search catalogue</label>
              <input
                id="product-search"
                type="text"
                placeholder="e.g. PPC Cement 50kg"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                autoFocus
              />
              {results.length > 0 && (
                <div style={{ marginTop: 8, border: '1px solid var(--border)', borderRadius: 4 }}>
                  {results.map((p) => (
                    <div
                      key={p.id}
                      onClick={() => setSelectedProduct(p)}
                      style={{ padding: '9px 12px', cursor: 'pointer', borderBottom: '1px solid var(--border)', fontSize: 13.5 }}
                    >
                      <strong>{p.name}</strong> &middot; {p.category}
                    </div>
                  ))}
                </div>
              )}
              <button
                type="button"
                onClick={() => setMode('new-product')}
                style={{ marginTop: 8, fontSize: 12.5, background: 'none', border: 'none', color: 'var(--steel)', cursor: 'pointer', textDecoration: 'underline', padding: 0 }}
              >
                Can't find it? Add a new product
              </button>
            </div>
          )}

          {selectedProduct && (
            <div className="banner info">
              Listing: <strong>{selectedProduct.name}</strong>{' '}
              <button type="button" onClick={() => setSelectedProduct(null)} style={{ marginLeft: 8, background: 'none', border: 'none', color: 'var(--steel)', cursor: 'pointer', fontSize: 12, textDecoration: 'underline' }}>
                change
              </button>
            </div>
          )}

          {mode === 'new-product' && (
            <>
              <div className="form-field">
                <label>Product name</label>
                <input
                  type="text"
                  value={newProduct.name}
                  onChange={(e) => setNewProduct({ ...newProduct, name: e.target.value })}
                  placeholder="e.g. PPC Cement 50kg"
                />
              </div>
              <div className="form-field">
                <label>Category</label>
                <input
                  type="text"
                  value={newProduct.category}
                  onChange={(e) => setNewProduct({ ...newProduct, category: e.target.value })}
                  placeholder="e.g. Cement"
                />
              </div>
              <div className="form-field">
                <label>Brand (optional)</label>
                <input
                  type="text"
                  value={newProduct.brand}
                  onChange={(e) => setNewProduct({ ...newProduct, brand: e.target.value })}
                />
              </div>
              <div className="form-field">
                <label>Unit (optional)</label>
                <input
                  type="text"
                  value={newProduct.unit}
                  onChange={(e) => setNewProduct({ ...newProduct, unit: e.target.value })}
                  placeholder="e.g. 50 kg bag"
                />
              </div>
              <button
                type="button"
                onClick={() => setMode('search')}
                style={{ fontSize: 12.5, background: 'none', border: 'none', color: 'var(--steel)', cursor: 'pointer', textDecoration: 'underline', padding: 0, marginBottom: 14 }}
              >
                &larr; search existing products instead
              </button>
            </>
          )}

          {readyToPriceIt && (
            <>
              <div className="form-field">
                <label>Price (&#8377;)</label>
                <input type="number" min="0.01" step="0.01" required value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} />
              </div>
              <div className="form-field">
                <label>Stock</label>
                <input type="number" min="0" step="1" required value={form.stock} onChange={(e) => setForm({ ...form, stock: e.target.value })} />
              </div>
              <div className="form-field">
                <label>Minimum order quantity</label>
                <input type="number" min="1" step="1" required value={form.minOrderQty} onChange={(e) => setForm({ ...form, minOrderQty: e.target.value })} />
              </div>
            </>
          )}

          <div className="form-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={!readyToPriceIt || submitting}>
              {submitting ? 'Saving…' : 'Create listing'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
