import { useState } from "react"
import type { ReactNode } from "react"
import { Filter, Inbox, MoreHorizontal, Pencil, Plus, RotateCcw, Trash2, Zap } from "lucide-react"

import type { DwFilter, DwListConfig, DwRow, DwRowAction } from "@/api/types"
import { DwCell } from "@/components/dw/DwCell"
import { MarkDot } from "@/components/dw/DwEditorBits"
import type { EditorLink } from "@/components/dw/link"
import { focusAnchor, type DwFocusTarget } from "@/components/dw/link"
import { rowLabel } from "@/components/dw/meta"
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
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { cn } from "@/lib/utils"

/**
 * 列表的**唯一**渲染入口。
 *
 * 线上页面（{@link DwList}）和配置页预览（{@link DwPreview}）都用这一个组件。它不发请求、
 * 不持有业务状态：数据、筛选值、页码、动作处理全部由外面传进来，它只决定「长什么样」。
 *
 * ## 为什么必须同源
 *
 * 之前是这个组件的前身被抄成了两份——线上 553 行、预览 501 行，`DropdownMenu` 出现次数
 * 18 对 18，`AlertDialog` 35 对 35，逐行照抄。抄写立刻产生了三处真实分歧：
 *
 * | | 线上 | 预览（当时） |
 * |---|---|---|
 * | 列名为空 | 空表头 | 兜底成字段标签，把「漏配列名」藏起来了 |
 * | 空态 | 有「清除筛选」按钮 | 没有 |
 * | 筛选控件 | 支持 optionsApi 远程取数 | 另一套，只认静态 options |
 *
 * 预览的全部价值是「配完长这样」。一旦它渲染的不是同一套代码，它就是在说谎，
 * 而说谎的预览比没有预览更坏——配置者会照着它做决定。
 *
 * ## 预览靠注入装饰，而不是靠复制
 *
 * 预览比线上多的东西只有三类，全部走可选 prop：联动高亮（{@link DwListViewProps.link}）、
 * 确认框的「不会真的执行」说明（`confirmHint` / `actionDetail`）、预览态专属的提示文字
 * （`emptyColumns` / `footnote` / `editHint`）。没有一处是「另写一个组件」。
 *
 * ## 哪些按钮可点由「有没有 handler」决定
 *
 * 预览不传 `onDelete` / `onRowAction`，于是确认键自动禁用；不传 `onPageChange`，
 * 翻页按钮自动禁用。不需要一个 `preview` 布尔到处判断——那种布尔值迟早会在某个分支里
 * 判断漏掉，然后预览就出现一个线上没有的可用按钮。
 */
export interface DwListViewProps {
  list: DwListConfig
  title: string
  /** 新建按钮文案。线上取 form.createTitle，预览取同一份配置。 */
  createLabel?: string
  canWrite: boolean

  rows: DwRow[]
  total: number
  page: number
  pageSize: number
  /** 首屏加载中：渲染骨架行 */
  loading?: boolean
  /** 加载失败：整个表格区换成错误态 */
  failed?: boolean
  /** 后台刷新中：保留当前数据，标题下提示「正在更新」 */
  refreshing?: boolean
  onReload?: () => void

  /** 筛选草稿值（含日期范围的结束键）。不传则不渲染筛选区。 */
  filterValues?: Record<string, string>
  /** 已填写的筛选条目数，用于「N 个条件」与空态里的「清除筛选」 */
  activeFilterCount?: number
  onFilterChange?: (key: string, value: string) => void
  onResetFilters?: () => void
  /**
   * 是否允许筛选按 optionsApi 取远程选项。预览传 false：配置还没入库，
   * 请求不到；而且预览不该因为权限或空字典而看起来「坏了」。
   */
  remoteOptions?: boolean
  /**
   * 某筛选的可选项。**由容器解析**（含远程接口），展示层只负责摆——
   * 这样本组件不含任何 `useQuery`，预览就不必活在 QueryClientProvider 里。
   * 不传时回落到 `filter.options` 静态值。
   */
  optionsFor?: (filter: DwFilter, index: number) => Array<{ value: string; label: string }>

