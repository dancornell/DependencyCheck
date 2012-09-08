package org.codesecure.dependencycheck.scanner;
/*
 * This file is part of DependencyCheck.
 *
 * DependencyCheck is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DependencyCheck is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DependencyCheck. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2012 Jeremy Long. All Rights Reserved.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.codesecure.dependencycheck.utils.Checksum;

/**
 *
 * Used to load a JAR file and collect information that can be used to determine the associated CPE.
 *
 * <!--
 * ideas - scan the JAR to see if there is a "version" final static string?
 *         scan manifest for version info
 *         get md5 and sh1 checksums to lookup file via maven centrals hosted md5sum files
 *         examine file name itself for version info
 * -->
 *
 * @author Jeremy Long (jeremy.long@gmail.com)
 */
public class JarAnalyzer implements Analyzer {

    private static final String BUNDLE_VERSION = "Bundle-Version"; //: 2.1.2
    private static final String BUNDLE_DESCRIPTION = "Bundle-Description"; //: Apache Struts 2
    private static final String BUNDLE_NAME = "Bundle-Name"; //: Struts 2 Core
    private static final String BUNDLE_VENDOR = "Bundle-Vendor"; //: Apache Software Foundation

    private enum STRING_STATE {

        ALPHA,
        NUMBER,
        OTHER
    }

    private STRING_STATE determineState(char c) {
        if (c >= '0' && c <= '9' || c == '.') {
            return STRING_STATE.NUMBER;
        } else if (c >= 'a' && c <= 'z') {
            return STRING_STATE.ALPHA;
        } else {
            return STRING_STATE.OTHER;
        }
    }

