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
 * カードをドラッグ&ドロップすると、ほかの列や同じ列の中の、落とした場所に移る。
 */
function App() {
  const [tasks, setTasks] = useState([])
  // 'loading'（読みこみ中） / 'error'（失敗） / 'ok'（表示できた）
  const [state, setState] = useState('loading')
  const [errorMessage, setErrorMessage] = useState('')
  // 詳細のモーダルで見ているタスク（見ていないときは null）
  const [selectedTask, setSelectedTask] = useState(null)
  // カードの移動・並べかえに失敗したときの理由（なければ ''）
  const [moveError, setMoveError] = useState('')
  // 「優先度順」で並べている最中の列の status（していないときは null）
  const [sortingStatus, setSortingStatus] = useState(null)

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
   * カードを動かす（ドラッグ&ドロップで落としたときに呼ばれる）。
   * dropIndex は、落とした列の「上から何番目のすきまか」（0 なら一番上）。
   * 待たせないように先に画面を変えてから、PATCH /api/tasks/{id} で status と position を送る。
   * 保存に失敗したら、元の並びに戻して理由を出す。
   */
  async function handleMove(taskId, status, dropIndex) {
    const original = tasks.find((t) => t.id === taskId)
    if (!original) return

    // 落とした列のカードを、並び順で取り出す（動かすカード自身は抜く）
    const column = tasks
      .filter((t) => t.status === status)
      .sort((a, b) => a.position - b.position)
    const oldIndex = column.findIndex((t) => t.id === taskId)
    // 同じ列で、自分より下のすきまに落としたときは、自分が抜けた分だけ1つずれる
    const newIndex = oldIndex !== -1 && dropIndex > oldIndex ? dropIndex - 1 : dropIndex
    // 同じ列の同じ場所に落としたときは何もしない
    if (oldIndex === newIndex) return

    // 先に画面だけ変える：落とした場所に入れて、その列の並び順を 1, 2, 3… と振りなおす
    // （サーバーも同じやり方で振りなおす）
    const reordered = column.filter((t) => t.id !== taskId)
    reordered.splice(newIndex, 0, { ...original, status })
    const newPositions = new Map(reordered.map((t, i) => [t.id, i + 1]))
    const before = tasks
    setTasks((prev) =>
      prev.map((t) =>
        newPositions.has(t.id)
          ? { ...t, status: t.id === taskId ? status : t.status, position: newPositions.get(t.id) }
          : t,
      ),
    )
    setMoveError('')

    try {
      const res = await fetch(`/api/tasks/${taskId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        // position は 1 から数える（一番上が 1）
        body: JSON.stringify({ status, position: newIndex + 1 }),
      })
      if (!res.ok) {
        // サーバーが理由（message）を返していれば、それを出す
        const body = await res.json().catch(() => null)
        throw new Error(body?.message || `サーバーがエラーを返しました（${res.status}）`)
      }
      // サーバーが決めた本当の値（position・最後に変えた日時など）で入れかえる
      replaceTask(await res.json())
    } catch (err) {
      // 動かす前の並びに戻す
      setTasks(before)
      setMoveError(`「${original.title}」を移動できませんでした：${err.message}`)
    }
  }

  /**
   * 1つの列を、優先度の高い順（高 → 中 → 低）に並べなおす（「優先度順」ボタンで呼ばれる）。
   * POST /api/tasks/sort-by-priority に送り、サーバーが並べなおして保存した結果で画面を変える。
   */
  async function handleSortByPriority(status) {
    if (sortingStatus) return
    setSortingStatus(status)
    setMoveError('')
    try {
      const res = await fetch('/api/tasks/sort-by-priority', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status }),
      })
      if (!res.ok) {
        // サーバーが理由（message）を返していれば、それを出す
        const body = await res.json().catch(() => null)
        throw new Error(body?.message || `サーバーがエラーを返しました（${res.status}）`)
      }
      // 返ってきたのは、並べなおしたその列のタスク。新しい並び順（position）で入れかえる
      const sorted = new Map((await res.json()).map((t) => [t.id, t]))
      setTasks((prev) => prev.map((t) => sorted.get(t.id) ?? t))
    } catch (err) {
      setMoveError(`優先度順に並べられませんでした：${err.message}`)
    } finally {
      setSortingStatus(null)
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
              sorting={sortingStatus === col.status}
              onSortByPriority={handleSortByPriority}
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
