/*
 * Copyright 2012-2015 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2015 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package com.unboundid.ldap.sdk.unboundidds;



import com.unboundid.asn1.ASN1Element;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.asn1.ASN1Sequence;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.Debug;
import com.unboundid.util.NotMutable;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;
import com.unboundid.util.Validator;

import static com.unboundid.ldap.sdk.unboundidds.UnboundIDDSMessages.*;



/**
 * <BLOCKQUOTE>
 *   <B>NOTE:</B>  This class is part of the Commercial Edition of the UnboundID
 *   LDAP SDK for Java.  It is not available for use in applications that
 *   include only the Standard Edition of the LDAP SDK, and is not supported for
 *   use in conjunction with non-UnboundID products.
 * </BLOCKQUOTE>
 * This class provides an implementation of the UNBOUNDID-TOTP SASL bind request
 * that contains a point-in-time version of the one-time password and can be
 * used for a single bind but is not suitable for repeated use.  This version of
 * the bind request should be used for authentication in which the one-time
 * password is provided by an external source rather than being generated by
 * the LDAP SDK.
 * <BR><BR>
 * Because the one-time password is provided rather than generated, this version
 * of the bind request is not suitable for cases in which the authentication
 * process may need to be repeated (e.g., for use in a connection pool,
 * following referrals, or if the auto-reconnect feature is enabled), then the
 * reusable variant (supported by the {@link ReusableTOTPBindRequest} class)
 * which generates the one-time password should be used instead.
  */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.NOT_THREADSAFE)
