/*
 * Copyright 2007-2015 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2008-2015 UnboundID Corp.
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
package com.unboundid.ldif;



import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.util.Base64;
import com.unboundid.util.LDAPSDKThreadFactory;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;
import com.unboundid.util.ByteStringBuffer;
import com.unboundid.util.parallel.ParallelProcessor;
import com.unboundid.util.parallel.Result;
import com.unboundid.util.parallel.Processor;

import static com.unboundid.util.Debug.*;
import static com.unboundid.util.StaticUtils.*;
import static com.unboundid.util.Validator.*;



/**
 * This class provides an LDIF writer, which can be used to write entries and
 * change records in the LDAP Data Interchange Format as per
 * <A HREF="http://www.ietf.org/rfc/rfc2849.txt">RFC 2849</A>.
 * <BR><BR>
 * <H2>Example</H2>
 * The following example performs a search to find all users in the "Sales"
 * department and then writes their entries to an LDIF file:
 * <PRE>
 * // Perform a search to find all users who are members of the sales
 * // department.
 * SearchRequest searchRequest = new SearchRequest("dc=example,dc=com",
 *      SearchScope.SUB, Filter.createEqualityFilter("ou", "Sales"));
 * SearchResult searchResult;
 * try
 * {
 *   searchResult = connection.search(searchRequest);
 * }
 * catch (LDAPSearchException lse)
 * {
 *   searchResult = lse.getSearchResult();
 * }
 * LDAPTestUtils.assertResultCodeEquals(searchResult, ResultCode.SUCCESS);
 *
 * // Write all of the matching entries to LDIF.
 * int entriesWritten = 0;
 * LDIFWriter ldifWriter = new LDIFWriter(pathToLDIF);
 * for (SearchResultEntry entry : searchResult.getSearchEntries())
 * {
 *   ldifWriter.writeEntry(entry);
 *   entriesWritten++;
 * }
 *
 * ldifWriter.close();
 * </PRE>
 */
@ThreadSafety(level=ThreadSafetyLevel.NOT_THREADSAFE)
public final class LDIFWriter
{
  /**
   * The bytes that comprise the LDIF version header.
   */
  private static final byte[] VERSION_1_HEADER_BYTES =
       getBytes("version: 1" + EOL);



  /**
   * The default buffer size (128KB) that will be used when writing LDIF data
   * to the appropriate destination.
   */
  private static final int DEFAULT_BUFFER_SIZE = 128 * 1024;


  // The writer that will be used to actually write the data.
  private final BufferedOutputStream writer;

  // The byte string buffer that will be used to convert LDIF records to LDIF.
  // It will only be used when operating synchronously.
  private final ByteStringBuffer buffer;

  // The translator to use for change records to be written, if any.
  private final LDIFWriterChangeRecordTranslator changeRecordTranslator;

  // The translator to use for entries to be written, if any.
  private final LDIFWriterEntryTranslator entryTranslator;

  // The column at which to wrap long lines.
  private int wrapColumn = 0;

  // A pre-computed value that is two less than the wrap column.
  private int wrapColumnMinusTwo = -2;

  // non-null if this writer was configured to use multiple threads when
  // writing batches of entries.
  private final ParallelProcessor<LDIFRecord,ByteStringBuffer>
       toLdifBytesInvoker;


  /**
   * Creates a new LDIF writer that will write entries to the provided file.
   *
   * @param  path  The path to the LDIF file to be written.  It must not be
   *               {@code null}.
   *
   * @throws  IOException  If a problem occurs while opening the provided file
   *                       for writing.
   */
  public LDIFWriter(final String path)
         throws IOException
  {
    this(new FileOutputStream(path));
  }



  /**
   * Creates a new LDIF writer that will write entries to the provided file.
   *
   * @param  file  The LDIF file to be written.  It must not be {@code null}.
   *
   * @throws  IOException  If a problem occurs while opening the provided file
   *                       for writing.
   */
  public LDIFWriter(final File file)
         throws IOException
  {
    this(new FileOutputStream(file));
  }



