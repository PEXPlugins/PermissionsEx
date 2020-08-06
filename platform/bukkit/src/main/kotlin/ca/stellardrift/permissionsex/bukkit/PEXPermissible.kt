/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2020 zml [at] stellardrift [dot] ca and PermissionsEx contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ca.stellardrift.permissionsex.bukkit

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.context.BeforeTimeContextDefinition
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.util.NodeTree
import ca.stellardrift.permissionsex.util.subjectIdentifier
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import org.bukkit.entity.Player
import org.bukkit.permissions.Permissible
import org.bukkit.permissions.PermissibleBase
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.permissions.PermissionAttachmentInfo
import org.bukkit.permissions.PermissionDefault
import org.bukkit.plugin.Plugin

/**
 * Implementation of Permissible using PEX for data
 */
internal class PEXPermissible(private val player: Player, private val plugin: PermissionsExPlugin) : PermissibleBase(player) {

    val manager: PermissionsEx<BukkitConfiguration> = plugin.manager
    private val pexSubject: CalculatedSubject = manager.getSubjects(PermissionsEx.SUBJECTS_USER)[player.uniqueId.toString()].get()
    var previousPermissible: Permissible? = null
    private val attachments: MutableSet<PEXPermissionAttachment> = mutableSetOf()

    override fun isOp(): Boolean {
        return super.isOp() // TODO: Implement op handling
    }

    override fun setOp(value: Boolean) {
        super.setOp(value)
    }

    override fun isPermissionSet(name: String): Boolean {
        return getPermissionValue(pexSubject.activeContexts, name.toLowerCase(Locale.ROOT)) != 0
    }

    private fun getPermissionValue(contexts: Set<ContextValue<*>>, permission: String): Int {
        var ret = getPermissionValue0(pexSubject.getPermissions(contexts), permission)
        if (ret == 0) {
            for (meta in METAPERMISSIONS) {
                val match = meta.matchAgainst.matchEntire(permission)
                if (match != null && meta.isMatch(match, pexSubject, contexts)) {
                    ret = 1
                }
            }
        }

        /*
         * We may fall back to op value if no other data is set
         * This only takes into account the permission directly being checked having a default set to FALSE or NOT_OP, and not any parents.
         * I believe this is incorrect, but the real-world impacts are likely minor -zml
         */
        if (ret == 0 && manager.config.platformConfig.fallbackOp) {
            val perm = plugin.permissionList?.get(permission)
            if (perm == null) {
                ret = if (isOp) 1 else 0
            } else if (perm.default != PermissionDefault.FALSE) {
                ret = if (isOp xor (perm.default == PermissionDefault.NOT_OP)) 1 else 0
            }
        }

        if (manager.hasDebugMode()) {
            manager.logger.info(Messages.SUPERPERMS_CHECK_NOTIFY(permission, player.name, contexts, ret))
        }
        return ret
    }

    private fun getPermissionValue0(nodeTree: NodeTree, name: String): Int {
        var result = nodeTree[name]
        if (result != 0) {
            return result
        }
        for ((key, value) in plugin.permissionList?.getParents(name) ?: emptySet()) {
            result = getPermissionValue0(nodeTree, key)
            if (!value) {
                result = -result
            }
            if (result != 0) {
                return result
            }
        }
        return 0
    }

    override fun isPermissionSet(perm: Permission): Boolean {
        return isPermissionSet(perm.name)
    }

    override fun hasPermission(perm: String): Boolean {
        return getPermissionValue(pexSubject.activeContexts, perm.toLowerCase(Locale.ROOT)) > 0
    }

    override fun hasPermission(perm: Permission): Boolean {
        return hasPermission(perm.name)
    }

    override fun addAttachment(plugin: Plugin, name: String, value: Boolean): PermissionAttachment {
        return super.addAttachment(plugin, name, value)
    }

    override fun addAttachment(plugin: Plugin): PermissionAttachment {
        val attach = PEXPermissionAttachment(plugin, player, this)
        pexSubject.transientData()
            .update {
                it.addParent(PermissionsEx.GLOBAL_CONTEXT, ATTACHMENT_TYPE, attach.identifier)
            }
            .thenRun { this.attachments.add(attach) }
        return attach
    }

    fun removeAttachmentInternal(attach: PEXPermissionAttachment): Boolean {
        pexSubject.transientData()
            .update {
                it.removeParent(PermissionsEx.GLOBAL_CONTEXT, ATTACHMENT_TYPE, attach.identifier)
            }
            .thenRun {
                attach.removalCallback?.attachmentRemoved(attach)
            }
        return true
    }

    override fun removeAttachment(attachment: PermissionAttachment) {
        require(attachment is PEXPermissionAttachment) { "Provided attachment was not a PEX attachment!" }
        removeAttachmentInternal(attachment)
        this.attachments.remove(attachment)
    }

