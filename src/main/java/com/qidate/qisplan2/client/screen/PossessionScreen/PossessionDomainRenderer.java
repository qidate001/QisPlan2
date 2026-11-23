package com.qidate.qisplan2.client.screen.PossessionScreen;

import com.qidate.qisplan2.ghost.module.GhostModule;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainPanel;
import com.qidate.qisplan2.ghost.module.domain.GhostDomainSettings;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.network.payload.GhostDomainPanelPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;

/** 鬼域总页：渲染模块声明的控件和通用配置，不识别鬼眼、鬼雾或宿主实现之外的具体厉鬼。 */
public final class PossessionDomainRenderer {
    private final Minecraft mc = Minecraft.getInstance();
    private final List<Hit> hits = new ArrayList<>();
    private final List<EditBox> fields = new ArrayList<>();
    private ResourceLocation selected;
    private List<GhostModule> modules = List.of();
    private GhostDomainPanel panel = GhostDomainPanel.EMPTY;
    private GhostDomainSettings settings = GhostDomainSettings.DEFAULT;
    private int tab, moduleScroll, ruleScroll, selectedRule = -1, selectedFunction, dropdown = -1, dropScroll;
    private int enterGroups, leaveGroups;
    private EditBox enterWhite, enterBlack, leaveWhite, leaveBlack,targets;
    private String tooltip;
    private int scrollDrag = -1;

