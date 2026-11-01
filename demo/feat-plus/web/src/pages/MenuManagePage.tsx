import { useMemo, useState } from "react"
import { GripVertical, Pencil, Plus, Trash2 } from "lucide-react"
import { toast } from "sonner"

import type { MenuAdminItem, MenuInput } from "@/api/types"
import { useRefreshSession, useSession } from "@/auth/session"
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
import { Switch } from "@/components/ui/switch"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { NAV_ICONS, NAV_ICON_KEYS } from "@/lib/icons"
import { groupByKey } from "@/lib/menus"
import { hasPermission } from "@/lib/permissions"
import { useMenuMutations, useMenuRoutes, useMenus, usePermissionCatalog } from "@/services/queries"

/**
 * 表单初值。分组的标识与名称留空而不是写死一个——写死会让「新增」总是落进同一个组，
 * 而这是演示数据里的偶然，不是规则。页面上给了「已有分组」的快捷按钮来省打字。
 */
const emptyDraft: MenuInput = {
  groupKey: "",
  groupLabel: "",
  groupSort: 10,
  path: "",
  name: "",
  icon: "dashboard",
  permission: "",
  sort: 10,
  visible: true,
}

/**
 * 菜单管理。
 *
 * 这个页面存在的意义是：菜单从「改代码重新构建」变成「改完立刻生效」。客户最常见的需求——
 * 「这个菜单别叫用户，叫员工」「把审计日志挪到工作台」——原来都要发一次版。
 *
 * 但有一件事仍然只能在代码里做：**新增页面**。Feat 的路由是编译期的，后端最多下发一个 path 字符串，
 * 拿不到要渲染的组件。所以「路径」是一个下拉框，选项来自后端白名单（MenuRoutes），
 * 而白名单与 App.tsx 的 Route 由 check_menu.py 双向校验。这条边界写在页面上，不藏着。
 */
