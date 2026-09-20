import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { formatMoney } from '../components/Money.jsx';

export default function ProductDetail() {
  const { id } = useParams();
  const [product, setProduct] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    api
      .getProduct(id)
      .then((data) => !cancelled && setProduct(data))
      .catch((err) => !cancelled && setError(err.message))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [id]);

  if (loading) return <main className="page">Loading&hellip;</main>;

  if (error || !product) {
    return (
      <main className="page">
        <div className="empty-state">
          <h3>Product not found</h3>
          <p>{error || 'This product may have been removed.'}</p>
          <p style={{ marginTop: 16 }}>
            <Link to="/">&larr; Back to browsing</Link>
          </p>
        </div>
      </main>
    );
  }

  const listings = product.listings ?? [];
  const cheapestId = listings.find((l) => l.orderable)?.id;

  return (
    <main className="page">
      <p style={{ marginBottom: 12 }}>
        <Link to="/" style={{ fontSize: 13, color: 'var(--ink-soft)' }}>
          &larr; Back to browsing
        </Link>
      </p>

      <div className="product-detail-head">
        <span className="category-tag">{product.category}</span>
        <h1>{product.name}</h1>
        <div className="brand-unit">
          {product.brand && <span>{product.brand}</span>}
          {product.unit && <span> &middot; {product.unit}</span>}
        </div>
        {product.description && <p className="description">{product.description}</p>}
      </div>

      {listings.length === 0 ? (
        <div className="empty-state">
          <h3>No sellers currently offer this product</h3>
          <p>Check back later, or browse similar products in the same category.</p>
        </div>
      ) : (
        <table className="listing-table">
          <thead>
            <tr>
              <th>Seller</th>
              <th>Price</th>
              <th>Stock</th>
              <th>Min. order</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {listings.map((l) => (
              <tr key={l.id} className={l.id === cheapestId ? 'best-price' : ''}>
                <td className="seller-name">
                  {l.sellerName}
                  {l.id === cheapestId && (
                    <span className="badge ok" style={{ marginLeft: 8 }}>
                      Best price
                    </span>
                  )}
                </td>
                <td className="price-cell">{formatMoney(l.price)}</td>
                <td>{l.stock}</td>
                <td>{l.minOrderQty}</td>
                <td>{statusBadge(l)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  );
}

function statusBadge(listing) {
  if (listing.orderable) return <span className="badge ok">In stock</span>;
  if (listing.stock === 0) return <span className="badge bad">Out of stock</span>;
  return <span className="badge warn">Below min. order</span>;
}
