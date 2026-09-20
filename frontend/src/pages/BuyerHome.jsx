import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client.js';
import { formatMoney } from '../components/Money.jsx';
import Pagination from '../components/Pagination.jsx';

const CATEGORIES = [
  'Cement', 'Steel', 'Bricks & Blocks', 'Aggregates', 'Paint',
  'Tiles', 'Plumbing', 'Sanitaryware', 'Electrical',
];

const PAGE_SIZE = 10;

export default function BuyerHome() {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Debounce keyword search so we're not firing a request on every keystroke.
  const [debouncedQuery, setDebouncedQuery] = useState('');
  useEffect(() => {
    const t = setTimeout(() => setDebouncedQuery(query), 300);
    return () => clearTimeout(t);
  }, [query]);

  useEffect(() => setPage(0), [debouncedQuery, category]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    api
      .searchProducts({ q: debouncedQuery, category, page, size: PAGE_SIZE })
      .then((data) => {
        if (!cancelled) setResult(data);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [debouncedQuery, category, page]);

  const products = result?.content ?? [];
  const totalPages = result?.totalPages ?? 0;

  const resultsLabel = useMemo(() => {
    if (!result) return '';
    return `${result.totalElements} product${result.totalElements === 1 ? '' : 's'}`;
  }, [result]);

  return (
    <main className="page">
      <div className="page-header">
        <div>
          <h1>Browse the catalogue</h1>
          <p>Compare prices across every approved seller. {resultsLabel}</p>
        </div>
      </div>

      <div className="filter-bar">
        <input
          type="text"
          placeholder="Search by product name or brand (e.g. UltraTech cement)"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <select value={category} onChange={(e) => setCategory(e.target.value)}>
          <option value="">All categories</option>
          {CATEGORIES.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
      </div>

      {error && <div className="banner error">Couldn't load products: {error}</div>}

      {!error && !loading && products.length === 0 && (
        <div className="empty-state">
          <h3>No products match that search</h3>
          <p>Try a different keyword or clear the category filter.</p>
        </div>
      )}

      <div className="product-list">
        {products.map((p) => (
          <Link to={`/products/${p.id}`} key={p.id} className="product-row">
            <div>
              <div className="name">{p.name}</div>
              <div className="meta">
                <span className="category-tag">{p.category}</span>
                {p.brand && <span>{p.brand}</span>}
                {p.unit && <span> &middot; {p.unit}</span>}
              </div>
            </div>
            <div className="seller-count">
              <strong>{p.sellerCount}</strong>
              <div>seller{p.sellerCount === 1 ? '' : 's'}</div>
            </div>
            <div className="price-block">
              {p.fromPrice != null ? (
                <>
                  <div className="price-label">From</div>
                  <div className="price">{formatMoney(p.fromPrice)}</div>
                </>
              ) : (
                <div className="no-price">Currently unavailable</div>
              )}
            </div>
          </Link>
        ))}
      </div>

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </main>
  );
}
