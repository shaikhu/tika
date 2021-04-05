/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.parser.image;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.drew.metadata.Directory;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.jpeg.JpegCommentDirectory;
import org.junit.Test;
import org.mockito.Mockito;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;

public class ImageMetadataExtractorTest {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testHandleDirectories() throws MetadataException {
        Metadata metadata = Mockito.mock(Metadata.class);
        ImageMetadataExtractor.DirectoryHandler handler1 =
                Mockito.mock(ImageMetadataExtractor.DirectoryHandler.class);
        ImageMetadataExtractor e = new ImageMetadataExtractor(metadata, handler1);

        Directory directory = new JpegCommentDirectory();
        Iterator directories = Mockito.mock(Iterator.class);
        Mockito.when(directories.hasNext()).thenReturn(true, false);
        Mockito.when(directories.next()).thenReturn(directory);
        Mockito.when(handler1.supports(JpegCommentDirectory.class)).thenReturn(true);

        e.handle(directories);
        Mockito.verify(handler1).supports(JpegCommentDirectory.class);
        Mockito.verify(handler1).handle(directory, metadata);
    }

    @Test
    public void testExifHandlerSupports() {
        assertTrue(new ImageMetadataExtractor.ExifHandler().supports(ExifIFD0Directory.class));
        assertTrue(new ImageMetadataExtractor.ExifHandler().supports(ExifSubIFDDirectory.class));
        assertFalse(new ImageMetadataExtractor.ExifHandler().supports(Directory.class));
        assertFalse(new ImageMetadataExtractor.ExifHandler().supports(JpegCommentDirectory.class));
    }

    @Test
    public void testExifHandlerParseDate() throws MetadataException {
        ExifSubIFDDirectory exif = Mockito.mock(ExifSubIFDDirectory.class);
        Mockito.when(exif.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)).thenReturn(true);
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getDefault(), Locale.ROOT);
        calendar.setTimeInMillis(0);
        calendar.set(2000, 0, 1, 0, 0, 0);
        Mockito.when(exif.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL))
                .thenReturn(calendar.getTime()); // jvm default timezone as in Metadata Extractor
        Metadata metadata = new Metadata();

        new ImageMetadataExtractor.ExifHandler().handle(exif, metadata);
        assertEquals("Should be ISO date without time zone", "2000-01-01T00:00:00",
                metadata.get(TikaCoreProperties.CREATED));
    }

    @Test
    public void testExifHandlerParseDateFallback() throws MetadataException {
        ExifIFD0Directory exif = Mockito.mock(ExifIFD0Directory.class);
        Mockito.when(exif.containsTag(ExifIFD0Directory.TAG_DATETIME)).thenReturn(true);
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getDefault(), Locale.ROOT);
        calendar.setTimeInMillis(0);
        calendar.set(1999, 0, 1, 0, 0, 0);
        Mockito.when(exif.getDate(ExifIFD0Directory.TAG_DATETIME))
                .thenReturn(calendar.getTime()); // jvm default timezone as in Metadata Extractor
        Metadata metadata = new Metadata();

        new ImageMetadataExtractor.ExifHandler().handle(exif, metadata);
        assertEquals("Should try EXIF Date/Time if Original is not set", "1999-01-01T00:00:00",
                metadata.get(TikaCoreProperties.CREATED));
    }

    @Test
    public void testExifHandlerParseDateError() throws MetadataException {
        ExifIFD0Directory exif = Mockito.mock(ExifIFD0Directory.class);
        Mockito.when(exif.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)).thenReturn(true);
        Mockito.when(exif.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)).thenReturn(null);
        Metadata metadata = new Metadata();

        new ImageMetadataExtractor.ExifHandler().handle(exif, metadata);
        assertEquals("Parsing should proceed without date", null,
                metadata.get(TikaCoreProperties.CREATED));
    }

    @Test
    public void testCopyUnknownFieldsHandler() throws MetadataException {
        Directory d = Mockito.mock(Directory.class);
        Tag t1 = Mockito.mock(Tag.class);
        Mockito.when(t1.getTagName()).thenReturn("Image Description");
        Mockito.when(t1.getDescription()).thenReturn("t1");
        Tag t2 = Mockito.mock(Tag.class);
        Mockito.when(t2.getTagName()).thenReturn(TikaCoreProperties.SUBJECT.toString());
        Mockito.when(t2.getDescription()).thenReturn("known");
        Tag t3 = Mockito.mock(Tag.class);
        Mockito.when(t3.getTagName()).thenReturn(TikaCoreProperties.DESCRIPTION.getName());
        Mockito.when(t3.getDescription()).thenReturn("known");
        List<Tag> tags = Arrays.asList(t1, t2, t3);
        Mockito.when(d.getTags()).thenReturn(tags);
        Metadata metadata = new Metadata();
        new ImageMetadataExtractor.CopyUnknownFieldsHandler().handle(d, metadata);
        assertEquals("t1", metadata.get("Image Description"));
        assertNull("keywords should be excluded from bulk copy because it is a defined field",
                metadata.get(TikaCoreProperties.SUBJECT));
        assertNull(metadata.get(TikaCoreProperties.DESCRIPTION));
    }

}
