/*
 * Copyright 2009-2015 UnboundID Corp.
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
import java.util.Iterator;
import java.util.List;

import com.unboundid.asn1.ASN1Boolean;
import com.unboundid.asn1.ASN1Element;
import com.unboundid.asn1.ASN1Enumerated;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.asn1.ASN1Sequence;
import com.unboundid.asn1.ASN1Integer;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.ExtendedRequest;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.sdk.unboundidds.extensions.ExtOpMessages.*;
import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;
import static com.unboundid.util.Validator.*;



/**
 * <BLOCKQUOTE>
 *   <B>NOTE:</B>  This class is part of the Commercial Edition of the UnboundID
 *   LDAP SDK for Java.  It is not available for use in applications that
 *   include only the Standard Edition of the LDAP SDK, and is not supported for
 *   use in conjunction with non-UnboundID products.
 * </BLOCKQUOTE>
 * This class provides an implementation of the stream directory values extended
 * request as used in the UnboundID Directory Server.  It may be used to obtain
 * all entry DNs and/or all all values for one or more attributes for a
 * specified portion of the DIT.  This extended request has an OID of
 * "1.3.6.1.4.1.30221.2.6.6" and the value is encoded as follows:
 * <PRE>
 *   StreamDirectoryValuesRequest ::= SEQUENCE {
 *        baseDN                [0] LDAPDN,
 *        includeDNs            [1] DNSelection OPTIONAL,
 *        attributes            [2] SEQUENCE OF LDAPString OPTIONAL,
 *        valuesPerResponse     [3] INTEGER (1 .. 32767) OPTIONAL,
 *        ... }
 *
 *   DNSelection ::= SEQUENCE {
 *        scope        [0] ENUMERATED {
 *             baseObject             (0),
 *             singleLevel            (1),
 *             wholeSubtree           (2),
 *             subordinateSubtree     (3),
 *             ... }
 *        relative     [1] BOOLEAN DEFAULT TRUE,
 *        ..... }
 * </PRE>
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class StreamDirectoryValuesExtendedRequest
       extends ExtendedRequest
{
  /**
   * The OID (1.3.6.1.4.1.30221.2.6.6) for the get stream directory values
   * extended request.
   */
  public static final String STREAM_DIRECTORY_VALUES_REQUEST_OID =
       "1.3.6.1.4.1.30221.2.6.6";



  /**
   * The BER type for the baseDN element of the stream directory values request
   * sequence.
   */
  private static final byte TYPE_BASE_DN = (byte) 0x80;



  /**
   * The BER type for the includeDNs element of the stream directory values
   * request sequence.
   */
  private static final byte TYPE_INCLUDE_DNS = (byte) 0xA1;



  /**
   * The BER type for the attributes element of the stream directory values
   * request sequence.
   */
  private static final byte TYPE_ATTRIBUTES = (byte) 0xA2;



  /**
   * The BER type for the valuesPerResponse element of the stream directory
   * values request sequence.
   */
  private static final byte TYPE_VALUES_PER_RESPONSE = (byte) 0x83;



  /**
   * The BER type for the scope element of the DNSelection sequence.
   */
  private static final byte TYPE_SCOPE = (byte) 0x80;



  /**
   * The BER type for the relative element of the DNSelection sequence.
   */
  private static final byte TYPE_RELATIVE = (byte) 0x81;



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -6365315263363449596L;



  // Indicates whether to return DN values that are relative to the base DN.
  private final boolean returnRelativeDNs;

  // The maximum number of values to include per response.
  private final int valuesPerResponse;

  // The list of attribute values to be returned.
  private final List<String> attributes;

  // The search scope to use if DN values are to be included.
  private final SearchScope dnScope;

  // The base DN for this stream directory values request.
  private final String baseDN;



  /**
   * Creates a new stream directory values extended request with the provided
   * information.
   *
   * @param  baseDN             The base DN which indicates the portion of the
   *                            DIT to target.  It must not be {@code null}.
   * @param  dnScope            The scope for which to return information about
   *                            entry DNs in the specified portion of the DIT.
   *                            This may be {@code null} if information about
   *                            entry DNs should not be returned.
   * @param  returnRelativeDNs  Indicates whether DNs returned should be
   *                            relative to the base DN rather than full DNs.
   * @param  attributes         The names of the attributes for which to
   *                            retrieve the values.  This may be {@code null}
   *                            or empty if only entry DNs should be retrieved.
   * @param  valuesPerResponse  The maximum number of values to include per
   *                            response.  A value less than or equal to zero
   *                            indicates that the server should choose an
   *                            appropriate value.
   * @param  controls           The set of controls to include in the request.
   *                            It may be {@code null} or empty if no controls
   *                            should be included in the request.
   */
  public StreamDirectoryValuesExtendedRequest(final String baseDN,
              final SearchScope dnScope, final boolean returnRelativeDNs,
              final List<String> attributes, final int valuesPerResponse,
              final Control... controls)
  {
    super(STREAM_DIRECTORY_VALUES_REQUEST_OID,
         encodeValue(baseDN, dnScope, returnRelativeDNs, attributes,
                     valuesPerResponse),
         controls);

    this.baseDN            = baseDN;
    this.dnScope           = dnScope;
    this.returnRelativeDNs = returnRelativeDNs;

    if (attributes == null)
    {
      this.attributes = Collections.emptyList();
    }
    else
    {
      this.attributes = Collections.unmodifiableList(attributes);
    }

    if (valuesPerResponse < 0)
    {
      this.valuesPerResponse = 0;
    }
    else
    {
      this.valuesPerResponse = valuesPerResponse;
    }
  }



  /**
   * Creates a new stream directory values extended request from the provided
   * generic extended request.
   *
   * @param  extendedRequest  The generic extended request to use to create this
   *                          stream directory values extended request.
   *
   * @throws  LDAPException  If a problem occurs while decoding the request.
   */
  public StreamDirectoryValuesExtendedRequest(
              final ExtendedRequest extendedRequest)
         throws LDAPException
  {
    super(extendedRequest);

    final ASN1OctetString value = extendedRequest.getValue();
    if (value == null)
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_STREAM_DIRECTORY_VALUES_REQUEST_NO_VALUE.get());
    }

    boolean                 tmpRelative  = true;
    int                     tmpNumValues = 0;
    final ArrayList<String> tmpAttrs     = new ArrayList<String>();
    SearchScope             tmpScope     = null;
    String                  tmpBaseDN    = null;

    try
    {
      final ASN1Element[] svElements =
           ASN1Element.decode(value.getValue()).decodeAsSequence().elements();
      for (final ASN1Element svElement : svElements)
      {
        switch (svElement.getType())
        {
          case TYPE_BASE_DN:
            tmpBaseDN = svElement.decodeAsOctetString().stringValue();
            break;

          case TYPE_INCLUDE_DNS:
            final ASN1Element[] idElements =
                 svElement.decodeAsSequence().elements();
            for (final ASN1Element idElement : idElements)
            {
              switch (idElement.getType())
              {
                case TYPE_SCOPE:
                  final int scopeValue =
                       idElement.decodeAsEnumerated().intValue();
                  tmpScope = SearchScope.definedValueOf(scopeValue);
                  if (tmpScope == null)
                  {
                    throw new LDAPException(ResultCode.DECODING_ERROR,
                         ERR_STREAM_DIRECTORY_VALUES_REQUEST_INVALID_SCOPE.get(
                              scopeValue));
                  }
                  break;
                case TYPE_RELATIVE:
                  tmpRelative =
                       idElement.decodeAsBoolean().booleanValue();
                  break;
                default:
                  throw new LDAPException(ResultCode.DECODING_ERROR,
                  ERR_STREAM_DIRECTORY_VALUES_REQUEST_INVALID_INCLUDE_DNS_TYPE.
                       get(toHex(idElement.getType())));
              }
            }
            break;

          case TYPE_ATTRIBUTES:
            final ASN1Element[] attrElements =
                 svElement.decodeAsSequence().elements();
            for (final ASN1Element attrElement : attrElements)
            {
              tmpAttrs.add(attrElement.decodeAsOctetString().stringValue());
            }
            break;

          case TYPE_VALUES_PER_RESPONSE:
            tmpNumValues = svElement.decodeAsInteger().intValue();
            if (tmpNumValues < 0)
            {
              tmpNumValues = 0;
            }
            break;

          default:
            throw new LDAPException(ResultCode.DECODING_ERROR,
                 ERR_STREAM_DIRECTORY_VALUES_REQUEST_INVALID_SEQUENCE_TYPE.get(
                      toHex(svElement.getType())));
        }
      }
    }
    catch (LDAPException le)
    {
      throw le;
    }
    catch (Exception e)
    {
      debugException(e);
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_STREAM_DIRECTORY_VALUES_REQUEST_CANNOT_DECODE.get(
                getExceptionMessage(e)), e);
    }

    if (tmpBaseDN == null)
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_STREAM_DIRECTORY_VALUES_REQUEST_NO_BASE_DN.get());
    }

    baseDN            = tmpBaseDN;
    dnScope           = tmpScope;
    returnRelativeDNs = tmpRelative;
    attributes        = Collections.unmodifiableList(tmpAttrs);
    valuesPerResponse = tmpNumValues;
  }



  /**
   * Encodes the provided information into a form suitable for use as the value
   * of this extended request.
   *
   * @param  baseDN             The base DN which indicates the portion of the
   *                            DIT to target.
   * @param  scope              The scope for which to return information about
   *                            entry DNs in the specified portion of the DIT.
   *                            This may be {@code null} if information about
   *                            entry DNs should not be returned.
   * @param  relativeDNs        Indicates whether DNs returned should be
   *                            relative to the base DN rather than full DNs.
   * @param  attributes         The names of the attributes for which to
   *                            retrieve the values.  This may be {@code null}
   *                            or empty if only entry DNs should be retrieved.
   * @param  valuesPerResponse  The maximum number of values to include per
   *                            response.  A value less than or equal to zero
   *                            indicates that the server should choose an
   *                            appropriate value.
   *
   * @return  The ASN.1 octet string containing the encoded value to use for
   *          this extended request.
   */
  private static ASN1OctetString encodeValue(final String baseDN,
       final SearchScope scope, final boolean relativeDNs,
       final List<String> attributes, final int valuesPerResponse)
  {
    ensureNotNull(baseDN);

    final ArrayList<ASN1Element> svElements = new ArrayList<ASN1Element>(4);
    svElements.add(new ASN1OctetString(TYPE_BASE_DN, baseDN));

    if (scope != null)
    {
      final ArrayList<ASN1Element> idElements = new ArrayList<ASN1Element>(2);
      idElements.add(new ASN1Enumerated(TYPE_SCOPE, scope.intValue()));

      if (! relativeDNs)
      {
        idElements.add(new ASN1Boolean(TYPE_RELATIVE, relativeDNs));
      }

      svElements.add(new ASN1Sequence(TYPE_INCLUDE_DNS, idElements));
    }

    if ((attributes != null) && (! attributes.isEmpty()))
    {
      final ArrayList<ASN1Element> attrElements =
           new ArrayList<ASN1Element>(attributes.size());
      for (final String s : attributes)
      {
        attrElements.add(new ASN1OctetString(s));
      }
      svElements.add(new ASN1Sequence(TYPE_ATTRIBUTES, attrElements));
    }

    if (valuesPerResponse > 0)
    {
      svElements.add(new ASN1Integer(TYPE_VALUES_PER_RESPONSE,
                                     valuesPerResponse));
    }

    return new ASN1OctetString(new ASN1Sequence(svElements).encode());
  }



  /**
   * Retrieves the base DN for this request.
   *
   * @return  The base DN for this request.
   */
  public String getBaseDN()
  {
    return baseDN;
  }



  /**
   * Retrieves the scope for entry DNs to be included in intermediate responses.
   *
   * @return  The scope for entry DNs to be included in intermediate responses,
   *          or {@code null} if information about entry DNs should not be
   *          returned.
   */
  public SearchScope getDNScope()
  {
    return dnScope;
  }



  /**
   * Indicates whether entry DN values returned should be relative to the
   * provided base DN.
   *
   * @return  {@code true} if entry DN values returned should be relative to the
   *          provided base DN, or {@code false} if they should be complete DNs.
   */
  public boolean returnRelativeDNs()
  {
    return returnRelativeDNs;
  }



  /**
   * Retrieves the list of names of attributes whose values should be returned
   * to the client.
   *
   * @return  The list of names of attributes whose values should be returned to
   *          the client, or an empty list if only information about entry DNs
   *          should be returned.
   */
  public List<String> getAttributes()
  {
    return attributes;
  }



  /**
   * Retrieves the maximum number of values that should be included in each
   * stream directory values intermediate response.
   *
   * @return  The maximum number of values that should be included in each
   *          stream directory values intermediate response, or 0 if the server
   *          should choose the appropriate number of values per response.
   */
  public int getValuesPerResponse()
  {
    return valuesPerResponse;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StreamDirectoryValuesExtendedRequest duplicate()
  {
    return duplicate(getControls());
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public StreamDirectoryValuesExtendedRequest duplicate(
              final Control[] controls)
  {
    final StreamDirectoryValuesExtendedRequest r =
         new StreamDirectoryValuesExtendedRequest(baseDN, dnScope,
              returnRelativeDNs, attributes, valuesPerResponse, controls);
    r.setResponseTimeoutMillis(getResponseTimeoutMillis(null));
    return r;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getExtendedRequestName()
  {
    return INFO_EXTENDED_REQUEST_NAME_STREAM_DIRECTORY_VALUES.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("StreamDirectoryValuesExtendedRequest(baseDN='");
    buffer.append(baseDN);
    buffer.append('\'');

    if (dnScope != null)
    {
      buffer.append(", scope='");
      buffer.append(dnScope.getName());
      buffer.append("', returnRelativeDNs=");
      buffer.append(returnRelativeDNs);
    }

    buffer.append(", attributes={");
    if (! attributes.isEmpty())
    {
      final Iterator<String> iterator = attributes.iterator();
      while (iterator.hasNext())
      {
        buffer.append('\'');
        buffer.append(iterator.next());
        buffer.append('\'');

        if (iterator.hasNext())
        {
          buffer.append(", ");
        }
      }
    }
    buffer.append('}');

    if (valuesPerResponse > 0)
    {
      buffer.append(", valuesPerResponse=");
      buffer.append(valuesPerResponse);
    }

    final Control[] controls = getControls();
    if (controls.length > 0)
    {
      buffer.append(", controls={");
      for (int i=0; i < controls.length; i++)
      {
        if (i > 0)
        {
          buffer.append(", ");
        }

        buffer.append(controls[i]);
      }
      buffer.append('}');
    }

    buffer.append(')');
  }
}
