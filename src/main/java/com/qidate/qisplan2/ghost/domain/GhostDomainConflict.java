package com.qidate.qisplan2.ghost.domain;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import java.util.*;

/**
 * 鬼域对抗的纯空间求解器。输入只有来源标识与位置、原始范围、形状、等级和强度，
 * 不读取玩家、厉鬼、阵营或模块类型。每次都从原始范围求解，禁止迭代缩小。
 */
public final class GhostDomainConflict {
    private GhostDomainConflict() {}
    /** 球体为 1，圆柱为 0；与鬼域网络形状协议一致。 */
    public record Node(UUID id, Vec3 center, double radius, double strength, int grade, int shape, UUID source,
                       UUID region, List<AABB> isolation) {
        /** 不需要隔绝裁剪的旧几何调用。 */
        public Node(UUID id,Vec3 center,double radius,double strength,int grade,int shape,UUID source) {
            this(id,center,radius,strength,grade,shape,source,null,null);
        }
        /** 适配层提供隔绝空间快照；null 为仅做分组的客户端节点。 */
        public Node inIsolation(UUID region,List<AABB> boxes) {
            return new Node(id,center,radius,strength,grade,shape,source,region,boxes);
        }
        /** 不同隔绝空间完全不参与对抗，包括内外两个方向。 */
        public boolean sameSpace(Node other) { return Objects.equals(region,other.region); }
        /** 旧调用未提供来源时按独立来源处理，不能因缺省值互相豁免。 */
        public Node(UUID id,Vec3 center,double radius,double strength,int grade,int shape) {
            this(id,center,radius,strength,grade,shape,id);
        }
        /** 来源由宿主适配层提供；不读取具体实体类型、模块种类或阵营。 */
        public boolean sameSource(Node other) { return source.equals(other.source); }
        public Node {
            Objects.requireNonNull(id);
            if (isolation != null) isolation=List.copyOf(isolation);
            if (source == null) source=id;
            if (!Double.isFinite(radius) || radius < 0 || !Double.isFinite(strength) || strength < 0 || grade < 1)
                throw new IllegalArgumentException("Invalid domain conflict input");
        }
        /** 对给定范围执行真实几何包含查询，圆柱沿用框架的无限高度。 */
        public boolean contains(Vec3 point, double extent) {
            if (extent<=0) return false;
            if (isolation != null) {
                boolean inside=isolation.stream().anyMatch(box->box.contains(point));
                if (inside != (region != null)) return false;
            }
            double dx = point.x - center.x, dz = point.z - center.z, dy = shape == 1 ? point.y - center.y : 0;
            return dx * dx + dy * dy + dz * dz <= extent * extent;
        }
    }
    /** 不可变结算结果同时供归属、边界、能力和客户端表现使用。 */
    public static final class Result {
        private final List<Node> nodes;
        private final Map<UUID,Double> radii;
        private final Map<UUID,Set<UUID>> cuts;
        private Result(List<Node> nodes,Map<UUID,Double> radii,Map<UUID,Set<UUID>> cuts) {
            this.nodes=List.copyOf(nodes);this.radii=Map.copyOf(radii);
            Map<UUID,Set<UUID>> copy=new HashMap<>();cuts.forEach((id,set)->copy.put(id,Set.copyOf(set)));this.cuts=Map.copyOf(copy);
        }
        public List<Node> nodes(){return nodes;}
        /** 对抗范围为零表示完全压制，但仍保留原始声明以便压制解除后恢复。 */
        public double radius(UUID id){return radii.getOrDefault(id,0D);}
        /** 需要从本域中扣除的真实空间；不因相机或目标位于哪一侧改变。 */
        public boolean cuts(UUID owner,UUID other){return cuts.getOrDefault(owner,Set.of()).contains(other);}
        /** 几何覆盖与空间扣除后的归属，不使用“高等级全屏覆盖”。 */
        public boolean owns(UUID id,Vec3 point) {
            Node node=nodes.stream().filter(n->n.id().equals(id)).findFirst().orElse(null);
            if(node==null||!node.contains(point,radius(id)))return false;
            for(Node other:nodes)if(cuts(id,other.id())&&other.contains(point,radius(other.id())))return false;
            return true;
        }
        /** 只在本域真实外边界检查进出；被其他域割出的内部边缘不是外域的囚笼。 */
        public int crossings(UUID id,Vec3 from,Vec3 to) {
            Node node=nodes.stream().filter(n->n.id().equals(id)).findFirst().orElse(null);
            if(node==null||radius(id)<=0)return 0;
            Vec3 movement=to.subtract(from),delta=movement,offset=from.subtract(node.center());
            if(node.shape()==0){delta=new Vec3(delta.x,0,delta.z);offset=new Vec3(offset.x,0,offset.z);}
            var points=new TreeSet<Double>();
            double a=delta.lengthSqr(),b=offset.dot(delta),c=offset.lengthSqr()-radius(id)*radius(id);
            double discriminant=b*b-a*c;
            if(a>=1e-16 && discriminant>0) {
                double root=Math.sqrt(discriminant);
                addCrossing(points,(-b-root)/a); addCrossing(points,(-b+root)/a);
            }
            // 隔绝边界也是真实边界。按所有长方体交点分段，内部拼接面不会制造进出。
            if(node.isolation()!=null)for(var box:node.isolation())boxCrossings(points,box,from,movement);
            var ordered=new ArrayList<>(points); int result=0;
            for(int i=0;i<ordered.size();i++) {
                double t=ordered.get(i);
                double left=i==0?(t>0?t/2:-1e-7):(ordered.get(i-1)+t)/2;
                double right=i+1==ordered.size()?(t<1?(t+1)/2:1+1e-7):(t+ordered.get(i+1))/2;
                boolean before=node.contains(from.add(movement.scale(left)),radius(id));
                boolean after=node.contains(from.add(movement.scale(right)),radius(id));
                if(before==after)continue;
                Vec3 point=from.add(movement.scale(t));
                boolean cut=false;
                for(Node other:nodes)if(cuts(id,other.id())&&other.contains(point,radius(other.id()))){cut=true;break;}
                if(!cut)result|=after?1:2;
            }
            return result;
        }
        /** 同级外部交界按本地参与者强度分摊；嵌套内域已经排除外域，不再次削弱。 */
        public double weight(UUID id,Vec3 point) {
            if(!owns(id,point))return 0;
            Node self=nodes.stream().filter(n->n.id().equals(id)).findFirst().orElseThrow();
            double sum=0;int count=0;
            for (Node node:nodes)
                if((node.id().equals(self.id()) || !node.sameSource(self))
                        && node.sameSpace(self) && node.grade() == self.grade()
                        && owns(node.id(),point)) {

                    sum += node.strength();
                    count++;
                }
            return sum > 0 ? self.strength() / sum : 1D / Math.max(1, count);
        }
    }
    /** 成对独立求解并采用最严格的范围限制，结果与注册顺序无关。 */
    public static Result solve(Collection<Node> input) {
        List<Node> nodes=input.stream().sorted(Comparator.comparing(Node::id)).toList();
        Map<UUID,Double> radii=new HashMap<>();Map<UUID,Set<UUID>> cuts=new HashMap<>();
        nodes.forEach(n -> { radii.put(n.id(), n.radius()); cuts.put(n.id(), new HashSet<>()); });
        // 高等级先裁定完全压制；已失去领地的来源不再压制第三个域。
        List<Node> byGrade=nodes.stream().sorted(Comparator.comparingInt(Node::grade).reversed()).toList();
        for(Node low:byGrade)for(Node high:byGrade) {
            if(low.sameSpace(high) && !low.sameSource(high) && high.grade() - low.grade() >= 3 && radii.get(high.id()) > 0 && high.contains(low.center(),high.radius())) {
                radii.put(low.id(), 0D);break;
            }
        }
        Set<UUID> suppressed=new HashSet<>();
        radii.forEach((id,radius)->{if(radius<=0)suppressed.add(id);});
        for(int i=0;i<nodes.size();i++)for(int j=i+1;j<nodes.size();j++){
            Node a=nodes.get(i),b=nodes.get(j);
            if(!a.sameSpace(b) || a.sameSource(b) || suppressed.contains(a.id()) || suppressed.contains(b.id()))continue;
            double overlap=overlapArea(a,b);if(overlap<=0)continue;
            boolean aInB=b.contains(a.center(),b.radius()),bInA=a.contains(b.center(),a.radius());
            // 源头进入高三级鬼域时，无论两者几何大小如何都完全压制。
            if(aInB && b.grade()-a.grade()>=3)radii.put(a.id(),0D);
            if(bInA && a.grade()-b.grade()>=3)radii.put(b.id(),0D);
            Node inner=null,outer=null;
            if(aInB&&!bInA){inner=a;outer=b;}
            else if(bInA&&!aInB){inner=b;outer=a;}
            else if(aInB&&bInA){
                // 双方来源互在范围内：几何较小者作为内域；完全同形同级时保持对称交界。
                if(a.radius()!=b.radius()){inner=a.radius()<b.radius()?a:b;outer=inner==a?b:a;}
                else if(a.grade()!=b.grade()){inner=a.grade()<b.grade()?a:b;outer=inner==a?b:a;}
            }
            if(inner!=null){
                int difference=outer.grade()-inner.grade();
                if(difference>=3)radii.put(inner.id(),0D);
                else if(difference>=0)limit(radii,inner,nestedRadius(inner.radius(),inner.strength(),outer.strength(),difference));
                if(difference==0)limit(radii,outer,nestedRadius(outer.radius(),outer.strength(),inner.strength(),0));
                cuts.get(outer.id()).add(inner.id());
            }else if(a.grade()!=b.grade()){
                Node low=a.grade()<b.grade()?a:b,high=low==a?b:a;
                int difference=high.grade()-low.grade();
                cuts.get(low.id()).add(high.id());
                if(difference<3)limit(radii,low,edgeRadius(low.radius(),low.strength(),high.strength(),difference,
                        overlap/(Math.PI*low.radius()*low.radius())));
            }
        }
        // 完全压制的域不能继续从其他鬼域中挖空空间。
        return new Result(nodes,radii,cuts);
    }
    /** 只保留本次线段中的精确交点。 */
    private static void addCrossing(Set<Double> points,double t) { if(t>=0 && t<=1)points.add(t); }
    /** 射线与长方体的 slab 求交；不按固定步长采样，支持跨越薄墙和不规则区域。 */
    private static void boxCrossings(Set<Double> points,AABB box,Vec3 from,Vec3 delta) {
        double lo=Double.NEGATIVE_INFINITY,hi=Double.POSITIVE_INFINITY;
        double[] starts={from.x,from.y,from.z},steps={delta.x,delta.y,delta.z};
        double[] mins={box.minX,box.minY,box.minZ},maxs={box.maxX,box.maxY,box.maxZ};
        for(int axis=0;axis<3;axis++) {
            if(Math.abs(steps[axis])<1e-16) { if(starts[axis]<mins[axis]||starts[axis]>=maxs[axis])return; continue; }
            double x=(mins[axis]-starts[axis])/steps[axis],y=(maxs[axis]-starts[axis])/steps[axis];
            lo=Math.max(lo,Math.min(x,y));hi=Math.min(hi,Math.max(x,y));
            if(lo>=hi)return;
        }
        addCrossing(points,lo);addCrossing(points,hi);
    }
    private static void limit(Map<UUID,Double> radii,Node node,double value){radii.compute(node.id(),(id,old)->Math.min(old,value));}
    /** 内域按等级差与相对强度收缩；强度优势不把范围扩大到原始声明之外。 */
    public static double nestedRadius(double radius,double own,double other,int difference){
        if(difference>=3)return 0;
        if(difference<0)return radius;
        double ratio=other>0?own/other:1;
        return Math.max(0,Math.min(radius,Math.floor(radius/Math.pow(2,difference)*ratio)));
    }
    /** 外部交界范围：(原始范围－压制量)×未覆盖面积占比；覆盖越多，剩余范围越小。无交集时不调用。 */
    public static double edgeRadius(double radius,double own,double other,int difference,double covered){
        double share=own+other>0?own/(own+other):.5;
        return Math.max(0,Math.min(radius,(radius-radius/Math.pow(2,difference)*share)*(1-Math.clamp(covered,0,1))));
    }
    /** 水平投影圆的精确交叠面积；球体垂直完全分离时没有交叠。 */
    public static double overlapArea(Node a,Node b){
        double r=a.radius(),s=b.radius();if(r<=0||s<=0)return 0;
        if(a.shape()==1&&b.shape()==1&&a.center().distanceToSqr(b.center())>=(r+s)*(r+s))return 0;
        double d=Math.sqrt(Math.pow(a.center().x-b.center().x,2)+Math.pow(a.center().z-b.center().z,2));
        if(d>=r+s)return 0;if(d<=Math.abs(r-s))return Math.PI*Math.min(r,s)*Math.min(r,s);
        double first=r*r*Math.acos(Math.clamp((d*d+r*r-s*s)/(2*d*r),-1,1));
        double second=s*s*Math.acos(Math.clamp((d*d+s*s-r*r)/(2*d*s),-1,1));
        return first+second-.5*Math.sqrt(Math.max(0,(-d+r+s)*(d+r-s)*(d-r+s)*(d+r+s)));
    }
}