public final class SingleUseTOTPBindRequest
       extends UnboundIDTOTPBindRequest
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -4429898810534930296L;



  // The hard-coded TOTP password to include in the bind request.
  private final String totpPassword;



  /**
   * Creates a new SASL TOTP bind request with the provided information.
   *
   * @param  authenticationID  The authentication identity for the bind request.
   *                           It must not be {@code null}, and must be in the
   *                           form "u:" followed by a username, or "dn:"
   *                           followed by a DN.
   * @param  authorizationID   The authorization identity for the bind request.
   *                           It may be {@code null} if the authorization
   *                           identity should be the same as the authentication
   *                           identity.  If an authorization identity is
   *                           specified, it must be in the form "u:" followed
   *                           by a username, or "dn:" followed by a DN.  The
   *                           value "dn:" may indicate an authorization
   *                           identity of the anonymous user.
   * @param  totpPassword      The hard-coded TOTP password to include in the
   *                           bind request.  It must not be {@code null}.
   * @param  staticPassword    The static password for the target user.  It may
   *                           be {@code null} if only the one-time password is
   *                           to be used for authentication (which may or may
   *                           not be allowed by the server).
   * @param  controls          The set of controls to include in the bind
   *                           request.
   */
  public SingleUseTOTPBindRequest(final String authenticationID,
                                  final String authorizationID,
                                  final String totpPassword,
                                  final String staticPassword,
                                  final Control... controls)
  {
    super(authenticationID, authorizationID, staticPassword, controls);

    Validator.ensureNotNull(totpPassword);
    this.totpPassword = totpPassword;
  }



  /**
   * Creates a new SASL TOTP bind request with the provided information.
   *
   * @param  authenticationID  The authentication identity for the bind request.
   *                           It must not be {@code null}, and must be in the
   *                           form "u:" followed by a username, or "dn:"
   *                           followed by a DN.
   * @param  authorizationID   The authorization identity for the bind request.
   *                           It may be {@code null} if the authorization
   *                           identity should be the same as the authentication
   *                           identity.  If an authorization identity is
   *                           specified, it must be in the form "u:" followed
   *                           by a username, or "dn:" followed by a DN.  The
   *                           value "dn:" may indicate an authorization
   *                           identity of the anonymous user.
   * @param  totpPassword      The hard-coded TOTP password to include in the
   *                           bind request.  It must not be {@code null}.
   * @param  staticPassword    The static password for the target user.  It may
   *                           be {@code null} if only the one-time password is
   *                           to be used for authentication (which may or may
   *                           not be allowed by the server).
   * @param  controls          The set of controls to include in the bind
   *                           request.
   */
  public SingleUseTOTPBindRequest(final String authenticationID,
                                  final String authorizationID,
                                  final String totpPassword,
                                  final byte[] staticPassword,
                                  final Control... controls)
  {
    super(authenticationID, authorizationID, staticPassword, controls);

    Validator.ensureNotNull(totpPassword);
    this.totpPassword = totpPassword;
  }



  /**
   * Creates a new SASL TOTP bind request with the provided information.
   *
   * @param  authenticationID  The authentication identity for the bind request.
   *                           It must not be {@code null}, and must be in the
   *                           form "u:" followed by a username, or "dn:"
   *                           followed by a DN.
   * @param  authorizationID   The authorization identity for the bind request.
   *                           It may be {@code null} if the authorization
   *                           identity should be the same as the authentication
   *                           identity.  If an authorization identity is
   *                           specified, it must be in the form "u:" followed
   *                           by a username, or "dn:" followed by a DN.  The
   *                           value "dn:" may indicate an authorization
   *                           identity of the anonymous user.
   * @param  totpPassword      The hard-coded TOTP password to include in the
   *                           bind request.  It must not be {@code null}.
   * @param  staticPassword    The static password for the target user.  It may
   *                           be {@code null} if only the one-time password is
   *                           to be used for authentication (which may or may
   *                           not be allowed by the server).
   * @param  controls          The set of controls to include in the bind
   *                           request.
   */
  private SingleUseTOTPBindRequest(final String authenticationID,
                                   final String authorizationID,
                                   final String totpPassword,
                                   final ASN1OctetString staticPassword,
                                   final Control... controls)
  {
    super(authenticationID, authorizationID, staticPassword, controls);

    Validator.ensureNotNull(totpPassword);
    this.totpPassword = totpPassword;
  }



  /**
   * Creates a new single-use TOTP bind request from the information contained
   * in the provided encoded SASL credentials.
   *
   * @param  saslCredentials  The encoded SASL credentials to be decoded in
   *                          order to create this single-use TOTP bind request.
   *                          It must not be {@code null}.
   * @param  controls         The set of controls to include in the bind
   *                          request.
   *
   * @return  The single-use TOTP bind request decoded from the provided
   *          credentials.
   *
   * @throws  LDAPException  If the provided credentials are not valid for an
   *                         UNBOUNDID-TOTP bind request.
   */
  public static SingleUseTOTPBindRequest
              decodeSASLCredentials(final ASN1OctetString saslCredentials,
                                    final Control... controls)
         throws LDAPException
  {
    try
    {
      String          authenticationID = null;
      String          authorizationID  = null;
      String          totpPassword     = null;
      ASN1OctetString staticPassword   = null;

      final ASN1Sequence s =
           ASN1Sequence.decodeAsSequence(saslCredentials.getValue());
      for (final ASN1Element e : s.elements())
      {
        switch (e.getType())
        {
          case TYPE_AUTHENTICATION_ID:
            authenticationID = e.decodeAsOctetString().stringValue();
            break;
          case TYPE_AUTHORIZATION_ID:
            authorizationID = e.decodeAsOctetString().stringValue();
            break;
          case TYPE_TOTP_PASSWORD:
            totpPassword = e.decodeAsOctetString().stringValue();
            break;
          case TYPE_STATIC_PASSWORD:
            staticPassword = e.decodeAsOctetString();
            break;
          default:
            throw new LDAPException(ResultCode.DECODING_ERROR,
                 ERR_SINGLE_USE_TOTP_DECODE_INVALID_ELEMENT_TYPE.get(
                      StaticUtils.toHex(e.getType())));
        }
      }

      if (authenticationID == null)
      {
        throw new LDAPException(ResultCode.DECODING_ERROR,
             ERR_SINGLE_USE_TOTP_DECODE_MISSING_AUTHN_ID.get());
      }

      if (totpPassword == null)
      {
        throw new LDAPException(ResultCode.DECODING_ERROR,
             ERR_SINGLE_USE_TOTP_DECODE_MISSING_TOTP_PW.get());
      }

      return new SingleUseTOTPBindRequest(authenticationID, authorizationID,
           totpPassword, staticPassword, controls);
    }
    catch (final Exception e)
    {
      Debug.debugException(e);
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_SINGLE_USE_TOTP_DECODE_ERROR.get(
                StaticUtils.getExceptionMessage(e)),
           e);
    }
  }



  /**
   * Retrieves the hard-coded TOTP password to include in the bind request.
   *
   * @return  The hard-coded TOTP password to include in the bind request.
   */
  public String getTOTPPassword()
  {
    return totpPassword;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected ASN1OctetString getSASLCredentials()
  {
    return encodeCredentials(getAuthenticationID(), getAuthorizationID(),
         totpPassword, getStaticPassword());
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public SingleUseTOTPBindRequest getRebindRequest(final String host,
                                                   final int port)
  {
    // Automatic rebinding is not supported for single-use TOTP binds.
    return null;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public SingleUseTOTPBindRequest duplicate()
  {
    return duplicate(getControls());
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public SingleUseTOTPBindRequest duplicate(final Control[] controls)
  {
    final SingleUseTOTPBindRequest bindRequest =
         new SingleUseTOTPBindRequest(getAuthenticationID(),
              getAuthorizationID(), totpPassword, getStaticPassword(),
              controls);
    bindRequest.setResponseTimeoutMillis(getResponseTimeoutMillis(null));
    return bindRequest;
  }
}
