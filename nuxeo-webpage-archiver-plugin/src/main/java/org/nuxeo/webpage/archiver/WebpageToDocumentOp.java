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

package org.nuxeo.webpage.archiver;

import java.io.IOException;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.runtime.api.Framework;

/**
 * Asynchronously converts a URL to PDF, and stores the PDF in the input Document
 * <p>
 * Please, see the comments of {@link WebpageToBlob} for details about the usage of the wkhtmltopdf commandline
 * 
 * @since 7.10
 */
@Operation(id = WebpageToDocumentOp.ID, category = Constants.CAT_CONVERSION, label = "Webpage to Document", description = "Read the distant web page and save it as a pdf in the xpath field of input document. This is always an asynchronous operation running in a worker. When it is done, it fires the webpageArchived event. Returns the input document (unchanged)")
public class WebpageToDocumentOp {

    public static final String ID = "WebpageToDocument";

    @Param(name = "url", required = true)
    protected String url;

    @Param(name = "fileName", required = false)
    protected String fileName;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    @Param(name = "timeoutMilliSecs", required = false)
    protected long timeoutMilliSecs = WebpageToBlob.TIMEOUT_DEFAULT;

    @OperationMethod
    public DocumentModel run(DocumentModel inDoc) throws IOException, CommandNotAvailable {

        WebpageToBlobWork work = new WebpageToBlobWork(url, inDoc.getRepositoryName(), inDoc.getId(), xpath, fileName,
                (int) timeoutMilliSecs);
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        workManager.schedule(work, Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);

        return inDoc;
    }

}