    /**
     * Loads a specified JAR file and collects information from the manifest and
     * checksums to identify the correct CPE information.
     *
     * @param file path to the JAR file.
     * @return a dependency derived for the specified file.
     * @throws IOException is thrown if there is an error reading the JAR file.
     */
    public Dependency insepct(File file) throws IOException {

        Dependency dependency = new Dependency();

        String fileName = file.getName();
        dependency.setFileName(fileName);
        dependency.setFilePath(file.getCanonicalPath());
        String fileNameEvidence = fileName.substring(0, fileName.length() - 4)
                .toLowerCase().replace('-', ' ').replace('_', ' ');
        StringBuilder sb = new StringBuilder(fileNameEvidence.length());
        STRING_STATE state = determineState(fileNameEvidence.charAt(0));

        for (int i = 0; i < fileNameEvidence.length(); i++) {
            char c = fileNameEvidence.charAt(i);
            STRING_STATE newState = determineState(c);
            if (newState != state) {
                sb.append(' ');
                state = newState;
            }
            sb.append(c);
        }
        Pattern rx = Pattern.compile("\\s\\s+");
        fileNameEvidence = rx.matcher(sb.toString()).replaceAll(" ");
        dependency.getTitleEvidence().addEvidence("jar", "file name",
                fileNameEvidence, Evidence.Confidence.HIGH);
        dependency.getVendorEvidence().addEvidence("jar", "file name",
                fileNameEvidence, Evidence.Confidence.HIGH);
        dependency.getVersionEvidence().addEvidence("jar", "file name",
                fileNameEvidence, Evidence.Confidence.HIGH);

        String md5 = null;
        String sha1 = null;
        try {
            md5 = Checksum.getMD5Checksum(file);
            sha1 = Checksum.getSHA1Checksum(file);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(JarAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(JarAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
        dependency.setMd5sum(md5);
        dependency.setSha1sum(sha1);

        parseManifest(dependency);
        analyzePackageNames(dependency);

        //TODO - can we get "version" information from the filename?  add it as medium confidence?
        //   strip extension. find first numeric, chop off the first part. consider replacing [_-] with .
        //dependency.getVersionEvidence().addEvidence("jar", "file name",
        //                      version from file, Evidence.Confidence.MEDIUM);

        return dependency;
    }

    /**
     * Analyzes the path information of the classes contained within the JarAnalyzer
     * to try and determine possible vendor or product names. If any are found they are
     * stored in the packageVendor and packageProduct hashSets.
     *
     * @param dependency A reference to the dependency.
     * @throws IOException is thrown if there is an error reading the JAR file.
     */
    protected void analyzePackageNames(Dependency dependency) throws IOException {

        JarFile jar = new JarFile(dependency.getFilePath());
        java.util.Enumeration en = jar.entries();

        HashMap<String, Integer> level0 = new HashMap<String, Integer>();
        HashMap<String, Integer> level1 = new HashMap<String, Integer>();
        HashMap<String, Integer> level2 = new HashMap<String, Integer>();
        HashMap<String, Integer> level3 = new HashMap<String, Integer>();
        int count = 0;
        while (en.hasMoreElements()) {
            java.util.jar.JarEntry entry = (java.util.jar.JarEntry) en.nextElement();
            if (entry.getName().endsWith(".class") && entry.getName().contains("/")) {
                String[] path = entry.getName().toLowerCase().split("/");

                if ("java".equals(path[0])
                        || "javax".equals(path[0])
                        || ("com".equals(path[0]) && "sun".equals(path[0]))) {
                    continue;
                }

                count += 1;
                String temp = path[0];
                if (level0.containsKey(temp)) {
                    level0.put(temp, level0.get(temp) + 1);
                } else {
                    level0.put(temp, 1);
                }

                if (path.length > 2) {
                    temp += "/" + path[1];
                    if (level1.containsKey(temp)) {
                        level1.put(temp, level1.get(temp) + 1);
                    } else {
                        level1.put(temp, 1);
                    }
                }
                if (path.length > 3) {
                    temp += "/" + path[2];
                    if (level2.containsKey(temp)) {
                        level2.put(temp, level2.get(temp) + 1);
                    } else {
                        level2.put(temp, 1);
                    }
                }

                if (path.length > 4) {
                    temp += "/" + path[3];
                    if (level3.containsKey(temp)) {
                        level3.put(temp, level3.get(temp) + 1);
                    } else {
                        level3.put(temp, 1);
                    }
                }

            }
        }

        if (count == 0) {
            return;
        }
        EvidenceCollection vendor = dependency.getVendorEvidence();
        EvidenceCollection title = dependency.getTitleEvidence();

        for (String s : level0.keySet()) {
            if (!"org".equals(s) && !"com".equals(s)) {
                vendor.addWeighting(s);
                title.addWeighting(s);
                vendor.addEvidence("jar", "package", s, Evidence.Confidence.LOW);
                title.addEvidence("jar", "package", s, Evidence.Confidence.LOW);
            }
        }
        for (String s : level1.keySet()) {
            float ratio = level1.get(s);
            ratio /= count;
            if (ratio > 0.5) {
                String[] parts = s.split("/");
                if ("org".equals(parts[0]) || "com".equals(parts[0])) {
                    vendor.addWeighting(parts[1]);
                    vendor.addEvidence("jar", "package", parts[1], Evidence.Confidence.LOW);
                } else {
                    vendor.addWeighting(parts[0]);
                    title.addWeighting(parts[1]);
                    vendor.addEvidence("jar", "package", parts[0], Evidence.Confidence.LOW);
                    title.addEvidence("jar", "package", parts[1], Evidence.Confidence.LOW);
                }
            }
        }
        for (String s : level2.keySet()) {
            float ratio = level2.get(s);
            ratio /= count;
            if (ratio > 0.4) {
                String[] parts = s.split("/");
                if ("org".equals(parts[0]) || "com".equals(parts[0])) {
                    vendor.addWeighting(parts[1]);
                    title.addWeighting(parts[2]);
                    vendor.addEvidence("jar", "package", parts[1], Evidence.Confidence.LOW);
                    title.addEvidence("jar", "package", parts[2], Evidence.Confidence.LOW);
                } else {
                    vendor.addWeighting(parts[0]);
                    vendor.addWeighting(parts[1]);
                    title.addWeighting(parts[1]);
                    title.addWeighting(parts[2]);
                    vendor.addEvidence("jar", "package", parts[0], Evidence.Confidence.LOW);
                    vendor.addEvidence("jar", "package", parts[1], Evidence.Confidence.LOW);
                    title.addEvidence("jar", "package", parts[1], Evidence.Confidence.LOW);
                    title.addEvidence("jar", "package", parts[2], Evidence.Confidence.LOW);
                }
            }
        }
        for (String s : level3.keySet()) {
            float ratio = level3.get(s);
            ratio /= count;
            if (ratio > 0.3) {
                String[] parts = s.split("/");
                if ("org".equals(parts[0]) || "com".equals(parts[0])) {
                    vendor.addWeighting(parts[1]);
                    vendor.addWeighting(parts[2]);
                    title.addWeighting(parts[2]);
                    title.addWeighting(parts[3]);
                    vendor.addEvidence("jar", "package", parts[1], Evidence.Confidence.LOW);
                    vendor.addEvidence("jar", "package", parts[2], Evidence.Confidence.LOW);
                    title.addEvidence("jar", "package", parts[2], Evidence.Confidence.LOW);
                    title.addEvidence("jar", "package", parts[3], Evidence.Confidence.LOW);

                } else {
                    vendor.addWeighting(parts[0]);
                    vendor.addWeighting(parts[1]);
                    vendor.addWeighting(parts[2]);
                    title.addWeighting(parts[1]);
                    title.addWeighting(parts[2]);
                    title.addWeighting(parts[3]);
                    vendor.addEvidence("jar", "package", parts[0], Evidence.Confidence.LOW);
                    vendor.addEvidence("jar", "package", parts[1], Evidence.Confidence.LOW);
                    vendor.addEvidence("jar", "package", parts[2], Evidence.Confidence.LOW);
                    title.addEvidence("jar", "package", parts[1], Evidence.Confidence.LOW);
                    title.addEvidence("jar", "package", parts[2], Evidence.Confidence.LOW);
                    title.addEvidence("jar", "package", parts[3], Evidence.Confidence.LOW);
                }
            }
        }
    }

    /**
     * <p>Reads the manifest from the JAR file and collects the:</p>
     * <ul><li>Implementation Title</li>
     *     <li>Implementation Version</li>
     *     <li>Implementation Vendor</li>
     *     <li>Implementation VendorId</li>
     *     <li>Bundle Name</li>
     *     <li>Bundle Version</li>
     *     <li>Bundle Vendor</li>
     *     <li>Bundle Description</li>
     *     <li>Main Class</li>
     * </ul>
     *
     * @param dependency A reference to the dependency.
     * @throws IOException if there is an issue reading the JAR file.
     */
    protected void parseManifest(Dependency dependency) throws IOException {
        JarFile jar = new JarFile(dependency.getFilePath());
        Manifest manifest = jar.getManifest();
        Attributes atts = manifest.getMainAttributes();

        EvidenceCollection vendorEvidence = dependency.getVendorEvidence();
        EvidenceCollection titleEvidence = dependency.getTitleEvidence();
        EvidenceCollection versionEvidence = dependency.getVendorEvidence();

        String source = "Manifest";
        String name = Attributes.Name.IMPLEMENTATION_TITLE.toString();
        String value = atts.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
        if (value != null) {
            titleEvidence.addEvidence(source, name, value, Evidence.Confidence.HIGH);
        }

        name = Attributes.Name.IMPLEMENTATION_VERSION.toString();
        value = atts.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        if (value != null) {
            versionEvidence.addEvidence(source, name, value, Evidence.Confidence.HIGH);
        }

        name = Attributes.Name.IMPLEMENTATION_VENDOR.toString();
        value = atts.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
        if (value != null) {
            vendorEvidence.addEvidence(source, name, value, Evidence.Confidence.HIGH);
        }

        name = Attributes.Name.IMPLEMENTATION_VENDOR_ID.toString();
        value = atts.getValue(Attributes.Name.IMPLEMENTATION_VENDOR_ID);
        if (value != null) {
            vendorEvidence.addEvidence(source, name, value, Evidence.Confidence.MEDIUM);
        }

        name = BUNDLE_DESCRIPTION;
        value = atts.getValue(BUNDLE_DESCRIPTION);
        if (value != null) {
            titleEvidence.addEvidence(source, name, value, Evidence.Confidence.MEDIUM);
        }

        name = BUNDLE_VENDOR;
        value = atts.getValue(BUNDLE_VENDOR);
        if (value != null) {
            vendorEvidence.addEvidence(source, name, value, Evidence.Confidence.MEDIUM);
        }

        name = BUNDLE_VERSION;
        value = atts.getValue(BUNDLE_VERSION);
        if (value != null) {
            versionEvidence.addEvidence(source, name, value, Evidence.Confidence.MEDIUM);
        }
        name = BUNDLE_NAME;
        value = atts.getValue(BUNDLE_NAME);
        if (value != null) {
            titleEvidence.addEvidence(source, name, value, Evidence.Confidence.LOW);
        }

        name = Attributes.Name.MAIN_CLASS.toString();
        value = atts.getValue(Attributes.Name.MAIN_CLASS);
        if (value != null) {
            titleEvidence.addEvidence(source, name, value, Evidence.Confidence.MEDIUM);
            vendorEvidence.addEvidence(source, name, value, Evidence.Confidence.MEDIUM);
        }
    }
}