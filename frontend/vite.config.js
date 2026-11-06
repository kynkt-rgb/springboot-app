import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // 画面は必ず 5173 番で動かす
    port: 5173,
    // 5173 番がふさがっていたら、別の番号に逃げずにエラーで止まる
    // （別の番号で動くと、取り次ぎや確認の手順が合わなくなるため）
    strictPort: true,
    // /api で始まる呼び出しは、バックエンド（8080 番）に取り次ぐ
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
