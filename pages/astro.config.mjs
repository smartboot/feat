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
                    tag:'script',
                    attrs: {
                        src: 'https://smartboot.tech/js/gitee.js'
                    }
                },{
                    tag:'script',
                    content: `if(!location.pathname.endsWith("feat/")&&!location.pathname.endsWith("/unstar/")&&!location.pathname.endsWith("/unauth/")){
                                checkStar("smartboot","feat",function(){
                                    location.href="/feat/unstar/";
                                });
                            }`
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
                    label: '1. 关于',
                    autogenerate: {directory: 'guides'},
                },
                {
                    label: '2. Feat Server开发',
                    autogenerate: {directory: 'server'},
                },
                {
                    label: '3. Feat Cloud开发',
                    autogenerate: {directory: 'cloud'},
                },
                {
                    label: '4. Feat Client开发',
                    autogenerate: {directory: 'client'},
                },
                {
                    label: '5. Feat AI开发',
                    autogenerate: {directory: 'ai'},
                },
                {
                    label: '6. 附录',
                    autogenerate: {directory: 'appendix'},
                },
            ],
        }),
    ],
});
