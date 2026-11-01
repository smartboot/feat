import type { DepartmentNode, DepartmentOption } from "@/api/types"

/**
 * 把部门树拍平成下拉选项，depth 用来做层级缩进。
 * 传 excludeId 时会把该节点及其全部后代标记为不可选——编辑部门时不能把节点挂到自己的子树里。
 */
export function flattenDepartments(
  nodes: DepartmentNode[],
  options: { depth?: number; excludeId?: number } = {},
  out: DepartmentOption[] = [],
): DepartmentOption[] {
  const depth = options.depth ?? 0
  for (const node of nodes) {
    const blocked = options.excludeId !== undefined && isSelfOrDescendant(node, options.excludeId)
    out.push({ id: node.id, name: node.name, depth, disabled: blocked || node.status !== 1 })
    flattenDepartments(node.children || [], { depth: depth + 1, excludeId: options.excludeId }, out)
  }
  return out
}

/** node 是否就是 target，或者 target 在 node 的子树里。 */
export function isSelfOrDescendant(node: DepartmentNode, targetId: number): boolean {
  if (node.id === targetId) return true
  return (node.children || []).some((child) => isSelfOrDescendant(child, targetId))
}

export function findDepartment(nodes: DepartmentNode[], id: number): DepartmentNode | undefined {
  for (const node of nodes) {
    if (node.id === id) return node
    const hit = findDepartment(node.children || [], id)
    if (hit) return hit
  }
  return undefined
}

/** 部门树的节点总数，用于标题栏与空状态判断。 */
export function countDepartments(nodes: DepartmentNode[]): number {
  return nodes.reduce((total, node) => total + 1 + countDepartments(node.children || []), 0)
}

/** 层级缩进前缀，用全角空格保证在 option 里也能对齐。 */
export function departmentLabelNames(name: string, depth: number): string {
  return depth === 0 ? name : `${"　".repeat(depth)}${name}`
}
