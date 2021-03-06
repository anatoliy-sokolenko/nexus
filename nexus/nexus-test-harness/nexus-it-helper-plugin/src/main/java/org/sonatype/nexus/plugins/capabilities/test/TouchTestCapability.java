/**
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions
 *
 * This program is free software: you can redistribute it and/or modify it only under the terms of the GNU Affero General
 * Public License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License Version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License Version 3 along with this program.  If not, see
 * http://www.gnu.org/licenses.
 *
 * Sonatype Nexus (TM) Open Source Version is available from Sonatype, Inc. Sonatype and Sonatype Nexus are trademarks of
 * Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation. M2Eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.plugins.capabilities.test;

import java.io.ByteArrayInputStream;
import java.util.Map;

import org.sonatype.nexus.plugins.capabilities.api.AbstractRepositoryCapability;
import org.sonatype.nexus.plugins.capabilities.api.Capability;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;

public class TouchTestCapability
    extends AbstractRepositoryCapability
    implements Capability
{

    public static final String ID = "TouchTest";

    protected TouchTestCapability( String id, RepositoryRegistry repositoryRegistry )
    {
        super( id, repositoryRegistry );
    }

    @Override
    public void create( Map<String, String> properties )
    {
        doIt( properties );
    }

    @Override
    public void update( Map<String, String> properties )
    {
        doIt( properties );
    }

    @Override
    public void load( Map<String, String> properties )
    {
        doIt( properties );
    }

    private void doIt( final Map<String, String> properties )
    {
        final Repository repo = getRepository( TouchTestCapabilityDescriptor.FIELD_REPO_OR_GROUP_ID, properties );

        try
        {
            repo.storeItem(
                new ResourceStoreRequest( "/capability/test.txt" ),
                new ByteArrayInputStream(
                    ( "capabilities test!\n" + properties.get( TouchTestCapabilityDescriptor.FIELD_MSG_ID ) + "\n" + properties.get( TouchTestCapabilityDescriptor.FIELD_REPO_OR_GROUP_ID ) ).getBytes() ),
                null );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }

    }
}