  onPageChange?: (page: number) => void
  onPageSizeChange?: (size: number) => void

  onCreate?: () => void
  onRefresh?: () => void
  /** index 是当前页内的行序（0 起）。预览靠它回填「第 N 行」，线上不用。 */
  onEdit?: (row: DwRow, index: number) => void
  /** 确认删除后调用。不传 → 确认键禁用（预览）。 */
  onDelete?: (row: DwRow) => Promise<unknown>
  /** 自定义按钮已确认（或无需确认）后调用。不传 → 确认键禁用（预览）。 */
  onRowAction?: (action: DwRowAction, row: DwRow) => Promise<unknown>
  /** 自定义按钮执行中：菜单项禁用，防止连点发多次 */
  actionPending?: boolean

  /** 确认框底部补充说明。预览用来说明「确认键不可用」。 */
  confirmHint?: ReactNode
  /** 自定义动作确认框里的请求明细（预览：接口与请求体） */
  actionDetail?: (action: DwRowAction, row: DwRow) => ReactNode
  /** 编辑菜单项尾注（预览：「回填第 N 行」） */
  editHint?: (rowIndex: number) => ReactNode

  /** 预览联动：悬停高亮、点击定位到配置、问题标记。线上不传。 */
  link?: EditorLink
  /** 列区为空时的提示（预览：「还没有配置列」） */
  emptyColumns?: ReactNode
  /** 表格下方补充说明（预览：行操作可点） */
  footnote?: ReactNode
  /**
   * 渲染在受限宽度里（预览面板）。只影响筛选区栅格：线上用视口断点 `md:grid-cols-N`，
   * 在 480px 的侧栏里会因为浏览器窗口宽而错误地铺成 4 列。
   */
  embedded?: boolean
}

