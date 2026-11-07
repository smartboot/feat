package tech.smartboot.feat.demo.plus.tx;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;

/**
 * 事务模板：写操作的唯一入口。
 * <p>
 * <b>为什么不是 {@code @Transactional}。</b>Feat 的装配在编译期完成，为一个注解在注解处理器里实现
 * 「传播行为、只读、回滚规则、内层回滚外层不复位」等于把 Spring 最重的那部分搬过来，
 * 与「生成的代码人能读懂」这个前提相反。模板的边界显式、可读、可 grep：
 *
 * <pre>
 * tx.run(session -&gt; {
 *     UserMapper users = session.getMapper(UserMapper.class);
 *     users.insert(user);
 *     users.insertUserRole(user.getId(), "ADMIN", operatorId);
 * });
 * </pre>
 *
 * <b>三条必须知道的规则</b>（都是「做错了不报错」的类型）：
 * <ol>
 *   <li><b>事务体里只能用 {@code session.getMapper(...)}。</b>注入进来的 Mapper 是生成代码包过的版本，
 *       每个方法自己 {@code openSession(true)} 独立提交；混用一个，那一笔写就落在事务外，
 *       接口照常返回成功，回滚时也不会被收回。</li>
 *   <li><b>跨 Service 不传播，除非对方也走模板。</b>本类用线程绑定实现「内层加入外层」：
 *       事务体里调用的另一个 Service 如果也通过 {@code tx.run} 写库，会复用同一个会话，同生共死。
 *       但对方若直接用手上的注入 Mapper 写，仍然是独立提交——所以规则 1 对每一层都成立。</li>
 *   <li><b>缓存失效要放在事务之外。</b>提交前清缓存，别的线程会立刻用提交前的数据把缓存填回去，
 *       于是「改完已生效」变成「改完十分钟后才生效」。本类不处理缓存，调用方自己注意顺序。</li>
 * </ol>
 *
 * <b>为什么审计记录加入事务而不是独立开会话。</b>「创建了用户张三」这条记录如果比业务写先落地，
 * 一旦业务回滚，审计就在说谎；反过来业务提交了而审计没写，则追不到人。两者原子，日志才可信。
 * 失败的操作本来就不该出现在「谁改了什么」里——那属于访问日志的职责。
 * <p>
 * <b>注入它的字段必须叫 {@code txTemplate}。</b>Feat 的 {@code @Autowired} 按<b>字段名</b>去容器里取 bean，
 * 不看类型；取不到不会退化成空值，而是启动时抛「bean[xxx] not ready」。
 * 想写成 {@code tx} 就得在类上加 {@code @Bean("tx")}，两处必须同时改。
 */
@Bean
public class TxTemplate {

    /**
     * 当前线程正在进行的会话。用 ThreadLocal 是唯一能在不改方法签名的前提下让内层加入外层的做法；
     * 危害（跨线程失效）在这里不存在：模板从开始到结束全在同一个请求线程上同步执行。
     */
    private static final ThreadLocal<SqlSession> CURRENT = new ThreadLocal<>();

    @Autowired
    private SqlSessionFactory sessionFactory;

    /**
     * 在事务里执行并返回结果。异常时回滚并原样抛出，业务代码不需要自己 try / catch。
     */
    public <T> T call(TxWork<T> work) {
        SqlSession bound = CURRENT.get();
        if (bound != null) {
            // 内层加入外层：不提交、不回滚、不关闭，把决定权留给最外层。
            return work.run(bound);
        }

        SqlSession session = sessionFactory.openSession(false);
        CURRENT.set(session);
        boolean committed = false;
        try {
            T result = work.run(session);
            session.commit();
            committed = true;
            return result;
        } finally {
            CURRENT.remove();
            if (!committed) {
                rollbackQuietly(session);
            }
            closeQuietly(session);
        }
    }

    /**
     * 在事务里执行，无返回值。
     */
    public void run(TxAction action) {
        call(session -> {
            action.run(session);
            return null;
        });
    }

    /**
     * 当前线程是否已处于事务中。给「必须在事务里调用」的断言用，也方便排查嵌套问题。
     */
    public boolean active() {
        return CURRENT.get() != null;
    }

    /**
     * 回滚失败不能盖掉真正的业务异常——那会让排查方向从「插入失败」变成「连接池问题」。
     * 数据源是 MyBatis 的 PooledDataSource，漏 close 会耗尽连接池，表现为服务假死而不是报错，
     * 所以 close 一定在 finally 里，且自身不抛。
     */
    private static void rollbackQuietly(SqlSession session) {
        try {
            session.rollback();
        } catch (RuntimeException e) {
            System.out.println("[feat-plus] 事务回滚失败：" + e.getMessage());
        }
    }

    private static void closeQuietly(SqlSession session) {
        try {
            session.close();
        } catch (RuntimeException e) {
            System.out.println("[feat-plus] 会话关闭失败：" + e.getMessage());
        }
    }
}
