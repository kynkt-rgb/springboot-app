import { useRef, useState } from 'react'

/** タイトルの最大の文字数（サーバーと同じ） */
const TITLE_MAX = 50

/**
 * タスクを新しく登録するフォーム（入力欄と「追加」ボタン）。
 * 「追加」を押すか Enter で、バックエンドの POST /api/tasks に送る。
 * 登録できたら onAdded(登録したタスク) を呼んで、ボードに知らせる。
 */
function TaskForm({ onAdded }) {
  const [title, setTitle] = useState('')
  // 送っている最中は true（ボタンを押せなくする）
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const inputRef = useRef(null)

  async function handleSubmit(e) {
    // フォームの送信でページが読みこみ直されるのを止める
    e.preventDefault()
    if (busy) return

    // 前後の空白を取りのぞく
    const trimmed = title.trim()
    if (trimmed === '') {
      setError('タイトルを入力してください')
      return
    }
    // [...文字列] にすると、絵文字も1文字と数えられる
    if ([...trimmed].length > TITLE_MAX) {
      setError(`タイトルは${TITLE_MAX}文字以内にしてください`)
      return
    }

    setBusy(true)
    setError('')
    try {
      const res = await fetch('/api/tasks', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title: trimmed }),
      })
      if (!res.ok) {
        // サーバーが理由（message）を返していれば、それを出す
        const body = await res.json().catch(() => null)
        throw new Error(body?.message || `サーバーがエラーを返しました（${res.status}）`)
      }
      const task = await res.json()
      onAdded(task)
      // 成功したら入力欄を空にする
      setTitle('')
    } catch (err) {
      // 失敗したときは入力欄の文字を消さない（もう一度押せるように）
      setError(`保存できませんでした：${err.message}`)
    } finally {
      setBusy(false)
      // 続けて入力できるように、入力欄にカーソルを戻す
      inputRef.current?.focus()
    }
  }

  return (
    <form className="task-form" onSubmit={handleSubmit}>
      <div className="task-form-row">
        <input
          ref={inputRef}
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder={`タイトルを入力（${TITLE_MAX}文字まで）`}
          aria-label="タスクのタイトル"
          aria-invalid={error !== ''}
          aria-describedby="task-form-error"
        />
        <button type="submit" disabled={busy}>
          {busy ? '追加中…' : '追加'}
        </button>
      </div>
      {error && (
        <p id="task-form-error" className="message error" role="alert">
          {error}
        </p>
      )}
    </form>
  )
}

export default TaskForm
