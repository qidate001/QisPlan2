package com.qidate.qisplan2.ghost.domain;
/** 一次鬼域事实派发的强度作用域；只影响本次能力，不改写模块持久强度。 */
public final class GhostDomainInfluenceScope {
    private static final ThreadLocal<Double> WEIGHT=ThreadLocal.withInitial(()->1D);
    private GhostDomainInfluenceScope() {}
    /** 当前事实的局部有效强度倍率。 */
    public static double weight(){return WEIGHT.get();}
    /** 延迟裁决仍使用产生事实时的倍率，不受后续目标派发影响。 */
    public static Runnable capture(Runnable action) {
        double captured = weight();
        return () -> run(captured, action);
    }
    /** 嵌套事实和异常都会恢复外层倍率，不污染随后独立的技能调用。 */
    public static void run(double weight,Runnable action){
        double previous=WEIGHT.get();WEIGHT.set(Math.clamp(weight,0,1));
        try{action.run();}finally{WEIGHT.set(previous);}
    }
}
