/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.core.ejb.ra;

import javax.ejb.Remote;

import org.cesecore.authorization.AuthorizationDeniedException;
import org.ejbca.core.model.approval.ApprovalException;
import org.ejbca.core.model.approval.WaitingForApprovalException;

/**
 * Remote interface to allow access to local methods from system tests
 * 
 * @version $Id: EndEntityManagementProxySessionRemote.java 28563 2018-03-27 14:16:48Z mikekushner $
 *
 */
@Remote
public interface EndEntityManagementProxySessionRemote {

    /**
     * Decreases (the optional) request counter by 1, until it reaches 0.
     * Returns the new value. If the value is already 0, -1 is returned, but the
     * -1 is not stored in the database. Also sets status of user to generated
     * once the request counter reaches zero.
     * 
     * @param username the unique username.
     * @param status the new status, from 'UserData'.
     * @throws NoSuchEndEntityException if user does not exist
     */
    public int decRequestCounter(String username) throws AuthorizationDeniedException, ApprovalException, WaitingForApprovalException, NoSuchEndEntityException;
    
}
