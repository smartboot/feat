import { useEffect, useMemo, useState } from "react"
import {
  AlertTriangle,
  ArrowLeft,
  Check,
  Copy,
  Database,
  FileJson,
  FormInput,
  Info,
  LayoutList,
  Link2,
  Rocket,
  Table2,
  Unlink,
} from "lucide-react"

import type { DatasourceOption, DwFieldMeta, PageAdminItem, PageInput } from "@/api/types"
import { DwFieldCatalog } from "@/components/dw/DwFieldCatalog"
import { DwFormConfigEditor } from "@/components/dw/DwFormConfigEditor"
import { DwListConfigEditor } from "@/components/dw/DwListConfigEditor"
import { DwPreview, PREVIEW_MODES, type PreviewMode } from "@/components/dw/DwPreview"
import {
  describeFocus,
  focusAnchor,
  focusFromAnchor,
  marksFromIssues,
  sameFocus,
  sectionOfFocus,
  type DwFocusTarget,
} from "@/components/dw/link"
import { countBySection, lintDraft, splitIssues, type DwIssue } from "@/components/dw/lint"
import {
  EMPTY_FORM,
  EMPTY_LIST,
  columnFromField,
  emptyDraft,
  fieldFromMeta,
  type PageDraft,
} from "@/components/dw/meta"
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
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { cn } from "@/lib/utils"

/**
 * 页面工作台。
 *
 * 原来是一个 92vh 的弹窗里塞三页签。配置一旦上规模（10 列 + 6 个筛选 + 12 个字段），
 * 弹窗的滚动区就变成了「盲写」：改完一个控件看不到结果，退回列表才能验证。
 *
 * 工作台把它拆成固定的四段心智模型：数据源 → 列表视图 → 表单 → 发布检查，
 * 中间编辑、右边实时预览、左下常驻问题面板。这是「页面配置成为主力功能」之后
 * 唯一撑得住的操作密度。
 */
export type StudioSection = "basic" | "fields" | "list" | "form" | "publish"

const SECTIONS: Array<{
  key: StudioSection
  label: string
  icon: typeof Table2
  hint: string
}> = [
  { key: "basic", label: "基本信息", icon: Info, hint: "标识 / 标题 / 数据源" },
  { key: "fields", label: "字段", icon: Database, hint: "数据源能提供什么" },
  { key: "list", label: "列表视图", icon: Table2, hint: "列 / 筛选 / 动作" },
  { key: "form", label: "表单", icon: FormInput, hint: "新建与编辑弹窗" },
  { key: "publish", label: "发布检查", icon: Rocket, hint: "问题与影响面" },
]

export interface ReferencingMenu {
  name: string
  path: string
}

/**
 * 跟随编辑的策略：**只在当前模式在这个分区讲不通时**才切走，不打断用户正在看的状态。
 *
 * - 表单分区：弹窗是主角，保留用户在新建 / 编辑之间的选择，改字段时画面不被抽走；
 * - 列表 / 字段 / 基本信息：都以列表为主角，停在弹窗态时回到列表（否则改列宽却看不见表）；
 * - 发布检查：不动，用户是在读检查结果，不是在看页面。
 *
 * 空态与无写权限是「临时看一眼」，跟随时不主动覆盖它们。
 */
function followMode(section: StudioSection, current: PreviewMode): PreviewMode {
  if (section === "form") return current === "edit" ? "edit" : "create"
  if (section === "publish") return current
  return current === "create" || current === "edit" ? "list" : current
}

/** 重新打开跟随时回到分区的「本命」模式：表单 → 新建，其余 → 列表。 */
function canonicalMode(section: StudioSection, current: PreviewMode): PreviewMode {
  if (section === "form") return current === "edit" ? "edit" : "create"
  return "list"
}

