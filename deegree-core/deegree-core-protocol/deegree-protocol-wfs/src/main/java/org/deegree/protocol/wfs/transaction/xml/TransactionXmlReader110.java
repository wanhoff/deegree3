//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2012 by:
 Department of Geography, University of Bonn
 and
 lat/lon GmbH

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
 ----------------------------------------------------------------------------*/
package org.deegree.protocol.wfs.transaction.xml;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.deegree.commons.xml.CommonNamespaces.OGCNS;
import static org.deegree.commons.xml.stax.XMLStreamUtils.getAttributeValue;
import static org.deegree.commons.xml.stax.XMLStreamUtils.getElementTextAsQName;
import static org.deegree.commons.xml.stax.XMLStreamUtils.getRequiredAttributeValue;
import static org.deegree.commons.xml.stax.XMLStreamUtils.getRequiredAttributeValueAsBoolean;
import static org.deegree.commons.xml.stax.XMLStreamUtils.getRequiredAttributeValueAsQName;
import static org.deegree.commons.xml.stax.XMLStreamUtils.nextElement;
import static org.deegree.commons.xml.stax.XMLStreamUtils.requireNextTag;
import static org.deegree.protocol.wfs.WFSConstants.VERSION_110;
import static org.deegree.protocol.wfs.WFSConstants.WFS_NS;

import java.util.NoSuchElementException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.deegree.commons.utils.kvp.MissingParameterException;
import org.deegree.commons.xml.CommonNamespaces;
import org.deegree.commons.xml.XMLParsingException;
import org.deegree.filter.Filter;
import org.deegree.filter.xml.Filter110XMLDecoder;
import org.deegree.protocol.i18n.Messages;
import org.deegree.protocol.wfs.transaction.ReleaseAction;
import org.deegree.protocol.wfs.transaction.Transaction;
import org.deegree.protocol.wfs.transaction.TransactionAction;
import org.deegree.protocol.wfs.transaction.action.Delete;
import org.deegree.protocol.wfs.transaction.action.IDGenMode;
import org.deegree.protocol.wfs.transaction.action.Insert;
import org.deegree.protocol.wfs.transaction.action.Native;
import org.deegree.protocol.wfs.transaction.action.PropertyReplacement;
import org.deegree.protocol.wfs.transaction.action.Update;

/**
 * {@link TransactionXmlReader} for WFS 1.1.0.
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider</a>
 * @author last edited by: $Author$
 * 
 * @version $Revision$, $Date$
 */
class TransactionXmlReader110 extends AbstractTransactionXmlReader {

    @Override
    public Transaction read( XMLStreamReader xmlStream )
                            throws XMLStreamException {

        xmlStream.require( START_ELEMENT, WFS_NS, "Transaction" );

        // optional: '@handle'
        String handle = getAttributeValue( xmlStream, "handle" );

        // optional: '@releaseAction'
        String releaseActionString = getAttributeValue( xmlStream, "releaseAction" );
        ReleaseAction releaseAction = parseReleaseAction( releaseActionString );

        // optional: 'wfs:LockId'
        String lockId = null;
        requireNextTag( xmlStream, START_ELEMENT );
        if ( xmlStream.getName().equals( new QName( WFS_NS, "LockId" ) ) ) {
            lockId = xmlStream.getElementText().trim();
            requireNextTag( xmlStream, START_ELEMENT );
        }

        LazyTransactionActionsReader iterable = new LazyTransactionActionsReader( xmlStream, this );
        return new Transaction( VERSION_110, handle, lockId, releaseAction, iterable, null );
    }

    @Override
    public TransactionAction readAction( XMLStreamReader xmlStream )
                            throws XMLStreamException, XMLParsingException {
        if ( !WFS_NS.equals( xmlStream.getNamespaceURI() ) ) {
            String msg = "Unexpected element: " + xmlStream.getName()
                         + "' is not a WFS 1.1.0 operation element. Not in the wfs namespace.";
            throw new XMLParsingException( xmlStream, msg );
        }

        TransactionAction operation = null;
        String localName = xmlStream.getLocalName();
        if ( "Delete".equals( localName ) ) {
            operation = readDelete( xmlStream );
            xmlStream.nextTag();
            xmlStream.require( END_ELEMENT, WFS_NS, "Delete" );
            xmlStream.nextTag();
        } else if ( "Insert".equals( localName ) ) {
            operation = readInsert( xmlStream );
        } else if ( "Native".equals( localName ) ) {
            operation = readNative( xmlStream );
        } else if ( "Update".equals( localName ) ) {
            operation = readUpdate( xmlStream );
        } else {
            throw new XMLParsingException( xmlStream, "Unexpected operation element " + localName + "." );
        }
        return operation;
    }

