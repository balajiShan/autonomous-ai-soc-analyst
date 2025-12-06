import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'dist', // Output to src/main/resources/static/dist
    emptyOutDir: true, // Clear the output directory before building
    rollupOptions: {
      input: resolve(__dirname, 'index.html'), // Use index.html as entry point
    },
  },
  base: '/', // Serve assets from root for Spring Boot
  server: {
    proxy: {
      '/api': 'http://localhost:3000', // Proxy API calls during development
    },
  },
});