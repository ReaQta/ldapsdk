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
package com.unboundid.ldap.sdk.unboundidds.tasks;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.util.NotMutable;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;

import static com.unboundid.ldap.sdk.unboundidds.tasks.TaskMessages.*;
import static com.unboundid.util.Debug.*;



/**
 * <BLOCKQUOTE>
 *   <B>NOTE:</B>  This class is part of the Commercial Edition of the UnboundID
 *   LDAP SDK for Java.  It is not available for use in applications that
 *   include only the Standard Edition of the LDAP SDK, and is not supported for
 *   use in conjunction with non-UnboundID products.
 * </BLOCKQUOTE>
 * This class defines a Directory Server task that can be used to request that
 * the server terminate a client connection.  The properties that are available
 * for use with this type of task include:
 * <UL>
 *   <LI>The connection ID for the client connection to be terminated.  This
 *       is required.</LI>
 *   <LI>A flag that indicates whether the client connection should be notified
 *       (e.g., using a notice of disconnection unsolicited notification) before
 *       the connection is actually terminated.</LI>
 *   <LI>An optional message that may provide a reason for the disconnect.  If
 *       this is provided, it will appear in the server log, and it may be
 *       provided to the client if the client is to be notified before the
 *       connection is closed.</LI>
 * </UL>

 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class DisconnectClientTask
       extends Task
{
  /**
   * The fully-qualified name of the Java class that is used for the disconnect
   * client task.
   */
  static final String DISCONNECT_CLIENT_TASK_CLASS =
       "com.unboundid.directory.server.tasks.DisconnectClientTask";



  /**
   * The name of the attribute used to specify the connection ID of the client
   * connection to terminate.
   */
  private static final String ATTR_CONNECTION_ID =
       "ds-task-disconnect-connection-id";



  /**
   * The name of the attribute used to specify the disconnect message to provide
   * to the server.
   */
  private static final String ATTR_DISCONNECT_MESSAGE =
       "ds-task-disconnect-message";



  /**
   * The name of the attribute used to indicate whether to send a notice of
   * disconnection message to the client before closing the connection.
   */
  private static final String ATTR_NOTIFY_CLIENT =
       "ds-task-disconnect-notify-client";



  /**
   * The name of the object class used in disconnect client task entries.
   */
  private static final String OC_DISCONNECT_CLIENT_TASK = "ds-task-disconnect";



  /**
   * The task property for the connection ID.
   */
  private static final TaskProperty PROPERTY_CONNECTION_ID =
       new TaskProperty(ATTR_CONNECTION_ID,
                        INFO_DISPLAY_NAME_DISCONNECT_CONN_ID.get(),
                        INFO_DESCRIPTION_DISCONNECT_CONN_ID.get(), Long.class,
                        true, false, false);



  /**
   * The task property for the disconnect message.
   */
  private static final TaskProperty PROPERTY_DISCONNECT_MESSAGE =
       new TaskProperty(ATTR_DISCONNECT_MESSAGE,
                        INFO_DISPLAY_NAME_DISCONNECT_MESSAGE.get(),
                        INFO_DESCRIPTION_DISCONNECT_MESSAGE.get(), String.class,
                        false, false, false);



  /**
   * The task property for the notify client flag.
   */
  private static final TaskProperty PROPERTY_NOTIFY_CLIENT =
       new TaskProperty(ATTR_NOTIFY_CLIENT,
                        INFO_DISPLAY_NAME_DISCONNECT_NOTIFY.get(),
                        INFO_DESCRIPTION_DISCONNECT_NOTIFY.get(), Boolean.class,
                        false, false, false);



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 6870137048384152893L;



  // Indicates whether to send the client a notice of disconnection.
  private final boolean notifyClient;

  // The connection ID of the connection to disconnect.
  private final long connectionID;

  // A disconnect message to provide to the server.
  private final String disconnectMessage;



  /**
   * Creates a new uninitialized disconnect client task instance which should
   * only be used for obtaining general information about this task, including
   * the task name, description, and supported properties.  Attempts to use a
   * task created with this constructor for any other reason will likely fail.
   */
  public DisconnectClientTask()
  {
    notifyClient      = false;
    connectionID      = -1;
    disconnectMessage = null;
  }




  /**
   * Creates a new disconnect client task with the provided information.
   *
   * @param  taskID             The task ID to use for this task.  If it is
   *                            {@code null} then a UUID will be generated for
   *                            use as the task ID.
   * @param  connectionID       The connection ID of the client connection to
   *                            terminate.
   * @param  disconnectMessage  A message to provide to the server to indicate
   *                            the reason for the disconnect.  It will be
   *                            included in the server log, and will be provided
   *                            to the client if a notice of disconnection is to
   *                            be sent.  It may be {@code null} if no message
   *                            is to be provided.
   * @param  notifyClient       Indicates whether to send a notice of
   *                            disconnection message to the client before
   *                            terminating the connection.
   */
  public DisconnectClientTask(final String taskID, final long connectionID,
                              final String disconnectMessage,
                              final boolean notifyClient)
  {
    this(taskID, connectionID, disconnectMessage, notifyClient, null, null,
         null, null, null);
  }



  /**
   * Creates a new add disconnect client task with the provided information.
   *
   * @param  taskID                  The task ID to use for this task.  If it is
   *                                 {@code null} then a UUID will be generated
   *                                 for use as the task ID.
   * @param  connectionID            The connection ID of the client connection
   *                                 to terminate.
   * @param  disconnectMessage       A message to provide to the server to
   *                                 indicate the reason for the disconnect.  It
   *                                 will be included in the server log, and
   *                                 will be provided to the client if a notice
   *                                 of disconnection is to be sent.  It may be
   *                                 {@code null} if no message is to be
   *                                 provided.
   * @param  notifyClient            Indicates whether to send a notice of
   *                                 disconnection message to the client before
   *                                 terminating the connection.
   * @param  scheduledStartTime      The time that this task should start
   *                                 running.
   * @param  dependencyIDs           The list of task IDs that will be required
   *                                 to complete before this task will be
   *                                 eligible to start.
   * @param  failedDependencyAction  Indicates what action should be taken if
   *                                 any of the dependencies for this task do
   *                                 not complete successfully.
   * @param  notifyOnCompletion      The list of e-mail addresses of individuals
   *                                 that should be notified when this task
   *                                 completes.
   * @param  notifyOnError           The list of e-mail addresses of individuals
   *                                 that should be notified if this task does
   *                                 not complete successfully.
   */
  public DisconnectClientTask(final String taskID, final long connectionID,
              final String disconnectMessage, final boolean notifyClient,
              final Date scheduledStartTime, final List<String> dependencyIDs,
              final FailedDependencyAction failedDependencyAction,
              final List<String> notifyOnCompletion,
              final List<String> notifyOnError)
  {
    super(taskID, DISCONNECT_CLIENT_TASK_CLASS, scheduledStartTime,
          dependencyIDs, failedDependencyAction, notifyOnCompletion,
          notifyOnError);

    this.connectionID      = connectionID;
    this.disconnectMessage = disconnectMessage;
    this.notifyClient      = notifyClient;
  }



  /**
   * Creates a new disconnect client task from the provided entry.
   *
   * @param  entry  The entry to use to create this disconnect client task.
   *
   * @throws  TaskException  If the provided entry cannot be parsed as a
   *                         disconnect client task entry.
   */
  public DisconnectClientTask(final Entry entry)
         throws TaskException
  {
    super(entry);


    // Get the connection ID.  It must be present.
    final String idStr = entry.getAttributeValue(ATTR_CONNECTION_ID);
    if (idStr == null)
    {
      throw new TaskException(ERR_DISCONNECT_TASK_NO_CONN_ID.get(
                                   getTaskEntryDN()));
    }
    else
    {
      try
      {
        connectionID = Long.parseLong(idStr);
      }
      catch (Exception e)
      {
        debugException(e);
        throw new TaskException(ERR_DISCONNECT_TASK_CONN_ID_NOT_LONG.get(
                                     getTaskEntryDN(), idStr),
                                e);
      }
    }


    // Get the disconnect message.  It may be absent.
    disconnectMessage = entry.getAttributeValue(ATTR_DISCONNECT_MESSAGE);


    // Determine whether to notify the client.  It may be absent.
    notifyClient = parseBooleanValue(entry, ATTR_NOTIFY_CLIENT, false);
  }



  /**
   * Creates a new disconnect client task from the provided set of task
   * properties.
   *
   * @param  properties  The set of task properties and their corresponding
   *                     values to use for the task.  It must not be
   *                     {@code null}.
   *
   * @throws  TaskException  If the provided set of properties cannot be used to
   *                         create a valid disconnect client task.
   */
  public DisconnectClientTask(final Map<TaskProperty,List<Object>> properties)
         throws TaskException
  {
    super(DISCONNECT_CLIENT_TASK_CLASS, properties);

    boolean notify = false;
    Long    connID = null;
    String  msg    = null;


    for (final Map.Entry<TaskProperty,List<Object>> entry :
         properties.entrySet())
    {
      final TaskProperty p = entry.getKey();
      final String attrName = p.getAttributeName();
      final List<Object> values = entry.getValue();

      if (attrName.equalsIgnoreCase(ATTR_CONNECTION_ID))
      {
        connID = parseLong(p, values, connID);
      }
      else if (attrName.equalsIgnoreCase(ATTR_DISCONNECT_MESSAGE))
      {
        msg = parseString(p, values, msg);
      }
      else if (attrName.equalsIgnoreCase(ATTR_NOTIFY_CLIENT))
      {
        notify = parseBoolean(p, values, notify);
      }
    }

    if (connID == null)
    {
      throw new TaskException(ERR_DISCONNECT_TASK_NO_CONN_ID.get(
                                   getTaskEntryDN()));
    }

    connectionID      = connID;
    disconnectMessage = msg;
    notifyClient      = notify;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getTaskName()
  {
    return INFO_TASK_NAME_DISCONNECT_CLIENT.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getTaskDescription()
  {
    return INFO_TASK_DESCRIPTION_DISCONNECT_CLIENT.get();
  }



  /**
   * Retrieves the connection ID of the client connection to disconnect.
   *
   * @return  The connection ID of the client connection to disconnect.
   */
  public long getConnectionID()
  {
    return connectionID;
  }



  /**
   * Retrieves the disconnect message to provide to the server, and potentially
   * to the client.
   *
   * @return  The disconnect message, or {@code null} if no message is to be
   *          provided.
   */
  public String getDisconnectMessage()
  {
    return disconnectMessage;
  }



  /**
   * Indicates whether to send a notice of disconnection message to the client
   * before terminating the connection.
   *
   * @return  {@code true} if the server should send a notice of disconnection
   *          to the client, or {@code false} if it should terminate the
   *          connection without warning.
   */
  public boolean notifyClient()
  {
    return notifyClient;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected List<String> getAdditionalObjectClasses()
  {
    return Arrays.asList(OC_DISCONNECT_CLIENT_TASK);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  protected List<Attribute> getAdditionalAttributes()
  {
    final ArrayList<Attribute> attrs = new ArrayList<Attribute>(3);

    attrs.add(new Attribute(ATTR_CONNECTION_ID, String.valueOf(connectionID)));
    attrs.add(new Attribute(ATTR_NOTIFY_CLIENT, String.valueOf(notifyClient)));

    if (disconnectMessage != null)
    {
      attrs.add(new Attribute(ATTR_DISCONNECT_MESSAGE, disconnectMessage));
    }

    return attrs;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public List<TaskProperty> getTaskSpecificProperties()
  {
    final List<TaskProperty> propList = Arrays.asList(
         PROPERTY_CONNECTION_ID,
         PROPERTY_DISCONNECT_MESSAGE,
         PROPERTY_NOTIFY_CLIENT);

    return Collections.unmodifiableList(propList);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public Map<TaskProperty,List<Object>> getTaskPropertyValues()
  {
    final LinkedHashMap<TaskProperty,List<Object>> props =
         new LinkedHashMap<TaskProperty,List<Object>>();

    props.put(PROPERTY_CONNECTION_ID,
              Collections.<Object>unmodifiableList(Arrays.asList(
                   connectionID)));

    if (disconnectMessage == null)
    {
      props.put(PROPERTY_DISCONNECT_MESSAGE, Collections.emptyList());
    }
    else
    {
      props.put(PROPERTY_DISCONNECT_MESSAGE,
                Collections.<Object>unmodifiableList(Arrays.asList(
                     disconnectMessage)));
    }

    props.put(PROPERTY_NOTIFY_CLIENT,
              Collections.<Object>unmodifiableList(Arrays.asList(
                   notifyClient)));

    props.putAll(super.getTaskPropertyValues());
    return Collections.unmodifiableMap(props);
  }
}
