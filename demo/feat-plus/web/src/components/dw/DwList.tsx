import { useState } from "react"
import { Filter, Inbox, MoreHorizontal, Pencil, Plus, RotateCcw, Trash2 } from "lucide-react"
import { toast } from "sonner"

import type { DwColumn, DwRow, PageConfig } from "@/api/types"
import { DwCell } from "@/components/dw/DwCell"
import { DwFormDialog } from "@/components/dw/DwFormDialog"
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
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useDwData, useDwMutations } from "@/services/queries"
import { cn } from "@/lib/utils"

/**
 * 通用动态列表渲染器：列 / 筛选 / 分页 / 行操作全部来自 PageConfig，
 * 组件本身不感知任何具体业务。交互质感以手写页（RoleListPage / ActivityLogPage）为母版。
 * <p>
 * 编辑回填直接用列表行数据：PageDataSource 的契约是行数据自带全部声明字段，
 * 不需要为打开表单再发一次单行请求。
 */
export function DwList({ pageKey, config }: { pageKey: string; config: PageConfig }) {
  const list = config.list
  const canWrite = config.canWrite
  const rowActions = list.actions.filter((action) => action === "edit" || action === "delete")
  const showRowMenu = canWrite && rowActions.length > 0

  // 筛选草稿：input 存空串、select 存 "all"，发起查询前统一清掉这两种「未设置」态。
  const [filters, setFilters] = useState<Record<string, string>>(() => initialFilters(list.filters))
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(list.pageSize)

  const query = {
    page,
    pageSize,
    filters: activeFilters(filters, list.filters),
  }
  // dataApi 通道覆盖：配置里指定了自定义接口就走它，留空的项回落内置 /api/dw 通道。
  const dataApi = list.dataApi
  const { data, isLoading, isFetching, isError, refetch } = useDwData(pageKey, query, true, dataApi)
  const mutations = useDwMutations(pageKey, dataApi)

  const rows: DwRow[] = data?.items || []
  const total = data?.total || 0
  const pageCount = Math.max(1, Math.ceil(total / pageSize))
  const activeFilterCount = list.filters.filter((filter) => {
    const value = filters[filter.key] ?? ""
    return value !== "" && value !== "all"
  }).length

  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<DwRow | null>(null)
  const [removing, setRemoving] = useState<DwRow | null>(null)

  function resetFilters() {
    setFilters(initialFilters(list.filters))
    setPage(1)
  }

  function updateFilter(key: string, value: string) {
    // 改筛选回到第一页，否则会停在一个不存在的页码上。
    setFilters((current) => ({ ...current, [key]: value }))
    setPage(1)
  }

  function openCreate() {
    setEditing(null)
    setDialogOpen(true)
  }

  function openEdit(row: DwRow) {
    setEditing(row)
    setDialogOpen(true)
  }

  async function submit(values: Record<string, string>) {
    try {
      await mutations.save.mutateAsync({
        id: editing ? rowId(editing, list.rowKey) : null,
        values,
      })
      toast.success(editing ? "已保存" : "已创建")
      setDialogOpen(false)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "操作失败")
    }
  }

  async function confirmRemove() {
    if (!removing) return
    try {
      await mutations.remove.mutateAsync(rowId(removing, list.rowKey))
      toast.success("已删除")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "操作失败")
    } finally {
      setRemoving(null)
    }
  }

  return (
    <div className="space-y-6">
      {list.filters.length > 0 && (
        <Card>
          <CardContent className={cn("grid gap-3 p-3 md:items-end", filterGrid(list.filters.length))}>
            {list.filters.map((filter) => (
              <div key={filter.key} className="grid gap-2">
                <Label htmlFor={`dw-filter-${filter.key}`}>{filter.label}</Label>
                {filter.widget === "select" ? (
                  <Select
                    value={filters[filter.key] ?? "all"}
                    onValueChange={(value) => updateFilter(filter.key, value)}
                  >
                    <SelectTrigger id={`dw-filter-${filter.key}`}>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">全部</SelectItem>
                      {(filter.options ?? []).map((option) => (
                        <SelectItem key={option.value} value={option.value}>
                          {option.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                ) : (
                  <Input
                    id={`dw-filter-${filter.key}`}
                    value={filters[filter.key] ?? ""}
                    placeholder={filter.placeholder || filter.label}
                    onChange={(event) => updateFilter(filter.key, event.target.value)}
                  />
                )}
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      <Card className="overflow-hidden">
        <CardHeader className="flex-row items-center justify-between space-y-0 border-b px-5 py-4">
          <div>
            <CardTitle className="text-base">{config.title}</CardTitle>
            <p className="mt-1 text-xs text-muted-foreground">
              共 {total} 条{isFetching && <span className="ml-2 text-primary">正在更新...</span>}
            </p>
          </div>
          <div className="flex items-center gap-2">
            {activeFilterCount > 0 && (
              <Button variant="ghost" size="sm" className="h-8 gap-2 text-xs" onClick={resetFilters}>
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
              <Button variant="outline" size="sm" onClick={() => refetch()}>
                刷新
              </Button>
            )}
            {canWrite && list.actions.includes("create") && (
              <Button onClick={openCreate}>
                <Plus className="size-4" />
                {config.form.createTitle}
              </Button>
            )}
          </div>
        </CardHeader>

        {isError ? (
          <div className="p-12 text-center">
            <p className="text-sm">加载数据失败</p>
            <Button className="mt-4" variant="outline" onClick={() => refetch()}>
              重新加载
            </Button>
          </div>
        ) : (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  {list.columns.map((column) => (
                    <TableHead key={column.key} className={cn("whitespace-nowrap", column.width || undefined)}>
                      {column.label}
                    </TableHead>
                  ))}
                  {showRowMenu && (
                    <TableHead className="w-16">
                      <span className="sr-only">操作</span>
                    </TableHead>
                  )}
                </TableRow>
              </TableHeader>
              <TableBody>
                {isLoading
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
                  : rows.map((row) => (
                      <TableRow key={String(row[list.rowKey])}>
                        {list.columns.map((column) => (
                          <TableCell key={column.key} className={cn(column.format === "number" && "text-right")}>
                            <DwCell column={column} row={row} />
                          </TableCell>
                        ))}
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
                                  <DropdownMenuItem onClick={() => openEdit(row)}>
                                    <Pencil /> 编辑
                                  </DropdownMenuItem>
                                )}
                                {list.actions.includes("delete") && (
                                  <DropdownMenuItem destructive onClick={() => setRemoving(row)}>
                                    <Trash2 /> 删除
                                  </DropdownMenuItem>
                                )}
                              </DropdownMenuContent>
                            </DropdownMenu>
                          </TableCell>
                        )}
                      </TableRow>
                    ))}
              </TableBody>
            </Table>

            {!isLoading && rows.length === 0 && (
              <div className="p-12 text-center">
                <Inbox className="mx-auto size-8 text-muted-foreground/40" />
                <p className="mt-3 text-sm font-medium">没有符合条件的数据</p>
                {activeFilterCount > 0 && (
                  <Button className="mt-4" variant="outline" onClick={resetFilters}>
                    清除筛选
                  </Button>
                )}
              </div>
            )}

            <div className="flex flex-col items-center justify-between gap-3 border-t px-5 py-3 sm:flex-row">
              <p className="text-xs text-muted-foreground">
                第 {page} / {pageCount} 页 · 共 {total} 条
              </p>
              <div className="flex items-center gap-2">
                <Select
                  value={String(pageSize)}
                  onValueChange={(value) => {
                    setPageSize(Number(value))
                    setPage(1)
                  }}
                >
                  <SelectTrigger className="h-8 w-[110px] text-xs">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {[20, 50, 100].map((size) => (
                      <SelectItem key={size} value={String(size)}>
                        每页 {size} 条
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Button variant="outline" size="sm" disabled={page <= 1} onClick={() => setPage((p) => p - 1)}>
                  上一页
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page >= pageCount}
                  onClick={() => setPage((p) => p + 1)}
                >
                  下一页
                </Button>
              </div>
            </div>
          </>
        )}
      </Card>

      <DwFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        form={config.form}
        row={editing}
        onSubmit={submit}
        pending={mutations.save.isPending}
      />

      <AlertDialog open={Boolean(removing)} onOpenChange={(open) => { if (!open) setRemoving(null) }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除？</AlertDialogTitle>
            <AlertDialogDescription>
              「{rowLabel(removing, list.columns, list.rowKey)}」将被删除，该操作会记录到审计日志。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction className="bg-destructive text-white hover:bg-destructive/90" onClick={confirmRemove}>
              确认删除
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

function initialFilters(filters: Array<{ key: string; widget: string }>): Record<string, string> {
  const result: Record<string, string> = {}
  for (const filter of filters) {
    result[filter.key] = filter.widget === "select" ? "all" : ""
  }
  return result
}

/** 发起查询前清掉「未设置」态：input 的空串、select 的 all。 */
function activeFilters(filters: Record<string, string>, definitions: Array<{ key: string; widget: string }>) {
  const result: Record<string, string> = {}
  for (const definition of definitions) {
    const value = (filters[definition.key] ?? "").trim()
    if (value !== "" && value !== "all") {
      result[definition.key] = value
    }
  }
  return result
}

function filterGrid(count: number): string {
  if (count <= 1) return "md:grid-cols-[1fr_auto]"
  if (count === 2) return "md:grid-cols-2"
  return "md:grid-cols-3"
}

function rowId(row: DwRow, rowKey: string): number {
  return Number(row[rowKey])
}

/**
 * 删除确认框里的行标识：优先用第一列的文本值（配置者认为那是这行的「名字」），
 * 退化成 row.name，最后退化成主键。写死 name 字段会让换了数据源的页面全都显示 #12。
 */
function rowLabel(row: DwRow | null, columns: DwColumn[], rowKey: string): string {
  if (!row) return ""
  for (const column of columns) {
    if (column.format === "text" || column.format === "muted") {
      const value = row[column.key]
      if (typeof value === "string" && value) return value
    }
  }
  const name = row.name
  if (typeof name === "string" && name) return name
  return `#${String(row[rowKey])}`
}

