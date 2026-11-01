import { useEffect, useMemo, useState } from "react"
import {
  ArrowRight,
  Check,
  Ellipsis,
  Filter,
  Mail,
  MoreHorizontal,
  Pencil,
  Plus,
  RotateCcw,
  Search,
  ShieldCheck,
  Trash2,
  UserRound,
  UserRoundCheck,
  UserRoundX,
} from "lucide-react"
import { useNavigate, useParams, useSearchParams } from "react-router-dom"
import { toast } from "sonner"

import type { DepartmentOption, RoleItem, UserInput, UserListItem, UserQuery, UserStatus } from "@/api/types"
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
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
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
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet"
import { Skeleton } from "@/components/ui/skeleton"
import { Switch } from "@/components/ui/switch"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { departmentLabelNames, flattenDepartments } from "@/lib/departments"
import { hasPermission } from "@/lib/permissions"
import { useAllUsers, useDepartments, useRoles, useUser, useUserMutations, useUsers } from "@/services/queries"

const PAGE_SIZE = 7

/** departmentId 为 null 时下拉用这个哨兵占位，因为 Select 的 value 不接受 null。 */
const NO_DEPARTMENT = "none"

const emptyDraft: UserInput = {
  username: "",
  displayName: "",
  email: "",
  departmentId: null,
  roleCodes: [],
  status: 1,
}

