package com.qidate.qisplan2.death;

/** 目标本体是否已是厉鬼的身份端口；承载模块、携带灵异物品或受到诅咒均不等于此身份。 */
public interface GhostBodyIdentity {
    /** 本体是厉鬼时返回 true；未来异类可由其身份适配实现，不依赖意识或玩家类型。 */
    boolean isGhostBody();

    /** 只读取明确声明的本体身份，未实现端口的目标默认仍是普通生命。 */
    static boolean isGhost(Object target) {
        return target instanceof GhostBodyIdentity identity && identity.isGhostBody();
    }
}
