export interface RestResult<T> {
  success: boolean
  code: number
  message: string | null
  data: T
}

/** 角色编码不再写死成联合类型：角色可以在「角色权限」页自行新增。 */
export type RoleCode = string
export type UserStatus = 0 | 1

export interface CurrentUser {
  id: number
  username: string
  displayName: string
  /** 一个人可以挂多个角色，权限取并集、数据范围取最宽。 */
  roleCodes: RoleCode[]
  roleNames: string[]
  /** 全部角色合并后的权限点，用来决定按钮是否出现；后端仍会独立校验。 */
  permissions: string[]
  /** 数据范围档位：1 全部 / 2 自定义 / 3 本部门 / 4 本部门及以下 / 5 仅本人 */
  dataScope: number
  /** 数据范围的中文说明，例如「本部门及以下（1 个部门）」 */
  scopeDescription: string
  /** 这个账号能看到的菜单，后端按 permissions 过滤后给出。侧边栏与页面标题都只认它。 */
  menus: MenuItem[]
  /**
   * 配置了菜单、但本账号没有对应权限点的页面路径。直达这些地址时前端直接弹回概览页，
   * 而不是让人面对一屏 403。这仍是体验层——真正的拦截在后端每个接口的权限校验里。
   */
  restrictedPaths: string[]
}

/**
 * 一条菜单。定义在数据库 sys_menu，前端只负责「path 渲染哪个组件」。
 *
 * 只有元数据下发得下来：Feat 的路由在编译期确定，组件必须先存在于构建产物里，
 * 所以 App.tsx 仍然要为新页面登记一条 Route——那不是菜单，是组件映射。
 */
export interface MenuItem {
  groupKey: string
  groupLabel: string
  path: string
  /** 菜单名，同时是浏览器标签页标题 */
  name: string
  /** 图标 key，按 NavIcons 取组件；缺映射时退化为默认图标 */
  icon: string
  /** 需要的权限点；空串表示只要登录就能看到（不用 null：登录响应走 fastjson2，null 字段会被省略） */
  permission: string
  sort: number
  /** false 表示不进侧栏，只贡献「路径 → 标题」，例如个人中心 */
  visible: boolean
}

/**
 * 菜单管理页的一行，来自 GET /api/menus。
 *
 * 比 MenuItem 多 id 与 groupSort：改删要用 id，调分组先后要用 groupSort，
 * 而这两样对渲染侧栏毫无用处，所以不塞进每请求一次的下发结构里。
 */
export interface MenuAdminItem extends MenuItem {
  id: number
  groupSort: number
}

/** 创建 / 更新菜单的请求体。path 必须取自 GET /api/menus/routes 的白名单。 */
export interface MenuInput {
  groupKey: string
  groupLabel: string
  groupSort: number
  path: string
  name: string
  icon: string
  /** 空串表示登录即可见；非空必须是已登记的权限点 */
  permission: string
  sort: number
  visible: boolean
}

/**
 * 一个权限点及其展示信息，来自 GET /api/permissions。
 *
 * 这是「字典」不是「当前登录人拥有的权限」：角色页要能给角色授予自己没有的权限点。
 * 前端不再维护权限点常量，加权限点只改后端 Permission.CATALOG 一处。
 */
export interface PermissionItem {
  groupKey: string
  groupLabel: string
  key: string
  label: string
  description: string
  sort: number
  /** 该权限点当前挂在多少条菜单上；0 表示只影响接口，不控制任何侧栏入口。 */
  menuCount: number
}

/** 「我的资料」。比 CurrentUser 多出邮箱、部门名、创建时间，少掉权限点。 */
export interface Profile {
  id: number
  username: string
  displayName: string
  email: string
  departmentName: string
  roleNames: string[]
  /** 后台按本人角色算出的数据范围说明，只读——数据范围由角色决定，不能在个人中心改 */
  scopeDescription: string
  createdTime: string
}

export interface ProfileUpdate {
  displayName: string
  email: string
}

