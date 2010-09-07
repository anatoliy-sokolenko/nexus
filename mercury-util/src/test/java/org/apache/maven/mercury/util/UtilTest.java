/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.maven.mercury.util;

import junit.framework.TestCase;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id: UtilTest.java 750267 2009-03-05 01:09:25Z ogusakov $
 *
 */
public class UtilTest
    extends TestCase
{
    public void testConvertLength()
    {
        String s = Util.convertLength( 25L );
        assertEquals( "25 bytes", s );
        
        s = Util.convertLength( 4999L );
        assertEquals( "4999 bytes", s );
        
        s = Util.convertLength( 5800L );
        assertEquals( "6 kb", s );
        
        s = Util.convertLength( 6400L );
        assertEquals( "6 kb", s );
    }
}
