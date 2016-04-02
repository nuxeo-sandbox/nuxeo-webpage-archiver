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

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.runtime.api.Framework;

/**
 * Asynchronously converts a URL to PDF, and stores the PDF in the input Document
 * <p>
 * Please, see the comments of {@link WebpageToBlob} for details about the usage of the wkhtmltopdf commandline
 * <p>
 * If access to <code>url</code> must be authenticated, a previous call to WebpageToBlob.Login must have returned Blob,
 * to pass in the <code>cookieJar</code> parameter.
 * <p>
 * If the command takes more than timeoutMillisecs, it is forced to terminate. Default value is 30000 ms
 * 
 * @since 7.10
 */
@Operation(id = WebpageToDocumentOp.ID, category = Constants.CAT_CONVERSION, label = "Webpage to Document", description = "Read the distant web page and save it as a pdf in the xpath field of input document. Default timeout is 30000ms. This is always an asynchronous operation running in a worker. When it is done, it fires the webpageArchived event. Returns the input document (unchanged)")
public class WebpageToDocumentOp {

    public static final String ID = "WebpageToDocument";

    @Param(name = "commandLine", required = false)
    protected String commandLine;

    @Param(name = "url", required = true)
    protected String url;

    @Param(name = "fileName", required = false)
    protected String fileName;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    @Param(name = "cookieJar", required = false)
    protected Blob cookieJar;

    @Param(name = "timeout", required = false)
    protected Long timeout;

    @OperationMethod
    public DocumentModel run(DocumentModel inDoc) throws IOException, CommandNotAvailable {

        WebpageToBlobWork work = new WebpageToBlobWork(commandLine, url, inDoc.getRepositoryName(), inDoc.getId(),
                xpath, fileName, cookieJar);
        if (timeout != null && timeout.longValue() != 0) {
            work.setTimeout(timeout.intValue());
        }
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        workManager.schedule(work, Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);

        return inDoc;
    }

}
