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
package ca.stellardrift.permissionsex.fabric.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

import java.util.Collection;
import java.util.Iterator;

/**
 * An injection point that will target the `return` of a with-er method in a data class.
 *
 * <p>This injection point is designed to be used with wildcard method targets, to allow applying fields
 * added by the mixin to the returned new instance.</p>
 */
@InjectionPoint.AtCode("WITHER_MUTATOR")
public class WitherMutatorInjectionPoint extends InjectionPoint {

    private static final String METHOD_CONSTRUCTOR = "<init>";

    private final String targetClassRef;

    public WitherMutatorInjectionPoint(final InjectionPointData data) {
        super(data);
        this.targetClassRef = data.getContext().getTargetClassRef();
    }

    @Override
    public boolean find(final String desc, final InsnList insns, final Collection<AbstractInsnNode> nodes) {
        // If the method return type isn't the target class, then we skip injecting at all
        final Type returnType = Type.getReturnType(desc);
        if (returnType.getSort() != Type.OBJECT
            || !targetClassRef.equals(returnType.getInternalName())) {
            return false;
        }

        // Find the call of the containing type's <init>, where the following instruction is a return
        final Iterator<AbstractInsnNode> it = insns.iterator();
        boolean hasSeenNew = false;
        boolean foundAny = false;

        while (it.hasNext()) {
            final AbstractInsnNode next = it.next();
            switch (next.getOpcode()) {
                case Opcodes.NEW:
                    hasSeenNew = true;
                    break;
                case Opcodes.INVOKESPECIAL:
                    if (hasSeenNew) {
                        assert next instanceof MethodInsnNode; // implied by opcode
                        final MethodInsnNode method = (MethodInsnNode) next;

                        // require that method is constructor and owner is the target class type
                        if (!method.name.equals(METHOD_CONSTRUCTOR) || !method.owner.equals(targetClassRef)) {
                            continue;
                        }

                        if (next.getNext() != null && next.getNext().getOpcode() == Opcodes.ARETURN) {
                            nodes.add(next.getNext());
                            hasSeenNew = false;
                            foundAny = true;
                        }
                    }
                    break;
            }
        }
        return foundAny;
    }

}
