import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Loader2 } from 'lucide-react'

/**
 * Handles the redirect from auth-service after a successful Google login.
 *
 * The OAuth2AuthenticationSuccessHandler on the backend redirects to:
 *   /auth/callback?token=...&username=...&userId=...&email=...&picture=...
 *
 * This component:
 *   1. Reads those params from the URL.
 *   2. Stores them in localStorage (same keys as the normal login flow).
 *   3. Calls onAuthenticated() so App.jsx updates its auth state.
 *   4. Navigates to /catalog.
 *
 * If params are missing (e.g. someone navigates here manually) it redirects to /auth.
 */
export default function AuthCallback({ onAuthenticated }) {
  const [params] = useSearchParams()
  const navigate  = useNavigate()

  useEffect(() => {
    const token    = params.get('token')
    const username = params.get('username')
    const userId   = params.get('userId')
    const email    = params.get('email') || ''
    const picture  = params.get('picture') || ''

    if (!token || !username || !userId) {
      // No valid params — bounce back to login
      navigate('/auth', { replace: true })
      return
    }

    onAuthenticated({ token, username, userId: Number(userId), email, picture })
    // onAuthenticated calls navigate('/catalog') itself, but we navigate anyway
    // as a safety net in case it doesn't.
    navigate('/catalog', { replace: true })
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="min-h-screen flex items-center justify-center">
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="flex flex-col items-center gap-4 text-center px-6"
      >
        <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary to-primarylight flex items-center justify-center shadow-glow">
          <span className="font-display font-bold text-white text-xl">O</span>
        </div>
        <Loader2 size={24} className="text-primary animate-spin" />
        <p className="text-textdim text-sm font-mono">Completing sign-in…</p>
      </motion.div>
    </div>
  )
}
