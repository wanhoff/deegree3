//$HeadURL$
/*----------------    FILE HEADER  ------------------------------------------

 This file is part of deegree.
 Copyright (C) 2001-2008 by:
 EXSE, Department of Geography, University of Bonn
 http://www.giub.uni-bonn.de/deegree/
 lat/lon GmbH
 http://www.lat-lon.de

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 Contact:

 Andreas Poth  
 lat/lon GmbH 
 Aennchenstr. 19
 53115 Bonn
 Germany
 E-Mail: poth@lat-lon.de

 Prof. Dr. Klaus Greve
 Department of Geography
 University of Bonn
 Meckenheimer Allee 166
 53115 Bonn
 Germany
 E-Mail: greve@giub.uni-bonn.de


 ---------------------------------------------------------------------------*/
package org.deegree.model.generic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.deegree.model.generic.schema.ObjectType;

/**
 * An <code>ApplicationSchema</code> wraps an XML Schema InfoSet to provide a view of its elements with a generic
 * {@link ObjectType} semantic.
 * 
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider </a>
 * @author last edited by: $Author:$
 * 
 * @version $Revision:$, $Date:$
 */
public class ApplicationSchema {

    private Log LOG = LogFactory.getLog( ApplicationSchema.class );

    // object types defined in the schema
    private Map<QName, ObjectType> nameToOT = new HashMap<QName, ObjectType>();

    /**
     * Constructs a new <code>ApplicationSchema</code> from the given XML Schema InfoSet.
     * 
     * @param types
     */
    public ApplicationSchema( Collection<ObjectType> types ) {
        for ( ObjectType ot : types ) {
            nameToOT.put( ot.getName(), ot );
        }
    }

    /**
     * Returns the type information for objects with the given name.
     * 
     * @param objectName
     * @return the type information
     */
    public ObjectType getObjectType( QName objectName ) {
        ObjectType ot = nameToOT.get( objectName );
        if ( ot == null ) {
            String msg = "No object type with name '" + objectName
                         + "' is defined in the scope of the application schema.";
            throw new RuntimeException( msg );
        }
        return ot;
    }
}