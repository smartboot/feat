import { describe, expect, it } from "vitest"

import type { DwFormConfig, DwListConfig } from "@/api/types"
import { resolveFormConfig, resolveListConfig } from "@/components/dw/resolve"

/**
 * 值域合并的规则断言。
 *
 * 规则本身只有一条（视图没配就继承字段的），但它是「预览等于线上」的地基：
 * 后端 `PageService.applyFieldMaps` 在**下发副本**上做同一件事，前端在**预览**上做。
 * 两边但凡有一次口径不一致，配置者就会看到「配了但没生效」，然后去配第二遍。
 */

const MAPPED = [
  { value: "1", label: "已启用" },
  { value: "0", label: "已停用" },
]

function list(overrides: Partial<DwListConfig> = {}): DwListConfig {
  return {
    rowKey: "id",
    pageSize: 20,
    actions: ["refresh"],
    columns: [{ key: "status", label: "状态", format: "text", width: "" }],
    filters: [],
    fieldMaps: { status: MAPPED },
    ...overrides,
  }
}

const form: DwFormConfig = {
  createTitle: "新建",
  editTitle: "编辑",
  createDescription: "",
  editDescription: "",
  fields: [{ key: "status", label: "状态", widget: "select", options: [], span: 1 }],
}

describe("resolveListConfig", () => {
  it("列没配映射时继承字段值域", () => {
    const resolved = resolveListConfig(list())
    expect(resolved.columns[0].options).toEqual(MAPPED)
  })

  it("列自己配了就用列的——覆盖，不是合并", () => {
    const own = [{ value: "1", label: "正常" }]
    const resolved = resolveListConfig(
      list({ columns: [{ key: "status", label: "状态", format: "badge", width: "", options: own }] }),
    )
    expect(resolved.columns[0].options).toEqual(own)
  })

  it("配了 optionsApi 的筛选不继承静态值域（两个来源同时生效时谁赢说不清）", () => {
    const resolved = resolveListConfig(
      list({
        filters: [
          { key: "status", label: "状态", widget: "select", optionsApi: "/api/status" },
          { key: "status", label: "状态", widget: "select" },
        ],
      }),
    )
    expect(resolved.filters[0].options).toBeUndefined()
    expect(resolved.filters[1].options).toEqual(MAPPED)
  })

  it("没有任何值域时原样返回同一个引用（memo / useMemo 不被白白打穿）", () => {
    const input = list({ fieldMaps: {} })
    expect(resolveListConfig(input)).toBe(input)
  })

  it("不改动入参：继承的是复制出来的数组，不共享引用", () => {
    const input = list()
    const resolved = resolveListConfig(input)
    expect(input.columns[0].options).toBeUndefined()
    expect(resolved.columns[0].options).not.toBe(MAPPED)
  })

  it("没命中值域的列保持原样", () => {
    const input = list({
      fieldMaps: { other: MAPPED },
      columns: [{ key: "name", label: "名称", format: "text", width: "" }],
    })
    expect(resolveListConfig(input)).toBe(input)
  })
})

describe("resolveFormConfig", () => {
  it("表单下拉没配选项时继承字段值域", () => {
    expect(resolveFormConfig(list(), form).fields[0].options).toEqual(MAPPED)
  })

  it("配了 optionsApi 的表单字段不继承", () => {
    const remote: DwFormConfig = {
      ...form,
      fields: [{ ...form.fields[0], optionsApi: "/api/status" }],
    }
    expect(resolveFormConfig(list(), remote).fields[0].options).toEqual([])
  })
})
