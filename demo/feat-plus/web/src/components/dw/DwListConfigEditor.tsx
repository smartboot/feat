import { useEffect, useState, type ReactNode } from "react"
import { ChevronDown, Plus, Settings2, Wand2 } from "lucide-react"

import type {
  DwAction,
  DwColumn,
  DwColumnFormat,
  DwFieldMeta,
  DwFilter,
  DwFilterWidget,
  DwListConfig,
  DwRowAction,
} from "@/api/types"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { FieldLabel, Hint } from "@/components/dw/DwEditorBits"
import { DwOptionListEditor } from "@/components/dw/DwOptionListEditor"
import {
  ACTION_TEMPLATE,
  COLUMN_TEMPLATE,
  FILTER_TEMPLATE,
  RowDelete,
  RowTable,
  RowTableItem,
  reorder,
  type RowDrag,
} from "@/components/dw/DwRowTable"
import type { EditorLink } from "@/components/dw/link"
import {
  ACTION_HINTS,
  ACTION_LABELS,
  COLUMN_FORMATS,
  COLUMN_FORMAT_HINTS,
  COLUMN_FORMAT_LABELS,
  FIELD_TYPE_LABELS,
  FILTER_WIDGET_LABELS,
  FILTER_WIDGETS,
  MAX_ROW_ACTIONS,
  WIDTH_TIERS,
  columnFromField,
  defaultFormatFor,
  isKnownWidth,
  newRowAction,
} from "@/components/dw/meta"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { cn } from "@/lib/utils"

/** create / refresh 是工具栏按钮，edit / delete 是行内动作——分组照用户看到的位置来。 */
const TOOLBAR_ACTIONS: DwAction[] = ["create", "refresh"]
const ROW_ACTIONS: DwAction[] = ["edit", "delete"]

/**
 * 列表视图编辑器。
 *
 * 结构按「用户的注意力」排，不按 list_json 的 key 排：
 *
 *   摘要条 → 列 → 筛选 → 动作 → 技术参数（折叠）
 *
 * 列表配置 90% 的使用是「改已有一列」，不是「从零建一页」。所以列与筛选是表格式的
 * 行内编辑（一行一条、栅格对齐、二级配置点开才展开），行主键 / 每页条数 / 接口覆盖
 * 收进默认折叠的技术参数——改这些的人知道自己要找什么，不需要的人不该被它们占第一屏。
 *
 * 三处刻意的设计：
 * ① 字段只能从数据源声明里选，已用过的置灰——「配置漂移」与「同字段两列」在这里就没有输入面；
 * ② 列名默认继承数据源字段标签，改过的才单独存；
 * ③ 宽度给刻度而不是 Tailwind 类名——存量值（w-36 之类）识别成「自定义」原样保留，不覆盖。
 */
