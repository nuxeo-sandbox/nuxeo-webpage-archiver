/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thibaud Arguillere
 */
package org.nuxeo.webpage.archiver.test;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.nuxeo.ecm.core.api.Blob;

/**
 * @since 7.10
 */
public class Utils {

    public static boolean hasText(Blob inBlob, String inText) throws IOException {

        boolean hasIt = false;
        PDDocument pdfDoc = null;

        try {
            pdfDoc = PDDocument.load(inBlob.getFile());
            PDFTextStripper stripper = new PDFTextStripper();
            String txt;
            int max = pdfDoc.getNumberOfPages();
            for (int i = 1; i <= max; ++i) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                txt = stripper.getText(pdfDoc);
                if (txt.indexOf(inText) > -1) {
                    hasIt = true;
                    break;
                }
            }
        } finally {
            if (pdfDoc != null) {
                pdfDoc.close();
            }
        }

        return hasIt;
    }

}
