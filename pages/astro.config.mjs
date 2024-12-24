// @ts-check
import {defineConfig} from 'astro/config';
import starlight from '@astrojs/starlight';
import starlightImageZoomPlugin from "starlight-image-zoom";

// https://astro.build/config
export default defineConfig({
    site: 'https://smartboot.tech/',
    base: '/feat',
    trailingSlash: "always",
    integrations: [
        starlight({
            title: 'FEAT',
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
                }
            ],
            social: {
                github: 'https://github.com/smartboot/feat',
            },
            plugins: [starlightImageZoomPlugin()],
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
                    label: '关于',
                    autogenerate: {directory: 'guides'},
                },
                {
                    label: '用户手册',
                    autogenerate: {directory: 'reference'},
                },
            ],
        }),
    ],
});