export function DwListView(props: DwListViewProps) {
  const {
    list,
    title,
    createLabel,
    canWrite,
    rows,
    total,
    page,
    pageSize,
    loading,
    failed,
    refreshing,
    onReload,
    filterValues = {},
    activeFilterCount = 0,
    onFilterChange,
    onResetFilters,
    remoteOptions = true,
    optionsFor,
    onPageChange,
    onPageSizeChange,
    onCreate,
    onRefresh,
    onEdit,
    onDelete,
    onRowAction,
    actionPending,
    confirmHint,
    actionDetail,
    editHint,
    link,
    emptyColumns,
    footnote,
    embedded,
  } = props

  /** 待确认删除的行 */
  const [removing, setRemoving] = useState<DwRow | null>(null)
  /** 待确认的自定义动作 */
  const [pendingAction, setPendingAction] = useState<{ action: DwRowAction; row: DwRow } | null>(null)

  const builtinActions = list.actions.filter((action) => action === "edit" || action === "delete")
  const customActions = list.rowActions ?? []
  const showRowMenu = canWrite && (builtinActions.length > 0 || customActions.length > 0)
  const pageCount = Math.max(1, Math.ceil(total / Math.max(1, pageSize)))
  const showEmpty = !loading && !failed && rows.length === 0

  const focused = (target: DwFocusTarget) =>
    Boolean(link?.focus && link.focus.kind === target.kind && link.focus.index === target.index)
  const markAt = (target: DwFocusTarget) => link?.marks?.[target.kind]?.[target.index]

  /** 表头 / 筛选共用：高亮 + 悬停 + 点一下跳到配置区。 */
  const focusProps = (target: DwFocusTarget, label: string) => {
    if (!link) return {}
    return {
      "data-preview-focus": focused(target) ? focusAnchor(target) : undefined,
      // title 只在「能点」时给：不可点的元素挂一句「点击可跳转」是骗人
      title: link.onSelect ? `跳到「${label}」的配置` : undefined,
      onMouseEnter: link.onHover ? () => link.onHover?.(target) : undefined,
      onMouseLeave: link.onHover ? () => link.onHover?.(null) : undefined,
      onClick: link.onSelect ? () => link.onSelect?.(target) : undefined,
    }
  }

  async function confirmRemove() {
    const target = removing
    if (!target || !onDelete) return
    try {
      await onDelete(target)
    } catch {
      // 容器已经 toast 过；这里只负责收框。删除失败时列表没变，留着框也没用。
    } finally {
      setRemoving(null)
    }
  }

  async function runAction(action: DwRowAction, row: DwRow) {
    if (!onRowAction) return
    try {
      await onRowAction(action, row)
      // 成功才收：失败时确认框留着，用户能看清提示并原地重试。
      setPendingAction(null)
    } catch {
      // 同上，toast 由容器发
    }
  }

  /**
   * 配了提示框才拦一下；留空表示这个动作可逆、不需要二次确认。
   *
   * 预览侧没有 `onRowAction`，此时**一律弹框**：配置者要验证的正是「这句话会不会出现、
   * 接口对不对」，静默什么都不发生等于没演示。
   */
  function triggerAction(action: DwRowAction, row: DwRow) {
    if (!onRowAction || (action.confirm ?? "").trim()) {
      setPendingAction({ action, row })
      return
    }
    void runAction(action, row)
  }

  return (
    <div className="space-y-3">
      {list.filters.length > 0 && (
        <Card>
          <CardContent
            className={cn("grid gap-3 p-3 md:items-end", filterGrid(list.filters, Boolean(embedded)))}
          >
            {list.filters.map((filter, index) => {
              const target: DwFocusTarget = { kind: "filter", index }
              const active = focused(target)
              return (
                <div
                  key={`${filter.key}-${index}`}
                  {...focusProps(target, filter.label || `筛选 ${index + 1}`)}
                  className={cn(
                    "relative grid gap-2",
                    filter.widget === "dateRange" && !embedded && "md:col-span-2",
                    active && "rounded-md ring-2 ring-primary/30",
                    link?.onSelect && "cursor-pointer",
                  )}
                >
                  <Label
                    htmlFor={`dw-filter-${filter.key}`}
                    className={cn("flex items-center gap-1.5", link && "cursor-pointer")}
                  >
                    {filter.label}
                    {markAt(target) && <MarkDot level={markAt(target)!} />}
                  </Label>
                  <DwFilterControl
                    filter={filter}
                    options={optionsFor ? optionsFor(filter, index) : (filter.options ?? [])}
                    value={filterValues[filter.key] ?? ""}
                    endValue={filter.endKey ? (filterValues[filter.endKey] ?? "") : ""}
                    onChange={onFilterChange}
                    allowRemote={remoteOptions}
                  />
                </div>
              )
            })}
          </CardContent>
        </Card>
      )}

      <Card className="overflow-hidden">
        <CardHeader className="flex flex-row flex-wrap items-center justify-between gap-2 space-y-0 border-b px-5 py-4">
          <div className="min-w-0">
            <CardTitle className="text-base">{title}</CardTitle>
            <p className="mt-1 text-xs text-muted-foreground">
              共 {total} 条{refreshing && <span className="ml-2 text-primary">正在更新...</span>}
            </p>
          </div>
          <div className="flex items-center gap-2">
            {activeFilterCount > 0 && (
              <Button
                variant="ghost"
                size="sm"
                className="h-8 gap-2 text-xs"
                disabled={!onResetFilters}
                onClick={onResetFilters}
              >
                <RotateCcw className="size-3.5" />
                清除筛选
              </Button>
            )}
            {activeFilterCount > 0 && (
              <Badge variant="outline" className="gap-1">
                <Filter className="size-3" />
                {activeFilterCount} 个条件
              </Badge>
            )}
            {list.actions.includes("refresh") && (
              <Button variant="outline" size="sm" disabled={!onRefresh} onClick={onRefresh}>
                刷新
              </Button>
            )}
            {canWrite && list.actions.includes("create") && (
              <Button
                disabled={!onCreate}
                title={onCreate ? undefined : "预览：这里只是展示按钮位置"}
                onClick={onCreate}
              >
                <Plus className="size-4" />
                {createLabel || "新建"}
              </Button>
            )}
          </div>
        </CardHeader>

        {failed ? (
          <div className="p-12 text-center">
            <p className="text-sm">加载数据失败</p>
            <Button className="mt-4" variant="outline" disabled={!onReload} onClick={onReload}>
              重新加载
            </Button>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    {list.columns.map((column, index) => {
                      const target: DwFocusTarget = { kind: "column", index }
                      const active = focused(target)
                      return (
                        <TableHead
                          key={`${column.key}-${index}`}
                          {...focusProps(target, column.label || column.key)}
                          className={cn(
                            "whitespace-nowrap",
                            column.width || undefined,
                            link?.onSelect && "cursor-pointer",
                            active && "bg-primary/10 text-foreground",
                          )}
                        >
                          {link ? (
                            <span className="inline-flex items-center gap-1.5">
                              {column.label}
                              {markAt(target) && <MarkDot level={markAt(target)!} />}
                            </span>
                          ) : (
                            column.label
                          )}
                        </TableHead>
                      )
                    })}
                    {showRowMenu && (
                      <TableHead className="w-16">
                        <span className="sr-only">操作</span>
                      </TableHead>
                    )}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {loading
                    ? Array.from({ length: 6 }, (_, index) => (
                        <TableRow key={index}>
                          {list.columns.map((column) => (
                            <TableCell key={column.key}>
                              <Skeleton className="h-4 w-28" />
                            </TableCell>
                          ))}
                          {showRowMenu && <TableCell />}
                        </TableRow>
                      ))
                    : rows.map((row, rowIndex) => (
                        <TableRow key={rowKeyOf(row, list.rowKey, rowIndex)}>
                          {list.columns.map((column, index) => {
                            const target: DwFocusTarget = { kind: "column", index }
                            return (
                              <TableCell
                                key={`${column.key}-${index}`}
                                className={cn(
                                  column.format === "number" && "text-right",
                                  focused(target) && "bg-primary/[0.06]",
                                )}
                              >
                                <DwCell column={column} row={row} />
                              </TableCell>
                            )
                          })}
                          {showRowMenu && (
                            <TableCell>
                              <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                  <Button variant="ghost" size="icon" aria-label="行操作">
                                    <MoreHorizontal className="size-4" />
                                  </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="end">
                                  {list.actions.includes("edit") && (
                                    <DropdownMenuItem disabled={!onEdit} onClick={() => onEdit?.(row, rowIndex)}>
                                      <Pencil /> 编辑
                                      {editHint?.(rowIndex)}
                                    </DropdownMenuItem>
                                  )}
                                  {list.actions.includes("delete") && (
                                    <DropdownMenuItem destructive onClick={() => setRemoving(row)}>
                                      <Trash2 /> 删除
                                    </DropdownMenuItem>
                                  )}
                                  {customActions.length > 0 && builtinActions.length > 0 && (
                                    <DropdownMenuSeparator />
                                  )}
                                  {customActions.map((action, index) => (
                                    <DropdownMenuItem
                                      key={`${action.api}-${index}`}
                                      disabled={actionPending}
                                      onClick={() => triggerAction(action, row)}
                                    >
                                      <Zap /> {action.label || `按钮 ${index + 1}`}
                                    </DropdownMenuItem>
                                  ))}
                                </DropdownMenuContent>
                              </DropdownMenu>
                            </TableCell>
                          )}
                        </TableRow>
                      ))}
                </TableBody>
              </Table>
            </div>

            {emptyColumns && list.columns.length === 0 && (
              <div className="border-t p-6 text-center text-xs text-muted-foreground">{emptyColumns}</div>
            )}

            {/*
              空态与线上同源：条件、插图、文案、按钮全部一份。
              以前预览把「清除筛选」省略了，于是配置者看不到空态里其实有个出口。
            */}
            {showEmpty && (
              <div className="p-12 text-center">
                <Inbox className="mx-auto size-8 text-muted-foreground/40" />
                <p className="mt-3 text-sm font-medium">没有符合条件的数据</p>
                {activeFilterCount > 0 && (
                  <Button className="mt-4" variant="outline" disabled={!onResetFilters} onClick={onResetFilters}>
                    清除筛选
                  </Button>
                )}
              </div>
            )}

            {footnote && (
              <p className="border-t px-5 py-2 text-[10px] leading-relaxed text-muted-foreground">{footnote}</p>
            )}

            <div className="flex flex-col items-center justify-between gap-3 border-t px-5 py-3 sm:flex-row">
              <p className="text-xs text-muted-foreground">
                第 {page} / {pageCount} 页 · 共 {total} 条
              </p>
              <div className="flex items-center gap-2">
                <Select
                  value={String(pageSize)}
                  disabled={!onPageSizeChange}
                  onValueChange={(value) => onPageSizeChange?.(Number(value))}
                >
                  <SelectTrigger className="h-8 w-[110px] text-xs">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {PAGE_SIZES.map((size) => (
                      <SelectItem key={size} value={String(size)}>
                        每页 {size} 条
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={!onPageChange || page <= 1}
                  onClick={() => onPageChange?.(page - 1)}
                >
                  上一页
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={!onPageChange || page >= pageCount}
                  onClick={() => onPageChange?.(page + 1)}
                >
                  下一页
                </Button>
              </div>
            </div>
          </>
        )}
      </Card>

      {/*
        删除确认框。文案平台不编——「XX 将被删除，该操作会记录到审计日志」是产品语言，
        改口径只改这一处。
        预览没有 onDelete，所以确认键自动禁用；`confirmHint` 负责说明这件事。
      */}
      <AlertDialog open={Boolean(removing)} onOpenChange={(open) => { if (!open) setRemoving(null) }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除？</AlertDialogTitle>
            <AlertDialogDescription>
              「{rowLabel(removing, list.columns, list.rowKey)}」将被删除，该操作会记录到审计日志。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter className="sm:items-center">
            {confirmHint && <span className="text-[10px] text-muted-foreground sm:mr-auto">{confirmHint}</span>}
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction
              disabled={!onDelete}
              className="bg-destructive text-white hover:bg-destructive/90"
              onClick={(event) => {
                // 默认行为是点完就关；这里要等请求回来（失败也要收框，由 finally 负责）。
                event.preventDefault()
                void confirmRemove()
              }}
            >
              确认删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/*
        自定义行操作的提示框。标题与正文都来自配置——这件事会做什么，只有配它的人说得清；
        平台不在这里编一句「确定执行吗」，那种话既没信息量也没法追责。
        按钮不换颜色：破坏性由提示框文案表达，不给配置再加一个「红不红」的枚举。
      */}
      <AlertDialog
        open={Boolean(pendingAction)}
        onOpenChange={(open) => { if (!open) setPendingAction(null) }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{pendingAction?.action.label || "未命名按钮"}</AlertDialogTitle>
            <AlertDialogDescription>
              {pendingAction?.action.confirm?.trim()
                ? pendingAction.action.confirm
                : "没有配置提示框：线上点一下会直接执行，不再拦一次。"}
            </AlertDialogDescription>
          </AlertDialogHeader>
          {pendingAction && actionDetail?.(pendingAction.action, pendingAction.row)}
          <AlertDialogFooter className="sm:items-center">
            <span className="truncate font-mono text-[10px] text-muted-foreground sm:mr-auto">
              {pendingAction && `${pendingAction.action.api || "（未配置接口）"} · ${list.rowKey}=${String(pendingAction.row[list.rowKey])}`}
            </span>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction
              disabled={!onRowAction || actionPending}
              // 不自动关闭：失败时要能重试，也要能看清错误；成功后由 runAction 收起来。
              onClick={(event) => {
                event.preventDefault()
                if (pendingAction) void runAction(pendingAction.action, pendingAction.row)
              }}
            >
              {actionPending ? "正在执行…" : "确认执行"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}

// ---------------------------------------------------------------------------
// 私有渲染辅助
// ---------------------------------------------------------------------------

const PAGE_SIZES = [20, 50, 100]

/**
 * 一个筛选控件。**纯展示**：选项由外面解析好传进来。
 *
 * 早先它自己调 `useRemoteOptions` 取数，于是列表渲染层被迫依赖 QueryClient，
 * 配置页预览也得跟着住在 Provider 里——预览是纯静态的，不该被数据层绑住。
 * 现在远程选项在容器里用 `useQueries` 一次取齐（hook 数量固定），这里只摆。
 *
 * `allowRemote` 为 false 时（预览）下拉里多一行说明：配置了接口但预览不取数，
 * 得让配置者知道选项从哪来，而不是看到一个空下拉以为配错了。
 */
function DwFilterControl({
  filter,
  options,
  value,
  endValue,
  onChange,
  allowRemote,
}: {
  filter: DwFilter
  options: Array<{ value: string; label: string }>
  value: string
  endValue: string
  onChange?: (key: string, value: string) => void
  allowRemote: boolean
}) {
  const controlId = `dw-filter-${filter.key}`
  const disabled = !onChange

  if (filter.widget === "dateRange") {
    // 起止是两个独立筛选键，不是「一个值拆开」：数据源按命名参数读（filters.get("from")）。
    return (
      <div className="flex items-center gap-2">
        <Input
          id={controlId}
          type="date"
          className="min-w-0 flex-1"
          value={value}
          disabled={disabled}
          onChange={(event) => onChange?.(filter.key, event.target.value)}
        />
        <span className="shrink-0 text-xs text-muted-foreground">至</span>
        <Input
          type="date"
          aria-label={`${filter.label}（结束）`}
          className="min-w-0 flex-1"
          value={endValue}
          // 没配 endKey 时禁用：静默丢条件比一个灰掉的框更难查
          disabled={disabled || !filter.endKey}
          onChange={(event) => {
            if (filter.endKey) onChange?.(filter.endKey, event.target.value)
          }}
        />
      </div>
    )
  }

  if (filter.widget === "select") {
    return (
      <div className="grid gap-1">
        <Select
          value={value || "all"}
          disabled={disabled}
          onValueChange={(next) => onChange?.(filter.key, next)}
        >
          <SelectTrigger id={controlId}>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">全部</SelectItem>
            {options.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label || option.value}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        {/*
          预览不发请求，所以下拉里只有静态选项。这句必须放在下拉**外面**：
          放进去时它躺在一个 disabled 的下拉里，永远打不开，等于没写——
          配置者只会看到一个空下拉，以为选项丢了。
        */}
        {!allowRemote && filter.optionsApi && (
          <p className="text-[10px] leading-tight text-muted-foreground">
            选项来自 {filter.optionsApi}，预览不取数
          </p>
        )}
      </div>
    )
  }

  return (
    <Input
      id={controlId}
      value={value}
      placeholder={filter.placeholder || filter.label}
      disabled={disabled}
      onChange={(event) => onChange?.(filter.key, event.target.value)}
    />
  )
}

/**
 * 按「占几列」算网格宽度，不按「有几个筛选」算：日期范围要两个输入框的宽度，
 * 三个普通筛选和「两个普通 + 一个日期范围」需要的列数不是一回事。
 *
 * `embedded` 时不用视口断点：预览面板只有几百像素宽，`md:grid-cols-4` 会因为
 * 浏览器窗口宽而生效，把四个筛选挤进 480px。
 */
function filterGrid(filters: DwFilter[], embedded: boolean): string {
  const span = filters.reduce((total, filter) => total + (filter.widget === "dateRange" ? 2 : 1), 0)
  if (embedded) {
    return span >= 2 ? "grid-cols-2" : "grid-cols-1"
  }
  if (filters.length <= 1 && !filters.some((filter) => filter.widget === "dateRange")) {
    return "md:grid-cols-[1fr_auto]"
  }
  if (span <= 2) return "md:grid-cols-2"
  if (span === 3) return "md:grid-cols-3"
  return "md:grid-cols-4"
}

/** 行主键；配置错或数据脏导致取不到时退回下标，至少不会让整表 key 相同。 */
function rowKeyOf(row: DwRow, rowKey: string, index: number): string {
  const value = row[rowKey]
  return value == null ? `__row-${index}` : String(value)
}
