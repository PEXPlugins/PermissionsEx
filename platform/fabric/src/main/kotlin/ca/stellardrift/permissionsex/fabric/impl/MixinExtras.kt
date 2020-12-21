/*
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.stellardrift.permissionsex.fabric.impl

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo
import org.spongepowered.asm.mixin.injection.InjectionPoint
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData

/**
 * A config plugin that will just apply our own injection point
 */
class PermissionsExMixinsPlugin : IMixinConfigPlugin {
    override fun onLoad(mixinPackage: String) {
        InjectionPoint.register(WitherMutator::class.java)
    }

    // Default implementations of other methods

    override fun getRefMapperConfig(): String? = null
    override fun shouldApplyMixin(targetClassName: String, mixinClassName: String): Boolean = true
    override fun acceptTargets(myTargets: MutableSet<String>, otherTargets: MutableSet<String>) = Unit
    override fun getMixins(): List<String> = listOf()

    override fun preApply(
        targetClassName: String,
        targetClass: ClassNode,
        mixinClassName: String,
        mixinInfo: IMixinInfo
    ) {
        // no-op
    }

    override fun postApply(
        targetClassName: String,
        targetClass: ClassNode,
        mixinClassName: String,
        mixinInfo: IMixinInfo
    ) {
        // no-op
    }
}

private const val METHOD_CONSTRUCTOR = "<init>"

/**
 * An injection point that will target the `return` of a with-er method in a data class.
 *
 * This injection point is designed to be used with wildcard method targets, to allow applying fields
 * added by the mixin to the returned new instance.
 */
@InjectionPoint.AtCode("WITHER_MUTATOR")
internal class WitherMutator(data: InjectionPointData) : InjectionPoint(data) {

    private val targetClassRef = data.context.targetClassRef

    override fun find(desc: String, insns: InsnList, nodes: MutableCollection<AbstractInsnNode>): Boolean {
        // If the method return type isn't the target class, then we skip injecting at all
        val returnType = Type.getReturnType(desc)
        if (returnType.sort != Type.OBJECT ||
            targetClassRef != returnType.internalName) {
            return false
        }

        // Find the call of the containing type's <init>, where the following instruction is a return
        val it = insns.iterator()
        var hasSeenNew = false
        var foundAny = true

        while (it.hasNext()) {
            val next = it.next()
            when (next.opcode) {
                Opcodes.NEW -> hasSeenNew = true
                Opcodes.INVOKESPECIAL -> if (hasSeenNew) {
                    require(next is MethodInsnNode) // this should be implied by the opcode

                    // require that method is constructor and owner is the target class type
                    if (next.name != METHOD_CONSTRUCTOR || next.owner != targetClassRef) {
                        continue
                    }

                    if (next.next != null && next.next.opcode == Opcodes.ARETURN) {
                        nodes.add(next.next)
                        hasSeenNew = false
                        foundAny = true
                    }
                }
            }
        }
        return foundAny
    }
}
