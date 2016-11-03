 package org.jpos.simulatorDimo;
 
 import bsh.BshClassManager;
 import bsh.EvalError;
 import bsh.Interpreter;
 import bsh.UtilEvalError;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 import org.jdom.Element;
 import org.jpos.core.Configuration;
 import org.jpos.iso.ISOComponent;
 import org.jpos.iso.ISOException;
 import org.jpos.iso.ISOField;
 import org.jpos.iso.ISOHeader;
 import org.jpos.iso.ISOMsg;
 import org.jpos.iso.ISOPackager;
 import org.jpos.iso.ISOUtil;
 import org.jpos.iso.MUX;
 import org.jpos.iso.packager.XMLPackager;
 import org.jpos.q2.Q2;
 import org.jpos.q2.QBeanSupport;
 import org.jpos.q2.QClassLoader;
 import org.jpos.util.Log;
 import org.jpos.util.LogEvent;
 import org.jpos.util.Logger;
 import org.jpos.util.NameRegistrar;
 import org.jpos.util.NameRegistrar.NotFoundException;

 public class TestRunner
   extends QBeanSupport
   implements Runnable
 {
   MUX mux;
   ISOPackager packager;
   Interpreter bsh;
   public static final long TIMEOUT = 60000L;
   
   protected void initService()
     throws ISOException { this.packager = new XMLPackager(); }
   
   protected void startService() {
     for (int i = 1; i <= this.cfg.getInt("sessions", 1); i++)
       new Thread(this).start();
   }
   
   public void run() {
     try { Interpreter bsh = initBSH();
       this.mux = ((MUX)NameRegistrar.get("mux." + this.cfg.get("mux")));
       List suite = initSuite(getPersist().getChild("test-suite"));
       runSuite(suite, this.mux, bsh);
     } catch (NameRegistrar.NotFoundException e) {
       LogEvent evt = getLog().createError();
       evt.addMessage(e);
       evt.addMessage(NameRegistrar.getInstance());
       Logger.log(evt);
     } catch (Throwable t) {
       getLog().error(t);
     }
     if (this.cfg.getBoolean("shutdown")) {
       shutdownQ2();
     }
   }
   
   private void runSuite(List suite, MUX mux, Interpreter bsh) throws ISOException, EvalError {
     LogEvent evt = getLog().createLogEvent("results");
     LogEvent evt_error = null;
     Iterator iter = suite.iterator();
     long start = System.currentTimeMillis();
     long serverTime = 0L;
     for (int i = 1; iter.hasNext(); i++) {
       evt_error = getLog().createLogEvent("error");
       TestCase tc = (TestCase)iter.next();
       
       getLog().trace("---------------------------[ " + tc.getName() + " ]---------------------------");
       
 
 
 
       ISOMsg m = (ISOMsg)tc.getRequest().clone();
       if (tc.getPreEvaluationScript() != null) {
         bsh.set("testcase", tc);
         bsh.set("request", m);
         bsh.eval(tc.getPreEvaluationScript());
       }
       tc.setExpandedRequest(applyRequestProps(m, bsh));
       tc.start();
       tc.setResponse(mux.request(m, tc.getTimeout()));
       tc.end();
       assertResponse(tc, bsh, evt_error);
       evt.addMessage(i + ": " + tc.toString());
       if (evt_error.getPayLoad().size() != 0) {
         evt_error.addMessage("filename", tc.getFilename());
         evt.addMessage("\r\n" + evt_error);
       }
       
       serverTime += tc.elapsed();
       if (!tc.ok()) {
         getLog().error(tc);
         if (!tc.isContinueOnErrors())
           break;
       }
     }
     long end = System.currentTimeMillis();
     
     long simulatorTime = end - start - serverTime;
     long total = end - start;
     
     evt.addMessage("elapsed server=" + serverTime + "ms(" + percentage(serverTime, total) + "%)" + ", simulator=" + simulatorTime + "ms(" + percentage(simulatorTime, total) + "%)" + ", total=" + total + "ms, shutdown=" + this.cfg.getBoolean("shutdown"));
     
 
 
 
 
 
 
     ISOUtil.sleep(100L);
     if (this.cfg.getBoolean("shutdown")) {
       evt.addMessage("Shutting down");
     }
     Logger.log(evt);
   }
   
   private List initSuite(Element suite) throws IOException, ISOException
   {
     List l = new ArrayList();
     String prefix = suite.getChildTextTrim("path");
     Iterator iter = suite.getChildren("test").iterator();
     while (iter.hasNext()) {
       Element e = (Element)iter.next();
       boolean cont = "yes".equals(e.getAttributeValue("continue"));
       String s = e.getAttributeValue("count");
       int count = s != null ? Integer.parseInt(s) : 1;
       String path = e.getAttributeValue("file");
       String name = e.getAttributeValue("name");
       if (name == null) {
         name = path;
       }
      // imam
       // please add  read bulk file here
       // and set to  the tc.setRequest
       for (int i = 0; i < count; i++) {
         TestCase tc = new TestCase(name);
         tc.setContinueOnErrors(cont);
         tc.setRequest(getMessage(prefix + path + "_s"));
         tc.setExpectedResponse(getMessage(prefix + path + "_r"));
         tc.setPreEvaluationScript(e.getChildTextTrim("init"));
         tc.setPostEvaluationScript(e.getChildTextTrim("post"));
         tc.setFilename(prefix + path);
         
         String to = e.getAttributeValue("timeout");
         if (to != null) {
           tc.setTimeout(Long.parseLong(to));
         } else
           tc.setTimeout(this.cfg.getLong("timeout", 60000L));
         l.add(tc);
       }
     }
     return l;
   }
   
   private ISOMsg getMessage(String filename) throws IOException, ISOException
   {
     File f = new File(filename);
     FileInputStream fis = new FileInputStream(f);
     try {
       byte[] b = new byte[fis.available()];
       fis.read(b);
       ISOMsg m = new ISOMsg();
       m.setPackager(this.packager);
       m.unpack(b);
       return m;
     } finally {
       fis.close();
     }
   }
   
   private boolean processResponse(ISOMsg er, ISOMsg m, ISOMsg expected, Interpreter bsh, LogEvent evt)
     throws ISOException, EvalError
   {
     int maxField = Math.max(m.getMaxField(), expected.getMaxField());
     
     for (int i = 0; i <= maxField; i++) {
       if (expected.hasField(i)) {
         ISOComponent c = expected.getComponent(i);
         if ((c instanceof ISOField)) {
           String value = expected.getString(i);
           if ((value.charAt(0) == '!') && (value.length() > 1))
           {
             bsh.set("value", m.getString(i));
             Object ret = bsh.eval(value.substring(1));
             if ((ret instanceof Boolean)) {
               if (!((Boolean)ret).booleanValue()) {
                 evt.addMessage("field", "[" + i + "] Boolean eval returned false");
               }
             }
             else if (((ret instanceof String)) && 
               (m.hasField(i)) && (!ret.equals(m.getString(i)))) {
               evt.addMessage("field", "[" + i + "] Received:[" + m.getString(i) + "]" + " Expected:[" + ret + "]");
             }
             
 
             m.unset(i);
             expected.unset(i);
           }
           else if (value.startsWith("*M")) {
             if (m.hasField(i)) {
               expected.unset(i);
               m.unset(i);
             } else {
               evt.addMessage("field", "[" + i + "] Mandatory field missing");
             }
             
           }
           else if (value.startsWith("*E")) {
             if ((m.hasField(i)) && (er.hasField(i))) {
               expected.set(i, er.getString(i));
             } else {
               evt.addMessage("field", "[" + i + "] Echo field missing");
             }
             
           }
           else if ((m.hasField(i)) && (!m.getString(i).equals(value))) {
             evt.addMessage("field", "[" + i + "] Received:[" + m.getString(i) + "]" + " Expected:[" + value + "]");
           }
           
         }
         else if ((c instanceof ISOMsg)) {
           ISOMsg rc = (ISOMsg)m.getComponent(i);
           ISOMsg innerExpectedResponse = (ISOMsg)er.getComponent(i);
           if ((rc instanceof ISOMsg)) {
             processResponse(innerExpectedResponse, rc, (ISOMsg)c, bsh, evt);
           }
         }
       } else {
         m.unset(i);
       }
     }
     if (evt.getPayLoad().size() != 0) {
       return false;
     }
     return true;
   }
   
   private boolean assertResponse(TestCase tc, Interpreter bsh, LogEvent evt) throws ISOException, EvalError
   {
     if (tc.getResponse() == null) {
       tc.setResultCode(2);
       return false;
     }
     ISOMsg c = (ISOMsg)tc.getResponse().clone();
     ISOMsg expected = (ISOMsg)tc.getExpectedResponse().clone();
     ISOMsg er = (ISOMsg)tc.getExpandedRequest().clone();
     c.setHeader((ISOHeader)null);
     if (!processResponse(er, c, expected, bsh, evt)) {
       tc.setResultCode(1);
       return false;
     }
     ISOPackager p = new XMLPackager();
     expected.setPackager(p);
     c.setPackager(p);
     
     if (tc.getPostEvaluationScript() != null) {
       bsh.set("testcase", tc);
       bsh.set("response", tc.getResponse());
       Object ret = bsh.eval(tc.getPostEvaluationScript());
       if (((ret instanceof Boolean)) && 
         (!((Boolean)ret).booleanValue())) {
         tc.setResultCode(1);
         return false;
       }
     }
     
     if (!new String(c.pack()).equals(new String(expected.pack()))) {
       tc.setResultCode(1);
       return false;
     }
     tc.setResultCode(0);
     return true;
   }
   
   private void eval(Element e, String name, Interpreter bsh) throws EvalError
   {
     Element ee = e.getChild(name);
     if (ee != null)
       bsh.eval(ee.getText());
   }
   
   private Interpreter initBSH() throws UtilEvalError, EvalError { Interpreter bsh = new Interpreter();
     BshClassManager bcm = bsh.getClassManager();
     bcm.setClassPath(getServer().getLoader().getURLs());
     bcm.setClassLoader(getServer().getLoader());
     bsh.set("qbean", this);
     bsh.set("log", getLog());
     bsh.eval(getPersist().getChildTextTrim("init"));
     return bsh;
   }
   
   private ISOMsg applyRequestProps(ISOMsg m, Interpreter bsh) throws ISOException, EvalError
   {
     int maxField = m.getMaxField();
     for (int i = 0; i <= maxField; i++) {
       if (m.hasField(i)) {
         ISOComponent c = m.getComponent(i);
         if ((c instanceof ISOMsg)) {
           applyRequestProps((ISOMsg)c, bsh);
         } else if ((c instanceof ISOField)) {
           String value = (String)c.getValue();
           if (value.length() > 0) {
             try {
               if (value.charAt(0) == '!') {
                 m.set(i, bsh.eval(value.substring(1)).toString());
               }
               else if (value.charAt(0) == '#') {
                 m.set(i, ISOUtil.hex2byte(bsh.eval(value.substring(1)).toString()));
               }
             } catch (NullPointerException e) {
               m.unset(i);
             }
           }
         }
       }
     }
     return m;
   }
   
   private long percentage(long a, long b) {
  long d = a / b;
     return (d * 100);
   }
 }