  /**
   * Creates a new LDIF writer that will write entries to the provided output
   * stream.
   *
   * @param  outputStream  The output stream to which the data is to be written.
   *                       It must not be {@code null}.
   */
  public LDIFWriter(final OutputStream outputStream)
  {
    this(outputStream, 0);
  }



  /**
   * Creates a new LDIF writer that will write entries to the provided output
   * stream optionally using parallelThreads when writing batches of LDIF
   * records.
   *
   * @param  outputStream     The output stream to which the data is to be
   *                          written.  It must not be {@code null}.
   * @param  parallelThreads  If this value is greater than zero, then the
   *                          specified number of threads will be used to
   *                          encode entries before writing them to the output
   *                          for the {@code writeLDIFRecords(List)} method.
   *                          Note this is the only output method that will
   *                          use multiple threads.
   *                          This should only be set to greater than zero when
   *                          performance analysis has demonstrated that writing
   *                          the LDIF is a bottleneck.  The default
   *                          synchronous processing is normally fast enough.
   *                          There is no benefit in passing in a value
   *                          greater than the number of processors in the
   *                          system.  A value of zero implies the
   *                          default behavior of reading and parsing LDIF
   *                          records synchronously when one of the read
   *                          methods is called.
   */
  public LDIFWriter(final OutputStream outputStream, final int parallelThreads)
  {
    this(outputStream, parallelThreads, null);
  }



  /**
   * Creates a new LDIF writer that will write entries to the provided output
   * stream optionally using parallelThreads when writing batches of LDIF
   * records.
   *
   * @param  outputStream     The output stream to which the data is to be
   *                          written.  It must not be {@code null}.
   * @param  parallelThreads  If this value is greater than zero, then the
   *                          specified number of threads will be used to
   *                          encode entries before writing them to the output
   *                          for the {@code writeLDIFRecords(List)} method.
   *                          Note this is the only output method that will
   *                          use multiple threads.
   *                          This should only be set to greater than zero when
   *                          performance analysis has demonstrated that writing
   *                          the LDIF is a bottleneck.  The default
   *                          synchronous processing is normally fast enough.
   *                          There is no benefit in passing in a value
   *                          greater than the number of processors in the
   *                          system.  A value of zero implies the
   *                          default behavior of reading and parsing LDIF
   *                          records synchronously when one of the read
   *                          methods is called.
   * @param  entryTranslator  An optional translator that will be used to alter
   *                          entries before they are actually written.  This
   *                          may be {@code null} if no translator is needed.
   */
  public LDIFWriter(final OutputStream outputStream, final int parallelThreads,
                    final LDIFWriterEntryTranslator entryTranslator)
  {
    this(outputStream, parallelThreads, entryTranslator, null);
  }



