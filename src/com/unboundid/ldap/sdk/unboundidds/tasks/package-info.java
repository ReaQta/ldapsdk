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



/**
 * <BLOCKQUOTE>
 *   <B>NOTE:</B>  This package is part of the Commercial Edition of the
 *   UnboundID LDAP SDK for Java.  It is not available for use in applications
 *   that include only the Standard Edition of the LDAP SDK, and is not
 *   supported for use in conjunction with non-UnboundID products.
 * </BLOCKQUOTE>
 * This package contains a number of helper classes for invoking and interacting
 * with scheduled tasks in the UnboundID Directory Server.  Tasks may be used to
 * perform various kinds of administrative functions, like backing up and
 * restoring backends, importing and exporting data, rebuilding indexes, and
 * shutting down or restarting the server.
 * <BR><BR>
 * The {@code TaskManager} class provides a number of utility methods for
 * interacting with tasks in an UnboundID Directory Server.  The {@code Task}
 * class and its subclasses provide a framework for accessing the generic and
 * task-specific information associated with tasks.
 */
package com.unboundid.ldap.sdk.unboundidds.tasks;
