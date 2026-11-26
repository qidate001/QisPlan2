package com.qidate.qisplan2.ghost.curse;

import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** 模块所属诅咒声明的查询视图。没有独立注册入口，旧存档仍按原类型 ID 读取。 */
public final class CurseRegistry {
    private CurseRegistry() {}
    private static int moduleCount=-1;
    private static Map<ResourceLocation,CurseType> definitions=Map.of();
    private static Map<ResourceLocation,ResourceLocation> owners=Map.of();
    /** 注册表只增不改，模块数变化时重建一次派生索引，运行时查询不遍历全部模块。 */
    private static void refresh(){
        var modules=GhostModuleRegistry.getAll();if(moduleCount==modules.size())return;
        var next=new LinkedHashMap<ResourceLocation,CurseType>();var origin=new LinkedHashMap<ResourceLocation,ResourceLocation>();
        for(var module:modules)for(var curse:module.curses()){next.put(curse.id(),curse);origin.put(curse.id(),module.getId());}
        definitions=Map.copyOf(next);owners=Map.copyOf(origin);moduleCount=modules.size();
    }
    /** 模块注册时验证跨模块重复 ID，拒绝歧义存档。 */
    public static void validate(GhostModule module) {
        var ids=new HashSet<ResourceLocation>();
        for(var curse:module.curses())if(!ids.add(curse.id())||get(curse.id())!=null)throw new IllegalStateException("Duplicate module curse: "+curse.id());
    }
    /** 仅从完整模块的声明查找类型。 */
    public static CurseType get(ResourceLocation id) {
        refresh();return definitions.get(id);
    }
    /** 查询所属模块，不要求原来的物理载体仍在场。 */
    public static ResourceLocation owner(ResourceLocation id) {
        refresh();return owners.get(id);
    }
    /** 命令补全来自模块声明。 */
    public static Set<ResourceLocation> ids() {refresh();return definitions.keySet();}
    /** 是否存在模块声明。 */
    public static boolean contains(ResourceLocation id){return get(id)!=null;}
}
