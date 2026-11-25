package com.qidate.qisplan2.ghost.domain.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qidate.qisplan2.ghost.domain.client.ClientGhostDomainManager;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import java.util.*;

/** 标准鬼域空间数据纹理。按真实实例生成，无厉鬼类型分支，也没有固定鬼域数量上限。 */
public final class GhostDomainConflictTextures {
    private static int geometry,cuts;
    private static List<UUID> ids=List.of();
    private static final Map<Integer,float[]> uploaded=new HashMap<>();
    private static final Map<Integer,Integer> widths=new HashMap<>();
    /** 退出世界时释放共享纹理；只允许渲染线程调用。 */
    public static void clear() {
        if(geometry!=0)RenderSystem.deleteTexture(geometry);
        if(cuts!=0)RenderSystem.deleteTexture(cuts);
        geometry=cuts=0;ids=List.of();uploaded.clear();widths.clear();
    }
    private GhostDomainConflictTextures() {}
    /** 每帧准备一次，所有后处理效果共用；保存纹理状态避免污染颜色/深度采样器。 */
    public static void prepare() {
        var result=ClientGhostDomainManager.conflict();
        var nodes=result.nodes();ids=nodes.stream().map(n->n.id()).toList();
        int count=nodes.size();if(count==0)return;
        var domains=new HashMap<UUID,Double>();
        ClientGhostDomainManager.getDomains().forEach(d->domains.put(d.getId(),d.getRenderRadius()));
        int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        int previous=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        var data=MemoryUtil.memAllocFloat(count*8);var mask=MemoryUtil.memAllocFloat(count*count*4);
        try {
            // 使用紧凑序号而非 UUID 哈希，避免浮点精度和哈希碰撞误判同源。
            Map<UUID,Integer> sources=new HashMap<>();
            for(var node:nodes){
                data.put((float)node.center().x).put((float)node.center().y).put((float)node.center().z).put(domains.get(node.id()).floatValue());
                data.put((float)node.strength()).put(node.grade()).put(node.shape()).put(sources.computeIfAbsent(node.source(),ignored->sources.size()));
            }
            for(var owner:nodes)for(var other:nodes)mask.put(result.cuts(owner.id(),other.id())?1:0).put(0).put(0).put(0);
            data.flip();mask.flip();geometry=upload(geometry,2,count,data);cuts=upload(cuts,count,count,mask);
        } finally {
            MemoryUtil.memFree(data);MemoryUtil.memFree(mask);
            RenderSystem.bindTexture(previous);RenderSystem.activeTexture(active);
        }
    }
    /** 浮点纹理避免世界坐标与范围被八位颜色量化；最近邻读取离散几何数据。 */
    private static int upload(int texture,int width,int height,java.nio.FloatBuffer data){
        if(texture==0)texture=GL11.glGenTextures();
        float[] values=new float[data.remaining()];data.duplicate().get(values);
        float[] previous=uploaded.get(texture);
        if(Arrays.equals(previous,values)&&widths.getOrDefault(texture,0)==width)return texture;
        RenderSystem.bindTexture(texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
        // 尺寸不变时复用显存；静止域的几何及扣除矩阵完全不重复上传。
        if(previous!=null&&previous.length==values.length&&widths.getOrDefault(texture,0)==width)
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D,0,0,0,width,height,GL11.GL_RGBA,GL11.GL_FLOAT,data);
        else GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA32F,width,height,0,GL11.GL_RGBA,GL11.GL_FLOAT,data);
        uploaded.put(texture,values);widths.put(texture,width);return texture;
    }
    /** 标准着色器使用共享契约，专属前缀仍由效果自身负责。 */
    public static void bind(ShaderInstance shader,UUID domain){
        shader.setSampler("DomainConflictGeometry",geometry);shader.setSampler("DomainConflictCuts",cuts);
        var count=shader.getUniform("DomainConflictCount");if(count!=null)count.set(ids.size());
        var index=shader.getUniform("DomainConflictIndex");if(index!=null)index.set(ids.indexOf(domain));
    }
}
