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
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;

/**
 * Runs the commandLine which must declare everything needed to authenticated (see README). Returns a blob to use for
 * further calls to the same site, accessing pages requesting authentication."
 * 
 * @since 7.10
 */
@Operation(id = WebpageToBlobLoginOp.ID, category = Constants.CAT_CONVERSION, label = "Webpage to PDF: Login", description = "Run the commandLine which must declare everything needed to authenticated (see README). Returns a blob to use for further calls to the same site, accessing pages requesting authentication.")
public class WebpageToBlobLoginOp {

    public static final String ID = "WebpageToBlob.Login";

    @Param(name = "commandLine", required = true)
    protected String commandLine;

    @OperationMethod
    public Blob run() throws IOException, CommandNotAvailable, NuxeoException {

        WebpageToBlob wptopdf = new WebpageToBlob();
        return wptopdf.login(commandLine);
    }

}
