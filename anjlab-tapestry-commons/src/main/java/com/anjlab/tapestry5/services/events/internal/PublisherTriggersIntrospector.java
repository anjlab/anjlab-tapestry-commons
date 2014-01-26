package com.anjlab.tapestry5.services.events.internal;

import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.internal.plastic.PlasticMethodExposure;
import org.apache.tapestry5.internal.plastic.asm.Type;
import org.apache.tapestry5.internal.plastic.asm.tree.AbstractInsnNode;
import org.apache.tapestry5.internal.plastic.asm.tree.FieldInsnNode;
import org.apache.tapestry5.internal.plastic.asm.tree.InsnList;
import org.apache.tapestry5.internal.plastic.asm.tree.LdcInsnNode;
import org.apache.tapestry5.internal.plastic.asm.tree.MethodInsnNode;
import org.apache.tapestry5.internal.plastic.asm.tree.MethodNode;
import org.apache.tapestry5.model.MutableComponentModel;
import org.apache.tapestry5.plastic.PlasticClass;
import org.apache.tapestry5.plastic.PlasticMethod;
import org.apache.tapestry5.services.transform.ComponentClassTransformWorker2;
import org.apache.tapestry5.services.transform.TransformationSupport;

import com.anjlab.tapestry5.services.events.Publisher;

public class PublisherTriggersIntrospector implements ComponentClassTransformWorker2
{
    private PublisherConfiguration publisherConfiguration;
    
    public PublisherTriggersIntrospector(PublisherConfiguration publisherConfiguration)
    {
        this.publisherConfiguration = publisherConfiguration;
    }
    
    @Override
    public void transform(PlasticClass plasticClass,
            TransformationSupport support, MutableComponentModel model)
    {
        for (PlasticMethod plasticMethod : plasticClass.getMethods())
        {
            MethodNode methodNode = PlasticMethodExposure.getNode(plasticMethod);
            
            InsnList list = methodNode.instructions;
            
            for (int i = 0; i < list.size(); i++)
            {
                AbstractInsnNode insn = list.get(i);
                
                if (insn instanceof MethodInsnNode)
                {
                    MethodInsnNode methodCall = ((MethodInsnNode) insn);
                    
                    checkPublisherTriggerEvent(plasticMethod, methodCall);
                    checkComponentResourcesCreateEventLink(plasticMethod, methodCall);
                }
            }
            
            if (!checkOnEventAnnotationHandler(plasticMethod))
            {
                checkOnEventMethodNameHandler(plasticMethod);
            }
        }
    }

    private boolean checkOnEventMethodNameHandler(PlasticMethod plasticMethod)
    {
        String methodName = plasticMethod.getDescription().methodName;
        
        if (methodName.startsWith("on"))
        {
            publisherConfiguration.addEventHandler(methodName.substring(2), plasticMethod);
            return true;
        }
        return false;
    }

    private boolean checkOnEventAnnotationHandler(PlasticMethod plasticMethod)
    {
        if (plasticMethod.hasAnnotation(OnEvent.class))
        {
            OnEvent onEvent = plasticMethod.getAnnotation(OnEvent.class);
            publisherConfiguration.addEventHandler(onEvent.value(), plasticMethod);
            return true;
        }
        return false;
    }

    private void checkComponentResourcesCreateEventLink(
            PlasticMethod plasticMethod, MethodInsnNode methodCall)
    {
        checkTriggerMethod(plasticMethod, methodCall, ComponentResources.class, "createEventLink");
    }

    private void checkPublisherTriggerEvent(PlasticMethod plasticMethod, MethodInsnNode methodCall)
    {
        checkTriggerMethod(plasticMethod, methodCall, Publisher.class, "trigger");
    }

    private void checkTriggerMethod(PlasticMethod plasticMethod,
            MethodInsnNode methodCall, Class<?> targetInterface,
            String methodPrefix)
    {
        if (methodCall.owner.equals(Type.getInternalName(targetInterface))
                && methodCall.name.startsWith(methodPrefix))
        {
            AbstractInsnNode prev = methodCall.getPrevious();
            
            while (prev != null)
            {
                if (prev instanceof FieldInsnNode)
                {
                    FieldInsnNode fieldInsn = (FieldInsnNode) prev;
                    
                    if (fieldInsn.desc.endsWith("L" + Type.getInternalName(targetInterface) + ";"))
                    {
                        break;
                    }
                }
                prev = prev.getPrevious();
            }
            
            if (prev != null)
            {
                AbstractInsnNode eventTypeInsn = prev.getNext();
                
                if (eventTypeInsn instanceof LdcInsnNode)
                {
                    LdcInsnNode lcdInsnNode = (LdcInsnNode) eventTypeInsn;
                    
                    publisherConfiguration.addTrigger(lcdInsnNode.cst.toString(), plasticMethod);
                }
                else
                {
                    //  Variable? Could be any eventType
                    publisherConfiguration.addTrigger("*", plasticMethod);
                }
            }
        }
    }

}