export function MenuManagePage() {
  const session = useSession()
  const canRead = hasPermission(session, "menu:read")
  const canWrite = hasPermission(session, "menu:write")
  const { data: menus = [], isLoading, isError, refetch } = useMenus(canRead)
  // 路径与权限点只在写的时候用得上，没有 menu:write 就不必去请求（否则只会拿到 403）。
  const { data: routes = [] } = useMenuRoutes(canWrite)
  const { data: permissionCatalog = [] } = usePermissionCatalog(canRead)
  // 改完菜单要重新拉一次 /me，否则管理页显示新标题、左边侧栏还是旧的。
  const refreshSession = useRefreshSession()
  const mutations = useMenuMutations(refreshSession)

  const [editing, setEditing] = useState<MenuAdminItem | null>(null)
  const [draft, setDraft] = useState<MenuInput>(emptyDraft)
  const [formOpen, setFormOpen] = useState(false)
  const [formError, setFormError] = useState("")
  const [removing, setRemoving] = useState<MenuAdminItem | null>(null)

  // 后端已按 groupSort / sort / id 排好序，这里只做分组，不再排序。
  const groups = useMemo(() => groupByKey(menus), [menus])
  const usedPaths = useMemo(() => new Set(menus.map((item) => item.path)), [menus])
  // 编辑自己时要把当前路径留在选项里，否则「改了别的字段但没动路径」会因为选项缺失被拦下。
  const routeOptions = useMemo(
    () => routes.filter((route) => route === draft.path || !usedPaths.has(route)),
    [routes, usedPaths, draft.path],
  )
  const permissionLabels = useMemo(
    () => new Map(permissionCatalog.map((item) => [item.key, item.label])),
    [permissionCatalog],
  )
  const groupPresets = useMemo(
    () => groups.map((group) => ({ key: group.key, label: group.label, sort: group.items[0].groupSort })),
    [groups],
  )

  function openCreate() {
    const free = routes.filter((route) => !usedPaths.has(route))
    if (!free.length) {
      toast.info("所有页面都已配好菜单", {
        description: "新增菜单只能指向已经存在的页面：先在 App.tsx 与 MenuRoutes 里登记，再回到这里配置。",
      })
      return
    }
    setEditing(null)
    setDraft({ ...emptyDraft, path: free[0] })
    setFormError("")
    setFormOpen(true)
  }

  function openEdit(item: MenuAdminItem) {
    setEditing(item)
    setDraft({
      groupKey: item.groupKey,
      groupLabel: item.groupLabel,
      groupSort: item.groupSort,
      path: item.path,
      name: item.name,
      icon: item.icon,
      permission: item.permission,
      sort: item.sort,
      visible: item.visible,
    })
    setFormError("")
    setFormOpen(true)
  }

  function patch(changes: Partial<MenuInput>) {
    setDraft((current) => ({ ...current, ...changes }))
  }

  async function submit() {
    if (!draft.groupKey.trim() || !draft.groupLabel.trim()) {
      setFormError("请填写分组标识与分组名称")
      return
    }
    if (!draft.path) {
      setFormError("请选择页面路径")
      return
    }
    if (!draft.name.trim()) {
      setFormError("请填写菜单名称")
      return
    }
    try {
      if (editing) {
        await mutations.update.mutateAsync({ id: editing.id, input: draft })
        toast.success("菜单已更新")
      } else {
        await mutations.create.mutateAsync(draft)
        toast.success("菜单已创建")
      }
      setFormOpen(false)
    } catch (error) {
      // 后端会拒掉「页面不存在」「该页面已有菜单」「未知权限点」这三类，原样显示比前端再猜一遍准确。
      setFormError(error instanceof Error ? error.message : "保存失败")
    }
  }

  async function confirmRemove() {
    if (!removing) return
    try {
      await mutations.remove.mutateAsync(removing.id)
      toast.success("菜单已删除")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "删除失败")
    } finally {
      setRemoving(null)
    }
  }

  if (!canRead) {
    return (
      <div className="space-y-2">
        <h2 className="text-3xl font-semibold tracking-tight">菜单管理</h2>
        <p className="text-sm text-muted-foreground">你没有查看菜单配置的权限（menu:read）。</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="text-sm text-muted-foreground">系统管理</p>
          <h2 className="mt-1 text-3xl font-semibold tracking-tight">菜单管理</h2>
          <p className="mt-2 max-w-3xl text-sm text-muted-foreground">
            侧边栏的名称、分组、图标、顺序与权限点都在这里改，保存后刷新即生效，不需要重新发版。
            能新增的只有「已经存在的页面」——路径下拉只列出白名单里的页面，因为组件必须在编译期存在。
          </p>
        </div>
        {canWrite && (
          <Button onClick={openCreate} className="shrink-0">
            <Plus className="size-4" /> 新增菜单
          </Button>
        )}
      </div>

      {isError && (
        <Card>
          <CardContent className="flex items-center justify-between gap-4 p-6 text-sm">
            <span className="text-muted-foreground">菜单加载失败，请稍后重试。</span>
            <Button variant="outline" onClick={() => refetch()}>重试</Button>
          </CardContent>
        </Card>
      )}

      {isLoading && <Skeleton className="h-64 w-full" />}

      {!isLoading && !isError && groups.length === 0 && (
        <Card>
          <CardContent className="p-10 text-center text-sm text-muted-foreground">
            还没有任何菜单。侧栏会变成空的，页面仍可通过地址直接访问。
          </CardContent>
        </Card>
      )}

      {groups.map((group) => {
        const first = group.items[0]
        return (
          <Card key={group.key}>
            <CardContent className="p-0">
              <div className="flex flex-wrap items-center gap-2 border-b px-4 py-3">
                <GripVertical className="size-4 text-muted-foreground/50" />
                <strong className="text-sm">{group.label}</strong>
                <Badge variant="secondary" className="text-[10px]">{group.key}</Badge>
                <span className="text-[11px] text-muted-foreground">组顺序 {first.groupSort}</span>
                <span className="ml-auto text-[11px] text-muted-foreground">{group.items.length} 项</span>
              </div>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>菜单</TableHead>
                    <TableHead>页面路径</TableHead>
                    <TableHead>所需权限点</TableHead>
                    <TableHead className="w-20">排序</TableHead>
                    <TableHead className="w-24">侧栏显示</TableHead>
                    <TableHead className="w-28 text-right">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {group.items.map((item) => {
                    const Icon = NAV_ICONS[item.icon]
                    return (
                      <TableRow key={item.id}>
                        <TableCell>
                          <span className="flex items-center gap-2">
                            {Icon ? <Icon className="size-4 text-muted-foreground" /> : <span className="text-[10px] text-destructive">图标缺失</span>}
                            <span className="font-medium">{item.name}</span>
                          </span>
                        </TableCell>
                        <TableCell className="text-xs text-muted-foreground">{item.path}</TableCell>
                        <TableCell>
                          {item.permission
                            ? <Badge variant="outline" className="text-[10px]">{permissionLabels.get(item.permission) ?? item.permission}</Badge>
                            : <span className="text-[11px] text-muted-foreground">登录可见</span>}
                        </TableCell>
                        <TableCell className="text-xs text-muted-foreground">{item.sort}</TableCell>
                        <TableCell>
                          {item.visible
                            ? <Badge variant="secondary" className="text-[10px]">显示</Badge>
                            : <span className="text-[11px] text-muted-foreground">仅标题</span>}
                        </TableCell>
                        <TableCell className="text-right">
                          {canWrite && (
                            <span className="flex justify-end gap-1">
                              <Button variant="ghost" size="icon" onClick={() => openEdit(item)} aria-label="编辑菜单">
                                <Pencil className="size-4" />
                              </Button>
                              <Button variant="ghost" size="icon" onClick={() => setRemoving(item)} aria-label="删除菜单">
                                <Trash2 className="size-4 text-destructive" />
                              </Button>
                            </span>
                          )}
                        </TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        )
      })}

      <Dialog open={formOpen} onOpenChange={setFormOpen}>
        <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>{editing ? "编辑菜单" : "新增菜单"}</DialogTitle>
            <DialogDescription>
              路径决定点进去是哪个页面，改不了目标；名称、图标、分组与顺序随时可改。
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 py-2">
            <div className="grid gap-2 sm:grid-cols-2">
              <div className="grid gap-2">
                <Label htmlFor="menu-name">菜单名称</Label>
                <Input id="menu-name" value={draft.name} onChange={(event) => patch({ name: event.target.value })}
                       placeholder="例如：员工管理" maxLength={64} />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="menu-path">页面路径</Label>
                <Select value={draft.path} onValueChange={(value) => patch({ path: value })}>
                  <SelectTrigger id="menu-path"><SelectValue placeholder="选择页面" /></SelectTrigger>
                  <SelectContent>
                    {routeOptions.map((route) => <SelectItem key={route} value={route}>{route}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid gap-2 sm:grid-cols-3">
              <div className="grid gap-2">
                <Label htmlFor="menu-group-key">分组标识</Label>
                <Input id="menu-group-key" value={draft.groupKey}
                       onChange={(event) => patch({ groupKey: event.target.value })}
                       placeholder="workspace" maxLength={32} />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="menu-group-label">分组名称</Label>
                <Input id="menu-group-label" value={draft.groupLabel}
                       onChange={(event) => patch({ groupLabel: event.target.value })}
                       placeholder="工作台" maxLength={32} />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="menu-group-sort">分组顺序</Label>
                <Input id="menu-group-sort" type="number" value={draft.groupSort}
                       onChange={(event) => patch({ groupSort: Number(event.target.value) || 0 })} />
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <span className="text-[11px] text-muted-foreground">已有分组：</span>
              {groupPresets.map((group) => (
                <Button key={group.key} type="button" variant="outline" size="sm" className="h-6 px-2 text-[11px]"
                        onClick={() => patch({ groupKey: group.key, groupLabel: group.label, groupSort: group.sort })}>
                  {group.label}
                </Button>
              ))}
            </div>

            <div className="grid gap-2 sm:grid-cols-2">
              <div className="grid gap-2">
                <Label htmlFor="menu-icon">图标</Label>
                <Select value={draft.icon || NAV_ICON_KEYS[0]} onValueChange={(value) => patch({ icon: value })}>
                  <SelectTrigger id="menu-icon"><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {NAV_ICON_KEYS.map((key) => {
                      const Icon = NAV_ICONS[key]
                      return <SelectItem key={key} value={key}><Icon className="size-4" />{key}</SelectItem>
                    })}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="menu-permission">所需权限点</Label>
                <Select value={draft.permission || "__none"} onValueChange={(value) => patch({ permission: value === "__none" ? "" : value })}>
                  <SelectTrigger id="menu-permission"><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="__none">登录可见（不额外校验）</SelectItem>
                    {permissionCatalog.map((item) => (
                      <SelectItem key={item.key} value={item.key}>{item.label}（{item.key}）</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid gap-2 sm:grid-cols-2">
              <div className="grid gap-2">
                <Label htmlFor="menu-sort">组内顺序</Label>
                <Input id="menu-sort" type="number" value={draft.sort}
                       onChange={(event) => patch({ sort: Number(event.target.value) || 0 })} />
              </div>
              <div className="flex items-end gap-3 pb-2">
                <Switch id="menu-visible" checked={draft.visible} onCheckedChange={(value) => patch({ visible: value })} />
                <div className="grid">
                  <Label htmlFor="menu-visible">显示在侧栏</Label>
                  <span className="text-[11px] text-muted-foreground">
                    关掉后只保留「路径 → 标题」，例如个人中心
                  </span>
                </div>
              </div>
            </div>

            {formError && <p className="text-sm text-destructive">{formError}</p>}
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setFormOpen(false)}>取消</Button>
            <Button onClick={submit} disabled={mutations.create.isPending || mutations.update.isPending}>
              {editing ? "保存" : "创建"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <AlertDialog open={Boolean(removing)} onOpenChange={(open) => !open && setRemoving(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>删除菜单「{removing?.name}」？</AlertDialogTitle>
            <AlertDialogDescription>
              侧栏入口会立刻消失，页面本身仍然可以访问。
              {removing?.path === "/menus" && " 这是菜单管理自己的入口——删掉后只能直接在地址栏输入 /menus 回来。"}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction onClick={confirmRemove}>删除</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
