import { useCallback, useMemo, useRef, useState } from "react"

import type { PageDraft } from "@/components/dw/meta"

/**
 * 页面配置的撤销栈。
 *
 * ## 为什么配置类工具必须有它
 *
 * 页面配置是「手工调出来的东西」，不是表单提交：一次「按字段生成」会覆盖整列，
 * 一次误点垃圾桶会删掉一列，一次拖动会打乱顺序——这些都没有二次确认，因为
 * 每个都确认会让正常操作变得很吵。没有撤销，代价就是用户只能手工重建。
 *
 * ## 为什么不用「每次改动都入栈」
 *
 * 在列名输入框里打 6 个字会入栈 6 次，撤销 6 次才退一个字——那样的撤销等于没有。
 * 所以规则是：**连续打字合并成一条，结构性改动作立刻另起一条**。
 *
 * 判据放在 {@link isStructuralChange} 里，只比长度和顺序，不做深比较：
 * 深比较要遍历整份配置，而这层存在的意义正是「别在每次击键上做全量工作」。
 *
 * ## 版本号也顺手解决了 dirty
 *
 * 之前 `dirty` 是 `JSON.stringify(draft) !== baseline`，每个击键序列化一整份配置。
 * 现在每一格历史自带版本号：起点是 0，新提交取一个只增不减的号，合并的击键沿用原号。
 * `rev !== 0` 就是「改过且没撤回」，而且撤销回到起点时它天然变回 false——
 * 比字符串比对便宜，也不会像「栈深」那样被合并与截断带偏。
 */

/** 撤销栈里的一格：快照 + 它自己的版本号。 */
interface DraftEntry {
  draft: PageDraft
  rev: number
}

interface HistoryState {
  past: DraftEntry[]
  entry: DraftEntry
  future: DraftEntry[]
  /** 下一个新版本的编号。只增不减，撤销不会把号还回去 */
  next: number
}

/** 撤销栈上限。配置页不会有人连撤 50 步，但也不必无限存快照。 */
const LIMIT = 50

/** 连续修改的合并窗口：这个间隔内的非结构性改动并进同一条历史 */
const COALESCE_MS = 600

export function useDraftHistory(initial: PageDraft) {
  const [state, setState] = useState<HistoryState>({
    past: [],
    entry: { draft: initial, rev: 0 },
    future: [],
    next: 1,
  })
  const lastAt = useRef(0)

  const setDraft = useCallback((next: PageDraft | ((current: PageDraft) => PageDraft)) => {
    // now 在 updater 外面取：updater 保持纯函数，StrictMode 下重复调用也不会得到不同结果
    const now = Date.now()
    setState((history) => {
      const value = typeof next === "function" ? next(history.entry.draft) : next
      if (value === history.entry.draft) return history
      // 撤销过之后又改动：redo 栈作废，并且必须另起一条（否则「撤了再改」会被合并进旧条目）
      const merge =
        history.future.length === 0 &&
        !isStructuralChange(history.entry.draft, value) &&
        now - lastAt.current < COALESCE_MS
      lastAt.current = now
      if (merge) {
        // 合并时版本号**不动**：这一串击键是同一个改动，撤销要一次退干净
        return { ...history, entry: { draft: value, rev: history.entry.rev } }
      }
      return {
        past: [...history.past, history.entry].slice(-LIMIT),
        entry: { draft: value, rev: history.next },
        future: [],
        next: history.next + 1,
      }
    })
  }, [])

  const undo = useCallback(() => {
    lastAt.current = 0
    setState((history) => {
      if (history.past.length === 0) return history
      return {
        past: history.past.slice(0, -1),
        entry: history.past[history.past.length - 1],
        future: [history.entry, ...history.future].slice(0, LIMIT),
        next: history.next,
      }
    })
  }, [])

  const redo = useCallback(() => {
    lastAt.current = 0
    setState((history) => {
      if (history.future.length === 0) return history
      return {
        past: [...history.past, history.entry].slice(-LIMIT),
        entry: history.future[0],
        future: history.future.slice(1),
        next: history.next,
      }
    })
  }, [])

  return useMemo(
    () => ({
      draft: state.entry.draft,
      setDraft,
      undo,
      redo,
      canUndo: state.past.length > 0,
      canRedo: state.future.length > 0,
      /** 改过且没撤回。撤销回到起点会变回 false，不需要额外保存 baseline 快照。 */
      dirty: state.entry.rev !== 0,
    }),
    [state, setDraft, undo, redo],
  )
}

/**
 * 这次改动是不是「结构性的」——是就另起一条历史，撤销时才会一步退掉整件事。
 *
 * 只看长度与顺序，不看内容：内容变化就是打字，该被合并。
 */
export function isStructuralChange(prev: PageDraft, next: PageDraft): boolean {
  if (prev.datasourceKey !== next.datasourceKey) return true
  if (prev.pageKey !== next.pageKey) return true
  const { list: a, form: fa } = prev
  const { list: b, form: fb } = next
  return (
    a.columns.length !== b.columns.length ||
    a.filters.length !== b.filters.length ||
    a.actions.length !== b.actions.length ||
    (a.rowActions?.length ?? 0) !== (b.rowActions?.length ?? 0) ||
    Object.keys(a.fieldMaps ?? {}).length !== Object.keys(b.fieldMaps ?? {}).length ||
    fa.fields.length !== fb.fields.length ||
    !sameOrder(a.columns, b.columns) ||
    !sameOrder(a.filters, b.filters) ||
    !sameOrder(fa.fields, fb.fields)
  )
}

/** 顺序变了 = 拖动或上下移。改 key 也算：那是一次离散的选择，不该并进上一次打字。 */
function sameOrder(a: Array<{ key: string }>, b: Array<{ key: string }>): boolean {
  if (a.length !== b.length) return false
  for (let i = 0; i < a.length; i += 1) {
    if (a[i].key !== b[i].key) return false
  }
  return true
}
