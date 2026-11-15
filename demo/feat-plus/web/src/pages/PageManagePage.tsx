import { useMemo, useState } from "react"
import { AlertTriangle, Check, Copy, ExternalLink, Pencil, Plus, Search, SlidersHorizontal, Trash2 } from "lucide-react"
import { Link } from "react-router-dom"
import { toast } from "sonner"

import type { PageAdminItem, PageInput } from "@/api/types"
import { useSession } from "@/auth/session"
import { PageStudio, type ReferencingMenu } from "@/pages/PageStudio"
import { lintDraft, splitIssues } from "@/components/dw/lint"
import { EMPTY_FORM, EMPTY_LIST, datasourceOf, type PageDraft } from "@/components/dw/meta"
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
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { hasPermission } from "@/lib/permissions"
import { useDatasources, useMenus, usePageAdmin, usePageMutations } from "@/services/queries"

/**
 * 页面配置（动态页面）的资产库。
 *
 * 这里管理的不再是「一堆 JSON」，而是可以上线、会被菜单引用、会影响他人的页面资产：
 * 谁能打开、被几个菜单引用、配置还健不健康、改的时候会波及谁。页面一多，
 * 没有这层信息就只能靠标识猜。
 *
 * 编辑走整页工作台（PageStudio），不再用弹窗：配置密度撑不住一个 modal。
 */
