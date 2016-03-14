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
 *     thibaud
 */
package org.nuxeo.webpage.archiver;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineDescriptor;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineExecutorComponent;

/**
 * Centralize handling and check of parameters.
 * <p>
 * Mainly because we are using Apache Common Exec instead of Nuxeo COmmandLineExecutorService to perform the commandline
 * (waiting for a timeout to be implemented in the Service)
 * <p>
 * BAsically, we ensure the parameters are quoted, so we avoid security problems when the URL is set from a client.
 * 
 * @since 7.10
 */
public class CommandLineParameters {

    public static final String COOKIE_JAR = "cookieJar";

    private static final String COOKIE_JAR_EXP = "#{cookieJar}";

    public static final String URL = "url";

    public static final String URL_EXP = "#{url}";

    public static final String TARGET_FILE_PATH = "targetFilePath";

    public static final String TARGET_FILE_PATH_EXP = "#{targetFilePath}";

    protected static Map<String, Boolean> commandLinesAndCheck = new HashMap<String, Boolean>();

    protected static Map<String, String> commandLinesParamString = new HashMap<String, String>();

    protected static String LOCK = "Mutex";

    private static boolean checkDefinition(String inCommandLine) {

        Boolean ok = commandLinesAndCheck.get(inCommandLine);
        if (ok == null) {
            synchronized (LOCK) {
                ok = commandLinesAndCheck.get(inCommandLine);
                if (ok == null) {
                    ok = false;
                    CommandLineDescriptor desc = CommandLineExecutorComponent.getCommandDescriptor(inCommandLine);
                    if (desc != null) {
                        String cleanedUp = desc.getParametersString();

                        // Replace all occurrences of quoted parameters
                        cleanedUp = StringUtils.replace(cleanedUp, "\"" + COOKIE_JAR_EXP + "\"", "");
                        cleanedUp = StringUtils.replace(cleanedUp, "'" + COOKIE_JAR_EXP + "'", "");
                        cleanedUp = StringUtils.replace(cleanedUp, "\"" + URL_EXP + "\"", "");
                        cleanedUp = StringUtils.replace(cleanedUp, "'" + URL_EXP + "'", "");
                        cleanedUp = StringUtils.replace(cleanedUp, "\"" + TARGET_FILE_PATH_EXP + "\"", "");
                        cleanedUp = StringUtils.replace(cleanedUp, "'" + TARGET_FILE_PATH_EXP + "'", "");

                        // Must not find the non-quoted params.
                        if (cleanedUp.indexOf(COOKIE_JAR_EXP) < 0 && cleanedUp.indexOf(URL_EXP) < 0
                                && cleanedUp.indexOf(TARGET_FILE_PATH_EXP) < 0) {
                            ok = true;
                            commandLinesParamString.put(inCommandLine, desc.getParametersString());
                        }
                    }
                    commandLinesAndCheck.put(inCommandLine, ok);
                }
            }
        }

        return ok.booleanValue();

    }

    /**
     * Get the parameter string for the command line and replaces the values.
     * <p>
     * If at least one parameter is not quoted in the deinifition, throws a NuxeoException.
     * 
     * @param inCommandLine
     * @param inCookieJar
     * @param inUrl
     * @param inTargetFile
     * @return
     * @throws NuxeoException
     * @since 7.10
     */
    public static String buildParameterString(String inCommandLine, Blob inCookieJar, String inUrl, Blob inTargetFile)
            throws NuxeoException {

        if (!checkDefinition(inCommandLine)) {
            throw new NuxeoException("The command line contribution \"" + inCommandLine
                    + "\" has unquoted parameters and cannot be run");
        }

        String paramString = commandLinesParamString.get(commandLinesParamString);
        ;

        if (inCookieJar != null) {
            paramString = StringUtils.replace(paramString, COOKIE_JAR_EXP, inCookieJar.getFile().getAbsolutePath());
        }
        if (StringUtils.isNotBlank(inUrl)) {
            paramString = StringUtils.replace(paramString, URL_EXP, inUrl);
        }
        if (inTargetFile != null) {
            paramString = StringUtils.replace(paramString, TARGET_FILE_PATH_EXP, inTargetFile.getFile()
                                                                                             .getAbsolutePath());
        }

        return paramString;

    }

    /**
     * Get the parameter string for the command line and replaces the values.
     * <p>
     * If at least one parameter is not quoted in the definition, throws a NuxeoException.
     * 
     * @param inCommandLine
     * @param inCookieJarPath
     * @param inUrl
     * @param inTargetFilePath
     * @return
     * @throws NuxeoException
     * @since 7.10
     */
    public static String buildParameterString(String inCommandLine, String inCookieJarPath, String inUrl,
            String inTargetFilePath) throws NuxeoException {

        if (!checkDefinition(inCommandLine)) {
            throw new NuxeoException("The command line contribution \"" + inCommandLine
                    + "\" has unquoted parameter(s)and cannot be run");
        }

        String paramString = commandLinesParamString.get(inCommandLine);
        ;

        if (StringUtils.isNotBlank(inCookieJarPath)) {
            paramString = StringUtils.replace(paramString, COOKIE_JAR_EXP, inCookieJarPath);
        }
        if (StringUtils.isNotBlank(inUrl)) {
            paramString = StringUtils.replace(paramString, URL_EXP, inUrl);
        }
        if (StringUtils.isNotBlank(inTargetFilePath)) {
            paramString = StringUtils.replace(paramString, TARGET_FILE_PATH_EXP, inTargetFilePath);
        }

        return paramString;

    }

    /**
     * Returns true/false if the parameter is a parameter that we check is quoted.
     * 
     * @param inParamName
     * @return
     * @since 7.10
     */
    public static boolean isHandledParameter(String inParamName) {

        return inParamName.equals(COOKIE_JAR) || inParamName.equals(URL) || inParamName.equals(TARGET_FILE_PATH);

    }

}
