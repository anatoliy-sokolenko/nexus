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
package org.sonatype.nexus.integrationtests.nexus3699;

import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import static org.hamcrest.Matchers.*;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.plugins.capabilities.internal.rest.dto.CapabilityListItemResource;
import org.sonatype.nexus.plugins.capabilities.internal.rest.dto.CapabilityPropertyResource;
import org.sonatype.nexus.plugins.capabilities.internal.rest.dto.CapabilityResource;
import org.sonatype.nexus.test.utils.CapabilitiesMessageUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Nexus3699CapabilityIT
    extends AbstractNexusIntegrationTest
{

    @Override
    protected void copyConfigFiles()
        throws IOException
    {
        super.copyConfigFiles();

        this.copyConfigFile( "capabilities.xml", WORK_CONF_DIR );

        // also need to move the plugin from optional to used
        installOptionalPlugin( "nexus-capabilities-plugin" );
    }

    @Test
    public void crud()
        throws Exception
    {
        // create
        CapabilityResource cap = new CapabilityResource();
        cap.setName( "crud-test" );
        cap.setTypeId( "TouchTest" );
        CapabilityPropertyResource prop = new CapabilityPropertyResource();
        prop.setKey( "repoOrGroupId" );
        prop.setValue( "repo_" + REPO_TEST_HARNESS_REPO );
        cap.addProperty( prop );
        prop = new CapabilityPropertyResource();
        prop.setKey( "message" );
        prop.setValue( "Testing CRUD" );
        cap.addProperty( prop );

        CapabilityListItemResource r = CapabilitiesMessageUtil.create( cap );
        Assert.assertNotNull( r.getId() );

        // read
        CapabilityResource read = CapabilitiesMessageUtil.read( r.getId() );
        Assert.assertEquals( r.getId(), read.getId() );
        Assert.assertEquals( cap.getName(), read.getName() );
        Assert.assertEquals( cap.getTypeId(), read.getTypeId() );
        Assert.assertEquals( cap.getProperties().size(), read.getProperties().size() );

        // update
        read.setName( "updateCrudTest" );
        CapabilityListItemResource updated = CapabilitiesMessageUtil.update( read );
        Assert.assertEquals( "updateCrudTest", updated.getName() );
        read = CapabilitiesMessageUtil.read( r.getId() );
        Assert.assertEquals( "updateCrudTest", read.getName() );

        // delete
        CapabilitiesMessageUtil.delete( r.getId() );
    }

    @Test
    public void execution()
        throws Exception
    {
        List<CapabilityListItemResource> data = CapabilitiesMessageUtil.list();

        Assert.assertFalse( data.isEmpty() );
        MatcherAssert.assertThat( data.get( 0 ).getId(), CoreMatchers.equalTo( "4fde59a80f4" ) );
        MatcherAssert.assertThat( data.get( 0 ).getName(), CoreMatchers.equalTo( "test-capability" ) );
        MatcherAssert.assertThat( data.get( 0 ).getTypeId(), CoreMatchers.equalTo( "TouchTest" ) );

        File touch = new File( nexusWorkDir, "storage/nexus-test-harness-repo/capability/test.txt" );
        assertTrue( touch.exists() );

        String content = FileUtils.readFileToString( touch );
        MatcherAssert.assertThat( content, containsString( "capabilities test!" ) );
        MatcherAssert.assertThat( content, containsString( "repo_nexus-test-harness-repo" ) );

        CapabilityResource cap = CapabilitiesMessageUtil.read( "4fde59a80f4" );
        setMessage( cap, "capability updated!" );
        CapabilitiesMessageUtil.update( cap );

        content = FileUtils.readFileToString( touch );
        MatcherAssert.assertThat( content, containsString( "capability updated!" ) );
        MatcherAssert.assertThat( content, containsString( "repo_nexus-test-harness-repo" ) );
    }

    private void setMessage( CapabilityResource cap, String msg )
    {
        for ( CapabilityPropertyResource prop : cap.getProperties() )
        {
            if ( prop.getKey().equals( "message" ) )
            {
                prop.setValue( msg );
            }
        }
    }

}
