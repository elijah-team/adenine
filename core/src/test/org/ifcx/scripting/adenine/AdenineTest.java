/*************************************************************************
 *
 *  The Contents of this file are made available subject to
 *  the terms of GNU Lesser General Public License Version 2.1.
 *
 *    Scala JSR-223 Scripting Engine.
 *    =============================================
 *    Copyright 2008 by James P. White
 *    mailto:jim@pagesmiths.com
 *    http://www.ifcx.org/
 *
 *    Portions Copyright 2006 by Sun Microsystems, Inc.
 *    901 San Antonio Road, Palo Alto, CA 94303, USA
 *
 *    GNU Lesser General Public License Version 2.1
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License version 2.1, as published by the Free Software Foundation.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *    MA  02111-1307  USA
 *
 ************************************************************************/

package org.ifcx.scripting.adenine;

import edu.mit.lcs.haystack.adenine.interpreter.Interpreter;
import edu.mit.lcs.haystack.adenine.interpreter.Environment;
import edu.mit.lcs.haystack.adenine.interpreter.DynamicEnvironment;
import edu.mit.lcs.haystack.adenine.interpreter.ICallable;
import edu.mit.lcs.haystack.adenine.interpreter.Message;
import edu.mit.lcs.haystack.adenine.compilers.ICompiler;
import edu.mit.lcs.haystack.adenine.compilers.rdfCode.RDFCodeCompiler;
import edu.mit.lcs.haystack.adenine.AdenineException;
import edu.mit.lcs.haystack.adenine.AdenineConstants;
import edu.mit.lcs.haystack.rdf.LocalRDFContainer;
import edu.mit.lcs.haystack.rdf.IRDFContainer;
import edu.mit.lcs.haystack.rdf.Resource;
import edu.mit.lcs.haystack.rdf.Utilities;
import edu.mit.lcs.haystack.rdf.URIGenerator;
import edu.mit.lcs.haystack.core.CoreLoader;
import edu.mit.lcs.haystack.HaystackException;
import edu.mit.lcs.haystack.Constants;
import edu.mit.lcs.haystack.proxy.IServiceAccessor;

import java.io.InputStreamReader;
import java.util.HashMap;

public class AdenineTest
{
    HashMap prefixes;
    Environment env;
    Interpreter interpreter;
    DynamicEnvironment denv;

    AdenineTest()
    {
        LocalRDFContainer rdfc = new LocalRDFContainer();
        ICompiler compiler = new RDFCodeCompiler(rdfc);

        compiler.compile(
            null,
            new InputStreamReader(CoreLoader.getResourceAsStream("/schemata/adenine.ad")),
            "/schemata/adenine.ad",
            null,
            null
        );

        interpreter = new Interpreter(rdfc);
        env = interpreter.createInitialEnvironment();
        denv = new DynamicEnvironment(rdfc, null);
        denv.setInstructionSource(rdfc);

        prefixes = new HashMap();
        prefixes.put("adenine", AdenineConstants.NAMESPACE);
        prefixes.put("rdf", Constants.s_rdf_namespace);
        prefixes.put("rdfs", Constants.s_rdfs_namespace);
        prefixes.put("daml", Constants.s_daml_namespace);
        prefixes.put("xsd", Constants.s_xsd_namespace);
        prefixes.put("dc", "http://purl.org/dc/elements/1.1/");
        prefixes.put("hs", "http://haystack.lcs.mit.edu/schemata/haystack#");
        prefixes.put("config", "http://haystack.lcs.mit.edu/schemata/config#");
        prefixes.put("mail", "http://haystack.lcs.mit.edu/schemata/mail#");
        prefixes.put("content", "http://haystack.lcs.mit.edu/schemata/content#");
        prefixes.put("ozone", "http://haystack.lcs.mit.edu/schemata/ozone#");
        prefixes.put("slide", "http://haystack.lcs.mit.edu/schemata/ozoneslide#");
        prefixes.put("source", "http://haystack.lcs.mit.edu/agents/adenine#");
        prefixes.put("op", "http://haystack.lcs.mit.edu/schemata/operation#");
        prefixes.put("opui", "http://haystack.lcs.mit.edu/ui/operation#");

        // Implied @base.
        final String uri = Utilities.generateUniqueResource().getURI();
        prefixes.put("@base", uri);
        prefixes.put("", uri + ":");
        prefixes.put("@urigenerator", new URIGenerator(uri));

        env.setValue("compile", new Compile());
        env.setValue("help", new Help());
    }