export function UserListPage() {
  const navigate = useNavigate()
  const params = useParams<{ userId?: string }>()
  const [searchParams, setSearchParams] = useSearchParams()
  const session = useSession()
  const canWrite = hasPermission(session, "user:write")

  const [keyword, setKeyword] = useState(searchParams.get("q") || "")
  const [status, setStatus] = useState<"all" | "0" | "1">((searchParams.get("status") as "all" | "0" | "1") || "all")
  const [roleCode, setRoleCode] = useState<string>(searchParams.get("role") || "all")
  const [departmentId, setDepartmentId] = useState<"all" | number>(() => {
    const raw = searchParams.get("department")
    return raw ? Number(raw) : "all"
  })
  const [page, setPage] = useState(Number(searchParams.get("page") || 1))
  const [pageSize, setPageSize] = useState(Number(searchParams.get("pageSize") || PAGE_SIZE))
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<UserListItem | null>(null)
  const [detailId, setDetailId] = useState<number | null>(params.userId ? Number(params.userId) : null)
  const [actionUser, setActionUser] = useState<UserListItem | null>(null)
  const [action, setAction] = useState<"status" | "delete" | "reset" | null>(null)
  const [resetPassword, setResetPassword] = useState("")
  const [filtersOpen, setFiltersOpen] = useState(false)

  const query: UserQuery = { page, pageSize, keyword, status, roleCode, departmentId }
  const { data, isLoading, isFetching, isError, refetch } = useUsers(query)
  const { data: allUsers = [] } = useAllUsers()
  const { data: roles = [] } = useRoles()
  const { data: departments = [] } = useDepartments()
  const mutations = useUserMutations()
  const { data: detailUser } = useUser(detailId || 0, Boolean(detailId))
  const pageCount = Math.max(1, Math.ceil((data?.total || 0) / pageSize))

  const departmentOptions = useMemo(() => flattenDepartments(departments), [departments])

  useEffect(() => {
    const nextId = params.userId ? Number(params.userId) : null
    setDetailId(nextId)
  }, [params.userId])

  useEffect(() => {
    const next = new URLSearchParams()
    if (keyword) next.set("q", keyword)
    if (status !== "all") next.set("status", status)
    if (roleCode !== "all") next.set("role", roleCode)
    if (departmentId !== "all") next.set("department", String(departmentId))
    if (page > 1) next.set("page", String(page))
    if (pageSize !== PAGE_SIZE) next.set("pageSize", String(pageSize))
    setSearchParams(next, { replace: true })
  }, [keyword, status, roleCode, departmentId, page, pageSize, setSearchParams])

  function resetPage() {
    setPage(1)
  }

  function clearFilters() {
    setKeyword("")
    setStatus("all")
    setRoleCode("all")
    setDepartmentId("all")
    resetPage()
  }

  function openCreate() {
    setEditingUser(null)
    setDialogOpen(true)
  }

  function openEdit(user: UserListItem) {
    setEditingUser(user)
    setDialogOpen(true)
  }

  function openDetails(user: UserListItem) {
    setDetailId(user.id)
    navigate(`/users/${user.id}?${searchParams.toString()}`)
  }

  function closeDetails() {
    setDetailId(null)
    navigate(`/users?${searchParams.toString()}`)
  }

  function beginAction(user: UserListItem, nextAction: "status" | "delete" | "reset") {
    setActionUser(user)
    setAction(nextAction)
  }

  async function submitDraft(input: UserInput) {
    try {
      if (editingUser) {
        await mutations.update.mutateAsync({ id: editingUser.id, input })
        toast.success("用户信息已更新")
      } else {
        const created = await mutations.create.mutateAsync(input)
        toast.success("用户已创建")
        openDetails(created)
      }
      setDialogOpen(false)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "操作失败")
    }
  }

  async function confirmAction() {
    if (!actionUser || !action) return
    try {
      if (action === "status") {
        await mutations.status.mutateAsync({ id: actionUser.id, status: actionUser.status ? 0 : 1 })
        toast.success(actionUser.status ? "账号已停用" : "账号已启用")
      }
      if (action === "delete") {
        await mutations.remove.mutateAsync(actionUser.id)
        toast.success("用户已删除")
        if (detailId === actionUser.id) closeDetails()
      }
      if (action === "reset") {
        const result = await mutations.resetPassword.mutateAsync(actionUser.id)
        setResetPassword(result.temporaryPassword)
        toast.success("临时密码已生成")
      }
      setAction(null)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "操作失败")
    }
  }

  const activeFilterCount = [status !== "all", roleCode !== "all", departmentId !== "all"].filter(Boolean).length
  const currentItems = data?.items || []

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="text-sm text-muted-foreground">系统管理</p>
          <h2 className="mt-1 text-3xl font-semibold tracking-tight">用户管理</h2>
          <p className="mt-2 text-sm text-muted-foreground">维护组织成员、账号状态与角色归属。</p>
        </div>
        {canWrite && (
          <Button onClick={openCreate}>
            <Plus className="size-4" />
            新建用户
          </Button>
        )}
      </div>

      <Card>
        <CardContent className="flex flex-col gap-3 p-3 md:flex-row md:items-center md:justify-between">
          <div className="relative w-full md:max-w-md">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              className="pl-9"
              aria-label="搜索用户"
              placeholder="搜索用户名、姓名、邮箱或部门"
              value={keyword}
              onChange={(event) => {
                setKeyword(event.target.value)
                resetPage()
              }}
            />
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Select
              value={status}
              onValueChange={(value) => {
                setStatus(value as "all" | "0" | "1")
                resetPage()
              }}
            >
              <SelectTrigger className="w-[130px]">
                <SelectValue placeholder="账号状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">全部状态</SelectItem>
                <SelectItem value="1">已启用</SelectItem>
                <SelectItem value="0">已停用</SelectItem>
              </SelectContent>
            </Select>
            <Popover open={filtersOpen} onOpenChange={setFiltersOpen}>
              <PopoverTrigger asChild>
                <Button variant="outline" className="gap-2">
                  <Filter className="size-4" />
                  更多筛选
                  {activeFilterCount > 0 && (
                    <Badge className="ml-1 size-5 justify-center rounded-full p-0 text-[10px]">
                      {activeFilterCount}
                    </Badge>
                  )}
                </Button>
              </PopoverTrigger>
              <PopoverContent align="end" className="w-72">
                <div className="space-y-4">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="font-medium">高级筛选</p>
                      <p className="text-xs text-muted-foreground">缩小成员列表范围</p>
                    </div>
                    {activeFilterCount > 0 && (
                      <Button variant="ghost" size="sm" className="h-7 px-2 text-xs" onClick={clearFilters}>
                        清除
                      </Button>
                    )}
                  </div>
                  <div className="grid gap-2">
                    <Label>角色</Label>
                    <Select
                      value={roleCode}
                      onValueChange={(value) => {
                        setRoleCode(value)
                        resetPage()
                      }}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">全部角色</SelectItem>
                        {roles.map((role) => (
                          <SelectItem key={role.id} value={role.code}>
                            {role.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="grid gap-2">
                    <Label>部门</Label>
                    <Select
                      value={departmentId === "all" ? "all" : String(departmentId)}
                      onValueChange={(value) => {
                        setDepartmentId(value === "all" ? "all" : Number(value))
                        resetPage()
                      }}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">全部部门</SelectItem>
                        {departmentOptions.map((item) => (
                          <SelectItem key={item.id} value={String(item.id)}>
                            {departmentLabelNames(item.name, item.depth)}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <p className="text-xs text-muted-foreground">选中上级部门时会包含其全部下级</p>
                  </div>
                </div>
              </PopoverContent>
            </Popover>
          </div>
        </CardContent>
      </Card>

      <Card className="overflow-hidden">
        <CardHeader className="flex-row items-center justify-between space-y-0 border-b px-5 py-4">
          <div>
            <CardTitle className="text-base">成员列表</CardTitle>
            <p className="mt-1 text-xs text-muted-foreground">
              共 {data?.total || 0} 位成员{isFetching && <span className="ml-2 text-primary">正在更新...</span>}
            </p>
          </div>
          <Badge variant="outline">实时数据</Badge>
        </CardHeader>

        {isError ? (
          <div className="p-12 text-center">
            <p className="text-sm">加载用户失败</p>
            <Button className="mt-4" variant="outline" onClick={() => refetch()}>
              重新加载
            </Button>
          </div>
        ) : (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>用户</TableHead>
                  <TableHead>部门</TableHead>
                  <TableHead>角色</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead>加入时间</TableHead>
                  <TableHead>
                    <span className="sr-only">操作</span>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {isLoading
                  ? Array.from({ length: 5 }, (_, index) => (
                      <TableRow key={index}>
                        <TableCell>
                          <div className="flex items-center gap-3">
                            <Skeleton className="size-9 rounded-lg" />
                            <div className="space-y-2">
                              <Skeleton className="h-3 w-24" />
                              <Skeleton className="h-3 w-36" />
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Skeleton className="h-3 w-20" />
                        </TableCell>
                        <TableCell>
                          <Skeleton className="h-5 w-16" />
                        </TableCell>
                        <TableCell>
                          <Skeleton className="h-5 w-16" />
                        </TableCell>
                        <TableCell>
                          <Skeleton className="h-3 w-28" />
                        </TableCell>
                        <TableCell />
                      </TableRow>
                    ))
                  : currentItems.map((user) => (
                      <TableRow key={user.id}>
                        <TableCell>
                          <button
                            className="flex items-center gap-3 text-left outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                            onClick={() => openDetails(user)}
                          >
                            <Avatar>
                              <AvatarFallback>{user.displayName.slice(0, 1)}</AvatarFallback>
                            </Avatar>
                            <span className="grid">
                              <strong className="text-sm">{user.displayName}</strong>
                              <span className="text-xs text-muted-foreground">
                                {user.username} · {user.email}
                              </span>
                            </span>
                          </button>
                        </TableCell>
                        <TableCell className="whitespace-nowrap text-sm">
                          {user.departmentName || <span className="text-muted-foreground">未分配</span>}
                        </TableCell>
                        <TableCell>
                          <span className="flex flex-wrap gap-1">
                            {user.roleNames.length === 0
                              ? <Badge variant="secondary">{user.roleCodes.join(", ") || "无角色"}</Badge>
                              : user.roleNames.map((name) => <Badge key={name} variant="secondary">{name}</Badge>)}
                          </span>
                        </TableCell>
                        <TableCell>
                          <Badge variant={user.status ? "success" : "warning"}>{user.status ? "已启用" : "已停用"}</Badge>
                        </TableCell>
                        <TableCell className="whitespace-nowrap text-sm text-muted-foreground">{user.createdTime}</TableCell>
                        <TableCell className="text-right">
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="icon" aria-label={`${user.displayName} 的操作`}>
                                <MoreHorizontal className="size-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem onClick={() => openDetails(user)}>
                                <UserRound />
                                查看详情
                              </DropdownMenuItem>
                              {canWrite && (
                                <>
                                  <DropdownMenuItem onClick={() => openEdit(user)}>
                                    <Pencil />
                                    编辑
                                  </DropdownMenuItem>
                                  <DropdownMenuItem onClick={() => beginAction(user, "status")}>
                                    {user.status ? <UserRoundX /> : <UserRoundCheck />}
                                    {user.status ? "停用账号" : "启用账号"}
                                  </DropdownMenuItem>
                                  <DropdownMenuItem onClick={() => beginAction(user, "reset")}>
                                    <RotateCcw />
                                    重置密码
                                  </DropdownMenuItem>
                                  <DropdownMenuSeparator />
                                  <DropdownMenuItem destructive onClick={() => beginAction(user, "delete")}>
                                    <Trash2 />
                                    删除
                                  </DropdownMenuItem>
                                </>
                              )}
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                    ))}
              </TableBody>
            </Table>

            {!isLoading && currentItems.length === 0 && (
              <div className="p-12 text-center">
                <p className="text-sm font-medium">没有匹配的成员</p>
                <p className="mt-1 text-xs text-muted-foreground">调整筛选条件，或者清空后重新查看全部成员。</p>
                <Button className="mt-4" variant="outline" onClick={clearFilters}>
                  清除筛选
                </Button>
              </div>
            )}

            <div className="flex flex-col items-center justify-between gap-3 border-t px-5 py-3 sm:flex-row">
              <p className="text-xs text-muted-foreground">
                第 {page} / {pageCount} 页 · 每页
                <select
                  className="mx-1 rounded border bg-transparent px-1 py-0.5"
                  value={pageSize}
                  onChange={(event) => {
                    setPageSize(Number(event.target.value))
                    resetPage()
                  }}
                >
                  {[7, 10, 20].map((size) => (
                    <option key={size} value={size}>
                      {size}
                    </option>
                  ))}
                </select>
                条
              </p>
              <div className="flex items-center gap-2">
                <Button variant="outline" size="sm" disabled={page <= 1} onClick={() => setPage((value) => Math.max(1, value - 1))}>
                  上一页
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page >= pageCount}
                  onClick={() => setPage((value) => Math.min(pageCount, value + 1))}
                >
                  下一页
                  <ArrowRight className="size-4" />
                </Button>
              </div>
            </div>
          </>
        )}
      </Card>

      <UserFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        user={editingUser}
        roles={roles}
        departmentOptions={departmentOptions}
        onSubmit={submitDraft}
        pending={mutations.create.isPending || mutations.update.isPending}
      />

      <UserDetailSheet
        user={detailUser}
        roles={roles}
        canWrite={canWrite}
        open={Boolean(detailId)}
        onOpenChange={(open) => {
          if (!open) closeDetails()
        }}
        onEdit={openEdit}
        onAction={beginAction}
      />

      <AlertDialog open={Boolean(action && action !== "reset")} onOpenChange={(open) => { if (!open) setAction(null) }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {action === "delete" ? "确认删除用户？" : actionUser?.status ? "确认停用账号？" : "确认启用账号？"}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {action === "delete"
                ? `将删除 ${actionUser?.displayName}（${actionUser?.username}），账号会从列表中隐藏且无法再登录，历史记录保留。`
                : `${actionUser?.displayName} 将${actionUser?.status ? "无法登录工作空间" : "恢复访问工作空间"}。`}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction
              className={action === "delete" ? "bg-destructive text-white hover:bg-destructive/90" : ""}
              onClick={confirmAction}
            >
              {action === "delete" ? "确认删除" : "确认操作"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <Dialog open={Boolean(resetPassword)} onOpenChange={(open) => { if (!open) setResetPassword("") }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>临时密码已生成</DialogTitle>
            <DialogDescription>请安全地将临时密码交给用户。关闭后不会再次显示。</DialogDescription>
          </DialogHeader>
          <div className="rounded-lg border bg-muted/50 p-4 text-center font-mono text-lg tracking-widest">
            {resetPassword}
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => {
                navigator.clipboard?.writeText(resetPassword)
                toast.success("临时密码已复制")
              }}
            >
              复制密码
            </Button>
            <Button onClick={() => setResetPassword("")}>完成</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

function UserFormDialog({
  open,
  onOpenChange,
  user,
  roles,
  departmentOptions,
  onSubmit,
  pending,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  user: UserListItem | null
  roles: RoleItem[]
  departmentOptions: DepartmentOption[]
  onSubmit: (input: UserInput) => Promise<void>
  pending: boolean
}) {
  const [draft, setDraft] = useState<UserInput>(emptyDraft)
  const [error, setError] = useState("")

  useEffect(() => {
    if (!open) return
    // 新建时默认勾选第一个可用角色，避免提交空角色被后端拒绝。
    const fallbackRole = roles.find((role) => role.status === 1)?.code
    setDraft(
      user
        ? {
            username: user.username,
            displayName: user.displayName,
            email: user.email,
            departmentId: user.departmentId,
            roleCodes: user.roleCodes,
            status: user.status,
          }
        : {
            ...emptyDraft,
            roleCodes: fallbackRole ? [fallbackRole] : [],
            departmentId: departmentOptions[0]?.id ?? null,
          },
    )
    setError("")
  }, [open, user, roles, departmentOptions])

  function update<K extends keyof UserInput>(key: K, value: UserInput[K]) {
    setDraft((current) => ({ ...current, [key]: value }))
  }

  async function submit() {
    if (!draft.username.trim() || !draft.displayName.trim() || !draft.email.trim()) {
      setError("请完整填写必填字段")
      return
    }
    if (draft.roleCodes.length === 0) {
      setError("请至少选择一个角色")
      return
    }
    if (draft.departmentId === null) {
      setError("请选择所属部门")
      return
    }
    if (!/^\S+@\S+\.\S+$/.test(draft.email)) {
      setError("请输入有效的邮箱地址")
      return
    }
    setError("")
    await onSubmit({
      ...draft,
      username: draft.username.trim(),
      displayName: draft.displayName.trim(),
      email: draft.email.trim(),
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{user ? "编辑用户" : "新建用户"}</DialogTitle>
          <DialogDescription>{user ? "更新成员资料和账号归属。" : "为工作空间添加一位新成员。"}</DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 py-2">
          <div className="grid gap-2">
            <Label htmlFor="user-username">用户名 *</Label>
            <Input
              id="user-username"
              value={draft.username}
              onChange={(event) => update("username", event.target.value)}
              disabled={Boolean(user)}
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="user-display-name">姓名 *</Label>
            <Input
              id="user-display-name"
              value={draft.displayName}
              onChange={(event) => update("displayName", event.target.value)}
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="user-email">邮箱 *</Label>
            <Input
              id="user-email"
              type="email"
              value={draft.email}
              onChange={(event) => update("email", event.target.value)}
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="user-department">部门 *</Label>
            <Select
              value={draft.departmentId === null ? NO_DEPARTMENT : String(draft.departmentId)}
              onValueChange={(value) => update("departmentId", value === NO_DEPARTMENT ? null : Number(value))}
            >
              <SelectTrigger id="user-department">
                <SelectValue placeholder="选择部门" />
              </SelectTrigger>
              <SelectContent>
                {departmentOptions.map((item) => (
                  <SelectItem key={item.id} value={String(item.id)}>
                    {departmentLabelNames(item.name, item.depth)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="grid gap-2">
            <Label>角色 *</Label>
            <div className="flex flex-wrap gap-2">
              {roles
                .filter((role) => role.status === 1 || draft.roleCodes.includes(role.code))
                .map((role) => {
                  const active = draft.roleCodes.includes(role.code)
                  return (
                    <button
                      key={role.id}
                      type="button"
                      aria-pressed={active}
                      onClick={() =>
                        update(
                          "roleCodes",
                          active
                            ? draft.roleCodes.filter((code) => code !== role.code)
                            : [...draft.roleCodes, role.code],
                        )
                      }
                      className={[
                        "inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs transition-colors",
                        active
                          ? "border-primary bg-primary/10 text-primary"
                          : "border-border text-muted-foreground hover:border-primary/40 hover:text-foreground",
                      ].join(" ")}
                    >
                      {active && <Check className="size-3" />}
                      {role.name}
                    </button>
                  )
                })}
            </div>
            <p className="text-xs text-muted-foreground">
              可以多选：权限点取并集，数据范围取最宽的一档
            </p>
          </div>
          <div className="flex items-center justify-between rounded-lg border p-3">
            <div>
              <Label htmlFor="user-status">启用账号</Label>
              <p className="mt-1 text-xs text-muted-foreground">停用后用户将无法登录</p>
            </div>
            <Switch
              id="user-status"
              checked={draft.status === 1}
              onCheckedChange={(checked) => update("status", (checked ? 1 : 0) as UserStatus)}
            />
          </div>
          {error && (
            <p className="text-sm text-destructive" role="alert">
              {error}
            </p>
          )}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button onClick={submit} disabled={pending}>
            <Check className="size-4" />
            {user ? "保存修改" : "创建用户"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function UserDetailSheet({
  user,
  roles,
  canWrite,
  open,
  onOpenChange,
  onEdit,
  onAction,
}: {
  user?: UserListItem
  roles: RoleItem[]
  canWrite: boolean
  open: boolean
  onOpenChange: (open: boolean) => void
  onEdit: (user: UserListItem) => void
  onAction: (user: UserListItem, action: "status" | "delete" | "reset") => void
}) {
  if (!user) return null
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent>
        <SheetHeader>
          <div className="flex items-center gap-3">
            <Avatar className="size-12">
              <AvatarFallback className="text-base">{user.displayName.slice(0, 1)}</AvatarFallback>
            </Avatar>
            <div>
              <SheetTitle>{user.displayName}</SheetTitle>
              <SheetDescription>{user.username}</SheetDescription>
            </div>
          </div>
        </SheetHeader>
        <div className="mt-8 space-y-6">
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">账号状态</span>
            <Badge variant={user.status ? "success" : "warning"}>{user.status ? "已启用" : "已停用"}</Badge>
          </div>
          <div className="grid gap-4 rounded-lg border p-4 text-sm">
            <DetailRow icon={<Mail />} label="邮箱" value={user.email} />
            <DetailRow icon={<ShieldCheck />} label="角色" value={user.roleNames.join(" / ") || "未分配"} />
            <DetailRow icon={<UserRound />} label="部门" value={user.departmentName || "未分配"} />
            <DetailRow icon={<Ellipsis />} label="加入时间" value={user.createdTime} />
            <DetailRow icon={<Ellipsis />} label="最后修改" value={user.editTime} />
          </div>
          {canWrite && (
            <div className="grid gap-2">
              <Button onClick={() => onEdit(user)}>
                <Pencil className="size-4" />
                编辑用户
              </Button>
              <Button variant="outline" onClick={() => onAction(user, "status")}>
                {user.status ? <UserRoundX className="size-4" /> : <UserRoundCheck className="size-4" />}
                {user.status ? "停用账号" : "启用账号"}
              </Button>
              <Button variant="outline" onClick={() => onAction(user, "reset")}>
                <RotateCcw className="size-4" />
                重置密码
              </Button>
            </div>
          )}
        </div>
      </SheetContent>
    </Sheet>
  )
}

function DetailRow({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-center gap-3">
      <span className="text-muted-foreground [&_svg]:size-4">{icon}</span>
      <span className="w-16 shrink-0 text-muted-foreground">{label}</span>
      <strong className="truncate">{value}</strong>
    </div>
  )
}

