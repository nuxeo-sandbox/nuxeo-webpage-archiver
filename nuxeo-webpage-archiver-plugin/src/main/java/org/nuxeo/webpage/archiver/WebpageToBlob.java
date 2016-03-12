/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thibaud Arguillere
 */
package org.nuxeo.webpage.archiver;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;

/**
 * Use the <code>wkhtmltopdf</code>
 * (http://wkhtmltopdf.org) command line (must be installed) to convert a distant webpage to pdf.
 * <p>
 * <b>Important</b>: Some webpages can be complicated, can contain errors, etc. To avoid the commandline to block and
 * freeze, it is used with options forcing it to ignore errors (see OSGI-INF/commandLines.xml), such as
 * <code>--load-media-error-handling ignore</code> and <code>--load-error-handling ignore</code>. Also, launching the
 * command in quiet mode is a requirement (<code>-q</code> option)
 * <p>
 * Also, the commandline can create a valid and complete pdf but still return an error code. This class does not rely
 * on the exitValue returned by wkhtmltopdf. Instead, it checks the resulting pdf.
 * <p>
 * <b>Pages Requiring Authentication<b>
 * <p>
 * <khtmltopdf allows handling such pages by using 2 steps:
 * <ul>
 * <li>First call with login informations. This creates a "cookie jar" file</li>
 * <li>Use this cookie jar in further authenticated calls</li>
 * </ul>
 * To handle this, which requires user and password to be exchanged, you must declare as many commandLine XML as you
 * need, o user/pwd are saved server-side, and pass the command lien name to the {@link login} method. This way no
 * exchange with a browser for example. See {@link login} for more information
 * <p>
 * <b>NOTICE</b>
 * <ul>
 * <li>In all cases, the command line <i>must</i> use "#{url}" and "#{targetFilePath}" (the later is handled by the
 * plug-in)</li>
 * <li>We strongly recommend to use the <code>-q</code> option as a minimum</li>
 * 
 * @since 7.10HF05
 */
public class WebpageToBlob {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(WebpageToBlob.class);

    public static final String COMMANDLINE_DEFAULT_wkhtmltopdf = "wkhtmlToPdf";

    public static final String COMMANDLINE_DEFAULT_wkhtmltopdf_AUTHENTICATED = "wkhtmlToPdf-authenticated";

    public WebpageToBlob() {

    }

    /**
     * Checks the availability of the default command line
     * 
     * @return
     * @since 7.10
     */
    public static boolean isAvailable() {

        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        CommandAvailability ca = cles.getCommandAvailability(WebpageToBlob.COMMANDLINE_DEFAULT_wkhtmltopdf);
        return ca.isAvailable();
    }

    /**
     * Converts the distant URL to PDF, returns the blob of the PDF.
     * <p>
     * If <code>inCommandLine</code> is empty, the default command is called.
     * 
     * @param inUrl, the url to convert
     * @param inFileName, the fileName of the final PDF
     * @return a Blob holding the pdf
     * @throws IOException
     * @throws CommandNotAvailable
     * @throws NuxeoException
     * @since 7.10
     */
    public Blob toPdf(String inCommandLine, String inUrl, String inFileName) throws IOException, CommandNotAvailable,
            NuxeoException {

        return run(inCommandLine, inUrl, inFileName, null);

    }

    /**
     * Converts the distant URL to PDF, returns the blob of the PDF.
     * <p>
     * To be used for pages requiring authentication (and {@link login} must have been previously called and have
     * returned the cookieJar blob).
     * <p>
     * If <code>inCommandLine</code> is empty, the default command is called.
     * 
     * @param inCommandLine
     * @param inUrl
     * @param inFileName
     * @param inCookieJar
     * @return
     * @throws IOException
     * @throws CommandNotAvailable
     * @throws NuxeoException
     * @since TODO
     */
    public Blob toPdf(String inCommandLine, String inUrl, String inFileName, Blob inCookieJar) throws IOException,
            CommandNotAvailable, NuxeoException {

        return run(inCommandLine, inUrl, inFileName, inCookieJar);

    }

