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
package org.ejbca.core.ejb.keyrecovery;

import javax.ejb.Remote;

/**
 * @version $Id: KeyRecoveryProxySessionRemote.java 26210 2017-08-03 10:12:32Z samuellb $
 */
@Remote
public interface KeyRecoveryProxySessionRemote extends KeyRecoverySessionLocal {

}
