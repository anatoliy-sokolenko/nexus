/*
 * Nexus: Maven Repository Manager
 * Copyright (C) 2008 Sonatype Inc.                                                                                                                          
 * 
 * This file is part of Nexus.                                                                                                                                  
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 */
package org.sonatype.nexus.proxy.maven;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.artifact.GavCalculator;
import org.sonatype.nexus.artifact.M2ArtifactRecognizer;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.PreparedContentLocator;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.util.AlphanumComparator;

/**
 * The default M2Repository.
 * 
 * @author cstamas
 * @plexus.component instantiation-strategy="per-lookup" role="org.sonatype.nexus.proxy.repository.Repository"
 *                   role-hint="maven2"
 */
public class M2Repository
    extends AbstractMavenRepository
{
    private static final Pattern VERSION_FILE_PATTERN = Pattern.compile( "^(.*)-([0-9]{8}.[0-9]{6})-([0-9]+)$" );

    /**
     * The ContentClass.
     * 
     * @plexus.requirement role-hint="maven2"
     */
    private ContentClass contentClass;

    /**
     * The GAV Calculator.
     * 
     * @plexus.requirement role-hint="maven2"
     */
    private GavCalculator gavCalculator;

    public ContentClass getRepositoryContentClass()
    {
        return contentClass;
    }

    public GavCalculator getGavCalculator()
    {
        return gavCalculator;
    }

    /**
     * Should serve by policies.
     * 
     * @param uid the uid
     * @return true, if successful
     */
    public boolean shouldServeByPolicies( RepositoryItemUid uid )
    {
        if ( M2ArtifactRecognizer.isMetadata( uid.getPath() ) )
        {
            if ( M2ArtifactRecognizer.isSnapshot( uid.getPath() ) )
            {
                return RepositoryPolicy.SNAPSHOT.equals( getRepositoryPolicy() );
            }
            else
            {
                // metadatas goes always
                return true;
            }
        }
        // we are using Gav to test the path
        Gav gav = gavCalculator.pathToGav( uid.getPath() );
        if ( gav == null )
        {
            return true;
        }
        else
        {
            if ( gav.isSnapshot() )
            {
                // snapshots goes if enabled
                return RepositoryPolicy.SNAPSHOT.equals( getRepositoryPolicy() );
            }
            else
            {
                return RepositoryPolicy.RELEASE.equals( getRepositoryPolicy() );
            }
        }
    }

    protected AbstractStorageItem doCacheItem( AbstractStorageItem item )
        throws StorageException
    {
        // if the item is file, is M2 repository metadata and this repo is release-only or snapshot-only
        if ( isCleanseRepositoryMetadata() && StorageFileItem.class.isAssignableFrom( item.getClass() )
            && M2ArtifactRecognizer.isMetadata( item.getPath() ) )
        {
            StorageFileItem mdFile = (StorageFileItem) item;
            ByteArrayInputStream backup = null;
            try
            {
                // remote item is not reusable, and we usually cache remote stuff locally
                ByteArrayOutputStream backup1 = new ByteArrayOutputStream();
                IOUtil.copy( mdFile.getInputStream(), backup1 );
                backup = new ByteArrayInputStream( backup1.toByteArray() );

                // Metadata is small, let's do it in memory
                MetadataXpp3Reader metadataReader = new MetadataXpp3Reader();
                InputStreamReader isr = new InputStreamReader( backup );
                Metadata imd = metadataReader.read( isr );

                // and fix it
                imd = cleanseMetadataForRepository( RepositoryPolicy.SNAPSHOT.equals( getRepositoryPolicy() ), imd );

                // serialize and swap the new metadata
                MetadataXpp3Writer metadataWriter = new MetadataXpp3Writer();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter( bos );
                metadataWriter.write( osw, imd );
                ByteArrayInputStream bis = new ByteArrayInputStream( bos.toByteArray() );
                mdFile.setContentLocator( new PreparedContentLocator( bis ) );
            }
            catch ( Exception e )
            {
                getLogger().error( "Exception during repository metadata cleansing.", e );

                if ( backup != null )
                {
                    // get backup and continue operation
                    backup.reset();
                    mdFile.setContentLocator( new PreparedContentLocator( backup ) );
                }
            }
        }
        return super.doCacheItem( item );
    }

    protected boolean isOld( StorageItem item )
    {
        if ( M2ArtifactRecognizer.isMetadata( item.getPath() ) )
        {
            return isOld( getMetadataMaxAge(), item );
        }
        if ( M2ArtifactRecognizer.isSnapshot( item.getPath() ) )
        {
            return isOld( getSnapshotMaxAge(), item );
        }

        // we are using Gav to test the path
        Gav gav = gavCalculator.pathToGav( item.getPath() );

        if ( gav == null )
        {
            // this is not an artifact, it is just any "file"
            return super.isOld( item );
        }
        // it is a release
        return isOld( getReleaseMaxAge(), item );
    }

    @SuppressWarnings( "unchecked" )
    protected Metadata cleanseMetadataForRepository( boolean snapshot, Metadata metadata )
    {
        // remove base versions not belonging here
        List<String> versions = metadata.getVersioning().getVersions();
        for ( Iterator<String> iversion = versions.iterator(); iversion.hasNext(); )
        {
            // if we need snapshots and the version is not snapshot, or
            // if we need releases and the version is snapshot
            if ( ( snapshot && !isSnapshot( iversion.next() ) ) || ( !snapshot && isSnapshot( iversion.next() ) ) )
            {
                iversion.remove();
            }
        }

        metadata.getVersioning().setLatest( getLatestVersion( metadata.getVersioning().getVersions() ) );
        if ( snapshot )
        {
            metadata.getVersioning().setRelease( null );
        }
        else
        {
            metadata.getVersioning().setRelease( metadata.getVersioning().getLatest() );
        }
        return metadata;
    }

    public String getLatestVersion( List<String> versions )
    {
        Collections.sort( versions, new AlphanumComparator() );

        return versions.get( versions.size() - 1 );
    }

    public boolean isSnapshot( String baseVersion )
    {
        return VERSION_FILE_PATTERN.matcher( baseVersion ).matches()
            || baseVersion.endsWith( Artifact.SNAPSHOT_VERSION );
    }

}
