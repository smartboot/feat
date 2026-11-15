import { memo, useEffect, useMemo, useRef, useState } from "react"
import { MoreHorizontal } from "lucide-react"

import type { DatasourceOption, DwFormValue, DwRow } from "@/api/types"
import { DwFormFields, initialFormValue } from "@/components/dw/DwFormFields"
import { DwListView } from "@/components/dw/DwListView"
import { focusAnchor, type DwFocusTarget, type DwMarks } from "@/components/dw/link"
import { rowLabel, sampleRows, type PageDraft } from "@/components/dw/meta"
import { resolveFormConfig, resolveListConfig } from "@/components/dw/resolve"
import { Button } from "@/components/ui/button"

/**
 * 配置的实时预览。
 *
 * ## 它与线上是同一个组件
 *
 * 列表部分不再自己画表格，而是渲染 {@link DwListView}——和 {@link DwList} 用的**同一个**。
 * 数据换成示例行，所有动作回调要么不传（于是按钮自动禁用），要么接到「切预览态」。
 *
 * 之前这里是照抄的第二套渲染，已经和线上分叉了三处（列名兜底、空态按钮、筛选控件），
 * 而预览的全部价值就是「配完长这样」。分叉的预览比没有预览更坏：配置者会照着它做决定。
 *
 * ## 它同时是导航面
 *
 * 光「长这样」还不够：看得出问题之后，下一个动作是「回去改那一处」。所以预览与编辑区
 * 双向联动——左边悬停哪一条，这里对应位置亮；点表头 / 筛选，左边滚到那一条。
 * 联动靠在 `link` 里注入 focus / marks / 回调，展示层为此没有第二个分支。
 *
 * ## 哪些按钮可点
 *
 * 判据不是「会不会改数据」，而是「点了会不会让人以为数据变了」：
 * - 新建 / 行内编辑只切预览状态（弹窗、回填第几行），不写任何东西 → 可点；
 * - 删除与自定义按钮会弹真实结构的确认框，但确认键禁用并说明「预览不会真的执行」→ 可点；
 * - 刷新会换掉眼前这份示例数据，点完只剩困惑 → 不传 handler，保持禁用。
 *
 * 行操作菜单如果整块禁用，配置者就只能看到一列「⋯」看不出那里有什么——这正是
 * 配了 edit / delete 却验证不了的地方。所以菜单本身可开，只是走到最后一步停住。
 */
export type PreviewMode = "list" | "empty" | "create" | "edit" | "readonly"

export const PREVIEW_MODES: Array<{ value: PreviewMode; label: string; hint: string }> = [
  { value: "list", label: "列表", hint: "有数据时的样子（示例数据）" },
  { value: "empty", label: "空态", hint: "查询无结果的样子" },
  { value: "create", label: "新建", hint: "新建弹窗，字段默认值已填入" },
  { value: "edit", label: "编辑", hint: "编辑弹窗，字段取第一行的示例值" },
  { value: "readonly", label: "无写权限", hint: "只读账号看到的样子：写按钮全部不渲染" },
]

