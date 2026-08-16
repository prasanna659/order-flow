/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#0A0D14",
        surface: "#12151F",
        surface2: "#1A1E2C",
        border: "#262B3B",
        textdim: "#8E95AB",
        primary: "#6D5CF5",
        primarylight: "#8B7CFF",
        success: "#2FD98A",
        danger: "#FB5570",
      },
      boxShadow: {
        glow: "0 0 0 1px rgba(109,92,245,0.25), 0 8px 24px -8px rgba(109,92,245,0.45)",
        card: "0 1px 2px rgba(0,0,0,0.4), 0 8px 24px -12px rgba(0,0,0,0.5)",
      },
      fontFamily: {
        display: ["'Space Grotesk'", "sans-serif"],
        body: ["'Inter'", "sans-serif"],
        mono: ["'JetBrains Mono'", "monospace"],
      },
    },
  },
  plugins: [],
}