export function PageManagePage() {
  const session = useSession()
  const canRead = hasPermission(session, "page:read")
  const canWrite = hasPermission(session, "page:write")
  const canReadMenus = hasPermission(session, "menu:read")

  const { data: pages = [], isLoading, isError, refetch } = usePageAdmin(canRead)
  const { data: datasources = [] } = useDatasources(canRead)
  const { data: menus = [] } = useMenus(canRead && canReadMenus)
  const mutations = usePageMutations()

  const [keyword, setKeyword] = useState("")
  const [datasourceFilter, setDatasourceFilter] = useState("all")
  const [studio, setStudio] = useState<{ mode: "create" | "edit"; record: PageAdminItem | null } | null>(null)
  const [removing, setRemoving] = useState<PageAdminItem | null>(null)

  /** pageKey → 指向它的菜单。删页面、改页面前都要知道「会断谁的路」。 */
  const menuIndex = useMemo(() => {
    const index = new Map<string, ReferencingMenu[]>()
    for (const menu of menus) {
      if (!menu.path.startsWith("/p/")) continue
      const key = menu.path.slice(3)
      const list = index.get(key) ?? []
      list.push({ name: menu.name, path: menu.path })
      index.set(key, list)
    }
    return index
  }, [menus])

  /** 每行的配置健康度：字段漂移、动作与数据源不匹配之类，在列表上就能看出来。 */
  const health = useMemo(() => {
    const map = new Map<number, { errors: number; warnings: number; missingDatasource: boolean }>()
    for (const page of pages) {
      const draft = toDraft(page)
      const issues = lintDraft({ draft, datasources })
      const { errors, warnings } = splitIssues(issues)
      map.set(page.id, {
        errors: errors.length,
        warnings: warnings.length,
        missingDatasource: !datasourceOf(datasources, page.datasourceKey),
      })
    }
    return map
  }, [pages, datasources])

  const filtered = useMemo(() => {
    const needle = keyword.trim().toLowerCase()
    return pages.filter((page) => {
      if (datasourceFilter !== "all" && page.datasourceKey !== datasourceFilter) return false
      if (!needle) return true
      return (
        page.title.toLowerCase().includes(needle) ||
        page.pageKey.toLowerCase().includes(needle) ||
        page.description.toLowerCase().includes(needle)
      )
    })
  }, [pages, keyword, datasourceFilter])

  if (!canRead) {
    return (
      <div className="space-y-6">
        <PageHeader />
        <Card>
          <CardContent className="p-12 text-center">
            <p className="text-sm">当前账号没有「查看页面配置」权限</p>
            <p className="mt-1 text-xs text-muted-foreground">需要管理员在角色管理里授予 page:read 权限点。</p>
          </CardContent>
        </Card>
      </div>
    )
  }

  async function submit(input: PageInput) {
    if (studio?.mode === "edit" && studio.record) {
      await mutations.update.mutateAsync({ id: studio.record.id, input })
      toast.success("页面配置已保存，立即生效")
    } else {
      await mutations.create.mutateAsync(input)
      toast.success("页面配置已创建，挂一条 /p/ 菜单即可上线")
    }
    setStudio(null)
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

  /** 复制一份配置：换标识、标题加「副本」，其余原样。这是「照着一个页面改」最快的路径。 */
  async function duplicate(page: PageAdminItem) {
    const pageKey = freeKey(page.pageKey, pages)
    try {
      await mutations.create.mutateAsync({
        pageKey,
        title: `${page.title} 副本`,
        description: page.description,
        datasourceKey: page.datasourceKey,
        listJson: page.listJson,
        formJson: page.formJson,
      })
      toast.success(`已复制为 /p/${pageKey}`, {
        description: "记得去菜单管理里给它挂一个入口。",
      })
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "复制失败")
    }
  }

  const removingMenus = removing ? menuIndex.get(removing.pageKey) ?? [] : []

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <PageHeader />
        {canWrite && (
          <Button onClick={() => setStudio({ mode: "create", record: null })}>
            <Plus className="size-4" />
            新建页面配置
          </Button>
        )}
      </div>

      <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
        <div className="relative sm:w-72">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input
            className="h-9 pl-8"
            value={keyword}
            placeholder="搜索标题、标识或说明"
            onChange={(event) => setKeyword(event.target.value)}
          />
        </div>
        <Select value={datasourceFilter} onValueChange={setDatasourceFilter}>
          <SelectTrigger className="h-9 sm:w-56">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">全部数据源</SelectItem>
            {datasources.map((ds) => (
              <SelectItem key={ds.key} value={ds.key}>
                {ds.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <p className="text-xs text-muted-foreground sm:ml-auto">
          共 {pages.length} 个页面
          {filtered.length !== pages.length && `，筛出 ${filtered.length} 个`}
        </p>
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
                <TableHead className="min-w-[180px]">页面</TableHead>
                <TableHead className="w-[150px]">访问地址</TableHead>
                <TableHead className="w-32">数据源</TableHead>
                <TableHead className="w-32">入口</TableHead>
                <TableHead className="w-28">配置状态</TableHead>
                <TableHead className="w-40">更新时间</TableHead>
                <TableHead className="w-40">
                  <span className="sr-only">操作</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading
                ? Array.from({ length: 3 }, (_, index) => (
                    <TableRow key={index}>
                      {Array.from({ length: 7 }, (_, cell) => (
                        <TableCell key={cell}>
                          <Skeleton className="h-4 w-24" />
                        </TableCell>
                      ))}
                    </TableRow>
                  ))
                : filtered.map((page) => {
                    const state = health.get(page.id)
                    const refs = menuIndex.get(page.pageKey) ?? []
                    return (
                      <TableRow key={page.id}>
                        <TableCell>
                          <strong className="text-sm">{page.title}</strong>
                          {page.description && (
                            <p className="mt-0.5 line-clamp-2 max-w-md text-xs text-muted-foreground">
                              {page.description}
                            </p>
                          )}
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="font-mono text-[10px]">
                            /p/{page.pageKey}
                          </Badge>
                        </TableCell>
                        <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                          {datasourceOf(datasources, page.datasourceKey)?.label ?? (
                            <span className="text-destructive">{page.datasourceKey}（已下线）</span>
                          )}
                        </TableCell>
                        <TableCell>
                          {!canReadMenus ? (
                            <span className="text-xs text-muted-foreground">—</span>
                          ) : refs.length > 0 ? (
                            <div className="flex flex-wrap gap-1">
                              {refs.slice(0, 2).map((menu) => (
                                <Badge key={menu.path} variant="secondary" className="text-[10px]">
                                  {menu.name}
                                </Badge>
                              ))}
                              {refs.length > 2 && (
                                <Badge variant="outline" className="text-[10px]">
                                  +{refs.length - 2}
                                </Badge>
                              )}
                            </div>
                          ) : (
                            <span className="text-xs text-muted-foreground">未挂菜单</span>
                          )}
                        </TableCell>
                        <TableCell>
                          <ConfigStatus state={state} />
                        </TableCell>
                        <TableCell className="whitespace-nowrap text-xs text-muted-foreground">
                          {page.editTime}
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center justify-end gap-0.5">
                            <Button variant="ghost" size="icon" className="size-8" asChild aria-label={`打开 ${page.title}`}>
                              <Link to={`/p/${page.pageKey}`} target="_blank">
                                <ExternalLink className="size-3.5" />
                              </Link>
                            </Button>
                            {canWrite && (
                              <>
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="size-8"
                                  aria-label={`编辑 ${page.title}`}
                                  onClick={() => setStudio({ mode: "edit", record: page })}
                                >
                                  <Pencil className="size-3.5" />
                                </Button>
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="size-8"
                                  aria-label={`复制 ${page.title}`}
                                  onClick={() => duplicate(page)}
                                >
                                  <Copy className="size-3.5" />
                                </Button>
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="size-8 text-destructive"
                                  aria-label={`删除 ${page.title}`}
                                  onClick={() => setRemoving(page)}
                                >
                                  <Trash2 className="size-3.5" />
                                </Button>
                              </>
                            )}
                          </div>
                        </TableCell>
                      </TableRow>
                    )
                  })}
              {!isLoading && filtered.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7} className="p-12 text-center">
                    <SlidersHorizontal className="mx-auto size-8 text-muted-foreground/40" />
                    <p className="mt-3 text-sm font-medium">
                      {pages.length === 0 ? "还没有页面配置" : "没有匹配的页面"}
                    </p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {pages.length === 0
                        ? "新建一条配置，再挂一条 /p/ 菜单即可上线动态页面。"
                        : "换个关键词或数据源试试。"}
                    </p>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Card>

      {studio && (
        <PageStudio
          key={studio.record?.id ?? "new"}
          mode={studio.mode}
          initial={studio.record}
          datasources={datasources}
          referencingMenus={studio.record ? menuIndex.get(studio.record.pageKey) ?? [] : []}
          pending={mutations.create.isPending || mutations.update.isPending}
          onSubmit={submit}
          onClose={() => setStudio(null)}
        />
      )}

      <AlertDialog open={Boolean(removing)} onOpenChange={(open) => { if (!open) setRemoving(null) }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除页面配置？</AlertDialogTitle>
            <AlertDialogDescription>
              「{removing?.title}」删除后 /p/{removing?.pageKey} 将不可访问；
              指向它的菜单会保留，需要到菜单管理里另行处理。
            </AlertDialogDescription>
          </AlertDialogHeader>
          {removingMenus.length > 0 && (
            <div className="rounded-md border border-destructive/30 bg-destructive/5 p-2 text-xs">
              <p className="flex items-center gap-1 font-medium text-destructive">
                <AlertTriangle className="size-3.5" />
                这 {removingMenus.length} 个入口会变成死链
              </p>
              <p className="mt-1 text-muted-foreground">
                {removingMenus.map((menu) => `${menu.name}（${menu.path}）`).join("、")}
              </p>
            </div>
          )}
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

function PageHeader() {
  return (
    <div>
      <p className="text-sm text-muted-foreground">系统管理</p>
      <h2 className="mt-1 text-3xl font-semibold tracking-tight">页面配置</h2>
      <p className="mt-2 text-sm text-muted-foreground">
        配置驱动的动态页面：数据源决定能用什么字段，列 / 筛选 / 表单决定怎么展示，改完即生效，不用发版。
      </p>
    </div>
  )
}

function ConfigStatus({
  state,
}: {
  state?: { errors: number; warnings: number; missingDatasource: boolean }
}) {
  if (!state) return <span className="text-xs text-muted-foreground">—</span>
  if (state.missingDatasource) {
    return (
      <Badge variant="destructive" className="text-[10px]">
        数据源已下线
      </Badge>
    )
  }
  if (state.errors > 0) {
    return (
      <Badge variant="destructive" className="gap-1 text-[10px]">
        <AlertTriangle className="size-2.5" />
        {state.errors} 个问题
      </Badge>
    )
  }
  if (state.warnings > 0) {
    return (
      <Badge variant="outline" className="gap-1 border-amber-500/50 text-[10px] text-amber-700 dark:text-amber-300">
        {state.warnings} 条提示
      </Badge>
    )
  }
  return (
    <Badge variant="outline" className="gap-1 border-emerald-600/40 text-[10px] text-emerald-700 dark:text-emerald-400">
      <Check className="size-2.5" />
      正常
    </Badge>
  )
}

// ---------------------------------------------------------------------------
// 行数据 → 草稿：只用于体检，不发请求
// ---------------------------------------------------------------------------

function toDraft(page: PageAdminItem): PageDraft {
  return {
    pageKey: page.pageKey,
    title: page.title,
    description: page.description,
    datasourceKey: page.datasourceKey,
    list: parse(page.listJson, EMPTY_LIST),
    form: parse(page.formJson, EMPTY_FORM),
  }
}

function parse<T>(json: string, fallback: T): T {
  if (!json.trim()) return fallback
  try {
    return { ...fallback, ...JSON.parse(json) } as T
  } catch {
    return fallback
  }
}

/** 复制时找一个没被占用的标识：roles → roles-copy → roles-copy-2。 */
function freeKey(base: string, pages: PageAdminItem[]): string {
  const used = new Set(pages.map((page) => page.pageKey))
  const seed = `${base.slice(0, 24)}-copy`.slice(0, 32)
  if (!used.has(seed)) return seed
  for (let i = 2; i < 100; i += 1) {
    const candidate = `${base.slice(0, 25)}-copy-${i}`.slice(0, 32)
    if (!used.has(candidate)) return candidate
  }
  return `${base.slice(0, 26)}-c${Date.now() % 1000}`.slice(0, 32)
}
