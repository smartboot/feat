import type {
  DatasourceOption,
  DwAction,
  DwColumn,
  DwColumnFormat,
  DwFieldMeta,
  DwFilterWidget,
  DwFormConfig,
  DwFormField,
  DwListConfig,
  DwRow,
  DwVariant,
  DwWidget,
  DwWriteMode,
} from "@/api/types"

/**
 * 页面配置的「人类语言层」。
 *
 * 配置的枚举值是给后端校验用的（text / badge / span / span=2），界面上一律给业务语言
 * （文本 / 状态标签 / 只读展示 / 半宽）。这一层是唯一映射点：加一种列格式、换一种宽度档位，
 * 只改这里，列表编辑器、表单编辑器、预览、问题面板同时生效。
 *
 * 为什么值得单独一层：企业里配页面的人通常不是前端。让他填 `w-36`，配置就只可能由
 * 写这个框架的人来维护——「主力功能」也就到此为止了。
 */

export const COLUMN_FORMATS: DwColumnFormat[] = ["text", "muted", "badge", "tags", "number"]

export const COLUMN_FORMAT_LABELS: Record<DwColumnFormat, string> = {
  text: "文本",
  muted: "次要文本",
  badge: "状态标签",
  tags: "多值标签",
  number: "数字",
}

export const COLUMN_FORMAT_HINTS: Record<DwColumnFormat, string> = {
  text: "原样展示，最主要的列格式",
  muted: "弱化的辅助信息，如编码、说明",
  badge: "单个状态值 → 彩色标签，需要值映射",
  tags: "多值字段，如权限点、标签，超出 4 个折叠",
  number: "右对齐等宽数字，如成员数、金额",
}

export const FILTER_WIDGETS: DwFilterWidget[] = ["input", "select", "dateRange"]

export const FILTER_WIDGET_LABELS: Record<DwFilterWidget, string> = {
  input: "文本（模糊匹配）",
  select: "下拉（精确匹配）",
  dateRange: "日期范围",
}

export const FILTER_WIDGET_HINTS: Record<DwFilterWidget, string> = {
  input: "单框输入，值原样交给数据源做模糊匹配",
  select: "选项可以写死在配置里，也可以指向一个接口按需取（角色、部门、操作人这类会变的）",
  dateRange: "起止两个日期，占两个筛选键。结束日期含当天，由数据源解释",
}

export const WIDGETS: DwWidget[] = ["input", "password", "textarea", "select", "switch", "span"]

export const FORM_WIDGET_LABELS: Record<DwWidget, string> = {
  input: "单行文本",
  password: "密码",
  textarea: "多行文本",
  select: "下拉选择",
  switch: "开关",
  span: "只读展示",
}

export const FORM_WIDGET_HINTS: Record<DwWidget, string> = {
  input: "最常用；可配占位、默认值、正则校验",
  password: "输入内容不回显，通常配合新建使用",
  textarea: "长说明、备注",
  select: "从固定选项里选一个，需要选项表",
  switch: "是 / 否两态，值按 \"1\" / \"0\" 提交",
  span: "只在编辑弹窗里展示，不参与提交，不能必填",
}

/**
 * 写入策略。三档而不是一个「创建后锁定」开关：
 * 企业表单里既有「一次定死」（编码），也有「谁都不许改」（系统生成、创建时间）。
 * 两态布尔表达不了后者，配置者只能用只读展示控件绕过——那样连新建都填不了。
 */
export const WRITE_MODES: DwWriteMode[] = ["always", "createOnly", "readonly"]

export const WRITE_MODE_LABELS: Record<DwWriteMode, string> = {
  always: "可编辑",
  createOnly: "仅新增可编辑",
  readonly: "始终只读",
}

export const WRITE_MODE_HINTS: Record<DwWriteMode, string> = {
  always: "新建、编辑都能改",
  createOnly: "新建时填，编辑弹窗里锁定并展示当前值",
  readonly: "任何状态都不能改，只展示当前值",
}

/** 归一：老配置只有 immutable，true 按 createOnly 读；新配置以 writeMode 为准。 */
export function writeModeOf(field: Pick<DwFormField, "writeMode" | "immutable">): DwWriteMode {
  if (field.writeMode && WRITE_MODES.includes(field.writeMode)) return field.writeMode
  return field.immutable ? "createOnly" : "always"
}