export interface ChangePasswordInput {
  oldPassword: string
  newPassword: string
}

/** 数据范围档位与名称，与后端 DataScope 一一对应。 */
export const DATA_SCOPE_ALL = 1
export const DATA_SCOPE_CUSTOM = 2
export const DATA_SCOPE_DEPARTMENT = 3
export const DATA_SCOPE_DEPARTMENT_AND_CHILD = 4
export const DATA_SCOPE_SELF = 5

export interface DataScopeOption {
  value: number
  label: string
  description: string
}

export const DATA_SCOPE_OPTIONS: DataScopeOption[] = [
  { value: DATA_SCOPE_ALL, label: "全部数据", description: "不做任何过滤" },
  { value: DATA_SCOPE_CUSTOM, label: "自定义部门", description: "只看选中的部门" },
  { value: DATA_SCOPE_DEPARTMENT, label: "本部门", description: "只看自己所在部门" },
  { value: DATA_SCOPE_DEPARTMENT_AND_CHILD, label: "本部门及以下", description: "自己所在部门及其全部下级" },
  { value: DATA_SCOPE_SELF, label: "仅本人", description: "只看自己" },
]

export interface LoginResponse {
  accessToken: string
  tokenType: "Bearer"
  expiresIn: number
  user: CurrentUser
}

export interface UserListItem {
  id: number
  username: string
  displayName: string
  /** 角色编码，用于提交与筛选 */
  roleCodes: RoleCode[]
  /** 角色名称，用于展示，与 roleCodes 顺序一致 */
  roleNames: string[]
  /** 所属部门主键；历史数据可能为空。 */
  departmentId: number | null
  /** 部门名称来自后端联表查询，只用于展示与搜索。 */
  departmentName: string
  email: string
  status: UserStatus
  createdTime: string
  editTime: string
  /** 创建人 / 最后编辑人的用户 id，系统初始化的种子数据为 null */
  creator?: number | null
  editor?: number | null
}

export interface UserInput {
  username: string
  displayName: string
  /** 多角色：权限取并集、数据范围取最宽 */
  roleCodes: RoleCode[]
  departmentId: number | null
  email: string
  status: UserStatus
}

export interface UserQuery {
  page: number
  pageSize: number
  keyword: string
  status: "all" | "0" | "1"
  roleCode: "all" | RoleCode
  /** all 表示不限部门；数字表示该部门及其全部下级。 */
  departmentId: "all" | number
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
}

export interface RoleItem {
  id: number
  code: RoleCode
  name: string
  description: string
  permissions: string[]
  /** 数据范围档位，取值见 DATA_SCOPE_OPTIONS */
  departmentScope: number
  scopeLabel: string
  /** 只在 departmentScope 为「自定义」时有值 */
  departmentIds: number[]
  /** 内置角色：编码不可改、不可删除；ADMIN 的权限与数据范围也不可改。 */
  builtin: boolean
  status: UserStatus
  /** 该角色下的账号数，用于删除前提示。 */
  userCount: number
  createdTime: string
  editTime: string
}

export interface RoleInput {
  code: string
  name: string
  description: string
  permissions: string[]
  departmentScope: number
  /** 仅当 departmentScope 为「自定义」时提交 */
  departmentIds: number[]
  status: UserStatus
}

export interface DepartmentNode {
  id: number
  parentId: number
  code: string
  name: string
  sortOrder: number
  description: string
  status: UserStatus
  /** 直属成员数，不含子部门 */
  userCount: number
  createdTime: string
  editTime: string
  children: DepartmentNode[]
}

export interface DepartmentInput {
  parentId: number
  code: string
  name: string
  sortOrder: number
  description: string
  status: UserStatus
}

/** 部门树拍平后的选项，depth 用于在下拉里做层级缩进。 */
export interface DepartmentOption {
  id: number
  name: string
  depth: number
  disabled: boolean
}

export type ActivityKind = "create" | "update" | "enable" | "disable" | "password" | "delete" | "system"