export function DwListConfigEditor({
  value,
  fields,
  writable,
  onChange,
  link,
}: {
  value: DwListConfig
  fields: DwFieldMeta[]
  /** 只读数据源：写动作只能看不能勾 */
  writable: boolean
  onChange: (list: DwListConfig) => void
  /** 与右侧预览的联动：悬停 / 聚焦某一条时，预览里对应位置高亮 */
  link?: EditorLink
}) {
  const fieldKeys = new Set(fields.map((field) => field.key))
  // 覆盖列之前先确认：现有列的格式 / 宽度 / 选项映射是手工调过的，静默覆盖等于丢工作。
  const [confirmGenerate, setConfirmGenerate] = useState(false)
  const pageSizeBad = value.pageSize < 1 || value.pageSize > 200
  const hasApiOverride = Boolean(value.dataApi?.query || value.dataApi?.delete)
  const [techOpen, setTechOpen] = useState(() => pageSizeBad || hasApiOverride)
  /** 同一时刻只展开一条：两条同时展开就又回到「卡片堆」那种看不完的状态。 */
  const [expanded, setExpanded] = useState("")
  const [dragFrom, setDragFrom] = useState<number | null>(null)
  const rowActions = value.rowActions ?? []

  // 每页条数越界时把技术参数自动摊开：错误指向一个折叠着的抽屉等于没指。
  useEffect(() => {
    if (pageSizeBad) setTechOpen(true)
  }, [pageSizeBad])

  function patch(patch: Partial<DwListConfig>) {
    onChange({ ...value, ...patch })
  }

  function toggleExpanded(key: string) {
    setExpanded((current) => (current === key ? "" : key))
  }

  /** 拖拽只负责记住「从第几条开始」，落点由目标行给。 */
  function dragHandlers(index: number, list: unknown[], commit: (from: number, to: number) => void): RowDrag {
    return {
      dragging: dragFrom === index,
      onDragStart: (event) => {
        event.dataTransfer.effectAllowed = "move"
        event.dataTransfer.setData("text/plain", String(index))
        setDragFrom(index)
      },
      onDragOver: (event) => {
        if (dragFrom === null) return
        event.preventDefault()
        event.dataTransfer.dropEffect = "move"
      },
      onDrop: (event) => {
        event.preventDefault()
        if (dragFrom !== null && dragFrom !== index && index < list.length) commit(dragFrom, index)
        setDragFrom(null)
      },
      onDragEnd: () => setDragFrom(null),
    }
  }

  function updateColumn(index: number, next: DwColumn) {
    patch({ columns: value.columns.map((column, i) => (i === index ? next : column)) })
  }

  function moveColumn(index: number, delta: number) {
    patch({ columns: reorder(value.columns, index, index + delta) })
  }

  function updateFilter(index: number, next: DwFilter) {
    patch({ filters: value.filters.map((filter, i) => (i === index ? next : filter)) })
  }

  function moveFilter(index: number, delta: number) {
    patch({ filters: reorder(value.filters, index, index + delta) })
  }

  /** 行操作是数组项，逐项打补丁；整表替换会把这行其他输入框的焦点状态一起换掉。 */
  function updateRowAction(index: number, patchAction: Partial<DwRowAction>) {
    patch({
      rowActions: rowActions.map((action, i) => (i === index ? { ...action, ...patchAction } : action)),
    })
  }

  function moveRowAction(index: number, delta: number) {
    patch({ rowActions: reorder(rowActions, index, index + delta) })
  }

  /** 通道覆盖的字段级更新：dataApi 整体存在或不存在都好处理。 */
  function patchDataApi(patch: Partial<NonNullable<DwListConfig["dataApi"]>>) {
    onChange({ ...value, dataApi: { ...value.dataApi, ...patch } })
  }

  function toggleAction(action: DwAction) {
    const active = value.actions.includes(action)
    patch({
      actions: active ? value.actions.filter((item) => item !== action) : [...value.actions, action],
    })
  }

  /** 按数据源字段一键生成列：格式按声明类型映射，列名继承字段标签。 */
  function generateColumns() {
    patch({ columns: fields.map(columnFromField) })
    setConfirmGenerate(false)
  }

  const unknownKeys = [
    ...value.columns.map((column) => column.key),
    ...value.filters.map((filter) => filter.key),
  ].filter((key) => key !== "" && !fieldKeys.has(key))

  /** 就地清理不认识引用的列 / 筛选——在出事的分区里修，而不是轰用户去「字段」分区。 */
  function dropUnknown() {
    patch({
      columns: value.columns.filter((column) => !column.key || fieldKeys.has(column.key)),
      filters: value.filters.filter((filter) => !filter.key || fieldKeys.has(filter.key)),
    })
  }

  const unusedFields = fields.filter(
    (field) =>
      !value.columns.some((column) => column.key === field.key) &&
      !value.filters.some((filter) => filter.key === field.key),
  )

  return (
    <div className="grid gap-5">
      {/* 摘要条：不滚动就知道这一页现在长什么样。 */}
      <div
        data-testid="list-summary"
        className="flex flex-wrap items-center gap-x-1.5 gap-y-1 rounded-lg bg-muted/40 px-3 py-1.5 text-[11px] text-muted-foreground"
      >
        <span className="font-medium text-foreground">{value.columns.length} 列</span>
        <span>·</span>
        <span>{value.filters.length} 筛选</span>
        <span>·</span>
        <span>{value.actions.length + rowActions.length} 个动作</span>
        <span>·</span>
        <span>{writable ? "可写" : "只读"}</span>
        <span>·</span>
        <span className={cn(pageSizeBad && "font-medium text-destructive")}>每页 {value.pageSize || "?"} 条</span>
        <span className="ml-auto">在这张表上直接改，右侧预览同步</span>
      </div>

      {unknownKeys.length > 0 && (
        <div
          className="flex flex-wrap items-center gap-2 rounded-md border border-destructive/30 bg-destructive/5 px-2 py-1.5 text-[11px] text-destructive"
          role="alert"
        >
          <span className="min-w-0 flex-1">
            这些字段不在当前数据源的声明里（可能是切换过数据源），保存会被拒绝：
            <span className="font-mono"> {unknownKeys.join("、")}</span>
          </span>
          <Button variant="outline" size="sm" className="h-6 shrink-0 px-1.5 text-[11px]" onClick={dropUnknown}>
            移除它们
          </Button>
        </div>
      )}

      <EditorSection
        title="列"
        count={`${value.columns.length} 列`}
        hint="按显示顺序排列；拖行首手柄，或用行尾箭头调整。点 ⋯ 配格式细节。"
        anchor="list-columns"
        actions={
          <>
            <Button
              variant="outline"
              size="sm"
              className="h-7 gap-1 text-xs"
              onClick={() => (value.columns.length === 0 ? generateColumns() : setConfirmGenerate(true))}
              title="按数据源的字段生成列，会覆盖现有列"
            >
              <Wand2 className="size-3" />
              按字段生成
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="h-7 gap-1 text-xs"
              onClick={() =>
                patch({
                  columns: [...value.columns, { key: "", label: "", format: "text", width: "", options: [] }],
                })
              }
            >
              <Plus className="size-3" />
              添加
            </Button>
          </>
        }
        footer={
          unusedFields.length > 0 && value.columns.length > 0 ? (
            <p className="flex flex-wrap items-center gap-x-1 text-[11px] text-muted-foreground">
              还有 {unusedFields.length} 个字段没进列表：
              {unusedFields
                .slice(0, 6)
                .map((field) => field.label)
                .join("、")}
              {unusedFields.length > 6 ? " 等" : ""}
              <button
                type="button"
                className="text-primary underline-offset-2 hover:underline"
                onClick={() =>
                  patch({ columns: [...value.columns, ...unusedFields.map((field) => columnFromField(field))] })
                }
              >
                全部加入
              </button>
            </p>
          ) : undefined
        }
      >
        {value.columns.length === 0 ? (
          <EmptyColumns fields={fields} onGenerate={generateColumns} />
        ) : (
          <RowTable template={COLUMN_TEMPLATE} header={["", "列名", "字段", "格式", "宽度", ""]}>
            {value.columns.map((column, index) => {
              const meta = fields.find((field) => field.key === column.key)
              const usableKeys = value.columns.map((item, i) => (i === index ? "" : item.key))
              return (
                <RowTableItem
                  key={index}
                  template={COLUMN_TEMPLATE}
                  target={{ kind: "column", index }}
                  link={link}
                  index={index}
                  total={value.columns.length}
                  onMove={(delta) => moveColumn(index, delta)}
                  drag={dragHandlers(index, value.columns, (from, to) =>
                    patch({ columns: reorder(value.columns, from, to) }),
                  )}
                  expanded={expanded === `column-${index}`}
                  onToggleExpand={() => toggleExpanded(`column-${index}`)}
                  detail={
                    <div className="grid gap-2">
                      {column.format === "badge" ? (
                        <div className="grid gap-1">
                          <FieldLabel>值 → 文案映射（不配只显示原始值）</FieldLabel>
                          <DwOptionListEditor
                            options={column.options ?? []}
                            onChange={(options) => updateColumn(index, { ...column, options })}
                          />
                        </div>
                      ) : (
                        <p className="text-[11px] text-muted-foreground">{COLUMN_FORMAT_HINTS[column.format]}</p>
                      )}
                      <RowDelete
                        name={column.label || meta?.label || column.key || `第 ${index + 1} 列`}
                        label={`删掉不影响数据，只是这页不再显示「${
                          column.label || meta?.label || column.key || "未选择字段"
                        }」。`}
                        onRemove={() => patch({ columns: value.columns.filter((_, i) => i !== index) })}
                      />
                    </div>
                  }
                >
                  <Input
                    className="h-7 text-xs"
                    aria-label={`第 ${index + 1} 列的列名`}
                    value={column.label}
                    placeholder={meta?.label || "表头文案"}
                    onChange={(event) => updateColumn(index, { ...column, label: event.target.value })}
                  />
                  <FieldKeySelect
                    compact
                    ariaLabel={`第 ${index + 1} 列的字段`}
                    value={column.key}
                    fields={fields}
                    used={usableKeys}
                    onChange={(key) => {
                      const nextMeta = fields.find((field) => field.key === key)
                      updateColumn(index, {
                        ...column,
                        key,
                        // 列名还是空的或是上一个字段的标签时，跟着换；手工改过就保留。
                        label: !column.label || column.label === meta?.label ? nextMeta?.label ?? "" : column.label,
                        format: meta ? column.format : defaultFormatFor(nextMeta?.type ?? "string"),
                      })
                    }}
                  />
                  <Select
                    value={column.format}
                    onValueChange={(format) => updateColumn(index, { ...column, format: format as DwColumnFormat })}
                  >
                    <SelectTrigger className="h-7 w-full text-xs" aria-label={`第 ${index + 1} 列的格式`}>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {COLUMN_FORMATS.map((format) => (
                        <SelectItem key={format} value={format}>
                          {COLUMN_FORMAT_LABELS[format]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <WidthTicks
                    value={column.width}
                    ariaLabel={`第 ${index + 1} 列的宽度`}
                    onChange={(width) => updateColumn(index, { ...column, width })}
                  />
                </RowTableItem>
              )
            })}
          </RowTable>
        )}
      </EditorSection>

      <EditorSection
        title="筛选"
        count={`${value.filters.length} 项`}
        hint="列表上方那一排条件；没有筛选项时，工具栏不出现筛选区。"
        anchor="list-filters"
        actions={
          <Button
            variant="outline"
            size="sm"
            className="h-7 gap-1 text-xs"
            onClick={() =>
              patch({
                filters: [...value.filters, { key: "", label: "", widget: "input", placeholder: "", options: [] }],
              })
            }
          >
            <Plus className="size-3" />
            添加
          </Button>
        }
      >
        {value.filters.length === 0 ? (
          <p className="rounded-md border border-dashed px-3 py-2.5 text-[11px] text-muted-foreground">
            还没有筛选项。加一个之后列表工具栏会出现它——配置只决定控件形态，筛选值由数据源自己读取。
          </p>
        ) : (
          <RowTable template={FILTER_TEMPLATE} header={["", "名称", "字段", "控件", "其余配置", ""]}>
            {value.filters.map((filter, index) => {
              const meta = fields.find((field) => field.key === filter.key)
              return (
                <RowTableItem
                  key={index}
                  template={FILTER_TEMPLATE}
                  target={{ kind: "filter", index }}
                  link={link}
                  index={index}
                  total={value.filters.length}
                  onMove={(delta) => moveFilter(index, delta)}
                  drag={dragHandlers(index, value.filters, (from, to) =>
                    patch({ filters: reorder(value.filters, from, to) }),
                  )}
                  expanded={expanded === `filter-${index}`}
                  onToggleExpand={() => toggleExpanded(`filter-${index}`)}
                  detail={
                    <div className="grid gap-2">
                      {filter.widget === "input" && (
                        <div className="grid gap-1">
                          <FieldLabel>占位提示</FieldLabel>
                          <Input
                            className="h-7 text-xs"
                            aria-label={`第 ${index + 1} 个筛选的占位提示`}
                            value={filter.placeholder ?? ""}
                            placeholder={filter.label || "输入关键字"}
                            onChange={(event) => updateFilter(index, { ...filter, placeholder: event.target.value })}
                          />
                        </div>
                      )}
                      {filter.widget === "dateRange" && (
                        <div className="grid gap-1">
                          <FieldLabel>结束字段</FieldLabel>
                          <FieldKeySelect
                            compact
                            value={filter.endKey ?? ""}
                            fields={fields}
                            used={reservedForEnd(value.filters, index)}
                            onChange={(endKey) => updateFilter(index, { ...filter, endKey })}
                            ariaLabel="日期范围结束字段"
                          />
                          <Hint>
                            日期范围占两个筛选键：起始 <code>{filter.key || "（未选）"}</code>、结束{" "}
                            <code>{filter.endKey || "（未选）"}</code>，数据源分别读，不做拼接解析。
                          </Hint>
                        </div>
                      )}
                      {filter.widget === "select" && (
                        <FilterOptionsEditor
                          filter={filter}
                          onChange={(next) => updateFilter(index, { ...filter, ...next })}
                        />
                      )}
                      <RowDelete
                        name={filter.label || meta?.label || `第 ${index + 1} 个筛选`}
                        label={`删掉这一项不会改变数据源，只是工具栏上不再出现「${
                          filter.label || meta?.label || "这一项"
                        }」。`}
                        onRemove={() => patch({ filters: value.filters.filter((_, i) => i !== index) })}
                      />
                    </div>
                  }
                >
                  <Input
                    className="h-7 text-xs"
                    aria-label={`第 ${index + 1} 个筛选的名称`}
                    value={filter.label}
                    placeholder="筛选项文案"
                    onChange={(event) => updateFilter(index, { ...filter, label: event.target.value })}
                  />
                  <FieldKeySelect
                    compact
                    ariaLabel={`第 ${index + 1} 个筛选的字段`}
                    value={filter.key}
                    fields={fields}
                    used={usedByOthers(value.filters, index)}
                    onChange={(key) => {
                      const nextMeta = fields.find((field) => field.key === key)
                      updateFilter(index, {
                        ...filter,
                        key,
                        label: !filter.label || filter.label === meta?.label ? nextMeta?.label ?? "" : filter.label,
                      })
                    }}
                  />
                  <Select
                    value={filter.widget}
                    onValueChange={(widget) =>
                      // 换控件时清掉另一支的配置：否则会存下「既是下拉又是日期范围」的混合体，
                      // 换回来时又带着上次的选项，配置页说不清它现在是什么。
                      updateFilter(index, {
                        ...filter,
                        widget: widget as DwFilterWidget,
                        endKey: widget === "dateRange" ? (filter.endKey ?? "") : "",
                        options: widget === "select" ? (filter.options ?? []) : [],
                        optionsApi: widget === "select" ? (filter.optionsApi ?? "") : "",
                        optionValueField: widget === "select" ? (filter.optionValueField ?? "") : "",
                        optionLabelField: widget === "select" ? (filter.optionLabelField ?? "") : "",
                      })
                    }
                  >
                    <SelectTrigger className="h-7 w-full text-xs" aria-label={`第 ${index + 1} 个筛选的控件`}>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {FILTER_WIDGETS.map((widget) => (
                        <SelectItem key={widget} value={widget}>
                          {FILTER_WIDGET_LABELS[widget]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <button
                    type="button"
                    className={cn(
                      "truncate rounded px-1 py-0.5 text-left text-[11px] hover:bg-muted",
                      filterNeedsAttention(filter) ? "text-amber-700 dark:text-amber-300" : "text-muted-foreground",
                    )}
                    onClick={() => toggleExpanded(`filter-${index}`)}
                    title="点开配这一项的细节"
                  >
                    {filterSummary(filter)}
                  </button>
                </RowTableItem>
              )
            })}
          </RowTable>
        )}
      </EditorSection>

      <EditorSection
        title="动作"
        count={`${value.actions.length + rowActions.length} 个`}
        hint="用户在这页能做什么；没有写权限的人看不到这些按钮。"
        anchor="list-actions"
      >
        <div className="divide-y overflow-hidden rounded-lg border">
          <div className="flex flex-wrap items-center gap-x-2 gap-y-1 px-2 py-1.5">
            <span className="w-12 shrink-0 text-[11px] text-muted-foreground">工具栏</span>
            {TOOLBAR_ACTIONS.map((action) => (
              <ActionToggle
                key={action}
                action={action}
                active={value.actions.includes(action)}
                disabled={!writable && action !== "refresh"}
                onToggle={() => toggleAction(action)}
              />
            ))}
            <span className="ml-auto text-[11px] text-muted-foreground">右上角按钮区</span>
          </div>
          <div className="flex flex-wrap items-center gap-x-2 gap-y-1 px-2 py-1.5">
            <span className="w-12 shrink-0 text-[11px] text-muted-foreground">每一行</span>
            {ROW_ACTIONS.map((action) => (
              <ActionToggle
                key={action}
                action={action}
                active={value.actions.includes(action)}
                disabled={!writable}
                onToggle={() => toggleAction(action)}
              />
            ))}
            <span className="ml-1 flex items-center gap-1 text-[11px] text-muted-foreground" id="row-key">
              按
              <span className="w-28">
                <FieldKeySelect
                  compact
                  ariaLabel="行主键字段"
                  value={value.rowKey}
                  fields={fields}
                  onChange={(key) => patch({ rowKey: key })}
                />
              </span>
              定位这一行
            </span>
          </div>
        </div>

        <div className="grid gap-2 pt-1" id="list-row-actions">
          <div className="flex items-center justify-between gap-2">
            <span className="text-[11px] text-muted-foreground">
              自定义按钮
              <span className="ml-1">
                {rowActions.length}/{MAX_ROW_ACTIONS}
              </span>
            </span>
            <Button
              variant="outline"
              size="sm"
              className="h-7 gap-1 text-xs"
              disabled={!writable || rowActions.length >= MAX_ROW_ACTIONS}
              title={
                !writable
                  ? "只读数据源不能加自定义行操作"
                  : rowActions.length >= MAX_ROW_ACTIONS
                    ? `已到上限 ${MAX_ROW_ACTIONS} 个：再多就不该挤在一个下拉里，改做成工具栏的批量操作`
                    : undefined
              }
              onClick={() => patch({ rowActions: [...rowActions, newRowAction()] })}
            >
              <Plus className="size-3" />
              添加
            </Button>
          </div>

          {rowActions.length === 0 ? (
            <p className="rounded-md border border-dashed px-3 py-2 text-[11px] text-muted-foreground">
              {writable
                ? "没有自定义按钮。需要「重置密码」「重新分配」这类对单行的操作时加一个，它会排在编辑 / 删除之后。"
                : "该数据源是只读的，不能加自定义行操作——行操作属于写动作，与编辑 / 删除同一条约束。"}
            </p>
          ) : (
            <RowTable template={ACTION_TEMPLATE} header={["", "按钮名称", "接口（POST）", "提示框", ""]}>
              {rowActions.map((action, index) => (
                <RowTableItem
                  key={index}
                  template={ACTION_TEMPLATE}
                  index={index}
                  total={rowActions.length}
                  onMove={(delta) => moveRowAction(index, delta)}
                  expandable={false}
                  onRemove={() => patch({ rowActions: rowActions.filter((_, i) => i !== index) })}
                >
                  <Input
                    className="h-7 text-xs"
                    aria-label={`第 ${index + 1} 个自定义按钮的名称`}
                    maxLength={12}
                    value={action.label}
                    placeholder="重置密码"
                    onChange={(event) => updateRowAction(index, { label: event.target.value })}
                  />
                  <Input
                    className="h-7 font-mono text-[11px]"
                    aria-label={`第 ${index + 1} 个自定义按钮的接口`}
                    value={action.api}
                    placeholder="/api/users/reset-password"
                    onChange={(event) => updateRowAction(index, { api: event.target.value.trim() })}
                  />
                  <Input
                    className="h-7 text-xs"
                    aria-label={`第 ${index + 1} 个自定义按钮的提示框`}
                    maxLength={120}
                    value={action.confirm ?? ""}
                    placeholder="留空：点一下直接执行"
                    onChange={(event) => updateRowAction(index, { confirm: event.target.value })}
                  />
                </RowTableItem>
              ))}
            </RowTable>
          )}

          {rowActions.length > 0 && (
            <p className="text-[11px] text-muted-foreground">
              调用方式固定：<code>POST {"{接口}"}</code>，请求体是{" "}
              <code>{`{"${value.rowKey || "id"}": 主键值}`}</code>——主键由上面的「定位这一行」决定。
              能不能点仍受当前账号写权限过滤；接口自己的鉴权由目标接口负责，配置只存路径。
            </p>
          )}
        </div>
      </EditorSection>

      <div className="rounded-lg border border-dashed">
        <button
          type="button"
          id="page-size"
          aria-expanded={techOpen}
          onClick={() => setTechOpen((open) => !open)}
          className="flex w-full items-center gap-2 px-3 py-2 text-left"
        >
          <Settings2 className="size-3.5 shrink-0 text-muted-foreground" />
          <span className="shrink-0 text-[11px] font-medium">技术参数</span>
          <span
            className={cn(
              "min-w-0 flex-1 truncate text-[11px] text-muted-foreground",
              pageSizeBad && "font-medium text-destructive",
            )}
          >
            每页 {value.pageSize || "?"} 条 · {hasApiOverride ? "查询 / 删除有接口覆盖" : "查询与删除走内置通道"}
          </span>
          <ChevronDown
            className={cn("size-3.5 shrink-0 text-muted-foreground transition-transform", techOpen && "rotate-180")}
          />
        </button>
        {techOpen && (
          <div className="grid gap-3 border-t px-3 py-3">
            <div className="grid gap-3 sm:grid-cols-3">
              <div className="grid gap-1">
                <FieldLabel>每页条数</FieldLabel>
                <Input
                  className="h-7 text-xs"
                  aria-label="每页条数"
                  type="number"
                  min={1}
                  max={200}
                  value={value.pageSize}
                  onChange={(event) => patch({ pageSize: Number(event.target.value) || 0 })}
                />
              </div>
              <div className="grid gap-1" id="data-api-query">
                <FieldLabel>查询接口（留空用内置）</FieldLabel>
                <Input
                  className="h-7 font-mono text-[11px]"
                  aria-label="查询接口"
                  value={value.dataApi?.query ?? ""}
                  placeholder="/api/..."
                  onChange={(event) => patchDataApi({ query: event.target.value.trim() })}
                />
              </div>
              <div className="grid gap-1" id="data-api-delete">
                <FieldLabel>删除接口（留空用内置）</FieldLabel>
                <Input
                  className="h-7 font-mono text-[11px]"
                  aria-label="删除接口"
                  value={value.dataApi?.delete ?? ""}
                  placeholder="/api/..."
                  onChange={(event) => patchDataApi({ delete: event.target.value.trim() })}
                />
              </div>
            </div>
            <Hint>
              每页条数允许 1–200，用户可以在列表底部临时改成其他档位。接口覆盖只影响查询与删除：查询走 GET{" "}
              <code>{"{path}?page&pageSize&filter"}</code>、删除走 DELETE <code>{"{path}/{id}"}</code>；
              保存与详情不支持覆盖——写路径牵涉校验与审计，不开旁路。
            </Hint>
          </div>
        )}
      </div>

      <AlertDialog open={confirmGenerate} onOpenChange={setConfirmGenerate}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>按字段重新生成列？</AlertDialogTitle>
            <AlertDialogDescription>
              现有 {value.columns.length} 列的格式、宽度与状态映射是手工配置的，重新生成会全部覆盖。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction onClick={generateColumns}>重新生成</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}

/** 分区标题：一句话说清这块配的是什么，动作靠右。 */
function EditorSection({
  title,
  count,
  hint,
  anchor,
  actions,
  children,
  footer,
}: {
  title: string
  count: string
  hint: string
  anchor: string
  actions?: ReactNode
  children: ReactNode
  footer?: ReactNode
}) {
  return (
    <section className="grid gap-2" id={anchor}>
      <div className="flex flex-wrap items-end justify-between gap-2">
        <div className="min-w-0">
          <h4 className="flex items-baseline gap-1.5 text-xs font-medium">
            {title}
            <span className="text-[11px] font-normal text-muted-foreground">{count}</span>
          </h4>
          <p className="text-[11px] leading-relaxed text-muted-foreground">{hint}</p>
        </div>
        {actions && <div className="flex shrink-0 items-center gap-1.5">{actions}</div>}
      </div>
      {children}
      {footer}
    </section>
  )
}

/** 动作开关：一个按钮的「有 / 没有」，样式沿用原来的勾选观感。 */
function ActionToggle({
  action,
  active,
  disabled,
  onToggle,
}: {
  action: DwAction
  active: boolean
  disabled: boolean
  onToggle: () => void
}) {
  return (
    <Button
      type="button"
      variant="outline"
      size="sm"
      aria-pressed={active}
      className={cn("h-7 text-xs", active && "border-primary bg-primary/10 text-primary")}
      disabled={disabled}
      onClick={onToggle}
      title={disabled ? "只读数据源不能配置写动作" : ACTION_HINTS[action]}
    >
      {ACTION_LABELS[action]}
    </Button>
  )
}

/** 列空态：直接给出一条出路，而不是让用户去标题右边找按钮。 */
function EmptyColumns({ fields, onGenerate }: { fields: DwFieldMeta[]; onGenerate: () => void }) {
  return (
    <div className="grid gap-2 rounded-md border border-dashed p-3">
      <p className="text-[11px] text-muted-foreground">
        还没有列。这个数据源声明了 {fields.length} 个字段——一次全加上、再删多余的，
        比一列一列加快。
      </p>
      <div className="flex flex-wrap gap-1.5">
        <Button variant="outline" size="sm" className="h-7 gap-1 text-xs" onClick={onGenerate}>
          <Wand2 className="size-3" />
          按字段全部生成
        </Button>
      </div>
    </div>
  )
}

/** 筛选行的「其余配置」摘要：不展开也知道这一项缺什么。 */
function filterSummary(filter: DwFilter): string {
  if (filter.widget === "input") return filter.placeholder ? `提示「${filter.placeholder}」` : "无额外配置"
  if (filter.widget === "dateRange") return filter.endKey ? `止于 ${filter.endKey}` : "未配结束字段"
  if (filter.optionsApi) return `接口 ${filter.optionsApi}`
  return `写死 ${filter.options?.length ?? 0} 项`
}

/** 下拉筛选一项都没配：行内先黄一下，别等保存时才发现下拉是空的。 */
function filterNeedsAttention(filter: DwFilter): boolean {
  return filter.widget === "select" && !filter.optionsApi && (filter.options?.length ?? 0) === 0
}

/** select 筛选的选项来源：静态与接口互斥，切过去就清另一支。 */
function FilterOptionsEditor({
  filter,
  onChange,
}: {
  filter: DwFilter
  onChange: (patch: Partial<DwFilter>) => void
}) {
  return (
    <div className="grid gap-2">
      <div className="grid gap-1">
        <FieldLabel>选项来源</FieldLabel>
        <Select
          value={filter.optionsApi ? "api" : "static"}
          onValueChange={(source) =>
            onChange({
              // 两个来源互斥：留着一半的旧配置，取数时就要猜「到底哪个算」。
              options: source === "static" ? (filter.options ?? []) : [],
              optionsApi: source === "api" ? (filter.optionsApi ?? "") : "",
              optionValueField: source === "api" ? (filter.optionValueField ?? "") : "",
              optionLabelField: source === "api" ? (filter.optionLabelField ?? "") : "",
            })
          }
        >
          <SelectTrigger className="h-7 w-[180px] text-xs" aria-label="选项来源">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="static">写死的选项</SelectItem>
            <SelectItem value="api">从接口按需取</SelectItem>
          </SelectContent>
        </Select>
      </div>
      {filter.optionsApi ? (
        <div className="grid gap-2 sm:grid-cols-3">
          <div className="grid gap-1 sm:col-span-3">
            <FieldLabel>接口路径</FieldLabel>
            <Input
              className="h-7 font-mono text-[11px]"
              aria-label="选项接口"
              value={filter.optionsApi}
              placeholder="/api/activities/operators"
              onChange={(event) => onChange({ optionsApi: event.target.value })}
            />
          </div>
          <div className="grid gap-1">
            <FieldLabel>取值字段</FieldLabel>
            <Input
              className="h-7 text-xs"
              aria-label="取值字段"
              value={filter.optionValueField ?? ""}
              placeholder="id"
              onChange={(event) => onChange({ optionValueField: event.target.value })}
            />
          </div>
          <div className="grid gap-1">
            <FieldLabel>显示字段</FieldLabel>
            <Input
              className="h-7 text-xs"
              aria-label="显示字段"
              value={filter.optionLabelField ?? ""}
              placeholder="name"
              onChange={(event) => onChange({ optionLabelField: event.target.value })}
            />
          </div>
          <p className="text-[11px] leading-relaxed text-muted-foreground sm:col-span-3">
            只允许站内 <code>/api/</code> 路径，接口返回对象数组。选项在打开页面时现取、不写进配置——
            角色改名、新增部门之后不用回来重存这一页。
          </p>
        </div>
      ) : (
        <div className="grid gap-1">
          <FieldLabel>选项（下拉里除「全部」之外的取值）</FieldLabel>
          <DwOptionListEditor options={filter.options ?? []} onChange={(options) => onChange({ options })} />
        </div>
      )}
    </div>
  )
}

/**
 * 宽度刻度。四档排在一起，选中的那根填色——宽度是相对关系，
 * 下拉里读「中」看不出它比「窄」宽多少，刻度条一眼就能比。
 * 存量值（w-40 之类）识别不了时退回输入框并原样保留。
 */
function WidthTicks({
  value,
  ariaLabel,
  onChange,
}: {
  value?: string
  ariaLabel: string
  onChange: (value: string) => void
}) {
  const [forceCustom, setForceCustom] = useState(false)
  const custom = forceCustom || !isKnownWidth(value)

  if (custom) {
    return (
      <div className="flex items-center gap-1">
        <Input
          className="h-7 font-mono text-[11px]"
          aria-label={`${ariaLabel}（自定义）`}
          value={value ?? ""}
          placeholder="留空自适应"
          onChange={(event) => onChange(event.target.value)}
        />
        <Button
          variant="ghost"
          size="sm"
          className="h-7 shrink-0 px-1 text-[10px]"
          onClick={() => {
            setForceCustom(false)
            onChange("")
          }}
        >
          档位
        </Button>
      </div>
    )
  }

  return (
    <div className="flex items-center gap-1">
      <div className="flex flex-1 items-end gap-0.5" role="group" aria-label={ariaLabel}>
        {WIDTH_TIERS.map((tier, tierIndex) => {
          const active = (value ?? "") === tier.value
          return (
            <button
              key={tier.value || "auto"}
              type="button"
              title={`${tier.label}：${tier.hint}`}
              aria-label={`${ariaLabel}：${tier.label}`}
              aria-pressed={active}
              className="flex h-6 flex-1 items-end justify-center rounded-sm px-0.5 hover:bg-muted"
              onClick={() => onChange(tier.value)}
            >
              <span
                className={cn(
                  "w-full rounded-[1px] transition-colors",
                  active ? "bg-primary" : "bg-muted-foreground/25 hover:bg-muted-foreground/40",
                )}
                style={{ height: 5 + tierIndex * 3 }}
              />
            </button>
          )
        })}
      </div>
      <button
        type="button"
        className="shrink-0 px-0.5 text-[10px] text-muted-foreground/60 hover:text-foreground"
        aria-label={`${ariaLabel}：自定义`}
        title="自定义宽度（填 Tailwind 类名）"
        onClick={() => setForceCustom(true)}
      >
        ⋯
      </button>
    </div>
  )
}

/**
 * 被「其他筛选」占用的字段键，含它们的日期范围结束键。
 * 漏掉 endKey 的后果是两个筛选能配成同一个结束字段，保存时被后端拒掉——
 * 前端能选、后端才报错，是这一层最该提前挡住的错配。
 */
function usedByOthers(filters: DwFilter[], index: number): string[] {
  const used: string[] = []
  filters.forEach((filter, i) => {
    if (i === index) return
    if (filter.key) used.push(filter.key)
    if (filter.widget === "dateRange" && filter.endKey) used.push(filter.endKey)
  })
  return used
}

/** 结束字段额外要排除当前筛选自己的起始键：起止配成同一个字段没有意义。 */
function reservedForEnd(filters: DwFilter[], index: number): string[] {
  const used = usedByOthers(filters, index)
  const self = filters[index]?.key
  if (self) used.push(self)
  return used
}

/**
 * 字段下拉：候选来自数据源声明，已用在别处的 key 置灰，杜绝「重复字段」的输入面。
 * 供列表与表单编辑器共用。
 */
export function FieldKeySelect({
  value,
  fields,
  used = [],
  onChange,
  ariaLabel,
  compact = false,
}: {
  value: string
  fields: DwFieldMeta[]
  /** 已被其他行占用的 key（当前行自己的 key 不算） */
  used?: string[]
  onChange: (key: string) => void
  ariaLabel?: string
  /** 表格行内用更矮的尺寸，与 Input h-7 对齐 */
  compact?: boolean
}) {
  const usedSet = new Set(used.filter(Boolean))
  return (
    <Select value={value || undefined} onValueChange={onChange}>
      <SelectTrigger
        aria-label={ariaLabel}
        // 表格里的格子窄，选中的字段文案（「角色名称 name · 文本」）必须截断，
        // 否则会把右侧箭头挤出格子：一行高被撑开，整张表就歪了。
        className={cn("w-full [&>span]:min-w-0 [&>span]:truncate", compact ? "h-7 text-xs" : "h-8")}
      >
        <SelectValue placeholder="选择字段" />
      </SelectTrigger>
      <SelectContent>
        {fields.map((field) => (
          <SelectItem key={field.key} value={field.key} disabled={usedSet.has(field.key)}>
            {field.label}
            <span className="ml-1 text-[10px] text-muted-foreground">
              {field.key} · {FIELD_TYPE_LABELS[field.type] ?? field.type}
            </span>
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}