    /**
     * Logins to a distant website and return a Blob, a file (named "cookie jar" by wkhtmltopdf), to be used when
     * accessing an authenticated page. You will have previously contribute the <code>inCommandLine</code> with all the
     * credentials and info.
     * <p>
     * <b>NOTE</b>: It is not easy to access authenticated pages, since wkhtmltopdf basically runs a faceless browser.
     * You must read wkhtmltopdf documentation, and also (mainly, actually) this blog:
     * http://test-mate.blogspot.com/2014/07/wkhtmltopdf-generate-pdf-of-password.html
     * <p>
     * Example: To access a page at http://my.url.com, you first have a login page. You must then:
     * <ul>
     * <li>Get the info to be sent in --post values by wkhtmltopdf. Which means, the <i>variables</i> sent in the form
     * in the POST request.</li>
     * <li>Add each of them to your commandline XML</li>
     * <li>In the command line, you must:
     * <ul>
     * <li>Hard code the URL and all the <code>--post</code> information</li>
     * <li>Use the <code>#{cookieJar}</code> parameter (<i>must be set</i>,filled by the plug-in, do not change this
     * name)</li>
     * <li>Use the <code>#{targetFilePath}</code> parameter (<i>must be set</i>,filled by the plug-in, do not change
     * this name)</li>
     * </ul>
     * So, say the form variables to send are "user_name", "user_pwd" and the submit button is "Submit", with a value of
     * "doLogin". You must have the following command:
     * <p>
     * 
     * <pre>
     * <command name="wkhtmlToPdf-login-EXAMPLE" enabled="true">
     *   <commandLine>wkhtmltopdf</commandLine>
     *   <parameterString>-q --cookie-jar #{cookieJar} --post user_name johndoe --post user-pwd 123456 --post Submit doLogin "http://my.site.com/login" "#{targetFilePath}"</parameterString>
     * </command>
     * </pre>
     * 
     * @param inCommandLine
     * @return the cookie jar (as a Blob) to be used with the next authenticated calls.
     * @throws IOException
     * @throws IOException, NuxeoException, CommandNotAvailable
     * @since 7.10
     */
    public Blob login(String inCommandLine) throws IOException, NuxeoException, CommandNotAvailable {

        if (StringUtils.isBlank(inCommandLine)) {
            throw new NuxeoException(
                    "When calling login(), a valid commandline must be passed, the default one does not handle authentification");
        }

        Blob cookieJar = Blobs.createBlobWithExtension(".jar");

        @SuppressWarnings("unused")
        Blob ignorePdf = run(inCommandLine, cookieJar);

        return cookieJar;
    }

    /*
     * This one is used only in tests, because as of today (2016-03), you can't dynamically setup login credentials from
     * unit test, when using the CommandLineService and its XML extensions. Well. You can, but it is a lot, a lot of
     * work. We secure this by checking we are running a test.
     */
    public Blob login(String inCommandLine, Properties inTestProps) throws IOException, NuxeoException,
            CommandNotAvailable {

        if (!Framework.isTestModeSet()) {
            throw new NuxeoException(
                    "A call to login(String inCommandLine, Properties inTestProps) can me made only in test mode.");
        }

        if (StringUtils.isBlank(inCommandLine)) {
            throw new NuxeoException(
                    "When calling login(), a valid commandline must be passed, the default one does not handle authentification");
        }

        Blob cookieJar = Blobs.createBlobWithExtension(".jar");

        CmdParameters params = new CmdParameters();
        params.addNamedParameter("cookieJar", cookieJar.getFile().getAbsolutePath());

        String loginUrl = inTestProps.getProperty("loginUrl");

        params.addNamedParameter("loginVar", inTestProps.getProperty("loginVar"));
        params.addNamedParameter("loginValue", inTestProps.getProperty("loginValue"));
        params.addNamedParameter("pwdVar", inTestProps.getProperty("pwdVar"));
        params.addNamedParameter("pwdValue", inTestProps.getProperty("pwdValue"));
        params.addNamedParameter("submitVar", inTestProps.getProperty("submitVar"));
        params.addNamedParameter("submitValue", inTestProps.getProperty("submitValue"));

        params.addNamedParameter("url", loginUrl);

        Blob ignorePdf = doRun(inCommandLine, params, loginUrl, null);

        return cookieJar;
    }

