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
package org.sonatype.nexus.test.utils;

import static org.hamcrest.MatcherAssert.*;
import static org.sonatype.nexus.test.utils.NexusRequestMatchers.*;

import java.io.IOException;
import java.util.List;

import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.restlet.resource.StringRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.security.rest.model.PlexusUserListResourceResponse;
import org.sonatype.security.rest.model.PlexusUserResource;
import org.sonatype.security.rest.model.PlexusUserResourceResponse;
import org.sonatype.security.rest.model.UserListResourceResponse;
import org.sonatype.security.rest.model.UserResource;
import org.sonatype.security.rest.model.UserResourceRequest;
import org.testng.Assert;

import com.thoughtworks.xstream.XStream;
public class UserMessageUtil
    extends ITUtil
{
    private XStream xstream;

    private MediaType mediaType;

    private static final Logger LOG = LoggerFactory.getLogger( UserMessageUtil.class );

    public UserMessageUtil( AbstractNexusIntegrationTest test, XStream xstream, MediaType mediaType )
    {
        super( test );
        this.xstream = xstream;
        this.mediaType = mediaType;
    }

    public UserResource createUser( UserResource user )
        throws IOException
    {

        Response response = this.sendMessage( Method.POST, user );

        if ( !response.getStatus().isSuccess() )
        {
            String responseText = response.getEntity().getText();
            Assert.fail( "Could not create user: " + response.getStatus() + ":\n" + responseText );
        }

        // get the Resource object
        UserResource responseResource = this.getResourceFromResponse( response );

        // make sure the id != null
        Assert.assertNotNull( responseResource.getUserId(), "User ID shouldn't be null: "
            + response.getEntity().getText() );
        user.setUserId( responseResource.getUserId() );

        Assert.assertEquals( user.getFirstName(), responseResource.getFirstName() );
        Assert.assertEquals( user.getLastName(), responseResource.getLastName() );
        Assert.assertEquals( user.getUserId(), responseResource.getUserId() );
        Assert.assertEquals( user.getStatus(), responseResource.getStatus() );
        Assert.assertEquals( user.getEmail(), responseResource.getEmail() );
        Assert.assertEquals( user.getRoles(), responseResource.getRoles() );

        getTest().getSecurityConfigUtil().verifyUser( user );

        return user;
    }

    public UserResource getUser( String userId )
        throws IOException
    {

        String entityText = RequestFacade.doGetForText( "service/local/users/" + userId );

        XStreamRepresentation representation =
            new XStreamRepresentation( XStreamFactory.getXmlXStream(), entityText, MediaType.APPLICATION_XML );

        UserResourceRequest resourceResponse =
            (UserResourceRequest) representation.getPayload( new UserResourceRequest() );

        return resourceResponse.getData();
    }

    public UserResource updateUser( UserResource user )
        throws IOException
    {
        Response response = null;
        UserResource responseResource;
        try {
            response = this.sendMessage( Method.PUT, user );
            responseResource = this.getResourceFromResponse( response );
            assertThat(response, isSuccessful());
        } finally {
            RequestFacade.releaseResponse(response);
        }

        // make sure the id != null

        Assert.assertEquals( user.getFirstName(), responseResource.getFirstName() );
        Assert.assertEquals( user.getLastName(), responseResource.getLastName() );
        Assert.assertEquals( user.getUserId(), responseResource.getUserId() );
        Assert.assertEquals( user.getStatus(), responseResource.getStatus() );
        Assert.assertEquals( user.getEmail(), responseResource.getEmail() );
        Assert.assertEquals( user.getRoles(), responseResource.getRoles() );

        getTest().getSecurityConfigUtil().verifyUser( user );
        return responseResource;
    }
    /**
     * IMPORTANT: Make sure to release the Response in a finally block when you are done with it.
     */
    public Response sendMessage( Method method, UserResource resource )
        throws IOException
    {

        XStreamRepresentation representation = new XStreamRepresentation( xstream, "", mediaType );

        String userId = ( method == Method.POST ) ? "" : "/" + resource.getUserId();

        String serviceURI = "service/local/users" + userId;

        UserResourceRequest userRequest = new UserResourceRequest();
        userRequest.setData( resource );

        // now set the payload
        representation.setPayload( userRequest );

        return RequestFacade.sendMessage( serviceURI, method, representation );
    }

    /**
     * This should be replaced with a REST Call, but the REST client does not set the Accept correctly on GET's/
     *
     * @return
     * @throws IOException
     */
    @SuppressWarnings( "unchecked" )
    public List<UserResource> getList()
        throws IOException
    {
        String responseText = RequestFacade.doGetForText( "service/local/users" );
        LOG.debug( "responseText: \n" + responseText );

        // must use the XML xstream even if we 'thought' we wanted to use JSON, because REST client doesn't listen to
        // the MediaType in some situations.
        XStreamRepresentation representation =
            new XStreamRepresentation( XStreamFactory.getXmlXStream(), responseText, MediaType.APPLICATION_XML );

        UserListResourceResponse resourceResponse =
            (UserListResourceResponse) representation.getPayload( new UserListResourceResponse() );

        return resourceResponse.getData();

    }

    public UserResource getResourceFromResponse( Response response )
        throws IOException
    {
        String responseString = response.getEntity().getText();
        LOG.debug( " getResourceFromResponse: " + responseString );

        XStreamRepresentation representation = new XStreamRepresentation( xstream, responseString, mediaType );

        // this
        UserResourceRequest resourceResponse =
            (UserResourceRequest) representation.getPayload( new UserResourceRequest() );

        return resourceResponse.getData();
    }

    public Object parseResponseText( String responseString, Object responseType )
        throws IOException
    {
        XStreamRepresentation representation = new XStreamRepresentation( xstream, responseString, mediaType );

        return representation.getPayload( responseType );
    }

    @SuppressWarnings( "unchecked" )
    public List<PlexusUserResource> getPlexusUsers( String source )
        throws IOException
    {
        // plexus_users
        String uriPart = RequestFacade.SERVICE_LOCAL + "plexus_users/" + source;

        Response response = null;
        String entityText;
        try {
            response = RequestFacade.sendMessage( uriPart, Method.GET, new StringRepresentation( "", this.mediaType ) );
            entityText = response.getEntity().getText();
            assertThat(response, isSuccessful());
        } finally {
            RequestFacade.releaseResponse(response);
        }

        PlexusUserListResourceResponse result =
            (PlexusUserListResourceResponse) this.parseResponseText( entityText,
                                                                     new PlexusUserListResourceResponse() );

        return result.getData();
    }

    public PlexusUserResource getPlexusUser( String source, String userId )
        throws IOException
    {
        String sourcePart = ( source != null ) ? source + "/" : "";

        // plexus_user
        String uriPart = RequestFacade.SERVICE_LOCAL + "plexus_user/" + sourcePart + userId;

        Response response = null;
        String entityText;
        try {
            response = RequestFacade.sendMessage( uriPart, Method.GET, new StringRepresentation( "", this.mediaType ) );
            entityText = response.getEntity().getText();
            assertThat(response, isSuccessful());
        } finally {
            RequestFacade.releaseResponse(response);
        }

        PlexusUserResourceResponse result =
            (PlexusUserResourceResponse) this.parseResponseText( entityText, new PlexusUserResourceResponse() );

        return result.getData();
    }

    public List<PlexusUserResource> searchPlexusUsers( String source )
        throws IOException
    {
        return searchPlexusUsers( source, "" );
    }

    @SuppressWarnings( "unchecked" )
    public List<PlexusUserResource> searchPlexusUsers( String source, String userId )
        throws IOException
    {
        // user_search
        String uriPart = RequestFacade.SERVICE_LOCAL + "user_search/" + source + "/" + userId;

        Response response = null;
        String entityText;
        try {
            response = RequestFacade.sendMessage( uriPart, Method.GET, new StringRepresentation( "", this.mediaType ) );
            entityText = response.getEntity().getText();
            assertThat(response, isSuccessful());
        } finally {
            RequestFacade.releaseResponse(response);
        }

        PlexusUserListResourceResponse result =
            (PlexusUserListResourceResponse) this.parseResponseText( entityText,
                                                                     new PlexusUserListResourceResponse() );

        return result.getData();
    }

    /**
     * Delete the user, asserting the user was deleted successfully by checking the response status code
     * @param userId
     * @return Returns true when the user was deleted and false when it was not deleted
     * @throws IOException problem with operation
     * @throws IllegalArgumentException if you try to delete the 'anonymous' user id
     */
    public static void removeUser( String userId )
        throws IOException
    {
        if ( "anonymous".equals( userId ) )
        {
            throw new IllegalArgumentException( "Unable to delete anonymous user" );
        }
        RequestFacade.doDelete("service/local/users/" + userId, isSuccessful());

    }

    /**
     * @param userId user to be disable
     * @return returns the disabled user instance
     * @throws IOException
     */
    public UserResource disableUser( String userId )
        throws IOException
    {
        UserResource user = getUser( "anonymous" );
        user.setStatus( "disabled" );
        updateUser( user );
        return user;
    }

}
