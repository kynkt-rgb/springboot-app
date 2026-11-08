/**
 * カード1枚。タイトルを表示する。
 * クリック（または Tab で選んで Enter）すると onOpen(task) を呼び、詳細のモーダルを開く。
 * （移動・削除のボタンは、API ができてから足す）
 */
function Card({ task, onOpen }) {
  return (
    <li className="card">
      {/* ボタンにしておくと、マウスがなくてもキーボードで開ける */}
      <button type="button" className="card-button" onClick={() => onOpen(task)}>
        {task.title}
      </button>
    </li>
  )
}

export default Card
