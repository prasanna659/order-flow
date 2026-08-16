import { useEffect, useState } from 'react'
import { Routes, Route, Navigate, useNavigate } from 'react-router-dom'
import Navbar from './components/Navbar'
import AuthScreen from './components/AuthScreen'
import AuthCallback from './components/AuthCallback'
import ProductGrid from './components/ProductGrid'
import CartDrawer from './components/CartDrawer'
import OrderTracker from './components/OrderTracker'
import OrdersPage from './components/OrdersPage'
import LandingPage from './pages/LandingPage'
import ArchitecturePage from './pages/ArchitecturePage'
import SystemHealthPage from './pages/SystemHealthPage'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import ResetPasswordPage from './pages/ResetPasswordPage'
import { SkeletonProductCard } from './components/shared/SkeletonCard'
import EmptyState from './components/shared/EmptyState'
import { ToastContainer, useToast } from './components/shared/Toast'
import { Package } from 'lucide-react'
import { inventoryApi, orderApi } from './api'

/* ── Catalog page ─────────────────────────────────────────────────────── */
function CatalogPage({ products, loadingProducts, onAddToCart }) {
  if (loadingProducts) {
    return (
      <main className="max-w-7xl mx-auto px-6 py-10">
        <div className="mb-8">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-[10px] font-mono text-primary bg-primary/10 border border-primary/20 px-2.5 py-1 rounded-full">
              PRODUCT CATALOG
            </span>
          </div>
          <h1 className="font-display font-bold text-3xl tracking-tight">Shop</h1>
          <p className="text-textdim text-sm mt-2 max-w-lg">
            Every purchase runs a distributed Saga — reserve stock, charge payment, confirm order — with automatic compensation on failure.
          </p>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          {Array.from({ length: 8 }).map((_, i) => <SkeletonProductCard key={i} />)}
        </div>
      </main>
    )
  }

  return (
    <main className="max-w-7xl mx-auto px-6 py-10">
      <div className="mb-8">
        <div className="flex items-center gap-2 mb-2">
          <span className="text-[10px] font-mono text-primary bg-primary/10 border border-primary/20 px-2.5 py-1 rounded-full">
            PRODUCT CATALOG
          </span>
          {products.length > 0 && (
            <span className="text-[10px] font-mono text-textdim/50">{products.length} items</span>
          )}
        </div>
        <h1 className="font-display font-bold text-3xl tracking-tight">Shop</h1>
        <p className="text-textdim text-sm mt-2 max-w-lg">
          Every purchase runs a distributed Saga — reserve stock, charge payment, confirm order — with automatic compensation on failure.
        </p>
      </div>
      {products.length === 0 ? (
        <EmptyState
          icon={Package}
          title="No products available"
          description="The inventory service may still be starting up. Check System Health to verify services are running."
        />
      ) : (
        <ProductGrid products={products} onAddToCart={onAddToCart} />
      )}
    </main>
  )
}

