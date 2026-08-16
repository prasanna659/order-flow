import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { AlertTriangle, Eye, EyeOff, Loader2, User, Mail, Lock } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { authApi } from '../api'

const AUTH_DIRECT_URL = import.meta.env.VITE_AUTH_DIRECT_URL || 'http://localhost:8081'

function parseError(err) {
  const data = err.response?.data
  if (!data) return err.message || 'Something went wrong. Try again.'
  if (data.errors?.length > 0) return data.errors.map((e) => `${e.field}: ${e.message}`).join(' · ')
  if (data.message) return data.message
  if (typeof data === 'string') return data
  return 'Something went wrong. Try again.'
}

function GoogleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 48 48" aria-hidden="true" className="flex-shrink-0">
      <path fill="#FFC107" d="M43.6 20.1H42V20H24v8h11.3C33.7 32.7 29.2 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3 0 5.8 1.1 7.9 3l5.7-5.7C34.1 6.5 29.3 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.6-.4-3.9z"/>
      <path fill="#FF3D00" d="M6.3 14.7l6.6 4.8C14.6 16 19 12 24 12c3 0 5.8 1.1 7.9 3l5.7-5.7C34.1 6.5 29.3 4 24 4 16.3 4 9.7 8.3 6.3 14.7z"/>
      <path fill="#4CAF50" d="M24 44c5.2 0 9.9-1.9 13.5-5.1l-6.2-5.3C29.4 35.5 26.8 36 24 36c-5.2 0-9.6-3.3-11.2-8L6.1 33.3C9.5 39.6 16.2 44 24 44z"/>
      <path fill="#1976D2" d="M43.6 20.1H42V20H24v8h11.3c-.8 2.3-2.3 4.2-4.2 5.6l6.2 5.3C40.8 36 44 30.5 44 24c0-1.3-.1-2.6-.4-3.9z"/>
    </svg>
  )
}

/* ── Labelled input with leading icon ──────────────────────────────────── */
function InputField({ label, id, icon: Icon, rightSlot, ...props }) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-xs font-medium text-slate-300 pl-0.5">
        {label}
      </label>
      <div className="relative">
        {Icon && (
          <Icon
            size={15}
            className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none"
          />
        )}
        <input
          id={id}
          {...props}
          className={`w-full bg-[#1A1E2C] border border-[#262B3B] rounded-xl py-2.5 text-sm text-white
            outline-none transition-all
            focus:border-[#6D5CF5] focus:ring-2 focus:ring-[#6D5CF5]/20
            placeholder:text-slate-500
            ${Icon ? 'pl-10' : 'pl-3.5'}
            ${rightSlot ? 'pr-10' : 'pr-3.5'}
          `}
        />
        {rightSlot && (
          <div className="absolute right-3 top-1/2 -translate-y-1/2">{rightSlot}</div>
        )}
      </div>
    </div>
  )
}

