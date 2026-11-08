import { useEffect, useState } from 'react'
import Column from './Column.jsx'
import TaskForm from './TaskForm.jsx'
import TaskDetail from './TaskDetail.jsx'
import ConfirmDialog from './ConfirmDialog.jsx'
import * as api from './api.js'
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
 * カードの「×」を押すと、確認してからタスクを削除する。
 */
function App() {
  const [tasks, setTasks] = useState([])
  // 'loading'（読みこみ中） / 'error'（失敗） / 'ok'（表示できた）
  const [state, setState] = useState('loading')
  const [errorMessage, setErrorMessage] = useState('')
  // 詳細のモーダルで見ているタスク（見ていないときは null）
  const [selectedTask, setSelectedTask] = useState(null)
  // カードの移動・並べかえ・削除に失敗したときの理由（なければ ''）
  const [moveError, setMoveError] = useState('')
  // 削除してよいか確認している最中のタスク（していないときは null）
  const [deleteTarget, setDeleteTarget] = useState(null)
  // 「優先度順」で並べている最中の列の status（していないときは null）
  const [sortingStatus, setSortingStatus] = useState(null)

  useEffect(() => {
    api
      .fetchTasks()
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
      // position は 1 から数える（一番上が 1）
      const saved = await api.updateTask(taskId, { status, position: newIndex + 1 })
      // サーバーが決めた本当の値（position・最後に変えた日時など）で入れかえる
      replaceTask(saved)
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
      // 返ってきたのは、並べなおしたその列のタスク。新しい並び順（position）で入れかえる
      const column = await api.sortTasksByPriority(status)
      const sorted = new Map(column.map((t) => [t.id, t]))
      setTasks((prev) => prev.map((t) => sorted.get(t.id) ?? t))
    } catch (err) {
      setMoveError(`優先度順に並べられませんでした：${err.message}`)
    } finally {
      setSortingStatus(null)
    }
  }

  /**
   * カードの「×」で呼ばれる。すぐには消さず、確認の窓（ConfirmDialog）を出す。
   * 窓で「削除する」を押したときだけ、deleteTask で本当に消す。
   */
  function handleDelete(task) {
    setDeleteTarget(task)
  }

  /**
   * タスクを削除する。DELETE /api/tasks/{id} を送る。
   * データベースから完全に消える（元には戻せない）ので、消せたことを確かめてから画面からも消す。
   */
  async function deleteTask(task) {
    setMoveError('')
    try {
      // 404（もう無い）のときも、api.deleteTask は成功あつかいにする（画面から消すだけでよい）
      await api.deleteTask(task.id)
      // 一覧から抜く。読みこみ直さなくても、すぐにカードが消える
      setTasks((prev) => prev.filter((t) => t.id !== task.id))
    } catch (err) {
      setMoveError(`「${task.title}」を削除できませんでした：${err.message}`)
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
              onDelete={handleDelete}
              sorting={sortingStatus === col.status}
              onSortByPriority={handleSortByPriority}
            />
          ))}
        </div>
      )}

      {deleteTarget && (
        <ConfirmDialog
          title="タスクの削除"
          message={`「${deleteTarget.title}」を削除しますか？\n削除すると元に戻せません。`}
          confirmLabel="削除する"
          onConfirm={() => {
            setDeleteTarget(null)
            deleteTask(deleteTarget)
          }}
          onCancel={() => setDeleteTarget(null)}
        />
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
