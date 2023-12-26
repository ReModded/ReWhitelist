package dev.remodded.rewhitelist.utils

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import java.util.function.Predicate

object CommandUtils {
    fun permissionRequirement(permission: String): Predicate<CommandSource> {
        return Predicate<CommandSource>{ src -> src !is Player || src.hasPermission(permission) }
    }
}
