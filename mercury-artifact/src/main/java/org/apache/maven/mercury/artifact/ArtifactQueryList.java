/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.maven.mercury.artifact;

import java.util.Collection;

/**
 * @author Oleg Gusakov
 * @version $Id: ArtifactQueryList.java 762963 2009-04-07 21:01:07Z ogusakov $
 */
public class ArtifactQueryList
    extends ArtifactMetadataList
{

    /**
     * @param md
     */
    public ArtifactQueryList( ArtifactMetadata... md )
    {
        super( md );
    }

    /**
     * @param md
     */
    public ArtifactQueryList( Collection<ArtifactMetadata> md )
    {
        super( md );
    }

    /**
     * @param mds
     */
    public ArtifactQueryList( String... mds )
    {
        super( mds );
    }

}
