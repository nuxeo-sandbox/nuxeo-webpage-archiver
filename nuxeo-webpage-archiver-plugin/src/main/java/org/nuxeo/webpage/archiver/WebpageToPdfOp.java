/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.io.IOException;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;

/**
 * 
 */
@Operation(id=WebpageToPdfOp.ID, category=Constants.CAT_CONVERSION, label="Webpage to PDF", description="Use mkhtmltopdf command line (must be installed) to render build a pdf of the distant webpage. If fileName is not used, a default name will apply.")
public class WebpageToPdfOp {

    public static final String ID = "WebpageToPdf";

    @Param(name = "url", required = true)
    protected String url;

    @Param(name = "fileName", required = false)
    protected String fileName;

    @OperationMethod
    public Blob run() throws IOException, CommandNotAvailable {
      
        return WebpageToBlob.toPdf(url, fileName);
    }    

}
