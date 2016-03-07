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

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.webpage.archiver.WebpageToBlob;

/**
 * @since 7.10HF05
 */
@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy({ "nuxeo-webpage-archiver", "org.nuxeo.ecm.platform.commandline.executor" })
public class TestWebpageArchiver {

    protected static final Log log = LogFactory.getLog(TestWebpageArchiver.class);

    // The file where you put your private information for testing "with authentication"
    protected static final String TEST_PROPS_FILE = "test-private.properties";

    Properties privateProps = null;

    @Before
    public void setUp() throws Exception {

        FileInputStream fileInput = null;
        try {
            File file = FileUtils.getResourceFileFromContext(TEST_PROPS_FILE);
            fileInput = new FileInputStream(file);
            privateProps = new Properties();
            privateProps.load(fileInput);
        } finally {
            if (fileInput != null) {
                fileInput.close();
            }
        }

    }

    @After
    public void cleanup() {

    }

    @Test
    public void testUrlToPdf() throws Exception {

        Assume.assumeTrue("wkhtmltopdf is not available, skipping test", WebpageToBlob.isAvailable());

        Blob result = null;
        WebpageToBlob wgtopdf = new WebpageToBlob();
        result = wgtopdf.toPdf("https://en.wikipedia.org/wiki/Unit_testing", null);
        assertNotNull(result);

        assertTrue(Utils.hasText(result, "unit testing"));
    }

    @Test
    public void testUrlToPdfShouldFailOnTimeOut() throws Exception {

        // Short timeout for a page we know is long to load
        int SHORT_TIMEOUT = 2000;
        String HEAVY_PAGE = "http://nuxeo.com";

        Assume.assumeTrue("wkhtmltopdf is not available, skipping test", WebpageToBlob.isAvailable());

        Blob result = null;
        WebpageToBlob wgtopdf = new WebpageToBlob(SHORT_TIMEOUT);
        try {
            result = wgtopdf.toPdf(HEAVY_PAGE, null);
            assertTrue("SHould have failed.", false);
        } catch (Exception e) {
            // All good
        }
        assertNull(result);
    }

    @Test
    public void testUrlToPdfWithAuthentication() throws Exception {

        Assume.assumeTrue("wkhtmltopdf is not available, skipping test", WebpageToBlob.isAvailable());

        Assume.assumeTrue("No \"" + TEST_PROPS_FILE + "\" file found, skipping test", privateProps != null);

        String loginInfo = privateProps.getProperty("loginInfo");
        String loginUrl = privateProps.getProperty("loginUrl");
        String pageUrl = privateProps.getProperty("pageUrl");
        String textToCheck = privateProps.getProperty("textToCheck");

        boolean allGood = StringUtils.isNotBlank(loginInfo) || StringUtils.isNotBlank(loginUrl)
                || StringUtils.isNotBlank(pageUrl) || StringUtils.isNotBlank(textToCheck);
        Assume.assumeTrue("Missing property in " + TEST_PROPS_FILE + "file, skipping test", allGood);

        Blob result = null;
        WebpageToBlob wgtopdf = new WebpageToBlob();
        Blob cookieJar = wgtopdf.login(loginUrl, loginInfo);
        assertNotNull(cookieJar);
        result = wgtopdf.toPdf(pageUrl, null, cookieJar);
        assertNotNull(result);
        Utils.hasText(result, textToCheck);

    }

}
