package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.ScrollableTooltip;
import com.github.noamm9.features.impl.visual.RevertAxes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

@Mixin(value = GuiGraphics.class)
public abstract class MixinGuiGraphics {
    @Shadow public abstract Matrix3x2fStack pose();
    @Unique private boolean noammaddons_pushed = false;

    @WrapOperation(
        method = "renderTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"
        )
    )
    private Vector2ic noammaddons_onPositionTooltip(ClientTooltipPositioner instance, int width, int height, int x, int y, int tooltipW, int tooltipH, Operation<Vector2ic> original) {
        if (!ScrollableTooltip.INSTANCE.enabled) {
            return original.call(instance, width, height, x, y, tooltipW, tooltipH);
        }

        float scaleFactor = ScrollableTooltip.INSTANCE.getScale().getValue().floatValue() / 100f + ScrollableTooltip.scaleOverride / 10f;
        if (scaleFactor <= 0f) scaleFactor = 1f;

        pose().pushMatrix();
        noammaddons_pushed = true;

        if (scaleFactor != 1f) pose().scale(scaleFactor, scaleFactor);

        if (ScrollableTooltip.scrollAmountX != 0f || ScrollableTooltip.scrollAmountY != 0f) {
            pose().translate(ScrollableTooltip.scrollAmountX, ScrollableTooltip.scrollAmountY);
        }

        return original.call(instance, width, height, (int) (x / scaleFactor), (int) (y / scaleFactor), tooltipW, tooltipH);
    }

    @Inject(method = "renderTooltip", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;popMatrix()Lorg/joml/Matrix3x2fStack;", remap = false))
    private void noammaddons_onPostRenderTooltip(Font font, List<ClientTooltipComponent> list, int i, int j, ClientTooltipPositioner positioner, ResourceLocation background, CallbackInfo ci) {
        if (noammaddons_pushed) {
            pose().popMatrix();
            noammaddons_pushed = false;
        }
    }

    @ModifyVariable(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"), argsOnly = true)
    private ItemStack revertAxe(ItemStack original) {
        if (original == null || original.isEmpty()) return original;
        ItemStack replacement = RevertAxes.shouldReplace(original);
        return Objects.requireNonNullElse(replacement, original);
    }
}