  /**
   * Creates a new LDIF writer that will write entries to the provided output
   * stream optionally using parallelThreads when writing batches of LDIF
   * records.
   *
   * @param  outputStream            The output stream to which the data is to
   *                                 be written.  It must not be {@code null}.
   * @param  parallelThreads         If this value is greater than zero, then
   *                                 the specified number of threads will be
   *                                 used to encode entries before writing them
   *                                 to the output for the
   *                                 {@code writeLDIFRecords(List)} method.
   *                                 Note this is the only output method that
   *                                 will use multiple threads.  This should
   *                                 only be set to greater than zero when
   *                                 performance analysis has demonstrated that
   *                                 writing the LDIF is a bottleneck.  The
   *                                 default synchronous processing is normally
   *                                 fast enough.  There is no benefit in
   *                                 passing in a value greater than the number
   *                                 of processors in the system.  A value of
   *                                 zero implies the default behavior of
   *                                 reading and parsing LDIF records
   *                                 synchronously when one of the read methods
   *                                 is called.
   * @param  entryTranslator         An optional translator that will be used to
   *                                 alter entries before they are actually
   *                                 written.  This may be {@code null} if no
   *                                 translator is needed.
   * @param  changeRecordTranslator  An optional translator that will be used to
   *                                 alter change records before they are
   *                                 actually written.  This may be {@code null}
   *                                 if no translator is needed.
   */
  public LDIFWriter(final OutputStream outputStream, final int parallelThreads,
              final LDIFWriterEntryTranslator entryTranslator,
              final LDIFWriterChangeRecordTranslator changeRecordTranslator)
  {
    ensureNotNull(outputStream);
    ensureTrue(parallelThreads >= 0,
         "LDIFWriter.parallelThreads must not be negative.");

    this.entryTranslator = entryTranslator;
    this.changeRecordTranslator = changeRecordTranslator;
    buffer = new ByteStringBuffer();

    if (outputStream instanceof BufferedOutputStream)
    {
      writer = (BufferedOutputStream) outputStream;
    }
    else
    {
      writer = new BufferedOutputStream(outputStream, DEFAULT_BUFFER_SIZE);
    }

    if (parallelThreads == 0)
    {
      toLdifBytesInvoker = null;
    }
    else
    {
      final LDAPSDKThreadFactory threadFactory =
           new LDAPSDKThreadFactory("LDIFWriter Worker", true, null);
      toLdifBytesInvoker = new ParallelProcessor<LDIFRecord,ByteStringBuffer>(
           new Processor<LDIFRecord,ByteStringBuffer>() {
             public ByteStringBuffer process(final LDIFRecord input)
                    throws IOException
             {
               final LDIFRecord r;
               if ((entryTranslator != null) && (input instanceof Entry))
               {
                 r = entryTranslator.translateEntryToWrite((Entry) input);
                 if (r == null)
                 {
                   return null;
                 }
               }
               else if ((changeRecordTranslator != null) &&
                        (input instanceof LDIFChangeRecord))
               {
                 r = changeRecordTranslator.translateChangeRecordToWrite(
                      (LDIFChangeRecord) input);
                 if (r == null)
                 {
                   return null;
                 }
               }
               else
               {
                 r = input;
               }

               final ByteStringBuffer b = new ByteStringBuffer(200);
               r.toLDIF(b, wrapColumn);
               return b;
             }
           }, threadFactory, parallelThreads, 5);
    }
  }



  /**
   * Flushes the output stream used by this LDIF writer to ensure any buffered
   * data is written out.
   *
   * @throws  IOException  If a problem occurs while attempting to flush the
   *                       output stream.
   */
  public void flush()
         throws IOException
  {
    writer.flush();
  }



  /**
   * Closes this LDIF writer and the underlying LDIF target.
   *
   * @throws  IOException  If a problem occurs while closing the underlying LDIF
   *                       target.
   */
  public void close()
         throws IOException
  {
    try
    {
      if (toLdifBytesInvoker != null)
      {
        try
        {
          toLdifBytesInvoker.shutdown();
        }
        catch (InterruptedException e)
        {
          debugException(e);
        }
      }
    }
    finally
    {
      writer.close();
    }
  }



  /**
   * Retrieves the column at which to wrap long lines.
   *
   * @return  The column at which to wrap long lines, or zero to indicate that
   *          long lines should not be wrapped.
   */
  public int getWrapColumn()
  {
    return wrapColumn;
  }



  /**
   * Specifies the column at which to wrap long lines.  A value of zero
   * indicates that long lines should not be wrapped.
   *
   * @param  wrapColumn  The column at which to wrap long lines.
   */
  public void setWrapColumn(final int wrapColumn)
  {
    this.wrapColumn = wrapColumn;

    wrapColumnMinusTwo = wrapColumn - 2;
  }



  /**
   * Writes the LDIF version header (i.e.,"version: 1").  If a version header
   * is to be added to the LDIF content, it should be done before any entries or
   * change records have been written.
   *
   * @throws  IOException  If a problem occurs while writing the version header.
   */
  public void writeVersionHeader()
         throws IOException
  {
    writer.write(VERSION_1_HEADER_BYTES);
  }



  /**
   * Writes the provided entry in LDIF form.
   *
   * @param  entry  The entry to be written.  It must not be {@code null}.
   *
   * @throws  IOException  If a problem occurs while writing the LDIF data.
   */
  public void writeEntry(final Entry entry)
         throws IOException
  {
    writeEntry(entry, null);
  }



