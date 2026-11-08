import Card from './Card.jsx'

/**
 * ボードの1列。見出し「やること（2）」と、その列のカードを並べる。
 * カードが0枚なら「カードはありません」と表示する。
 */
function Column({ status, name, tasks, onOpen }) {
  return (
    <section className={`column column-${status}`}>
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
