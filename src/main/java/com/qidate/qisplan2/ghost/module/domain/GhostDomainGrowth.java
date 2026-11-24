package com.qidate.qisplan2.ghost.module.domain;

/** 鬼域成长契约。只接收模块基础强度，不识别宿主或临时对抗削弱。 */
public interface GhostDomainGrowth {
    GhostDomainGrowth STANDARD = incremental(50, 20, 1.5, 10);
    /** 达到指定等级所需的总强度；不是升级消耗。 */
    double requiredIntensity(int grade);
    /** 此策略允许的最高等级，当前框架支持 1～10。 */
    int maxGrade();
    /** 只返回公式解锁的等级；返回零不代表鬼域消失，声明层还必须保留 initialLayer 下限。 */
    default int gradeFor(double intensity) {
        if (!Double.isFinite(intensity) || intensity < 0) return 0;
        for (int grade=maxGrade(); grade>=1; grade--)
            if (intensity>=requiredIntensity(grade)) return grade;
        return 0;
    }
    /** 线性预设，每次升级新增成本相同。 */
    static GhostDomainGrowth linear(double first,double step,int maximum) {
        return incremental(first,step,1,maximum);
    }
    /** 递增新增成本，只在声明时计算一次，运行时读取门槛表。 */
    static GhostDomainGrowth incremental(double first,double step,double ratio,int maximum) {
        if (!Double.isFinite(first)||first<0||!Double.isFinite(step)||step<=0
                ||!Double.isFinite(ratio)||ratio<1||maximum<1||maximum>10)
            throw new IllegalArgumentException("Invalid domain growth parameters");
        double[] values=new double[maximum];
        for(int i=0;i<maximum;i++)
            values[i]=Math.ceil(first+step*(ratio==1?i:(Math.pow(ratio,i)-1)/(ratio-1)));
        return table(values);
    }
    /** 自定义门槛表；复制输入并验证，避免声明后的外部修改污染规则。 */
    static GhostDomainGrowth table(double... thresholds) {
        if(thresholds.length<1||thresholds.length>10)throw new IllegalArgumentException("Invalid grade count");
        double[] values=thresholds.clone();
        for(int i=0;i<values.length;i++)
            if(!Double.isFinite(values[i])||values[i]<0||i>0&&values[i]<=values[i-1])
                throw new IllegalArgumentException("Thresholds must be finite and increasing");
        return new GhostDomainGrowth() {
            /** 总门槛已取整，不能把每一级新增成本分别取整后再累加。 */
            public double requiredIntensity(int grade) {
                if(grade<1||grade>values.length)throw new IllegalArgumentException("Invalid grade");
                return values[grade-1];
            }
            /** 门槛数量就是此策略的等级上限。 */
            public int maxGrade(){return values.length;}
        };
    }
}
