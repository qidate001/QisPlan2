// 所有标准鬼域效果共享的空间归属契约；不包含具体厉鬼名称。
uniform sampler2D DomainConflictGeometry;
uniform sampler2D DomainConflictCuts;
uniform int DomainConflictCount;
uniform int DomainConflictIndex;

bool qisDomainContains(int index, vec3 point) {
    vec4 geometry=texelFetch(DomainConflictGeometry,ivec2(0,index),0);
    vec4 properties=texelFetch(DomainConflictGeometry,ivec2(1,index),0);
    vec3 offset=point-geometry.xyz;
    if(properties.z<0.5)offset.y=0.0;
    return geometry.w>0.0 && dot(offset,offset)<=geometry.w*geometry.w;
}
bool qisDomainOwns(int index,vec3 point) {
    if(!qisDomainContains(index,point))return false;
    for(int other=0;other<DomainConflictCount;other++)
        if(texelFetch(DomainConflictCuts,ivec2(other,index),0).r>0.5 && qisDomainContains(other,point))return false;
    return true;
}
float qisDomainWeight(vec3 point) {
    int index=DomainConflictIndex;
    if(index<0 || !qisDomainOwns(index,point))return 0.0;
    vec4 own=texelFetch(DomainConflictGeometry,ivec2(1,index),0);
    float total=0.0;int count=0;
    for(int other=0;other<DomainConflictCount;other++) {
        vec4 properties=texelFetch(DomainConflictGeometry,ivec2(1,other),0);
        if(abs(properties.y-own.y)<0.5 && qisDomainOwns(other,point)){total+=properties.x;count++;}
    }
    return total>0.0 ? own.x/total : 1.0/float(max(1,count));
}
// 取得每个球体/圆柱与视线的交点，用实际几何边界分段，不用固定步长采样。
vec2 qisDomainInterval(int index,vec3 origin,vec3 direction) {
    vec4 geometry=texelFetch(DomainConflictGeometry,ivec2(0,index),0);
    vec3 offset=origin-geometry.xyz,ray=direction;
    if(texelFetch(DomainConflictGeometry,ivec2(1,index),0).z<0.5){offset.y=0.0;ray.y=0.0;}
    float a=dot(ray,ray),b=dot(offset,ray),c=dot(offset,offset)-geometry.w*geometry.w;
    if(geometry.w<=0.0)return vec2(1e30);
    if(a<1e-12)return c<=0.0?vec2(-1e30,1e30):vec2(1e30);
    float discriminant=b*b-a*c;
    if(discriminant<0.0)return vec2(1e30);
    float root=sqrt(max(0.0,discriminant));return vec2((-b-root)/a,(-b+root)/a);
}
float qisConflictLength(vec3 origin,vec3 direction,float start,float end) {
    if(DomainConflictCount<=0)return max(0.0,end-start);
    float cursor=start,total=0.0;
    // 每个域最多提供两个交点；区间内归属与强度比例恒定，因此积分是精确的。
    for(int step=0;step<DomainConflictCount*2+2 && cursor<end;step++) {
        float next=end;
        for(int index=0;index<DomainConflictCount;index++) {
            vec2 interval=qisDomainInterval(index,origin,direction);
            if(interval.x>cursor+0.00001)next=min(next,interval.x);
            if(interval.y>cursor+0.00001)next=min(next,interval.y);
        }
        total+=(next-cursor)*qisDomainWeight(origin+direction*((cursor+next)*0.5));
        cursor=next;
    }
    return total;
}