  /**
   * Writes the provided entry in LDIF form, preceded by the provided comment.
   *
   * @param  entry    The entry to be written in LDIF form.  It must not be
   *                  {@code null}.
   * @param  comment  The comment to be written before the entry.  It may be
   *                  {@code null} if no comment is to be written.
   *
   * @throws  IOException  If a problem occurs while writing the LDIF data.
   */
  public void writeEntry(final Entry entry, final String comment)
         throws IOException
  {
    ensureNotNull(entry);

    final Entry e;
    if (entryTranslator == null)
    {
      e = entry;
    }
    else
    {
      e = entryTranslator.translateEntryToWrite(entry);
      if (e == null)
      {
        return;
      }
    }

    if (comment != null)
    {
      writeComment(comment, false, false);
    }

    debugLDIFWrite(e);
    writeLDIF(e);
  }



  /**
   * Writes the provided change record in LDIF form.
   *
   * @param  changeRecord  The change record to be written.  It must not be
   *                       {@code null}.
   *
   * @throws  IOException  If a problem occurs while writing the LDIF data.
   */
  public void writeChangeRecord(final LDIFChangeRecord changeRecord)
         throws IOException
  {
    writeChangeRecord(changeRecord, null);
  }



  /**
   * Writes the provided change record in LDIF form, preceded by the provided
   * comment.
   *
   * @param  changeRecord  The change record to be written.  It must not be
   *                       {@code null}.
   * @param  comment       The comment to be written before the entry.  It may
   *                       be {@code null} if no comment is to be written.
   *
   * @throws  IOException  If a problem occurs while writing the LDIF data.
   */
  public void writeChangeRecord(final LDIFChangeRecord changeRecord,
                                final String comment)
         throws IOException
  {
    ensureNotNull(changeRecord);

    final LDIFChangeRecord r;
    if (changeRecordTranslator == null)
    {
      r = changeRecord;
    }
    else
    {
      r = changeRecordTranslator.translateChangeRecordToWrite(changeRecord);
      if (r == null)
      {
        return;
      }
    }

    if (comment != null)
    {
      writeComment(comment, false, false);
    }

    debugLDIFWrite(r);
    writeLDIF(r);
  }



  /**
   * Writes the provided record in LDIF form.
   *
   * @param  record  The LDIF record to be written.  It must not be
   *                 {@code null}.
   *
   * @throws  IOException  If a problem occurs while writing the LDIF data.
   */
  public void writeLDIFRecord(final LDIFRecord record)
         throws IOException
  {
    writeLDIFRecord(record, null);
  }



  /**
   * Writes the provided record in LDIF form, preceded by the provided comment.
   *
   * @param  record   The LDIF record to be written.  It must not be
   *                  {@code null}.
   * @param  comment  The comment to be written before the LDIF record.  It may
   *                  be {@code null} if no comment is to be written.
   *
   * @throws  IOException  If a problem occurs while writing the LDIF data.
   */
  public void writeLDIFRecord(final LDIFRecord record, final String comment)
         throws IOException
  {
    ensureNotNull(record);

    final LDIFRecord r;
    if ((entryTranslator != null) && (record instanceof Entry))
    {
      r = entryTranslator.translateEntryToWrite((Entry) record);
      if (r == null)
      {
        return;
      }
    }
    else if ((changeRecordTranslator != null) &&
             (record instanceof LDIFChangeRecord))
    {
      r = changeRecordTranslator.translateChangeRecordToWrite(
           (LDIFChangeRecord) record);
      if (r == null)
      {
        return;
      }
    }
    else
    {
      r = record;
    }

    debugLDIFWrite(r);
    if (comment != null)
    {
      writeComment(comment, false, false);
    }

    writeLDIF(r);
  }



