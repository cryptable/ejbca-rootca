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

package org.ejbca.ui.web.pub;

import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.cesecore.certificates.ca.CaSessionLocal;
import org.cesecore.certificates.certificate.CertificateStoreSessionLocal;
import org.cesecore.certificates.certificateprofile.CertificateProfileSessionLocal;
import org.cesecore.configuration.GlobalConfigurationSessionLocal;
import org.cesecore.util.CryptoProviderTools;
import org.ejbca.core.ejb.ca.auth.EndEntityAuthenticationSessionLocal;
import org.ejbca.core.ejb.ca.sign.SignSessionLocal;
import org.ejbca.core.ejb.ra.EndEntityAccessSessionLocal;
import org.ejbca.core.ejb.ra.EndEntityManagementSessionLocal;
import org.ejbca.core.ejb.ra.KeyStoreCreateSessionLocal;
import org.ejbca.core.ejb.ra.raadmin.EndEntityProfileSessionLocal;
import org.ejbca.core.model.InternalEjbcaResources;

/**
 * Servlet used to install a private key with a corresponding certificate in a browser. A new
 * certificate is installed in the browser in following steps:<br>
 * 1. The key pair is generated by the browser. <br>
 * 2. The public part is sent to the servlet in a POST together with user info ("pkcs10|keygen",
 * "inst", "user", "password"). For internet explorer the public key is sent as a PKCS10
 * certificate request. <br>
 * 3. The new certificate is created by calling the RSASignSession session bean. <br>
 * 4. A page containing the new certificate and a script that installs it is returned to the
 * browser. <br>
 * 
 * <p></p>
 * 
 * <p>
 * The following initiation parameters are needed by this servlet: <br>
 * "responseTemplate" file that defines the response to the user (IE). It should have one line
 * with the text "cert =". This line is replaced with the new certificate. "keyStorePass".
 * Password needed to load the key-store. If this parameter is none existing it is assumed that no
 * password is needed. The path could be absolute or relative.<br>
 * </p>
 *
 * @version $Id: CertReqServlet.java 25830 2017-05-10 13:36:45Z mikekushner $
 */
public class CertReqServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(CertReqServlet.class);
    private static final InternalEjbcaResources intres = InternalEjbcaResources.getInstance();

    // This injection has been verified on JBoss
	@EJB
	private EndEntityAuthenticationSessionLocal authenticationSession;
	@EJB
	private CaSessionLocal caSession;
	@EJB
	private CertificateProfileSessionLocal certificateProfileSession;
	@EJB
	private CertificateStoreSessionLocal certificateStoreSession;
	@EJB
	private EndEntityAccessSessionLocal endEntityAccessSession;
	@EJB
	private EndEntityProfileSessionLocal endEntityProfileSession;
	@EJB
	private KeyStoreCreateSessionLocal keyStoreCreateSession;
	@EJB
	private SignSessionLocal signSession;
	@EJB
	private EndEntityManagementSessionLocal endEntityManagementSession;
	@EJB
	private GlobalConfigurationSessionLocal globalConfigurationSession;

    /**
     * Servlet init
     * 
     * @param config
     *            servlet configuration
     * 
     * @throws ServletException
     *             on error
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // Install BouncyCastle provider
        CryptoProviderTools.installBCProvider();
    }

    /**
     * Handles HTTP POST
     * 
     * @param request
     *            servlet request
     * @param response
     *            servlet response
     * 
     * @throws IOException
     *             input/output error
     * @throws ServletException
     *             on error
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        new RequestInstance(getServletContext(), getServletConfig(), endEntityAccessSession, caSession,
                certificateProfileSession, endEntityProfileSession, keyStoreCreateSession, signSession, endEntityManagementSession,
                globalConfigurationSession).doPost(request, response);
    }

    /**
     * Handles HTTP GET
     * 
     * @param request
     *            servlet request
     * @param response
     *            servlet response
     * 
     * @throws IOException
     *             input/output error
     * @throws ServletException
     *             on error
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        log.trace(">doGet()");
        response.setHeader("Allow", "POST");

        ServletDebug debug = new ServletDebug(request, response);
        String iMsg = intres.getLocalizedMessage("certreq.postonly");
        debug.print(iMsg);
        debug.printDebugInfo();
        log.trace("<doGet()");
    }
}
