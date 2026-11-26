package com.qidate.qisplan2.client.curse;

import com.qidate.qisplan2.ghost.curse.CurseRegistry;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.LinkedHashMap;

/** 客户端展示只投影模块所属诅咒声明，不再独立注册一套类型。 */
public final class CurseClientRegistry {
    private CurseClientRegistry(){}
    public record CurseClientType(ResourceLocation icon,boolean canDetect){}
    /** 按模块提供的图标和可察觉属性生成展示。 */
    public static CurseClientType get(ResourceLocation id){var type=CurseRegistry.get(id);return type==null?null:new CurseClientType(type.icon(),type.detectable());}
    /** 判断是否有对应模块声明。 */
    public static boolean contains(ResourceLocation id){return CurseRegistry.contains(id);}
    /** 展示工具使用的只读定义视图。 */
    public static Map<ResourceLocation,CurseClientType> getAll(){var result=new LinkedHashMap<ResourceLocation,CurseClientType>();for(var id:CurseRegistry.ids())result.put(id,get(id));return Map.copyOf(result);}
}
