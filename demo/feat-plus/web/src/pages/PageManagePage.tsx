import { useEffect, useState } from "react"
import { Pencil, Plus, SlidersHorizontal, Trash2 } from "lucide-react"
import { toast } from "sonner"

import type {
  DatasourceOption,
  DwFormConfig,
  DwListConfig,
  PageAdminItem,
  PageInput,
} from "@/api/types"
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
import { DwFormConfigEditor } from "@/components/dw/DwFormConfigEditor"
import { DwListConfigEditor } from "@/components/dw/DwListConfigEditor"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { cn } from "@/lib/utils"
import { hasPermission } from "@/lib/permissions"
import { useDatasources, usePageAdmin, usePageMutations } from "@/services/queries"

/**
 * 动态页面配置管理页。配置编辑是全结构化的：列 / 筛选 / 表单字段按控件编辑，
 * 字段 key 只能从数据源的字段声明里选。
 * <p>
 * 前端校验是后端 PageService 的镜像（必填、选项完整性、正则可编译），目的是即时反馈；
 * 「存进去的一定能渲染」的最终闸门仍然在后端。
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

/** 新建时的空白配置：与后端 DTO 的字段默认值对齐。 */
const emptyList: DwListConfig = { columns: [], filters: [], pageSize: 20, actions: [], rowKey: "id" }
const emptyForm: DwFormConfig = {
  createTitle: "新建",
  editTitle: "编辑",
  createDescription: "",
  editDescription: "",
  fields: [],
}

/** 配置草稿：头部字段 + 结构化的 list / form，提交时才序列化成 JSON。 */
interface ConfigDraft {
  pageKey: string
  title: string
  description: string
  datasourceKey: string
  list: DwListConfig
  form: DwFormConfig
}

/** 编辑弹窗的三个页签：配置分块编辑，避免在一个滚动区里找控件。 */
type TabKey = "basic" | "list" | "form"

const emptyDraft: ConfigDraft = {
  pageKey: "",
  title: "",
  description: "",
  datasourceKey: "",
  list: { ...emptyList, columns: [], filters: [], actions: [] },
  form: { ...emptyForm, fields: [] },
}