  /**
   * Writes the provided list of LDIF records (most likely Entries) to the
   * output.  If this LDIFWriter was constructed without any parallel
   * output threads, then this behaves identically to calling
   * {@code writeLDIFRecord()} sequentially for each item in the list.
   * If this LDIFWriter was constructed to write records in parallel, then
   * the configured number of threads are used to convert the records to raw
   * bytes, which are sequentially written to the input file.  This can speed up
   * the total time to write a large set of records. Either way, the output
   * records are guaranteed to be written in the order they appear in the list.
   *
   * @param ldifRecords  The LDIF records (most likely entries) to write to the
   *                     output.
   *
   * @throws IOException  If a problem occurs while writing the LDIF data.
   *
   * @throws InterruptedException  If this thread is interrupted while waiting
   *                               for the records to be written to the output.
   */
  public void writeLDIFRecords(final List<? extends LDIFRecord> ldifRecords)
         throws IOException, InterruptedException
  {
    if (toLdifBytesInvoker == null)
    {
      for (final LDIFRecord ldifRecord : ldifRecords)
      {
        writeLDIFRecord(ldifRecord);
      }
    }
    else
    {
      final List<Result<LDIFRecord,ByteStringBuffer>> results =
           toLdifBytesInvoker.processAll(ldifRecords);
      for (final Result<LDIFRecord,ByteStringBuffer> result: results)
      {
        rethrow(result.getFailureCause());

        final ByteStringBuffer encodedBytes = result.getOutput();
        if (encodedBytes != null)
        {
          encodedBytes.write(writer);
          writer.write(EOL_BYTES);
        }
      }
    }
  }




  /**
   * Writes the provided comment to the LDIF target, wrapping long lines as
   * necessary.
   *
   * @param  comment      The comment to be written to the LDIF target.  It must
   *                      not be {@code null}.
   * @param  spaceBefore  Indicates whether to insert a blank line before the
   *                      comment.
   * @param  spaceAfter   Indicates whether to insert a blank line after the
   *                      comment.
   *
   * @throws  IOException  If a problem occurs while writing the LDIF data.
   */
  public void writeComment(final String comment, final boolean spaceBefore,
                           final boolean spaceAfter)
         throws IOException
  {
    ensureNotNull(comment);
    if (spaceBefore)
    {
      writer.write(EOL_BYTES);
    }

    //
    // Check for a newline explicitly to avoid the overhead of the regex
    // for the common case of a single-line comment.
    //

    if (comment.indexOf('\n') < 0)
    {
      writeSingleLineComment(comment);
    }
    else
    {
      //
      // Split on blank lines and wrap each line individually.
      //

      final String[] lines = comment.split("\\r?\\n");
      for (final String line: lines)
      {
        writeSingleLineComment(line);
      }
    }

    if (spaceAfter)
    {
      writer.write(EOL_BYTES);
    }
  }



  /**
   * Writes the provided comment to the LDIF target, wrapping long lines as
   * necessary.
   *
   * @param  comment      The comment to be written to the LDIF target.  It must
   *                      not be {@code null}, and it must not include any line
   *                      breaks.
   *
   * @throws  IOException  If a problem occurs while writing the LDIF data.
   */
  private void writeSingleLineComment(final String comment)
          throws IOException
  {
    // We will always wrap comments, even if we won't wrap LDIF entries.  If
    // there is a wrap column set, then use it.  Otherwise use 79 characters,
    // and back off two characters for the "# " at the beginning.
    final int commentWrapMinusTwo;
    if (wrapColumn <= 0)
    {
      commentWrapMinusTwo = 77;
    }
    else
    {
      commentWrapMinusTwo = wrapColumnMinusTwo;
    }

    buffer.clear();
    final int length = comment.length();
    if (length <= commentWrapMinusTwo)
    {
      buffer.append("# ");
      buffer.append(comment);
      buffer.append(EOL_BYTES);
    }
    else
    {
      int minPos = 0;
      while (minPos < length)
      {
        if ((length - minPos) <= commentWrapMinusTwo)
        {
          buffer.append("# ");
          buffer.append(comment.substring(minPos));
          buffer.append(EOL_BYTES);
          break;
        }

        // First, adjust the position until we find a space.  Go backwards if
        // possible, but if we can't find one there then go forward.
        boolean spaceFound = false;
        final int pos = minPos + commentWrapMinusTwo;
        int     spacePos   = pos;
        while (spacePos > minPos)
        {
          if (comment.charAt(spacePos) == ' ')
          {
            spaceFound = true;
            break;
          }

          spacePos--;
        }

        if (! spaceFound)
        {
          spacePos = pos + 1;
          while (spacePos < length)
          {
            if (comment.charAt(spacePos) == ' ')
            {
              spaceFound = true;
              break;
            }

            spacePos++;
          }

          if (! spaceFound)
          {
            // There are no spaces at all in the remainder of the comment, so
            // we'll just write the remainder of it all at once.
            buffer.append("# ");
            buffer.append(comment.substring(minPos));
            buffer.append(EOL_BYTES);
            break;
          }
        }

        // We have a space, so we'll write up to the space position and then
        // start up after the next space.
        buffer.append("# ");
        buffer.append(comment.substring(minPos, spacePos));
        buffer.append(EOL_BYTES);

        minPos = spacePos + 1;
        while ((minPos < length) && (comment.charAt(minPos) == ' '))
        {
          minPos++;
        }
      }
    }

    buffer.write(writer);
  }