export function PageStudio({
  mode,
  initial,
  datasources,
  referencingMenus,
  pending,
  onSubmit,
  onClose,
}: {
  mode: "create" | "edit"
  initial: PageAdminItem | null
  datasources: DatasourceOption[]
  /** 指向 /p/{pageKey} 的菜单，用于展示「改这个页面会影响谁」 */
  referencingMenus: ReferencingMenu[]
  pending: boolean
  onSubmit: (input: PageInput) => Promise<void>
  onClose: () => void
}) {
  const [draft, setDraft] = useState<PageDraft>(() => (initial ? fromRecord(initial) : emptyDraft()))
  const [baseline] = useState(() => (initial ? JSON.stringify(fromRecord(initial)) : JSON.stringify(emptyDraft())))
  const [section, setSection] = useState<StudioSection>(mode === "create" ? "basic" : "fields")
  const [previewMode, setPreviewMode] = useState<PreviewMode>("list")
  /** 预览模式是否跟随左侧分区自动切换。手动点过预览页签即锁定，避免「我明明选了空态」被抢走。 */
  const [followPreview, setFollowPreview] = useState(true)
  /** 点选的条目（持久）：点过某列之后，右边一直留着它的高亮，直到换分区或换一条。 */
  const [selected, setSelected] = useState<DwFocusTarget | null>(null)
  /** 悬停的条目（临时）：优先于选中，鼠标离开即消失。 */
  const [hovered, setHovered] = useState<DwFocusTarget | null>(null)
  const [previewOpen, setPreviewOpen] = useState(false)
  const [saveError, setSaveError] = useState("")
  const [parseWarning, setParseWarning] = useState("")
  const [pendingDatasource, setPendingDatasource] = useState<string | null>(null)
  const [confirmClose, setConfirmClose] = useState(false)

  useEffect(() => {
    if (!initial) return
    const badList = initial.listJson.trim() !== "" && isEmptyConfig(initial.listJson, EMPTY_LIST)
    const badForm = initial.formJson.trim() !== "" && isEmptyConfig(initial.formJson, EMPTY_FORM)
    // 保存闸门保证库里的一定能渲染，解析失败只能是脏数据——提示而不是静默丢弃。
    if (badList || badForm) {
      setParseWarning("存量配置 JSON 解析失败，已回退为空白配置；直接保存会覆盖原配置，请先核对。")
    }
  }, [initial])

  const datasource = datasources.find((ds) => ds.key === draft.datasourceKey)
  const issues = useMemo(() => lintDraft({ draft, datasources }), [draft, datasources])
  const { errors, warnings } = splitIssues(issues)
  const sectionCounts = countBySection(issues)
  const dirty = JSON.stringify(draft) !== baseline
  const marks = useMemo(() => marksFromIssues(issues), [issues])
  const focus = hovered ?? selected
  /** 高亮来自点选（而不是悬停）：只有这种才自动滚动预览，不然鼠标一动预览就抖。 */
  const pinned = Boolean(selected) && !hovered
  const focusLabel = focus ? describeFocus(draft, focus, datasource?.fields ?? []) : ""

  /** 分区切换：预览模式跟着走，并清掉不属于新分区的选中项。 */
  function goSection(next: StudioSection) {
    setSection(next)
    setHovered(null)
    if (selected && sectionOfFocus(selected) !== next) setSelected(null)
    if (followPreview) setPreviewMode((current) => followMode(next, current))
  }

  /**
   * 预览里点某一条（表头 / 筛选 / 表单字段）→ 编辑区切分区、滚过去、选中它。
   * 这里不走 goSection：那会把刚选中的目标当成「不属于新分区」清掉。
   */
  function locate(target: DwFocusTarget) {
    setSelected(target)
    setHovered(null)
    const next = sectionOfFocus(target)
    setSection(next)
    if (followPreview) setPreviewMode((current) => followMode(next, current))
    scrollToAnchor(focusAnchor(target))
  }

  /** 预览里点「新建」：切预览模式；跟随时顺手把编辑区带到表单分区。 */
  function openFormPreview(next: "create" | "edit") {
    if (followPreview) goSection("form")
    setPreviewMode(next)
  }

  /** 手动点预览页签：明确表达了「我要看这个」，从此不再被分区切换抢走。 */
  function selectPreviewMode(next: PreviewMode) {
    setPreviewMode(next)
    setFollowPreview(false)
  }

  function toggleFollow() {
    const next = !followPreview
    setFollowPreview(next)
    if (next) setPreviewMode((current) => canonicalMode(section, current))
  }

  function update(patch: Partial<PageDraft>) {
    setDraft((current) => ({ ...current, ...patch }))
    setSaveError("")
  }

  /** 数据源切换：先把「有损」这件事说清楚，再决定是迁移还是重生成。 */
  function changeDatasource(key: string) {
    if (draft.datasourceKey === key) return
    const next = datasources.find((ds) => ds.key === key)
    const nextKeys = new Set((next?.fields ?? []).map((field) => field.key))
    const lossy =
      draft.list.columns.some((column) => column.key && !nextKeys.has(column.key)) ||
      draft.list.filters.some((filter) => filter.key && !nextKeys.has(filter.key)) ||
      draft.form.fields.some((field) => field.key && !nextKeys.has(field.key))
    if (lossy) {
      setPendingDatasource(key)
      return
    }
    applyDatasource(key, false)
  }

  function applyDatasource(key: string, regenerate: boolean) {
    const next = datasources.find((ds) => ds.key === key)
    const nextKeys = new Set((next?.fields ?? []).map((field) => field.key))
    const writable = (next?.fields ?? []).filter((field) => field.writable)
    setDraft((current) => {
      const actions = next?.writable
        ? current.list.actions
        : current.list.actions.filter((action) => action === "refresh")
      if (regenerate) {
        return {
          ...current,
          datasourceKey: key,
          list: {
            ...current.list,
            columns: (next?.fields ?? []).map(columnFromField),
            filters: [],
            rowKey: nextKeys.has("id") ? "id" : next?.fields[0]?.key ?? "",
            actions,
          },
          form: { ...current.form, fields: writable.map(fieldFromMeta) },
        }
      }
      return {
        ...current,
        datasourceKey: key,
        list: {
          ...current.list,
          columns: current.list.columns.filter((column) => !column.key || nextKeys.has(column.key)),
          filters: current.list.filters.filter((filter) => !filter.key || nextKeys.has(filter.key)),
          rowKey: nextKeys.has(current.list.rowKey) ? current.list.rowKey : next?.fields[0]?.key ?? "",
          actions,
        },
        form: {
          ...current.form,
          fields: current.form.fields.filter((field) => !field.key || nextKeys.has(field.key)),
        },
      }
    })
    setPendingDatasource(null)
  }

  /** 字段面板的一键加入：按类型给出合理默认值，细节留给对应分区去改。 */
  function addFromCatalog(field: DwFieldMeta, target: "column" | "filter" | "form") {
    if (target === "column") {
      update({ list: { ...draft.list, columns: [...draft.list.columns, columnFromField(field)] } })
      return
    }
    if (target === "filter") {
      update({
        list: {
          ...draft.list,
          filters: [
            ...draft.list.filters,
            {
              key: field.key,
              label: field.label,
              widget: field.type === "string" ? "input" : "select",
              placeholder: "",
              options: [],
            },
          ],
        },
      })
      return
    }
    update({ form: { ...draft.form, fields: [...draft.form.fields, fieldFromMeta(field)] } })
  }

  function migrate() {
    const known = new Set((datasource?.fields ?? []).map((field) => field.key))
    update({
      list: {
        ...draft.list,
        columns: draft.list.columns.filter((column) => !column.key || known.has(column.key)),
        filters: draft.list.filters.filter((filter) => !filter.key || known.has(filter.key)),
      },
      form: {
        ...draft.form,
        fields: draft.form.fields.filter((field) => !field.key || known.has(field.key)),
      },
    })
  }

  /** 滚到编辑区的某一条，并把焦点落到第一个可输入控件上。 */
  function scrollToAnchor(anchor: string) {
    window.setTimeout(() => {
      const target = document.getElementById(anchor)
      if (!target) return
      // jsdom 没有 scrollIntoView；滚动只是体验优化，测试里跳过
      if (typeof target.scrollIntoView === "function") {
        target.scrollIntoView({ block: "center", behavior: "smooth" })
      }
      const focusable = target.querySelector<HTMLElement>("input, select, textarea, button")
      focusable?.focus({ preventScroll: true })
    }, 60)
  }

  /**
   * 跳到出问题的地方：切分区 → 选中 → 滚动 → 聚焦。
   * 锚点能对上列 / 筛选 / 字段时顺手选中它，预览里同一格同时亮起来——
   * 「问题在哪」和「改的是哪」必须是同一个位置。
   */
  function jumpTo(issue: DwIssue) {
    if (issue.section !== section) goSection(issue.section)
    const target = focusFromAnchor(issue.anchor)
    if (target) setSelected(target)
    if (issue.anchor) scrollToAnchor(issue.anchor)
  }

  async function save() {
    if (errors.length > 0) {
      goSection("publish")
      setSaveError(`还有 ${errors.length} 个问题需要处理，点问题可直接跳到对应位置。`)
      return
    }
    try {
      await onSubmit({
        pageKey: draft.pageKey.trim(),
        title: draft.title.trim(),
        description: draft.description,
        datasourceKey: draft.datasourceKey,
        listJson: JSON.stringify(draft.list),
        formJson: JSON.stringify(draft.form),
      })
    } catch (error) {
      setSaveError(error instanceof Error ? error.message : "保存失败")
    }
  }

  function requestClose() {
    if (dirty) {
      setConfirmClose(true)
      return
    }
    onClose()
  }

  const preview = (mode: PreviewMode) => (
    <DwPreview
      draft={draft}
      datasource={datasource}
      mode={mode}
      focus={focus}
      pinned={pinned}
      marks={marks}
      onHover={setHovered}
      onLocate={locate}
      onOpenForm={openFormPreview}
    />
  )

  return (
    <div className="fixed inset-0 z-50 flex flex-col bg-background">
      <header className="flex h-14 shrink-0 items-center justify-between gap-3 border-b px-3 sm:px-4">
        <div className="flex min-w-0 items-center gap-2">
          <Button variant="ghost" size="icon" aria-label="返回页面列表" onClick={requestClose}>
            <ArrowLeft className="size-4" />
          </Button>
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold">
              {draft.title.trim() || (mode === "create" ? "新建页面配置" : "未命名页面")}
            </p>
            <p className="truncate font-mono text-[11px] text-muted-foreground">
              {draft.pageKey ? `/p/${draft.pageKey}` : "尚未填写页面标识"}
              {dirty ? " · 有未保存的修改" : ""}
            </p>
          </div>
        </div>
        <div className="flex shrink-0 items-center gap-1.5">
          <Button variant="outline" size="sm" className="h-8 gap-1.5 text-xs xl:hidden" onClick={() => setPreviewOpen(true)}>
            <LayoutList className="size-3.5" />
            预览
          </Button>
          <Button
            variant="outline"
            size="sm"
            className={cn(
              "h-8 gap-1.5 text-xs",
              errors.length > 0 && "border-destructive/40 text-destructive",
              errors.length === 0 && warnings.length > 0 && "border-amber-500/40 text-amber-700 dark:text-amber-300",
            )}
            onClick={() => goSection("publish")}
          >
            {errors.length > 0 ? (
              <>
                <AlertTriangle className="size-3.5" />
                {errors.length} 个问题
              </>
            ) : warnings.length > 0 ? (
              <>
                <AlertTriangle className="size-3.5" />
                {warnings.length} 条提示
              </>
            ) : (
              <>
                <Check className="size-3.5 text-emerald-600" />
                检查通过
              </>
            )}
          </Button>
          <Button size="sm" className="h-8 text-xs" onClick={save} disabled={pending}>
            {pending ? "正在保存…" : mode === "create" ? "创建并生效" : "保存并生效"}
          </Button>
        </div>
      </header>

      <div className="flex min-h-0 flex-1">
        <nav className="w-52 shrink-0 overflow-y-auto border-r p-2" aria-label="配置分区">
          {SECTIONS.map((item) => {
            const Icon = item.icon
            const active = section === item.key
            {/* 按分区取问题数：新增分区不必回来改这行判断，漏改的后果是那个分区的问题数永远不显示 */}
            const count = (sectionCounts as Record<string, number>)[item.key] ?? 0
            return (
              <button
                key={item.key}
                type="button"
                aria-current={active ? "true" : undefined}
                className={cn(
                  "mb-0.5 flex w-full items-start gap-2 rounded-md px-2 py-1.5 text-left transition-colors",
                  active ? "bg-secondary text-foreground" : "text-muted-foreground hover:bg-secondary/60",
                )}
                onClick={() => goSection(item.key)}
              >
                <Icon className="mt-0.5 size-3.5 shrink-0" />
                <span className="min-w-0 flex-1">
                  <span className="flex items-center gap-1.5">
                    <span className="truncate text-xs font-medium">{item.label}</span>
                    {count > 0 && (
                      <span className="rounded-full bg-destructive/15 px-1 text-[10px] font-medium text-destructive">
                        {count}
                      </span>
                    )}
                    {item.key === "publish" && errors.length === 0 && (
                      <Check className="size-3 text-emerald-600" />
                    )}
                  </span>
                  <span className="block truncate text-[10px] text-muted-foreground">
                    {item.key === "fields"
                      ? `${datasource?.fields.length ?? 0} 个字段`
                      : item.key === "list"
                        ? `${draft.list.columns.length} 列 · ${draft.list.filters.length} 筛选`
                        : item.key === "form"
                          ? `${draft.form.fields.length} 字段`
                          : item.hint}
                  </span>
                </span>
              </button>
            )
          })}
          <div className="mt-3 border-t pt-3">
            <p className="px-2 text-[10px] leading-relaxed text-muted-foreground">
              保存后立即对所有人生效，并记录审计日志。
            </p>
          </div>
        </nav>

        <main className="min-w-0 flex-1 overflow-y-auto px-4 py-4 sm:px-6">
          <div className="mx-auto max-w-3xl">
            {section === "basic" && (
              <div className="grid gap-4">
                <SectionTitle title="基本信息" hint="标识决定访问地址，创建后不可修改。" />
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="grid gap-1.5" id="page-key">
                    <Label htmlFor="studio-page-key">页面标识 *</Label>
                    <Input
                      id="studio-page-key"
                      value={draft.pageKey}
                      disabled={mode === "edit"}
                      placeholder="roles"
                      onChange={(event) => update({ pageKey: event.target.value })}
                    />
                    <p className="text-[11px] text-muted-foreground">
                      {mode === "edit"
                        ? "创建后不可修改：菜单以 /p/{标识} 引用它。需要换标识请「复制为新页面」。"
                        : "小写字母、数字、下划线、短横线。菜单用 /p/{标识} 指向它。"}
                    </p>
                  </div>
                  <div className="grid gap-1.5" id="page-title">
                    <Label htmlFor="studio-page-title">页面标题 *</Label>
                    <Input
                      id="studio-page-title"
                      value={draft.title}
                      placeholder="角色权限"
                      onChange={(event) => update({ title: event.target.value })}
                    />
                    <p className="text-[11px] text-muted-foreground">列表卡片标题；菜单名称可以另起。</p>
                  </div>
                </div>

                <div className="grid gap-1.5" id="page-desc">
                  <Label htmlFor="studio-page-desc">说明</Label>
                  <Input
                    id="studio-page-desc"
                    value={draft.description}
                    placeholder="这个页面给谁用、解决什么问题（会显示在标题下）"
                    onChange={(event) => update({ description: event.target.value })}
                  />
                </div>

                <div className="grid gap-1.5" id="page-datasource">
                  <Label htmlFor="studio-page-datasource">数据源 *</Label>
                  <Select value={draft.datasourceKey} onValueChange={changeDatasource}>
                    <SelectTrigger id="studio-page-datasource">
                      <SelectValue placeholder="选择一个数据源" />
                    </SelectTrigger>
                    <SelectContent>
                      {datasources.map((ds) => (
                        <SelectItem key={ds.key} value={ds.key}>
                          {ds.label}
                          <span className="ml-1.5 text-[10px] text-muted-foreground">
                            {ds.key} · {ds.fields.length} 字段{ds.writable ? "" : " · 只读"}
                          </span>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <p className="text-[11px] text-muted-foreground">
                    数据源是编译期注册的白名单：它决定有哪些字段、能不能写、要什么权限点。
                    页面只能配「怎么展示与编辑」，不能配 SQL。
                  </p>
                </div>

                {datasource && (
                  <div className="grid gap-2 rounded-lg border bg-muted/30 p-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge variant={datasource.writable ? "secondary" : "outline"} className="text-[10px]">
                        {datasource.writable ? "可读写" : "只读"}
                      </Badge>
                      <span className="text-xs text-muted-foreground">
                        {datasource.fields.length} 个字段 ·{" "}
                        {datasource.fields.filter((field) => field.writable).length} 个可写
                      </span>
                    </div>
                    <p className="text-[11px] text-muted-foreground">
                      {datasource.writable
                        ? "可以配置新建 / 编辑 / 删除；实际能不能点，还取决于当前账号有没有写权限。"
                        : "只读数据源只能配置列、筛选与刷新；写按钮不会出现。"}
                    </p>
                  </div>
                )}

                {parseWarning && (
                  <p className="rounded-md border border-amber-500/40 bg-amber-500/5 p-2 text-xs text-amber-700 dark:text-amber-300">
                    {parseWarning}
                  </p>
                )}
              </div>
            )}

            {section === "fields" && (
              <div className="grid gap-4">
                <SectionTitle
                  title="字段"
                  hint="数据源声明的字段与它们在本页的使用情况。列 / 筛选 / 表单都从这里加。"
                />
                <DwFieldCatalog
                  datasource={datasource}
                  draft={draft}
                  fieldMaps={draft.list.fieldMaps ?? {}}
                  onChangeFieldMap={(fieldKey, options) => {
                    // 删到空就整项移除：留着空值域在保存时被后端拒绝，而界面上只是「0 项」，
                    // 看起来像个无害的中间状态。
                    const next = { ...(draft.list.fieldMaps ?? {}) }
                    if (options.length === 0) {
                      delete next[fieldKey]
                    } else {
                      next[fieldKey] = options
                    }
                    update({ list: { ...draft.list, fieldMaps: next } })
                  }}
                  onAdd={addFromCatalog}
                  onMigrate={migrate}
                  onLocate={locate}
                />
              </div>
            )}

            {section === "list" && (
              <div className="grid gap-4">
                <SectionTitle title="列表视图" hint="打开页面时看到的那张表。" />
                {datasource ? (
                  <DwListConfigEditor
                    value={draft.list}
                    fields={datasource.fields}
                    writable={datasource.writable}
                    onChange={(list) => update({ list })}
                    link={{
                      focus,
                      marks,
                      onHover: setHovered,
                      onSelect: setSelected,
                    }}
                  />
                ) : (
                  <EmptyDatasource />
                )}
              </div>
            )}

            {section === "form" && (
              <div className="grid gap-4">
                <SectionTitle title="表单" hint="新建与编辑弹窗里填什么、怎么校验。" />
                {datasource ? (
                  <DwFormConfigEditor
                    value={draft.form}
                    fields={datasource.fields}
                    onChange={(form) => update({ form })}
                    link={{
                      focus,
                      marks,
                      onHover: setHovered,
                      onSelect: setSelected,
                    }}
                  />
                ) : (
                  <EmptyDatasource />
                )}
              </div>
            )}

            {section === "publish" && (
              <div className="grid gap-4">
                <SectionTitle title="发布检查" hint="保存前最后一次确认：问题、影响面、生效范围。" />

                <div
                  className={cn(
                    "rounded-lg border p-3",
                    errors.length > 0 ? "border-destructive/40 bg-destructive/5" : "border-emerald-600/30 bg-emerald-500/5",
                  )}
                >
                  <p className="text-sm font-medium">
                    {errors.length > 0
                      ? `还有 ${errors.length} 个问题不能保存`
                      : warnings.length > 0
                        ? `可以保存，另有 ${warnings.length} 条提示`
                        : "配置完整，可以保存"}
                  </p>
                  <p className="mt-1 text-xs text-muted-foreground">
                    保存后立即对所有人生效（本项目暂无草稿 / 发布两态），并写入审计日志。
                  </p>
                </div>

                {issues.length > 0 && (
                  <div className="overflow-hidden rounded-lg border">
                    {issues.map((issue, index) => (
                      <button
                        key={`${issue.message}-${index}`}
                        type="button"
                        className="flex w-full items-start gap-2 border-b px-3 py-2 text-left last:border-b-0 hover:bg-secondary/50"
                        onClick={() => jumpTo(issue)}
                      >
                        <span
                          className={cn(
                            "mt-0.5 shrink-0 rounded px-1 text-[10px] font-medium",
                            issue.level === "error"
                              ? "bg-destructive/15 text-destructive"
                              : "bg-amber-500/15 text-amber-700 dark:text-amber-300",
                          )}
                        >
                          {issue.level === "error" ? "错误" : "提示"}
                        </span>
                        <span className="min-w-0 flex-1 text-xs">{issue.message}</span>
                        <span className="shrink-0 text-[10px] text-muted-foreground">定位 →</span>
                      </button>
                    ))}
                  </div>
                )}

                <div className="grid gap-2 rounded-lg border p-3">
                  <p className="text-sm font-medium">影响面</p>
                  {referencingMenus.length > 0 ? (
                    <div className="grid gap-1">
                      <p className="text-xs text-muted-foreground">
                        有 {referencingMenus.length} 个菜单指向这个页面，改完它们同时变化：
                      </p>
                      <div className="flex flex-wrap gap-1">
                        {referencingMenus.map((menu) => (
                          <Badge key={menu.path} variant="outline" className="text-[10px]">
                            {menu.name}
                            <span className="ml-1 font-mono text-muted-foreground">{menu.path}</span>
                          </Badge>
                        ))}
                      </div>
                    </div>
                  ) : (
                    <p className="text-xs text-muted-foreground">
                      还没有菜单指向 /p/{draft.pageKey || "{标识}"}。保存后到「菜单管理」新增一条，
                      路径选它，入口才会出现在导航里。
                    </p>
                  )}
                </div>

                <details className="rounded-lg border p-3">
                  <summary className="flex cursor-pointer items-center gap-1.5 text-sm font-medium">
                    <FileJson className="size-3.5" />
                    配置原文（跨环境迁移 / 排查用）
                  </summary>
                  <div className="mt-2 grid gap-2">
                    <CopyBlock label="list_json" value={JSON.stringify(draft.list, null, 2)} />
                    <CopyBlock label="form_json" value={JSON.stringify(draft.form, null, 2)} />
                  </div>
                </details>
              </div>
            )}
          </div>
        </main>

        <aside className="hidden w-[44%] max-w-[640px] shrink-0 flex-col border-l bg-muted/20 xl:flex">
          <div className="flex shrink-0 flex-wrap items-center gap-1 border-b px-3 py-2">
            {PREVIEW_MODES.map((mode) => (
              <button
                key={mode.value}
                type="button"
                title={mode.hint}
                className={cn(
                  "rounded-md px-2 py-1 text-[11px] transition-colors",
                  previewMode === mode.value
                    ? "bg-background font-medium shadow-sm"
                    : "text-muted-foreground hover:text-foreground",
                )}
                onClick={() => selectPreviewMode(mode.value)}
              >
                {mode.label}
              </button>
            ))}
            <button
              type="button"
              onClick={toggleFollow}
              title={
                followPreview
                  ? "预览模式正在跟随左侧分区，点一下锁定当前模式"
                  : "切换分区不再改变预览模式，点一下恢复跟随"
              }
              aria-pressed={followPreview}
              className={cn(
                "ml-auto inline-flex items-center gap-1 rounded-md px-1.5 py-0.5 text-[10px] transition-colors",
                followPreview
                  ? "text-primary hover:bg-primary/10"
                  : "text-muted-foreground hover:bg-secondary hover:text-foreground",
              )}
            >
              {followPreview ? <Link2 className="size-3" /> : <Unlink className="size-3" />}
              {followPreview ? "跟随编辑" : "模式已锁定"}
            </button>
          </div>
          {/*
            联动要「可感知」：只让元素变色，用户不知道自己看的是哪一条、也不知道能点。
            所以焦点一变就在顶部报出来，同时说明反向操作的入口。
          */}
          {focusLabel && (
            <div className="flex shrink-0 items-center gap-1.5 border-b bg-primary/5 px-3 py-1 text-[10px] text-primary">
              <span className="min-w-0 truncate">已定位 {focusLabel}</span>
              <span className="ml-auto hidden shrink-0 text-muted-foreground 2xl:inline">点它可跳回编辑区</span>
            </div>
          )}
          <div className="min-h-0 flex-1 overflow-auto p-3">
            {preview(previewMode)}
            <p className="mt-3 text-[10px] leading-relaxed text-muted-foreground">
              {PREVIEW_MODES.find((mode) => mode.value === previewMode)?.hint}
              。同一份单元格 / 表单渲染，只是数据换成示例行；悬停左侧任意一条，这里会跟着亮。
            </p>
          </div>
        </aside>
      </div>

      {saveError && (
        <div className="shrink-0 border-t bg-destructive/10 px-4 py-2 text-xs text-destructive" role="alert">
          {saveError}
        </div>
      )}

      <Dialog open={previewOpen} onOpenChange={setPreviewOpen}>
        <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>预览</DialogTitle>
          </DialogHeader>
          <div className="flex flex-wrap gap-1">
            {PREVIEW_MODES.map((mode) => (
              <Button
                key={mode.value}
                variant={previewMode === mode.value ? "secondary" : "ghost"}
                size="sm"
                className="h-7 text-[11px]"
                onClick={() => selectPreviewMode(mode.value)}
              >
                {mode.label}
              </Button>
            ))}
          </div>
          {preview(previewMode)}
        </DialogContent>
      </Dialog>

      <AlertDialog open={Boolean(pendingDatasource)} onOpenChange={(open) => { if (!open) setPendingDatasource(null) }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>新数据源缺少当前配置引用的字段</AlertDialogTitle>
            <AlertDialogDescription>
              切换后，引用不存在字段的列 / 筛选 / 表单会被移除。也可以按新数据源的字段全部重建。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter className="gap-2">
            <AlertDialogCancel>取消</AlertDialogCancel>
            <Button variant="outline" onClick={() => pendingDatasource && applyDatasource(pendingDatasource, false)}>
              保留可用配置
            </Button>
            <AlertDialogAction onClick={() => pendingDatasource && applyDatasource(pendingDatasource, true)}>
              按字段重建
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={confirmClose} onOpenChange={setConfirmClose}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>放弃未保存的修改？</AlertDialogTitle>
            <AlertDialogDescription>
              这次的改动还没有保存，关闭后不会保留。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>继续编辑</AlertDialogCancel>
            <AlertDialogAction className="bg-destructive text-white hover:bg-destructive/90" onClick={onClose}>
              放弃修改
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}

// ---------------------------------------------------------------------------
// 辅助
// ---------------------------------------------------------------------------

function fromRecord(record: PageAdminItem): PageDraft {
  return {
    pageKey: record.pageKey,
    title: record.title,
    description: record.description,
    datasourceKey: record.datasourceKey,
    list: parseConfig(record.listJson, EMPTY_LIST),
    form: parseConfig(record.formJson, EMPTY_FORM),
  }
}

function parseConfig<T>(json: string, fallback: T): T {
  if (!json.trim()) return fallback
  try {
    return { ...fallback, ...JSON.parse(json) } as T
  } catch {
    return fallback
  }
}

function isEmptyConfig(json: string, fallback: object): boolean {
  try {
    const parsed = JSON.parse(json)
    if (!parsed || typeof parsed !== "object" || Object.keys(parsed).length === 0) return true
    return JSON.stringify({ ...fallback, ...parsed }) === JSON.stringify(fallback)
  } catch {
    return true
  }
}

function SectionTitle({ title, hint }: { title: string; hint: string }) {
  return (
    <div>
      <h3 className="text-sm font-semibold">{title}</h3>
      <p className="mt-0.5 text-[11px] text-muted-foreground">{hint}</p>
    </div>
  )
}

function EmptyDatasource() {
  return (
    <p className="rounded-lg border border-dashed p-8 text-center text-sm text-muted-foreground">
      请先在「基本信息」里选择数据源。
    </p>
  )
}

function CopyBlock({ label, value }: { label: string; value: string }) {
  const [copied, setCopied] = useState(false)
  return (
    <div className="grid gap-1">
      <div className="flex items-center justify-between">
        <span className="font-mono text-[10px] text-muted-foreground">{label}</span>
        <Button
          variant="ghost"
          size="sm"
          className="h-6 gap-1 px-1.5 text-[10px]"
          onClick={async () => {
            try {
              await navigator.clipboard.writeText(value)
              setCopied(true)
              window.setTimeout(() => setCopied(false), 1500)
            } catch {
              setCopied(false)
            }
          }}
        >
          {copied ? <Check className="size-3" /> : <Copy className="size-3" />}
          {copied ? "已复制" : "复制"}
        </Button>
      </div>
      <pre className="max-h-52 overflow-auto rounded-md bg-muted/60 p-2 text-[10px] leading-relaxed">{value}</pre>
    </div>
  )
}
