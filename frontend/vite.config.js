import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // Cererile catre /api sunt trimise mai departe la backend-ul Spring (fara probleme de CORS)
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
