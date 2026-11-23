package com.qidate.qisplan2.network;

import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.ability.*;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainSettings;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.network.payload.GhostDomainPanelPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/** 鬼域面板服务器入口；配置修改与能力释放严格分离，开关和层数不能靠修改 NBT 绕过媒介。 */
public final class GhostDomainPanelNetwork {
    private static final Map<ServerPlayer,Long> LAST=new WeakHashMap<>();
    private GhostDomainPanelNetwork(){}
    /** 在主线程处理，拒绝其他玩家、无承载模块、伪造控件和超出目录的自动化功能。 */
    public static void register(RegisterPayloadHandlersEvent event){
        event.registrar("1").playToServer(GhostDomainPanelPayload.TYPE,GhostDomainPanelPayload.STREAM_CODEC,(packet,context)->context.enqueueWork(()->{
            if(!(context.player() instanceof ServerPlayer player)||!player.isAlive()||player.isSpectator())return;
            long now=player.serverLevel().getGameTime();Long last=LAST.put(player,now);if(last!=null&&last==now)return;
            boolean success=false;try{success=apply(player,packet);}catch(IllegalArgumentException ignored){}
            if(!success)player.displayClientMessage(Component.translatable("hud.qisplan2.ability_wheel.unavailable"),true);
        }));
    }
    /** 应用单个白名单操作；规则索引在当前服务器列表中校验。 */
    private static boolean apply(ServerPlayer player,GhostDomainPanelPayload p){
        var host=new PlayerGhostModuleHost(player);var module=GhostModuleRegistry.get(p.module());
        if(module==null||!host.carriesModule(p.module())||module.domainDefinition().isEmpty())return false;
        var panel=module.domainPanel();ResourceLocation ability=null;Integer number=null;
        if(p.action().equals("control")){
            if(p.value()<0||p.value()>=panel.controls().size())return false;
            var control=panel.controls().get(p.value());ability=control.ability();number=control.integer();
        }else if(p.action().equals("common")){ability=panel.common().get(p.key());}
        else if(p.action().equals("ability")){
            var id=ResourceLocation.tryParse(p.key());if(id!=null&&panel.buttons().stream().limit(5).anyMatch(id::equals))ability=id;
        }else {
            var custom=host.getModuleCustomData(p.module());var settings=GhostDomainSettings.read(custom);
            var enter=settings.enter();var leave=settings.leave();var rules=new ArrayList<>(settings.rules());
            switch(p.action()){
                case "enter" -> enter=new GhostDomainSettings.Access(p.value(),p.text(),p.extra());
                case "leave" -> leave=new GhostDomainSettings.Access(p.value(),p.text(),p.extra());
                case "add" -> {
                    var id=ResourceLocation.tryParse(p.key());
                    if(id==null||rules.size()>=GhostDomainSettings.MAX_RULES||p.text().isBlank()||panel.automations().stream().noneMatch(a->a.ability().equals(id)))return false;
                    rules.add(new GhostDomainSettings.Rule(id,p.text(),true));
                }
                case "toggle", "delete" -> {
                    if(p.value()<0||p.value()>=rules.size())return false;
                    var rule=rules.get(p.value());if(p.action().equals("delete"))rules.remove(p.value());
                    else rules.set(p.value(),new GhostDomainSettings.Rule(rule.ability(),rule.targets(),!rule.enabled()));
                }
                default -> {return false;}
            }
            return host.setModuleCustomData(p.module(),new GhostDomainSettings(enter,leave,rules).write(custom));
        }
        if(ability==null)return false;
        final ResourceLocation selected=ability;
        var definition=module.getAbilities().stream().filter(a->a.getId().equals(selected)&&a.isPublicSkill()).findFirst().orElse(null);
        if(definition==null)return false;
        var input=definition.integerInput();if(input!=null?(number==null||!input.accepts(number)):number!=null)return false;
        return GhostAbilityExecutor.tryExecuteWithResult(new GhostAbilityContext(host,p.module(),ability,GhostAbilityContext.Source.POSSESSION,null,()->{},new GhostInvocationParameters(player,number)));
    }
}
