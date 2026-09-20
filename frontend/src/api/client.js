const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

/**
 * Thin fetch wrapper. Throws an Error whose `.details` carries the parsed
 * backend ErrorResponse (status/message/validation details) so callers can
 * show something more useful than "request failed".
 */
async function request(path, { method = 'GET', body, sellerId, headers } = {}) {
  const finalHeaders = { 'Content-Type': 'application/json', ...headers };
  if (sellerId) {
    finalHeaders['X-Seller-Id'] = sellerId;
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: finalHeaders,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 204) return null;

  const isJson = res.headers.get('content-type')?.includes('application/json');
  const data = isJson ? await res.json() : null;

  if (!res.ok) {
    const err = new Error((data && data.message) || `Request failed with status ${res.status}`);
    err.status = res.status;
    err.details = data;
    throw err;
  }
  return data;
}

export const api = {
  // Buyer / catalogue
  searchProducts: (params) => {
    const qs = new URLSearchParams(
      Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined && v !== '' && v !== null))
    ).toString();
    return request(`/products?${qs}`);
  },
  getProduct: (id) => request(`/products/${id}`),
  createProduct: (payload) => request('/products', { method: 'POST', body: payload }),

  // Sellers directory (mock login)
  listSellers: () => request('/sellers'),

  // Seller-scoped listings
  myListings: (sellerId) => request('/seller/listings', { sellerId }),
  createListing: (sellerId, payload) => request('/seller/listings', { method: 'POST', body: payload, sellerId }),
  updateListing: (sellerId, id, payload) =>
    request(`/seller/listings/${id}`, { method: 'PUT', body: payload, sellerId }),
  stopSellingListing: (sellerId, id, version) =>
    request(`/seller/listings/${id}`, { method: 'DELETE', body: { version }, sellerId }),
};
