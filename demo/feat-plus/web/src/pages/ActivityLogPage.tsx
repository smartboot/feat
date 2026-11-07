import { useState } from "react"
import { CalendarDays, Filter, RotateCcw, ScrollText } from "lucide-react"

import type { ActivityItem, ActivityKind, ActivityQuery } from "@/api/types"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useActivities, useActivityOperators } from "@/services/queries"

const PAGE_SIZE = 20

/** 日志类型与后端 ActivityRecorder 写入的 kind 一一对应。 */
const KIND_OPTIONS: Array<{ value: ActivityKind; label: string; badge: "success" | "secondary" | "warning" | "destructive" | "outline" }> = [
  { value: "create", label: "新建", badge: "success" },
  { value: "update", label: "修改", badge: "secondary" },
  { value: "enable", label: "启用", badge: "success" },
  { value: "disable", label: "停用", badge: "warning" },
  { value: "password", label: "重置密码", badge: "warning" },
  { value: "delete", label: "删除", badge: "destructive" },
  { value: "system", label: "系统", badge: "outline" },
]

const emptyQuery: ActivityQuery = {
  page: 1,
  pageSize: PAGE_SIZE,
  kind: "all",
  operatorId: "all",
  from: "",
  to: "",
}

export function ActivityLogPage() {
  const [query, setQuery] = useState<ActivityQuery>(emptyQuery)
  const { data, isLoading, isFetching, isError, refetch } = useActivities(query)
  const { data: operators = [] } = useActivityOperators()

  const items: ActivityItem[] = data?.items || []
  const total = data?.total || 0
  const pageCount = Math.max(1, Math.ceil(total / query.pageSize))
  const activeFilterCount = [query.kind !== "all", query.operatorId !== "all", Boolean(query.from), Boolean(query.to)]
    .filter(Boolean).length

  function update(patch: Partial<ActivityQuery>) {
    // 改筛选条件时回到第一页，否则会停在一个不存在的页码上。
    setQuery((current) => ({ ...current, ...patch, page: patch.page ?? 1 }))
  }

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm text-muted-foreground">系统管理</p>
        <h2 className="mt-1 text-3xl font-semibold tracking-tight">审计日志</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          记录每一次写操作：谁在什么时候改了谁。日志只由系统写入，界面上不提供修改与删除。
        </p>
      </div>

      <Card>
        <CardContent className="grid gap-3 p-3 md:grid-cols-[1fr_1fr_auto_auto] md:items-end">
          <div className="grid gap-2">
            <Label htmlFor="activity-kind">操作类型</Label>
            <Select
              value={query.kind}
              onValueChange={(value) => update({ kind: value as "all" | ActivityKind })}
            >
              <SelectTrigger id="activity-kind">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">全部类型</SelectItem>
                {KIND_OPTIONS.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="grid gap-2">
            <Label htmlFor="activity-operator">操作人</Label>
            <Select
              value={query.operatorId === "all" ? "all" : String(query.operatorId)}
              onValueChange={(value) => update({ operatorId: value === "all" ? "all" : Number(value) })}
            >
              <SelectTrigger id="activity-operator">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">全部操作人</SelectItem>
                {operators.map((operator) => (
                  <SelectItem key={operator.id} value={String(operator.id)}>
                    {operator.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="grid gap-2">
            <Label htmlFor="activity-from">开始日期</Label>
            <Input
              id="activity-from"
              type="date"
              value={query.from}
              max={query.to || undefined}
              onChange={(event) => update({ from: event.target.value })}
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="activity-to">结束日期</Label>
            <Input
              id="activity-to"
              type="date"
              value={query.to}
              min={query.from || undefined}
              onChange={(event) => update({ to: event.target.value })}
            />
          </div>
        </CardContent>
      </Card>

      <Card className="overflow-hidden">
        <CardHeader className="flex-row items-center justify-between space-y-0 border-b px-5 py-4">
          <div>
            <CardTitle className="text-base">操作记录</CardTitle>
            <p className="mt-1 text-xs text-muted-foreground">
              共 {total} 条{isFetching && <span className="ml-2 text-primary">正在更新...</span>}
            </p>
          </div>
          <div className="flex items-center gap-2">
            {activeFilterCount > 0 && (
              <Button variant="ghost" size="sm" className="h-8 gap-2 text-xs" onClick={() => setQuery(emptyQuery)}>
                <RotateCcw className="size-3.5" />
                清除筛选
              </Button>
            )}
            <Badge variant="outline" className="gap-1">
              <Filter className="size-3" />
              {activeFilterCount > 0 ? `${activeFilterCount} 个条件` : "全部记录"}
            </Badge>
          </div>
        </CardHeader>

        {isError ? (
          <div className="p-12 text-center">
            <p className="text-sm">加载审计日志失败</p>
            <Button className="mt-4" variant="outline" onClick={() => refetch()}>
              重新加载
            </Button>
          </div>
        ) : (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[150px]">时间</TableHead>
                  <TableHead className="w-[110px]">类型</TableHead>
                  <TableHead className="w-[140px]">操作人</TableHead>
                  <TableHead>内容</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {isLoading
                  ? Array.from({ length: 6 }, (_, index) => (
                      <TableRow key={index}>
                        <TableCell><Skeleton className="h-3 w-28" /></TableCell>
                        <TableCell><Skeleton className="h-5 w-16" /></TableCell>
                        <TableCell><Skeleton className="h-3 w-20" /></TableCell>
                        <TableCell>
                          <div className="space-y-2">
                            <Skeleton className="h-3 w-56" />
                            <Skeleton className="h-3 w-72" />
                          </div>
                        </TableCell>
                      </TableRow>
                    ))
                  : items.map((item) => (
                      <TableRow key={item.id}>
                        <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                          {item.createdTime}
                        </TableCell>
                        <TableCell>
                          <Badge variant={kindMeta(item.kind).badge}>{kindMeta(item.kind).label}</Badge>
                        </TableCell>
                        <TableCell className="whitespace-nowrap text-sm">
                          {item.operatorName || <span className="text-muted-foreground">系统</span>}
                        </TableCell>
                        <TableCell>
                          <strong className="text-sm font-medium">{item.title}</strong>
                          <p className="mt-1 text-xs text-muted-foreground">{item.description}</p>
                        </TableCell>
                      </TableRow>
                    ))}
              </TableBody>
            </Table>

            {!isLoading && items.length === 0 && (
              <div className="p-12 text-center">
                <ScrollText className="mx-auto size-8 text-muted-foreground/40" />
                <p className="mt-3 text-sm font-medium">没有符合条件的日志</p>
                <p className="mt-1 text-xs text-muted-foreground">放宽时间范围或换一个操作类型再试。</p>
                {activeFilterCount > 0 && (
                  <Button className="mt-4" variant="outline" onClick={() => setQuery(emptyQuery)}>
                    清除筛选
                  </Button>
                )}
              </div>
            )}

            <div className="flex flex-col items-center justify-between gap-3 border-t px-5 py-3 sm:flex-row">
              <p className="flex items-center gap-1 text-xs text-muted-foreground">
                <CalendarDays className="size-3.5" />第 {query.page} / {pageCount} 页 · 共 {total} 条
              </p>
              <div className="flex items-center gap-2">
                <Select
                  value={String(query.pageSize)}
                  onValueChange={(value) => update({ pageSize: Number(value), page: 1 })}
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
                <Button
                  variant="outline"
                  size="sm"
                  disabled={query.page <= 1}
                  onClick={() => setQuery((current) => ({ ...current, page: current.page - 1 }))}
                >
                  上一页
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={query.page >= pageCount}
                  onClick={() => setQuery((current) => ({ ...current, page: current.page + 1 }))}
                >
                  下一页
                </Button>
              </div>
            </div>
          </>
        )}
      </Card>
    </div>
  )
}

/** 未知类型（后端未来新增）也不至于渲染成空白。 */
function kindMeta(kind: ActivityKind) {
  return KIND_OPTIONS.find((option) => option.value === kind) || { label: kind, badge: "outline" as const }
}
