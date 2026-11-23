package com.qidate.qisplan2.client.screen.PossessionScreen;

import com.qidate.qisplan2.client.key.ModKeyMappings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 驭鬼总面板，四个页面共享标题栏和面板坐标转换。 */
public class PossessionScreen extends Screen {
    public static final int ABILITIES_PAGE = 2;
    public static final int DOMAINS_PAGE = 3;
    private static final int PANEL_WIDTH = 600, PANEL_HEIGHT = 320;
    private float panelScale = 1;
    private int panelX, panelY, currentPage;
    private final PossessionStatusRenderer statusRenderer = new PossessionStatusRenderer();
    private final PossessionSuppressionRenderer suppressionRenderer = new PossessionSuppressionRenderer();
    private final PossessionDomainRenderer domainRenderer = new PossessionDomainRenderer();
    private final PossessionAbilityRenderer abilityRenderer = new PossessionAbilityRenderer();
    private static final String[] PAGES = {"status", "supernatural", "ability_page", "domain_page"};

    /** H 默认打开状态页。 */
    public PossessionScreen() { this(0); }

    /** 快捷键可直接指定技能配置页。 */
    public PossessionScreen(int page) {
        super(Component.translatable("screen.qisplan2.possession.title"));
        currentPage = Math.clamp(page, 0, 3);
    }

    /** 按窗口计算整体缩放，所有子页使用统一的六百乘三百二十坐标。 */
    @Override protected void init() {
        panelScale = Math.min((float)width / (PANEL_WIDTH + 2), (float)height / (PANEL_HEIGHT + 2));
        panelX = (width - Math.round(PANEL_WIDTH * panelScale)) / 2;
        panelY = (height - Math.round(PANEL_HEIGHT * panelScale)) / 2;
        abilityRenderer.cancelDrag();
    }

    /** 局部 X 坐标转换，命中区域与视觉缩放一致。 */
    private double localX(double x) { return (x - panelX) / panelScale; }

    /** 局部 Y 坐标转换。 */
    private double localY(double y) { return (y - panelY) / panelScale; }

    /** 绘制共享标题栏，再绘制当前页。 */
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g, mx, my, partial);
        g.pose().pushPose();
        g.pose().translate(panelX, panelY, 0);
        g.pose().scale(panelScale, panelScale, 1);
        g.fill(-1, -1, 601, 321, 0xff606060);
        g.fill(0, 0, 600, 320, 0xf0101016);
        g.fill(0, 0, 600, 26, 0xf0181820);
        g.drawString(font, title, 12, 9, 0xffffffff);
        for (int page = 0; page < PAGES.length; page++) {
            int x = 214 + page * 96;
            g.fill(x, 3, x + 92, 23, currentPage == page ? 0xff39525c : 0xff252732);
            g.drawCenteredString(font, Component.translatable("screen.qisplan2.possession." + PAGES[page]),
                    x + 46, 9, currentPage == page ? 0xffffffff : 0xffaaaaaa);
        }
        if (currentPage == 0) statusRenderer.render(g, panelX, panelY, panelScale);
        else if (currentPage == 1) suppressionRenderer.render(g);
        else if(currentPage == 3) domainRenderer.render(g, localX(mx), localY(my), partial);
        else abilityRenderer.render(g, panelX, panelY, panelScale, localX(mx), localY(my));
        g.pose().popPose();
    }

    /** 切页先释放未完成拖动，避免另一个页面接收旧拖拽。 */
    private void changePage(int page) {
        abilityRenderer.cancelDrag();
        statusRenderer.mouseReleased();
        suppressionRenderer.cancelDrag();
        domainRenderer.cancelFocus();
        currentPage = page;
    }

    /** 标题栏按钮优先命中，随后路由当前页交互。 */
    @Override public boolean mouseClicked(double x, double y, int button) {
        double lx = localX(x), ly = localY(y);
        if (button == 0 && ly >= 3 && ly < 23) for (int page = 0; page < PAGES.length; page++) {
            if (lx >= 214 + page * 96 && lx < 306 + page * 96) { changePage(page); return true; }
        }
        if (currentPage == 3 && domainRenderer.mouseClicked(lx, ly, button)) return true;
        if (currentPage == 2 && abilityRenderer.mouseClicked(lx, ly, button)) return true;
        if (button == 0 && currentPage == 0 && statusRenderer.mouseClicked(lx, ly)) return true;
        if (button == 0 && currentPage == 1 && suppressionRenderer.mouseClicked(lx, ly)) return true;
        return super.mouseClicked(x, y, button);
    }

    /** 滚轮按当前页的局部区域分发。 */
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (currentPage == 3 && domainRenderer.mouseScrolled(localX(x), localY(y), dy)) return true;
        if (currentPage == 0 && statusRenderer.mouseScrolled(localX(x), localY(y), dy)) return true;
        if (currentPage == 2 && abilityRenderer.mouseScrolled(localX(x), localY(y), dy)) return true;
        return super.mouseScrolled(x, y, dx, dy);
    }

    /** 技能和压制各自拥有拖动状态。 */
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 0) {
            if (currentPage == 3 && domainRenderer.mouseDragged(localX(x), localY(y))) return true;
            if (currentPage == 0 && statusRenderer.mouseDragged(localX(x), localY(y))) return true;
            if (currentPage == 1 && suppressionRenderer.mouseDragged(localX(x), localY(y))) return true;
            if (currentPage == 2 && abilityRenderer.mouseDragged(localX(x), localY(y))) return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    /** 松开只完成当前页面的交互，编辑页绝不释放技能。 */
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0) {
            if (currentPage == 3 && domainRenderer.mouseReleased()) return true;
            if (currentPage == 0 && statusRenderer.mouseReleased()) return true;
            if (currentPage == 1 && suppressionRenderer.mouseReleased(localX(x), localY(y))) return true;
            if (currentPage == 2 && abilityRenderer.mouseReleased(localX(x), localY(y))) return true;
        }
        return super.mouseReleased(x, y, button);
    }

    /** 面板不暂停世界。 */
    @Override public boolean isPauseScreen() { return false; }

    /** H 再次关闭；Alt 加轮盘键可在面板内部直接切到配置页。 */
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (currentPage == 3 && domainRenderer.keyPressed(key, scan, modifiers)) return true;
        if (ModKeyMappings.OPEN_POSSESSION_SCREEN.matches(key, scan)) { onClose(); return true; }
        if (hasAltDown() && ModKeyMappings.ABILITY_WHEEL.matches(key, scan)) { changePage(ABILITIES_PAGE); return true; }
        return super.keyPressed(key, scan, modifiers);
    }

    /** 字符输入由当前鬼域配置页接管，使用与绘制一致的本地字段。 */
    @Override public boolean charTyped(char character, int modifiers) {
        return currentPage == 3 && domainRenderer.charTyped(character, modifiers) || super.charTyped(character, modifiers);
    }

    /** 关闭或被替换时清理拖动；已完成的编辑已即时保存。 */
    @Override public void removed() { domainRenderer.cancelFocus(); abilityRenderer.cancelDrag(); suppressionRenderer.cancelDrag(); statusRenderer.mouseReleased(); super.removed(); }
}
