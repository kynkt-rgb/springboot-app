import { useEffect, useRef } from 'react'

/**
 * 「本当にやりますか？」を聞く確認の窓（例：削除の前）。
 *
 * ブラウザに最初からある window.confirm() は、VS Code の中のブラウザなど一部の場所では
 * 窓が出ないまま「OK」あつかいになってしまう。なので、詳細モーダルと同じ <dialog> で自分で作る。
 * showModal() で開くと、後ろの画面は暗くなってさわれず、Esc キーで閉じられる。
 *
 *   message      … 窓に出す文（\n で改行できる）
 *   confirmLabel … 「OK」のかわりに出すボタンの文字（例：「削除する」）
 *   onConfirm    … 「削除する」などを押したときに呼ぶ
 *   onCancel     … 「キャンセル」・Esc キー・外側のクリックで閉じたときに呼ぶ
 */
function ConfirmDialog({ title, message, confirmLabel, onConfirm, onCancel }) {
  const dialogRef = useRef(null)
  // どのボタンで閉じたかを覚えておく（閉じたあとの onClose で使う）
  const confirmedRef = useRef(false)

  // 表示されたら窓を開く
  useEffect(() => {
    const dialog = dialogRef.current
    if (dialog && !dialog.open) {
      dialog.showModal()
    }
  }, [])

  /** 窓が閉じたとき（どの閉じ方でも呼ばれる） */
  function handleClose() {
    if (confirmedRef.current) {
      onConfirm()
    } else {
      onCancel()
    }
  }

  function handleConfirm() {
    confirmedRef.current = true
    dialogRef.current.close()
  }

  /** 外側（暗い部分）をクリックしたら、キャンセルとして閉じる */
  function handleClick(e) {
    if (e.target === dialogRef.current) {
      dialogRef.current.close()
    }
  }

  return (
    <dialog
      ref={dialogRef}
      className="task-detail confirm-dialog"
      aria-labelledby="confirm-dialog-title"
      aria-describedby="confirm-dialog-message"
      onClose={handleClose}
      onClick={handleClick}
    >
      <div className="task-detail-body">
        <h2 id="confirm-dialog-title">{title}</h2>
        <p id="confirm-dialog-message" className="confirm-message">
          {message}
        </p>
        <div className="task-detail-buttons">
          {/* まちがえて消さないように、最初は「キャンセル」を選んだ状態にしておく */}
          <button
            type="button"
            className="secondary"
            onClick={() => dialogRef.current.close()}
            autoFocus
          >
            キャンセル
          </button>
          <button type="button" className="danger" onClick={handleConfirm}>
            {confirmLabel}
          </button>
        </div>
      </div>
    </dialog>
  )
}

export default ConfirmDialog
