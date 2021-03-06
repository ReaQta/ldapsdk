/*
 * Copyright 2014-2015 UnboundID Corp.
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
package com.unboundid.ldap.sdk.unboundidds.controls;



import java.util.ArrayList;

import com.unboundid.asn1.ASN1Boolean;
import com.unboundid.asn1.ASN1Element;
import com.unboundid.asn1.ASN1Integer;
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

import static com.unboundid.ldap.sdk.unboundidds.controls.ControlMessages.*;



/**
 * <BLOCKQUOTE>
 *   <B>NOTE:</B>  This class is part of the Commercial Edition of the UnboundID
 *   LDAP SDK for Java.  It is not available for use in applications that
 *   include only the Standard Edition of the LDAP SDK, and is not supported for
 *   use in conjunction with non-UnboundID products.
 * </BLOCKQUOTE>
 * This class provides a request control which may be included in a search
 * request to indicate that the server should provide the number of entries that
 * match the search criteria.  The count will be included in the search result
 * done message, and all search result entries will be suppressed.
 * <BR><BR>
 * Whenever possible, the server will use index information to quickly identify
 * entries matching the criteria of the associated search request.  However, if
 * the count is only determined using index information, then that count may
 * include entries that would not actually be returned to the client in the
 * course of processing that search (e.g., because the client doesn't have
 * permission to access the entry, or because it is a special "operational"
 * entry like an LDAP subentry, replication conflict entry, or soft-deleted
 * entry).  Indicating that the server should always examine candidate entries
 * will increase the length of time to obtain the matching entry count, but will
 * ensure that the count will not include entries that would not otherwise be
 * returned by that search.
 * <BR><BR>
 * Also note that this control is not compatible for use with other controls
 * that may cause only a subset of entries to be returned, including the simple
 * paged results control and the virtual list view control.  It is also not
 * compatible for use with other controls that may cause the server to return
 * more entries than those that match the search criteria, like the LDAP join
 * control.
 * <BR><BR>
 * The OID for a matching entry count request control is
 * "1.3.6.1.4.1.30221.2.5.36", and it may have a criticality of either
 * {@code true} or {@code false}.  It must include a value with the following
 * encoding:
 * <PRE>
 *   MatchingEntryCountRequest ::= SEQUENCE {
 *        maxCandidatesToExamine       [0] INTEGER (0 .. MAX) DEFAULT 0,
 *        alwaysExamineCandidates      [1] BOOLEAN DEFAULT FALSE,
 *        processSearchIfUnindexed     [2] BOOLEAN DEFAULT FALSE,
 *        includeDebugInfo             [3] BOOLEAN DEFAULT FALSE,
 *        ... }
 * </PRE>
 *
 * @see  MatchingEntryCountResponseControl
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class MatchingEntryCountRequestControl
       extends Control
{
  /**
   * The OID (1.3.6.1.4.1.30221.2.5.36) for the matching entry count request
   * control.
   */
  public static final String MATCHING_ENTRY_COUNT_REQUEST_OID =
       "1.3.6.1.4.1.30221.2.5.36";



  /**
   * The BER type for the element that specifies the maximum number of candidate
   * entries to examine.
   */
  private static final byte TYPE_MAX_CANDIDATES_TO_EXAMINE = (byte) 0x80;



  /**
   * The BER type for the element that indicates whether always examine
   * candidate entries to determine whether they would actually be returned to
   * the client.
   */
  private static final byte TYPE_ALWAYS_EXAMINE_CANDIDATES = (byte) 0x81;



  /**
   * The BER type for the element that indicates whether to process an unindexed
   * search to determine the number of matching entries.
   */
  private static final byte TYPE_PROCESS_SEARCH_IF_UNINDEXED = (byte) 0x82;



  /**
   * The BER type for the element that indicates whether to include debug
   * information in the response.
   */
  private static final byte TYPE_INCLUDE_DEBUG_INFO = (byte) 0x83;



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -6077150575658563941L;



  // Indicates whether the server should internally retrieve and examine
  // candidate entries to determine whether they would actually be returned to
  // the client.
  private final boolean alwaysExamineCandidates;

  // Indicates whether to include debug information in the response control.
  private final boolean includeDebugInfo;

  // Indicates whether the server should attempt to actually iterate through the
  // entries in the backend in order to obtain the count if the search criteria
  // is not indexed.
  private final boolean processSearchIfUnindexed;

  // The maximum number of candidate entries that should be examined if it is
  // not possible to obtain an exact count using only information contained in
  // the server indexes.
  private final int maxCandidatesToExamine;



  /**
   * Creates a new matching entry count request control with the default
   * settings.  The control will be critical, no candidate entries will be
   * examined, and the search will not be processed if it is unindexed.
   */
  public MatchingEntryCountRequestControl()
  {
    this(true, 0, false, false, false);
  }



  /**
   * Creates a new matching entry count request control with the provided
   * information.
   *
   * @param  isCritical                Indicates whether this control should be
   *                                   critical.
   * @param  maxCandidatesToExamine    The maximum number of candidate entries
   *                                   that the server should retrieve and
   *                                   examine to determine whether they
   *                                   actually match the search criteria.  If
   *                                   the search is partially indexed and the
   *                                   total number of candidate entries is less
   *                                   than or equal to this value, then these
   *                                   candidate entries will be examined to
   *                                   determine which of them match the search
   *                                   criteria so that an accurate count can
   *                                   be determined.  If the search is fully
   *                                   indexed such that the all candidate
   *                                   entries are known to match the search
   *                                   criteria, then the server may still
   *                                   examine each of these entries if the
   *                                   number of candidates is less than
   *                                   {@code maxCandidatesToExamine} and
   *                                   {@code alwaysExamineCandidates} is true
   *                                   in order to allow the entry count that
   *                                   is returned to be restricted to only
   *                                   those entries that would actually be
   *                                   returned to the client.  This will be
   *                                   ignored for searches that are completely
   *                                   unindexed.
   *                                   <BR><BR>
   *                                   The value for this argument must be
   *                                   greater than or equal to zero.  If it
   *                                   is zero, then the server will not
   *                                   examine any entries, so a
   *                                   partially-indexed search will only be
   *                                   able to return a count that is an upper
   *                                   bound, and a fully-indexed search will
   *                                   only be able to return an unexamined
   *                                   exact count.  If there should be no bound
   *                                   on the number of entries to retrieve,
   *                                   then a value of {@code Integer.MAX_VALUE}
   *                                   may be specified.
   * @param  alwaysExamineCandidates   Indicates whether the server should
   *                                   always examine candidate entries to
   *                                   determine whether they would actually
   *                                   be returned to the client in a normal
   *                                   search.  This will only be used for
   *                                   fully-indexed searches in which the
   *                                   set of matching entries is known.  If the
   *                                   value is {@code true} and the number of
   *                                   candidates is smaller than
   *                                   {@code maxCandidatesToExamine}, then each
   *                                   matching entry will be internally
   *                                   retrieved and examined to determine
   *                                   whether it would be returned to the
   *                                   client based on the details of the search
   *                                   request (e.g., whether the requester has
   *                                   permission to access the entry, whether
   *                                   it's an LDAP subentry, replication
   *                                   conflict entry, soft-deleted entry, or
   *                                   other type of entry that is normally
   *                                   hidden) so that an exact count can be
   *                                   returned.  If this is {@code false} or
   *                                   the number of candidates exceeds
   *                                   {@code maxCandidatesToExamine}, then the
   *                                   server will only be able to return an
   *                                   unexamined count which may include
   *                                   entries that match the search criteria
   *                                   but that would not normally be returned
   *                                   to the requester.
   * @param  processSearchIfUnindexed  Indicates whether the server should
   *                                   attempt to determine the number of
   *                                   matching entries if the search criteria
   *                                   is completely unindexed.  If this is
   *                                   {@code true} and the requester has the
   *                                   unindexed-search privilege, then the
   *                                   server will iterate through all entries
   *                                   in the scope (which may take a very long
   *                                   time to complete) in order to to
   *                                   determine which of them match the search
   *                                   criteria so that it can return an
   *                                   accurate count.  If this is
   *                                   {@code false} or the requester does not
   *                                   have the unindexed-search privilege, then
   *                                   the server will not spend any time
   *                                   attempting to determine the number of
   *                                   matching entries and will instead return
   *                                   a matching entry count response control
   *                                   indicating that the entry count is
   *                                   unknown.
   * @param  includeDebugInfo          Indicates whether the server should
   *                                   include debug information in the response
   *                                   that may help better understand how it
   *                                   arrived at the result.  If any debug
   *                                   information is returned, it will be in
   *                                   the form of human-readable text that is
   *                                   not intended to be machine-parsable.
   */
  public MatchingEntryCountRequestControl(final boolean isCritical,
              final int maxCandidatesToExamine,
              final boolean alwaysExamineCandidates,
              final boolean processSearchIfUnindexed,
              final boolean includeDebugInfo)
  {
    super(MATCHING_ENTRY_COUNT_REQUEST_OID, isCritical,
         encodeValue(maxCandidatesToExamine, alwaysExamineCandidates,
              processSearchIfUnindexed, includeDebugInfo));

    Validator.ensureTrue(maxCandidatesToExamine >= 0);

    this.maxCandidatesToExamine   = maxCandidatesToExamine;
    this.alwaysExamineCandidates  = alwaysExamineCandidates;
    this.processSearchIfUnindexed = processSearchIfUnindexed;
    this.includeDebugInfo         = includeDebugInfo;
  }



  /**
   * Creates a new matching entry count request control that is decoded from the
   * provided generic control.
   *
   * @param  control  The control to decode as a matching entry count request
   *                  control.
   *
   * @throws  LDAPException  If the provided control cannot be decoded as a
   *                         matching entry count request control.
   */
  public MatchingEntryCountRequestControl(final Control control)
         throws LDAPException
  {
    super(control);

    final ASN1OctetString value = control.getValue();
    if (value == null)
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_MATCHING_ENTRY_COUNT_REQUEST_MISSING_VALUE.get());
    }

    try
    {
      boolean alwaysExamine    = false;
      boolean debug            = false;
      boolean processUnindexed = false;
      int     maxCandidates    = 0;
      final ASN1Element[] elements =
           ASN1Sequence.decodeAsSequence(value.getValue()).elements();
      for (final ASN1Element e : elements)
      {
        switch (e.getType())
        {
          case TYPE_MAX_CANDIDATES_TO_EXAMINE:
            maxCandidates = ASN1Integer.decodeAsInteger(e).intValue();
            if (maxCandidates < 0)
            {
              throw new LDAPException(ResultCode.DECODING_ERROR,
                   ERR_MATCHING_ENTRY_COUNT_REQUEST_INVALID_MAX.get());
            }
            break;

          case TYPE_ALWAYS_EXAMINE_CANDIDATES:
            alwaysExamine = ASN1Boolean.decodeAsBoolean(e).booleanValue();
            break;

          case TYPE_PROCESS_SEARCH_IF_UNINDEXED:
            processUnindexed = ASN1Boolean.decodeAsBoolean(e).booleanValue();
            break;

          case TYPE_INCLUDE_DEBUG_INFO:
            debug = ASN1Boolean.decodeAsBoolean(e).booleanValue();
            break;

          default:
            throw new LDAPException(ResultCode.DECODING_ERROR,
                 ERR_MATCHING_ENTRY_COUNT_REQUEST_INVALID_ELEMENT_TYPE.get(
                      StaticUtils.toHex(e.getType())));
        }
      }

      maxCandidatesToExamine   = maxCandidates;
      alwaysExamineCandidates  = alwaysExamine;
      processSearchIfUnindexed = processUnindexed;
      includeDebugInfo         = debug;
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      throw le;
    }
    catch (final Exception e)
    {
      Debug.debugException(e);
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_MATCHING_ENTRY_COUNT_REQUEST_CANNOT_DECODE.get(
                StaticUtils.getExceptionMessage(e)),
           e);
    }
  }



  /**
   * Encodes the provided information into an ASN.1 octet string suitable for
   * use as the control value.
   *
   * @param  maxCandidatesToExamine    The maximum number of candidate entries
   *                                   that the server should retrieve and
   *                                   examine to determine whether they
   *                                   actually match the search criteria.
   * @param  alwaysExamineCandidates   Indicates whether the server should
   *                                   always examine candidate entries to
   *                                   determine whether they would actually
   *                                   be returned to the client in a normal
   *                                   search with the same criteria.
   * @param  processSearchIfUnindexed  Indicates whether the server should
   *                                   attempt to determine the number of
   *                                   matching entries if the search criteria
   *                                   is completely unindexed.
   * @param  includeDebugInfo          Indicates whether the server should
   *                                   include debug information in the response
   *                                   that may help better understand how it
   *                                   arrived at the result.
   *
   * @return  The ASN.1 octet string containing the encoded control value.
   */
  private static ASN1OctetString encodeValue(
                                      final int maxCandidatesToExamine,
                                      final boolean alwaysExamineCandidates,
                                      final boolean processSearchIfUnindexed,
                                      final boolean includeDebugInfo)
  {
    final ArrayList<ASN1Element> elements = new ArrayList<ASN1Element>(4);

    if (maxCandidatesToExamine > 0)
    {
      elements.add(new ASN1Integer(TYPE_MAX_CANDIDATES_TO_EXAMINE,
           maxCandidatesToExamine));
    }

    if (alwaysExamineCandidates)
    {
      elements.add(new ASN1Boolean(TYPE_ALWAYS_EXAMINE_CANDIDATES, true));
    }

    if (processSearchIfUnindexed)
    {
      elements.add(new ASN1Boolean(TYPE_PROCESS_SEARCH_IF_UNINDEXED, true));
    }

    if (includeDebugInfo)
    {
      elements.add(new ASN1Boolean(TYPE_INCLUDE_DEBUG_INFO, true));
    }

    return new ASN1OctetString(new ASN1Sequence(elements).encode());
  }



  /**
   * Retrieves the maximum number of candidate entries that should be examined
   * in order to determine accurate count of the number of matching entries.
   * <BR><BR>
   * For a fully-indexed search, this property will only be used if
   * {@link #alwaysExamineCandidates} is true.  If the number of candidate
   * entries identified is less than the maximum number of candidates to
   * examine, then the server will return an {@code EXAMINED_COUNT} result that
   * indicates the number of entries matching the criteria that would actually
   * be returned in a normal search with the same criteria.  If the number of
   * candidate entries exceeds the maximum number of candidates to examine, then
   * the server will return an {@code UNEXAMINED_COUNT} result that indicates
   * the number of entries matching the search criteria but that may include
   * entries that would not actually be returned to the client.
   * <BR><BR>
   * For a partially-indexed search, if the upper bound on the number of
   * candidates is less than or equal to the maximum number of candidates to
   * examine, then the server will internally retrieve and examine each of those
   * candidates to determine which of them match the search criteria and would
   * actually be returned to the client, and will then return an
   * {@code EXAMINED_COUNT} result with that count.  If the upper bound on the
   * number of candidates is greater than the maximum number of candidates to
   * examine, then the server will return an {@code UPPER_BOUND} result to
   * indicate that the exact count is not known but an upper bound is available.
   *
   * @return  The maximum number of candidate entries to examine in order to
   *          determine an accurate count of the number of matching entries.
   */
  public int getMaxCandidatesToExamine()
  {
    return maxCandidatesToExamine;
  }



  /**
   * Indicates whether the server should always examine candidate entries in
   * fully-indexed searches to determine whether they would actually be returned
   * to the client in a normal search with the same criteria.
   *
   * @return  {@code true} if the server should attempt to internally retrieve
   *          and examine matching entries to determine whether they would
   *          normally be returned to the client (i.e.., that the client has
   *          permission to access the entry and that it is not a
   *          normally-hidden entry like an LDAP subentry, a replication
   *          conflict entry, or a soft-deleted entry), or {@code false} if the
   *          server should return an unverified count.
   */
  public boolean alwaysExamineCandidates()
  {
    return alwaysExamineCandidates;
  }



  /**
   * Indicates whether the server should internally retrieve and examine all
   * entries within the search scope in order to obtain an exact matching entry
   * count for an unindexed search.  Note that this value will not be considered
   * for completely-indexed or partially-indexed searches, nor for searches in
   * which matching entries should be returned.
   *
   * @return  {@code true} if the server should internally retrieve and examine
   *          all entries within the search scope in order to obtain an exact
   *          matching entry count for an unindexed search, or {@code false} if
   *          not.
   */
  public boolean processSearchIfUnindexed()
  {
    return processSearchIfUnindexed;
  }



  /**
   * Indicates whether the server should include debug information in the
   * response control that provides additional information about how the server
   * arrived at the result.  If debug information is to be provided, it will be
   * in a human-readable rather than machine-parsable form.
   *
   * @return  {@code true} if the server should include debug information in
   *          the response control, or {@code false} if not.
   */
  public boolean includeDebugInfo()
  {
    return includeDebugInfo;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getControlName()
  {
    return INFO_CONTROL_NAME_MATCHING_ENTRY_COUNT_REQUEST.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("MatchingEntryCountRequestControl(isCritical=");
    buffer.append(isCritical());
    buffer.append(", maxCandidatesToExamine=");
    buffer.append(maxCandidatesToExamine);
    buffer.append(", alwaysExamineCandidates=");
    buffer.append(alwaysExamineCandidates);
    buffer.append(", processSearchIfUnindexed=");
    buffer.append(processSearchIfUnindexed);
    buffer.append(", includeDebugInfo=");
    buffer.append(includeDebugInfo);
    buffer.append(')');
  }
}
