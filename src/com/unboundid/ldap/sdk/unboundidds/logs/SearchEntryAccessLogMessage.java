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
package com.unboundid.ldap.sdk.unboundidds.logs;



import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;



/**
 * <BLOCKQUOTE>
 *   <B>NOTE:</B>  This class is part of the Commercial Edition of the UnboundID
 *   LDAP SDK for Java.  It is not available for use in applications that
 *   include only the Standard Edition of the LDAP SDK, and is not supported for
 *   use in conjunction with non-UnboundID products.
 * </BLOCKQUOTE>
 * This class provides a data structure that holds information about a log
 * message that may appear in the Directory Server access log about a search
 * result entry returned to a client.
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class SearchEntryAccessLogMessage
       extends SearchRequestAccessLogMessage
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 6423635071693560277L;



  // The names of the attributes included in the entry that was returned.
  private final List<String> attributesReturned;

  // The list of response control OIDs for the operation.
  private final List<String> responseControlOIDs;

  // The DN of the entry returned.
  private final String dn;



  /**
   * Creates a new search result entry access log message from the provided
   * message string.
   *
   * @param  s  The string to be parsed as a search result entry access log
   *            message.
   *
   * @throws  LogException  If the provided string cannot be parsed as a valid
   *                        log message.
   */
  public SearchEntryAccessLogMessage(final String s)
         throws LogException
  {
    this(new LogMessage(s));
  }



  /**
   * Creates a new search result entry access log message from the provided log
   * message.
   *
   * @param  m  The log message to be parsed as a search entry access log
   *            message.
   */
  public SearchEntryAccessLogMessage(final LogMessage m)
  {
    super(m);

    dn = getNamedValue("dn");

    final String controlStr = getNamedValue("responseControls");
    if (controlStr == null)
    {
      responseControlOIDs = Collections.emptyList();
    }
    else
    {
      final LinkedList<String> controlList = new LinkedList<String>();
      final StringTokenizer t = new StringTokenizer(controlStr, ",");
      while (t.hasMoreTokens())
      {
        controlList.add(t.nextToken());
      }
      responseControlOIDs = Collections.unmodifiableList(controlList);
    }

    final String attrs = getNamedValue("attrsReturned");
    if (attrs == null)
    {
      attributesReturned = null;
    }
    else
    {
      final ArrayList<String> l = new ArrayList<String>(10);
      final StringTokenizer tokenizer = new StringTokenizer(attrs, ",");
      while (tokenizer.hasMoreTokens())
      {
        l.add(tokenizer.nextToken());
      }

      attributesReturned = Collections.unmodifiableList(l);
    }
  }



  /**
   * Retrieves the DN of the entry returned to the client.
   *
   * @return  The DN of the entry returned to the client, or {@code null} if it
   *          is not included in the log message.
   */
  public String getDN()
  {
    return dn;
  }



  /**
   * Retrieves the names of the attributes included in the entry that was
   * returned.
   *
   * @return  The names of the attributes included in the entry that was
   *          returned, or {@code null} if it is not included in the log
   *          message.
   */
  public List<String> getAttributesReturned()
  {
    return attributesReturned;
  }



  /**
   * Retrieves the OIDs of any response controls contained in the log message.
   *
   * @return  The OIDs of any response controls contained in the log message, or
   *          an empty list if it is not included in the log message.
   */
  public List<String> getResponseControlOIDs()
  {
    return responseControlOIDs;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public AccessLogMessageType getMessageType()
  {
    return AccessLogMessageType.ENTRY;
  }
}