/* ── Root app ─────────────────────────────────────────────────────────── */
export default function App() {
  const [auth, setAuth]                     = useState(null)
  const [products, setProducts]             = useState([])
  const [loadingProducts, setLoadingProducts] = useState(false)
  const [cart, setCart]                     = useState([])
  const [cartOpen, setCartOpen]             = useState(false)
  const [checkingOut, setCheckingOut]       = useState(false)
  const [activeOrderId, setActiveOrderId]   = useState(null)
  const navigate                            = useNavigate()
  const { toasts, success, error: toastError, dismiss } = useToast()

  // ── Restore session from localStorage ───────────────────────────────
  useEffect(() => {
    const token    = localStorage.getItem('orderflow_token')
    const username = localStorage.getItem('orderflow_username')
    const userId   = localStorage.getItem('orderflow_userId')
    const email    = localStorage.getItem('orderflow_email')
    if (token) setAuth({ token, username, userId: Number(userId), email })
  }, [])

  // ── Load products once authenticated ────────────────────────────────
  useEffect(() => {
    if (!auth) return
    setLoadingProducts(true)
    inventoryApi.listProducts()
      .then((res) => setProducts(res.data))
      .catch(() => {
        setProducts([])
        toastError('Inventory unavailable', 'Could not load products. Is the stack running?')
      })
      .finally(() => setLoadingProducts(false))
  }, [auth])   // eslint-disable-line react-hooks/exhaustive-deps

  // ── Auth handlers ────────────────────────────────────────────────────
  function handleAuthenticated(data) {
    localStorage.setItem('orderflow_token',    data.token)
    localStorage.setItem('orderflow_username', data.username)
    localStorage.setItem('orderflow_userId',   data.userId)
    if (data.email) localStorage.setItem('orderflow_email', data.email)
    setAuth({
      token:    data.token,
      username: data.username,
      userId:   data.userId,
      email:    data.email || '',
    })
    success('Welcome back!', `Logged in as ${data.username}`)
    navigate('/catalog')
  }

  function handleLogout() {
    localStorage.clear()
    setAuth(null)
    setCart([])
    navigate('/')
  }

  // ── Cart handlers ────────────────────────────────────────────────────
  function addToCart(product) {
    setCart((prev) => {
      const existing = prev.find((i) => i.productId === product.id)
      if (existing) {
        return prev.map((i) =>
          i.productId === product.id ? { ...i, quantity: i.quantity + 1 } : i
        )
      }
      return [...prev, {
        productId:   product.id,
        productName: product.name,
        price:       product.price,
        quantity:    1,
      }]
    })
    setCartOpen(true)
  }

  function updateQty(productId, qty) {
    if (qty <= 0) {
      setCart((prev) => prev.filter((i) => i.productId !== productId))
    } else {
      setCart((prev) => prev.map((i) =>
        i.productId === productId ? { ...i, quantity: qty } : i
      ))
    }
  }

  function removeItem(productId) {
    setCart((prev) => prev.filter((i) => i.productId !== productId))
  }

  // ── Order handlers ───────────────────────────────────────────────────
  async function handleCheckout() {
    setCheckingOut(true)
    try {
      const res = await orderApi.placeOrder({
        userId:    auth.userId,
        userEmail: auth.email    || '',
        username:  auth.username || '',
        items:     cart,
      })
      setActiveOrderId(res.data.id)
      setCart([])
      setCartOpen(false)
    } catch (e) {
      const msg = e.response?.data?.message || e.response?.data || e.message
      toastError('Order failed', String(msg))
    } finally {
      setCheckingOut(false)
    }
  }

  async function handleRetryOrder(items) {
    setCheckingOut(true)
    setActiveOrderId(null)
    try {
      const res = await orderApi.placeOrder({
        userId:    auth.userId,
        userEmail: auth.email    || '',
        username:  auth.username || '',
        items,
      })
      setActiveOrderId(res.data.id)
      navigate('/catalog')
    } catch (e) {
      const msg = e.response?.data?.message || e.response?.data || e.message
      toastError('Retry failed', String(msg))
    } finally {
      setCheckingOut(false)
    }
  }

  const cartCount = cart.reduce((sum, i) => sum + i.quantity, 0)

  return (
    <div className="min-h-screen">
      <Navbar
        username={auth?.username}
        cartCount={cartCount}
        onCartClick={() => setCartOpen(true)}
        onLogout={handleLogout}
      />

      <Routes>
        {/* Public */}
        <Route path="/"             element={<LandingPage />} />
        <Route path="/architecture" element={<ArchitecturePage />} />
        <Route path="/health"       element={<SystemHealthPage />} />

        {/* Auth */}
        <Route
          path="/auth"
          element={
            auth
              ? <Navigate to="/catalog" replace />
              : <AuthScreen onAuthenticated={handleAuthenticated} />
          }
        />

        {/* OAuth2 callback — Google redirects here after login */}
        <Route
          path="/auth/callback"
          element={<AuthCallback onAuthenticated={handleAuthenticated} />}
        />

        {/* Forgot / Reset password — public, no auth needed */}
        <Route path="/auth/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/auth/reset-password"  element={<ResetPasswordPage />} />

        {/* Protected */}
        <Route
          path="/catalog"
          element={
            auth
              ? <CatalogPage
                  products={products}
                  loadingProducts={loadingProducts}
                  onAddToCart={addToCart}
                />
              : <Navigate to="/auth" replace />
          }
        />
        <Route
          path="/orders"
          element={
            auth
              ? <OrdersPage
                  auth={auth}
                  onBack={() => navigate('/catalog')}
                  onRetry={handleRetryOrder}
                />
              : <Navigate to="/auth" replace />
          }
        />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>

      {/* ── Global overlays ────────────────────────────────────────── */}
      {auth && (
        <>
          <CartDrawer
            open={cartOpen}
            items={cart}
            onClose={() => setCartOpen(false)}
            onUpdateQty={updateQty}
            onRemove={removeItem}
            onCheckout={handleCheckout}
            checkingOut={checkingOut}
          />
          <OrderTracker
            orderId={activeOrderId}
            onClose={() => setActiveOrderId(null)}
            onRetry={handleRetryOrder}
          />
        </>
      )}

      {/* ── Toast notifications ────────────────────────────────────── */}
      <ToastContainer toasts={toasts} onDismiss={dismiss} />
    </div>
  )
}
