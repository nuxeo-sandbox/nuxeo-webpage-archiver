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

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

/**
 * A Nuxeo Worker, used asynchronously handle the conversion of a distant URL to PDF, saving this PDF in a Document.
 * <p>
 * Please, see the comments of {@link WebpageToBlob} for details about the usage of the wkhtmltopdf commandline
 * 
 * @since 7.10
 */
public class WebpageToBlobWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(WebpageToBlobWork.class);

    // WARNING: Must be the same as the one used in OSGI/Works.xml
    public static final String CATEGORY_WEBPAGE_TO_BLOB = "webpageToBlob";

    public static final String WEBPAGE_ARCHIVED_EVENT = "webpageArchived";

    public static final int MAX_ATTEMPTS = 3;

    protected String url;

    protected String fileName;

    protected String xpath;
    
    protected int timeout;

    protected static String computeIdPrefix(String repoName, String inDocId, String inUrl) {
        return repoName + ":" + inDocId + ":" + inUrl;
    }

    public WebpageToBlobWork(String inUrl, String repoName, String inDocId, String inXPath, String inFileName, int inTimeout) {
        super(computeIdPrefix(repoName, inDocId, inUrl));
        setDocument(repoName, inDocId);

        url = inUrl;
        xpath = inXPath;
        fileName = inFileName;
        timeout = inTimeout;
    }

    @Override
    public void work() {

        Blob pdf = null;

        setStatus("Extracting webpage");
        setProgress(Progress.PROGRESS_INDETERMINATE);

        int i, max = MAX_ATTEMPTS;
        for (i = 1; i <= max; ++i) {
            try {
                initSession(); // IN 8.1, USE openSystemSession() instead
                WebpageToBlob wptopdf = new WebpageToBlob(timeout);
                pdf = wptopdf.toPdf(url, fileName);
                commitOrRollbackTransaction();
            } catch (IOException | NuxeoException e) {
                log.error("Attempt " + i + "/" + max + ": Failed to convert the \"" + url + "\" to pdf", e);
                pdf = null;
            } finally {
                cleanUp(true, null);
            }

            if (pdf != null) {
                break;
            }
        }

        if (pdf != null) {
            // Saving it to the document
            startTransaction();
            setStatus("Saving to Document");
            initSession(); // IN 8.1, USE openSystemSession() instead
            DocumentModel doc = session.getDocument(new IdRef(docId));
            doc.setPropertyValue(xpath, (Serializable) pdf);

            // It may happen the async. job is done while, in the meantime, the user
            // created a version
            if (doc.isVersion()) {
                doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
            }

            session.saveDocument(doc);
            fireWebpageArchivedEvent(doc);
        }

        setStatus("Done " + (pdf == null ? "with error" : "with no error"));

    }

    @Override
    public String getCategory() {
        return CATEGORY_WEBPAGE_TO_BLOB;
    }

    @Override
    public String getTitle() {
        return "Webpage to Blob for " + url;
    }

    protected void fireWebpageArchivedEvent(DocumentModel doc) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        List<String> workIds = workManager.listWorkIds(CATEGORY_WEBPAGE_TO_BLOB, null);
        String idPrefix = computeIdPrefix(repositoryName, docId, url);
        int worksCount = 0;
        for (String workId : workIds) {
            if (workId.startsWith(idPrefix)) {
                if (++worksCount > 1) {
                    // another work scheduled
                    return;
                }
            }
        }

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        Event event = ctx.newEvent(WEBPAGE_ARCHIVED_EVENT);
        Framework.getLocalService(EventService.class).fireEvent(event);
    }

}
