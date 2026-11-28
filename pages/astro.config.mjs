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
                        label: '认识 Feat',
                        description: 'Feat 的定位、模块关系和设计原理',
                        paths: ['guides/**'],
                    },
                    {
                        label: '构建业务应用',
                        description: '从第一个接口到完整业务应用的开发教程',
                        paths: ['building/**'],
                    },
                    {
                        label: '实践指南',
                        description: '安全、部署、性能、测试等实践类文档',
                        paths: ['practices/**'],
                    },
                    {
                        label: 'Feat Cloud',
                        description: 'Feat Cloud 注解式业务应用开发模型',
                        paths: ['cloud/**'],
                    },
                    {
                        label: 'Feat AI',
                        description: 'Feat AI 模型、Agent 与应用能力',
                        paths: ['ai/**'],
                    },
                    {
                        label: 'Feat Core 服务端',
                        description: 'Feat Core HTTP 服务端文档',
                        paths: ['server/**'],
                    },
                    {
                        label: 'Feat Core 客户端',
                        description: 'Feat Core HTTP 客户端文档',
                        paths: ['client/**'],
                    },
                    {
                        label: '技术参考',
                        description: '版本、基准测试和补充参考资料',
                        paths: ['appendix/**'],
                    },
                ],
            }),starlightImageZoomPlugin(),starlightScrollToTop({
                tooltipText: '回到顶部',
                showTooltip: true,
                threshold: 20,
                borderRadius: '50',
                showProgressRing: true,
                progressRingColor: '#ff6b6b',
            })],
            defaultLocale: 'root',
            locales: {
                root: {
                    label: '简体中文',
                    lang: 'zh-CN',
                },
            },
            lastUpdated: true,
            editLink: {
                baseUrl: 'https://github.com/smartboot/feat/edit/master/pages/',
            },
            sidebar: [
                {
                    label: '认识 Feat',
                    items: [{autogenerate: {directory: 'guides'}}],
                },
                {
                    label: '开始使用',
                    items: [
                        {label: 'Feat Core', link: '/server/'},
                        {label: 'Feat Cloud', link: '/cloud/'},
                        {label: 'Feat AI', link: '/ai/'},
                    ],
                },
                {
                    label: '构建业务应用',
                    items: [{autogenerate: {directory: 'building'}}],
                },
                {
                    label: '实践指南',
                    items: [{autogenerate: {directory: 'practices'}}],
                },
                {
                    label: 'Feat Cloud',
                    items: [{autogenerate: {directory: 'cloud'}}, 'sponsors'],
                },
                {
                    label: 'Feat AI',
                    items: [{autogenerate: {directory: 'ai'}}],
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
                    items: [{autogenerate: {directory: 'appendix'}}],
                },
            ],
        }),
    ],
});
