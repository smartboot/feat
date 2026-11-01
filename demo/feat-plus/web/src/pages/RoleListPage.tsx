import { useEffect, useMemo, useState } from "react"
import { Check, Lock, Pencil, Plus, ShieldCheck, Trash2, Users } from "lucide-react"
import { toast } from "sonner"

import type { DepartmentOption, RoleInput, RoleItem } from "@/api/types"
import { DATA_SCOPE_ALL, DATA_SCOPE_CUSTOM, DATA_SCOPE_OPTIONS } from "@/api/types"
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
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Skeleton } from "@/components/ui/skeleton"
import { Switch } from "@/components/ui/switch"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { flattenDepartments } from "@/lib/departments"
import { groupByKey } from "@/lib/menus"
import { hasPermission } from "@/lib/permissions"
import { useDepartments, usePermissionCatalog, useRoleMutations, useRoles } from "@/services/queries"
import { cn } from "@/lib/utils"

const emptyDraft: RoleInput = {
  code: "",
  name: "",
  description: "",
  permissions: [],
  departmentScope: DATA_SCOPE_ALL,
  departmentIds: [],
  status: 1,
}

export function RoleListPage() {
  const session = useSession()
  const canWrite = hasPermission(session, "role:write")
  const { data: roles = [], isLoading, isError, refetch } = useRoles()
  // 数据范围选「自定义」时要选部门，这里只要扁平化的选项，不需要树。
  const { data: departmentTree = [] } = useDepartments()
  // 权限点字典由后端下发，前端不再维护常量：新增权限点只改后端 Permission.CATALOG 一处。
  // 这里拿到的是「字典」不是「本人拥有的权限」——给角色授予自己没有的权限点是合法操作。
  const { data: permissionCatalog = [] } = usePermissionCatalog()
  const permissionLabels = useMemo(
    () => new Map(permissionCatalog.map((item) => [item.key, item.label])),
    [permissionCatalog],
  )
  // 用 useMemo 固定引用：否则每次渲染都会生成新数组，把表单里的选择冲掉。
  const departmentOptions = useMemo(() => flattenDepartments(departmentTree), [departmentTree])
  const mutations = useRoleMutations()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<RoleItem | null>(null)
  const [removing, setRemoving] = useState<RoleItem | null>(null)

  function openCreate() {
    setEditing(null)
    setDialogOpen(true)
  }

  function openEdit(role: RoleItem) {
    setEditing(role)
    setDialogOpen(true)
  }

  async function submit(input: RoleInput) {
    try {
      if (editing) {
        await mutations.update.mutateAsync({ id: editing.id, input })
        toast.success("角色已更新")
      } else {
        await mutations.create.mutateAsync(input)
        toast.success("角色已创建")
      }
      setDialogOpen(false)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "操作失败")
    }
  }

  async function toggleStatus(role: RoleItem) {
    try {
      await mutations.status.mutateAsync({ id: role.id, status: role.status ? 0 : 1 })
      toast.success(role.status ? "角色已停用" : "角色已启用")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "操作失败")
    }
  }

  async function confirmRemove() {
    if (!removing) return
    try {
      await mutations.remove.mutateAsync(removing.id)
      toast.success("角色已删除")
      setRemoving(null)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "操作失败")
      setRemoving(null)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="text-sm text-muted-foreground">系统管理</p>
          <h2 className="mt-1 text-3xl font-semibold tracking-tight">角色权限</h2>
          <p className="mt-2 text-sm text-muted-foreground">
            角色决定成员能做什么。权限点改动后立即生效，不需要成员重新登录。
          </p>
        </div>
        {canWrite && (
          <Button onClick={openCreate}>
            <Plus className="size-4" />新建角色
          </Button>
        )}
      </div>

      <Card className="overflow-hidden">
        {isError ? (
          <div className="p-12 text-center">
            <p className="text-sm">加载角色失败</p>
            <Button className="mt-4" variant="outline" onClick={() => refetch()}>重新加载</Button>
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[220px]">角色</TableHead>
                <TableHead>权限</TableHead>
                <TableHead className="w-36 whitespace-nowrap">数据范围</TableHead>
                <TableHead className="w-24 whitespace-nowrap">成员数</TableHead>
                <TableHead className="w-24">状态</TableHead>
                <TableHead className="w-16">
                  <span className="sr-only">操作</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading
                ? Array.from({ length: 3 }, (_, index) => (
                    <TableRow key={index}>
                      <TableCell><Skeleton className="h-4 w-40" /></TableCell>
                      <TableCell><Skeleton className="h-5 w-56" /></TableCell>
                      <TableCell><Skeleton className="h-4 w-8" /></TableCell>
                      <TableCell><Skeleton className="h-5 w-14" /></TableCell>
                      <TableCell />
                    </TableRow>
                  ))
                : roles.map((role) => (
                    <TableRow key={role.id}>
                      <TableCell>
                        <div className="flex items-start gap-2">
                          <span className="mt-0.5 text-muted-foreground">
                            {role.builtin ? <Lock className="size-4" /> : <ShieldCheck className="size-4" />}
                          </span>
                          <div className="grid gap-1">
                            <div className="flex items-center gap-2">
                              <strong className="text-sm">{role.name}</strong>
                              <Badge variant="outline" className="font-mono text-[10px]">{role.code}</Badge>
                              {role.builtin && <Badge variant="secondary" className="text-[10px]">内置</Badge>}
                            </div>
                            {role.description && (
                              <span className="text-xs text-muted-foreground">{role.description}</span>
                            )}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex flex-wrap gap-1">
                          {role.permissions.slice(0, 4).map((key) => (
                            <Badge key={key} variant="secondary" className="text-[10px]">{permissionLabels.get(key) ?? key}</Badge>
                          ))}
                          {role.permissions.length > 4 && (
                            <Badge variant="outline" className="text-[10px]">+{role.permissions.length - 4}</Badge>
                          )}
                          {role.permissions.length === 0 && (
                            <span className="text-xs text-muted-foreground">无业务权限</span>
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="whitespace-nowrap">
                        <span className="text-sm">{role.scopeLabel}</span>
                        {role.departmentScope === DATA_SCOPE_CUSTOM && (
                          <span className="ml-1 text-xs text-muted-foreground">
                            {role.departmentIds.length} 个部门
                          </span>
                        )}
                      </TableCell>
                      <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                        <span className="inline-flex items-center gap-1">
                          <Users className="size-3.5" />{role.userCount}
                        </span>
                      </TableCell>
                      <TableCell>
                        <Badge variant={role.status ? "success" : "warning"}>
                          <span className="size-1.5 rounded-full bg-current" />
                          {role.status ? "已启用" : "已停用"}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {canWrite && (
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="icon" aria-label={`${role.name} 的操作`}>
                                <Pencil className="size-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem onClick={() => openEdit(role)}>编辑角色</DropdownMenuItem>
                              <DropdownMenuItem
                                disabled={role.builtin}
                                onClick={() => toggleStatus(role)}
                              >
                                {role.status ? "停用角色" : "启用角色"}
                              </DropdownMenuItem>
                              <DropdownMenuSeparator />
                              <DropdownMenuItem
                                destructive
                                disabled={role.builtin || role.userCount > 0}
                                onClick={() => setRemoving(role)}
                              >
                                <Trash2 /> 删除角色
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
            </TableBody>
          </Table>
        )}
      </Card>

      <RoleFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        role={editing}
        departmentOptions={departmentOptions}
        onSubmit={submit}
        pending={mutations.create.isPending || mutations.update.isPending}
      />

      <AlertDialog open={Boolean(removing)} onOpenChange={(open) => { if (!open) setRemoving(null) }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除角色？</AlertDialogTitle>
            <AlertDialogDescription>
              「{removing?.name}」将从角色列表移除。仍被账号引用的角色无法删除，需要先改派。
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

function RoleFormDialog({
  open,
  onOpenChange,
  role,
  departmentOptions,
  onSubmit,
  pending,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  role: RoleItem | null
  departmentOptions: DepartmentOption[]
  onSubmit: (input: RoleInput) => Promise<void>
  pending: boolean
}) {
  const [draft, setDraft] = useState<RoleInput>(emptyDraft)
  const [error, setError] = useState("")
  // 权限点字典由后端下发。与列表页共用同一个 queryKey，只会发一次请求。
  const { data: permissionCatalog = [] } = usePermissionCatalog()
  const permissionGroups = useMemo(() => groupByKey(permissionCatalog), [permissionCatalog])
  const lockedPermissions = Boolean(role?.builtin && role.code === "ADMIN")
  // ADMIN 的数据范围固定为全部：把它改成「仅本人」会让所有管理员看不到任何数据。
  const lockedScope = lockedPermissions
  const lockedCode = Boolean(role)

  useEffect(() => {
    if (!open) return
    setDraft(
      role
        ? {
            code: role.code,
            name: role.name,
            description: role.description,
            permissions: role.permissions,
            departmentScope: role.departmentScope,
            departmentIds: role.departmentIds,
            status: role.status,
          }
        : emptyDraft,
    )
    setError("")
  }, [open, role])

  function update<K extends keyof RoleInput>(key: K, value: RoleInput[K]) {
    setDraft((current) => ({ ...current, [key]: value }))
  }

  function toggleScopeDepartment(id: number) {
    setDraft((current) => ({
      ...current,
      departmentIds: current.departmentIds.includes(id)
        ? current.departmentIds.filter((item) => item !== id)
        : [...current.departmentIds, id],
    }))
  }

  function togglePermission(key: string) {
    if (lockedPermissions) return
    setDraft((current) => ({
      ...current,
      permissions: current.permissions.includes(key)
        ? current.permissions.filter((item) => item !== key)
        : [...current.permissions, key],
    }))
  }

  async function submit() {
    if (!draft.code.trim()) { setError("请填写角色编码"); return }
    if (!draft.name.trim()) { setError("请填写角色名称"); return }
    if (!/^[A-Za-z][A-Za-z0-9_]{1,31}$/.test(draft.code.trim())) {
      setError("角色编码需以字母开头，只能包含字母、数字和下划线")
      return
    }
    if (draft.departmentScope === DATA_SCOPE_CUSTOM && draft.departmentIds.length === 0) {
      setError("选择「自定义部门」时至少要勾选一个部门")
      return
    }
    setError("")
    await onSubmit({
      ...draft,
      code: draft.code.trim().toUpperCase(),
      name: draft.name.trim(),
      // 非自定义档位不提交部门，避免切换档位后残留上一次的选择。
      departmentIds: draft.departmentScope === DATA_SCOPE_CUSTOM ? draft.departmentIds : [],
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{role ? "编辑角色" : "新建角色"}</DialogTitle>
          <DialogDescription>
            {role ? "调整名称、描述与权限点，保存后立即对相关账号生效。" : "角色编码创建后不可修改，它是权限判定的锚点。"}
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 py-2">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="grid gap-2">
              <Label htmlFor="role-code">角色编码 *</Label>
              <Input
                id="role-code"
                value={draft.code}
                disabled={lockedCode}
                placeholder="OPS"
                onChange={(event) => update("code", event.target.value)}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="role-name">角色名称 *</Label>
              <Input id="role-name" value={draft.name} onChange={(event) => update("name", event.target.value)} />
            </div>
          </div>

          <div className="grid gap-2">
            <Label htmlFor="role-desc">描述</Label>
            <Input
              id="role-desc"
              value={draft.description}
              placeholder="这个角色负责什么"
              onChange={(event) => update("description", event.target.value)}
            />
          </div>

          <div className="grid gap-3">
            <div className="flex items-center justify-between">
              <Label>权限点</Label>
              {lockedPermissions && (
                <span className="text-xs text-muted-foreground">管理员角色固定拥有全部权限</span>
              )}
            </div>
            <div className={cn("grid gap-4 rounded-lg border p-4", lockedPermissions && "opacity-70")}>
              {permissionGroups.map((group) => (
                <div key={group.key} className="grid gap-2">
                  <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{group.label}</p>
                  <div className="grid gap-2 sm:grid-cols-2">
                    {group.items.map((item) => {
                      const checked = draft.permissions.includes(item.key)
                      return (
                        <button
                          key={item.key}
                          type="button"
                          disabled={lockedPermissions}
                          onClick={() => togglePermission(item.key)}
                          className={cn(
                            "flex items-start gap-3 rounded-md border p-3 text-left transition-colors",
                            checked ? "border-primary/40 bg-primary/5" : "hover:bg-muted/60",
                            lockedPermissions && "cursor-not-allowed",
                          )}
                        >
                          <span
                            className={cn(
                              "mt-0.5 flex size-4 shrink-0 items-center justify-center rounded border",
                              checked ? "border-primary bg-primary text-primary-foreground" : "border-input",
                            )}
                          >
                            {checked && <Check className="size-3" />}
                          </span>
                          <span className="grid gap-0.5">
                            <span className="text-sm font-medium">{item.label}</span>
                            <span className="text-xs text-muted-foreground">{item.description}</span>
                          </span>
                        </button>
                      )
                    })}
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="grid gap-3">
            <div className="flex items-center justify-between">
              <Label>数据范围</Label>
              {lockedScope && (
                <span className="text-xs text-muted-foreground">管理员角色固定为全部数据</span>
              )}
            </div>
            <p className="text-xs text-muted-foreground">
              权限点决定「能不能做」，数据范围决定「能对谁做」。一个人挂多个角色时取最宽的一档。
            </p>
            <div className="grid gap-2 sm:grid-cols-2">
              {DATA_SCOPE_OPTIONS.map((option) => {
                const checked = draft.departmentScope === option.value
                return (
                  <button
                    key={option.value}
                    type="button"
                    disabled={lockedScope}
                    onClick={() => update("departmentScope", option.value)}
                    className={cn(
                      "flex items-start gap-3 rounded-md border p-3 text-left transition-colors",
                      checked ? "border-primary/40 bg-primary/5" : "hover:bg-muted/60",
                      lockedScope && "cursor-not-allowed",
                    )}
                  >
                    <span
                      className={cn(
                        "mt-0.5 flex size-4 shrink-0 items-center justify-center rounded-full border",
                        checked ? "border-primary" : "border-input",
                      )}
                    >
                      {checked && <span className="size-2 rounded-full bg-primary" />}
                    </span>
                    <span className="grid gap-0.5">
                      <span className="text-sm font-medium">{option.label}</span>
                      <span className="text-xs text-muted-foreground">{option.description}</span>
                    </span>
                  </button>
                )
              })}
            </div>
            {draft.departmentScope === DATA_SCOPE_CUSTOM && (
              <div className="grid gap-2 rounded-lg border p-3">
                <span className="text-xs text-muted-foreground">
                  勾选这个角色可以查看的部门（共 {draft.departmentIds.length} 个）
                </span>
                <div className="grid max-h-56 gap-1 overflow-y-auto sm:grid-cols-2">
                  {departmentOptions.map((item) => {
                    const checked = draft.departmentIds.includes(item.id)
                    return (
                      <button
                        key={item.id}
                        type="button"
                        onClick={() => toggleScopeDepartment(item.id)}
                        className={cn(
                          "flex items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm transition-colors hover:bg-muted/60",
                          checked && "bg-primary/5",
                        )}
                      >
                        <span
                          className={cn(
                            "flex size-4 shrink-0 items-center justify-center rounded border",
                            checked ? "border-primary bg-primary text-primary-foreground" : "border-input",
                          )}
                        >
                          {checked && <Check className="size-3" />}
                        </span>
                        <span style={{ paddingLeft: item.depth * 12 }}>{item.name}</span>
                      </button>
                    )
                  })}
                </div>
              </div>
            )}
          </div>

          <div className="flex items-center justify-between rounded-lg border p-3">
            <div>
              <Label htmlFor="role-status">启用角色</Label>
              <p className="mt-1 text-xs text-muted-foreground">
                停用后，拥有该角色的账号登录依然可用，但会失去全部业务权限
              </p>
            </div>
            <Switch
              id="role-status"
              checked={draft.status === 1}
              disabled={Boolean(role?.builtin)}
              onCheckedChange={(checked) => update("status", checked ? 1 : 0)}
            />
          </div>

          {error && <p className="text-sm text-destructive" role="alert">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>取消</Button>
          <Button onClick={submit} disabled={pending}>
            {pending ? "正在保存..." : role ? "保存修改" : "创建角色"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
