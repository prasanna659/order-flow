import { useState } from 'react'
import { Link } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { AlertTriangle, ArrowLeft, CheckCircle2, Loader2, Mail } from 'lucide-react'
import { authApi } from '../api'

function parseError(err) {
  const data = err.response?.data
  if (!data) return err.message || 'Something went wrong. Try again.'
  if (data.errors?.length > 0) return data.errors.map((e) => `${e.field}: ${e.message}`).join(' · ')
  if (data.message) return data.message
  if (typeof data === 'string') return data
  return 'Something went wrong. Try again.'
}

export default function ForgotPasswordPage() {
  const [email, setEmail]     = useState('')
  const [loading, setLoading] = useState(false)
  const [submitted, setSubmitted] = useState(false)
  const [error, setError]     = useState(null)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await authApi.forgotPassword({ email })
      setSubmitted(true)
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
          <p className="text-textdim text-sm">Reset your password</p>
        </div>

        <div className="bg-surface border border-border rounded-2xl p-6 shadow-card">
          <AnimatePresence mode="wait">
            {submitted ? (
              /* ── Success state ── */
              <motion.div
                key="success"
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="flex flex-col items-center text-center gap-3 py-4"
              >
                <div className="w-14 h-14 rounded-full bg-success/15 flex items-center justify-center">
                  <CheckCircle2 size={28} className="text-success" />
                </div>
                <h2 className="font-display font-semibold text-lg">Check your email</h2>
                <p className="text-textdim text-sm leading-relaxed">
                  If an account exists for <span className="text-white font-medium">{email}</span>,
                  we've sent a reset link. It expires in 30 minutes.
                </p>
                <Link
                  to="/auth"
                  className="mt-2 flex items-center gap-2 text-xs text-primary hover:text-primarylight transition-colors"
                >
                  <ArrowLeft size={13} /> Back to login
                </Link>
              </motion.div>
            ) : (
              /* ── Form state ── */
              <motion.div key="form" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
                <h2 className="font-display font-semibold text-base mb-1">Forgot your password?</h2>
                <p className="text-textdim text-xs mb-5 leading-relaxed">
                  Enter your email address and we'll send you a reset link.
                </p>

                <form onSubmit={handleSubmit} className="space-y-3">
                  <div className="relative">
                    <Mail size={14} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-textdim/60" />
                    <input
                      type="email"
                      placeholder="Your email address"
                      required
                      autoComplete="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      className="w-full bg-surface2 border border-border rounded-xl pl-9 pr-3.5 py-2.5 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary/30 transition-all placeholder:text-textdim/50"
                    />
                  </div>

                  <AnimatePresence>
                    {error && (
                      <motion.div
                        key="err"
                        initial={{ opacity: 0, y: -4 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0 }}
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
                    disabled={loading}
                    className="w-full flex items-center justify-center gap-2 bg-primary text-white font-semibold py-2.5 rounded-xl shadow-glow hover:brightness-110 hover:-translate-y-0.5 transition-all disabled:opacity-50 disabled:translate-y-0 disabled:shadow-none"
                  >
                    {loading ? <><Loader2 size={15} className="animate-spin" /> Sending…</> : 'Send reset link'}
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
