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

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.webpage.archiver.WebpageToBlob;
import org.nuxeo.webpage.archiver.WebpageToPdfOp;

/**
 * To test the "url to pdf for pages requiring authentication", and because we cannot hard-code logins and credentials
 * in the source (shared on HitHub, here is how it works:
 * <p>
 * <ul>
 * <li>A Properties file must be created at /src/test/resources/, with the name "test-private.properties"</li>
 * <li>(this file was added to .gitignore, so it is not shared)</li>
 * <li>It must declare properties that will hold the credential informations and value to test</li>
 * <li>These values are then used:
 * <ul>
 * <li>In the /src/test/resources/commandlines-test.xml contribution (keys will be replaced by their values at runtime)</li>
 * <li>And in the test, to pass the url of a page to test and a value to check once the PDF has been created</li>
 * </ul>
 * </li>
 * </ul>
 * Here are the keys that must exist in the file, with dummy values:
 * 
 * <pre>
 * loginUrl=http://my.distant.test.site.com/login
 * testPageUrl=http:///my.distant.test.site.com/some/page/about/the/sun.html
 * textToCheck=The sun looks yellow
 * loginVar=username
 * loginValue=jdoe
 * pwdVar=user_password
 * pwdValue=123456
 * submitVar=Submit
 * submitValue=doLogin
 * </pre>
 * 
 * <i>NOTE</I>: Following documentation explaining how to login using wkhtmltopdf, you will probably have very different
 * values for the names of variables (loginVar, pwdVar and submitVar)
 * 
 * @since 7.10HF05
 */
@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, EmbeddedAutomationServerFeature.class })
@Deploy({ "nuxeo-webpage-archiver", "org.nuxeo.ecm.platform.commandline.executor" })
@LocalDeploy({ "nuxeo-webpage-archiver-test:commandlines-test.xml" })
public class TestWebpageArchiver {

    protected static final Log log = LogFactory.getLog(TestWebpageArchiver.class);

    // The file where you put your private information for testing "with authentication"
    protected static final String TEST_PROPS_FILE = "test-private.properties";
    
    protected static final String DEFAULT_TEST_URL = "https://en.wikipedia.org/wiki/Unit_testing";
    
    protected static final String DEFAULT_TEST_URL_TEXT_CHECK = "unit testing";

    Properties privateProps = null;

    @Inject
    CoreSession session;
    
    @Inject
    AutomationService automationService;

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
    public void cleanup() throws Exception {

    }

    @Test
    public void testUrlToPdf() throws Exception {

        Assume.assumeTrue("wkhtmltopdf is not available, skipping test", WebpageToBlob.isAvailable());

        Blob result = null;
        WebpageToBlob wgtopdf = new WebpageToBlob();
        result = wgtopdf.toPdf(null, DEFAULT_TEST_URL, null);
        assertNotNull(result);

        assertTrue(Utils.hasText(result, DEFAULT_TEST_URL_TEXT_CHECK));
    }

    @Test
    public void testUrlToPdfWithAuthentication() throws Exception {

        Assume.assumeTrue("wkhtmltopdf is not available, skipping test", WebpageToBlob.isAvailable());
        Assume.assumeTrue("No \"" + TEST_PROPS_FILE + "\" file found, skipping test", privateProps != null);

        String testPageUrl = privateProps.getProperty("testPageUrl");
        String textToCheck = privateProps.getProperty("textToCheck");

        Blob result = null;
        WebpageToBlob wgtopdf = new WebpageToBlob();
        // The contrib hard-codes the credentials, URL, etc.
        Blob cookieJar = wgtopdf.login("wkhtmlToPdf-login-TEST", privateProps);
        assertNotNull(cookieJar);

        // Using a default contribution (no need to hard code things here)
        result = wgtopdf.toPdf("wkhtmlToPdf-authenticated", testPageUrl, null, cookieJar);
        assertNotNull(result);

        assertTrue(Utils.hasText(result, textToCheck));

    }
    
    @Test
    public void testOperation_noAuthentification() throws Exception {
        
        OperationContext ctx = new OperationContext();
        // No input nor session needed here
        OperationChain chain = new OperationChain("TestWPTPDF-1");
        chain.add(WebpageToPdfOp.ID).set("url", DEFAULT_TEST_URL).set("fileName", "myfile.pdf");
        Blob result = (Blob) automationService.run(ctx,  chain);
        
        assertNotNull(result);
        assertTrue(Utils.hasText(result, DEFAULT_TEST_URL_TEXT_CHECK));
        assertEquals("myfile.pdf", result.getFilename());
        
    }

}
