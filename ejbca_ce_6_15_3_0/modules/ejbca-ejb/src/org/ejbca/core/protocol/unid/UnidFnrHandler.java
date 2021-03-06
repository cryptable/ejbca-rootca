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

package org.ejbca.core.protocol.unid;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.cesecore.certificates.certificate.request.RequestMessage;
import org.cesecore.util.CeSecoreNameStyle;
import org.ejbca.core.ejb.unidfnr.UnidfnrSessionLocal;
import org.ejbca.core.model.util.EjbLocalHelper;
import org.ejbca.core.protocol.ExtendedUserDataHandler;
import org.ejbca.util.passgen.LettersAndDigitsPasswordGenerator;

/**
 * Adds items to the Unid-Fnr DB.
 * 
 * @version $Id: UnidFnrHandler.java 28616 2018-04-03 11:51:50Z samuellb $
 */
public class UnidFnrHandler implements ExtendedUserDataHandler {
	private static final Logger LOG = Logger.getLogger(UnidFnrHandler.class);
	private static final Pattern onlyDecimalDigits = Pattern.compile("^[0-9]+$");
	private Storage mockStorage;
	private UnidfnrSessionLocal unidfnrSession;

	/**
	 * Used by EJBCA
	 */
	public UnidFnrHandler() {
		super();
		mockStorage = null;
		unidfnrSession = new EjbLocalHelper().getUnidfnrSession();
	}
	/**
	 * Used by unit test.
	 * @param mockStorage Emulates the {@link UnidfnrSessionLocal#stroreUnidFnrData(String, String)} call.
	 */
	public UnidFnrHandler(final Storage mockStorage) {
		super();
		this.mockStorage = mockStorage;
		unidfnrSession = null;
	}
	
	@Override
	public RequestMessage processRequestMessage(RequestMessage req, final String certificateProfileName) throws HandlerException {
	    final X500Name dn = req.getRequestX500Name();
		if (LOG.isDebugEnabled()) {
			LOG.debug(">processRequestMessage:'"+dn+"' and '"+certificateProfileName+"'");
		}
		final String unidPrefix = getPrefixFromCertProfileName(certificateProfileName);
		if ( unidPrefix==null ) {
			return req;
		}
        final List<ASN1ObjectIdentifier> asn1ObjectIdentifiers = Arrays.asList(dn.getAttributeTypes());
		X500NameBuilder nameBuilder = new X500NameBuilder(new CeSecoreNameStyle());
		boolean changed = false;
		for (final ASN1ObjectIdentifier asn1ObjectIdentifier : asn1ObjectIdentifiers) {
			if (asn1ObjectIdentifier.equals(CeSecoreNameStyle.SERIALNUMBER) ) {
			    RDN[] rdns = dn.getRDNs(asn1ObjectIdentifier);
			    String value = rdns[0].getFirst().getValue().toString();
				final String newSerial = storeUnidFrnAndGetNewSerialNr(value, unidPrefix);
				if ( newSerial!=null ) {
					nameBuilder.addRDN(asn1ObjectIdentifier, newSerial);
					changed = true;
				}
			} else {
			    nameBuilder.addRDN(dn.getRDNs(asn1ObjectIdentifier)[0].getFirst());
			}
		}
		if(changed) {
		    req = new RequestMessageSubjectDnAdapter( req, nameBuilder.build());
		}
		return req;
	}
	private static boolean hasOnlyDecimalDigits(String s, int first, int last) {
		return hasOnlyDecimalDigits( s.substring(first, last));
	}
	private static boolean hasOnlyDecimalDigits(String s) {
		return onlyDecimalDigits.matcher(s).matches();
	}
	private String getPrefixFromCertProfileName(String certificateProfileName) {
		if ( certificateProfileName.length()<10 ) {
			return null;
		}
		if ( certificateProfileName.charAt(4)!='-' ) {
			return null;
		}
		if ( certificateProfileName.charAt(9)!='-' ) {
			return null;
		}
		if ( !hasOnlyDecimalDigits(certificateProfileName, 0, 4) ) {
			return null;
		}
		if ( !hasOnlyDecimalDigits(certificateProfileName, 5, 9) ) {
			return null;
		}
		return certificateProfileName.substring(0, 10);
	}
	/**
	 * @param inputSerialNr SN of subject DN in the incoming request
	 * @param unidPrefix Prefix of the unid
	 * @return the serial number of the subject DN of the certificate that will be created. Null if the format of the SN is not fnr-lra.
	 * Returning null means that the handler should not do anything (SN in DN not changed and nothing stored to DB).
	 * @throws HandlerException if unid-fnr can't be stored in DB. This will prevent any certificate to be created.
	 */
	private String storeUnidFrnAndGetNewSerialNr(final String inputSerialNr, final String unidPrefix) throws HandlerException {
	    
        if (unidfnrSession == null && mockStorage == null) {
            throw new HandlerException("Unidfnr session bean is null!");
        }	    
	    
		if ( inputSerialNr.length()!=17 ) {
			return null;
		}
		if ( inputSerialNr.charAt(11)!='-' ) {
			return null;
		}
		final String fnr = inputSerialNr.substring(0, 11);
		if ( !hasOnlyDecimalDigits(fnr) ) {
			return null;
		}
		final String lra = inputSerialNr.substring(12);
		if ( !hasOnlyDecimalDigits(lra) ) {
			return null;
		}
		final String random = new LettersAndDigitsPasswordGenerator().getNewPassword(6, 6);
		final String unid = unidPrefix + lra + random;
		storeUnidFnrData(unid, fnr);
		return unid;
	}
	
	public void storeUnidFnrData(final String unid, final String fnr) throws HandlerException {
	    if (unidfnrSession != null) {
	        unidfnrSession.stroreUnidFnrData(unid, fnr);
	    } else if (mockStorage != null) {
	        mockStorage.storeIt(unid, fnr);
	    } else {
	        throw new IllegalStateException();
	    }
    }
	
	
	/**
	 * To be implemented by unit test.
	 */
	public interface Storage {
		/**
		 * @param unid
		 * @param fnr
		 * @throws HandlerException
		 */
		void storeIt(String unid, String fnr) throws HandlerException;
	}
}
