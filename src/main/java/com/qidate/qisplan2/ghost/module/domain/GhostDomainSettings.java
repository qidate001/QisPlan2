package com.qidate.qisplan2.ghost.module.domain;

import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

/** 鬼域通用配置的唯一序列化边界；只读写自己的命名空间，不覆盖模块其他数据。 */
public record GhostDomainSettings(Access enter, Access leave, List<Rule> rules) {
    public static final String KEY="qisplan2:domain_settings";
    public static final int MAX_RULES=64, MAX_TEXT=512;
    public static final GhostDomainSettings DEFAULT=new GhostDomainSettings(Access.OPEN,Access.CLOSED,List.of());
    /** 分类顺序为凡人、厉鬼、驭鬼者、普通实体，存档使用稳定标识，不保存翻译文本。 */
    public static final List<String> GROUPS=List.of("mortal","ghost","possessor","ordinary");
    /** 不可变规则快照使运行时可以安全缓存编译后的选择器。 */
    public GhostDomainSettings { rules=List.copyOf(rules); if(rules.size()>MAX_RULES)throw new IllegalArgumentException("Too many domain rules"); }
    /** 名称和实体资源 ID 以半角逗号分隔；黑名单优先，白名单其次，分类兜底。 */
    public record Access(int groups,String whitelist,String blacklist) {
        public static final Access OPEN=new Access(15,"","");
        public static final Access CLOSED=new Access(0,"","");
        public Access { if((groups & ~15)!=0)throw new IllegalArgumentException("Invalid groups"); whitelist=bounded(whitelist);blacklist=bounded(blacklist); }
        /** 对预先分类的事实判断权限，禁止此数据对象读取宿主。 */
        public boolean allows(int group,String name,String type) {
            if(matches(blacklist,group,name,type,false))return false;
            return matches(whitelist,group,name,type,false) || (groups & (1<<group))!=0;
        }
    }
    public record Rule(ResourceLocation ability,String targets,boolean enabled) {
        public Rule { targets=bounded(targets); }
    }
    /** 拒绝过长输入，不静默截断玩家名或实体标识。 */
    private static String bounded(String value) { if(value.length()>MAX_TEXT)throw new IllegalArgumentException("Domain selector too long");return value.trim(); }
    /** 同时支持稳定分类标识、中文分类别名、玩家名及实体 ID，采用整项匹配。 */
    public static boolean matches(String expression,int group,String name,String type,boolean categories) {
        for(String raw:expression.split(",")) {
            String token=raw.trim().toLowerCase(Locale.ROOT);
            if(token.isEmpty())continue;
            if(token.equals(name.toLowerCase(Locale.ROOT)) || token.equals(type.toLowerCase(Locale.ROOT)))return true;
            if(categories && (token.equals("#"+GROUPS.get(group)) || token.equals(GROUPS.get(group))
                    || token.equals(List.of("凡人","厉鬼","驭鬼者","普通实体").get(group))))return true;
        }
        return false;
    }
    /** 页面、网络与运行时在未保存配置时使用同一个模块默认声明。 */
    public static GhostDomainSettings forModule(ResourceLocation module,CompoundTag custom) {
        var definition=GhostModuleRegistry.get(module);
        return read(custom,definition==null?DEFAULT:definition.domainDefinition().map(GhostDomainDefinition::defaultSettings).orElse(DEFAULT));
    }
    /** 从模块完整 customData 中读取；未配置时采用声明默认值，已保存的设置保持不变。 */
    public static GhostDomainSettings read(CompoundTag custom) {
        return read(custom,DEFAULT);
    }
    /** 带模块默认值的解码入口，服务端和界面统一使用。 */
    public static GhostDomainSettings read(CompoundTag custom,GhostDomainSettings defaults) {
        if(!custom.contains(KEY,Tag.TAG_COMPOUND))return defaults;
        try {
            CompoundTag tag=custom.getCompound(KEY);List<Rule> rules=new ArrayList<>();
            ListTag list=tag.getList("rules",Tag.TAG_COMPOUND);
            for(int i=0;i<Math.min(list.size(),MAX_RULES);i++) {
                CompoundTag row=list.getCompound(i);ResourceLocation id=ResourceLocation.tryParse(row.getString("ability"));
                if(id!=null)rules.add(new Rule(id,row.getString("targets"),row.getBoolean("enabled")));
            }
            return new GhostDomainSettings(readAccess(tag.getCompound("enter")),readAccess(tag.getCompound("leave")),rules);
        } catch(IllegalArgumentException e) { return defaults; }
    }
    /** 解码一侧权限。 */
    private static Access readAccess(CompoundTag tag) { return new Access(tag.getInt("groups"),tag.getString("white"),tag.getString("black")); }
    /** 返回带配置的副本，保留其他能力、鬼域状态与宿主数据。 */
    public CompoundTag write(CompoundTag original) {
        CompoundTag result=original.copy(),tag=new CompoundTag();
        tag.put("enter",writeAccess(enter));tag.put("leave",writeAccess(leave));ListTag list=new ListTag();
        for(Rule rule:rules){CompoundTag row=new CompoundTag();row.putString("ability",rule.ability().toString());row.putString("targets",rule.targets());row.putBoolean("enabled",rule.enabled());list.add(row);}
        tag.put("rules",list);result.put(KEY,tag);return result;
    }
    /** 编码一侧权限。 */
    private static CompoundTag writeAccess(Access access){CompoundTag tag=new CompoundTag();tag.putInt("groups",access.groups());tag.putString("white",access.whitelist());tag.putString("black",access.blacklist());return tag;}
}
