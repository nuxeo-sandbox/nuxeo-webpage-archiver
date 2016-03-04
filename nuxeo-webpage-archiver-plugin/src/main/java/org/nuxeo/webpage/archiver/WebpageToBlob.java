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
 *     thibaud
 */
package org.nuxeo.webpage.archiver;

import java.io.File;

import java.io.IOException;

import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;

/**
 * Use the <code>wkhtmltopdf</code> command line (must be installed) to convert a distant webpage to pdf.
 * <p>
 * The class uses the <code>wkhtmltopdf</code> (http://wkhtmltopdf.org) command line, which, of course, must be
 * installed on your server.
 * <p>
 * <b>Important</b>: Some webpages can be complicated, can contain errors, etc. To avoid the commandline to block and
 * freeze, it is used with options forcing it to ignore errors (see OSGI-INF/commandLInes.xml), such as
 * <code>--load-media-error-handling ignore</code> and <code>--load-error-handling ignore</code>.
 * 
 * @since 7.10HF05
 */
public class WebpageToBlob {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(WebpageToBlob.class);

    public static final String COMMANDLINE_wkhtmltopdf = "wkhtmlToPdf";

    public WebpageToBlob() {

    }

    /**
     * Converts the distant URL to PDF, returns the blob of the PDF.
     * 
     * @param inUrl, the url to convert
     * @param inFileName, the fileName of the final PDF
     * @return a Blob holding the pdf
     * @throws IOException
     * @throws CommandNotAvailable
     * @throws NuxeoException
     * @since 7.10
     */
    public static Blob toPdf(String inUrl, String inFileName) throws IOException, CommandNotAvailable, NuxeoException {

        // Create a temp. File handled by Nuxeo
        Blob resultPdf = Blobs.createBlobWithExtension(".pdf");

        CmdParameters params = new CmdParameters();
        params.addNamedParameter("url", inUrl);
        params.addNamedParameter("targetFilePath", resultPdf.getFile().getAbsolutePath());

        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        ExecResult execResult = cles.execCommand(COMMANDLINE_wkhtmltopdf, params);

        // WARNING
        // wkhtmltopdf can return a non zero code while the execution went totally OK and we
        // have a valid pdf. The problem is that the CommandLineExecutorService assumes a
        // non-zero return code is an error => we must handle the thing by ourselves, basically
        // just checking if we do have a comparison file created by wkhtmltopdf
        // *BUT* still, maybe it can happen a real error is returned and the pdf is invalid. So
        // we just check the size, but maybe we could use PDFBox to check the pdf
        File tempDestFile = resultPdf.getFile();
        if (!tempDestFile.exists() || tempDestFile.length() < 1) {
            throw new NuxeoException("Failed to execute the command <" + COMMANDLINE_wkhtmltopdf
                    + ">. Final command [ " + execResult.getCommandLine() + " ] returned with error "
                    + execResult.getReturnCode(), execResult.getError());
        }

        resultPdf.setMimeType("application/pdf");
        if (StringUtils.isBlank(inFileName)) {
            URL urlObj = new URL(inUrl);
            inFileName = StringUtils.replace(urlObj.getHost(), ".", "-") + ".pdf";
        }
        if (StringUtils.isNotBlank(inFileName)) {
            resultPdf.setFilename(inFileName);
        }

        return resultPdf;

    }

}