export interface ActivityItem {
  id: number
  kind: ActivityKind
  title: string
  description: string
  createdTime: string
  /** 动态发生在谁身上；系统事件为 null */
  userId?: number | null
  /** 执行操作的人，系统事件为 null */
  creator?: number | null
  /** 操作人姓名，由后端联表得到；操作人已被删除时为 null */
  operatorName?: string | null
}

export interface ActivityQuery {
  page: number
  pageSize: number
  kind: "all" | ActivityKind
  operatorId: "all" | number
  /** yyyy-MM-dd，空串表示不限 */
  from: string
  /** yyyy-MM-dd，含当天 */
  to: string
}

/** 日志筛选下拉的操作人选项，来自日志表本身而不是用户表。 */
export interface OperatorOption {
  id: number
  name: string
}

export interface NotificationItem {
  id: number
  title: string
  description: string
  createdTime: string
  read: boolean
  userId?: number
}

export interface TrendPoint {
  label: string
  value: number
}

export interface DashboardSummary {
  totalUsers: number
  enabledUsers: number
  disabledUsers: number
  unreadNotifications: number
  trend7: TrendPoint[]
  trend30: TrendPoint[]
  activities: ActivityItem[]
}

/**
 * 菜单管理页的分组信息。后端从 sys_menu 按 groupKey 聚合出来——所以 count=0 的分组意味着
 * 该组下没有菜单，可以安全删除（但后端不暴露单独的删除接口：删完组内最后一个菜单，组自然消失）。
 */
export interface MenuGroup {
  key: string
  label: string
  sort: number
  /** 该分组下的菜单数。为 0 表示空组。 */
  count: number
}

/** 更新分组的请求体。groupKey 在 URL 路径里，这里只有 label 和 sort。 */
export interface GroupUpdateInput {
  label: string
  sort: number
}

// ---------------------------------------------------------------------------
// 动态页面（元数据驱动的列表 / 表单）
//
// 渲染配置存在后端 sys_page（list_json / form_json），下发即这些接口；
// 数据经 /api/dw 通用通道按 datasourceKey 分发。字段命名与后端 Dw* DTO 一一对应，
// 可选字段标注 ?（后端总是下发非 null 值，? 只是 JSON 里可能省略的防御）。
// ---------------------------------------------------------------------------

/** 表单控件枚举，与后端 PageService 的 FIELD_WIDGETS 校验集一致 */
export type DwWidget = "input" | "password" | "textarea" | "select" | "switch" | "span"
/** 列表内置动作枚举。自定义按钮动作（老平台的 script 存库）刻意不支持 */
export type DwAction = "create" | "edit" | "delete" | "refresh"
export type DwColumnFormat = "text" | "muted" | "badge" | "tags" | "number"
export type DwVariant = "success" | "warning" | "destructive" | "secondary" | "outline"

/** 枚举选项：select 的 option / badge、tags 的值映射 */
export interface DwOption {
  value: string
  label: string
  variant?: DwVariant
}

/** 一列：key 必须命中数据源声明的字段 */
export interface DwColumn {
  key: string
  label: string
  format: DwColumnFormat
  /** Tailwind 宽度 class（如 "w-36"），省略表示自适应 */
  width?: string
  /** badge / tags 格式的值 → 文案 / 变体映射 */
  options?: DwOption[]
}

/**
 * 筛选控件枚举，与后端 PageService 的 FILTER_WIDGETS 校验集一致。
 * - input：模糊匹配
 * - select：精确匹配，选项来自静态 options 或远程 optionsApi
 * - dateRange：日期范围，产出两个筛选键（key 与 endKey）
 */
export type DwFilterWidget = "input" | "select" | "dateRange"

/**
 * 一条筛选。值的解释权在数据源：input 约定模糊匹配、select 约定精确匹配。
 *
 * dateRange 占两个键：起止分别是 key 与 endKey，前端产出 {"from":"2026-08-01","to":"2026-08-30"}。
 * 选项两种来源二选一：静态 options，或远程 optionsApi（站内 /api/ 路径，用
 * optionValueField / optionLabelField 指定取哪两个字段）。
 */