    /**
     * Returns the object representation of a <code>wfs:Delete</code> element. Consumes all corresponding events from
     * the given <code>XMLStream</code>.
     * 
     * @param xmlStream
     *            cursor must point at the <code>START_ELEMENT</code> event (&lt;wfs:Delete&gt;), points at the
     *            corresponding <code>END_ELEMENT</code> event (&lt;/wfs:Delete&gt;) afterwards
     * @return corresponding {@link Delete} object
     * @throws XMLStreamException
     * @throws XMLParsingException
     */
    Delete readDelete( XMLStreamReader xmlStream )
                            throws XMLStreamException {

        // optional: '@handle'
        String handle = xmlStream.getAttributeValue( null, "handle" );

        // required: '@typeName'
        QName ftName = getRequiredAttributeValueAsQName( xmlStream, null, "typeName" );

        // required: 'ogc:Filter'
        xmlStream.nextTag();

        try {
            xmlStream.require( START_ELEMENT, OGCNS, "Filter" );
        } catch ( XMLStreamException e ) {
            // CITE compliance (wfs:wfs-1.1.0-Transaction-tc12.1)
            throw new MissingParameterException( "Mandatory 'ogc:Filter' element is missing in request." );
        }

        Filter filter = Filter110XMLDecoder.parse( xmlStream );
        xmlStream.require( END_ELEMENT, CommonNamespaces.OGCNS, "Filter" );
        return new Delete( handle, ftName, filter );
    }

    /**
     * Returns the object representation for the given <code>wfs:Insert</code> element.
     * <p>
     * NOTE: In order to allow stream-oriented processing, this method does *not* consume all events corresponding to
     * the <code>wfs:Insert</code> elemetn from the given <code>XMLStream</code>. After a call to this method, the XML
     * stream points at the <code>START_ELEMENT</code> of the insert payload.
     * </p>
     * 
     * @param xmlStream
     *            cursor must point at the <code>START_ELEMENT</code> event (&lt;wfs:Insert&gt;)
     * @return corresponding {@link Insert} object, never <code>null</code>
     * @throws NoSuchElementException
     * @throws XMLStreamException
     * @throws XMLParsingException
     */
    Insert readInsert( XMLStreamReader xmlStream )
                            throws XMLStreamException {

        // optional: '@idgen'
        String idGenString = xmlStream.getAttributeValue( null, "idgen" );
        IDGenMode idGen = null;
        if ( idGenString != null ) {
            if ( "GenerateNew".equals( idGenString ) ) {
                idGen = IDGenMode.GENERATE_NEW;
            } else if ( "ReplaceDuplicate".equals( idGenString ) ) {
                idGen = IDGenMode.REPLACE_DUPLICATE;
            } else if ( "UseExisting".equals( idGenString ) ) {
                idGen = IDGenMode.USE_EXISTING;
            } else {
                String msg = Messages.get( "WFS_UNKNOWN_IDGEN_MODE", idGenString, "1.1.0",
                                           "'GenerateNew', 'ReplaceDuplicate' and 'UseExisting'" );
                throw new XMLParsingException( xmlStream, msg );
            }
        }

        // optional: '@handle'
        String handle = xmlStream.getAttributeValue( null, "handle" );

        // optional: '@inputFormat'
        String inputFormat = xmlStream.getAttributeValue( null, "inputFormat" );

        // optional: '@srsName'
        String srsName = xmlStream.getAttributeValue( null, "srsName" );

        nextElement( xmlStream );
        if ( !xmlStream.isStartElement() ) {
            throw new XMLParsingException( xmlStream, Messages.get( "WFS_INSERT_MISSING_FEATURE_ELEMENT" ) );
        }
        return new Insert( handle, idGen, inputFormat, srsName, xmlStream );
    }

    Update readUpdate( XMLStreamReader xmlStream )
                            throws XMLStreamException {

        // optional: '@handle'
        String handle = xmlStream.getAttributeValue( null, "handle" );

        // required: '@typeName'
        QName ftName = getRequiredAttributeValueAsQName( xmlStream, null, "typeName" );

        // optional: '@inputFormat'
        String inputFormat = xmlStream.getAttributeValue( null, "inputFormat" );

        // optional: '@srsName'
        String srsName = xmlStream.getAttributeValue( null, "srsName" );

        // skip to first "wfs:Property" element
        xmlStream.nextTag();
        xmlStream.require( START_ELEMENT, WFS_NS, "Property" );

        return new Update( handle, VERSION_110, ftName, inputFormat, srsName, xmlStream, this );
    }

    public PropertyReplacement readProperty( XMLStreamReader xmlStream )
                            throws XMLStreamException {

        xmlStream.require( START_ELEMENT, WFS_NS, "Property" );
        xmlStream.nextTag();
        xmlStream.require( START_ELEMENT, WFS_NS, "Name" );
        QName propName = getElementTextAsQName( xmlStream );
        xmlStream.nextTag();

        PropertyReplacement replacement = null;
        if ( new QName( WFS_NS, "Value" ).equals( xmlStream.getName() ) ) {
            replacement = new PropertyReplacement( propName, xmlStream );
        } else {
            // if the wfs:Value element is omitted, the property shall be removed (CITE 1.1.0 test,
            // wfs:wfs-1.1.0-Transaction-tc11.1)
            xmlStream.require( END_ELEMENT, WFS_NS, "Property" );
            replacement = new PropertyReplacement( propName, null );
            xmlStream.nextTag();
        }
        return replacement;
    }

    Native readNative( XMLStreamReader xmlStream ) {
        // optional: '@handle'
        String handle = xmlStream.getAttributeValue( null, "handle" );

        // required: '@vendorId'
        String vendorId = getRequiredAttributeValue( xmlStream, "vendorId" );

        // required: '@safeToIgnore'
        boolean safeToIgnore = getRequiredAttributeValueAsBoolean( xmlStream, null, "safeToIgnore" );
        return new Native( handle, vendorId, safeToIgnore, xmlStream );
    }
}
