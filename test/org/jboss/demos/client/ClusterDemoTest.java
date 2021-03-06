package org.jboss.demos.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

/**
 * GWT JUnit tests must extend GWTTestCase.
 */
public class ClusterDemoTest extends GWTTestCase {

  /**
   * Must refer to a valid module that sources this class.
   */
  public String getModuleName() {
    return "org.jboss.demos.ClusterDemoJUnit";
  }

  /**
   * Tests the FieldVerifier.
   */
  public void testFieldVerifier() {
/*
    assertFalse(FieldVerifier.isValidName(null));
    assertFalse(FieldVerifier.isValidName(""));
    assertFalse(FieldVerifier.isValidName("a"));
    assertFalse(FieldVerifier.isValidName("ab"));
    assertFalse(FieldVerifier.isValidName("abc"));
    assertTrue(FieldVerifier.isValidName("abcd"));
*/
  }

  /**
   * This test will send a request to the server using the getClusterInfo method in
   * GreetingService and verify the response.
   */
  public void testGreetingService() {
    // Create the service that we will test.
    ManagementServiceAsync greetingService = GWT.create(ManagementService.class);
    ServiceDefTarget target = (ServiceDefTarget) greetingService;
    target.setServiceEntryPoint(GWT.getModuleBaseURL() + "clusterdemo/greet");

    // Since RPC calls are asynchronous, we will need to wait for a response
    // after this test method returns. This line tells the test runner to wait
    // up to 10 seconds before timing out.
    delayTestFinish(10000);

    // Send a request to the server.
/*
    greetingService.getClusterInfo("GWT User", new AsyncCallback<List<ClusterNode>>() {
        public void onFailure(Throwable caught) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void onSuccess(List<ClusterNode> result) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    });
*/
  }


}