    fun removeAllAttachments() {
        for (attach in this.attachments) {
            removeAttachmentInternal(attach)
        }
        this.attachments.clear()
    }

    override fun recalculatePermissions() { // We don't need this currently? Guess could clear cache somehow, but automated should get it right. well, except for people adding children to permissions -- that is just weird
    }

    @Synchronized
    override fun clearPermissions() {
        // todo
    }

    override fun addAttachment(plugin: Plugin, name: String, value: Boolean, ticks: Int): PermissionAttachment? {
        return super.addAttachment(plugin, name, value, ticks)
    }

    override fun addAttachment(
        plugin: Plugin,
        ticks: Int
    ): PermissionAttachment {
        val attach = PEXPermissionAttachment(plugin, player, this)
        pexSubject.transientData()
            .update {
                it.addParent(
                    setOf(
                        BeforeTimeContextDefinition.createValue(ZonedDateTime.now().plus(ticks * 50.toLong(), ChronoUnit.MILLIS))
                    ),
                    ATTACHMENT_TYPE,
                    attach.identifier
                )
            }
            .thenRun { this.attachments.add(attach) }
        return attach
    }

    override fun getEffectivePermissions(): Set<PermissionAttachmentInfo> {
        val activeContexts = pexSubject.activeContexts
        return sequence {
            pexSubject.getPermissions(activeContexts).asMap().forEach { (perm, value) ->
                yield(PermissionAttachmentInfo(player, perm, null, value > 0))
            }

            for (mPerm in METAPERMISSIONS) {
                yieldAll(mPerm.getValues(pexSubject, activeContexts)
                    .map { perm -> PermissionAttachmentInfo(player, perm, null, true) })
            }
        }.toSet()
    }

    companion object {
        private val METAPERMISSIONS = arrayOf(
            /*
             * | Permission                 | Usage
             |----------------------------|------
             | `group.<group>`            | Added for each group a user is in
             | `groups.<group>`           | same as above
             | `options.<option>.<value>` | Each option the user has
             | `prefix.<prefix>`          | User's prefix
             | `suffix.<suffix>`          | User's suffix
             */
            object : Metapermission(Regex("groups?\\.(?<name>.+)")) {
                override fun isMatch(result: MatchResult, subj: CalculatedSubject, contexts: Set<ContextValue<*>>): Boolean {
                    return subjectIdentifier(PermissionsEx.SUBJECTS_GROUP, result.groups["name"]!!.value) in subj.getParents(contexts)
                }

                override fun getValues(subj: CalculatedSubject, contexts: Set<ContextValue<*>>): Sequence<String> {
                    return subj.getParents(contexts).asSequence()
                        .filter { (key, _) -> key == PermissionsEx.SUBJECTS_GROUP }
                        .flatMap { (_, value) -> sequenceOf("group.$value", "groups.$value") }
                }
            },
            object : Metapermission(Regex("options\\.(?<key>.*)\\.(?<value>.*)")) {
                override fun isMatch(result: MatchResult, subj: CalculatedSubject, contexts: Set<ContextValue<*>>): Boolean {
                    return subj.getOption(contexts, result.groups["key"]!!.value)
                        .map { it == result.groups["value"]!!.value }
                        .orElse(false)
                }

                override fun getValues(subj: CalculatedSubject, contexts: Set<ContextValue<*>>): Sequence<String> {
                    return subj.getOptions(contexts).asSequence()
                        .map { (key, value) -> "options.$key.$value" }
                }
            },
            SpecificOptionMetapermission("prefix"),
            SpecificOptionMetapermission("suffix")
        )
    }
}

internal abstract class Metapermission(
    /**
     * Pattern to match against
     */
    internal val matchAgainst: Regex
) {
    abstract fun isMatch(result: MatchResult, subj: CalculatedSubject, contexts: Set<ContextValue<*>>): Boolean
    abstract fun getValues(subj: CalculatedSubject, contexts: Set<ContextValue<*>>): Sequence<String>
}

private class SpecificOptionMetapermission(private val option: String) :
    Metapermission(Regex(Regex.escape(option) + "\\.(?<value>.+)")) {
    override fun isMatch(result: MatchResult, subj: CalculatedSubject, contexts: Set<ContextValue<*>>): Boolean {
        return subj.getOption(contexts, option)
            .map { it == result.groups["value"]?.value }
            .orElse(false)
    }

    override fun getValues(subj: CalculatedSubject, contexts: Set<ContextValue<*>>): Sequence<String> {
        val ret = subj.getOptions(contexts)[option]
        return if (ret == null) emptySequence() else sequenceOf("$option.$ret")
    }
}
