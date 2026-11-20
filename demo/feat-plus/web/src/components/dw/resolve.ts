import type { DwFormConfig, DwFormField, DwListConfig, DwOption } from "@/api/types"

/**
 * 渲染前的「值域合并」：把页面级的字段值域铺到各视图的空位上。
 *
 * ## 为什么必须有这一层
 *
 * 线上拿到的是后端已经合并过的**下发副本**（`PageService.applyFieldMaps`），
 * 配置页预览拿到的却是编辑器里的**原始 draft**。两侧各自实现一遍合并规则，
 * 预览就只是「长得像」而不是「就是」——最典型的表现是：在「字段」分区配好一份
 * 13 条的值域，右侧预览纹丝不动，配置者以为没生效，回列表里再配一份，
 * 于是同一个字段有了两份会互相漂移的映射。
 *
 * 规则与后端逐条对齐，只有一条：**视图自己配了就用视图的**（那是覆盖，不是重复）。
 * 唯一例外是配了 `optionsApi` 的筛选与表单字段——远程选项与静态值域同时生效时
 * 谁赢说不清，所以配了接口就不再继承。
 *
 * ## 返回值为什么可以「还是原来那个对象」
 *
 * 两处都没配值域时原样返回同一个引用，让上层的 `useMemo` / `memo` 能继续生效。
 * 合并只发生在这里一次，渲染面拿到的就是最终值，没有第二处需要记住这条规则。
 */
export function resolveListConfig(list: DwListConfig): DwListConfig {
  const maps = list.fieldMaps ?? {}
  if (Object.keys(maps).length === 0) return list

  let changed = false
  const columns = list.columns.map((column) => {
    const inherited = inheritOptions(column.options, maps[column.key])
    if (!inherited) return column
    changed = true
    return { ...column, options: inherited }
  })
  const filters = list.filters.map((filter) => {
    // 与后端 `!filter.getOptionsApi().isEmpty()` 同义，不用 trim：判空口径必须逐字一致
    if ((filter.optionsApi ?? "") !== "") return filter
    const inherited = inheritOptions(filter.options, maps[filter.key])
    if (!inherited) return filter
    changed = true
    return { ...filter, options: inherited }
  })

  return changed ? { ...list, columns, filters } : list
}

/**
 * 表单同理。表单字段多一个 `optionsApi` 来源，规则与筛选一致。
 *
 * 接收 `list` 而不是 `fieldMaps`：调用方手上通常是一个 draft，多传一个拆开的
 * 字段只会让「这张表单属于哪个列表」这层关系在签名里消失。
 */
export function resolveFormConfig(list: DwListConfig, form: DwFormConfig): DwFormConfig {
  const maps = list.fieldMaps ?? {}
  if (Object.keys(maps).length === 0) return form

  let changed = false
  const fields = form.fields.map((field: DwFormField) => {
    if ((field.optionsApi ?? "") !== "") return field
    const inherited = inheritOptions(field.options, maps[field.key])
    if (!inherited) return field
    changed = true
    return { ...field, options: inherited }
  })

  return changed ? { ...form, fields } : form
}

/** 视图自己配了 → 不继承；视图为空且字段配了 → 用字段的（复制一份，不共享数组）。 */
function inheritOptions(own: DwOption[] | undefined, mapped: DwOption[] | undefined): DwOption[] | null {
  if (own && own.length > 0) return null
  if (!mapped || mapped.length === 0) return null
  return [...mapped]
}
