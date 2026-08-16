import { useState, useEffect } from 'react'
import { Link, useSearchParams, useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { AlertTriangle, ArrowLeft, CheckCircle2, Eye, EyeOff, Loader2, XCircle } from 'lucide-react'
import { authApi } from '../api'

/* Password strength: returns 0–4 */
function strength(pw) {
  let score = 0
  if (pw.length >= 8) score++
  if (/[A-Z]/.test(pw)) score++
  if (/[0-9]/.test(pw)) score++
  if (/[^A-Za-z0-9]/.test(pw)) score++
  return score
}
const STRENGTH_LABELS = ['Weak', 'Fair', 'Good', 'Strong', 'Very strong']
const STRENGTH_COLORS = ['bg-danger', 'bg-yellow-400', 'bg-yellow-300', 'bg-success', 'bg-success']

function parseError(err) {
  const data = err.response?.data
  if (!data) return err.message || 'Something went wrong. Try again.'
  if (data.errors?.length > 0) return data.errors.map((e) => `${e.field}: ${e.message}`).join(' · ')
  if (data.message) return data.message
  if (typeof data === 'string') return data
  return 'Something went wrong. Try again.'
}

export default function ResetPasswordPage() {
  const [params]    = useSearchParams()
  const navigate    = useNavigate()
  const token       = params.get('token') || ''

  const [tokenState, setTokenState] = useState('loading') // 'loading' | 'valid' | 'invalid'
  const [tokenError, setTokenError] = useState('')

  const [password, setPassword]         = useState('')
  const [confirm, setConfirm]           = useState('')
  const [showPw, setShowPw]             = useState(false)
  const [showConfirm, setShowConfirm]   = useState(false)
  const [loading, setLoading]           = useState(false)
  const [error, setError]               = useState(null)
  const [success, setSuccess]           = useState(false)

  // Validate token on mount
  useEffect(() => {
    if (!token) {
      setTokenState('invalid')
      setTokenError('No reset token found. Please request a new password reset.')
      return
    }
    authApi.validateResetToken(token)
      .then(() => setTokenState('valid'))
      .catch((err) => {
        setTokenState('invalid')
        setTokenError(parseError(err))
      })
  }, [token])

  const pw    = strength(password)
  const match = password === confirm && confirm.length > 0

  async function handleSubmit(e) {
    e.preventDefault()
    if (!match) { setError('Passwords do not match'); return }
    if (password.length < 8) { setError('Password must be at least 8 characters'); return }
    setError(null)
    setLoading(true)
    try {
      await authApi.resetPassword({ token, newPassword: password })
      setSuccess(true)
      setTimeout(() => navigate('/auth'), 3000)
    } catch (err) {
      setError(parseError(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-12">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="w-full max-w-sm"
      >
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-primary to-primarylight mx-auto mb-5 flex items-center justify-center shadow-glow">
            <span className="font-display font-bold text-white text-2xl">O</span>
          </div>
          <h1 className="font-display font-bold text-3xl tracking-tight mb-1">OrderFlow</h1>
          <p className="text-textdim text-sm">Choose a new password</p>
        </div>

        <div className="bg-surface border border-border rounded-2xl p-6 shadow-card">
          <AnimatePresence mode="wait">

            {/* Loading token validation */}
            {tokenState === 'loading' && (
              <motion.div key="loading" className="flex flex-col items-center gap-3 py-6">
                <Loader2 size={28} className="text-primary animate-spin" />
                <p className="text-textdim text-sm font-mono">Validating reset link…</p>
              </motion.div>
            )}

            {/* Invalid token */}
            {tokenState === 'invalid' && (
              <motion.div
                key="invalid"
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="flex flex-col items-center text-center gap-3 py-4"
              >
                <div className="w-14 h-14 rounded-full bg-danger/15 flex items-center justify-center">
                  <XCircle size={28} className="text-danger" />
                </div>
                <h2 className="font-display font-semibold text-base">Link expired or invalid</h2>
                <p className="text-textdim text-sm leading-relaxed">{tokenError}</p>
                <Link
                  to="/auth/forgot-password"
                  className="mt-2 px-4 py-2 bg-primary text-white text-sm font-medium rounded-xl shadow-glow hover:brightness-110 transition-all"
                >
                  Request new link
                </Link>
                <Link
                  to="/auth"
                  className="flex items-center gap-1.5 text-xs text-textdim hover:text-white transition-colors"
                >
                  <ArrowLeft size={12} /> Back to login
                </Link>
              </motion.div>
            )}

            {/* Success */}
            {success && (
              <motion.div
                key="success"
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="flex flex-col items-center text-center gap-3 py-4"
              >
                <div className="w-14 h-14 rounded-full bg-success/15 flex items-center justify-center">
                  <CheckCircle2 size={28} className="text-success" />
                </div>
                <h2 className="font-display font-semibold text-base">Password updated!</h2>
                <p className="text-textdim text-sm">Redirecting you to login…</p>
              </motion.div>
            )}

            {/* Valid token — show form */}
            {tokenState === 'valid' && !success && (
              <motion.div key="form" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
                <h2 className="font-display font-semibold text-base mb-1">New password</h2>
                <p className="text-textdim text-xs mb-5">Must be at least 8 characters.</p>

                <form onSubmit={handleSubmit} className="space-y-3">
                  {/* New password */}
                  <div className="relative">
                    <input
                      type={showPw ? 'text' : 'password'}
                      placeholder="New password"
                      required
                      autoComplete="new-password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="w-full bg-surface2 border border-border rounded-xl px-3.5 py-2.5 pr-10 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary/30 transition-all placeholder:text-textdim/50"
                    />
                    <button type="button" onClick={() => setShowPw((v) => !v)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-textdim hover:text-white">
                      {showPw ? <EyeOff size={14} /> : <Eye size={14} />}
                    </button>
                  </div>

                  {/* Strength bar */}
                  {password.length > 0 && (
                    <div className="space-y-1">
                      <div className="flex gap-1">
                        {[0, 1, 2, 3].map((i) => (
                          <div key={i}
                            className={`h-1 flex-1 rounded-full transition-all duration-300 ${
                              i < pw ? STRENGTH_COLORS[pw] : 'bg-border'
                            }`}
                          />
                        ))}
                      </div>
                      <p className={`text-[10px] font-mono ${
                        pw <= 1 ? 'text-danger' : pw <= 2 ? 'text-yellow-400' : 'text-success'
                      }`}>{STRENGTH_LABELS[pw]}</p>
                    </div>
                  )}

                  {/* Confirm password */}
                  <div className="relative">
                    <input
                      type={showConfirm ? 'text' : 'password'}
                      placeholder="Confirm new password"
                      required
                      autoComplete="new-password"
                      value={confirm}
                      onChange={(e) => setConfirm(e.target.value)}
                      className={`w-full bg-surface2 border rounded-xl px-3.5 py-2.5 pr-10 text-sm outline-none transition-all placeholder:text-textdim/50 ${
                        confirm.length > 0
                          ? match ? 'border-success/50 focus:ring-success/30' : 'border-danger/50 focus:ring-danger/30'
                          : 'border-border focus:border-primary focus:ring-primary/30'
                      }`}
                    />
                    <button type="button" onClick={() => setShowConfirm((v) => !v)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-textdim hover:text-white">
                      {showConfirm ? <EyeOff size={14} /> : <Eye size={14} />}
                    </button>
                  </div>
                  {confirm.length > 0 && !match && (
                    <p className="text-[11px] text-danger font-mono">Passwords do not match</p>
                  )}

                  <AnimatePresence>
                    {error && (
                      <motion.div
                        key="err"
                        initial={{ opacity: 0, y: -4 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}
                        className="flex items-start gap-2 bg-danger/10 border border-danger/30 rounded-xl px-3.5 py-2.5"
                        role="alert"
                      >
                        <AlertTriangle size={14} className="text-danger flex-shrink-0 mt-0.5" />
                        <p className="text-danger text-xs font-mono">{error}</p>
                      </motion.div>
                    )}
                  </AnimatePresence>

                  <button
                    type="submit"
                    disabled={loading || !match || password.length < 8}
                    className="w-full flex items-center justify-center gap-2 bg-primary text-white font-semibold py-2.5 rounded-xl shadow-glow hover:brightness-110 hover:-translate-y-0.5 transition-all disabled:opacity-50 disabled:translate-y-0 disabled:shadow-none"
                  >
                    {loading ? <><Loader2 size={15} className="animate-spin" /> Saving…</> : 'Set new password'}
                  </button>
                </form>

                <div className="mt-4 text-center">
                  <Link
                    to="/auth"
                    className="flex items-center justify-center gap-1.5 text-xs text-textdim hover:text-white transition-colors"
                  >
                    <ArrowLeft size={12} /> Back to login
                  </Link>
                </div>
              </motion.div>
            )}

          </AnimatePresence>
        </div>
      </motion.div>
    </div>
  )
}
