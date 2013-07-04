/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jcstress.infra.grading;

import org.openjdk.jcstress.schema.descr.Case;
import org.openjdk.jcstress.schema.descr.ObjectFactory;
import org.openjdk.jcstress.schema.descr.Template;
import org.openjdk.jcstress.schema.descr.Test;
import org.openjdk.jcstress.schema.descr.Testsuite;
import org.openjdk.jcstress.util.Reflections;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Basic reporter class which is responsible for reading test descriptions.
 *
 * @author Aleksey Shipilev (aleksey.shipilev@oracle.com)
 */
public class DescriptionReader {

    protected final Map<String, Test> testDescriptions;
    private final Unmarshaller testSuiteUnmarshaller;

    public DescriptionReader() {
        testDescriptions = new TreeMap<String, Test>();
        try {
            SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);

            JAXBContext jc1 = JAXBContext.newInstance(Testsuite.class.getPackage().getName());

            testSuiteUnmarshaller = jc1.createUnmarshaller();
            testSuiteUnmarshaller.setSchema(sf.newSchema(getClass().getResource("/xsd/descriptions/test-descriptions.xsd")));

            readDescriptions();
        } catch (SAXException e) {
            throw new IllegalStateException(e);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void readDescriptions() throws JAXBException {
        for (String res : Reflections.getResources("org/openjdk/jcstress/desc", "xml")) {
            loadDescription(res);
        }
    }

    private void loadDescription(String name)  {
        Testsuite suite;
        try {
            suite = unmarshalTestsuite(this.getClass().getResourceAsStream("/" + name));
        } catch (JAXBException e) {
            throw new IllegalStateException(name + ": " + e.getMessage(), e);
        }

        Map<String, Template> templates = new HashMap<String, Template>();
        for (Template t : suite.getTemplate()) {
            templates.put(t.getName(), t);
        }

        for (Test t : suite.getTest()) {
            if (t.getTemplate() != null && !t.getTemplate().isEmpty()) {
                Template template = templates.get(t.getTemplate());
                if (template == null) {
                    throw new IllegalStateException(name + ": template \"" + t.getTemplate() + "\" is not found");
                }
                mergeTemplate(t, template);
            }
            testDescriptions.put(t.getName(), t);
        }
    }

    private void splitCases(Collection<Case> cases) {
        List<Case> newCases = new ArrayList<Case>();
        for (Case c : cases) {
            for (String match : c.getMatch()) {
                Case newCase = new Case();
                newCase.getMatch().clear();
                newCase.getMatch().add(match);
                newCase.setDescription(c.getDescription());
                newCase.setExpect(c.getExpect());
                newCase.setRefs(c.getRefs());
                newCases.add(newCase);
            }
        }
        cases.clear();
        cases.addAll(newCases);
    }

    private void mergeTemplate(Test t, Template template) {
        if (t.getContributedBy() == null) t.setContributedBy(template.getContributedBy());
        if (t.getDescription() == null) t.setDescription(template.getDescription());

        // split the matches
        splitCases(t.getCase());
        splitCases(template.getCase());

        // merge the matches
        Collection<Case> newCases = new ArrayList<Case>();

        Set<String> fulfilledMatches = new HashSet<String>();
        for (Case c : t.getCase()) {
            fulfilledMatches.add(c.getMatch().get(0));
            newCases.add(c);
        }

        for (Case c : template.getCase()) {
            if (fulfilledMatches.add(c.getMatch().get(0))) {
                newCases.add(c);
            }
        }

        t.getCase().clear();
        t.getCase().addAll(newCases);

        // merge unmatched
        if (t.getUnmatched() == null) {
            t.setUnmatched(template.getUnmatched());
        }

        // merge refs
        Set<String> urls = new HashSet<String>();
        if (t.getRefs() == null) {
            t.setRefs(new ObjectFactory().createRef());
        }
        urls.addAll(t.getRefs().getUrl());
        if (template.getRefs() != null) {
            urls.addAll(template.getRefs().getUrl());
        }
        t.getRefs().getUrl().clear();
        t.getRefs().getUrl().addAll(urls);
    }

    @SuppressWarnings("unchecked")
    public Testsuite unmarshalTestsuite(InputStream inputStream) throws JAXBException {
        return (Testsuite) testSuiteUnmarshaller.unmarshal(inputStream);
    }

}
