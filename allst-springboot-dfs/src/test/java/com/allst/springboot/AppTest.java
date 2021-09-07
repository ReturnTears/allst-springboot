package com.allst.springboot;

import static org.junit.Assert.assertTrue;

import com.allst.springboot.dfs.FastDFSUtils;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue( true );
    }

    @Test
    public void testUpload() {
        FastDFSUtils utils = new FastDFSUtils();
        utils.uploadFile();
    }

    @Test
    public void testQuery() {
        FastDFSUtils utils = new FastDFSUtils();
        utils.queryFile();
    }

    @Test
    public void testDown() {
        FastDFSUtils utils = new FastDFSUtils();
        utils.downloadFile();
    }
}
