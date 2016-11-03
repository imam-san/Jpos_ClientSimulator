 package org.jpos.simulatorDimo;
 
 import java.io.PrintStream;
 import org.jpos.iso.ISOMsg;
 import org.jpos.iso.ISOUtil;
 import org.jpos.util.Loggeable;
 
public class TestCase
   implements Loggeable
 {
   public static final int OK = 0;
   public static final int FAILURE = 1;
   public static final int TIMEOUT = 2;
   String name;
   long start;
   long end;
   long timeout;
   ISOMsg request;
   ISOMsg expandedRequest;
   ISOMsg response;
   ISOMsg expectedResponse;
   String preEvaluationScript;
   String postEvaluationScript;
   int resultCode;
   boolean continueOnErrors;
   private String testcasePath;
   
   public TestCase(String name)
   {
     this.name = name;
     this.resultCode = -1;
     this.continueOnErrors = false;
   }
   
   public void setRequest(ISOMsg request) { this.request = request; }
   
   public void setResponse(ISOMsg response) {
     this.response = response;
   }
   
   public void setExpandedRequest(ISOMsg expandedRequest) { this.expandedRequest = expandedRequest; }
   
   public void setExpectedResponse(ISOMsg expectedResponse) {
     this.expectedResponse = expectedResponse;
   }
   
   public String getName() { return this.name; }
   
   public void setPreEvaluationScript(String preEvaluationScript) {
     this.preEvaluationScript = preEvaluationScript;
   }
   
   public String getPreEvaluationScript() { return this.preEvaluationScript; }
   
   public void setPostEvaluationScript(String postEvaluationScript) {
     this.postEvaluationScript = postEvaluationScript;
   }
   
   public String getPostEvaluationScript() { return this.postEvaluationScript; }
   
   public ISOMsg getRequest() {
     return this.request;
   }
   
   public ISOMsg getExpandedRequest() { return this.expandedRequest; }
   
   public ISOMsg getResponse() {
     return this.response;
   }
   
   public ISOMsg getExpectedResponse() { return this.expectedResponse; }
   
   public void setResultCode(int resultCode) {
     this.resultCode = resultCode;
   }
   
   public int getResultCode() { return this.resultCode; }
   
   public String getResultCodeAsString() {
     switch (this.resultCode) {
     case 0: 
       return "OK";
     case 1: 
       return "FAILURE";
     case 2: 
       return "TIMEOUT";
     }
     return Integer.toString(this.resultCode);
   }
   
   public void dump(PrintStream p, String indent) {
     String inner = indent + "  ";
     p.println(indent + "<test-case name='" + this.name + "'>");
     p.println(inner + "<request>");
     this.request.dump(p, inner + "  ");
     p.println(inner + "</request>");
     if (this.expandedRequest != null) {
       p.println(inner + "<expanded-request>");
       this.expandedRequest.dump(p, inner + "  ");
       p.println(inner + "</expanded-request>");
     }
     p.println(inner + "<expected-response>");
     this.expectedResponse.dump(p, inner + "  ");
     p.println(inner + "</expected-response>");
     if (this.response != null) {
       p.println(inner + "<response>");
       this.response.dump(p, inner + "  ");
       p.println(inner + "</response>");
     }
     p.println(inner + "<elapsed>" + elapsed() + "</elapsed>");
     p.println(indent + "</test-case>");
   }
   
   public String toString() { StringBuffer sb = new StringBuffer(ISOUtil.strpad(this.name, 50));
     sb.append(" [");
     sb.append(getResultCodeAsString());
     sb.append("] ");
     sb.append(elapsed());
     sb.append("ms.");
     return sb.toString();
   }
   
   public void start() { this.start = System.currentTimeMillis(); }
   
 
   public void end() { this.end = System.currentTimeMillis(); }
   
   public long elapsed() {
     if ((this.start != 0L) && (this.end == 0L))
       end();
     return this.end - this.start;
   }
   
   public boolean ok() { return this.resultCode == 0; }
   
   public void setContinueOnErrors(boolean continueOnErrors) {
     this.continueOnErrors = continueOnErrors;
   }
   
   public boolean isContinueOnErrors() { return this.continueOnErrors; }
   
   public void setTimeout(long timeout) {
     this.timeout = timeout;
   }
   
   public long getTimeout() { return this.timeout; }
   
   public void setFilename(String string)
   {
     this.testcasePath = string;
   }
   
   public String getFilename() { return this.testcasePath; }
 }