    /*
     * (wrapper for Blob run(String inCommandLine, String inUrl, String inFileName, Blob inCookieJar))
     */
    protected Blob run(String inCommandLine, Blob inCookieJar) throws NuxeoException, IOException, CommandNotAvailable {

        return run(inCommandLine, null, null, inCookieJar);

    }

    protected Blob run(String inCommandLine, String inUrl, String inFileName, Blob inCookieJar) throws IOException,
            CommandNotAvailable, NuxeoException {

        if (StringUtils.isBlank(inCommandLine)) {
            inCommandLine = COMMANDLINE_DEFAULT_wkhtmltopdf;
        }

        CmdParameters params = new CmdParameters();
        if (inCookieJar != null) {
            params.addNamedParameter("cookieJar", inCookieJar.getFile().getAbsolutePath());
        }
        if (StringUtils.isNotBlank(inUrl)) {
            params.addNamedParameter("url", inUrl);
        }

        return doRun(inCommandLine, params, inUrl, inFileName);

    }

    protected Blob doRun(String inCommandLine, CmdParameters inParams, String inUrl, String inFileName)
            throws IOException, CommandNotAvailable, NuxeoException {

        // Create a temp. File handled by Nuxeo
        Blob resultPdf = Blobs.createBlobWithExtension(".pdf");

        inParams.addNamedParameter("targetFilePath", resultPdf.getFile().getAbsolutePath());

        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        ExecResult execResult = cles.execCommand(inCommandLine, inParams);

        // WARNING
        // wkhtmltopdf can return a non zero code while the execution went totally OK and we
        // have a valid pdf. The problem is that the CommandLineExecutorService assumes a
        // non-zero return code is an error => we must handle the thing by ourselves, basically
        // just checking if we do have a comparison file created by wkhtmltopdf
        // *BUT* still, maybe it can happen a real error is returned and the pdf is invalid. So
        // we just check the size, but maybe we could use PDFBox to check the pdf
        if (!pdfLooksValid(resultPdf.getFile())) {
            throw new NuxeoException("Failed to execute the command <" + COMMANDLINE_DEFAULT_wkhtmltopdf
                    + ">. Final command [ " + execResult.getCommandLine() + " ] returned with error "
                    + execResult.getReturnCode(), execResult.getError());
        }

        resultPdf.setMimeType("application/pdf");
        if (StringUtils.isBlank(inFileName) && StringUtils.isNotBlank(inUrl)) {
            try {
                URL urlObj = new URL(inUrl);
                inFileName = StringUtils.replace(urlObj.getHost(), ".", "-") + ".pdf";
            } finally {
                // Nothing. Default name has been set by nuxeo
            }
        }
        if (StringUtils.isNotBlank(inFileName)) {
            resultPdf.setFilename(inFileName);
        }

        return resultPdf;
    }

    /*
     * This call is a bit expensive (in term of CPU). But as we can't rely on the exitReturn value from wkhtmltopdf, not
     * on just the size of the file, we must check the PDF looks ok. Using PDFBox here.
     */
    protected boolean pdfLooksValid(File inPdf) {

        boolean valid = false;

        if (inPdf.exists() && inPdf.length() > 0) {
            PDDocument pdfDoc = null;
            try {
                pdfDoc = PDDocument.load(inPdf);
                if (pdfDoc.getNumberOfPages() > 0) {
                    valid = true;
                }
            } catch (IOException e) {
                // Nothing
            } finally {
                if (pdfDoc != null) {
                    try {
                        pdfDoc.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }

        return valid;
    }

}