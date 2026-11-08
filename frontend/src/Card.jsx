import { useState } from 'react'
import { priorityName } from './priorities.js'

/** 期限（例: 2026-11-10）を「11/10」の形にする */
function formatDueDate(value) {
  const [, month, day] = value.split('-')
  return `${Number(month)}/${Number(day)}`
}

/**
 * カード1枚。タイトルと、優先度・期限を小さく表示する。
 * クリック（または Tab で選んで Enter）すると onOpen(task) を呼び、詳細のモーダルを開く。
 * つかんで（ドラッグして）ほかの列や同じ列のほかの場所に落とすと、そこに移る。落とし先の処理は Column がやる。
 * index は列の中で上から何番目か（0から数える）。dropMark が 'before' / 'after' なら、上か下に青い線を出す。
 * 右上の「×」を押すと onDelete(task) を呼ぶ（確認ダイアログと削除は App がやる）。
 */
function Card({ task, index, dropMark, onOpen, onDelete }) {
  const priority = task.priority ?? 'medium'
  // つかんでいるあいだは true（カードを薄くする）
  const [dragging, setDragging] = useState(false)

  /** つかみ始めたとき：落とし先の列にわかるよう「カードの番号」をしまっておく */
  function handleDragStart(e) {
    e.dataTransfer.setData('text/plain', String(task.id))
    e.dataTransfer.effectAllowed = 'move'
    setDragging(true)
  }

  return (
    <li
      className={`card${dragging ? ' dragging' : ''}${dropMark ? ` drop-${dropMark}` : ''}`}
      // Column が「どのカードの上にいるか」を知るために使う
      data-index={index}
    >
      {/* ボタンにしておくと、マウスがなくてもキーボードで開ける。draggable でつかめるようになる */}
      <button
        type="button"
        className="card-button"
        draggable
        onDragStart={handleDragStart}
        onDragEnd={() => setDragging(false)}
        onClick={() => onOpen(task)}
      >
        <span className="card-title">{task.title}</span>
        <span className="card-meta">
          <span className={`priority priority-${priority}`}>優先度：{priorityName(priority)}</span>
          {task.dueDate && <span>期限：{formatDueDate(task.dueDate)}</span>}
        </span>
      </button>
      {/* ボタンの中にボタンは入れられないので、カードのボタンのとなりに置く */}
      <button
        type="button"
        className="delete-button"
        onClick={() => onDelete(task)}
        aria-label={`「${task.title}」を削除`}
        title="削除"
      >
        ×
      </button>
    </li>
  )
}

export default Card
