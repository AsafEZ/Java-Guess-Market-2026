import { defineConfig } from 'vite';

export default defineConfig({
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
    proxy: {
      '/Server/api': {
        target: process.env.GUESS_MARKET_SERVER ?? 'http://127.0.0.1:8080',
        changeOrigin: false,
      },
    },
  },
});
