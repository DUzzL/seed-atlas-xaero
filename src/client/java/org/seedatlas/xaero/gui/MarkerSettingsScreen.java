package org.seedatlas.xaero.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.seedatlas.xaero.config.SeedAtlasClientState;
import org.seedatlas.xaero.integration.SeedAtlasXaeroIntegration;

/** Scrollable, compact marker selector using Seed Atlas' original icons. */
public final class MarkerSettingsScreen extends Screen {
private static final Component TITLE = Component.translatable("screen.seedatlas_xaero.markers");
private static final int HEADER_HEIGHT = 68;
private static final int FOOTER_HEIGHT = 38;
private static final int MAX_LIST_WIDTH = 420;
private static final int MIN_LIST_WIDTH = 180;
private final Screen parent;

public MarkerSettingsScreen(final Screen parent) {
super(TITLE);
this.parent = parent;
}

@Override
protected void init() {
SeedAtlasXaeroIntegration.setHeavyWorkPaused(true);
int horizontalMargin = Math.min(16, Math.max(6, this.width / 18));
int listWidth = Math.min(MAX_LIST_WIDTH, Math.max(MIN_LIST_WIDTH, this.width - horizontalMargin * 2));
listWidth = Math.min(listWidth, this.width - 8);
int listLeft = (this.width - listWidth) / 2;
int listTop = HEADER_HEIGHT;
int listBottom = Math.max(listTop + 48, this.height - FOOTER_HEIGHT);
listBottom = Math.min(listBottom, this.height - 26);
int listHeight = Math.max(48, listBottom - listTop);

this.addRenderableWidget(Button.builder(completedFilterMessage(), button -> {
SeedAtlasClientState.setHideCompletedStructures(!SeedAtlasClientState.config().hideCompletedStructures());
button.setMessage(completedFilterMessage());
}).bounds(listLeft, 40, listWidth, 20).build());

this.addRenderableWidget(new MarkerToggleList(this.minecraft, listLeft, listTop, listWidth, listHeight));

int doneWidth = Math.min(200, Math.max(120, this.width - 24));
this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
.bounds((this.width - doneWidth) / 2, Math.max(0, this.height - 28), doneWidth, 20)
.build());
}

@Override
public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
super.extractRenderState(graphics, mouseX, mouseY, partialTick);
graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
Component status = Component.translatable(
SeedAtlasClientState.config().structuresEnabled()
? "screen.seedatlas_xaero.markers.enabled"
: "screen.seedatlas_xaero.markers.disabled"
);
graphics.centeredText(this.font, status, this.width / 2, 23, 0xFFB8B8B8);
}

private static Component completedFilterMessage() {
return Component.translatable("options.seedatlas_xaero.hide_completed",
SeedAtlasClientState.config().hideCompletedStructures() ? "ON" : "OFF");
}

@Override
public void onClose() {
SeedAtlasClientState.save();
// Parent is the main Seed Atlas settings screen; keep heavy work paused.
this.minecraft.gui.setScreen(this.parent);
}
}