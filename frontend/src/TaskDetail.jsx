import { useEffect, useRef } from 'react'

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

/**
 * タスクの詳細を表示するモーダル（画面の上に重ねて出る窓）。表示だけで、編集はできない。
 *
 * HTML の <dialog> を使う。showModal() で開くと、ブラウザが自動で
 *   ・後ろの画面を暗くして、さわれなくする
 *   ・Esc キーで閉じられるようにする
 *   ・Tab キーの移動をモーダルの中だけにする
 * をやってくれる。
 *
 * task が null のときは何も表示しない。閉じたら onClose() を呼ぶ。
 */
function TaskDetail({ task, statusName, onClose }) {
  const dialogRef = useRef(null)

  // task が渡されたらモーダルを開く
  useEffect(() => {
    const dialog = dialogRef.current
    if (task && dialog && !dialog.open) {
      dialog.showModal()
    }
  }, [task])

  if (!task) return null

  /** モーダルの外側（暗い部分）をクリックしたら閉じる */
  function handleClick(e) {
    // 外側をクリックすると、クリックされたのは <dialog> そのものになる
    if (e.target === dialogRef.current) {
      dialogRef.current.close()
    }
  }

  return (
    <dialog
      ref={dialogRef}
      className="task-detail"
      aria-labelledby="task-detail-title"
      // 「閉じる」ボタン・Esc キー・外側のクリック、どれで閉じてもここが呼ばれる
      onClose={onClose}
      onClick={handleClick}
    >
      <div className="task-detail-body">
        <h2 id="task-detail-title">{task.title}</h2>

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

        {/* method="dialog" のフォームのボタンを押すと、<dialog> が閉じる */}
        <form method="dialog">
          <button type="submit" autoFocus>
            閉じる
          </button>
        </form>
      </div>
    </dialog>
  )
}

export default TaskDetail