  /**
   * Writes the provided record to the LDIF target, wrapping long lines as
   * necessary.
   *
   * @param  record  The LDIF record to be written.
   *
   * @throws  IOException  If a problem occurs while writing the LDIF data.
   */
  private void writeLDIF(final LDIFRecord record)
          throws IOException
  {
    buffer.clear();
    record.toLDIF(buffer, wrapColumn);
    buffer.append(EOL_BYTES);
    buffer.write(writer);
  }



  /**
   * Performs any appropriate wrapping for the provided set of LDIF lines.
   *
   * @param  wrapColumn  The column at which to wrap long lines.  A value that
   *                     is less than or equal to two indicates that no
   *                     wrapping should be performed.
   * @param  ldifLines   The set of lines that make up the LDIF data to be
   *                     wrapped.
   *
   * @return  A new list of lines that have been wrapped as appropriate.
   */
  public static List<String> wrapLines(final int wrapColumn,
                                       final String... ldifLines)
  {
    return wrapLines(wrapColumn, Arrays.asList(ldifLines));
  }



  /**
   * Performs any appropriate wrapping for the provided set of LDIF lines.
   *
   * @param  wrapColumn  The column at which to wrap long lines.  A value that
   *                     is less than or equal to two indicates that no
   *                     wrapping should be performed.
   * @param  ldifLines   The set of lines that make up the LDIF data to be
   *                     wrapped.
   *
   * @return  A new list of lines that have been wrapped as appropriate.
   */
  public static List<String> wrapLines(final int wrapColumn,
                                       final List<String> ldifLines)
  {
    if (wrapColumn <= 2)
    {
      return new ArrayList<String>(ldifLines);
    }

    final ArrayList<String> newLines = new ArrayList<String>(ldifLines.size());
    for (final String s : ldifLines)
    {
      final int length = s.length();
      if (length <= wrapColumn)
      {
        newLines.add(s);
        continue;
      }

      newLines.add(s.substring(0, wrapColumn));

      int pos = wrapColumn;
      while (pos < length)
      {
        if ((length - pos + 1) <= wrapColumn)
        {
          newLines.add(' ' + s.substring(pos));
          break;
        }
        else
        {
          newLines.add(' ' + s.substring(pos, (pos+wrapColumn-1)));
          pos += wrapColumn - 1;
        }
      }
    }

    return newLines;
  }



  /**
   * Creates a string consisting of the provided attribute name followed by
   * either a single colon and the string representation of the provided value,
   * or two colons and the base64-encoded representation of the provided value.
   *
   * @param  name   The name for the attribute.
   * @param  value  The value for the attribute.
   *
   * @return  A string consisting of the provided attribute name followed by
   *          either a single colon and the string representation of the
   *          provided value, or two colons and the base64-encoded
   *          representation of the provided value.
   */
  public static String encodeNameAndValue(final String name,
                                          final ASN1OctetString value)
  {
    final StringBuilder buffer = new StringBuilder();
    encodeNameAndValue(name, value, buffer);
    return buffer.toString();
  }



