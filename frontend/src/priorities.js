/** 優先度の選びかた（サーバーと同じ値）。上から順に、モーダルの選択肢に並ぶ */
export const PRIORITIES = [
  { value: 'high', name: '高' },
  { value: 'medium', name: '中' },
  { value: 'low', name: '低' },
]

/** 'high' → '高' のように、画面に出す名前に変える */
export function priorityName(value) {
  return PRIORITIES.find((p) => p.value === value)?.name ?? '中'
}
