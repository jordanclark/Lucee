/**
 *
 * Copyright (c) 2014, the Railo Company Ltd. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 **/

package lucee.runtime.functions.xml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
//import org.apache.xpath.XPathAPI;
//import org.apache.xpath.objects.XObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import lucee.commons.lang.StringUtil;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.Function;
import lucee.runtime.op.Caster;
import lucee.runtime.text.xml.XMLCaster;
import lucee.runtime.text.xml.XMLUtil;
import lucee.runtime.text.xml.struct.XMLObject;
import lucee.runtime.text.xml.struct.XMLStruct;
import lucee.runtime.type.Array;
import lucee.runtime.type.ArrayImpl;

/**
 * Implements the CFML Function xmlsearch
 */
public final class XmlSearch implements Function {

    private static final long serialVersionUID = 5770611088309897382L;

    private static List<String> operators = new ArrayList<String>();
    static {
	operators.add("=");
	operators.add("<>");
    }

    public static Object call(PageContext pc, Node node, String expr) throws PageException {
	boolean caseSensitive = true;
	if (node instanceof XMLObject) {
	    caseSensitive = ((XMLObject) node).getCaseSensitive();
	}
	if (node instanceof XMLStruct) {
	    node = ((XMLStruct) node).toNode();
	}
	return _call(node, expr, caseSensitive);
    }

    public static Object _call(Node node, String strExpr, boolean caseSensitive) throws PageException {
	if (StringUtil.endsWith(strExpr, '/')) strExpr = strExpr.substring(0, strExpr.length() - 1);
	// compile
	XPathExpression expr;
	try {
	    XPathFactory factory = XPathFactory.newInstance();
	    XPath path = factory.newXPath();
	    path.setNamespaceContext(new UniversalNamespaceResolver(XMLUtil.getDocument(node)));
	    expr = path.compile(strExpr);
	}
	catch (Exception e) {
	    throw Caster.toPageException(e);
	}

	// evaluate
	try {
	    Object obj = expr.evaluate(node, XPathConstants.NODESET);
	    return nodelist((NodeList) obj, caseSensitive);
	}
	catch (XPathExpressionException e) {
	    String msg = e.getMessage();
	    if (msg == null) msg = "";
	    try {
		if (msg.indexOf("#BOOLEAN") != -1) return Caster.toBoolean(expr.evaluate(node, XPathConstants.BOOLEAN));
		else if (msg.indexOf("#NUMBER") != -1) return Caster.toDouble(expr.evaluate(node, XPathConstants.NUMBER));
		else if (msg.indexOf("#STRING") != -1) return Caster.toString(expr.evaluate(node, XPathConstants.STRING));
		// TODO XObject.CLASS_NULL ???
	    }
	    catch (XPathExpressionException ee) {
		throw Caster.toPageException(ee);
	    }
	    throw Caster.toPageException(e);
	}
	catch (TransformerException e) {
	    throw Caster.toPageException(e);
	}
    }

    private static Array nodelist(NodeList list, boolean caseSensitive) throws TransformerException, PageException {
	// NodeList list = rs.nodelist();
	int len = list.getLength();
	Array rtn = new ArrayImpl();
	for (int i = 0; i < len; i++) {
	    Node n = list.item(i);
	    if (n != null) rtn.append(XMLCaster.toXMLStruct(n, caseSensitive));
	}
	return rtn;
    }

    private static class UniversalNamespaceResolver implements NamespaceContext {
	// the delegate
	private Document sourceDocument;

	/**
	 * This constructor stores the source document to search the namespaces in it.
	 * 
	 * @param document source document
	 */
	public UniversalNamespaceResolver(Document document) {
	    sourceDocument = document;
	}

	/**
	 * The lookup for the namespace uris is delegated to the stored document.
	 * 
	 * @param prefix to search for
	 * @return uri
	 */
	@Override
	public String getNamespaceURI(String prefix) {
	    if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
		return sourceDocument.lookupNamespaceURI(null);
	    }
	    else {
		return sourceDocument.lookupNamespaceURI(prefix);
	    }
	}

	/**
	 * This method is not needed in this context, but can be implemented in a similar way.
	 */
	@Override
	public String getPrefix(String namespaceURI) {
	    return sourceDocument.lookupPrefix(namespaceURI);
	}

	@Override
	public Iterator getPrefixes(String namespaceURI) {
	    // not implemented yet
	    return null;
	}

    }
}