// @ts-check
import {defineConfig} from 'astro/config';
import starlight from '@astrojs/starlight';
import starlightImageZoomPlugin from "starlight-image-zoom";
import starlightScrollToTop from 'starlight-scroll-to-top';
import mermaid from 'astro-mermaid';
import starlightLlmsTxt from 'starlight-llms-txt'

// https://astro.build/config
export default defineConfig({
    site: 'https://smartboot.tech/',
    base: '/feat',
    trailingSlash: "always",
    integrations: [mermaid({
        theme: 'forest',
        autoTheme: true
    }),
        starlight({
            title: 'FEAT',
            logo: {
                src: './src/assets/feat_logo_v2.svg',
            },
            customCss: [
                // 你的自定义 CSS 文件的相对路径
                './src/styles/custom.css',
            ],
            head: [
                {
                    tag: 'meta',
                    attrs: {
                        name: 'keywords',
                        content: 'smart-http,feat,web,java web,web服务器,java服务器,高性能web服务器',
                    }
                }, {
                    tag: 'meta',
                    attrs: {
                        name: 'description',
                        content: 'Feat是一款基于Java语言开发的轻量级、高性能Web服务器',
                    }
                },
                {
                    tag: 'script',
                    content: `
                var _hmt = _hmt || [];
                (function() {
                  var hm = document.createElement("script");
                  hm.src = "https://hm.baidu.com/hm.js?ee8630857921d8030d612dbd7d751b55";
                  var s = document.getElementsByTagName("script")[0]; 
                  s.parentNode.insertBefore(hm, s);
                })();
          `
                }
            ],
            social: [
                {icon: 'github', label: 'GitHub', href: 'https://github.com/smartboot/feat'},
                {icon: 'seti:git', label: 'Gitee', href: 'https://gitee.com/smartboot/feat'}
            ],
            plugins: [starlightLlmsTxt({
                customSets: [
                    {
                        label: '理解 Feat',
                        description: 'Feat 核心概念和设计原理文档',
                        paths: ['guides/**'],
                    },
                    {
                        label: '实践指南',
                        description: '安全、部署、性能、测试等实践类文档',
                        paths: ['practices/**'],
                    },
                    {
                        label: 'Feat Cloud',
                        description: 'Feat 云原生模块文档',
                        paths: ['cloud/**'],
                    },
                    {
                        label: 'Feat AI',
                        description: 'Feat AI 集成模块文档',
                        paths: ['ai/**'],
                    },
                    {
                        label: 'Feat Core 服务端',
                        description: 'Feat 核心服务器模块文档',
                        paths: ['server/**'],
                    },
                    {
                        label: 'Feat Core 客户端',
                        description: 'Feat 客户端相关文档',
                        paths: ['client/**'],
                    },
                    {
                        label: '附录',
                        description: 'Feat 附录和参考文档',
                        paths: ['appendix/**'],
                    },
                ],
            }),starlightImageZoomPlugin(),starlightScrollToTop({
                // Button position
                // Tooltip text
                tooltipText: '回到顶部',
                showTooltip: true,
                // Use smooth scrolling
                // smoothScroll: true,
                // Visibility threshold (show after scrolling 20% down)
                threshold: 20,
                // Customize the SVG icon
                borderRadius: '50',
                // Show scroll progress ring
                showProgressRing: true,
                // Customize progress ring color
                progressRingColor: '#ff6b6b',
            })],
            // 为此网站设置中文为默认语言。
            defaultLocale: 'root',
            locales: {
                root: {
                    label: '简体中文',
                    lang: 'zh-CN',
                },
            },
            // 展示页面最后更新时间与"编辑此页"入口
            lastUpdated: true,
            editLink: {
                baseUrl: 'https://github.com/smartboot/feat/edit/master/pages/',
            },
            sidebar: [
                {
                    label: '理解 Feat',
                    items: [{autogenerate: {directory: 'guides'}}],
                },
                {
                    label: '实践指南',
                    items: [{autogenerate: {directory: 'practices'}}],
                },
                {
                    label: 'Feat Cloud',
                    badge: { text: 'VIP', variant: 'danger' },
                    items: [{  autogenerate: {directory: 'cloud'}},'sponsors'],
                },
                {
                    label: 'Feat AI',
                    items: [{  autogenerate: {directory: 'ai'}}],
                },
                {
                    label: 'Feat Core',
                    items: [{
                        label: '服务端',
                        items: [{autogenerate: {directory: 'server'}}],
                    },{
                        label: '客户端',
                        items: [{autogenerate: {directory: 'client'}}],
                    }],
                },
                {
                    label: '附录',
                    items: [{ autogenerate: {directory: 'appendix'}}],
                },
            ],
        }),
    ],
});
