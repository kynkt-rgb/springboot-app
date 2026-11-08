import { useState } from 'react'
import Card from './Card.jsx'

/**
 * ボードの1列。見出し「やること（2）」と、その列のカードを並べる。
 * カードが0枚なら「カードはありません」と表示する。
 *
 * 列ぜんたいが、カードの「落とし先」になる。
 * カードをこの列の上に持ってくると色が変わり、落とすと onMove(カードの番号, この列の status) を呼ぶ。
 */
function Column({ status, name, tasks, onOpen, onMove }) {
  // カードがこの列の上に来ているあいだは true（色を変える）
  const [isOver, setIsOver] = useState(false)

  /** カードが列の上を通っているあいだ、ずっと呼ばれる */
  function handleDragOver(e) {
    // これを書かないと、ブラウザは「ここには落とせない」と判断する
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
    setIsOver(true)
  }

  /** カードが列の外に出たとき */
  function handleDragLeave(e) {
    // 列の中のカードの上に移っただけのときも呼ばれるので、本当に列の外に出たときだけ戻す
    if (!e.currentTarget.contains(e.relatedTarget)) {
      setIsOver(false)
    }
  }

  /** カードを落としたとき */
  function handleDrop(e) {
    e.preventDefault()
    setIsOver(false)
    // ドラッグを始めたときに Card がしまっておいた「カードの番号」を取り出す
    const taskId = Number(e.dataTransfer.getData('text/plain'))
    if (taskId) {
      onMove(taskId, status)
    }
  }

  return (
    <section
      className={`column column-${status}${isOver ? ' drag-over' : ''}`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      <h2>
        {name}（{tasks.length}）
      </h2>
      {tasks.length === 0 ? (
        <p className="empty">カードはありません</p>
      ) : (
        <ul className="cards">
          {tasks.map((task) => (
            <Card key={task.id} task={task} onOpen={onOpen} />
          ))}
        </ul>
      )}
    </section>
  )
}

export default Column
