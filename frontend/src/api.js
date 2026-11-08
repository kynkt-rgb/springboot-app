/**
 * バックエンド（/api/tasks）を呼ぶ関数をまとめたファイル。
 * 画面の部品（App・TaskForm・TaskDetail）は fetch を直接書かず、ここの関数を呼ぶ。
 *
 * どの関数も、うまくいかなかったときは Error を投げる。
 *   err.message … 画面に出す理由（サーバーが理由を返していればそれ）
 *   err.status  … サーバーが返した番号（404 など）。サーバーに届かなかったときは無い
 */

/** fetch して、失敗なら Error を投げ、成功なら返ってきた JSON（中身がなければ null）を返す */
async function request(url, options = {}) {
  const res = await fetch(url, options)
  if (!res.ok) {
    // サーバーが理由（message）を返していれば、それを出す
    const body = await res.json().catch(() => null)
    const error = new Error(body?.message || `サーバーがエラーを返しました（${res.status}）`)
    error.status = res.status
    throw error
  }
  // 204 No Content（削除の成功）は中身がない
  if (res.status === 204) return null
  return res.json()
}

/** JSON を送るときの共通の形 */
function jsonOptions(method, data) {
  return {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  }
}

/** 全部のタスク（並び順の小さい順） */
export function fetchTasks() {
  return request('/api/tasks')
}

/** タスクを 1件登録する。登録したタスクを返す */
export function createTask(title) {
  return request('/api/tasks', jsonOptions('POST', { title }))
}

/** タスクの一部を変える（送った項目だけ変わる）。変えたあとのタスクを返す */
export function updateTask(id, changes) {
  return request(`/api/tasks/${id}`, jsonOptions('PATCH', changes))
}

/**
 * タスクを削除する。
 * 404（もう無い）なら、だれかが先に消している。消えているのは同じなので成功あつかいにする
 */
export async function deleteTask(id) {
  try {
    await request(`/api/tasks/${id}`, { method: 'DELETE' })
  } catch (err) {
    if (err.status !== 404) throw err
  }
}

/** 1つの列を優先度の高い順に並べなおす。並べなおしたその列のタスクを返す */
export function sortTasksByPriority(status) {
  return request('/api/tasks/sort-by-priority', jsonOptions('POST', { status }))
}
