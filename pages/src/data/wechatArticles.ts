export interface WechatArticle {
  title: string;
  href: string;
  publishedAt: string;
  summary: string;
  imageUrl?: string;
  imageAlt?: string;
  featured?: boolean;
}

export const wechatArticles: WechatArticle[] = [
  {
    title: 'Feat vs Quarkus：结果比我预想的还要大',
    href: 'https://mp.weixin.qq.com/s/7goVCSPWksdCbm66rKPZWA',
    publishedAt: '2026-07-09',
    summary: '本次测试涵盖两个经典场景：JSON 响应 和 Hello World，参与对比的框架包括 Feat、Quarkus、Spring Boot 和 Vert.x。',
    imageUrl:'https://fastly.jsdelivr.net/gh/bucketio/img7@main/2026/07/09/1783561263484-7fa06cb8-bb3b-409f-8a3c-c71058292748.png',
    featured: true,
  },
  {
    title: '为什么 Java 社区挑战 Spring 的框架，最后都输给了"用户体验"？',
    href: 'https://mp.weixin.qq.com/s/_O0h3vFFPmY9mAXqNxiaXA',
    publishedAt: '2026-07-04',
    summary: '几乎每个 Java 开发者都吐槽过 Spring：启动慢、吃内存、依赖臃肿。改一行代码，等五秒重启...',
    imageUrl:'https://fastly.jsdelivr.net/gh/bucketio/img0@main/2026/07/04/1783134695747-bd5d9efb-b8b1-4197-9606-969989dd7caf.png',
    featured: true,
  },
    {
    title: '启动 8ms，内存仅 6MB! 这款 Java 框架把云原生做到了极致',
    href: 'https://mp.weixin.qq.com/s/Jn0akkWfw5-XUohILN5QpQ',
    publishedAt: '2025-08-24',
    summary: '从启动速度、内存占用和部署体验切入，介绍 Feat 面向云原生场景的轻量化能力。',
    featured: true,
  },
  {
    title: '也许，现在正是重塑 Java 生态最好的时候',
    href: 'https://mp.weixin.qq.com/s/lHVY3Vam9TBlMfU-CVo_Gg',
    publishedAt: '2025-03-12',
    summary: '围绕 Java Web 开发生态的演进机会，讨论 Feat 希望解决的问题和长期方向。',
  },
  {
    title: '轻量级 Java 框架的性能之王',
    href: 'https://mp.weixin.qq.com/s/r4IgzoqHCKzYbATTI44anw',
    publishedAt: '2025-03-09',
    summary: '聚焦框架吞吐、资源效率和轻量运行时，展示 Feat 在高性能 Web 服务场景中的优势。',
  },
  {
    title: 'DeepSeek 助力打造的国产 Java Web 服务开发框架',
    href: 'https://mp.weixin.qq.com/s/azMX0KA6RzdkPaLC5YG7Ng',
    publishedAt: '2025-02-11',
    summary: '介绍 Feat 与 AI 原生开发体验的结合，以及国产 Java Web 框架的新探索。',
  },
];

export const sortedWechatArticles = [...wechatArticles].sort((left, right) => {
  const leftTime = new Date(left.publishedAt).getTime();
  const rightTime = new Date(right.publishedAt).getTime();
  return rightTime - leftTime;
});
