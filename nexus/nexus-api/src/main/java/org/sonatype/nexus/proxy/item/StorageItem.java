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
package org.sonatype.nexus.proxy.item;

import java.util.Map;

import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.ResourceStoreRequest;

/**
 * The Interface StorageItem, a top of the item abstraction.
 */
public interface StorageItem
{
    /**
     * The request.
     * 
     * @return
     */
    ResourceStoreRequest getResourceStoreRequest();

    /**
     * Gets the repository item uid from where originates item.
     * 
     * @return the repository item uid
     */
    RepositoryItemUid getRepositoryItemUid();

    /**
     * Set the repository item uid.
     * 
     * @param repositoryItemUid
     */
    void setRepositoryItemUid( RepositoryItemUid repositoryItemUid );

    /**
     * Gets the repository id from where originates item.
     * 
     * @return the repository id
     */
    String getRepositoryId();

    /**
     * Gets the creation time.
     * 
     * @return the created
     */
    long getCreated();

    /**
     * Gets the modification time.
     * 
     * @return the modified
     */
    long getModified();

    /**
     * Gets the stored locally time.
     * 
     * @return the stored locally
     */
    long getStoredLocally();

    /**
     * Sets the stored locally time.
     * 
     * @return the stored locally
     */
    void setStoredLocally( long ts );

    /**
     * Gets the remote checked.
     * 
     * @return the remote checked
     */
    long getRemoteChecked();

    /**
     * Sets the remote checked.
     * 
     * @return the remote checked
     */
    void setRemoteChecked( long ts );

    /**
     * Gets the last requested.
     * 
     * @return time when it was last served
     */
    long getLastRequested();

    /**
     * Sets the last requested.
     * 
     * @return time when it was last served
     */
    void setLastRequested( long ts );

    /**
     * Checks if is virtual.
     * 
     * @return true, if is virtual
     */
    boolean isVirtual();

    /**
     * Checks if is readable.
     * 
     * @return true, if is readable
     */
    boolean isReadable();

    /**
     * Checks if is writable.
     * 
     * @return true, if is writable
     */
    boolean isWritable();

    /**
     * Returns true if the item is expired.
     */
    boolean isExpired();

    /**
     * Sets if the item is expired.
     */
    void setExpired( boolean expired );

    /**
     * Gets the path.
     * 
     * @return the path
     */
    String getPath();

    /**
     * Gets the name.
     * 
     * @return the name
     */
    String getName();

    /**
     * Gets the parent path.
     * 
     * @return the parent path
     */
    String getParentPath();

    /**
     * Gets the remote url.
     * 
     * @return the remote url
     */
    String getRemoteUrl();

    /**
     * Gets the user attributes. These are saved and persisted.
     * 
     * @return the attributes
     */
    Map<String, String> getAttributes();

    /**
     * Gets the item context. It is living only during item processing, it is not stored.
     * 
     * @return the attributes
     */
    RequestContext getItemContext();

    /**
     * Overlay.
     * 
     * @param item the item
     */
    void overlay( StorageItem item );

    /**
     * Returns the generation of the attributes. For Nexus internal use only!
     * 
     * @return
     */
    int getGeneration();

    /**
     * Increments the generation of the attributes. For Nexus internal use only!
     * 
     * @return
     */
    void incrementGeneration();
}
