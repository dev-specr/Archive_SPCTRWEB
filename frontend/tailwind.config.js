/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: '#ef4444', // red-500
          dark: '#dc2626',     // red-600
        }
      }
    },
  },
  plugins: [],
}
