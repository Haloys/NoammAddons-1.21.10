package com.github.noamm9.features.impl.misc

import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.IAbstractContainerScreen
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.withDescription
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import org.lwjgl.glfw.GLFW
import kotlin.math.sign


object ScrollableTooltip: Feature("Allows you to scroll through long tooltips.") {
    val scale by SliderSetting("Tooltip Scale", 100, 30, 150, 0.1).withDescription("Scale of the tooltip")
    val scrollSpeed by SliderSetting("Scroll Speed", 9, 1, 30, 1).withDescription("How fast the tooltip scrolls")
    private val scaleSpeed by SliderSetting("Scale Speed", 3, 1, 10, 1).withDescription("How fast the tooltip scales")

    @JvmField
    var scrollAmountX = 0f

    @JvmField
    var scrollAmountY = 0f

    @JvmField
    var scaleOverride = 0f

    private var lastSlotIndex = -1

    @JvmStatic
    var slot = 0
        set(value) {
            if (value == field) return
            scrollAmountX = 0f
            scrollAmountY = 0f
            scaleOverride = 0f
            field = value
        }

    override fun init() {
        ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
            ScreenMouseEvents.afterMouseScroll(screen).register { _, _, _, _, delta, _ ->
                if (!enabled) return@register true
                if (screen !is AbstractContainerScreen<*>) return@register true
                val accessor = screen as? IAbstractContainerScreen ?: return@register true
                val slot = accessor.hoveredSlot ?: return@register true
                if (slot.item.isEmpty) return@register true

                val slotIdx = slot.index
                if (slotIdx != lastSlotIndex) {
                    scrollAmountX = 0f
                    scrollAmountY = 0f
                    scaleOverride = 0f
                    lastSlotIndex = slotIdx
                }

                val holdingShift = GLFW.glfwGetKey(mc.window.handle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                val holdingCtrl = GLFW.glfwGetKey(mc.window.handle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS

                when {
                    holdingShift && !holdingCtrl -> scrollAmountX += (sign(delta).toFloat() * scrollSpeed.value.toFloat())
                    !holdingShift && holdingCtrl -> scaleOverride += (sign(delta).toFloat() * 0.1f * scaleSpeed.value.toFloat())
                    else -> scrollAmountY += (sign(delta).toFloat() * scrollSpeed.value.toFloat())
                }

                true
            }
        }
    }
}