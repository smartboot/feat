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
                        property: 'keywords',
                        content: 'smart-http,feat,web,java web,web服务器,java服务器,高性能web服务器',
                    }
                }, {
                    tag: 'meta',
                    attrs: {
                        property: 'description',
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
            plugins: [starlightLlmsTxt(),starlightImageZoomPlugin(),starlightScrollToTop({
                // Button position
                // Tooltip text
                tooltipText: 'Back to top',
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
            // 为此网站设置英语为默认语言。
            defaultLocale: 'root',
            locales: {
                root: {
                    label: '简体中文',
                    lang: 'zh-CN',
                },
                // 英文文档在 `src/content/docs/en/` 中。
                'en': {
                    label: 'English',
                    lang: 'en'
                }
            },
            sidebar: [
                {
                    label: '快速开始',
                    autogenerate: {directory: 'getting-started'},
                },
                {
                    label: '项目说明',
                    autogenerate: {directory: 'guides'},
                },
                {
                    label: 'Feat Core',
                    autogenerate: {directory: 'server'},
                },
                {
                    label: 'Feat Cloud',
                    autogenerate: {directory: 'cloud'},
                },
                {
                    label: 'Feat AI',
                    autogenerate: {directory: 'ai'},
                },
                {
                    label: '客户端',
                    autogenerate: {directory: 'client'},
                },
                {
                    label: '附录',
                    autogenerate: {directory: 'appendix'},
                },
                {
                    label: '支持项目',
                    items: ['sponsors'],
                },
            ],
        }),
    ],
});
