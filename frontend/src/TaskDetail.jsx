import { useEffect, useRef, useState } from 'react'
import { PRIORITIES } from './priorities.js'
import * as api from './api.js'

/** タイトル・説明の最大の文字数（サーバーと同じ） */
const TITLE_MAX = 50
const DESCRIPTION_MAX = 500

/** 日時（例: 2026-11-08T14:30:00+09:00）を「2026/11/08 14:30」の形にする */
function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('ja-JP', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** [...文字列] にすると、絵文字も1文字と数えられる */
function countChars(text) {
  return [...text].length
}

/**
 * タスクの詳細を表示・編集するモーダル（画面の上に重ねて出る窓）。
 * タイトル・説明・優先度・期限を変えて「保存」を押すと、
 * バックエンドの PATCH /api/tasks/{id} に送ってデータベースを更新する。
 * 保存できたら onSaved(変えたあとのタスク) を呼んで、ボードに知らせる。
 *
 * HTML の <dialog> を使う。showModal() で開くと、ブラウザが自動で
 *   ・後ろの画面を暗くして、さわれなくする
 *   ・Esc キーで閉じられるようにする
 *   ・Tab キーの移動をモーダルの中だけにする
 * をやってくれる。
 *
 * 開くたびに入力欄を作り直すため、App 側で key={task.id} をつけて使う。
 * 閉じたら onClose() を呼ぶ。
 */
function TaskDetail({ task, statusName, onSaved, onClose }) {
  const dialogRef = useRef(null)

  // 入力欄の中身。最初はいまのタスクの値を入れておく（空のものは '' にする）
  const [title, setTitle] = useState(task.title)
  const [description, setDescription] = useState(task.description ?? '')
  const [priority, setPriority] = useState(task.priority ?? 'medium')
  const [dueDate, setDueDate] = useState(task.dueDate ?? '')
  // 送っている最中は true（ボタンを押せなくする）
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  // 表示されたらモーダルを開く
  useEffect(() => {
    const dialog = dialogRef.current
    if (dialog && !dialog.open) {
      dialog.showModal()
    }
  }, [])

  /** モーダルを閉じる（閉じると <dialog> の onClose が呼ばれる） */
  function close() {
    dialogRef.current?.close()
  }

  /** モーダルの外側（暗い部分）をクリックしたら閉じる */
  function handleClick(e) {
    // 外側をクリックすると、クリックされたのは <dialog> そのものになる。送っている最中は閉じない
    if (e.target === dialogRef.current && !busy) {
      close()
    }
  }

  /** 送っている最中は Esc キーで閉じないようにする */
  function handleCancel(e) {
    if (busy) e.preventDefault()
  }

  async function handleSubmit(e) {
    // フォームの送信でページが読みこみ直されるのを止める
    e.preventDefault()
    if (busy) return

    const trimmedTitle = title.trim()
    const trimmedDescription = description.trim()
    if (trimmedTitle === '') {
      setError('タイトルを入力してください')
      return
    }
    if (countChars(trimmedTitle) > TITLE_MAX) {
      setError(`タイトルは${TITLE_MAX}文字以内にしてください`)
      return
    }
    if (countChars(trimmedDescription) > DESCRIPTION_MAX) {
      setError(`説明は${DESCRIPTION_MAX}文字以内にしてください`)
      return
    }

    setBusy(true)
    setError('')
    try {
      const saved = await api.updateTask(task.id, {
        title: trimmedTitle,
        // 空のときは null を送る（サーバーで「空にする」という意味になる）
        description: trimmedDescription === '' ? null : trimmedDescription,
        priority,
        dueDate: dueDate === '' ? null : dueDate,
      })
      onSaved(saved)
      setBusy(false)
      close()
    } catch (err) {
      // 失敗したときは入力したものを消さない（もう一度押せるように）
      setError(`保存できませんでした：${err.message}`)
      setBusy(false)
    }
  }

  return (
    <dialog
      ref={dialogRef}
      className="task-detail"
      aria-labelledby="task-detail-heading"
      // 「キャンセル」ボタン・Esc キー・外側のクリック・保存のあと、どれで閉じてもここが呼ばれる
      onClose={onClose}
      onCancel={handleCancel}
      onClick={handleClick}
    >
      <form className="task-detail-body" onSubmit={handleSubmit}>
        <h2 id="task-detail-heading">タスクの詳細</h2>

        <label className="field">
          <span>タイトル（必須・{TITLE_MAX}文字まで）</span>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            autoFocus
          />
        </label>

        <label className="field">
          <span>説明（{DESCRIPTION_MAX}文字まで）</span>
          <textarea
            rows={4}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </label>

        <div className="field-row">
          <label className="field">
            <span>優先度</span>
            <select value={priority} onChange={(e) => setPriority(e.target.value)}>
              {PRIORITIES.map((p) => (
                <option key={p.value} value={p.value}>
                  {p.name}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>期限</span>
            <input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
          </label>
        </div>

        {/* ここから下は表示だけ（変えられない） */}
        <dl>
          <dt>状態</dt>
          <dd>
            <span className={`status-badge status-${task.status}`}>{statusName}</span>
          </dd>
          <dt>作った日時</dt>
          <dd>{formatDateTime(task.createdAt)}</dd>
          <dt>最後に変えた日時</dt>
          <dd>{formatDateTime(task.updatedAt)}</dd>
        </dl>

        {error && (
          <p className="message error" role="alert">
            {error}
          </p>
        )}

        <div className="task-detail-buttons">
          <button type="button" className="secondary" onClick={close} disabled={busy}>
            キャンセル
          </button>
          <button type="submit" disabled={busy}>
            {busy ? '保存中…' : '保存'}
          </button>
        </div>
      </form>
    </dialog>
  )
}

export default TaskDetail
