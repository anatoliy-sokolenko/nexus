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
package org.sonatype.security.ldap.realms;

import java.net.MalformedURLException;
import java.util.Set;
import java.util.SortedSet;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;
import org.apache.shiro.realm.ldap.LdapContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.security.authentication.AuthenticationException;
import org.sonatype.security.ldap.LdapAuthenticator;
import org.sonatype.security.ldap.dao.LdapAuthConfiguration;
import org.sonatype.security.ldap.dao.LdapDAOException;
import org.sonatype.security.ldap.dao.LdapGroupDAO;
import org.sonatype.security.ldap.dao.LdapUser;
import org.sonatype.security.ldap.dao.LdapUserDAO;
import org.sonatype.security.ldap.dao.NoLdapUserRolesFoundException;
import org.sonatype.security.ldap.dao.NoSuchLdapGroupException;
import org.sonatype.security.ldap.dao.NoSuchLdapUserException;
import org.sonatype.security.ldap.realms.connector.DefaultLdapConnector;
import org.sonatype.security.ldap.realms.connector.LdapConnector;
import org.sonatype.security.ldap.realms.persist.LdapConfiguration;
import org.sonatype.security.ldap.realms.persist.model.CConnectionInfo;
import org.sonatype.security.ldap.realms.tools.LdapURL;

@Component( role = LdapManager.class )
public class SimpleLdapManager
    implements LdapManager
{

    private Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    private LdapAuthenticator ldapAuthenticator;

    @Requirement
    private LdapUserDAO ldapUserManager;

    @Requirement
    private LdapGroupDAO ldapGroupManager;

    @Requirement
    private LdapConfiguration ldapConfiguration;

    private LdapConnector ldapManagerStrategy;

    public SortedSet<String> getAllGroups()
        throws LdapDAOException
    {
        return this.getLdapManagerConnector().getAllGroups();
    }

    public SortedSet<LdapUser> getAllUsers()
        throws LdapDAOException
    {
        return this.getLdapManagerConnector().getAllUsers();
    }

    public String getGroupName( String groupId )
        throws LdapDAOException, NoSuchLdapGroupException
    {
        return this.getLdapManagerConnector().getGroupName( groupId );
    }

    public LdapUser getUser( String username )
        throws NoSuchLdapUserException,
            LdapDAOException
    {
        return this.getLdapManagerConnector().getUser( username );
    }

    public Set<String> getUserRoles( String userId )
        throws LdapDAOException, NoLdapUserRolesFoundException
    {
        return this.getLdapManagerConnector().getUserRoles( userId );
    }

    public SortedSet<LdapUser> getUsers( int userCount )
        throws LdapDAOException
    {
        return this.getLdapManagerConnector().getUsers( userCount );
    }

    public SortedSet<LdapUser> searchUsers( String username )
        throws LdapDAOException
    {
        return this.getLdapManagerConnector().searchUsers( username );
    }

    private LdapConnector getLdapManagerConnector()
        throws LdapDAOException
    {
        if ( this.ldapManagerStrategy == null )
        {
            this.ldapManagerStrategy = new DefaultLdapConnector(
                "test",
                this.ldapUserManager,
                this.ldapGroupManager,
                this.getLdapContextFactory(),
                this.getLdapAuthConfiguration() );
        }
        return this.ldapManagerStrategy;
    }

    protected LdapConfiguration getLdapConfiguration()
    {
        return this.ldapConfiguration;
    }

    protected LdapAuthConfiguration getLdapAuthConfiguration()
    {
        return this.getLdapConfiguration().getLdapAuthConfiguration();
    }

    protected LdapContextFactory getLdapContextFactory()
        throws LdapDAOException
    {
        DefaultLdapContextFactory defaultLdapContextFactory = new DefaultLdapContextFactory();

        if ( this.getLdapConfiguration() == null || this.getLdapConfiguration().readConnectionInfo() == null )
        {
            throw new LdapDAOException( "Ldap connection is not configured." );
        }

        CConnectionInfo connInfo = this.getLdapConfiguration().readConnectionInfo();

        String url;
        try
        {
            url = new LdapURL( connInfo.getProtocol(), connInfo.getHost(), connInfo.getPort(), connInfo.getSearchBase() )
                .toString();
        }
        catch ( MalformedURLException e )
        {
            // log an error, because the user could still log in and fix the config.
            this.logger.error( "LDAP Configuration is Invalid." );
            throw new LdapDAOException( "Invalid LDAP URL: " + e.getMessage() );
        }

        defaultLdapContextFactory.setUsePooling( true );
        defaultLdapContextFactory.setUrl( url );
        defaultLdapContextFactory.setSystemUsername( connInfo.getSystemUsername() );
        defaultLdapContextFactory.setSystemPassword( connInfo.getSystemPassword() );
        defaultLdapContextFactory.setSearchBase( connInfo.getSearchBase() );
        defaultLdapContextFactory.setAuthentication( connInfo.getAuthScheme() );

        return defaultLdapContextFactory;
    }

    public LdapUser authenticateUser( String userId, String password ) throws AuthenticationException
    {
        try
        {
            LdapUser ldapUser = this.getUser( userId );

            String authScheme = this.getLdapConfiguration().readConnectionInfo().getAuthScheme();

            if ( StringUtils.isEmpty( this
                .getLdapConfiguration().readUserAndGroupConfiguration().getUserPasswordAttribute() ) )
            {
                // auth with bind

                this.ldapAuthenticator.authenticateUserWithBind(
                    ldapUser,
                    password,
                    this.getLdapContextFactory(),
                    authScheme );
            }
            else
            {
                // auth by checking password,
                this.ldapAuthenticator.authenticateUserWithPassword( ldapUser, password );
            }

            // everything was successful
            return ldapUser;
        }
        catch ( Exception e )
        {
            if( this.logger.isDebugEnabled())
            {
                this.logger.debug( "Failed to find user: " + userId, e );
            }
        }
        throw new AuthenticationException( "User: " + userId + " could not be authenticated." );
    }

}
