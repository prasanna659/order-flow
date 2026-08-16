import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, ChevronLeft, ChevronRight, Package, Calendar, DollarSign, Filter, ChevronRight as ChevronRightIcon, RefreshCw, AlertTriangle } from 'lucide-react'
import { orderApi } from '../api'

export default function OrdersPage({ auth, onBack, onRetry }) {
  const navigate = useNavigate()
  const handleBack = onBack || (() => navigate('/catalog'))
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [pageSize] = useState(10)
  const [totalPages, setTotalPages] = useState(0)
  const [statusFilter, setStatusFilter] = useState('')
  const [selectedOrder, setSelectedOrder] = useState(null)
  const [loadingDetail, setLoadingDetail] = useState(false)

  const statusOptions = [
    { value: '', label: 'All Orders' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'RESERVING', label: 'Reserving' },
    { value: 'CHARGING', label: 'Charging' },
    { value: 'CONFIRMED', label: 'Confirmed' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ]

  useEffect(() => {
    loadOrders()
  }, [currentPage, statusFilter])

  async function loadOrderDetail(orderId) {
    setLoadingDetail(true)
    try {
      const res = await orderApi.getOrder(orderId)
      setSelectedOrder(res.data)
    } catch (e) {
      setError('Failed to load order details: ' + (e.response?.data?.message || e.message))
    } finally {
      setLoadingDetail(false)
    }
  }

  async function loadOrders() {
    setLoading(true)
    setError(null)
    try {
      const res = await orderApi.getOrdersPaginated(
        auth.userId,
        currentPage,
        pageSize,
        statusFilter || null
      )
      setOrders(res.data.content)
      setTotalPages(res.data.totalPages)
    } catch (e) {
      setError('Failed to load orders: ' + (e.response?.data?.message || e.message))
    } finally {
      setLoading(false)
    }
  }

  function formatDate(isoString) {
    return new Date(isoString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  function getStatusColor(status) {
    const colors = {
      PENDING: 'text-primary',
      RESERVING: 'text-primary',
      CHARGING: 'text-primary',
      CONFIRMED: 'text-success',
      CANCELLED: 'text-danger'
    }
    return colors[status] || 'text-textdim'
  }

  function getStatusBg(status) {
    const colors = {
      PENDING: 'bg-primary/10 border-primary/30',
      RESERVING: 'bg-primary/10 border-primary/30',
      CHARGING: 'bg-primary/10 border-primary/30',
      CONFIRMED: 'bg-success/10 border-success/30',
      CANCELLED: 'bg-danger/10 border-danger/30'
    }
    return colors[status] || 'bg-surface2 border-border'
  }

  function getCancellationType(reason) {
    if (!reason) return 'UNKNOWN'
    const lowerReason = reason.toLowerCase()
    if (lowerReason.includes('payment') || lowerReason.includes('card') || lowerReason.includes('charge')) {
      return 'PAYMENT'
    }
    if (lowerReason.includes('inventory') || lowerReason.includes('stock') || lowerReason.includes('reserve')) {
      return 'INVENTORY'
    }
    return 'OTHER'
  }

  if (selectedOrder) {
    return (
      <div className="min-h-screen bg-ink">
        <div className="max-w-6xl mx-auto px-6 py-10">
          <button
            onClick={() => setSelectedOrder(null)}
            className="flex items-center gap-2 text-textdim hover:text-white mb-6 transition-colors"
          
          >
            <ArrowLeft size={20} />
            Back to orders
          </button>

          {loadingDetail ? (
            <div className="text-center py-12">
              <div className="inline-block w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
              <p className="text-textdim mt-4">Loading order details...</p>
            </div>
          ) : (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="bg-surface border border-border rounded-2xl p-6 shadow-card"
            >
            <div className="flex justify-between items-start mb-6">
              <div>
                <h1 className="font-display font-bold text-2xl mb-2">Order Details</h1>
                <p className="font-mono text-sm text-textdim">{selectedOrder.id}</p>
              </div>
              <span className={`px-3 py-1 rounded-full text-sm font-mono font-medium border ${getStatusBg(selectedOrder.status)} ${getStatusColor(selectedOrder.status)}`}>
                {selectedOrder.status}
              </span>
            </div>

            <div className="grid grid-cols-3 gap-4 mb-6">
              <div className="bg-surface2 rounded-lg p-4">
                <div className="flex items-center gap-2 text-textdim text-sm mb-1">
                  <Calendar size={16} />
                  Date
                </div>
                <p className="font-mono text-sm">{formatDate(selectedOrder.createdAt)}</p>
              </div>
              <div className="bg-surface2 rounded-lg p-4">
                <div className="flex items-center gap-2 text-textdim text-sm mb-1">
                  <DollarSign size={16} />
                  Total
                </div>
                <p className="font-mono text-lg">${selectedOrder.totalAmount?.toFixed(2) || '0.00'}</p>
              </div>
              <div className="bg-surface2 rounded-lg p-4">
                <div className="flex items-center gap-2 text-textdim text-sm mb-1">
                  <Package size={16} />
                  Items
                </div>
                <p className="font-mono text-lg">{selectedOrder.itemCount || 0}</p>
              </div>
            </div>

            {selectedOrder.failureReason && (
              <div className="bg-danger/10 border border-danger/30 rounded-lg p-4 mb-6">
                <div className="flex items-center gap-2 mb-2">
                  <AlertTriangle size={16} className="text-danger" />
                  <p className="text-danger text-sm font-medium">
                    {getCancellationType(selectedOrder.failureReason) === 'PAYMENT' ? 'Payment declined' : 
                     getCancellationType(selectedOrder.failureReason) === 'INVENTORY' ? 'Inventory unavailable' : 
                     'Order failed'}
                  </p>
                </div>
                <p className="text-textdim text-sm">{selectedOrder.failureReason}</p>
              </div>
            )}

            {selectedOrder.status === 'CANCELLED' && selectedOrder.items && selectedOrder.items.length > 0 && onRetry && (
              <button
                onClick={() => {
                  onRetry(selectedOrder.items, true)
                  setSelectedOrder(null)
                }}
                className="mb-6 flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg font-medium hover:bg-primary/80 transition-colors text-sm"
              >
                <RefreshCw size={16} />
                Retry order
              </button>
            )}

            <div>
              <h2 className="font-display font-semibold mb-4">Order Items</h2>
              {selectedOrder.items && selectedOrder.items.length > 0 ? (
                <div className="space-y-2">
                  {selectedOrder.items.map((item, index) => (
                    <div key={index} className="bg-surface2 rounded-lg p-4 flex justify-between items-center">
                      <div>
                        <p className="font-medium">{item.productName}</p>
                        <p className="text-textdim text-sm">Qty: {item.quantity}</p>
                      </div>
                      <p className="font-mono">${(item.price * item.quantity).toFixed(2)}</p>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-textdim text-sm">No items available</p>
              )}
            </div>
          </motion.div>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-ink">
      <div className="max-w-6xl mx-auto px-6 py-10">
        <div className="flex items-center justify-between mb-8">
          <div>
            <button
              onClick={handleBack}
              className="flex items-center gap-2 text-textdim hover:text-white mb-4 transition-colors"
            >
              <ArrowLeft size={20} />
              Back to catalog
            </button>
            <h1 className="font-display font-bold text-2xl">Order History</h1>
            <p className="text-textdim text-sm mt-1">View and manage your past orders</p>
          </div>

          <div className="flex items-center gap-2">
            <Filter size={18} className="text-textdim" />
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value)
                setCurrentPage(0)
              }}
              className="bg-surface border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-primary/50"
            >
              {statusOptions.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        {loading ? (
          <div className="text-center py-12">
            <div className="inline-block w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
            <p className="text-textdim mt-4">Loading orders...</p>
          </div>
        ) : error ? (
          <div className="bg-danger/10 border border-danger/30 rounded-lg p-6 text-center">
            <p className="text-danger">{error}</p>
            <button
              onClick={loadOrders}
              className="mt-4 px-4 py-2 bg-danger text-white rounded-lg font-medium hover:bg-danger/80 transition-colors"
            >
              Retry
            </button>
          </div>
        ) : orders.length === 0 ? (
          <div className="text-center py-12">
            <Package size={48} className="text-textdim mx-auto mb-4" />
            <p className="text-textdim">No orders found</p>
          </div>
        ) : (
          <>
            <div className="space-y-3">
              {orders.map((order) => (
                <motion.div
                  key={order.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  onClick={() => loadOrderDetail(order.id)}
                  className="bg-surface border border-border rounded-xl p-4 hover:border-primary/50 hover:-translate-y-0.5 shadow-card cursor-pointer transition-all"
                >
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <span className={`px-2 py-0.5 rounded text-xs font-mono font-medium border ${getStatusBg(order.status)} ${getStatusColor(order.status)}`}>
                          {order.status}
                        </span>
                        <p className="font-mono text-xs text-textdim">{order.id}</p>
                      </div>
                      <p className="font-mono text-lg">${order.totalAmount?.toFixed(2) || '0.00'}</p>
                      <p className="text-textdim text-sm mt-1">{order.itemCount || 0} items • {formatDate(order.createdAt)}</p>
                    </div>
                    <ChevronRightIcon size={20} className="text-textdim" />
                  </div>
                </motion.div>
              ))}
            </div>

            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-4 mt-8">
                <button
                  onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                  disabled={currentPage === 0}
                  className="p-2 bg-surface border border-border rounded-lg hover:border-primary/50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  <ChevronLeft size={20} />
                </button>
                <span className="text-textdim text-sm">
                  Page {currentPage + 1} of {totalPages}
                </span>
                <button
                  onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={currentPage === totalPages - 1}
                  className="p-2 bg-surface border border-border rounded-lg hover:border-primary/50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  <ChevronRight size={20} />
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
