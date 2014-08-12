/**
 * Copyright 2014 AnjLab
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
