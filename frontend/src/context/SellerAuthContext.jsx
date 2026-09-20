import { createContext, useContext, useEffect, useState } from 'react';

const SellerAuthContext = createContext(null);

const STORAGE_KEY = 'bajrix.activeSellerId';

/**
 * Mocked "log in as seller" state. Holds the currently selected seller id
 * (persisted to localStorage for convenience across reloads) and exposes it
 * for the api client to send as the X-Seller-Id header.
 */
export function SellerAuthProvider({ children }) {
  const [activeSeller, setActiveSeller] = useState(null);

  useEffect(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      try {
        setActiveSeller(JSON.parse(saved));
      } catch {
        localStorage.removeItem(STORAGE_KEY);
      }
    }
  }, []);

  function selectSeller(seller) {
    setActiveSeller(seller);
    if (seller) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(seller));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  }

  return (
    <SellerAuthContext.Provider value={{ activeSeller, selectSeller }}>
      {children}
    </SellerAuthContext.Provider>
  );
}

export function useSellerAuth() {
  const ctx = useContext(SellerAuthContext);
  if (!ctx) throw new Error('useSellerAuth must be used within SellerAuthProvider');
  return ctx;
}
