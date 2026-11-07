package tech.smartboot.feat.demo.plus.tx;

import org.apache.ibatis.session.SqlSession;

/**
 * 事务体，有返回值。
 */
public interface TxWork<T> {

    /**
     * @param session 当前事务的会话。不要 close，也不要 commit——生命周期由 {@link TxTemplate} 管。
     */
    T run(SqlSession session);
}
