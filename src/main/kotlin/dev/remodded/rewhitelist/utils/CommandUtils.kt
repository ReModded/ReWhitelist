package dev.remodded.rewhitelist.utils

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.velocitypowered.api.command.BrigadierCommand.literalArgumentBuilder
import com.velocitypowered.api.command.BrigadierCommand.requiredArgumentBuilder
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import java.util.function.Predicate

object CommandUtils {
    fun permissionRequirement(permission: String): Predicate<CommandSource> {
        return Predicate<CommandSource>{ src -> src !is Player || src.hasPermission(permission) }
    }

    fun literal(name: String): LiteralArgumentBuilder<CommandSource> = literalArgumentBuilder(name)
    fun <T> argument(name: String, type: ArgumentType<T>): RequiredArgumentBuilder<CommandSource, T> = requiredArgumentBuilder(name, type)
}