    private record Hit(int x, int y, int w, int h, Runnable action, String tip) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }
    /** 翻译统一归入鬼域页语言命名空间。 */
    private Component text(String key) { return Component.translatable("screen.qisplan2.domain." + key); }

    /** 字段由本页接管坐标与焦点，避免外层整体缩放导致原生控件命中偏移。 */
    private EditBox field(int x, int y, int width, String hint){
        EditBox box = new EditBox(mc.font, x, y, width, 15, text(hint));
        box.setMaxLength(GhostDomainSettings.MAX_TEXT);box.setHint(text(hint));fields.add(box);return box;
    }

    /** 选择鬼域时读取服务器同步的模块配置；切换时丢弃尚未点击保存的输入。 */
    private void select(ResourceLocation id){
        selected = id; selectedRule = -1; ruleScroll = 0; selectedFunction = 0; dropdown = -1; fields.clear();
        var host = new PlayerGhostModuleHost(mc.player);
        var module = GhostModuleRegistry.get(id);
        panel = module.domainPanel();

        settings = GhostDomainSettings.read(host.getModuleCustomData(id)); enterGroups=settings.enter().groups();leaveGroups=settings.leave().groups();
        enterWhite = field(124,119,133,"whitelist"); enterBlack=field(124,137,133,"blacklist");
        leaveWhite = field(124,241,133,"whitelist"); leaveBlack=field(124,259,133,"blacklist");
        targets = field(273,260,207,"targets_hint");
        enterWhite.setValue(settings.enter().whitelist()); enterBlack.setValue(settings.enter().blacklist());
        leaveWhite.setValue(settings.leave().whitelist()); leaveBlack.setValue(settings.leave().blacklist());
    }

    /** 只列出玩家真正承载的、有鬼域声明的模块；移除模块后立即切换有效项。 */
    private GhostModule refresh(){
        if (mc.player==null) return null;
        var host = new PlayerGhostModuleHost(mc.player);

        modules = host.getModules().stream().filter(e -> host.carriesModule(e.id())).map(e -> GhostModuleRegistry.get(e.id()))
                .filter(m -> m != null && m.domainDefinition().isPresent()).toList();
        if (modules.isEmpty()) { selected = null; fields.clear(); return null; }
        if (selected == null || modules.stream().noneMatch(m -> m.getId().equals(selected))) select(modules.getFirst().getId());
        settings=GhostDomainSettings.read(host.getModuleCustomData(selected));
        selectedRule = Math.min(selectedRule,settings.rules().size()-1);
        moduleScroll = Math.clamp(moduleScroll, 0, Math.max(0, modules.size() - 5));
        ruleScroll = Math.clamp(ruleScroll, 0, Math.max(0, settings.rules().size() - 5));
        return GhostModuleRegistry.get(selected);
    }

    /** 所有操作都带当前模块身份；客户端不能直接改写能力状态。 */
    private void send(String action, String key, int value, String valueText, String extra){
        if (selected != null) PacketDistributor.sendToServer(new GhostDomainPanelPayload(selected, action, key, value, valueText, extra));
    }

    /** 绘制按钮并登记局部坐标命中框。 */
    private void button(GuiGraphics g, int x, int y, int w, int h, Component label, boolean active, Runnable action){
        g.fill(x, y, x + w, y + h, active ? 0xff385c64 : 0xff242d38);
        g.drawCenteredString(mc.font, mc.font.substrByWidth(label, w - 6).getString(), x + w / 2, y + (h - 8) / 2, 0xffe3ecee);
        hits.add(new Hit(x, y, w, h, action, label.getString()));
    }

    /** 绘制可滚动列表的位置提示，滚动范围由真实条目数量限制。 */
    private void scrollbar(GuiGraphics g, int x, int y, int height, int offset, int count, int visible){
        if (count <= visible) return;
        g.fill(x, y, x + 3, y + height, 0xff28313b);

        int thumb = Math.max(12, height * visible / count);
        int top = y + (height - thumb) * offset / (count - visible);
        g.fill(x, top, x + 3, top + thumb, 0xff6c9a9c);
    }

    /** 绘制模块列表、两级页签，再按声明绘制控制区或通用配置。 */
    public void render(GuiGraphics g, double mx, double my, float partial){
        hits.clear();tooltip=null;var module=refresh();
        g.fill(489, 30, 594, 310, 0xff18212a);
        g.drawCenteredString(mc.font,text("available"), 541, 37, 0xffa7d4d5);
        for(int i=moduleScroll;i<Math.min(modules.size(),moduleScroll + 5); i++){
            var m=modules.get(i);int y=57+(i-moduleScroll)*48;
            button(g,494,y,94,43,Component.empty(),m.getId().equals(selected),()->select(m.getId()));
            ResourceLocation icon=m.iconTexture();if(icon==null)icon=ResourceLocation.parse("minecraft:textures/item/nether_star.png");
            g.blit(icon,499,y+6,0,0,20,20,20,20);
            g.drawString(mc.font,mc.font.substrByWidth(m.displayName(),64).getString(),521,y+15,0xffe5ebee);
        }
        scrollbar(g,590,57,240,moduleScroll,modules.size(),5);
        button(g,10,32,233,20,text("control"),tab==0,()->{tab=0;cancelFocus();});
        button(g,247,32,235,20,text("configuration"),tab==1,()->{tab=1;cancelFocus();});
        if(module==null){g.drawCenteredString(mc.font,text("empty"),245,150,0xffaaaaaa);return;}
        if(tab==0)renderControls(g);else renderConfiguration(g,module);
        if(tab==1)for(EditBox box:fields)box.render(g,(int)mx,(int)my,partial);
        renderDropdown(g,mx,my);
        if(dropdown<0)for(Hit hit:hits)if(hit.contains(mx,my))tooltip=hit.tip;
        if(tooltip!=null&&!tooltip.isBlank())g.drawString(mc.font,mc.font.plainSubstrByWidth(tooltip,565),12,310,0xffacbec7);
    }

    /** 通用眼形只是可选控件样式，位置和开闭逻辑完全来自模块声明。 */
    private void renderControls(GuiGraphics g){
        var context=new GhostModuleContext(new PlayerGhostModuleHost(mc.player),selected);
        if(panel.controls().isEmpty()){g.drawCenteredString(mc.font,text("no_controls"),245,160,0xff91a3af);return;}
        for(int i=0;i<panel.controls().size();i++){
            final int index=i;var control=panel.controls().get(i);int x=20+(int)(control.x()*450),y=63+(int)(control.y()*225);
            boolean active=control.active().test(context);Component label=Component.translatable(control.label());
            if(control.style()==GhostDomainPanel.Style.EYE){
                for(int dx=-22;dx<=22;dx++){
                    int h=(int)(9*Math.sin(Math.PI*(dx+22)/44));int color=active?0xffdfb4ac:0xff657580;
                    g.fill(x+dx,y-h,x+dx+1,y-h+2,color);
                    g.fill(x+dx,active?y+h-1:y-h,x+dx+1,active?y+h+1:y-h+2,color);
                }
                if(active){g.fill(x-4,y-6,x+5,y+7,0xff9f2835);g.fill(x-1,y-5,x+2,y+6,0xff130c11);g.fill(x+1,y-4,x+3,y-2,0xffffd2bc);}
                hits.add(new Hit(x-26,y-18,52,36,()->send("control","",index,"",""),label.getString()));
            }else button(g,x-35,y-10,70,20,label,active,()->send("control","",index,"",""));
        }
    }

    /** 常规能力、进出策略和自动化各自拥有明确的操作区域。 */
    private void renderConfiguration(GuiGraphics g,GhostModule module){
        g.fill(10,57,114,306,0xff18212a);g.fill(118,57,263,306,0xff18212a);g.fill(268,57,483,306,0xff18212a);
        g.drawCenteredString(mc.font,text("abilities"),62,62,0xffa7d4d5);
        String[] common={"toggle","domain_up","domain_down","self_up","self_down"};
        for(int i=0;i<common.length;i++){
            String key=common[i];if(!panel.common().containsKey(key))continue;
            int x=i==0?15:15+((i-1)%2)*48,y=i==0?77:101+((i-1)/2)*23,w=i==0?94:45;
            button(g,x,y,w,20,text(key),false,()->send("common",key,0,"",""));
        }
        int by=153;for(var id:panel.buttons().stream().limit(5).toList()){
            var ability=module.getAbilities().stream().filter(a->a.getId().equals(id)&&a.isPublicSkill()).findFirst().orElse(null);
            if(ability==null)continue;
            button(g,15,by,94,25,Component.translatable(ability.translationKey()),false,()->send("ability",id.toString(),0,"",""));by+=29;
        }
        access(g,true,62);access(g,false,184);
        g.drawCenteredString(mc.font,text("automation"),375,62,0xffa7d4d5);
        for(int i=ruleScroll;i<Math.min(settings.rules().size(),ruleScroll+5);i++){
            int index=i,y=79+(i-ruleScroll)*22;var rule=settings.rules().get(i);
            String label=panel.automations().stream().filter(a->a.ability().equals(rule.ability())).map(a->Component.translatable(a.label()).getString()).findFirst().orElse(rule.ability().toString());
            button(g,273,y,201,20,Component.literal((rule.enabled()?"● ":"○ ")+label+" → "+rule.targets()),selectedRule==i,()->selectedRule=index);
        }
        scrollbar(g,477,79,110,ruleScroll,settings.rules().size(),5);
        if(settings.rules().isEmpty())g.drawCenteredString(mc.font,text("no_rules"),375,115,0xff7e919e);
        g.drawString(mc.font,text("function"),273,195,0xffa7bfc7);
        Component function=panel.automations().isEmpty()?text("no_functions"):Component.translatable(panel.automations().get(selectedFunction).label());
        button(g,273,207,207,20,function,false,()->openDropdown(0));
        button(g,273,233,207,20,text("target_presets"),false,()->openDropdown(1));
        button(g,273,282,65,20,text("add"),false,()->{if(!panel.automations().isEmpty())send("add",panel.automations().get(selectedFunction).ability().toString(),0,targets.getValue(),"");});
        button(g,342,282,65,20,text("enable_disable"),false,()->send("toggle","",selectedRule,"",""));
        button(g,411,282,69,20,text("delete"),false,()->send("delete","",selectedRule,"",""));
    }

    /** 分类勾选与名单组成一侧完整策略，点击保存后提交服务端。 */
    private void access(GuiGraphics g,boolean entering,int top){
        g.drawCenteredString(mc.font,text(entering?"enter":"leave"),190,top,0xffa7d4d5);
        int groups=entering?enterGroups:leaveGroups;
        for(int i = 0; i < 4; i++){
            final int bit = 1 << i;
            boolean on = (groups&bit) != 0;
            button(g,124 + (i % 2) * 68,top + 15 + (i / 2) * 19,65,17,
                    Component.literal(on?"☑ ":"☐ ").append(text(GhostDomainSettings.GROUPS.get(i))), on,
                    () -> { if(entering) enterGroups ^= bit; else leaveGroups ^= bit; }
            );
        }
        button(g,124,top + 94,133,16,text("save"),false,
                () -> send(entering ? "enter" : "leave","", entering ? enterGroups : leaveGroups,
                (entering ? enterWhite : leaveWhite).getValue(), (entering ? enterBlack : leaveBlack).getValue())
        );
    }

    /** 下拉框是真正的可滚动目录；打开时关闭文本输入焦点。 */
    private void openDropdown(int type) {
        for(EditBox box : fields) box.setFocused(false);
        dropdown = dropdown == type ? -1 : type;
        dropScroll = 0;
    }

    /**
     * 弹出目录使用独立的较高深度层，并先提交底层文字批次。
     * 仅调整 Java 调用顺序无法保证遮住字体批次；背景和目录文字必须一起抬高，
     * 绘制完成后恢复矩阵，避免后续控件继承弹出层深度。
     */
    private void renderDropdown(GuiGraphics g,double mx,double my){
        if(dropdown<0)return;int count=dropdown==0?panel.automations().size():4;if(count==0){dropdown=-1;return;}
        int visible = Math.min(7, count), top = dropdown == 0 ? 207 - visible * 19 : 233 - visible * 19;
        g.flush();
        g.pose().pushPose();
        g.pose().translate(0,0,200);
        try {
            // 整个弹出区完全不透明，连选项间的缝隙也不能透出原有文字。
            g.fill(271, top - 2, 482, top + visible * 19 + 2, 0xff080e15);
            g.fill(272, top - 1, 481, top + visible * 19 + 1, 0xff080e15);
            for(int i = dropScroll; i < Math.min(count, dropScroll + visible); i++){
                int y = top + (i - dropScroll) * 19;
                Component label = dropdown == 0 ? Component.translatable(panel.automations().get(i).label()) : text(GhostDomainSettings.GROUPS.get(i));
                boolean hovered = mx >= 273 && mx < 480 && my >= y && my < y + 19;
                g.fill(273,y,480,y+18,hovered?0xff42616b:0xff25353e);
                g.drawString(mc.font,mc.font.substrByWidth(label,194).getString(),278,y + 5,0xffedf1f2);
            }
            g.flush();
        } finally {
            g.pose().popPose();
        }
    }
    /** 鼠标先交给弹出目录，再交给文本框与普通控件。 */
    public boolean mouseClicked(double x,double y,int button){
        if (button != 0) return false;
        if (dropdown < 0 && x >= 588 && x < 594 && y >= 57 && y <= 297 && modules.size() > 5) {
            scrollDrag = 0;
            return mouseDragged(x, y);
        }
        if (dropdown < 0 && tab == 1 && x >= 475 && x < 482 && y >= 79 && y <= 189 && settings.rules().size() > 5) {
            scrollDrag = 1;
            return mouseDragged(x, y);
        }
        if (dropdown >= 0) {
            int count = dropdown == 0 ? panel.automations().size() : 4;
            int visible = Math.min(7, count), top = (dropdown == 0 ? 207 : 233) - visible * 19;
            int row = (int)((y - top) / 19) + dropScroll;

            if(x >= 273 && x < 480 && y >= top && y < top + visible * 19 && row < count){
                if (dropdown == 0) selectedFunction = row;
                else {
                    String token = "#" + GhostDomainSettings.GROUPS.get(row);
                    String value = targets.getValue();
                    targets.setValue(value.isBlank() ? token : value + "," + token);
                }
            }
            dropdown=-1;return true;
        }
        if(tab==1){boolean used=false;for(EditBox box:fields){boolean hit=box.mouseClicked(x,y,button);box.setFocused(hit);used|=hit;}if(used)return true;}
        for(Hit hit:hits)if(hit.contains(x,y)){hit.action.run();return true;}return false;
    }

    /** 滚轮只移动鼠标所在列表，避免模块选择与自动化列表互相抢输入。 */
    public boolean mouseScrolled(double x,double y,double delta){
        int step = delta > 0 ? -1 : 1;

        if (dropdown >= 0) {
            int count = dropdown == 0 ? panel.automations().size() : 4;
            dropScroll = Math.clamp(dropScroll + step, 0, Math.max(0, count-7));
            return true;
        }

        if (x >= 489) {
            moduleScroll = Math.clamp(moduleScroll + step, 0, Math.max(0, modules.size() - 5));
            return true;
        }

        if (tab == 1 && x >= 268 && x < 483 && y >= 79 && y < 190) {
            ruleScroll = Math.clamp(ruleScroll + step, 0, Math.max(0, settings.rules().size() - 5));
            return true;
        }

        return false;
    }

    /** 滚动条拖动只修改本地可视偏移，不改变规则顺序或服务器配置。 */
    public boolean mouseDragged(double x,double y){
        if(scrollDrag==0){moduleScroll=(int)Math.round(Math.clamp((y-57)/240,0,1)*Math.max(0,modules.size()-5));return true;}
        if(scrollDrag==1){ruleScroll=(int)Math.round(Math.clamp((y-79)/110,0,1)*Math.max(0,settings.rules().size()-5));return true;}
        return false;
    }

    /** 松开后停止拖动，切页也会调用同样的清理。 */
    public boolean mouseReleased() { boolean dragging = scrollDrag >= 0; scrollDrag = -1; return dragging; }

    /** 聚焦文本时必须优先消费 H 等按键，不能在填写玩家名时关闭面板。 */
    public boolean keyPressed(int key, int scan, int modifiers){
        if(tab!=1)return false;
        for(EditBox box:fields)if(box.isFocused()){
            if(key == 256) { box.setFocused(false); return true; }
            if(key == 258) {
                int index = fields.indexOf(box);
                box.setFocused(false);
                fields.get((index + ((modifiers & 1) != 0 ? fields.size() - 1 : 1)) % fields.size()).setFocused(true);
                return true;
            }
            box.keyPressed(key,scan,modifiers);
            return true;
        }
        return false;
    }

    /** 字符事件仅交给当前聚焦字段，支持中文输入法提交和剪贴板粘贴。 */
    public boolean charTyped(char c, int modifiers) {
        if (tab == 1)
            for (EditBox box : fields)
                if(box.isFocused())
                    return box.charTyped(c,modifiers);
        return false;
    }

    /** 切页、关闭时清除所有输入焦点和弹出目录。 */
    public void cancelFocus() {
        scrollDrag = -1;
        for (EditBox box : fields) box.setFocused(false);
        dropdown=-1;
    }
}
