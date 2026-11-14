import { useEffect, useState } from "react"
import { FileWarning, Pencil, Plus, SlidersHorizontal, Trash2 } from "lucide-react"
import { toast } from "sonner"

import type { DatasourceOption, PageAdminItem, PageInput } from "@/api/types"
import { useSession } from "@/auth/session"
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
import { Card, CardContent } from "@/components/ui/card"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Textarea } from "@/components/ui/textarea"
import { hasPermission } from "@/lib/permissions"
import { useDatasources, usePageAdmin, usePageMutations } from "@/services/queries"

const emptyDraft: PageInput = {
  pageKey: "",
  title: "",
  description: "",
  datasourceKey: "",
  listJson: "",
  formJson: "",
}

/**
 * 动态页面配置管理页：第一阶段的配置编辑形态是「结构化头部 + JSON 文本区」。
 * <p>
 * JSON 本地只做「是不是合法 JSON」的预校验，字段级规则（命中数据源字段、控件枚举、
 * 动作与可写能力一致）在后端 PageService——存进去的一定能渲染，这层保证不能放前端。
 */
export function PageManagePage() {
  const session = useSession()
  const canRead = hasPermission(session, "page:read")
  const canWrite = hasPermission(session, "page:write")
  const { data: pages = [], isLoading, isError, refetch } = usePageAdmin(canRead)
  const { data: datasources = [] } = useDatasources(canRead)
  const mutations = usePageMutations()

  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<PageAdminItem | null>(null)
  const [removing, setRemoving] = useState<PageAdminItem | null>(null)

  if (!canRead) {
    return (
      <div className="space-y-6">
        <div>
          <p className="text-sm text-muted-foreground">系统管理</p>
          <h2 className="mt-1 text-3xl font-semibold tracking-tight">页面配置</h2>
        </div>
        <Card>
          <CardContent className="p-12 text-center">
            <p className="text-sm">当前账号没有「查看页面配置」权限</p>
            <p className="mt-1 text-xs text-muted-foreground">需要管理员在角色管理里授予 page:read 权限点。</p>
          </CardContent>
        </Card>
      </div>
    )
  }

  function openCreate() {
    setEditing(null)
    setDialogOpen(true)
  }

  function openEdit(page: PageAdminItem) {
    setEditing(page)
    setDialogOpen(true)
  }

  async function submit(input: PageInput) {
    try {
      if (editing) {
        await mutations.update.mutateAsync({ id: editing.id, input })
        toast.success("页面配置已更新")
      } else {
        await mutations.create.mutateAsync(input)
        toast.success("页面配置已创建")
      }
      setDialogOpen(false)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "操作失败")
    }
  }

  async function confirmRemove() {
    if (!removing) return
    try {
      await mutations.remove.mutateAsync(removing.id)
      toast.success("页面配置已删除")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "操作失败")
    } finally {
      setRemoving(null)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="text-sm text-muted-foreground">系统管理</p>
          <h2 className="mt-1 text-3xl font-semibold tracking-tight">页面配置</h2>
          <p className="mt-2 text-sm text-muted-foreground">
            配置驱动的动态页面：列、筛选与表单存数据库，改完即生效，不用发版。
          </p>
        </div>
        {canWrite && (
          <Button onClick={openCreate}>
            <Plus className="size-4" />新建页面配置
          </Button>
        )}
      </div>

      <Card className="overflow-hidden">
        {isError ? (
          <div className="p-12 text-center">
            <p className="text-sm">加载页面配置失败</p>
            <Button className="mt-4" variant="outline" onClick={() => refetch()}>
              重新加载
            </Button>
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[140px]">页面</TableHead>
                <TableHead>标识</TableHead>
                <TableHead className="w-36">数据源</TableHead>
                <TableHead className="w-44">更新时间</TableHead>
                <TableHead className="w-16">
                  <span className="sr-only">操作</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading
                ? Array.from({ length: 3 }, (_, index) => (
                    <TableRow key={index}>
                      <TableCell><Skeleton className="h-4 w-32" /></TableCell>
                      <TableCell><Skeleton className="h-4 w-24" /></TableCell>
                      <TableCell><Skeleton className="h-4 w-16" /></TableCell>
                      <TableCell><Skeleton className="h-4 w-28" /></TableCell>
                      <TableCell />
                    </TableRow>
                  ))
                : pages.map((page) => (
                    <TableRow key={page.id}>
                      <TableCell>
                        <strong className="text-sm">{page.title}</strong>
                        {page.description && (
                          <p className="mt-1 max-w-md truncate text-xs text-muted-foreground">{page.description}</p>
                        )}
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline" className="font-mono text-[10px]">/p/{page.pageKey}</Badge>
                      </TableCell>
                      <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                        {datasourceLabel(datasources, page.datasourceKey)}
                      </TableCell>
                      <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                        {page.editTime}
                      </TableCell>
                      <TableCell>
                        {canWrite && (
                          <div className="flex items-center gap-1">
                            <Button variant="ghost" size="icon" aria-label={`编辑 ${page.title}`} onClick={() => openEdit(page)}>
                              <Pencil className="size-4" />
                            </Button>
                            <Button variant="ghost" size="icon" aria-label={`删除 ${page.title}`} onClick={() => setRemoving(page)}>
                              <Trash2 className="size-4" />
                            </Button>
                          </div>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
              {!isLoading && pages.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} className="p-12 text-center">
                    <SlidersHorizontal className="mx-auto size-8 text-muted-foreground/40" />
                    <p className="mt-3 text-sm font-medium">还没有页面配置</p>
                    <p className="mt-1 text-xs text-muted-foreground">新建一条配置，再挂一条 /p/ 菜单即可上线动态页面。</p>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Card>

      <PageFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        editing={editing}
        datasources={datasources}
        onSubmit={submit}
        pending={mutations.create.isPending || mutations.update.isPending}
      />

      <AlertDialog open={Boolean(removing)} onOpenChange={(open) => { if (!open) setRemoving(null) }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除页面配置？</AlertDialogTitle>
            <AlertDialogDescription>
              「{removing?.title}」删除后 /p/{removing?.pageKey} 将不可访问；
              指向它的菜单会保留，需要到菜单管理里另行处理。
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

function PageFormDialog({
  open,
  onOpenChange,
  editing,
  datasources,
  onSubmit,
  pending,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  editing: PageAdminItem | null
  datasources: DatasourceOption[]
  onSubmit: (input: PageInput) => Promise<void>
  pending: boolean
}) {
  const [draft, setDraft] = useState<PageInput>(emptyDraft)
  const [error, setError] = useState("")

  useEffect(() => {
    if (!open) return
    setDraft(
      editing
        ? {
            pageKey: editing.pageKey,
            title: editing.title,
            description: editing.description,
            datasourceKey: editing.datasourceKey,
            listJson: editing.listJson,
            formJson: editing.formJson,
          }
        : { ...emptyDraft },
    )
    setError("")
  }, [open, editing])

  const selected = datasources.find((ds) => ds.key === draft.datasourceKey)

  function update(patch: Partial<PageInput>) {
    setDraft((current) => ({ ...current, ...patch }))
  }

  async function submit() {
    if (!draft.pageKey.trim()) { setError("请填写页面标识"); return }
    if (!/^[a-z0-9_-]{1,32}$/.test(draft.pageKey.trim())) {
      setError("页面标识只能包含小写字母、数字、下划线和短横线")
      return
    }
    if (!draft.title.trim()) { setError("请填写页面标题"); return }
    if (!draft.datasourceKey) { setError("请选择数据源"); return }
    for (const [what, json] of [["列表配置", draft.listJson], ["表单配置", draft.formJson]] as const) {
      if (!json.trim()) { setError(`请填写${what} JSON`); return }
      try {
        JSON.parse(json)
      } catch {
        setError(`${what}不是合法的 JSON`)
        return
      }
    }
    setError("")
    await onSubmit({ ...draft, pageKey: draft.pageKey.trim(), title: draft.title.trim() })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{editing ? "编辑页面配置" : "新建页面配置"}</DialogTitle>
          <DialogDescription>
            {editing
              ? "页面标识创建后不可修改（菜单以 /p/{标识} 引用它）。字段级校验在后端：列与表单字段必须命中数据源声明的字段。"
              : "保存后挂一条 /p/{标识} 的菜单即可上线。字段级校验在后端：列与表单字段必须命中数据源声明的字段。"}
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 py-2">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="grid gap-2">
              <Label htmlFor="page-key">页面标识 *</Label>
              <Input
                id="page-key"
                value={draft.pageKey}
                disabled={Boolean(editing)}
                placeholder="roles"
                onChange={(event) => update({ pageKey: event.target.value })}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="page-title">页面标题 *</Label>
              <Input id="page-title" value={draft.title} onChange={(event) => update({ title: event.target.value })} />
            </div>
          </div>

          <div className="grid gap-2">
            <Label htmlFor="page-desc">说明</Label>
            <Input
              id="page-desc"
              value={draft.description}
              placeholder="这个页面做什么用"
              onChange={(event) => update({ description: event.target.value })}
            />
          </div>

          <div className="grid gap-2">
            <Label htmlFor="page-datasource">数据源 *</Label>
            <Select value={draft.datasourceKey} onValueChange={(value) => update({ datasourceKey: value })}>
              <SelectTrigger id="page-datasource">
                <SelectValue placeholder="选择一个数据源" />
              </SelectTrigger>
              <SelectContent>
                {datasources.map((ds) => (
                  <SelectItem key={ds.key} value={ds.key}>
                    {ds.label}（{ds.key}）
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {selected && (
              <div className="flex flex-wrap gap-1 rounded-md border bg-muted/40 p-2">
                <span className="w-full text-xs text-muted-foreground">
                  可用字段（只读字段不能配进表单）：
                </span>
                {selected.fields.map((field) => (
                  <Badge key={field.key} variant={field.writable ? "secondary" : "outline"} className="text-[10px]">
                    {field.key}
                  </Badge>
                ))}
              </div>
            )}
          </div>

          <div className="grid gap-2">
            <div className="flex items-center gap-1">
              <Label htmlFor="page-list-json">列表配置 JSON *</Label>
              <FileWarning className="size-3 text-muted-foreground" />
            </div>
            <Textarea
              id="page-list-json"
              value={draft.listJson}
              className="min-h-[140px] font-mono text-xs"
              placeholder='{"rowKey":"id","pageSize":20,"actions":["create","edit"],"filters":[],"columns":[]}'
              onChange={(event) => update({ listJson: event.target.value })}
            />
          </div>

          <div className="grid gap-2">
            <Label htmlFor="page-form-json">表单配置 JSON *</Label>
            <Textarea
              id="page-form-json"
              value={draft.formJson}
              className="min-h-[140px] font-mono text-xs"
              placeholder='{"createTitle":"新建","editTitle":"编辑","fields":[]}'
              onChange={(event) => update({ formJson: event.target.value })}
            />
          </div>

          {error && <p className="text-sm text-destructive" role="alert">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>取消</Button>
          <Button onClick={submit} disabled={pending}>
            {pending ? "正在保存..." : editing ? "保存修改" : "创建配置"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function datasourceLabel(datasources: DatasourceOption[], key: string): string {
  return datasources.find((ds) => ds.key === key)?.label ?? key
}