function DwPreviewImpl({
  draft,
  datasource,
  mode,
  focus = null,
  pinned = false,
  marks,
  onHover,
  onLocate,
  onOpenForm,
}: {
  draft: PageDraft
  datasource?: DatasourceOption
  mode: PreviewMode
  /** 当前联动焦点（悬停优先，其次是点选） */
  focus?: DwFocusTarget | null
  /**
   * 焦点来自「点选」而不是悬停。只有点选才自动滚动：悬停会频繁变化，
   * 跟着滚会让预览一直抖。
   */
  pinned?: boolean
  marks?: DwMarks
  onHover?: (target: DwFocusTarget | null) => void
  onLocate?: (target: DwFocusTarget) => void
  /** 点预览里的「新建」：切到新建态（由工作台决定要不要同时跳到表单分区） */
  onOpenForm?: (mode: "create" | "edit") => void
}) {
  const fields = datasource?.fields ?? []

  /**
   * 关键的一层：**预览必须先合并字段值域，再渲染**。
   *
   * 线上拿到的配置是后端 `applyFieldMaps` 合并过的副本；这里拿到的是编辑器里的原始
   * draft，列上的 `options` 还空着。少了这一步，配好值域的列在预览里仍显示原始编码，
   * 而保存后线上显示中文——预览就变成了一句谎话（见 `resolve.ts`）。
   *
   * `useMemo` 的依赖是 draft：它随每次编辑换新，但合并在没有任何值域时原样返回同一个
   * 引用，所以下面那些按引用比较的判断不会被白白打穿。
   */
  const { list, form } = useMemo(
    () => ({ list: resolveListConfig(draft.list), form: resolveFormConfig(draft.list, draft.form) }),
    [draft.list, draft.form],
  )
  const containerRef = useRef<HTMLDivElement>(null)

  /**
   * 编辑弹窗预览的是第几行示例数据。行内「编辑」点哪一行就是哪一行——
   * 不带上这一层，三行示例点哪行都弹同一份值，「按行回填」这件事在预览里就没发生过。
   */
  const [editIndex, setEditIndex] = useState(0)

  const anchor = focus ? focusAnchor(focus) : ""
  useEffect(() => {
    if (!pinned || !anchor) return
    const element = containerRef.current?.querySelector(`[data-preview-focus="${anchor}"]`)
    // jsdom 没有 scrollIntoView，测试环境里跳过（这条只是体验优化）
    if (element && typeof element.scrollIntoView === "function") {
      element.scrollIntoView({ block: "nearest", inline: "nearest" })
    }
  }, [pinned, anchor])

  if (mode === "create" || mode === "edit") {
    const editing = mode === "edit"
    const rows = sampleRows(fields, list.columns, 3)
    // 行内编辑点过第几行，这里就回填第几行；行数变少时退到最后一行的示例值。
    const index = Math.min(editIndex, Math.max(rows.length - 1, 0))
    const row = rows[index] ?? null
    const values: Record<string, DwFormValue> = {}
    for (const field of form.fields) {
      // 预览里的初值走和真实弹窗同一个函数：多选字段在预览里也要是多选的样子。
      values[field.key] = initialFormValue(field, editing ? (row as Record<string, unknown> | null) : null)
    }
    return (
      <div ref={containerRef} data-preview-mode={mode}>
        <div className="rounded-xl border bg-background p-4 shadow-sm">
          <div className="grid gap-1">
            <p className="text-base font-semibold">{editing ? form.editTitle || "编辑" : form.createTitle || "新建"}</p>
            <p className="text-xs text-muted-foreground">
              {(editing ? form.editDescription : form.createDescription) || "（未配置说明文案）"}
            </p>
            {editing && (
              <p className="text-[10px] text-muted-foreground/80">
                示例数据第 {index + 1} 行
                {row ? `（${rowLabel(row, list.columns, list.rowKey)}）` : ""}——行内「编辑」点哪行，
                这里回填哪一行。
              </p>
            )}
          </div>
          <div className="mt-4">
            <DwFormFields
              form={form}
              values={values}
              editing={editing}
              disabled
              // 预览不发请求：配了 optionsApi 的字段在这里只说明「选项来自哪」，
              // 不去真的取——否则「预览不依赖数据层」这条约束就名存实亡了。
              remote={false}
              link={{
                index: focus?.kind === "field" ? focus.index : null,
                marks: marks?.field,
                onSelect: onLocate ? (index) => onLocate({ kind: "field", index }) : undefined,
                onHover: onHover
                  ? (index) => onHover(index == null ? null : { kind: "field", index })
                  : undefined,
              }}
            />
          </div>
          <div className="mt-5 flex justify-end gap-2">
            <Button variant="outline" size="sm" disabled>
              取消
            </Button>
            <Button size="sm" disabled>
              {editing ? "保存修改" : form.createTitle || "新建"}
            </Button>
          </div>
        </div>
      </div>
    )
  }

  const canWrite = mode !== "readonly"
  // 空态就该是空的：留着 3 行示例数据还要在下面写「没有符合条件的数据」，是自相矛盾。
  const rows = mode === "empty" ? [] : sampleRows(fields, list.columns, 3)
  const total = mode === "empty" ? 0 : 24
  const showRowMenu =
    canWrite &&
    (list.actions.some((action) => action === "edit" || action === "delete") ||
      (list.rowActions ?? []).length > 0)

  return (
    <div ref={containerRef} data-preview-mode={mode}>
      <DwListView
        // 换预览态时重建：确认框是展示层的内部状态，跟着模式一起复位，
        // 否则会有一个删除确认框压在「编辑」弹窗上层，说不清在看哪一态。
        key={mode}
        list={list}
        title={draft.title || "未命名页面"}
        createLabel={form.createTitle}
        canWrite={canWrite}
        rows={rows}
        total={total}
        page={1}
        pageSize={list.pageSize}
        filterValues={{}}
        // 预览假设「筛选都填了」：这样空态的「清除筛选」和头部的「N 个条件」都会出现，
        // 配置者才能验证它们长什么样。
        activeFilterCount={list.filters.length}
        remoteOptions={false}
        onCreate={() => onOpenForm?.("create")}
        onEdit={(_, index) => {
          setEditIndex(index)
          onOpenForm?.("edit")
        }}
        confirmHint="预览：确认键不可用，不会真的删除。"
        actionDetail={(action, row) => (
          <div className="rounded-md border bg-muted/40 p-2 font-mono text-[10px] leading-relaxed">
            <p className="break-all">POST {action.api || "（未配置接口）"}</p>
            <p className="text-muted-foreground">{`{ "${list.rowKey}": ${String(row[list.rowKey])} }`}</p>
          </div>
        )}
        editHint={(rowIndex) => (
          <span className="ml-1 text-[10px] text-muted-foreground">回填第 {rowIndex + 1} 行</span>
        )}
        link={{ focus, marks, onHover, onSelect: onLocate }}
        emptyColumns="还没有配置列，这里只会显示空表头"
        footnote={
          showRowMenu ? (
            <>
              每行最右的 <MoreHorizontal className="inline size-3 align-text-bottom" /> 可点开：「编辑」切到编辑弹窗并
              回填该行，「删除」与自定义按钮弹确认框——确认键在预览里不可用，不会真的删掉数据或发出请求。
            </>
          ) : undefined
        }
        embedded
      />
    </div>
  )
}

/**
 * 预览整块做成 memo：编辑区的紧急渲染（每敲一个字）不该穿透到这里。
 *
 * 工作台把 `draft` 换成 `useDeferredValue` 之后，一次击键的紧急渲染拿到的是**同一个**
 * 延迟草稿，于是这里整块跳过；真正要重画时已经在低优先级渲染里了。悬停仍然会重画
 * （`focus` 变了），但那只是移动一个高亮，不是每个字符都来一次。
 */
export const DwPreview = memo(DwPreviewImpl)