  /**
   * Appends a string to the provided buffer consisting of the provided
   * attribute name followed by either a single colon and the string
   * representation of the provided value, or two colons and the base64-encoded
   * representation of the provided value.
   *
   * @param  name    The name for the attribute.
   * @param  value   The value for the attribute.
   * @param  buffer  The buffer to which the name and value are to be written.
   */
  public static void encodeNameAndValue(final String name,
                                        final ASN1OctetString value,
                                        final StringBuilder buffer)
  {
    encodeNameAndValue(name, value, buffer, 0);
  }



  /**
   * Appends a string to the provided buffer consisting of the provided
   * attribute name followed by either a single colon and the string
   * representation of the provided value, or two colons and the base64-encoded
   * representation of the provided value.
   *
   * @param  name        The name for the attribute.
   * @param  value       The value for the attribute.
   * @param  buffer      The buffer to which the name and value are to be
   *                     written.
   * @param  wrapColumn  The column at which to wrap long lines.  A value that
   *                     is less than or equal to two indicates that no
   *                     wrapping should be performed.
   */
  public static void encodeNameAndValue(final String name,
                                        final ASN1OctetString value,
                                        final StringBuilder buffer,
                                        final int wrapColumn)
  {
    final int bufferStartPos = buffer.length();

    try
    {
      buffer.append(name);
      buffer.append(':');

      final byte[] valueBytes = value.getValue();
      final int length = valueBytes.length;
      if (length == 0)
      {
        buffer.append(' ');
        return;
      }

      // If the value starts with a space, colon, or less-than character, then
      // it must be base64-encoded.
      switch (valueBytes[0])
      {
        case ' ':
        case ':':
        case '<':
          buffer.append(": ");
          Base64.encode(valueBytes, buffer);
          return;
      }

      // If the value ends with a space, then it should be base64-encoded.
      if (valueBytes[length-1] == ' ')
      {
        buffer.append(": ");
        Base64.encode(valueBytes, buffer);
        return;
      }

      // If any character in the value is outside the ASCII range, or is the
      // NUL, LF, or CR character, then the value should be base64-encoded.
      for (int i=0; i < length; i++)
      {
        if ((valueBytes[i] & 0x7F) != (valueBytes[i] & 0xFF))
        {
          buffer.append(": ");
          Base64.encode(valueBytes, buffer);
          return;
        }

        switch (valueBytes[i])
        {
          case 0x00:  // The NUL character
          case 0x0A:  // The LF character
          case 0x0D:  // The CR character
            buffer.append(": ");
            Base64.encode(valueBytes, buffer);
            return;
        }
      }

      // If we've gotten here, then the string value is acceptable.
      buffer.append(' ');
      buffer.append(value.stringValue());
    }
    finally
    {
      if (wrapColumn > 2)
      {
        final int length = buffer.length() - bufferStartPos;
        if (length > wrapColumn)
        {
          final String EOL_PLUS_SPACE = EOL + ' ';
          buffer.insert((bufferStartPos+wrapColumn), EOL_PLUS_SPACE);

          int pos = bufferStartPos + (2*wrapColumn) +
                    EOL_PLUS_SPACE.length() - 1;
          while (pos < buffer.length())
          {
            buffer.insert(pos, EOL_PLUS_SPACE);
            pos += (wrapColumn - 1 + EOL_PLUS_SPACE.length());
          }
        }
      }
    }
  }



