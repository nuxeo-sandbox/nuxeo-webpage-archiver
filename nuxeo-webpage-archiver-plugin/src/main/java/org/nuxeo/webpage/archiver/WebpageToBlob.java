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
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;

/**
 * 
 * @since 7.10HF05
 */
public class WebpageToBlob {
    
    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(WebpageToBlob.class);

    public static final String COMMANDLINE_wkhtmltopdf = "wkhtmlToPdf";
    
    
    public WebpageToBlob() {
        
    }
    
    public static Blob toPdf(String inUrl, String inFileName) throws IOException, CommandNotAvailable {
                
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
        // we just check the size, but maybe we should use PDFBox to check the pdf
        File tempDestFile = resultPdf.getFile();
        if (!tempDestFile.exists() || tempDestFile.length() < 1) {
            throw new NuxeoException("Failed to execute the command <" + COMMANDLINE_wkhtmltopdf + ">. Final command [ "
                    + execResult.getCommandLine() + " ] returned with error " + execResult.getReturnCode(),
                    execResult.getError());
        }

        resultPdf.setMimeType("application/pdf");
        if(StringUtils.isBlank(inFileName)) {
            URL urlObj = new URL(inUrl);
            inFileName = StringUtils.replace(urlObj.getHost(), ".", "-") + ".pdf";
        }
        if(StringUtils.isNotBlank(inFileName)) {
            resultPdf.setFilename(inFileName);
        }
        return resultPdf;
        
    }

}
