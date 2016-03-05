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

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineDescriptor;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineExecutorComponent;
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
 * <p>
 * Despite these adjustements, the wkhtmltopdf command line can still freeze when an occurs (instead of quitting). This
 * means: The Nuxeo thread calling the command line also is blocked and never returns. The
 * <code>CommandLineExecutorService</code> does not handle a timeout, and it is not that straightforward to handle such
 * timeout in a cross platform way (at least Linux and Windows). This is why we use Apache Common Exec instead.
 * <p>
 * We sill have the XML contribution declaring the command line (so we benefit of the test of its existence in the
 * system for example), we just get the <code><parameterString</code> attribute and replace the variables place holders
 * 
 * @since 7.10HF05
 */
public class WebpageToBlob {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(WebpageToBlob.class);

    public static final String COMMANDLINE_wkhtmltopdf = "wkhtmlToPdf";

    // Must be the same as in the commandLines.xml contribution
    public static final String COMMANDLINE_PARAM_URL = "#{url}";

    public static final String COMMANDLINE_PARAM_TARGET_FILE_PATH = "#{targetFilePath}";

    // 30s timeout by default
    public static final int TIMEOUT_DEFAULT = 30000;

    protected int timeout = TIMEOUT_DEFAULT;

    protected static String parameterString = null;

    /**
     * Default constructor, using the default execution timeout
     */
    public WebpageToBlob() {
        this(0);
    }

    /**
     * Constructor letting the caller change the default timeout
     * <p>
     * See {@link setTimeout}
     * <p>
     * 
     * @param inTimeout
     */
    public WebpageToBlob(int inTimeout) {

        setTimeout(inTimeout);
        setupParameterString();
    }

    protected void setupParameterString() {

        if (parameterString == null) {
            CommandLineDescriptor desc = CommandLineExecutorComponent.getCommandDescriptor(COMMANDLINE_wkhtmltopdf);
            parameterString = desc.getParametersString();
        }
    }

    public static boolean isAvailable() {

        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        CommandAvailability ca = cles.getCommandAvailability(WebpageToBlob.COMMANDLINE_wkhtmltopdf);
        return ca.isAvailable();
    }

    /**
     * Converts the distant URL to PDF, returns the blob of the PDF. if the command takes more time to run than the
     * timeout, it is killed.
     * 
     * @param inUrl, the url to convert
     * @param inFileName, the fileName of the final PDF. Optionnal.
     * @return a Blob holding the pdf
     * @throws IOException
     * @throws CommandNotAvailable
     * @throws NuxeoException
     * @since 7.10
     */
    public Blob toPdf(String inUrl, String inFileName) throws IOException, NuxeoException {

        Exception exception = null;

        Blob resultPdf = Blobs.createBlobWithExtension(".pdf");

        String params = StringUtils.replace(parameterString, COMMANDLINE_PARAM_URL, inUrl);
        params = StringUtils.replace(params, COMMANDLINE_PARAM_TARGET_FILE_PATH, resultPdf.getFile().getAbsolutePath());

        String line = "wkhtmltopdf " + params;
        CommandLine cmdLine = CommandLine.parse(line);

        DefaultExecutor executor = new DefaultExecutor();
        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
        // We do't want a check on exit values, because a PDF can still be created with errors
        // (can't get a font, ...)
        executor.setExitValues(null);
        executor.setWatchdog(watchdog);
        int exitValue = 0;
        try {
            exitValue = executor.execute(cmdLine);
        } catch (IOException e) {
            exception = e;
        }
        // Even if we had no error catched, we must check if the pdf is valid.
        // Exit value may be 1, or non zero while the pdf was created. But maybe
        // a font could not be correctly rendered, etc. Let's check if we have
        // something in the pdf
        File tempDestFile = resultPdf.getFile();
        if (!pdfLooksValid(tempDestFile)) {
            resultPdf = null;
            String msg = "Failed to execute the command line [" + cmdLine.toString()
                    + " ]. No valid PDF generated. exitValue: " + exitValue;
            if (exitValue == 143) { // On linux: Timeout, wkhtmltopdf was SIGTERM
                msg += " (time out reached. The timeout was " + timeout + "ms)";
            }
            if (exception == null) {
                throw new NuxeoException(msg);
            } else {
                throw new NuxeoException(msg, exception);
            }
        }

        return resultPdf;
    }

    protected boolean pdfLooksValid(File inPdf) {

        boolean valid = false;

        if (inPdf.exists() && inPdf.length() > 0) {
            try {
                PDDocument pdfDoc = PDDocument.load(inPdf);
                if (pdfDoc.getNumberOfPages() > 0) {
                    valid = true;
                }
            } catch (IOException e) {
                // Nothing
            }
        }

        return valid;
    }

    /**
     * If the new value is < 1000 (1s), it is realigned to TIMEOUT_DEFAULT
     * 
     * @param newValue
     * @since 7.10
     */
    public void setTimeout(int newValue) {
        timeout = newValue < 1000 ? TIMEOUT_DEFAULT : newValue;
    }

}
