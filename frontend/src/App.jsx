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
 * カードをほかの列にドラッグ&ドロップすると、その列の一番下に移る。
 */
function App() {
  const [tasks, setTasks] = useState([])
  // 'loading'（読みこみ中） / 'error'（失敗） / 'ok'（表示できた）
  const [state, setState] = useState('loading')
  const [errorMessage, setErrorMessage] = useState('')
  // 詳細のモーダルで見ているタスク（見ていないときは null）
  const [selectedTask, setSelectedTask] = useState(null)
  // カードの移動に失敗したときの理由（なければ ''）
  const [moveError, setMoveError] = useState('')

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

  /** モーダルで保存できたタスクを、一覧の同じ番号のものと入れかえる（読みこみ直さなくても画面が変わる） */
  function handleSaved(saved) {
    replaceTask(saved)
  }

  /**
   * カードを別の列に移す（ドラッグ&ドロップで落としたときに呼ばれる）。
   * 待たせないように先に画面を変えてから、PATCH /api/tasks/{id} で status を送る。
   * 保存に失敗したら、カードを元の列に戻して理由を出す。
   */
  async function handleMove(taskId, status) {
    const original = tasks.find((t) => t.id === taskId)
    // 同じ列に落としたときは何もしない
    if (!original || original.status === status) return

    // 先に画面だけ変える：落とした列の一番下（いちばん大きい position + 1）に入れる
    const lastPosition = Math.max(
      0,
      ...tasks.filter((t) => t.status === status).map((t) => t.position),
    )
    replaceTask({ ...original, status, position: lastPosition + 1 })
    setMoveError('')

    try {
      const res = await fetch(`/api/tasks/${taskId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status }),
      })
      if (!res.ok) {
        // サーバーが理由（message）を返していれば、それを出す
        const body = await res.json().catch(() => null)
        throw new Error(body?.message || `サーバーがエラーを返しました（${res.status}）`)
      }
      // サーバーが決めた本当の値（position・最後に変えた日時など）で入れかえる
      replaceTask(await res.json())
    } catch (err) {
      replaceTask(original)
      setMoveError(`「${original.title}」を移動できませんでした：${err.message}`)
    }
  }

  /** 一覧の中の、同じ番号のタスクを入れかえる */
  function replaceTask(task) {
    setTasks((prev) => prev.map((t) => (t.id === task.id ? task : t)))
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

      {moveError && (
        <p className="message error" role="alert">
          {moveError}
        </p>
      )}

      {state === 'ok' && (
        <div className="board">
          {COLUMNS.map((col) => (
            <Column
              key={col.status}
              status={col.status}
              name={col.name}
              // 並び順（position）の小さい順に並べる（移動したカードが一番下に来る）
              tasks={tasks
                .filter((t) => t.status === col.status)
                .sort((a, b) => a.position - b.position)}
              onOpen={setSelectedTask}
              onMove={handleMove}
            />
          ))}
        </div>
      )}

      {selectedTask && (
        <TaskDetail
          // key が変わると入力欄が作り直されるので、別のカードを開いたときに前の入力が残らない
          key={selectedTask.id}
          task={selectedTask}
          statusName={COLUMNS.find((c) => c.status === selectedTask.status)?.name}
          onSaved={handleSaved}
          onClose={() => setSelectedTask(null)}
        />
      )}
    </main>
  )
}

export default App
