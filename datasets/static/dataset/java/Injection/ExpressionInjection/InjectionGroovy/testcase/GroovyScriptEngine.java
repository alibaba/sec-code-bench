<filename>seco-master/languages/groovy/src/seco/langs/groovy/jsr/GroovyScriptEngine.java<fim_prefix>

/*
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.  
 * Use is subject to license terms.
 *
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met: Redistributions of source code 
 * must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of 
 * conditions and the following disclaimer in the documentation and/or other materials 
 * provided with the distribution. Neither the name of the Sun Microsystems nor the names of 
 * is contributors may be used to endorse or promote products derived from this software 
 * without specific prior written permission. 

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER 
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * GroovyScriptEngine.java
 * @author Mike Grogan
 * @author A. Sundararajan
 */
package seco.langs.groovy.jsr;

import java.io.*;
import java.util.*;
import javax.script.*;

import groovy.lang.*;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.MetaClassHelper;
import org.codehaus.groovy.runtime.MethodClosure;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.CompilationFailedException;
import java.lang.reflect.*;

public class GroovyScriptEngine extends AbstractScriptEngine
/* implements Compilable, Invocable */ {
	private GroovyScriptEngineFactory factory;
	private GroovyClassLoader loader;
	GroovyShell shell = null;
	
	public GroovyScriptEngine(GroovyScriptEngineFactory factory)
	{
		this.factory = factory;
		loader = new GroovyClassLoader(getParentLoader()); 
		
	}

	public GroovyClassLoader getClassLoader()
	{
		return loader;
	}
	
//	public Object eval(Class<?> clasz, ScriptContext context)
//	{
//		
//	}
	
	@Override
	public Object eval(String script, ScriptContext context) throws ScriptException
	{
		<fim_suffix>

		if (shell == null)
			shell = new GroovyShell(new GroovyScriptContextBinding(context));
		return shell.evaluate(script);
	}

	@Override
	public Object eval(Reader reader, ScriptContext context) throws ScriptException
	{
		return eval(readFully(reader), context);
	}

	@Override
	public Bindings createBindings()
	{
		return new SimpleBindings();
	}

	@Override
	public ScriptEngineFactory getFactory()
	{
		return factory;
	}

	//
	// private static boolean DEBUG = false;
	//
	// // script-string-to-generated Class map
	// // private Map<String, Class<?>> classMap;
	// // global closures map - this is used to simulate a single
	// // global functions namespace
	// public Map<String, Closure> globalClosures;
	// // class loader for Groovy generated classes	 
	// private ImportCustomizer importsCustomizer = new ImportCustomizer();
	// // lazily initialized factory
	// private volatile GroovyScriptEngineFactory factory;
	//
	 // counter used to generate unique global Script class names
	 private static int counter;
	
	 static {
	 counter = 0;
	 }
	//
	// private void extractImports(String script) {
	// BufferedReader reader = new BufferedReader(new StringReader(script));
	// reader.lines().forEach(line -> {
	// int pos = 0;
	// String [] tokens = line.trim().split("\\s+");
	// if (pos == tokens.length) return;
	// if (!"import".equals(tokens[pos++])) return;
	// if (pos == tokens.length) return;
	// boolean is_static = false;
	// if ("static".equals(tokens[pos])) { is_static = true; pos++; }
	// if (pos == tokens.length) return;
	// String theimport = tokens[pos++];
	// int lastdot = theimport.lastIndexOf('.');
	// String importMain = theimport.substring(0, lastdot);
	// String importLastPart = theimport.substring(lastdot+1);
	// if (pos == tokens.length)
	// {
	// if (is_static)
	// importsCustomizer.addStaticImport(importMain, importLastPart);
	// else if (theimport.endsWith("*"))
	// importsCustomizer.addStarImports(importMain);
	// else
	// importsCustomizer.addImports(theimport);
	// return;
	// }
	// if (!"as".equals(tokens[pos++]) || pos == tokens.length)
	// throw new RuntimeException("Can't parse Groovy import statement " +
	// line);
	// String alias = tokens[pos];
	// if (is_static)
	// {
	// importsCustomizer.addStaticImport(alias, importMain, importLastPart);
	// }
	// else
	// importsCustomizer.addImport(alias, theimport);
	// });
	// }
	//
	// public GroovyScriptEngine() {
	// //classMap = Collections.synchronizedMap(new HashMap<String,
	// Class<?>>());
	// globalClosures = Collections.synchronizedMap(new HashMap<String,
	// Closure>());
	// CompilerConfiguration compileConfiguration = new CompilerConfiguration();
	// compileConfiguration.addCompilationCustomizers(importsCustomizer);
	// loader = new GroovyClassLoader(getParentLoader(),
	// compileConfiguration);
	// }
	//
	// public Object eval(Reader reader, ScriptContext ctx)
	// throws ScriptException {
	// return eval(readFully(reader), ctx);
	// }
	//
	// public Object eval(String script, ScriptContext ctx)
	// throws ScriptException {
	// try {
	// extractImports(script);
	// return eval(getScriptClass(script), ctx);
	// } catch (SyntaxException e) {
	// throw new ScriptException(e.getMessage(),
	// e.getSourceLocator(), e.getLine());
	// } catch (Exception e) {
	// if (DEBUG) e.printStackTrace();
	// throw new ScriptException(e);
	// }
	// }
	//
	// public Bindings createBindings() {
	// return new SimpleBindings();
	// }
	//
	// public ScriptEngineFactory getFactory() {
	// if (factory == null) {
	// synchronized (this) {
	// if (factory == null) {
	// factory = new GroovyScriptEngineFactory();
	// }
	// }
	// }
	// return factory;
	// }
	//
	// // javax.script.Compilable methods
	// public CompiledScript compile(String scriptSource) throws ScriptException
	// {
	// try {
	// return new GroovyCompiledScript(this,
	// getScriptClass(scriptSource));
	// } catch (SyntaxException e) {
	// throw new ScriptException(e.getMessage(),
	// e.getSourceLocator(), e.getLine());
	// } catch (IOException e) {
	// throw new ScriptException(e);
	// } catch (CompilationFailedException ee) {
	// throw new ScriptException(ee);
	// }
	// }
	//
	// public CompiledScript compile(Reader reader) throws ScriptException {
	// return compile(readFully(reader));
	// }
	//
	// // javax.script.Invocable methods.
	// public Object invokeFunction(String name, Object... args)
	// throws ScriptException, NoSuchMethodException {
	// return invokeImpl(null, name, args);
	// }
	//
	// public Object invokeMethod(Object thiz, String name, Object... args)
	// throws ScriptException, NoSuchMethodException {
	// if (thiz == null) {
	// throw new IllegalArgumentException("script object is null");
	// }
	// return invokeImpl(thiz, name, args);
	// }
	//
	// public <T> T getInterface(Class<T> clasz) {
	// return makeInterface(null, clasz);
	// }
	//
	// public <T> T getInterface(Object thiz, Class<T> clasz) {
	// if (thiz == null) {
	// throw new IllegalArgumentException("script object is null");
	// }
	// return makeInterface(thiz, clasz);
	// }
	//
	// // package-privates
	// Object eval(Class<?> scriptClass, final ScriptContext ctx) throws
	// ScriptException {
	// //add context to bindings
	// ctx.setAttribute("context", ctx,
	// ScriptContext.GLOBAL_SCOPE/*ENGINE_SCOPE*/);
	//
	// //direct output to ctx.getWriter
	// Writer writer = ctx.getWriter();
	// ctx.setAttribute("out", (writer instanceof PrintWriter) ?
	// writer :
	// new PrintWriter(writer),
	// ScriptContext.GLOBAL_SCOPE/*ENGINE_SCOPE*/);
	// /*
	// * We use the following Binding instance so that global variable lookup
	// * will be done in the current ScriptContext instance.
	// */
	// Binding binding = new
	// Binding(ctx.getBindings(ScriptContext.GLOBAL_SCOPE/*ENGINE_SCOPE*/)) {
	// @Override
	// public Object getVariable(String name) {
	// synchronized (ctx) {
	// int scope = ctx.getAttributesScope(name);
	// if (scope != -1) {
	// return ctx.getAttribute(name, scope);
	// }
	// }
	// throw new MissingPropertyException(name, getClass());
	// }
	// @Override
	// public void setVariable(String name, Object value) {
	// synchronized (ctx) {
	// int scope = ctx.getAttributesScope(name);
	// if (scope == -1) {
	// scope = ScriptContext.GLOBAL_SCOPE/*ENGINE_SCOPE*/;
	// }
	// ctx.setAttribute(name, value, scope);
	// }
	// }
	// };
	//
	// try {
	// Script scriptObject = InvokerHelper.createScript(scriptClass, binding);
	//
	// // create a Map of MethodClosures from this new script object
	// Method[] methods = scriptClass.getMethods();
	// Map<String, MethodClosure> closures = new HashMap<String,
	// MethodClosure>();
	// for (Method m : methods) {
	// String name = m.getName();
	// closures.put(name, new MethodClosure(scriptObject, name));
	// }
	//
	// // save all current closures into global closures map
	// globalClosures.putAll(closures);
	//
	// MetaClass oldMetaClass = scriptObject.getMetaClass();
	// // List<MetaProperty> props = oldMetaClass.getProperties();
	//
	// /*
	// * We override the MetaClass of this script object so that we can
	// * forward calls to global closures (of previous or future "eval" calls)
	// * This gives the illusion of working on the same "global" scope.
	// */
	// scriptObject.setMetaClass(new DelegatingMetaClass(oldMetaClass) {
	// @Override
	// public Object invokeMethod(Object object, String name, Object args) {
	// if (args == null) {
	// return invokeMethod(object, name, MetaClassHelper.EMPTY_ARRAY);
	// }
	// if (args instanceof Tuple) {
	// return invokeMethod(object, name, ((Tuple)args).toArray());
	// }
	// if (args instanceof Object[]) {
	// return invokeMethod(object, name, (Object[]) args);
	// } else {
	// return invokeMethod(object, name, new Object[] { args });
	// }
	// }
	//
	// @Override
	// public Object invokeMethod(Object object, String name, Object[] args) {
	// try {
	// return super.invokeMethod(object, name, args);
	// } catch (MissingMethodException mme) {
	// return callGlobal(name, args, ctx);
	// }
	// }
	// @Override
	// public Object invokeStaticMethod(Object object, String name, Object[]
	// args) {
	// try {
	// return super.invokeStaticMethod(object, name, args);
	// } catch (MissingMethodException mme) {
	// return callGlobal(name, args, ctx);
	// }
	// }
	// });
	//
	// return scriptObject.run();
	// } catch (Exception e) {
	// throw new ScriptException(e);
	// }
	// }
	//
	// Class<?> getScriptClass(String script)
	// throws SyntaxException,
	// CompilationFailedException,
	// IOException {
	// Class<?> clazz = null;//classMap.get(script);
	//// if (clazz != null) {
	//// return clazz;
	//// }
	//
	// /// InputStream stream = new ByteArrayInputStream(script.getBytes());
	// clazz = loader.parseClass(script, generateScriptName());
	// // classMap.put(script, clazz);
	// return clazz;
	// }
	//
	// //-- Internals only below this point
	//
	// // invokes the specified method/function on the given object.
	// private Object invokeImpl(Object thiz, String name, Object... args)
	// throws ScriptException, NoSuchMethodException {
	// if (name == null) {
	// throw new NullPointerException("method name is null");
	// }
	//
	// try {
	// if (thiz != null) {
	// return InvokerHelper.invokeMethod(thiz, name, args);
	// } else {
	// return callGlobal(name, args);
	// }
	// } catch (MissingMethodException mme) {
	// throw new NoSuchMethodException(mme.getMessage());
	// } catch (Exception e) {
	// throw new ScriptException(e);
	// }
	// }
	//
	// // call the script global function of the given name
	// private Object callGlobal(String name, Object[] args) {
	// return callGlobal(name, args, context);
	// }
	//
	// private Object callGlobal(String name, Object[] args, ScriptContext ctx)
	// {
	// Closure closure = globalClosures.get(name);
	// if (closure != null) {
	// return closure.call(args);
	// } else {
	// // Look for closure valued variable in the
	// // given ScriptContext. If available, call it.
	// Object value = ctx.getAttribute(name);
	// if (value instanceof Closure) {
	// return ((Closure)value).call(args);
	// } // else fall thru..
	// }
	// throw new MissingMethodException(name, getClass(), args);
	// }
	//
	 // generate a unique name for top-level Script classes
	 public static synchronized String generateScriptName() {
	 return "Script" + (++counter) + ".groovy";
	 }
	//
	// private <T> T makeInterface(Object obj, Class<T> clazz) {
	// final Object thiz = obj;
	// if (clazz == null || !clazz.isInterface()) {
	// throw new IllegalArgumentException("interface Class expected");
	// }
	// return (T) Proxy.newProxyInstance(
	// clazz.getClassLoader(),
	// new Class<?>[] { clazz },
	// new InvocationHandler() {
	// public Object invoke(Object proxy, Method m, Object[] args)
	// throws Throwable {
	// return invokeImpl(thiz, m.getName(), args);
	// }
	// });
	// }
	//
	// determine appropriate class loader to serve as parent loader
	// for GroovyClassLoader instance
	private ClassLoader getParentLoader()
	{
		// check whether thread context loader can "see" Groovy Script class
		ClassLoader ctxtLoader = Thread.currentThread().getContextClassLoader();
		try
		{
			Class<?> c = ctxtLoader.loadClass("org.codehaus.groovy.Script");
			if (c == Script.class)
			{
				return ctxtLoader;
			}
		}
		catch (ClassNotFoundException cnfe)
		{
		}
		// exception was thrown or we get wrong class
		return Script.class.getClassLoader();
	}

	private String readFully(Reader reader) throws ScriptException
	{
		char[] arr = new char[8 * 1024]; // 8K at a time
		StringBuilder buf = new StringBuilder();
		int numChars;
		try
		{
			while ((numChars = reader.read(arr, 0, arr.length)) > 0)
			{
				buf.append(arr, 0, numChars);
			}
		}
		catch (IOException exp)
		{
			throw new ScriptException(exp);
		}
		return buf.toString();
	}
}
<fim_middle>