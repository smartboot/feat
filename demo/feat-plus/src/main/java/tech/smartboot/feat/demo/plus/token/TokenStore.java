package tech.smartboot.feat.demo.plus.token;

public interface TokenStore {
    IssuedToken issue(long userId);

    TokenSession find(String token);

    void remove(String token);

    /**
     * 踢掉某个用户的其它会话，保留 keepToken 对应的这一个。
     * <p>
     * 改密码之后必须调用：密码是「凭证失效」的唯一信号，
     * 如果旧密码换掉后旧会话还能继续用，改密码就成了心理安慰。
     * 保留当前会话是为了不让操作者自己被踢出去、还得重新登录。
     *
     * @return 被踢掉的会话数
     */
    int removeOthers(long userId, String keepToken);
}
