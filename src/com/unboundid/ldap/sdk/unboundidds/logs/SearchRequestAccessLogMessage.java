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



import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.NotExtensible;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.util.Debug.*;



/**
 * <BLOCKQUOTE>
 *   <B>NOTE:</B>  This class is part of the Commercial Edition of the UnboundID
 *   LDAP SDK for Java.  It is not available for use in applications that
 *   include only the Standard Edition of the LDAP SDK, and is not supported for
 *   use in conjunction with non-UnboundID products.
 * </BLOCKQUOTE>
 * This class provides a data structure that holds information about a log
 * message that may appear in the Directory Server access log about a search
 * request received from a client.
 */
@NotExtensible()
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public class SearchRequestAccessLogMessage
       extends OperationRequestAccessLogMessage
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -6751258649156129642L;



  // The typesOnly value for the search request.
  private final Boolean typesOnly;

  // The alias dereferencing policy for the search request.
  private final DereferencePolicy derefPolicy;

  // The size limit for the search request.
  private final Integer sizeLimit;

  // The time limit for the search request.
  private final Integer timeLimit;

  // The list of requested attributes for the search request.
  private final List<String> requestedAttributes;

  // The scope for the search request.
  private final SearchScope scope;

  // The base DN for the search request.
  private final String baseDN;

  // The string representation of the filter for the search request.
  private final String filter;



  /**
   * Creates a new search request access log message from the provided message
   * string.
   *
   * @param  s  The string to be parsed as a search request access log message.
   *
   * @throws  LogException  If the provided string cannot be parsed as a valid
   *                        log message.
   */
  public SearchRequestAccessLogMessage(final String s)
         throws LogException
  {
    this(new LogMessage(s));
  }



  /**
   * Creates a new search request access log message from the provided log
   * message.
   *
   * @param  m  The log message to be parsed as a search request access log
   *            message.
   */
  public SearchRequestAccessLogMessage(final LogMessage m)
  {
    super(m);

    baseDN    = getNamedValue("base");
    filter    = getNamedValue("filter");
    sizeLimit = getNamedValueAsInteger("sizeLimit");
    timeLimit = getNamedValueAsInteger("timeLimit");
    typesOnly = getNamedValueAsBoolean("typesOnly");

    SearchScope ss = null;
    try
    {
      ss = SearchScope.definedValueOf(getNamedValueAsInteger("scope"));
    }
    catch (Exception e)
    {
      debugException(e);
    }
    scope = ss;

    DereferencePolicy deref = null;
    final String derefStr = getNamedValue("deref");
    if (derefStr != null)
    {
      for (final DereferencePolicy p : DereferencePolicy.values())
      {
        if (p.getName().equalsIgnoreCase(derefStr))
        {
          deref = p;
          break;
        }
      }
    }
    derefPolicy = deref;

    final String attrStr = getNamedValue("attrs");
    if (attrStr == null)
    {
      requestedAttributes = null;
    }
    else if (attrStr.equals("ALL"))
    {
      requestedAttributes = Collections.emptyList();
    }
    else
    {
      final LinkedList<String> attrs = new LinkedList<String>();
      final StringTokenizer st = new StringTokenizer(attrStr, ",", false);
      while (st.hasMoreTokens())
      {
        attrs.add(st.nextToken());
      }
      requestedAttributes = Collections.unmodifiableList(attrs);
    }
  }



  /**
   * Retrieves the base DN for the search request.
   *
   * @return  The base DN for the search request, or {@code null} if it is not
   *          included in the log message.
   */
  public final String getBaseDN()
  {
    return baseDN;
  }



  /**
   * Retrieves the scope for the search request.
   *
   * @return  The scope for the search request, or {@code null} if it is not
   *          included in the log message.
   */
  public final SearchScope getScope()
  {
    return scope;
  }



  /**
   * Retrieves a string representation of the filter for the search request.
   *
   * @return  A string representation of the filter for the search request, or
   *          {@code null} if it is not included in the log message.
   */
  public final String getFilter()
  {
    return filter;
  }



  /**
   * Retrieves a parsed representation of the filter for the search request.
   *
   * @return  A parsed representation of the filter for the search request, or
   *          {@code null} if it is not included in the log message or the
   *          filter string cannot be parsed as a filter.
   */
  public final Filter getParsedFilter()
  {
    try
    {
      if (filter == null)
      {
        return null;
      }
      else
      {
        return Filter.create(filter);
      }
    }
    catch (Exception e)
    {
      debugException(e);
      return null;
    }
  }



  /**
   * Retrieves the dereference policy for the search request.
   *
   * @return  The dereference policy for the search request, or {@code null} if
   *          it is not included in the log message or the value cannot be
   *          parsed as a valid {@code DereferencePolicy} value.
   */
  public final DereferencePolicy getDereferencePolicy()
  {
    return derefPolicy;
  }



  /**
   * Retrieves the size limit for the search request.
   *
   * @return  The size limit for the search request, or {@code null} if it is
   *          not included in the log message or the value cannot be parsed as
   *          an integer.
   */
  public final Integer getSizeLimit()
  {
    return sizeLimit;
  }



  /**
   * Retrieves the time limit for the search request.
   *
   * @return  The time limit for the search request, or {@code null} if it is
   *          not included in the log message or the value cannot be parsed as
   *          an integer.
   */
  public final Integer getTimeLimit()
  {
    return timeLimit;
  }



  /**
   * Retrieves the typesOnly value for the search request.
   *
   * @return  {@code true} if only attribute type names should be included in
   *          entries that are returned, {@code false} if both attribute types
   *          and values should be returned, or {@code null} if is not included
   *          in the log message or cannot be parsed as a Boolean.
   */
  public final Boolean typesOnly()
  {
    return typesOnly;
  }



  /**
   * Retrieves the list of requested attributes for the search request.
   *
   * @return  The list of requested attributes for the search request, an empty
   *          list if the client did not explicitly request any attributes, or
   *          {@code null} if it is not included in the log message.
   */
  public final List<String> getRequestedAttributes()
  {
    return requestedAttributes;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public final AccessLogOperationType getOperationType()
  {
    return AccessLogOperationType.SEARCH;
  }
}
