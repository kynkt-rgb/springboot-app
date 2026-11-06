/**
 * カード1枚。今はタイトルだけを表示する。
 * （追加・移動・削除のボタンは、API ができてから足す）
 */
function Card({ task }) {
  return <li className="card">{task.title}</li>
}

export default Card