  /**
   * Appends a string to the provided buffer consisting of the provided
   * attribute name followed by either a single colon and the string
   * representation of the provided value, or two colons and the base64-encoded
   * representation of the provided value.  It may optionally be wrapped at the
   * specified column.
   *
   * @param  name        The name for the attribute.
   * @param  value       The value for the attribute.
   * @param  buffer      The buffer to which the name and value are to be
   *                     written.
   * @param  wrapColumn  The column at which to wrap long lines.  A value that
   *                     is less than or equal to two indicates that no
   *                     wrapping should be performed.
   */
  public static void encodeNameAndValue(final String name,
                                        final ASN1OctetString value,
                                        final ByteStringBuffer buffer,
                                        final int wrapColumn)
  {
    final int bufferStartPos = buffer.length();

    try
    {
      buffer.append(name);
      encodeValue(value, buffer);
    }
    finally
    {
      if (wrapColumn > 2)
      {
        final int length = buffer.length() - bufferStartPos;
        if (length > wrapColumn)
        {
          final byte[] EOL_BYTES_PLUS_SPACE = new byte[EOL_BYTES.length + 1];
          System.arraycopy(EOL_BYTES, 0, EOL_BYTES_PLUS_SPACE, 0,
                           EOL_BYTES.length);
          EOL_BYTES_PLUS_SPACE[EOL_BYTES.length] = ' ';

          buffer.insert((bufferStartPos+wrapColumn), EOL_BYTES_PLUS_SPACE);

          int pos = bufferStartPos + (2*wrapColumn) +
                    EOL_BYTES_PLUS_SPACE.length - 1;
          while (pos < buffer.length())
          {
            buffer.insert(pos, EOL_BYTES_PLUS_SPACE);
            pos += (wrapColumn - 1 + EOL_BYTES_PLUS_SPACE.length);
          }
        }
      }
    }
  }



  /**
   * Appends a string to the provided buffer consisting of the properly-encoded
   * representation of the provided value, including the necessary colon(s) and
   * space that precede it.  Depending on the content of the value, it will
   * either be used as-is or base64-encoded.
   *
   * @param  value   The value for the attribute.
   * @param  buffer  The buffer to which the value is to be written.
   */
  static void encodeValue(final ASN1OctetString value,
                          final ByteStringBuffer buffer)
  {
    buffer.append(':');

    final byte[] valueBytes = value.getValue();
    final int length = valueBytes.length;
    if (length == 0)
    {
      buffer.append(' ');
      return;
    }

    // If the value starts with a space, colon, or less-than character, then
    // it must be base64-encoded.
    switch (valueBytes[0])
    {
      case ' ':
      case ':':
      case '<':
        buffer.append(':');
        buffer.append(' ');
        Base64.encode(valueBytes, buffer);
        return;
    }

    // If the value ends with a space, then it should be base64-encoded.
    if (valueBytes[length-1] == ' ')
    {
      buffer.append(':');
      buffer.append(' ');
      Base64.encode(valueBytes, buffer);
      return;
    }

    // If any character in the value is outside the ASCII range, or is the
    // NUL, LF, or CR character, then the value should be base64-encoded.
    for (int i=0; i < length; i++)
    {
      if ((valueBytes[i] & 0x7F) != (valueBytes[i] & 0xFF))
      {
        buffer.append(':');
        buffer.append(' ');
        Base64.encode(valueBytes, buffer);
        return;
      }

      switch (valueBytes[i])
      {
        case 0x00:  // The NUL character
        case 0x0A:  // The LF character
        case 0x0D:  // The CR character
          buffer.append(':');
          buffer.append(' ');
          Base64.encode(valueBytes, buffer);
          return;
      }
    }

    // If we've gotten here, then the string value is acceptable.
    buffer.append(' ');
    buffer.append(valueBytes);
  }



  /**
   * If the provided exception is non-null, then it will be rethrown as an
   * unchecked exception or an IOException.
   *
   * @param t  The exception to rethrow as an an unchecked exception or an
   *           IOException or {@code null} if none.
   *
   * @throws IOException  If t is a checked exception.
   */
  static void rethrow(final Throwable t)
         throws IOException
  {
    if (t == null)
    {
      return;
    }

    if (t instanceof IOException)
    {
      throw (IOException) t;
    }
    else if (t instanceof RuntimeException)
    {
      throw (RuntimeException) t;
    }
    else if (t instanceof Error)
    {
      throw (Error) t;
    }
    else
    {
      throw createIOExceptionWithCause(null, t);
    }
  }
}
