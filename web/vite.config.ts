import react from '@vitejs/plugin-react';
import {defineConfig} from 'vite';

// https://vite.dev/config/
export default defineConfig({
    server: {
        proxy: {
            '/login': 'http://localhost:3000',
            '/logout': 'http://localhost:3000',
            '/user': 'http://localhost:3000',
            '/visitor': 'http://localhost:3000',
            '/parking': 'http://localhost:3000',
        },
    },
    plugins: [react()],
});
