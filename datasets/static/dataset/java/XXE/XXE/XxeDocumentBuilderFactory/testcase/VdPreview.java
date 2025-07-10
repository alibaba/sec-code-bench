<filename>sdk-common/src/main/java/com/android/ide/common/vectordrawable/VdPreview.java<fim_prefix>

/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.common.vectordrawable;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.util.AssetUtil;
import com.google.common.base.Charsets;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Locale;

/**
 * Generate a Image based on the VectorDrawable's XML content.
 *
 * <p>This class also contains a main method, which can be used to preview a vector drawable file.
 */
public class VdPreview {

    private static final String ANDROID_ALPHA = "android:alpha";
    private static final String ANDROID_AUTO_MIRRORED = "android:autoMirrored";
    private static final String ANDROID_HEIGHT = "android:height";
    private static final String ANDROID_WIDTH = "android:width";
    public static final int MAX_PREVIEW_IMAGE_SIZE = 4096;
    public static final int MIN_PREVIEW_IMAGE_SIZE = 1;

    /**
     * Parse the VectorDrawable's XML file into a document object.
     *
     * @param xmlFileContent the content of the VectorDrawable's XML file.
     * @param errorLog when errors were found, log them in this builder if it is not null.
     * @return parsed document or null if errors happened.
     */
    @Nullable
    public static Document parseVdStringIntoDocument(@NonNull String xmlFileContent,
                                                     @Nullable StringBuilder errorLog) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        <fim_suffix>
        DocumentBuilder db;
        Document document;
        try {
            db = dbf.newDocumentBuilder();
            document = db.parse(new InputSource(new StringReader(xmlFileContent)));
        }
        catch (Exception e) {
            if (errorLog != null) {
                errorLog.append("Exception while parsing XML file:\n").append(e.getMessage());
            }
            return null;
        }
        return document;
    }

    /**
     * This encapsulates the information used to determine the preview image size.
     * The reason we have different ways here is that both Studio UI and build process need
     * to use this common code path to generate images for vectordrawable.
     * When {@code mUseWidth} is true, use {@code mImageMaxDimension} as the maximum
     * dimension value while keeping the aspect ratio.
     * Otherwise, use {@code mImageScale} to scale the image based on the XML's size information.
     */
    public static class TargetSize {
        private boolean mUseWidth;

        private int mImageMaxDimension;
        private float mImageScale;

        private TargetSize(boolean useWidth, int imageWidth, float imageScale) {
            mUseWidth = useWidth;
            mImageMaxDimension = imageWidth;
            mImageScale = imageScale;
        }

        public static TargetSize createSizeFromWidth(int imageWidth) {
            return new TargetSize(true, imageWidth, 0.0f);
        }

        public static TargetSize createSizeFromScale(float imageScale) {
            return new TargetSize(false, 0, imageScale);
        }
    }

    /**
     * Since we allow overriding the vector drawable's size, we also need to keep
     * the original size and aspect ratio.
     */
    public static class SourceSize {
        public int getHeight() {
            return mSourceHeight;
        }

        public int getWidth() {
            return mSourceWidth;
        }

        private int mSourceWidth;
        private int mSourceHeight;
    }


    /**
     * @return a format object for XML formatting.
     */
    @NonNull
    private static OutputFormat getPrettyPrintFormat() {
        OutputFormat format = new OutputFormat();
        format.setLineWidth(120);
        format.setIndenting(true);
        format.setIndent(4);
        format.setEncoding("UTF-8");
        format.setOmitComments(true);
        format.setOmitXMLDeclaration(true);
        return format;
    }

    /**
     * Get the vector drawable's original size.
     */
    public static SourceSize getVdOriginalSize(@NonNull Document document) {
        Element root = document.getDocumentElement();
        SourceSize srcSize = new SourceSize();
        // Update attributes, note that attributes as width and height are required,
        // while others are optional.
        NamedNodeMap attr = root.getAttributes();
        Node nodeAttr = attr.getNamedItem(ANDROID_WIDTH);
        assert nodeAttr != null;
        srcSize.mSourceWidth = parseDimension(0, nodeAttr, false);

        nodeAttr = attr.getNamedItem(ANDROID_HEIGHT);
        assert nodeAttr != null;
        srcSize.mSourceHeight = parseDimension(0, nodeAttr, false);
        return srcSize;
    }



}