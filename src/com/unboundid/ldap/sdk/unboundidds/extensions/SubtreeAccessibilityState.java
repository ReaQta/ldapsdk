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
package com.unboundid.ldap.sdk.unboundidds.extensions;



import com.unboundid.util.StaticUtils;



/**
 * <BLOCKQUOTE>
 *   <B>NOTE:</B>  This class is part of the Commercial Edition of the UnboundID
 *   LDAP SDK for Java.  It is not available for use in applications that
 *   include only the Standard Edition of the LDAP SDK, and is not supported for
 *   use in conjunction with non-UnboundID products.
 * </BLOCKQUOTE>
 * This enum defines the set of allowed accessibility states that may be used
 * with the {@link SetSubtreeAccessibilityExtendedRequest}.
 */
public enum SubtreeAccessibilityState
{
  /**
   * Indicates that the subtree should return to normal accessibility so that
   * all appropriately-authorized users will be able to perform all kinds of
   * operations in the target subtree.
   */
  ACCESSIBLE(0, "accessible"),



  /**
   * Indicates that the subtree should be made read-only so that search and
   * compare operations targeting those entries will be allowed, but add,
   * delete, modify, and modify DN operations will only be allowed for one
   * specified user (as indicated in the set subtree accessibility request).
   * Bind operations will be allowed, but any changes intended to update
   * password policy or other account state (e.g., to record failed
   * authentication attempts or update last login time) will not be applied.
   */
  READ_ONLY_BIND_ALLOWED(1, "read-only-bind-allowed"),



  /**
   * Indicates that the subtree should be made read-only so that search and
   * compare operations targeting those entries will be allowed, but add,
   * delete, modify, and modify DN operations will only be allowed for one
   * specified user (as indicated in the set subtree accessibility request).
   * Bind operations will not be allowed for any user in the specified subtree.
   */
  READ_ONLY_BIND_DENIED(2, "read-only-bind-denied"),



  /**
   * Indicates that the subtree should be made hidden so that is is not
   * accessible to most clients for any kinds of operations.  The subtree will
   * be available to one specified user (as indicated in the set subtree
   * accessibility request) for add, compare, delete, modify, modify DN, and
   * search operations.  Bind operations will not be allowed for any user in a
   * hidden subtree.
   */
  HIDDEN(3, "hidden");



  // The integer value for this subtree accessibility state.
  private final int intValue;

  // The name for this subtree accessibility state.
  private final String stateName;



  /**
   * Creates a new subtree accessibility state with the provided integer value.
   *
   * @param  intValue   The integer value for this subtree accessibility state.
   * @param  stateName  The name for this subtree accessibility state.
   */
  SubtreeAccessibilityState(final int intValue, final String stateName)
  {
    this.intValue  = intValue;
    this.stateName = stateName;
  }



  /**
   * Retrieves the integer value for this subtree accessibility state.
   *
   * @return  The integer value for this subtree accessibility state.
   */
  public int intValue()
  {
    return intValue;
  }



  /**
   * Retrieves the name for this subtree accessibility state.
   *
   * @return  The name for this subtree accessibility state.
   */
  public String getStateName()
  {
    return stateName;
  }



  /**
   * Indicates whether this state object represents the ACCESSIBLE state.
   *
   * @return  {@code true} if this state object represents the ACCESSIBLE state,
   *          or {@code false} if not.
   */
  public boolean isAccessible()
  {
    return (this == ACCESSIBLE);
  }



  /**
   * Indicates whether this state object represents the HIDDEN state.
   *
   * @return  {@code true} if this state object represents the HIDDEN state,
   *          or {@code false} if not.
   */
  public boolean isHidden()
  {
    return (this == HIDDEN);
  }



  /**
   * Indicates whether this state object represents one of the read-only states.
   *
   * @return  {@code true} if this state object represents one of the read-only
   *          states, or {@code false} if not.
   */
  public boolean isReadOnly()
  {
    return ((this == READ_ONLY_BIND_ALLOWED) ||
            (this == READ_ONLY_BIND_DENIED));
  }



  /**
   * Retrieves the subtree accessibility state with the specified integer value.
   *
   * @param  intValue  The integer value for the state to retrieve.
   *
   * @return  The subtree accessibility state with the specified integer value,
   *          or {@code null} if there is no accessibility state with the
   *          specified integer value.
   */
  public static SubtreeAccessibilityState valueOf(final int intValue)
  {
    switch (intValue)
    {
      case 0:
        return ACCESSIBLE;
      case 1:
        return READ_ONLY_BIND_ALLOWED;
      case 2:
        return READ_ONLY_BIND_DENIED;
      case 3:
        return HIDDEN;
      default:
        return null;
    }
  }



  /**
   * Retrieves the subtree accessibility state with the provided name.
   *
   * @param  name  The name for the subtree accessibility state to retrieve.
   *
   * @return  The subtree accessibility state with the specified name, or
   *          {@code null} if no state has the provided name.
   */
  public static SubtreeAccessibilityState forName(final String name)
  {
    final String lowerName = StaticUtils.toLowerCase(name).replace('_', '-');
    for (final SubtreeAccessibilityState s : values())
    {
      if (s.stateName.equals(lowerName))
      {
        return s;
      }
    }

    return null;
  }



  /**
   * Retrieves a string representation of this subtree accessibility state.
   *
   * @return  A string representation of this subtree accessibility state.
   */
  @Override()
  public String toString()
  {
    return stateName;
  }
}