/** 某一态下这个字段是否可写：新建 / 编辑都要判断，别只判一半。 */
export function isFieldWritable(field: Pick<DwFormField, "writeMode" | "immutable" | "widget">, editing: boolean): boolean {
  // 只读展示控件不参与写入，任何态都不可写（服务端也不会收到它）。
  if (field.widget === "span") return false
  const mode = writeModeOf(field)
  if (mode === "readonly") return false
  return !(mode === "createOnly" && editing)
}

export const ACTIONS: DwAction[] = ["create", "edit", "delete", "refresh"]

export const ACTION_LABELS: Record<DwAction, string> = {
  create: "新建",
  edit: "编辑",
  delete: "删除",
  refresh: "刷新",
}

export const ACTION_HINTS: Record<DwAction, string> = {
  create: "右上角「新建」按钮 + 新建弹窗",
  edit: "行操作菜单里的「编辑」，按行回填表单",
  delete: "行操作菜单里的「删除」，二次确认后删除",
  refresh: "工具栏的「刷新」，只重查数据",
}

export const VARIANTS: DwVariant[] = ["secondary", "success", "warning", "destructive", "outline"]

export const VARIANT_LABELS: Record<DwVariant, string> = {
  secondary: "灰色",
  success: "绿色",
  warning: "橙色",
  destructive: "红色",
  outline: "描边",
}

/**
 * 列宽档位。空串 = 自适应（由内容撑开）。
 *
 * 存量配置里存的是 Tailwind 类名（`w-36` 这种），所以这里只做「已知档位」的映射：
 * 不认识的值一律当自定义类名原样保留，切换档位才覆盖。删掉这个兼容，老配置会全部变成自适应。
 */
export const WIDTH_TIERS: Array<{ value: string; label: string; hint: string }> = [
  { value: "", label: "自适应", hint: "按内容撑开，适合名称、说明" },
  { value: "w-24", label: "窄", hint: "6rem，适合状态、ID、日期" },
  { value: "w-36", label: "中", hint: "9rem，适合编码、分类" },
  { value: "w-56", label: "宽", hint: "14rem，适合长文本" },
]

export function isKnownWidth(value: string | undefined): boolean {
  return WIDTH_TIERS.some((tier) => tier.value === (value ?? ""))
}

export const FIELD_TYPE_LABELS: Record<string, string> = {
  string: "文本",
  number: "数字",
  list: "多值",
}

/** 数据源没有声明过的类型按文本处理——宁可展示朴素一点，也不要因为类型怪就不渲染。 */
export function defaultFormatFor(type: string): DwColumnFormat {
  if (type === "number") return "number"
  if (type === "list") return "tags"
  return "text"
}

/** 数字用单行文本、多值不给表单（多值需要多选控件，不在当前控件集里）。 */
export function defaultWidgetFor(type: string): DwWidget {
  if (type === "list") return "span"
  return "input"
}

/** 表单字段是否可写：只读字段配进表单，后端保存时会 400，编辑器直接不给选。 */
export function writableFields(fields: DwFieldMeta[]): DwFieldMeta[] {
  return fields.filter((field) => field.writable)
}

export interface FieldUsage {
  columns: number[]
  filters: number[]
  form: number[]
}

/**
 * 字段使用矩阵：每个字段被哪些列 / 筛选 / 表单字段引用。
 *
 * 页面配置最容易出的错不是写错一个值，而是「同一个字段在列里叫角色名称、在表单里叫名称、
 * 在筛选里叫角色名」。把使用情况摊在一张表上，这类不一致在配置时就能看见。
 */
export function fieldUsage(list: DwListConfig, form: DwFormConfig): Record<string, FieldUsage> {
  const usage: Record<string, FieldUsage> = {}
  const touch = (key: string): FieldUsage => {
    if (!usage[key]) usage[key] = { columns: [], filters: [], form: [] }
    return usage[key]
  }
  list.columns.forEach((column, index) => {
    if (column.key) touch(column.key).columns.push(index)
  })
  list.filters.forEach((filter, index) => {
    if (filter.key) touch(filter.key).filters.push(index)
  })
  form.fields.forEach((field, index) => {
    if (field.key) touch(field.key).form.push(index)
  })
  return usage
}

