import { priorityName } from './priorities.js'

/** 期限（例: 2026-11-10）を「11/10」の形にする */
function formatDueDate(value) {
  const [, month, day] = value.split('-')
  return `${Number(month)}/${Number(day)}`
}

/**
 * カード1枚。タイトルと、優先度・期限を小さく表示する。
 * クリック（または Tab で選んで Enter）すると onOpen(task) を呼び、詳細のモーダルを開く。
 * （移動・削除のボタンは、API ができてから足す）
 */
function Card({ task, onOpen }) {
  const priority = task.priority ?? 'medium'
  return (
    <li className="card">
      {/* ボタンにしておくと、マウスがなくてもキーボードで開ける */}
      <button type="button" className="card-button" onClick={() => onOpen(task)}>
        <span className="card-title">{task.title}</span>
        <span className="card-meta">
          <span className={`priority priority-${priority}`}>優先度：{priorityName(priority)}</span>
          {task.dueDate && <span>期限：{formatDueDate(task.dueDate)}</span>}
        </span>
      </button>
    </li>
  )
}

export default Card
