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
package com.unboundid.ldap.sdk.unboundidds.monitors;



import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.OperationType;
import com.unboundid.util.Debug;
import com.unboundid.util.NotMutable;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;



/**
 * <BLOCKQUOTE>
 *   <B>NOTE:</B>  This class is part of the Commercial Edition of the UnboundID
 *   LDAP SDK for Java.  It is not available for use in applications that
 *   include only the Standard Edition of the LDAP SDK, and is not supported for
 *   use in conjunction with non-UnboundID products.
 * </BLOCKQUOTE>
 * This class provides a data structure that provides information about the
 * result codes associated with various types of extended operations.
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class ExtendedOperationResultCodeInfo
       implements Serializable
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 2412562905271298484L;



  // The percentage of all extended operations that failed.
  private final Double failedPercent;

  // The total number of operations of the associated type that failed.
  private final Long failedCount;

  // The total number of operations of the associated type.
  private final Long totalCount;

  // The percentage of extended operations that failed, indexed by OID.
  private final Map<String,Double> failedPercentsByOID;

  // The number of extended operations that failed, indexed by OID.
  private final Map<String,Long> failedCountsByOID;

  // The number of extended operations processed, indexed by OID.
  private final Map<String,Long> totalCountsByOID;

  // Information about each result code returned for each type of extended
  // operation, indexed first by extended request OID, then by the result code's
  // integer value.
  private final Map<String,Map<Integer,ResultCodeInfo>> resultCodeInfoMap;

  // The names of the types of extended operations processed, indexed by OID.
  private final Map<String,String> requestNamesByOID;



  /**
   * Creates a new extended operation result code information object from the
   * provided information.
   *
   * @param  entry  The monitor entry to use to obtain the result code
   *                information.
   */
  ExtendedOperationResultCodeInfo(final MonitorEntry entry)
  {
    totalCount = entry.getLong("extended-op-total-count");
    failedCount = entry.getLong("extended-op-failed-count");
    failedPercent = entry.getDouble("extended-op-failed-percent");

    final TreeMap<String,String> names = new TreeMap<String,String>();
    final TreeMap<String,Long> totalCounts = new TreeMap<String,Long>();
    final TreeMap<String,Long> failedCounts = new TreeMap<String,Long>();
    final TreeMap<String,Double> failedPercents = new TreeMap<String,Double>();
    final TreeMap<String,Map<Integer,ResultCodeInfo>> rcMaps =
         new TreeMap<String,Map<Integer,ResultCodeInfo>>();
    final Entry e = entry.getEntry();
    for (final Attribute a : e.getAttributes())
    {
      try
      {
        final String lowerName = StaticUtils.toLowerCase(a.getName());
        if (lowerName.startsWith("extended-op-") &&
            lowerName.endsWith("-total-count"))
        {
          final String dashedOID =
               lowerName.substring(12, (lowerName.length() - 12));
          final String dottedOID = dashedOID.replace('-', '.');

          final String name = entry.getString(
               "extended-op-" + dashedOID + "-name");
          final long total = a.getValueAsLong();
          final long failed = entry.getLong(
               "extended-op-" + dashedOID + "-failed-count");
          final double failedPct = entry.getDouble(
               "extended-op-" + dashedOID + "-failed-percent");

          names.put(dottedOID, name);
          totalCounts.put(dottedOID, total);
          failedCounts.put(dottedOID, failed);
          failedPercents.put(dottedOID, failedPct);
          rcMaps.put(dottedOID,
               getRCMap(e, "extended-op-" + dashedOID + "-result-"));
        }
      }
      catch (final Exception ex)
      {
        Debug.debugException(ex);
      }
    }

    requestNamesByOID = Collections.unmodifiableMap(names);
    totalCountsByOID = Collections.unmodifiableMap(totalCounts);
    failedCountsByOID = Collections.unmodifiableMap(failedCounts);
    failedPercentsByOID = Collections.unmodifiableMap(failedPercents);
    resultCodeInfoMap = Collections.unmodifiableMap(rcMaps);
  }



  /**
   * Retrieves a map with result code information for a particular type of
   * extended operation.
   *
   * @param  entry   The entry to be examined.
   * @param  prefix  The prefix that will be used for all attributes of
   *                 interest.
   *
   * @return  A map with result code information for a particular type of
   *          extended operation.
   */
  private static Map<Integer,ResultCodeInfo> getRCMap(final Entry entry,
                                                      final String prefix)
  {
    final TreeMap<Integer,ResultCodeInfo> m =
         new TreeMap<Integer,ResultCodeInfo>();

    for (final Attribute a : entry.getAttributes())
    {
      try
      {
        final String lowerName = StaticUtils.toLowerCase(a.getName());
        if (lowerName.startsWith(prefix) && lowerName.endsWith("-name"))
        {
          final int intValue = Integer.parseInt(lowerName.substring(
               prefix.length(), (lowerName.length() - 5)));
          final String name = a.getValue();
          final long count = entry.getAttributeValueAsLong(
               prefix + intValue + "-count");
          final double percent = Double.parseDouble(
               entry.getAttributeValue(prefix + intValue + "-percent"));
          final double totalResponseTimeMillis = Double.parseDouble(
               entry.getAttributeValue(prefix + intValue +
                    "-total-response-time-millis"));
          final double averageResponseTimeMillis = Double.parseDouble(
               entry.getAttributeValue(prefix + intValue +
                    "-average-response-time-millis"));
          m.put(intValue, new ResultCodeInfo(intValue, name,
               OperationType.EXTENDED, count, percent, totalResponseTimeMillis,
               averageResponseTimeMillis));
        }
      }
      catch (final Exception ex)
      {
        Debug.debugException(ex);
      }
    }

    return Collections.unmodifiableMap(m);
  }



  /**
   * Retrieves the total number of extended operations of all types that have
   * been processed, if available.
   *
   * @return  The total number of extended operations of all types that have
   *          been processed, or {@code null} if this information was not in the
   *          monitor entry.
   */
  public Long getTotalCount()
  {
    return totalCount;
  }



  /**
   * Retrieves the number of extended operations of each type that have been
   * processed, indexed by extended request OID, if available.
   *
   * @return  The number of extended operations of each type that have been
   *          processed, or an empty map if this information was not in the
   *          monitor entry.
   */
  public Map<String,Long> getTotalCountsByOID()
  {
    return totalCountsByOID;
  }



  /**
   * Retrieves the number of extended operations of all types that resulted in
   * failure, if available.
   *
   * @return  The number of extended operations of all types that resulted in
   *          failure, or {@code null} if this information was not in the
   *          monitor entry.
   */
  public Long getFailedCount()
  {
    return failedCount;
  }



  /**
   * Retrieves the number of extended operations of each type that resulted in
   * failure, indexed by extended request OID, if available.
   *
   * @return  The number of extended operations of each type that resulted in
   *          failure, or an empty map if this information was not in the
   *          monitor entry.
   */
  public Map<String,Long> getFailedCountsByOID()
  {
    return failedCountsByOID;
  }



  /**
   * Retrieves the percent of extended operations of all types that resulted in
   * failure, if available.
   *
   * @return  The percent of extended operations of all types that resulted in
   *          failure, or {@code null} if this information was not in the
   *          monitor entry.
   */
  public Double getFailedPercent()
  {
    return failedPercent;
  }



  /**
   * Retrieves the percent of extended operations of each type that resulted in
   * failure, indexed by extended request OID, if available.
   *
   * @return  The percent of extended operations of each type that resulted in
   *          failure, or an empty map if this information was not in the
   *          monitor entry.
   */
  public Map<String,Double> getFailedPercentsByOID()
  {
    return failedPercentsByOID;
  }



  /**
   * Retrieves a map with information about the result codes that have been
   * returned for extended operations of each type, indexed first by extended
   * request OID, and then by the result code's integer value.
   *
   * @return  A map with information about the result codes that have been
   *          returned for extended operations of each type, or an empty map if
   *          this information was not in the monitor entry.
   */
  public Map<String,Map<Integer,ResultCodeInfo>> getResultCodeInfoMap()
  {
    return resultCodeInfoMap;
  }



  /**
   * Retrieves a map with the human-readable names for each type of extended
   * request, indexed by request OID, if available.
   *
   * @return  A map with the human-readable names for each type of extended
   *          request, or an empty map if this information was not in the
   *          monitor entry.
   */
  public Map<String,String> getExtendedRequestNamesByOID()
  {
    return requestNamesByOID;
  }
}
