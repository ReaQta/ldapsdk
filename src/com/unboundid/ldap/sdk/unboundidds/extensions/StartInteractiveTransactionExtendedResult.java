/*
 * Copyright 2008-2015 UnboundID Corp.
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
package com.unboundid.ldap.sdk.unboundidds.extensions;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.unboundid.asn1.ASN1Element;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.asn1.ASN1Sequence;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.sdk.unboundidds.extensions.ExtOpMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;



/**
 * <BLOCKQUOTE>
 *   <B>NOTE:</B>  This class is part of the Commercial Edition of the UnboundID
 *   LDAP SDK for Java.  It is not available for use in applications that
 *   include only the Standard Edition of the LDAP SDK, and is not supported for
 *   use in conjunction with non-UnboundID products.
 * </BLOCKQUOTE>
 * This class implements a data structure for storing the information from an
 * extended result for the start interactive transaction extended request.  It
 * is able to decode a generic extended result to extract the transaction ID and
 * base DNs that it may contain, if the operation was successful.
 * <BR><BR>
 * See the documentation for the
 * {@link StartInteractiveTransactionExtendedRequest} class for an example that
 * demonstrates the use of interactive transactions.
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class StartInteractiveTransactionExtendedResult
       extends ExtendedResult
{
  /**
   * The BER type for the {@code txnID} element of the response.
   */
  private static final byte TYPE_TXN_ID = (byte) 0x80;



  /**
   * The BER type for the {@code baseDNs} element of the response.
   */
  private static final byte TYPE_BASE_DNS = (byte) 0xA1;



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 4010094216900393866L;



  // The transaction ID returned by the server.
  private final ASN1OctetString transactionID;

  // The list of base DNs returned by the server, if any.
  private final List<String> baseDNs;



  /**
   * Creates a new start interactive transaction extended result from the
   * provided extended result.
   *
   * @param  extendedResult  The extended result to be decoded as a start
   *                         interactive transaction extended result.  It must
   *                         not be {@code null}.
   *
   * @throws  LDAPException  If a problem occurs while attempting to decode the
   *                         provided extended result as a start interactive
   *                         transaction extended result.
   */
  public StartInteractiveTransactionExtendedResult(
              final ExtendedResult extendedResult)
         throws LDAPException
  {
    super(extendedResult);

    if (! extendedResult.hasValue())
    {
      transactionID = null;
      baseDNs       = null;
      return;
    }

    final ASN1Sequence valueSequence;
    try
    {
      final ASN1Element valueElement =
           ASN1Element.decode(extendedResult.getValue().getValue());
      valueSequence = ASN1Sequence.decodeAsSequence(valueElement);
    }
    catch (Exception e)
    {
      debugException(e);
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_START_INT_TXN_RESULT_VALUE_NOT_SEQUENCE.get(e.getMessage()), e);
    }

    ASN1OctetString txnID      = null;
    List<String>    baseDNList = null;
    for (final ASN1Element element : valueSequence.elements())
    {
      switch (element.getType())
      {
        case TYPE_TXN_ID:
          txnID = ASN1OctetString.decodeAsOctetString(element);
          break;
        case TYPE_BASE_DNS:
          try
          {
            final ASN1Sequence baseDNsSequence =
                 ASN1Sequence.decodeAsSequence(element);
            final ArrayList<String> dnList =
                 new ArrayList<String>(baseDNsSequence.elements().length);
            for (final ASN1Element e : baseDNsSequence.elements())
            {
              dnList.add(ASN1OctetString.decodeAsOctetString(e).stringValue());
            }
            baseDNList = Collections.unmodifiableList(dnList);
          }
          catch (Exception e)
          {
            debugException(e);
            throw new LDAPException(ResultCode.DECODING_ERROR,
                 ERR_START_INT_TXN_RESULT_BASE_DNS_NOT_SEQUENCE.get(
                      e.getMessage()), e);
          }
          break;
        default:
          throw new LDAPException(ResultCode.DECODING_ERROR,
               ERR_START_INT_TXN_RESULT_INVALID_ELEMENT.get(
                    toHex(element.getType())));
      }
    }

    transactionID = txnID;
    baseDNs       =  baseDNList;

    if (transactionID == null)
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
                              ERR_START_INT_TXN_RESULT_NO_TXN_ID.get());
    }
  }



  /**
   * Creates a new start interactive transaction extended result with the
   * provided information.
   *
   * @param  messageID          The message ID for the LDAP message that is
   *                            associated with this LDAP result.
   * @param  resultCode         The result code from the response.
   * @param  diagnosticMessage  The diagnostic message from the response, if
   *                            available.
   * @param  matchedDN          The matched DN from the response, if available.
   * @param  referralURLs       The set of referral URLs from the response, if
   *                            available.
   * @param  transactionID      The transaction ID for this response, if
   *                            available.
   * @param  baseDNs            The list of base DNs for this response, if
   *                            available.
   * @param  responseControls   The set of controls from the response, if
   *                            available.
   */
  public StartInteractiveTransactionExtendedResult(final int messageID,
              final ResultCode resultCode, final String diagnosticMessage,
              final String matchedDN, final String[] referralURLs,
              final ASN1OctetString transactionID, final List<String> baseDNs,
              final Control[] responseControls)
  {
    super(messageID, resultCode, diagnosticMessage, matchedDN, referralURLs,
          null, encodeValue(transactionID, baseDNs), responseControls);

    this.transactionID = transactionID;

    if (baseDNs == null)
    {
      this.baseDNs = null;
    }
    else
    {
      this.baseDNs =
           Collections.unmodifiableList(new ArrayList<String>(baseDNs));
    }
  }



  /**
   * Encodes the provided information into an ASN.1 octet string suitable for
   * use as the value of this extended result.
   *
   * @param  transactionID  The transaction ID for this response, if available.
   * @param  baseDNs        The list of base DNs for this response, if
   *                        available.
   *
   * @return  The ASN.1 octet string containing the encoded value, or
   *          {@code null} if no value should be used.
   */
  private static ASN1OctetString encodeValue(
                                      final ASN1OctetString transactionID,
                                      final List<String> baseDNs)
  {
    if ((transactionID == null) && (baseDNs == null))
    {
      return null;
    }

    final ArrayList<ASN1Element> elements = new ArrayList<ASN1Element>(2);
    if (transactionID != null)
    {
      elements.add(new ASN1OctetString(TYPE_TXN_ID, transactionID.getValue()));
    }

    if ((baseDNs != null) && (! baseDNs.isEmpty()))
    {
      final ArrayList<ASN1Element> baseDNElements =
           new ArrayList<ASN1Element>(baseDNs.size());
      for (final String s : baseDNs)
      {
        baseDNElements.add(new ASN1OctetString(s));
      }
      elements.add(new ASN1Sequence(TYPE_BASE_DNS, baseDNElements));
    }

    return new ASN1OctetString(new ASN1Sequence(elements).encode());
  }



  /**
   * Retrieves the transaction ID for this start interactive transaction
   * extended result, if available.
   *
   * @return  The transaction ID for this start interactive transaction extended
   *          result, or {@code null} if none was provided.
   */
  public ASN1OctetString getTransactionID()
  {
    return transactionID;
  }



  /**
   * Retrieves the list of base DNs for this start interactive transaction
   * extended result, if available.
   *
   * @return  The list of base DNs for this start interactive transaction
   *          extended result, or {@code null} if no base DN list was provided.
   */
  public List<String> getBaseDNs()
  {
    return baseDNs;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getExtendedResultName()
  {
    return INFO_EXTENDED_RESULT_NAME_START_INTERACTIVE_TXN.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("StartInteractiveTransactionExtendedResult(resultCode=");
    buffer.append(getResultCode());

    final int messageID = getMessageID();
    if (messageID >= 0)
    {
      buffer.append(", messageID=");
      buffer.append(messageID);
    }

    if (transactionID != null)
    {
      buffer.append(", transactionID='");
      buffer.append(transactionID.stringValue());
      buffer.append('\'');
    }

    if (baseDNs != null)
    {
      buffer.append(", baseDNs={");
      for (int i=0; i < baseDNs.size(); i++)
      {
        if (i > 0)
        {
          buffer.append(", ");
        }

        buffer.append('\'');
        buffer.append(baseDNs.get(i));
        buffer.append('\'');
      }
      buffer.append('}');
    }

    final String diagnosticMessage = getDiagnosticMessage();
    if (diagnosticMessage != null)
    {
      buffer.append(", diagnosticMessage='");
      buffer.append(diagnosticMessage);
      buffer.append('\'');
    }

    final String matchedDN = getMatchedDN();
    if (matchedDN != null)
    {
      buffer.append(", matchedDN='");
      buffer.append(matchedDN);
      buffer.append('\'');
    }

    final String[] referralURLs = getReferralURLs();
    if (referralURLs.length > 0)
    {
      buffer.append(", referralURLs={");
      for (int i=0; i < referralURLs.length; i++)
      {
        if (i > 0)
        {
          buffer.append(", ");
        }

        buffer.append('\'');
        buffer.append(referralURLs[i]);
        buffer.append('\'');
      }
      buffer.append('}');
    }

    final Control[] responseControls = getResponseControls();
    if (responseControls.length > 0)
    {
      buffer.append(", responseControls={");
      for (int i=0; i < responseControls.length; i++)
      {
        if (i > 0)
        {
          buffer.append(", ");
        }

        buffer.append(responseControls[i]);
      }
      buffer.append('}');
    }

    buffer.append(')');
  }
}