export default function AuthScreen({ onAuthenticated }) {
  const [mode, setMode]                 = useState('login')
  const [form, setForm]                 = useState({ username: '', password: '', email: '' })
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError]               = useState(null)
  const [loading, setLoading]           = useState(false)
  const [searchParams, setSearchParams] = useSearchParams()

  useEffect(() => {
    const oauthError = searchParams.get('error')
    if (oauthError) {
      setError(decodeURIComponent(oauthError))
      setSearchParams({}, { replace: true })
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  function handleGoogleLogin() {
    window.location.href = `${AUTH_DIRECT_URL}/oauth2/authorization/google`
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const res = mode === 'login'
        ? await authApi.login({ username: form.username, password: form.password })
        : await authApi.register(form)
      onAuthenticated({ ...res.data, email: form.email })
    } catch (err) {
      setError(parseError(err))
    } finally {
      setLoading(false)
    }
  }

  const eyeToggle = (
    <button
      type="button"
      onClick={() => setShowPassword((v) => !v)}
      className="text-slate-500 hover:text-white transition-colors"
      aria-label={showPassword ? 'Hide password' : 'Show password'}
    >
      {showPassword ? <EyeOff size={15} /> : <Eye size={15} />}
    </button>
  )

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-12">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45, ease: 'easeOut' }}
        className="w-full max-w-sm"
      >
        {/* ── Logo ──────────────────────────────────────────────── */}
        <div className="text-center mb-8">
          <motion.div
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ delay: 0.1, duration: 0.4 }}
            className="w-14 h-14 rounded-2xl bg-gradient-to-br from-[#6D5CF5] to-[#8B7CFF] mx-auto mb-5 flex items-center justify-center shadow-[0_0_0_1px_rgba(109,92,245,0.25),0_8px_24px_-8px_rgba(109,92,245,0.45)]"
          >
            <span className="font-bold text-white text-2xl select-none">O</span>
          </motion.div>
          <h1 className="font-bold text-3xl tracking-tight text-white mb-1">OrderFlow</h1>
          <p className="text-slate-400 text-sm">Event-driven microservices demo platform</p>
        </div>

        {/* ── Card ──────────────────────────────────────────────── */}
        <div className="bg-[#12151F] border border-[#262B3B] rounded-2xl p-6 shadow-[0_1px_2px_rgba(0,0,0,0.4),0_8px_24px_-12px_rgba(0,0,0,0.5)]">

          {/* Tab toggle */}
          <div className="flex gap-1 mb-6 bg-[#1A1E2C] rounded-xl p-1">
            {(['login', 'register']).map((m) => (
              <button
                key={m}
                type="button"
                onClick={() => { setMode(m); setError(null) }}
                className={`flex-1 py-2 rounded-lg text-sm font-semibold transition-all capitalize tracking-wide ${
                  mode === m
                    ? 'bg-[#6D5CF5] text-white shadow-[0_0_0_1px_rgba(109,92,245,0.25),0_4px_12px_-4px_rgba(109,92,245,0.45)]'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                {m === 'login' ? 'Log in' : 'Register'}
              </button>
            ))}
          </div>

          {/* Google button */}
          <button
            type="button"
            onClick={handleGoogleLogin}
            className="w-full flex items-center justify-center gap-3 bg-[#1A1E2C] border border-[#262B3B] hover:border-slate-500 hover:bg-[#20253A] text-white rounded-xl py-2.5 text-sm font-medium transition-all mb-5 group"
          >
            <GoogleIcon />
            <span className="text-slate-300 group-hover:text-white transition-colors">
              Continue with Google
            </span>
          </button>

          {/* OR divider */}
          <div className="flex items-center gap-3 mb-5">
            <div className="flex-1 h-px bg-[#262B3B]" />
            <span className="text-[11px] font-mono text-slate-500 uppercase tracking-widest select-none">
              or
            </span>
            <div className="flex-1 h-px bg-[#262B3B]" />
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4">

            <InputField
              label="Username"
              id="username"
              icon={User}
              type="text"
              placeholder="e.g. john_doe"
              required
              autoComplete="username"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
            />

            <AnimatePresence>
              {mode === 'register' && (
                <motion.div
                  key="email-field"
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  transition={{ duration: 0.22 }}
                  className="overflow-hidden"
                >
                  <InputField
                    label="Email address"
                    id="email"
                    icon={Mail}
                    type="email"
                    placeholder="you@example.com"
                    required
                    autoComplete="email"
                    value={form.email}
                    onChange={(e) => setForm({ ...form, email: e.target.value })}
                  />
                </motion.div>
              )}
            </AnimatePresence>

            <InputField
              label={mode === 'register' ? 'Password (min 6 characters)' : 'Password'}
              id="password"
              icon={Lock}
              type={showPassword ? 'text' : 'password'}
              placeholder={mode === 'register' ? 'Choose a strong password' : 'Enter your password'}
              required
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              rightSlot={eyeToggle}
            />

            {/* Forgot password */}
            {mode === 'login' && (
              <div className="flex justify-end -mt-1">
                <Link
                  to="/auth/forgot-password"
                  className="text-xs text-slate-500 hover:text-[#6D5CF5] transition-colors"
                >
                  Forgot password?
                </Link>
              </div>
            )}

            {/* Error */}
            <AnimatePresence>
              {error && (
                <motion.div
                  key="error"
                  initial={{ opacity: 0, y: -4 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.2 }}
                  className="flex items-start gap-2.5 bg-red-500/10 border border-red-500/25 rounded-xl px-3.5 py-2.5"
                  role="alert"
                >
                  <AlertTriangle size={14} className="text-red-400 flex-shrink-0 mt-0.5" />
                  <p className="text-red-400 text-xs font-mono leading-relaxed">{error}</p>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Submit */}
            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center gap-2 bg-[#6D5CF5] hover:bg-[#7C6CF6] active:bg-[#5D4CE5] text-white font-semibold py-2.5 rounded-xl transition-all
                shadow-[0_0_0_1px_rgba(109,92,245,0.25),0_4px_16px_-4px_rgba(109,92,245,0.5)]
                hover:shadow-[0_0_0_1px_rgba(109,92,245,0.35),0_6px_20px_-4px_rgba(109,92,245,0.6)]
                hover:-translate-y-0.5
                disabled:opacity-50 disabled:translate-y-0 disabled:shadow-none disabled:cursor-not-allowed
                text-sm tracking-wide"
            >
              {loading ? (
                <><Loader2 size={15} className="animate-spin" /> Please wait…</>
              ) : mode === 'login' ? (
                'Log in to OrderFlow'
              ) : (
                'Create account'
              )}
            </button>
          </form>
        </div>

        {/* Footer note */}
        <p className="text-center text-xs text-slate-600 mt-5">
          By continuing you agree this is a demo project, not a real store.
        </p>
      </motion.div>
    </div>
  )
}