export interface DwFilter {
  key: string
  label: string
  widget: DwFilterWidget
  placeholder?: string
  /** dateRange 的结束键；其他控件留空 */
  endKey?: string
  /** 静态选项 */
  options?: DwOption[]
  /** 远程选项路径，如 /api/activities/operators */
  optionsApi?: string
  /** 远程选项的取值字段，如 id；留空按 value */
  optionValueField?: string
  /** 远程选项的显示字段，如 name；留空按 label */
  optionLabelField?: string
}

/**
 * 表单字段的写入策略。决定「什么时候能改」，不是「字段本身可变不可变」：
 * - always：新建、编辑都可写
 * - createOnly：新建可写，编辑态只读（角色编码这类一次定死的字段）
 * - readonly：始终只读，只展示当前值
 *
 * 服务端保存时会按它清洗入参（新建丢弃 readonly，编辑回填 createOnly / readonly 的旧值），
 * 所以 UI 置灰与真实可写性是同一套规则。
 */
export type DwWriteMode = "always" | "createOnly" | "readonly"

/** 一个表单字段。pattern 两端各自校验 */
export interface DwFormField {
  key: string
  label: string
  widget: DwWidget
  required?: boolean
  /** 写入策略；缺省按 always，`immutable: true` 的老配置等价于 createOnly */
  writeMode?: DwWriteMode
  /** @deprecated 旧表达：true = createOnly。新配置请写 writeMode */
  immutable?: boolean
  pattern?: string
  patternError?: string
  placeholder?: string
  defaultValue?: string
  options?: DwOption[]
  /** 1 = 半宽、2 = 全宽 */
  span?: 1 | 2
}

/**
 * 数据通道覆盖：列表查询与行删除可改走自定义接口，留空走内置 /api/dw 通道，允许混搭。
 * 只覆盖这两个操作——保存 / 详情始终走内置通道（表单写路径牵涉校验与审计，不开旁路）。
 * 行结构契约不变——自定义接口返回的行字段仍须与数据源声明一致。
 */
export interface DwDataApi {
  query?: string
  delete?: string
}

export interface DwListConfig {
  columns: DwColumn[]
  filters: DwFilter[]
  pageSize: number
  actions: DwAction[]
  rowKey: string
  dataApi?: DwDataApi
}

export interface DwFormConfig {
  createTitle: string
  editTitle: string
  createDescription: string
  editDescription: string
  fields: DwFormField[]
}

/** GET /api/dw/pages/{pageKey} 的响应。canWrite 按当前请求人逐次计算 */
export interface PageConfig {
  pageKey: string
  title: string
  description: string
  datasourceKey: string
  list: DwListConfig
  form: DwFormConfig
  canWrite: boolean
}

/** 动态行的取值：字符串 / 数字 / 布尔 / 字符串数组，渲染层按 format 解释 */
export type DwRow = Record<string, unknown>

export interface DwDataQuery {
  page: number
  pageSize: number
  filters: Record<string, string>
}

export interface DwSaveInput {
  /** null / 省略表示新建 */
  id?: number | null
  values: Record<string, string>
}

/** 配置管理页的一行，来自 GET /api/pages。listJson / formJson 原文供编辑框回显 */
export interface PageAdminItem {
  id: number
  pageKey: string
  title: string
  description: string
  datasourceKey: string
  listJson: string
  formJson: string
  editTime: string
}

export interface PageInput {
  pageKey: string
  title: string
  description: string
  datasourceKey: string
  listJson: string
  formJson: string
}

/** 数据源声明的可用字段：配置表单的字段提示 + 「防配置漂移」的锚点 */
export interface DwFieldMeta {
  key: string
  label: string
  type: string
  writable: boolean
}

export interface DatasourceOption {
  key: string
  label: string
  /** 只读数据源：编辑器据此禁用 create / edit / delete 动作勾选 */
  writable: boolean
  fields: DwFieldMeta[]
}