/** 库里的 list_json / form_json → 结构化草稿。解析失败理论上不该发生（保存时已过校验）。 */
function parseConfig<T>(json: string, fallback: T): T {
  if (!json.trim()) return fallback
  try {
    return { ...fallback, ...JSON.parse(json) } as T
  } catch {
    return fallback
  }
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
  const [draft, setDraft] = useState<ConfigDraft>(emptyDraft)
  const [tab, setTab] = useState<TabKey>("basic")
  const [error, setError] = useState("")
  const [errorTab, setErrorTab] = useState<TabKey>("basic")
  const [parseWarning, setParseWarning] = useState("")

  useEffect(() => {
    if (!open) return
    setTab("basic")
    setErrorTab("basic")
    if (editing) {
      const list = parseConfig<DwListConfig>(editing.listJson, emptyList)
      const form = parseConfig<DwFormConfig>(editing.formJson, emptyForm)
      setDraft({
        pageKey: editing.pageKey,
        title: editing.title,
        description: editing.description,
        datasourceKey: editing.datasourceKey,
        list,
        form,
      })
      // 保存闸门保证库里的一定能渲染，解析失败只能是脏数据——提示而不是静默丢弃配置。
      const badList = editing.listJson.trim() !== "" && list === emptyList
      const badForm = editing.formJson.trim() !== "" && form === emptyForm
      setParseWarning(
        badList || badForm
          ? "存量配置 JSON 解析失败，已回退为空白配置；直接保存会覆盖原配置，请先核对。"
          : "",
      )
    } else {
      setDraft({ ...emptyDraft })
      setParseWarning("")
    }
    setError("")
  }, [open, editing])

  const selected = datasources.find((ds) => ds.key === draft.datasourceKey)
  const fields = selected?.fields ?? []

  function update(patch: Partial<ConfigDraft>) {
    setDraft((current) => ({ ...current, ...patch }))
  }

  function switchDatasource(key: string) {
    setDraft((current) => {
      const next = datasources.find((ds) => ds.key === key)
      // 只读数据源：把已勾选的写动作摘掉，避免「保存时被后端 400」。
      const actions =
        next && !next.writable
          ? current.list.actions.filter((action) => action === "refresh")
          : current.list.actions
      return { ...current, datasourceKey: key, list: { ...current.list, actions } }
    })
    // 新建流程里选完数据源是「基本信息」的最后一步，直接带到列表页签开始配列。
    if (!editing) {
      setTab("list")
    }
  }

  /**
   * 后端校验的镜像：能在这里拦住的错误就不发请求。
   * 返回错误文案 + 所在页签（提交失败时自动切过去，错误文案固定展示在页脚上方）。
   */
  function validate(): { tab: TabKey; message: string } {
    const fail = (tab: TabKey, message: string) => ({ tab, message })
    if (!draft.pageKey.trim()) return fail("basic", "请填写页面标识")
    if (!/^[a-z0-9_-]{1,32}$/.test(draft.pageKey.trim())) {
      return fail("basic", "页面标识只能包含小写字母、数字、下划线和短横线")
    }
    if (!draft.title.trim()) return fail("basic", "请填写页面标题")
    if (!draft.datasourceKey) return fail("basic", "请选择数据源")
    if (!selected) return fail("basic", "数据源不存在，请重新选择")

    const fieldKeys = new Set(fields.map((field) => field.key))
    const writableKeys = new Set(fields.filter((field) => field.writable).map((field) => field.key))
    const { list, form } = draft

    if (list.pageSize < 1 || list.pageSize > 200) {
      return fail("list", "每页条数必须在 1 到 200 之间")
    }
    if (!fieldKeys.has(list.rowKey)) return fail("list", "行主键字段必须是数据源声明过的字段")
    if (list.columns.length === 0) {
      return fail("list", "列表至少要有一列，可用「按字段生成」快速建列")
    }

    for (const column of list.columns) {
      const where = `列 ${column.label || column.key}`
      if (!column.key) return fail("list", `${where}缺少字段`)
      if (!column.label.trim()) return fail("list", `${where}缺少名称`)
      if (!fieldKeys.has(column.key)) {
        return fail("list", `${where}的字段 ${column.key} 不在当前数据源声明里`)
      }
      if (column.format === "badge" && (column.options ?? []).length === 0) {
        return fail("list", `${where}是 badge 格式，需要配置值映射（或改用 text / muted）`)
      }
      const optionError = checkOptions(column.options ?? [], where)
      if (optionError) return fail("list", optionError)
    }
    for (const [i, column] of list.columns.entries()) {
      if (list.columns.slice(0, i).some((other) => other.key === column.key)) {
        return fail("list", `列 ${column.label} 的字段 ${column.key} 重复出现`)
      }
    }

    for (const filter of list.filters) {
      const where = `筛选 ${filter.label || filter.key}`
      if (!filter.key) return fail("list", `${where}缺少字段`)
      if (!filter.label.trim()) return fail("list", `${where}缺少名称`)
      if (!fieldKeys.has(filter.key)) {
        return fail("list", `${where}的字段 ${filter.key} 不在当前数据源声明里`)
      }
      if (filter.widget === "select" && (filter.options ?? []).length === 0) {
        return fail("list", `${where}是下拉控件，必须配置选项`)
      }
      const optionError = checkOptions(filter.options ?? [], where)
      if (optionError) return fail("list", optionError)
    }
    for (const [i, filter] of list.filters.entries()) {
      if (list.filters.slice(0, i).some((other) => other.key === filter.key)) {
        return fail("list", `筛选 ${filter.label} 的字段 ${filter.key} 重复出现`)
      }
    }

    for (const field of form.fields) {
      const where = `表单字段 ${field.label || field.key}`
      if (!field.key) return fail("form", `${where}缺少字段`)
      if (!field.label.trim()) return fail("form", `${where}缺少名称`)
      if (!writableKeys.has(field.key)) {
        return fail("form", `${where}的字段 ${field.key} 不是当前数据源的可写字段`)
      }
      if (field.widget === "select" && (field.options ?? []).length === 0) {
        return fail("form", `${where}是下拉字段，必须配置选项`)
      }
      if (field.required && field.widget === "span") {
        return fail("form", `${where}是只读展示字段，不能同时是必填项`)
      }
      if (field.pattern && field.pattern.trim() !== "") {
        try {
          new RegExp(field.pattern)
        } catch {
          return fail("form", `${where}的校验正则无法编译：${field.pattern}`)
        }
      }
      const optionError = checkOptions(field.options ?? [], where)
      if (optionError) return fail("form", optionError)
    }

    for (const [i, field] of form.fields.entries()) {
      if (form.fields.slice(0, i).some((other) => other.key === field.key)) {
        return fail("form", `表单字段 ${field.label} 的 ${field.key} 重复出现`)
      }
    }
    return { tab, message: "" }
  }

  async function submit() {
    const result = validate()
    if (result.message) {
      setErrorTab(result.tab)
      setTab(result.tab)
      setError(result.message)
      return
    }
    setError("")
    await onSubmit({
      pageKey: draft.pageKey.trim(),
      title: draft.title.trim(),
      description: draft.description,
      datasourceKey: draft.datasourceKey,
      listJson: JSON.stringify(draft.list),
      formJson: JSON.stringify(draft.form),
    })
  }

  const tabs: Array<{ key: TabKey; label: string; count?: string }> = [
    { key: "basic", label: "基本信息" },
    {
      key: "list",
      label: "列表",
      count: `${draft.list.columns.length} 列 · ${draft.list.filters.length} 筛选`,
    },
    {
      key: "form",
      label: "表单",
      count: `${draft.form.fields.length} 字段`,
    },
  ]

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="flex h-[92vh] max-h-[92vh] w-[calc(100vw-2rem)] flex-col overflow-hidden sm:max-w-5xl">
        <DialogHeader>
          <DialogTitle>{editing ? "编辑页面配置" : "新建页面配置"}</DialogTitle>
          <DialogDescription>
            {editing
              ? "页面标识创建后不可修改（菜单以 /p/{标识} 引用它）。字段只能从数据源声明里选，校验规则与后端一致。"
              : "保存后挂一条 /p/{标识} 的菜单即可上线。字段只能从数据源声明里选，校验规则与后端一致。"}
          </DialogDescription>
        </DialogHeader>

        <div className="flex gap-1 border-b" role="tablist" aria-label="配置分区">
          {tabs.map((item) => {
            const active = tab === item.key
            return (
              <button
                key={item.key}
                type="button"
                role="tab"
                aria-selected={active}
                className={cn(
                  "-mb-px flex items-center gap-1.5 border-b-2 px-3 py-2 text-sm font-medium transition-colors",
                  active
                    ? "border-primary text-primary"
                    : "border-transparent text-muted-foreground hover:text-foreground",
                  !selected && item.key !== "basic" && "opacity-50",
                )}
                onClick={() => setTab(item.key)}
              >
                {item.label}
                {item.count && (
                  <span className="text-xs font-normal text-muted-foreground">{item.count}</span>
                )}
                {error && errorTab === item.key && (
                  <span className="size-1.5 rounded-full bg-destructive" aria-label="此分区有校验错误" />
                )}
              </button>
            )
          })}
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto py-4">
          {tab === "basic" && (
            <div className="grid gap-4">
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
                  <Input
                    id="page-title"
                    value={draft.title}
                    onChange={(event) => update({ title: event.target.value })}
                  />
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
                <Select value={draft.datasourceKey} onValueChange={switchDatasource}>
                  <SelectTrigger id="page-datasource">
                    <SelectValue placeholder="选择一个数据源" />
                  </SelectTrigger>
                  <SelectContent>
                    {datasources.map((ds) => (
                      <SelectItem key={ds.key} value={ds.key}>
                        {ds.label}（{ds.key}）{ds.writable ? "" : " · 只读"}
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
                      <Badge
                        key={field.key}
                        variant={field.writable ? "secondary" : "outline"}
                        className="text-[10px]"
                      >
                        {field.key}
                      </Badge>
                    ))}
                  </div>
                )}
                {!selected && (
                  <p className="text-xs text-muted-foreground">
                    选定数据源后即可配置列、筛选与表单。
                  </p>
                )}
              </div>

              {parseWarning && (
                <p className="text-sm text-amber-600 dark:text-amber-400">{parseWarning}</p>
              )}
            </div>
          )}

          {tab === "list" && (
            selected ? (
              <DwListConfigEditor
                value={draft.list}
                fields={fields}
                writable={selected.writable}
                onChange={(list) => update({ list })}
              />
            ) : (
              <p className="py-8 text-center text-sm text-muted-foreground">
                请先在「基本信息」里选择数据源。
              </p>
            )
          )}

          {tab === "form" && (
            selected ? (
              <DwFormConfigEditor value={draft.form} fields={fields} onChange={(form) => update({ form })} />
            ) : (
              <p className="py-8 text-center text-sm text-muted-foreground">
                请先在「基本信息」里选择数据源。
              </p>
            )
          )}
        </div>

        {error && (
          <p className="pb-2 text-sm text-destructive" role="alert">
            {error}
          </p>
        )}

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

/** 选项校验：值非空、不重复。样式枚举由下拉控件保证。 */
function checkOptions(options: Array<{ value: string }>, where: string): string {
  const seen = new Set<string>()
  for (const option of options) {
    if (!option.value.trim()) return `${where}有选项的值为空`
    if (seen.has(option.value)) return `${where}的选项值重复：${option.value}`
    seen.add(option.value)
  }
  return ""
}

function datasourceLabel(datasources: DatasourceOption[], key: string): string {
  return datasources.find((ds) => ds.key === key)?.label ?? key
}
