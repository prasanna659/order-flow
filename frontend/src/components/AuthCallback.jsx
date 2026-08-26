import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Loader2 } from 'lucide-react'

export default function AuthCallback({ onAuthenticated }) {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()

  useEffect(() => {
    const token = searchParams.get('token')
    const username = searchParams.get('username')
    const userId = searchParams.get('userId')
    const email = searchParams.get('email') || ''
    const picture = searchParams.get('picture') || ''

    // Check whether the backend sent the required parameters
    if (!token || !username || !userId) {
      console.error('OAuth callback: Missing required parameters', {
        token: !!token,
        username,
        userId,
      })

      navigate('/auth', { replace: true })
      return
    }

    // Convert userId from URL string to number
    const numericUserId = Number(userId)

    if (Number.isNaN(numericUserId)) {
      console.error('OAuth callback: Invalid userId:', userId)

      navigate('/auth', { replace: true })
      return
    }

    // Send authentication data to App.jsx
    // App.jsx stores it in localStorage and updates auth state.
    onAuthenticated({
      token,
      username,
      userId: numericUserId,
      email,
      picture,
    })

    // App.jsx's handleAuthenticated already navigates to /catalog.
    // This is intentionally not needed here.
  }, [searchParams, navigate, onAuthenticated])

  return (
    <div className="min-h-screen flex items-center justify-center">
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="flex flex-col items-center gap-4 text-center px-6"
      >
        {/* Logo */}
        <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary to-primarylight flex items-center justify-center shadow-glow">
          <span className="font-display font-bold text-white text-xl">
            O
          </span>
        </div>

        {/* Loading spinner */}
        <Loader2
          size={24}
          className="text-primary animate-spin"
        />

        {/* Message */}
        <p className="text-textdim text-sm font-mono">
          Completing sign-in…
        </p>
      </motion.div>
    </div>
  )
}