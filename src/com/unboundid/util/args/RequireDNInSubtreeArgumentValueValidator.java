/*
 * Copyright 2015 UnboundID Corp.
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
package com.unboundid.util.args;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.unboundid.ldap.sdk.DN;
import com.unboundid.util.Debug;
import com.unboundid.util.NotMutable;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;
import com.unboundid.util.Validator;

import static com.unboundid.util.args.ArgsMessages.*;



/**
 * This class provides an implementation of an argument value validator that is
 * expected to be used with string or DN arguments and ensures that all values
 * for the argument are valid DNs that are within one or more specified
 * subtrees.
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class RequireDNInSubtreeArgumentValueValidator
       extends ArgumentValueValidator
{
  // The set of permitted base DNs for values of the associated argument.
  private final List<DN> baseDNs;



  /**
   * Creates a new instance of this argument value validator with the provided
   * information.
   *
   * @param  baseDNs  The set of permitted base DNs for values of the associated
   *                  argument.  It must not be {@code null} or empty.
   */
  public RequireDNInSubtreeArgumentValueValidator(final DN... baseDNs)
  {
    this(StaticUtils.toList(baseDNs));
  }



  /**
   * Creates a new instance of this argument value validator with the provided
   * information.
   *
   * @param  baseDNs  The set of permitted base DNs for values of the associated
   *                  argument.  It must not be {@code null} or empty.
   */
  public RequireDNInSubtreeArgumentValueValidator(final Collection<DN> baseDNs)
  {
    Validator.ensureNotNull(baseDNs);
    Validator.ensureFalse(baseDNs.isEmpty());

    this.baseDNs = Collections.unmodifiableList(new ArrayList<DN>(baseDNs));
  }



  /**
   * Retrieves a list of the permitted base DNs for this argument value
   * validator.
   *
   * @return  A list of the permitted base DNs for this argument value
   *          validator.
   */
  public List<DN> getBaseDNs()
  {
    return baseDNs;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void validateArgumentValue(final Argument argument,
                                    final String valueString)
         throws ArgumentException
  {
    final DN dn;
    try
    {
      dn = new DN(valueString);
    }
    catch (final Exception e)
    {
      Debug.debugException(e);
      throw new ArgumentException(
           ERR_REQUIRE_DN_IN_SUBTREE_VALIDATOR_VALUE_NOT_DN.get(valueString,
                argument.getIdentifierString()),
           e);
    }


    if (baseDNs.size() == 1)
    {
      if (! dn.isDescendantOf(baseDNs.get(0), true))
      {
        throw new ArgumentException(
             ERR_REQUIRE_DN_IN_SUBTREE_VALIDATOR_VALUE_NOT_IN_SUBTREE.get(
                  valueString, argument.getIdentifierString(),
                  String.valueOf(baseDNs.get(0))));
      }
    }
    else
    {
      final StringBuilder dnList = new StringBuilder();
      final Iterator<DN> iterator = baseDNs.iterator();
      while (iterator.hasNext())
      {
        final DN baseDN = iterator.next();
        if (dn.isDescendantOf(baseDN, true))
        {
          return;
        }

        dnList.append('\'');
        dnList.append(baseDN);
        dnList.append('\'');

        if (iterator.hasNext())
        {
          dnList.append(", ");
        }
      }

      throw new ArgumentException(
           ERR_REQUIRE_DN_IN_SUBTREE_VALIDATOR_VALUE_NOT_IN_SUBTREES.get(
                valueString, argument.getIdentifierString(),
                dnList.toString()));
    }
  }



  /**
   * Retrieves a string representation of this argument value validator.
   *
   * @return  A string representation of this argument value validator.
   */
  @Override()
  public String toString()
  {
    final StringBuilder buffer = new StringBuilder();
    toString(buffer);
    return buffer.toString();
  }



  /**
   * Appends a string representation of this argument value validator to the
   * provided buffer.
   *
   * @param  buffer  The buffer to which the string representation should be
   *                 appended.
   */
  public void toString(final StringBuilder buffer)
  {
    buffer.append("RequireDNInSubtreeArgumentValueValidator(baseDNs={");

    final Iterator<DN> iterator = baseDNs.iterator();
    while (iterator.hasNext())
    {
      buffer.append('\'');
      buffer.append(iterator.next().toString());
      buffer.append('\'');

      if (iterator.hasNext())
      {
        buffer.append(", ");
      }
    }

    buffer.append("})");
  }
}
