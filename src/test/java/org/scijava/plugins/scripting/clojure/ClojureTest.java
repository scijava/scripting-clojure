/*
 * #%L
 * JSR-223-compliant Clojure scripting language plugin.
 * %%
 * Copyright (C) 2013 - 2025 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package org.scijava.plugins.scripting.clojure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.junit.Test;
import org.scijava.Context;
import org.scijava.script.AbstractScriptLanguageTest;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

/**
 * Clojure unit tests.
 * 
 * @author Johannes Schindelin
 */
public class ClojureTest extends AbstractScriptLanguageTest {

	@Test
	public void testDiscovery() {
		assertDiscovered(ClojureScriptLanguage.class);
	}

	@Test
	public void testBasic() throws InterruptedException, ExecutionException,
		IOException, ScriptException
	{
		final Context context = new Context(ScriptService.class);
		final ScriptService scriptService = context.getService(ScriptService.class);
		final String script = "(+ 1 2)";
		final ScriptModule m = scriptService.run("add.clj", script, true).get();
		final Long result = (Long) m.getReturnValue();
		assertEquals(3L, result.longValue());
	}

	@Test
	public void testLocals() throws ScriptException {
		final Context context = new Context(ScriptService.class);
		final ScriptService scriptService = context.getService(ScriptService.class);

		final ScriptLanguage language = scriptService.getLanguageByExtension("clj");
		final ScriptEngine engine = language.getScriptEngine();
		assertEquals(ClojureScriptEngine.class, engine.getClass());
		engine.put("$hello", 17);
		assertEquals("17", engine.eval("$hello").toString());
		assertEquals("17", engine.get("$hello").toString());

		try {
			final Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
			bindings.clear();
			fail("UnsupportedOperationException expected on clear()");
		}
		catch (final UnsupportedOperationException exc) {
			// NB: Expected.
		}
	}

	@Test
	public void testParameters() throws InterruptedException, ExecutionException,
		IOException, ScriptException
	{
		final Context context = new Context(ScriptService.class);
		final ScriptService scriptService = context.getService(ScriptService.class);

		final String script = "" + //
			"; @ScriptService ss\n" + //
			"; @OUTPUT String language\n" + //
			"(def language (.getLanguageName (.getLanguageByName ss \"Clojure\")))\n";
		final ScriptModule m = scriptService.run("hello.clj", script, true).get();

		final Object actual = m.getOutput("language");
		final String expected =
			scriptService.getLanguageByName("Clojure").getLanguageName();
		assertEquals(expected, actual);
	}

	@Test
	public void testBindings() {
		final Context context = new Context(ScriptService.class);
		final ScriptService scriptService = context.getService(ScriptService.class);

		ScriptLanguage clojure = scriptService.getLanguageByName("clojure");
		assertSame(ClojureScriptLanguage.class, clojure.getClass());

		final ScriptEngine engine = clojure.getScriptEngine();
		final Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		final int bSize = bindings.size();

		assertEquals(bSize, bindings.keySet().size());
		assertFalse(bindings.keySet().contains("foo"));

		engine.put("foo", "bar");
		assertEquals("bar", engine.get("foo"));
		assertEquals("bar", bindings.get("foo"));
		assertEquals(bSize + 1, bindings.size());

		assertEquals(bSize + 1, bindings.keySet().size());
		assertTrue(bindings.keySet().contains("foo"));
	}
}
