import { NavLink, Route, Routes } from 'react-router-dom';
import BuyerHome from './pages/BuyerHome.jsx';
import ProductDetail from './pages/ProductDetail.jsx';
import SellerDashboard from './pages/SellerDashboard.jsx';

export default function App() {
  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="topbar-inner">
          <NavLink to="/" className="brand">
            Bajri<span>X</span>
            <small>construction &amp; building supplies</small>
          </NavLink>
          <nav className="nav-links">
            <NavLink to="/" end className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
              Browse
            </NavLink>
            <NavLink to="/seller" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
              Seller dashboard
            </NavLink>
          </nav>
        </div>
      </header>

      <Routes>
        <Route path="/" element={<BuyerHome />} />
        <Route path="/products/:id" element={<ProductDetail />} />
        <Route path="/seller" element={<SellerDashboard />} />
      </Routes>

      <p className="footer-note">BajriX take-home project &middot; demo data, not a real marketplace</p>
    </div>
  );
}
