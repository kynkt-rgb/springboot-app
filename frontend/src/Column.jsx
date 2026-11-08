import { useState } from 'react'
import Card from './Card.jsx'

/**
 * ボードの1列。見出し「やること（2）」と、その列のカードを並べる。
 * カードが0枚なら「カードはありません」と表示する。
 *
 * 列ぜんたいが、カードの「落とし先」になる。
 *   ・カードの上半分に持ってくる → そのカードの上に入る
 *   ・カードの下半分に持ってくる → そのカードの下に入る
 *   ・カードのないところに持ってくる → 列の一番下に入る
 * どこに入るかは青い線で見せる。落とすと onMove(カードの番号, この列の status, 入れる場所) を呼ぶ。
 * 入れる場所は「上から何番目のすきまか」（0 なら一番上、カードの数と同じなら一番下）。
 *
 * 見出しのとなりの「優先度順」ボタンを押すと onSortByPriority(この列の status) を呼ぶ。
 * sorting が true のあいだ（保存している最中）は、ボタンを押せなくする。
 */
function Column({ status, name, tasks, sorting, onOpen, onMove, onDelete, onSortByPriority }) {
  // カードがこの列の上に来ているあいだ、入れる場所を入れておく（来ていないときは null）
  const [dropIndex, setDropIndex] = useState(null)

  /** マウスの位置から、入れる場所（上から何番目のすきまか）を決める */
  function findDropIndex(e) {
    const cardElement = e.target.closest('.card')
    if (!cardElement || !e.currentTarget.contains(cardElement)) {
      // カードのないところ（見出しやすきま）なら一番下
      return tasks.length
    }
    const index = Number(cardElement.dataset.index)
    const rect = cardElement.getBoundingClientRect()
    // カードの上半分なら、そのカードの上。下半分なら下
    return e.clientY < rect.top + rect.height / 2 ? index : index + 1
  }

  /** カードが列の上を通っているあいだ、ずっと呼ばれる */
  function handleDragOver(e) {
    // これを書かないと、ブラウザは「ここには落とせない」と判断する
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
    setDropIndex(findDropIndex(e))
  }

  /** カードが列の外に出たとき */
  function handleDragLeave(e) {
    // 列の中のカードの上に移っただけのときも呼ばれるので、本当に列の外に出たときだけ戻す
    if (!e.currentTarget.contains(e.relatedTarget)) {
      setDropIndex(null)
    }
  }

  /** カードを落としたとき */
  function handleDrop(e) {
    e.preventDefault()
    const index = findDropIndex(e)
    setDropIndex(null)
    // ドラッグを始めたときに Card がしまっておいた「カードの番号」を取り出す
    const taskId = Number(e.dataTransfer.getData('text/plain'))
    if (taskId) {
      onMove(taskId, status, index)
    }
  }

  /** そのカードの上か下に青い線を出すか（出さないときは null） */
  function dropMarkOf(index) {
    if (dropIndex === index) return 'before'
    // 一番下に入れるときは、最後のカードの下に線を出す
    if (dropIndex === tasks.length && index === tasks.length - 1) return 'after'
    return null
  }

  return (
    <section
      className={`column column-${status}${dropIndex !== null ? ' drag-over' : ''}`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      <div className="column-header">
        <h2>
          {name}（{tasks.length}）
        </h2>
        {/* カードが2枚以上あるときだけ出す（1枚以下なら並べかえるものがない） */}
        {tasks.length >= 2 && (
          <button
            type="button"
            className="sort-button"
            onClick={() => onSortByPriority(status)}
            disabled={sorting}
            aria-label={`${name}の列を優先度の高い順に並べる`}
          >
            {sorting ? '並べています…' : '優先度順'}
          </button>
        )}
      </div>
      {tasks.length === 0 ? (
        <p className="empty">カードはありません</p>
      ) : (
        <ul className="cards">
          {tasks.map((task, index) => (
            <Card
              key={task.id}
              task={task}
              index={index}
              dropMark={dropMarkOf(index)}
              onOpen={onOpen}
              onDelete={onDelete}
            />
          ))}
        </ul>
      )}
    </section>
  )
}

export default Column
