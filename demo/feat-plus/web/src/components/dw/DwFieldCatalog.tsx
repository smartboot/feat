import { AlertTriangle, Plus } from "lucide-react"

import type { DatasourceOption, DwFieldMeta } from "@/api/types"
import type { DwFocusTarget } from "@/components/dw/link"
import { FIELD_TYPE_LABELS, fieldUsage, type PageDraft } from "@/components/dw/meta"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { cn } from "@/lib/utils"

/**
 * 字段面板：数据源声明的字段 + 它在当前页面被用在哪。
 *
 * 这是配置页的「数据模型视图」。页面配置最容易出的错不是写错某个值，而是不知道
 * 「这个数据源到底有哪些字段、哪些可写、哪些还没用上」。把它摊成一张表以后：
 * 列 / 筛选 / 表单的入口都在这里，不用先在脑子里过一遍再翻到对应页签找。
 *
 * 未使用字段的「+」按钮只做一件事：按类型给出合理默认值后追加。配置者要改的是细节，
 * 不是从零填一遍 key 和标签。
 */
export function DwFieldCatalog({
  datasource,
  draft,
  onAdd,
  onMigrate,
  onLocate,
}: {
  datasource?: DatasourceOption
  draft: PageDraft
  onAdd: (field: DwFieldMeta, target: "column" | "filter" | "form") => void
  /** 清理当前配置里不属于新数据源的 key（迁移向导的「保留可用配置」） */
  onMigrate: () => void
  /** 「用于」里的列 / 筛选 / 表单：点一下跳到那一条，预览同步高亮 */
  onLocate?: (target: DwFocusTarget) => void
}) {
  const fields = datasource?.fields ?? []
  const usage = fieldUsage(draft.list, draft.form)
  const known = new Set(fields.map((field) => field.key))

  const staleColumns = draft.list.columns.filter((column) => column.key && !known.has(column.key))
  const staleFilters = draft.list.filters.filter((filter) => filter.key && !known.has(filter.key))
  const staleFields = draft.form.fields.filter((field) => field.key && !known.has(field.key))
  const staleCount = staleColumns.length + staleFilters.length + staleFields.length

  return (
    <div className="grid gap-4">
      {staleCount > 0 && (
        <div className="grid gap-2 rounded-lg border border-amber-500/40 bg-amber-500/5 p-3">
          <div className="flex items-start gap-2">
            <AlertTriangle className="mt-0.5 size-4 shrink-0 text-amber-600" />
            <div className="grid gap-1">
              <p className="text-sm font-medium">
                有 {staleCount} 处配置引用了当前数据源没有的字段
              </p>
              <p className="text-xs text-muted-foreground">
                通常是换过数据源，或者数据源改了字段名。保留其余配置、只清掉这些无用项即可，
                保存前必须处理，否则后端会拒绝。
              </p>
              <p className="text-[11px] text-muted-foreground">
                涉及：
                {[...staleColumns.map((item) => `列 ${item.key}`),
                  ...staleFilters.map((item) => `筛选 ${item.key}`),
                  ...staleFields.map((item) => `表单 ${item.key}`)].join("、")}
              </p>
            </div>
          </div>
          <div>
            <Button size="sm" variant="outline" className="h-7 text-xs" onClick={onMigrate}>
              清理 {staleCount} 处无效引用
            </Button>
          </div>
        </div>
      )}

      <div className="overflow-hidden rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="min-w-[160px]">字段</TableHead>
              <TableHead className="w-16">类型</TableHead>
              <TableHead className="w-16">可写</TableHead>
              <TableHead>用于</TableHead>
              <TableHead className="w-[190px]">
                <span className="sr-only">加入</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {fields.map((field) => {
              const used = usage[field.key]
              return (
                <TableRow key={field.key}>
                  <TableCell>
                    <p className="text-sm font-medium">{field.label}</p>
                    <p className="font-mono text-[10px] text-muted-foreground">{field.key}</p>
                  </TableCell>
                  <TableCell className="text-xs text-muted-foreground">
                    {FIELD_TYPE_LABELS[field.type] ?? field.type}
                  </TableCell>
                  <TableCell>
                    {field.writable ? (
                      <span className="text-xs">可写</span>
                    ) : (
                      <span className="text-xs text-muted-foreground">只读</span>
                    )}
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-wrap gap-1">
                      {!used && <span className="text-[11px] text-muted-foreground">未使用</span>}
                      {(used?.columns ?? []).map((index) => (
                        <UsageChip
                          key={`c${index}`}
                          label={`列 ${index + 1}`}
                          solid
                          target={{ kind: "column", index }}
                          onLocate={onLocate}
                        />
                      ))}
                      {(used?.filters ?? []).map((index) => (
                        <UsageChip
                          key={`f${index}`}
                          label={`筛选 ${index + 1}`}
                          target={{ kind: "filter", index }}
                          onLocate={onLocate}
                        />
                      ))}
                      {(used?.form ?? []).map((index) => (
                        <UsageChip
                          key={`m${index}`}
                          label={`表单 ${index + 1}`}
                          target={{ kind: "field", index }}
                          onLocate={onLocate}
                        />
                      ))}
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1">
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-7 gap-0.5 px-1.5 text-[11px]"
                        disabled={Boolean(used?.columns.length)}
                        onClick={() => onAdd(field, "column")}
                      >
                        <Plus className="size-3" />
                        列
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-7 gap-0.5 px-1.5 text-[11px]"
                        disabled={Boolean(used?.filters.length)}
                        onClick={() => onAdd(field, "filter")}
                      >
                        <Plus className="size-3" />
                        筛选
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-7 gap-0.5 px-1.5 text-[11px]"
                        disabled={!field.writable || Boolean(used?.form.length)}
                        title={field.writable ? undefined : "只读字段不能进表单"}
                        onClick={() => onAdd(field, "form")}
                      >
                        <Plus className="size-3" />
                        表单
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              )
            })}
            {fields.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} className="p-8 text-center text-sm text-muted-foreground">
                  选择数据源后，这里会列出它的全部字段与使用情况。
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      <p className="text-[11px] leading-relaxed text-muted-foreground">
        字段来自数据源声明（编译期注册），页面上不能新增或改名。「可写」决定它能不能进表单；
        只读字段只能出现在列与筛选里。
      </p>
    </div>
  )
}

/**
 * 「用于」里的一枚标签。以前它是纯文字，配置者看到「列 2」还得自己翻到列表分区、
 * 数到第 2 个卡片。现在点它就直接落到那一条上，右侧预览同时亮。
 */
function UsageChip({
  label,
  target,
  solid = false,
  onLocate,
}: {
  label: string
  target: DwFocusTarget
  solid?: boolean
  onLocate?: (target: DwFocusTarget) => void
}) {
  return (
    <button
      type="button"
      title={`跳到「${label}」的配置`}
      className={cn(
        "rounded-md px-1.5 text-[10px] leading-5 transition-colors",
        solid
          ? "bg-secondary text-secondary-foreground hover:bg-primary/15 hover:text-primary"
          : "border text-muted-foreground hover:border-primary/50 hover:text-primary",
      )}
      onClick={() => onLocate?.(target)}
    >
      {label}
    </button>
  )
}
