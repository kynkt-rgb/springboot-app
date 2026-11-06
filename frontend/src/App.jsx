import { useEffect, useState } from 'react'
import Column from './Column.jsx'
import './App.css'

/** ボードの3つの列（左から順に表示する） */
const COLUMNS = [
  { status: 'todo', name: 'やること' },
  { status: 'doing', name: '進行中' },
  { status: 'done', name: '完了' },
]

/**
 * ボード画面。
 * 開いたときにバックエンドの GET /api/tasks を呼び、
 * 返ってきたタスクを status ごとに3列に分けて表示する。
 */
function App() {
  const [tasks, setTasks] = useState([])
  // 'loading'（読みこみ中） / 'error'（失敗） / 'ok'（表示できた）
  const [state, setState] = useState('loading')
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    fetch('/api/tasks')
      .then((res) => {
        if (!res.ok) {
          throw new Error(`サーバーがエラーを返しました（${res.status}）`)
        }
        return res.json()
      })
      .then((data) => {
        setTasks(data)
        setState('ok')
      })
      .catch((err) => {
        setErrorMessage(err.message)
        setState('error')
      })
  }, [])

  return (
    <main className="board-page">
      <h1>タスクボード</h1>

      {state === 'loading' && <p className="message">読みこみ中…</p>}
      {state === 'error' && (
        <p className="message error">
          カードを読みこめませんでした：{errorMessage}
        </p>
      )}

      {state === 'ok' && (
        <div className="board">
          {COLUMNS.map((col) => (
            <Column
              key={col.status}
              status={col.status}
              name={col.name}
              // API が並び順（position）で返すので、そのまま絞るだけでよい
              tasks={tasks.filter((t) => t.status === col.status)}
            />
          ))}
        </div>
      )}
    </main>
  )
}

export default App
