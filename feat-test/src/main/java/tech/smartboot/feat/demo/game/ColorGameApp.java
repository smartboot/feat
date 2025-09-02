/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.demo.game;

import tech.smartboot.feat.Feat;
import tech.smartboot.feat.core.server.handler.HttpStaticResourceHandler;

/**
 * 一个适合3岁小朋友玩的颜色匹配游戏
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ColorGameApp {

    public static void main(String[] args) {
        Feat.httpServer().httpHandler(new HttpStaticResourceHandler(opt -> opt.baseDir("classpath:static/game"))).listen(8080);

        System.out.println("颜色游戏已启动，请访问 http://localhost:8080");
    }
}