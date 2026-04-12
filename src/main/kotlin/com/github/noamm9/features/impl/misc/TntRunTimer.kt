package com.github.noamm9.features.impl.misc

import com.github.noamm9.event.impl.BlockChangeEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.render.Render3D
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import java.awt.Color

object TntRunTimer: Feature("Shows a timer on TNT Run blocks you step on.") {
    private const val BREAK_TICKS = 7

    private data class BlockInfo(
        val startTick: Long,
        val startMs: Long,
        var ticksLeft: Int = BREAK_TICKS,
        var contactTicks: Int = 0,
        var leftTick: Long? = null,
        var onBlock: Boolean = true
    )

    private val steppedBlocks = mutableMapOf<BlockPos, BlockInfo>()
    private var lastPos: BlockPos? = null
    private var serverTick = 0L

    override fun init() {
        register<TickEvent.Start> {
            val player = mc.player ?: return@register
            val below = BlockPos.containing(player.x, player.y - 0.5, player.z)
            val block = mc.level?.getBlockState(below)?.block ?: return@register

            if (block == Blocks.SAND || block == Blocks.GRAVEL || block == Blocks.TNT) {
                if (below != lastPos) {
                    steppedBlocks.putIfAbsent(below, BlockInfo(serverTick, System.currentTimeMillis()))
                    lastPos = below
                }
            }

            // track contact / leave
            steppedBlocks.forEach { (pos, info) ->
                val playerOn = below == pos
                if (playerOn && info.onBlock) info.contactTicks ++
                if (! playerOn && info.onBlock) {
                    info.onBlock = false
                    info.leftTick = serverTick
                }
            }

            steppedBlocks.keys.removeAll { mc.level?.getBlockState(it)?.isAir == true }
        }

        register<TickEvent.Server> {
            serverTick ++
            val toRemove = mutableListOf<BlockPos>()
            steppedBlocks.forEach { (pos, info) ->
                info.ticksLeft --
                if (info.ticksLeft <= 0) toRemove.add(pos)
            }
            toRemove.forEach { steppedBlocks.remove(it) }
        }

        register<RenderWorldEvent> {
            steppedBlocks.forEach { (pos, info) ->
                val ratio = (info.ticksLeft.toFloat() / BREAK_TICKS).coerceIn(0f, 1f)
                val color = Color(((1f - ratio) * 255).toInt(), (ratio * 255).toInt(), 0)

                Render3D.renderString(
                    "${info.ticksLeft}t",
                    pos.x + 0.5, pos.y + 1.0, pos.z + 0.5,
                    color, scale = 1.5f, phase = true
                )
            }
        }

        register<BlockChangeEvent> {
            if (! event.newState.isAir) return@register
            val info = steppedBlocks.remove(event.pos) ?: return@register
            val now = System.currentTimeMillis()
            val ticksElapsed = serverTick - info.startTick
            val msElapsed = now - info.startMs
            val leftAt = info.leftTick?.let { serverTick - it } ?: 0
            val status = if (info.onBlock) "&cstill on" else "&aleft ${leftAt}t ago"

            ChatUtils.modMessage(
                "&eTntRun &7> &b${event.oldBlock.name.string} &7broke after &b${ticksElapsed}t &7(${msElapsed}ms)" +
                " &7| contact: &e${info.contactTicks}ct &7| left: &c${info.ticksLeft}t &7| status: $status"
            )
        }

        register<WorldChangeEvent> {
            steppedBlocks.clear()
            lastPos = null
            serverTick = 0
        }
    }
}
