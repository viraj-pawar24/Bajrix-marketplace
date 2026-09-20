import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import { SellerAuthProvider } from './context/SellerAuthContext.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <SellerAuthProvider>
        <App />
      </SellerAuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
