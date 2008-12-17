package org.codalang.codaserver;

import groovy.lang.GroovyClassLoader;

/**
 * Created by IntelliJ IDEA.
 * User: michaelarace
 * Date: Dec 24, 2007
 * Time: 1:42:52 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */
public class FancyGroovyClassLoader extends GroovyClassLoader {
	public FancyGroovyClassLoader() {
		super();
	}
	public FancyGroovyClassLoader(ClassLoader classLoader) {
		super(classLoader);
	}

	public void linkClass(Class c) {
		super.resolveClass(c);
	}
}
