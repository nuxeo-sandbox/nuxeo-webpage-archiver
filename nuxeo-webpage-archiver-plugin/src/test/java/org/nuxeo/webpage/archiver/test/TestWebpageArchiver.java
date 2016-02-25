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
package org.nuxeo.webpage.archiver.test;

import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.webpage.archiver.WebpageToBlob;

/**
 * 
 * @since 7.10HF05
 */
@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class })
@Deploy({ "nuxeo-webpage-archiver", "org.nuxeo.ecm.platform.commandline.executor" })
public class TestWebpageArchiver {
    
    protected static final Log log = LogFactory.getLog(TestWebpageArchiver.class);
    
    @Test
    public void testUrlToImage() throws Exception {
        
        Blob result = null;
        
        //result = WebpageToBlob.toPdf("http://nuxeo.com", "thepage.pdf");
        result = WebpageToBlob.toPdf("http://www.nuxeo.com/services/training/", null);
        assertNull(result);
    }

}
