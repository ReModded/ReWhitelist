package dev.remodded.rewhitelist.entries

import com.google.gson.JsonObject
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.command.VelocityBrigadierMessage
import com.velocitypowered.api.proxy.Player
import dev.remodded.rewhitelist.utils.CommandUtils.argument
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.math.min

class IPEntry private constructor(factory: Factory, val minAddress: InetAddress, val maxAddress: InetAddress) : Entry(factory) {

    override fun match(player: Player): Boolean {
        val address = player.remoteAddress.address.address
        return compareIps(address, minAddress.address) >= 0 && compareIps(address, maxAddress.address) <= 0
    }

    override fun toString(): String {
        return "$${minAddress.hostAddress}-${maxAddress.hostAddress}"
    }

    object Factory : Entry.Factory<IPEntry>() {
        override val type = "ip"

        override fun save(entry: IPEntry): JsonObject {
            return JsonObject().apply {
                addProperty("minAddress", entry.minAddress.hostAddress)
                addProperty("maxAddress", entry.maxAddress.hostAddress)
            }
        }

        override fun load(data: JsonObject): IPEntry {
            return IPEntry(
                this,
                InetAddress.getByName(data["minAddress"].asString),
                InetAddress.getByName(data["maxAddress"].asString),
            )
        }

        override fun getCommandNode(entryConsumer: (CommandContext<CommandSource>, IPEntry) -> Unit): ArgumentBuilder<CommandSource, *> =
            argument("minAddress", StringArgumentType.string())
                .then(
                    argument("maxAddress", StringArgumentType.string())
                        .executes { ctx ->
                            val entry = addEntry(StringArgumentType.getString(ctx, "minAddress"), StringArgumentType.getString(ctx, "maxAddress"))
                            entryConsumer(ctx, entry)
                            0
                        }
                )
                .executes { ctx ->
                    val entry = addEntry(StringArgumentType.getString(ctx, "minAddress"), null)
                    entryConsumer(ctx, entry)
                    0
                }


        val MALFORMED_ADDRESS_EXCEPTION = DynamicCommandExceptionType{ arg -> VelocityBrigadierMessage.tooltip(Component.text("Malformed address ($arg) [eg. 127.0.0.1]", NamedTextColor.RED)) }
        val MALFORMED_ADDRESS_MASK_EXCEPTION = DynamicCommandExceptionType{ arg -> VelocityBrigadierMessage.tooltip(Component.text("Malformed address with mask ($arg) [eg. 127.0.0.1/24]", NamedTextColor.RED)) }
        val MALFORMED_ADDRESS_RANGE_EXCEPTION = DynamicCommandExceptionType{ arg -> VelocityBrigadierMessage.tooltip(Component.text("Malformed address range ($arg) [eg. 127.0.0.1-127.0.0.255]", NamedTextColor.RED)) }

        val MALFORMED_MASK_EXCEPTION = Dynamic2CommandExceptionType{ arg, arg2 -> VelocityBrigadierMessage.tooltip(Component.text("Malformed address mask ($arg) [0 - $arg2]", NamedTextColor.RED)) }

        private fun addEntry(minAddress: String, maxAddress: String?): IPEntry {
            if (maxAddress == null) {
                // Range in single argument
                if (minAddress.contains('-')) {
                    val split = minAddress.split('-')
                    if (split.size != 2) throw MALFORMED_ADDRESS_RANGE_EXCEPTION.create(minAddress)
                    return addEntry(split[0], split[1])
                }

                // Range specified by mask
                if (minAddress.contains('/')) {
                    val split = minAddress.split('/')
                    if (split.size != 2) throw MALFORMED_ADDRESS_MASK_EXCEPTION.create(minAddress)

                    val minAddressBytes = try {
                        InetAddress.getByName(split[0]).address
                    } catch (_: UnknownHostException) {
                        throw MALFORMED_ADDRESS_EXCEPTION.create(split[0])
                    }

                    val maxMask = minAddressBytes.size * 8 // 8 bits per byte

                    val mask = try { split[1].toInt() } catch (_: NumberFormatException) { throw MALFORMED_MASK_EXCEPTION.create(split[1], maxMask) }
                    if (mask < 0 || mask > maxMask) throw MALFORMED_MASK_EXCEPTION.create(split[1], maxMask)

                    val maskBytes = generateMask(mask, minAddressBytes.size)

                    val maxAddressBytes = minAddressBytes.clone()

                    for (i in 0 ..< maskBytes.size) {
                        val currentByte = maskBytes[i]
                        minAddressBytes[i] = minAddressBytes[i] and currentByte
                        maxAddressBytes[i] = maxAddressBytes[i] or currentByte.inv()
                    }

                    return IPEntry(this, InetAddress.getByAddress(minAddressBytes), InetAddress.getByAddress(maxAddressBytes))
                }
            }

            val minAddress = try {
                InetAddress.getByName(minAddress)
            } catch (_: UnknownHostException) {
                throw MALFORMED_ADDRESS_EXCEPTION.create(minAddress)
            }

            if (maxAddress.isNullOrEmpty()) {
                return IPEntry(this, minAddress, minAddress)
            }

            val maxAddress = try {
                InetAddress.getByName(maxAddress)
            } catch (_: UnknownHostException) {
                throw MALFORMED_ADDRESS_EXCEPTION.create(maxAddress)
            }

            if (compareIps(minAddress.address, maxAddress.address) > 0)
                throw MALFORMED_ADDRESS_RANGE_EXCEPTION.create("$minAddress-$maxAddress")

            return IPEntry(this, minAddress, maxAddress)
        }

        override fun getHelp(): List<Component> {
            return listOf(
                Component.text("<address>"),
                Component.text("<minAddress> <maxAddress>"),
            )
        }
    }

    companion object {
        private fun generateMask(mask: Int, ipAddressLength: Int): ByteArray {
            var maskBits: Int = mask
            val maskBytes = ByteArray(ipAddressLength)
            for (i in 0..<ipAddressLength) {
                maskBytes[i] = (0xFF shl (8 - min(8, maskBits))).toByte()
                maskBits -= 8
            }
            return maskBytes
        }

        private fun compareIps(ip1: ByteArray, ip2: ByteArray): Int {
            if (ip1.size != ip2.size)
                throw IllegalArgumentException("IP addresses must be of the same version (IPv4 or IPv6)")

            for (i in 0..<ip1.size) {
                val result = ip1[i].toUByte().compareTo(ip2[i].toUByte())
                if (result != 0) {
                    return result
                }
            }
            return 0
        }
    }
}
