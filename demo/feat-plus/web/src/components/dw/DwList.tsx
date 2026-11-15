import { useState } from "react"
import { toast } from "sonner"

import type { DwFilter, DwFormValue, DwRow, DwRowAction, PageConfig } from "@/api/types"
import { DwFormDialog } from "@/components/dw/DwFormDialog"
import { DwListView } from "@/components/dw/DwListView"
import { useDwData, useDwMutations, useFilterOptions } from "@/services/queries"

/**
 * 动态列表页的容器。
 *
 * 它只做四件事：取数、持有查询状态（筛选 / 分页 / 弹窗）、把写操作接到 mutation、
 * 把结果交给 {@link DwListView}。**不渲染任何表格结构**——那部分与配置页预览共用同一个
 * 组件，所以「线上长这样」和「预览长这样」在结构上不可能分叉。
 *
 * 编辑回填直接用列表行数据：PageDataSource 的契约是行数据自带全部声明字段，
 * 不需要为打开表单再发一次单行请求。
 */
export function DwList({ pageKey, config }: { pageKey: string; config: PageConfig }) {
  const list = config.list

  // 筛选草稿：input 存空串、select 存 "all"，发起查询前统一清掉这两种「未设置」态。
  const [filters, setFilters] = useState<Record<string, string>>(() => initialFilters(list.filters))
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(list.pageSize)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<DwRow | null>(null)

  const query = {
    page,
    pageSize,
    filters: activeFilters(filters, list.filters),
  }
  // dataApi 通道覆盖：配置里指定了自定义接口就走它，留空的项回落内置 /api/dw 通道。
  const dataApi = list.dataApi
  const { data, isLoading, isFetching, isError, refetch } = useDwData(pageKey, query, true, dataApi)
  const mutations = useDwMutations(pageKey, dataApi)
  // 每个筛选一个 query 配置、一次 hook 取齐：hook 数量固定，筛选数量变化不会打乱顺序。
  const filterOptions = useFilterOptions(list.filters)

  const activeFilterCount = countActiveFilters(filters, list.filters)

  function resetFilters() {
    setFilters(initialFilters(list.filters))
    setPage(1)
  }

  function updateFilter(key: string, value: string) {
    // 改筛选回到第一页，否则会停在一个不存在的页码上。
    setFilters((current) => ({ ...current, [key]: value }))
    setPage(1)
  }

  function changePageSize(size: number) {
    setPageSize(size)
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

  async function submit(values: Record<string, DwFormValue>) {
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

  /** 确认框已经拦过一道，这里只管执行。失败要抛出，让确认框留着可重试。 */
  async function removeRow(row: DwRow) {
    try {
      await mutations.remove.mutateAsync(rowId(row, list.rowKey))
      toast.success("已删除")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "操作失败")
      throw error
    }
  }

  /**
   * 自定义行操作：POST 配置里的接口，请求体只带主键。
   * 成功后用它自己的 message 提示——「已下发一次性凭证」比「操作成功」有用得多；
   * 没配 message 才退回按钮名。
   */
  async function runAction(action: DwRowAction, row: DwRow) {
    try {
      const message = await mutations.action.mutateAsync({
        api: action.api,
        rowKey: list.rowKey,
        id: rowId(row, list.rowKey),
      })
      toast.success(message || `已执行「${action.label}」`)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "操作失败")
      throw error
    }
  }

  return (
    <>
      <DwListView
        list={list}
        title={config.title}
        createLabel={config.form.createTitle}
        canWrite={config.canWrite}
        rows={data?.items || []}
        total={data?.total || 0}
        page={page}
        pageSize={pageSize}
        loading={isLoading}
        failed={isError}
        refreshing={isFetching && !isLoading}
        onReload={() => void refetch()}
        filterValues={filters}
        activeFilterCount={activeFilterCount}
        onFilterChange={updateFilter}
        onResetFilters={resetFilters}
        // 配了 optionsApi 的用远程结果，否则用配置里的静态选项——两处都归一到 {value,label}
        optionsFor={(filter, index) =>
          filter.optionsApi ? (filterOptions[index]?.data ?? []) : (filter.options ?? [])
        }
        onPageChange={setPage}
        onPageSizeChange={changePageSize}
        onCreate={openCreate}
        onRefresh={() => void refetch()}
        onEdit={openEdit}
        onDelete={removeRow}
        onRowAction={runAction}
        actionPending={mutations.action.isPending}
      />

      <DwFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        form={config.form}
        row={editing}
        onSubmit={submit}
        pending={mutations.save.isPending}
      />
    </>
  )
}

// ---------------------------------------------------------------------------
// 查询参数：筛选草稿 ↔ 请求条件
// ---------------------------------------------------------------------------

function initialFilters(filters: DwFilter[]): Record<string, string> {
  const result: Record<string, string> = {}
  for (const filter of filters) {
    result[filter.key] = filter.widget === "select" ? "all" : ""
    // 日期范围的结束键不是独立筛选，不会出现在 filters 数组里，所以单独初始化。
    if (filter.widget === "dateRange" && filter.endKey) {
      result[filter.endKey] = ""
    }
  }
  return result
}

/** 发起查询前清掉「未设置」态：input 的空串、select 的 all；日期范围两端各自判断。 */
function activeFilters(filters: Record<string, string>, definitions: DwFilter[]) {
  const result: Record<string, string> = {}
  for (const definition of definitions) {
    if (definition.widget === "dateRange") {
      const from = (filters[definition.key] ?? "").trim()
      if (from !== "") result[definition.key] = from
      if (definition.endKey) {
        const to = (filters[definition.endKey] ?? "").trim()
        if (to !== "") result[definition.endKey] = to
      }
      continue
    }
    const value = (filters[definition.key] ?? "").trim()
    if (value !== "" && value !== "all") {
      result[definition.key] = value
    }
  }
  return result
}

/** 「几个条件」按筛选条目算，不按键算：日期范围填了两端仍是一个条件。 */
function countActiveFilters(filters: Record<string, string>, definitions: DwFilter[]) {
  let count = 0
  for (const definition of definitions) {
    if (definition.widget === "dateRange") {
      const from = (filters[definition.key] ?? "").trim()
      const to = definition.endKey ? (filters[definition.endKey] ?? "").trim() : ""
      if (from !== "" || to !== "") count++
      continue
    }
    const value = filters[definition.key] ?? ""
    if (value !== "" && value !== "all") count++
  }
  return count
}

function rowId(row: DwRow, rowKey: string): number {
  return Number(row[rowKey])
}
