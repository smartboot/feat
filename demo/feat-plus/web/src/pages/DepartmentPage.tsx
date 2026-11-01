import { useEffect, useMemo, useState } from "react"
import { Building2, ChevronRight, CornerDownRight, Pencil, Plus, Trash2, Users } from "lucide-react"
import { toast } from "sonner"

import type { DepartmentInput, DepartmentNode } from "@/api/types"
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
import { Switch } from "@/components/ui/switch"
import { useDepartmentMutations, useDepartments } from "@/services/queries"
import { countDepartments, departmentLabelNames, findDepartment, flattenDepartments } from "@/lib/departments"
import { hasPermission } from "@/lib/permissions"
import { cn } from "@/lib/utils"

const emptyDraft: DepartmentInput = { parentId: 0, code: "", name: "", sortOrder: 0, description: "", status: 1 }

type DialogState = { mode: "create"; parentId: number } | { mode: "edit"; node: DepartmentNode } | null

export function DepartmentPage() {
  const session = useSession()
  const canWrite = hasPermission(session, "dept:write")
  const { data: tree = [], isLoading, isError, refetch } = useDepartments()
  const mutations = useDepartmentMutations()
  const [expanded, setExpanded] = useState<Set<number>>(new Set())
  const [dialog, setDialog] = useState<DialogState>(null)
  const [removing, setRemoving] = useState<DepartmentNode | null>(null)

  // 首次加载后默认展开全部节点，之后由用户手动折叠。
  useEffect(() => {
    if (!tree.length) return
    setExpanded((current) => (current.size ? current : new Set(collectIds(tree))))
  }, [tree])

  const total = useMemo(() => countDepartments(tree), [tree])

  function toggle(id: number) {
    setExpanded((current) => {
      const next = new Set(current)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  async function submit(input: DepartmentInput) {
    try {
      if (dialog?.mode === "edit") {
        await mutations.update.mutateAsync({ id: dialog.node.id, input })
        toast.success("部门已更新")
      } else {
        await mutations.create.mutateAsync(input)
        toast.success("部门已创建")
      }
      setDialog(null)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "操作失败")
    }
  }

  async function confirmRemove() {
    if (!removing) return
    try {
      await mutations.remove.mutateAsync(removing.id)
      toast.success("部门已删除")
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
          <h2 className="mt-1 text-3xl font-semibold tracking-tight">部门管理</h2>
          <p className="mt-2 text-sm text-muted-foreground">
            维护组织层级，共 {total} 个部门。按部门筛选成员时会自动包含其全部下级。
          </p>
        </div>
        {canWrite && (
          <Button onClick={() => setDialog({ mode: "create", parentId: 0 })}>
            <Plus className="size-4" />新建根部门
          </Button>
        )}
      </div>

      <Card>
        <CardContent className="p-2 sm:p-4">
          {isError ? (
            <div className="p-12 text-center">
              <p className="text-sm">加载部门失败</p>
              <Button className="mt-4" variant="outline" onClick={() => refetch()}>重新加载</Button>
            </div>
          ) : isLoading ? (
            <div className="space-y-2 p-2">
              {Array.from({ length: 6 }, (_, index) => <Skeleton key={index} className="h-9 w-full" />)}
            </div>
          ) : (
            <div className="divide-y">
              {tree.map((node) => (
                <DepartmentRow
                  key={node.id}
                  node={node}
                  depth={0}
                  expanded={expanded}
                  onToggle={toggle}
                  canWrite={canWrite}
                  onCreateChild={(parentId) => setDialog({ mode: "create", parentId })}
                  onEdit={(target) => setDialog({ mode: "edit", node: target })}
                  onRemove={setRemoving}
                />
              ))}
              {!tree.length && (
                <div className="flex flex-col items-center gap-2 py-16 text-center">
                  <Building2 className="size-6 text-muted-foreground" />
                  <p className="text-sm text-muted-foreground">还没有部门，先建一个根部门</p>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      <DepartmentFormDialog
        open={Boolean(dialog)}
        onOpenChange={(open) => { if (!open) setDialog(null) }}
        state={dialog}
        tree={tree}
        onSubmit={submit}
        pending={mutations.create.isPending || mutations.update.isPending}
      />

      <AlertDialog open={Boolean(removing)} onOpenChange={(open) => { if (!open) setRemoving(null) }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除部门？</AlertDialogTitle>
            <AlertDialogDescription>
              「{removing?.name}」将被移除。存在子部门或成员时无法删除，需要先调整层级与成员归属。
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

function DepartmentRow({
  node,
  depth,
  expanded,
  onToggle,
  canWrite,
  onCreateChild,
  onEdit,
  onRemove,
}: {
  node: DepartmentNode
  depth: number
  expanded: Set<number>
  onToggle: (id: number) => void
  canWrite: boolean
  onCreateChild: (parentId: number) => void
  onEdit: (node: DepartmentNode) => void
  onRemove: (node: DepartmentNode) => void
}) {
  const hasChildren = (node.children || []).length > 0
  const open = expanded.has(node.id)

  return (
    <>
      <div className="group flex items-center gap-2 rounded-md px-2 py-2 hover:bg-muted/50" style={{ paddingLeft: depth * 20 + 8 }}>
        <button
          type="button"
          className={cn("flex size-6 items-center justify-center rounded text-muted-foreground", !hasChildren && "invisible")}
          onClick={() => onToggle(node.id)}
          aria-label={open ? "折叠" : "展开"}
        >
          <ChevronRight className={cn("size-4 transition-transform", open && "rotate-90")} />
        </button>
        {depth > 0 && <CornerDownRight className="size-3.5 shrink-0 text-muted-foreground/60" />}
        <span className="text-sm font-medium">{node.name}</span>
        {node.code && <Badge variant="outline" className="font-mono text-[10px]">{node.code}</Badge>}
        {node.status === 0 && <Badge variant="warning" className="text-[10px]">已停用</Badge>}
        <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
          <Users className="size-3.5" />{node.userCount}
        </span>
        {node.description && (
          <span className="hidden truncate text-xs text-muted-foreground sm:inline">· {node.description}</span>
        )}
        {canWrite && (
          <div className="ml-auto flex items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100 focus-within:opacity-100">
            <Button variant="ghost" size="sm" onClick={() => onCreateChild(node.id)}>
              <Plus className="size-3.5" />子部门
            </Button>
            <Button variant="ghost" size="icon" aria-label="编辑部门" onClick={() => onEdit(node)}>
              <Pencil className="size-3.5" />
            </Button>
            <Button variant="ghost" size="icon" aria-label="删除部门" onClick={() => onRemove(node)}>
              <Trash2 className="size-3.5" />
            </Button>
          </div>
        )}
      </div>
      {open &&
        (node.children || []).map((child) => (
          <DepartmentRow
            key={child.id}
            node={child}
            depth={depth + 1}
            expanded={expanded}
            onToggle={onToggle}
            canWrite={canWrite}
            onCreateChild={onCreateChild}
            onEdit={onEdit}
            onRemove={onRemove}
          />
        ))}
    </>
  )
}

function DepartmentFormDialog({
  open,
  onOpenChange,
  state,
  tree,
  onSubmit,
  pending,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  state: DialogState
  tree: DepartmentNode[]
  onSubmit: (input: DepartmentInput) => Promise<void>
  pending: boolean
}) {
  const [draft, setDraft] = useState<DepartmentInput>(emptyDraft)
  const [error, setError] = useState("")
  const editing = state?.mode === "edit" ? state.node : null

  const options = useMemo(
    () => flattenDepartments(tree, { excludeId: editing?.id }),
    [tree, editing?.id],
  )

  useEffect(() => {
    if (!open) return
    if (state?.mode === "edit") {
      const node = findDepartment(tree, state.node.id) ?? state.node
      setDraft({
        parentId: node.parentId,
        code: node.code,
        name: node.name,
        sortOrder: node.sortOrder,
        description: node.description,
        status: node.status,
      })
    } else {
      setDraft({ ...emptyDraft, parentId: state?.mode === "create" ? state.parentId : 0 })
    }
    setError("")
  }, [open, state, tree])

  function update<K extends keyof DepartmentInput>(key: K, value: DepartmentInput[K]) {
    setDraft((current) => ({ ...current, [key]: value }))
  }

  async function submit() {
    if (!draft.name.trim()) { setError("请填写部门名称"); return }
    if (draft.code && !/^[A-Za-z0-9_-]{1,32}$/.test(draft.code.trim())) {
      setError("部门编码只能包含字母、数字、下划线和短横线")
      return
    }
    setError("")
    await onSubmit({
      ...draft,
      name: draft.name.trim(),
      code: draft.code.trim().toUpperCase(),
      sortOrder: Number.isFinite(draft.sortOrder) ? draft.sortOrder : 0,
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{editing ? "编辑部门" : "新建部门"}</DialogTitle>
          <DialogDescription>
            {editing ? "调整名称、上级或排序。不能把部门移动到自己的下级里。" : "选择一个上级部门，或直接建在根上。"}
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 py-2">
          <div className="grid gap-2">
            <Label>上级部门</Label>
            <Select
              value={String(draft.parentId)}
              onValueChange={(value) => update("parentId", Number(value))}
            >
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="0">无（作为根部门）</SelectItem>
                {options.map((option) => (
                  <SelectItem key={option.id} value={String(option.id)} disabled={option.disabled}>
                    {departmentLabelNames(option.name, option.depth)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="grid gap-2">
              <Label htmlFor="dept-name">部门名称 *</Label>
              <Input id="dept-name" value={draft.name} onChange={(event) => update("name", event.target.value)} />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="dept-code">部门编码</Label>
              <Input
                id="dept-code"
                value={draft.code}
                placeholder="TECH_RD"
                onChange={(event) => update("code", event.target.value)}
              />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="grid gap-2">
              <Label htmlFor="dept-order">排序</Label>
              <Input
                id="dept-order"
                type="number"
                value={String(draft.sortOrder)}
                onChange={(event) => update("sortOrder", Number(event.target.value))}
              />
            </div>
            <div className="flex items-center justify-between rounded-lg border p-3">
              <div>
                <Label htmlFor="dept-status">启用</Label>
                <p className="mt-1 text-xs text-muted-foreground">停用后不能再分配成员</p>
              </div>
              <Switch
                id="dept-status"
                checked={draft.status === 1}
                onCheckedChange={(checked) => update("status", checked ? 1 : 0)}
              />
            </div>
          </div>

          <div className="grid gap-2">
            <Label htmlFor="dept-desc">描述</Label>
            <Input
              id="dept-desc"
              value={draft.description}
              placeholder="这个部门负责什么"
              onChange={(event) => update("description", event.target.value)}
            />
          </div>

          {error && <p className="text-sm text-destructive" role="alert">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>取消</Button>
          <Button onClick={submit} disabled={pending}>
            {pending ? "正在保存..." : editing ? "保存修改" : "创建部门"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function collectIds(nodes: DepartmentNode[]): number[] {
  const ids: number[] = []
  for (const node of nodes) {
    ids.push(node.id)
    ids.push(...collectIds(node.children || []))
  }
  return ids
}
