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

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;

/**
 * Convert a distant URL to PDF, returns the blob. WARNING: This runs <i>synchronously</i>. If the conversion takes
 * time, caller will have to wait.
 * <p>
 * If access to <code>url</code> must be authenticated, a previous call to WebpageToBlob.Login must have returned Blob,
 * to pass in the <code>cookieJar</code> parameter.
 * <p>
 * Please, see the comments of {@link WebpageToBlob} for details about the usage of the wkhtmltopdf command line
 * 
 * @since 7.10
 */
@Operation(id = WebpageToPdfOp.ID, category = Constants.CAT_CONVERSION, label = "Webpage to Pdf", description = "Read the distant web page and save it as a pdf. Default commandline contribution is used if a command line is not provided. If the page requests authentication, a previous call to WebpageToBlob.Login must have returned the cookieJar blob.")
public class WebpageToPdfOp {

    public static final String ID = "WebpageToPdf";

    @Param(name = "commandLine", required = false)
    protected String commandLine;

    @Param(name = "url", required = true)
    protected String url;

    @Param(name = "fileName", required = false)
    protected String fileName;

    @Param(name = "cookieJar", required = false)
    protected Blob cookieJar;

    @OperationMethod
    public Blob run() throws IOException, CommandNotAvailable, NuxeoException {

        if (cookieJar != null && StringUtils.isBlank(commandLine)) {
            commandLine = "wkhtmlToPdf-authenticated";
        }
        WebpageToBlob wptopdf = new WebpageToBlob();
        return wptopdf.toPdf(commandLine, url, fileName, cookieJar);
    }

}
