/**
 * 权限判断。字典与展示信息都不在这里——它们由后端 GET /api/permissions 下发。
 *
 * 前端不再维护权限点常量，理由：一份字典两处维护时，漏改前端的表现是「角色页里配不出这个点，
 * 接口永远 403」，不报错、只报障。现在新增权限点只改后端 Permission.CATALOG 一处。
 *
 * 代价要认：这里收 string 而不是联合类型，拼错 key 不会编译失败，只会让判断恒为 false。
 * 兜底是 scripts/check_menu.py —— 菜单与注册表里出现的 key 必须存在于后端字典。
 */

export function hasPermission(
  user: { permissions: string[] } | null | undefined,
  key: string,
): boolean {
  if (!user) return false
  return user.permissions.includes(key)
}

export function hasAnyPermission(
  user: { permissions: string[] } | null | undefined,
  keys: string[],
): boolean {
  return keys.some((key) => hasPermission(user, key))
}
