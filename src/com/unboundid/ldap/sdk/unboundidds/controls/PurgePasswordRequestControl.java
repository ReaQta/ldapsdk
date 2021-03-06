/*
 * Copyright 2013-2015 UnboundID Corp.
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



import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.sdk.unboundidds.controls.ControlMessages.*;



/**
 * <BLOCKQUOTE>
 *   <B>NOTE:</B>  This class is part of the Commercial Edition of the UnboundID
 *   LDAP SDK for Java.  It is not available for use in applications that
 *   include only the Standard Edition of the LDAP SDK, and is not supported for
 *   use in conjunction with non-UnboundID products.
 * </BLOCKQUOTE>
 * This class provides a request control that can be included in a modify
 * request or a password modify extended request in order to indicate that if
 * the operation results in changing the password for a user, the user's former
 * password should be purged from the entry rather than retired, and any
 * existing retired password should also be purged.
 * <BR><BR>
 * This control has an OID of "1.3.6.1.4.1.30221.2.5.32" and does not have a
 * value.  The criticality may be either true (in which case the operation will
 * succeed only if the user's password policy allows passwords to be retired by
 * a request control) or false (in which case if the password policy does not
 * allow the use of this control, the operation will be processed as if the
 * control had not been included in the request).
 * <BR><BR>
 * <H2>Example</H2>
 * The following example demonstrates the use of the purge password request
 * control to request that a user's current password be purged in the course of
 * a password change.
 * <PRE>
 * Control[] requestControls =
 * {
 *   new PurgePasswordRequestControl(true)
 * };
 *
 * PasswordModifyExtendedRequest passwordModifyRequest =
 *      new PasswordModifyExtendedRequest(
 *           "uid=test.user,ou=People,dc=example,dc=com", // The user to update
 *           null, // The current password -- we don't know it.
 *           "newPassword", // The new password to assign to the user.
 *           requestControls); // The controls to include in the request.
 * PasswordModifyExtendedResult passwordModifyResult =
 *      (PasswordModifyExtendedResult)
 *      connection.processExtendedOperation(passwordModifyRequest);
 * </PRE>
 *
 * @see  RetirePasswordRequestControl
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class PurgePasswordRequestControl
       extends Control
{
  /**
   * The OID (1.3.6.1.4.1.4203.1.10.2) for the purge password request control.
   */
  public static final  String PURGE_PASSWORD_REQUEST_OID =
       "1.3.6.1.4.1.30221.2.5.32";



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -3756801088881565921L;



  /**
   * Creates a new retire password request control with the specified
   * criticality.
   *
   * @param  isCritical  Indicates whether the control should be considered
   *                     critical.
   */
  public PurgePasswordRequestControl(final boolean isCritical)
  {
    super(PURGE_PASSWORD_REQUEST_OID, isCritical, null);
  }



  /**
   * Creates a new retire password request control which is decoded from the
   * provided generic control.
   *
   * @param  control  The generic control to be decoded as a retire password
   *                  request control.
   *
   * @throws LDAPException  If the provided control cannot be decoded as a
   *                         retire password request control.
   */
  public PurgePasswordRequestControl(final Control control)
       throws LDAPException
  {
    super(control);

    if (control.hasValue())
    {
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_PURGE_PASSWORD_REQUEST_CONTROL_HAS_VALUE.get());
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getControlName()
  {
    return INFO_CONTROL_NAME_PURGE_PASSWORD_REQUEST.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("PurgePasswordRequestControl(isCritical=");
    buffer.append(isCritical());
    buffer.append(')');
  }
}
