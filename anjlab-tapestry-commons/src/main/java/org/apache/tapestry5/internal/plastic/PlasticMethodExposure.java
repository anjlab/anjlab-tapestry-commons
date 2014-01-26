package org.apache.tapestry5.internal.plastic;

import java.lang.reflect.Field;

import org.apache.tapestry5.internal.plastic.asm.tree.MethodNode;
import org.apache.tapestry5.plastic.PlasticMethod;

public class PlasticMethodExposure
{
    public static MethodNode getNode(PlasticMethod plasticMethod)
    {
        try
        {
            Field nodeField = ((PlasticMethodImpl) plasticMethod)
                    .getClass().getDeclaredField("node");
            
            nodeField.setAccessible(true);
            
            try
            {
                return (MethodNode) nodeField.get(plasticMethod);
            }
            finally 
            {
                nodeField.setAccessible(false);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed accessing Plastic internals", e);
        }
    }
}
