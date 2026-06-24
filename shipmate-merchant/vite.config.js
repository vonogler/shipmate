import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig({
    base: process.env.VITE_BASE_PATH ?? '/',
    plugins: [react()],
    build: {
        chunkSizeWarningLimit: 1200,
        rollupOptions: {
            output: {
                manualChunks(id) {
                    if (id.includes('node_modules/react') || id.includes('node_modules/react-dom')) {
                        return 'react';
                    }
                    if (id.includes('node_modules/antd') || id.includes('node_modules/@ant-design')) {
                        return 'antd';
                    }
                    if (id.includes('node_modules')) {
                        return 'vendor';
                    }
                    return undefined;
                }
            }
        }
    },
    server: {
        host: '0.0.0.0',
        port: 3001
    }
});
