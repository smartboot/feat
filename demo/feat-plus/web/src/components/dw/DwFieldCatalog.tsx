import { Fragment, useState } from "react"
import { AlertTriangle, ChevronDown, Plus } from "lucide-react"

import type { DatasourceOption, DwFieldMeta, DwOption } from "@/api/types"
import { DwOptionListEditor } from "@/components/dw/DwOptionListEditor"
import type { DwFocusTarget } from "@/components/dw/link"
import { FIELD_TYPE_LABELS, fieldUsage, type PageDraft } from "@/components/dw/meta"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { cn } from "@/lib/utils"

/**
 * 字段面板：数据源声明的字段 + 它在当前页面被用在哪 + 它的值域。
 *
 * 这是配置页的「数据模型视图」。页面配置最容易出的错不是写错某个值，而是不知道
 * 「这个数据源到底有哪些字段、哪些可写、哪些还没用上」。把它摊成一张表以后：
 * 列 / 筛选 / 表单的入口都在这里，不用先在脑子里过一遍再翻到对应页签找。
 *
 * 未使用字段的「+」按钮只做一件事：按类型给出合理默认值后追加。配置者要改的是细节，
 * 不是从零填一遍 key 和标签。
 *
 * 「值域」这一列是字段自己的属性，不是某个视图的属性：同一份 status 映射要同时喂给
 * 列表彩标、筛选下拉、表单下拉，配在视图上就必然漂移。所以它在这一行，由整页共享。
 */
export function DwFieldCatalog({
  datasource,
  draft,
  fieldMaps,
  onChangeFieldMap,
  onAdd,
  onMigrate,
  onLocate,
}: {
  datasource?: DatasourceOption
  draft: PageDraft
  /** 本页共享的字段值域，存在 draft.list.fieldMaps 上 */
  fieldMaps: Record<string, DwOption[]>
  /** 传空数组表示删掉这一项——留着空值域会在保存时被后端拒绝 */
  onChangeFieldMap: (fieldKey: string, options: DwOption[]) => void
  onAdd: (field: DwFieldMeta, target: "column" | "filter" | "form") => void
  /** 清理当前配置里不属于新数据源的 key（迁移向导的「保留可用配置」） */
  onMigrate: () => void
  /** 「用于」里的列 / 筛选 / 表单：点一下跳到那一条，预览同步高亮 */
  onLocate?: (target: DwFocusTarget) => void
}) {
  const fields = datasource?.fields ?? []
  const usage = fieldUsage(draft.list, draft.form)
  const known = new Set(fields.map((field) => field.key))
  /** 同一时刻只展开一个值域编辑器：多个同时摊开就回到「整页都是输入框」 */
  const [openMap, setOpenMap] = useState<string | null>(null)

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
              <TableHead className="w-14">类型</TableHead>
              <TableHead className="w-14">可写</TableHead>
              <TableHead className="w-20">值域</TableHead>
              <TableHead>用于</TableHead>
              <TableHead className="w-[190px]">
                <span className="sr-only">加入</span>
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {fields.map((field) => {
              const used = usage[field.key]
              const map = fieldMaps[field.key] ?? []
              const expanded = openMap === field.key
              return (
                <Fragment key={field.key}>
                  <TableRow>
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
                    {
                      /* 值域是字段自己的属性：没配也留一个可点的「—」，
                         否则「这个字段没有值域」和「这个功能不存在」长得一模一样。 */
                    }
                    <button
                      type="button"
                      id={`field-map-${field.key}`}
                      aria-label={`${field.label} 的值域`}
                      aria-expanded={expanded}
                      title="值 → 文案映射；列表、筛选、表单下拉共用这一份"
                      className={cn(
                        "flex h-6 w-full min-w-0 items-center gap-1 rounded-md border px-1.5 text-[11px] transition-colors",
                        map.length > 0
                          ? "border-primary/40 bg-primary/5 text-foreground"
                          : "text-muted-foreground hover:bg-muted",
                      )}
                      onClick={() => setOpenMap(expanded ? null : field.key)}
                    >
                      <span className="min-w-0 flex-1 truncate text-left">
                        {map.length > 0 ? `${map.length} 项` : "—"}
                      </span>
                      <ChevronDown
                        className={cn("size-3 shrink-0 text-muted-foreground/60 transition-transform", expanded && "rotate-180")}
                      />
                    </button>
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
                {expanded && (
                  <TableRow>
                    <TableCell colSpan={6} className="bg-muted/20 px-3 py-2.5">
                      <div className="grid gap-2">
                        <p className="text-[11px] leading-relaxed text-muted-foreground">
                          这份值域本页共用：列表单元格、筛选下拉、表单下拉都从这里取。
                          某一处要不同说法时，在那里的映射里单独配——那会覆盖这里，而不是重复。
                        </p>
                        <DwOptionListEditor
                          options={map}
                          onChange={(options) => onChangeFieldMap(field.key, options)}
                        />
                        <p className="text-[11px] text-muted-foreground">
                          删到一条不剩，这项值域会自动移除；留着空值域保存时会被拒绝。
                        </p>
                      </div>
                    </TableCell>
                  </TableRow>
                )}
                </Fragment>
              )
            })}
            {fields.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="p-8 text-center text-sm text-muted-foreground">
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
