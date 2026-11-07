package tech.smartboot.feat.demo.plus.tx;

import org.apache.ibatis.session.SqlSession;

/**
 * 事务体，无返回值。
 * <p>
 * 与 {@link TxWork} 分开命名，而不是用重载：{@code session -> mapper.update(x)} 这种表达式
 * 既是值兼容也是 void 兼容，重载会让编译器无从选择，而后人只会看到一个没法解释的编译错误。
 */
public interface TxAction {

    /**
     * @param session 当前事务的会话。不要 close，也不要 commit——生命周期由 {@link TxTemplate} 管。
     */
    void run(SqlSession session);
}
