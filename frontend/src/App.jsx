import { useEffect, useState } from 'react'
import Column from './Column.jsx'
import TaskForm from './TaskForm.jsx'
import TaskDetail from './TaskDetail.jsx'
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
 * 上のフォームでタスクを登録すると、「やること」列の一番下に増える。
 */
function App() {
  const [tasks, setTasks] = useState([])
  // 'loading'（読みこみ中） / 'error'（失敗） / 'ok'（表示できた）
  const [state, setState] = useState('loading')
  const [errorMessage, setErrorMessage] = useState('')
  // 詳細のモーダルで見ているタスク（見ていないときは null）
  const [selectedTask, setSelectedTask] = useState(null)

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

  /** フォームで登録できたタスクを、一覧の最後に足す（読みこみ直さなくても画面に出る） */
  function handleAdded(task) {
    setTasks((prev) => [...prev, task])
  }

  return (
    <main className="board-page">
      <h1>タスクボード</h1>

      {state === 'loading' && <p className="message">読みこみ中…</p>}
      {state === 'error' && (
        <p className="message error">
          カードを読みこめませんでした：{errorMessage}
        </p>
      )}

      {state === 'ok' && <TaskForm onAdded={handleAdded} />}

      {state === 'ok' && (
        <div className="board">
          {COLUMNS.map((col) => (
            <Column
              key={col.status}
              status={col.status}
              name={col.name}
              // API が並び順（position）で返すので、そのまま絞るだけでよい
              tasks={tasks.filter((t) => t.status === col.status)}
              onOpen={setSelectedTask}
            />
          ))}
        </div>
      )}

      <TaskDetail
        task={selectedTask}
        statusName={COLUMNS.find((c) => c.status === selectedTask?.status)?.name}
        onClose={() => setSelectedTask(null)}
      />
    </main>
  )
}

export default App