/** 字段在数据源里的声明；用于「未知字段」提示与标签继承。 */
export function fieldMeta(fields: DwFieldMeta[], key: string): DwFieldMeta | undefined {
  return fields.find((field) => field.key === key)
}

/**
 * 预览用的示例行。不请求真实数据：配置还没保存，后端拿不到这份配置，
 * 预览必须能在「未保存、甚至字段都还没配全」的状态下渲染。
 */
export function sampleRows(fields: DwFieldMeta[], columns: DwColumn[], count = 3): DwRow[] {
  const rows: DwRow[] = []
  for (let i = 1; i <= count; i += 1) {
    const row: DwRow = {}
    for (const field of fields) {
      row[field.key] = sampleValue(field, i)
    }
    // 配了值映射的列，样例值取映射里的值，否则预览看不到 badge / 状态色。
    for (const column of columns) {
      const options = column.options ?? []
      if (options.length > 0) {
        row[column.key] = options[(i - 1) % options.length].value
      }
    }
    rows.push(row)
  }
  return rows
}

function sampleValue(field: DwFieldMeta, index: number): string | number | string[] {
  const key = field.key.toLowerCase()
  if (key === "id" || key.endsWith("id")) return index
  if (field.type === "number") return index * 3
  if (field.type === "list") return index % 2 === 0 ? ["示例 A", "示例 B"] : ["示例 A"]
  if (key.includes("time") || key.includes("date")) return `2026-11-0${index} 10:0${index}`
  if (key.includes("status") || key.includes("enable")) return index % 2 === 0 ? "1" : "0"
  if (key.includes("email")) return `user${index}@example.com`
  if (key.includes("code")) return `CODE-${index}`
  return `${field.label}${index}`
}

/** 预览里的列名：配置为空时退化成字段标签 / 字段 key，别让表头空着。 */
export function columnTitle(column: DwColumn, fields: DwFieldMeta[]): string {
  return column.label || fieldMeta(fields, column.key)?.label || column.key || "未命名字段"
}

/** 新建一行配置时的默认值，与后端 DTO 的字段默认值对齐。 */
export const EMPTY_LIST: DwListConfig = {
  columns: [],
  filters: [],
  pageSize: 20,
  actions: [],
  rowKey: "id",
}

export const EMPTY_FORM: DwFormConfig = {
  createTitle: "新建",
  editTitle: "编辑",
  createDescription: "",
  editDescription: "",
  fields: [],
}

export interface PageDraft {
  pageKey: string
  title: string
  description: string
  datasourceKey: string
  list: DwListConfig
  form: DwFormConfig
}

export function emptyDraft(): PageDraft {
  return {
    pageKey: "",
    title: "",
    description: "",
    datasourceKey: "",
    list: { ...EMPTY_LIST, columns: [], filters: [], actions: [] },
    form: { ...EMPTY_FORM, fields: [] },
  }
}

/** 按数据源字段生成一列：格式按类型映射，标签继承字段声明。 */
export function columnFromField(field: DwFieldMeta): DwColumn {
  return {
    key: field.key,
    label: field.label,
    format: defaultFormatFor(field.type),
    width: "",
    options: [],
  }
}

/** 按可写字段生成一个表单项。 */
export function fieldFromMeta(meta: DwFieldMeta): DwFormField {
  return {
    key: meta.key,
    label: meta.label,
    widget: defaultWidgetFor(meta.type),
    required: false,
    writeMode: "always",
    placeholder: "",
    defaultValue: "",
    pattern: "",
    patternError: "",
    options: [],
    span: 1,
  }
}

/** 数据源下拉的选项文案：只读源要显式标注，否则保存时才被 400 打回来。 */
export function datasourceLabel(datasources: DatasourceOption[], key: string): string {
  const found = datasources.find((ds) => ds.key === key)
  return found ? found.label : key ? `${key}（数据源已下线）` : "未选择"
}

export function datasourceOf(datasources: DatasourceOption[], key: string): DatasourceOption | undefined {
  return datasources.find((ds) => ds.key === key)
}
