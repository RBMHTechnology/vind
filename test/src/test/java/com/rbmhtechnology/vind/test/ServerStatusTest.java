package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.api.result.StatusResult;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ServerStatusTest {

    @Rule
    public TestSearchServer testSearchServer = new TestSearchServer();

    @Test
    public void testPing() {
        StatusResult statusResult = testSearchServer.getSearchServer().getBackendStatus();
        assertEquals( StatusResult.Status.UP, statusResult.getStatus());
        assertEquals(0, statusResult.getDetails().get("status"));
    }
}
