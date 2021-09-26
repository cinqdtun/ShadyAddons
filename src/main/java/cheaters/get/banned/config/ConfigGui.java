package cheaters.get.banned.config;

import cheaters.get.banned.Shady;
import cheaters.get.banned.config.components.ConfigButton;
import cheaters.get.banned.config.components.NumberInput;
import cheaters.get.banned.config.components.Scrollbar;
import cheaters.get.banned.config.components.SwitchButton;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;

public class ConfigGui extends GuiScreen {

    private int columnWidth = 300;
    public static ArrayList<Setting> settings = filterSettings();

    private int prevMouseY;
    private int scrollOffset = 0;
    private boolean scrolling = false;
    private ResourceLocation logo;
    private Scrollbar scrollbar;

    private Integer prevWidth = null;
    private Integer prevHeight = null;

    public ConfigGui() {
        this(new ResourceLocation("shadyaddons:logo.png"));
    }

    public ConfigGui(ResourceLocation logo) {
        this.logo = logo;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Dark Background
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        mouseMoved(mouseY);

        // Logo + Version
        GlStateManager.color(255, 255, 255);
        Shady.mc.getTextureManager().bindTexture(logo);
        drawModalRectWithCustomSizedTexture(width / 2 - 143, 24-scrollOffset, 0, 0, 286, 40, 286, 40);
        drawCenteredString(Shady.mc.fontRendererObj, (Shady.PRIVATE ? "Insider ✦ " : "") + Shady.VERSION, width / 2, 67-scrollOffset, -1);

        // Settings
        for(int i = 0; i < settings.size(); i++) {
            Setting setting = settings.get(i);

            int x = getOffset();
            int y = (columnWidth / 3) + (i * 15) - scrollOffset;

            // Nested Setting
            if(setting.parent != null) {
                x += 10;
                Setting parentSetting = ConfigLogic.getSetting(setting.parent);
                if(parentSetting != null && parentSetting.parent != null) {
                    x += 10;
                }
            } else if(i > 0) {
                // Setting Border
                drawRect(x, y-3, getOffset() + columnWidth, y-2, ConfigButton.transparent.getRGB());
            }

            // Setting Text
            Shady.mc.fontRendererObj.drawString((setting.enabled() ? "§a" : "§f") + setting.name, x, y+1, -1);
        }

        if(prevHeight != null && prevWidth != null && (prevWidth != width || prevHeight != height)) {
            Shady.mc.displayGuiScreen(new ConfigGui(logo));
        }

        prevWidth = width;
        prevHeight = height;
    }

    @Override
    public void initGui() {
        buttonList.clear();

        for(int i = 0; i < settings.size(); i++) {
            Setting setting = settings.get(i);

            int x = getOffset() + columnWidth;
            int y = (columnWidth / 3) + (i * 15) - scrollOffset;

            if(setting.type == Setting.SettingType.BOOLEAN) {
                if(setting.booleanType == Setting.BooleanType.SWITCH) buttonList.add(new SwitchButton(setting, x, y));
                if(setting.booleanType == Setting.BooleanType.CHECKBOX) buttonList.add(new SwitchButton(setting, x, y)); // TODO: Implement components.CheckboxButton
            } else if(setting.type == Setting.SettingType.INTEGER) {
                buttonList.add(new NumberInput(setting, x, y));
            }
        }

        int viewport = height - 100 - 10;
        int contentHeight = settings.size() * 15;
        int scrollbarX = getOffset() + columnWidth + 10;

        scrollbar = new Scrollbar(viewport, contentHeight, scrollOffset, scrollbarX, scrolling);
        buttonList.add(scrollbar);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if(button instanceof Scrollbar) {
            scrolling = true;
        } else {
            settings.clear();
            settings = filterSettings();
        }
        initGui();
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        scrolling = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    private void mouseMoved(int mouseY) {
        if(scrolling) {
            int viewport = height - 100 - 10;
            int contentHeight = settings.size() * 15;

            /*
            increase by a percentage of the scrollbar height depending on the percentage of the viewport that was scrolled
            */

            // 1) percentage of viewport scrolled
            // 2) answer * height of scrollbar
            // 3) scrollOffset += answer + amount scrolled

            int dragAmount = mouseY-prevMouseY;
            int scrollbarCompensation = dragAmount/viewport*scrollbar.height;

            scrollOffset += dragAmount + scrollbarCompensation;
            scrollOffset = MathHelper.clamp_int(scrollOffset, 0, contentHeight-viewport);
            initGui();
        }
        prevMouseY = mouseY;
    }

    private static ArrayList<Setting> filterSettings() {
        ArrayList<Setting> newSettings = new ArrayList<>();

        for(Setting setting : Shady.settings) {
            if(setting.parent == null) {
                newSettings.add(setting);
                continue;
            }

            for(Setting subSetting : Shady.settings) {
                if(subSetting.name.equals(setting.parent) && subSetting.enabled()) {
                    newSettings.add(setting);
                }
            }
        }

        return newSettings;
    }

    @Override
    public void onGuiClosed() {
        ConfigLogic.save();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private int getOffset() {
        return (width - columnWidth) / 2;
    }

}
