import { defineConfig } from "vitest/config"
import { fileURLToPath, URL } from "node:url"

export default defineConfig({
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: "./src/test/setup.ts",
    /**
     * 默认的 5 秒不够。
     *
     * PageStudio 的用例要渲染整个工作台（一千多行的配置编辑器 + 一张预览表 + 十来个
     * Radix Select），一次点击就是几百毫秒。单文件跑时刚好压线，全量并行跑时会被挤爆，
     * 于是「有时候红、单独跑又绿」——这种假红比慢更浪费时间。
     */
    testTimeout: 20000,
  },
})