    public void setServiceAccessor(IServiceAccessor sa)
    {
        denv.setServiceAccessor(sa);
    }

    public void setEnvironmentValue(String name, Object o)
    {
        env.setValue(name, o);
    }

    public void setDynamicEnvironmentValue(String name, Object o)
    {
        denv.setValue(name, o);
    }

    public void setEnvironment(Environment env)
    {
        this.env = env;
    }

    public void setDynamicEnvironment(DynamicEnvironment denv)
    {
        this.denv = denv;
    }

    public Object eval(final String exp) throws Exception
    {
        return interpreter.eval(exp, prefixes, env, denv);
    }

    class Help implements ICallable
    {
        /**
         * @see ICallable#invoke(Message, DynamicEnvironment)
         */
        public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
            StringBuffer sb = new StringBuffer();
            java.lang.reflect.Method[] methods = message.m_values[0].getClass().getMethods();
            if (methods.length > 0) {
                sb.append("Methods:\n");
                for (int i = 0; i < methods.length; i++) {
                    java.lang.reflect.Method m = methods[i];
                    try {
                        Object.class.getMethod(m.getName(), m.getParameterTypes());
                    } catch (Exception e) {
                        sb.append(m.getReturnType().getName() + " " + m.getName() + "(");
                        Class[] params = m.getParameterTypes();
                        for (int j = 0; j < params.length; j++) {
                            sb.append(params[j].getName());
                            if (j != (params.length - 1)) {
                                sb.append(", ");
                            }
                        }
                        sb.append(")\n");
                    }
                }
            }

            java.lang.reflect.Field[] fields = message.m_values[0].getClass().getFields();
            if (fields.length > 0) {
                sb.append("Fields:\n");
                for (int i = 0; i < fields.length; i++) {
                    java.lang.reflect.Field f = fields[i];
                    sb.append(f.getType());
                    sb.append(" ");
                    sb.append(f.getName());
                    sb.append("\n");
                }
                sb.append("\n");
            }

            System.out.print(sb.toString());

            return new Message();
        }
    }

    class Compile implements ICallable
    {
        /**
         * @see ICallable#invoke(Message, DynamicEnvironment)
         */
        public Message invoke(Message message, DynamicEnvironment denv) throws AdenineException {
            try {
                interpreter.compileMethodToJava((Resource)message.m_values[0], (String)message.m_values[1]);
                return new Message();
            } catch (Exception e) {
                HaystackException.uncaught(e);
                return new Message();
            }
        }
    }

    static public void main(String[] args)
    {
        try {
            final AdenineTest at = new AdenineTest();
            final Object result = at.eval("#@base <http://ifcx.org/example>\n" +
                    "add { :john\n" +
                    "    :hasChild ${\n" +
                    "        rdf:type :Person ;\n" +
                    "        dc:title \"Joe\"\n" +
                    "    }\n" +
                    "}\n" +
                    "printset (query { :john ?x ?y})\n" +
                    "add { ^\n" +
                    "    rdf:type daml:Ontology ;\n" +
                    "    dc:title \"Sample Ontology\"\n" +
                    "}\n" +
                    "add { ^\n" +
                    "    :hasChild ${\n" +
                    "        rdf:type :Person ;\n" +
                    "        dc:title \"Joe\"\n" +
                    "    }\n" +
                    "}\n" +
                    "printset (query { ^ ?x ?y})\n");
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
