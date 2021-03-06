/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.codesecure.dependencycheck;

import org.codesecure.dependencycheck.data.lucene.BaseIndexTestCase;
import org.codesecure.dependencycheck.reporting.ReportGenerator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jeremy Long (jeremy.long@gmail.com)
 */
public class EngineTest extends BaseIndexTestCase {

    public EngineTest(String testName) {
        super(testName);
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of scan method, of class Engine.
     * @throws Exception is thrown when an exception occurs.
     */
    @Test
    public void testScan() throws Exception {
        System.out.println("scan");
        String path = "./src/test/resources/";
        Engine instance = new Engine();
        instance.scan(path);
        assertTrue(instance.getDependencies().size() > 0);
        instance.analyzeDependencies();
        ReportGenerator rg = new ReportGenerator();
        rg.generateReports("./target/", "DependencyCheck", instance.getDependencies());
    }
}
