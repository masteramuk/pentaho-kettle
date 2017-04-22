/*
 * *****************************************************************************
 *
 *  Pentaho Data Integration
 *
 *  Copyright (C) 2002-2017 by Pentaho : http://www.pentaho.com
 *
 *  *******************************************************************************
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 *  this file except in compliance with the License. You may obtain a copy of the
 *  License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * *****************************************************************************
 *
 */

package org.pentaho.pdi.engine.serializers;

import org.junit.Test;
import org.pentaho.di.engine.api.model.Row;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by nbaker on 3/6/17.
 */
public class DeserializedRowTest {

  @Test
  public void testRow() throws Exception {

    Date date = new Date();
    URI uri = new URI( "http://www.pentaho.com" );

    List<Object> objects = new ArrayList<>();
    objects.add( 100 );
    objects.add( 100.50 );
    BigDecimal bigDecimal = new BigDecimal( "10000000000000000000.50" );
    objects.add( bigDecimal );
    objects.add( true );
    objects.add( date );
    objects.add( "A String" );
    objects.add( uri );

    List<String> names = new ArrayList<>();
    names.add( "some int" );
    names.add( "some Double" );
    names.add( "some Decimal" );
    names.add( "some Boolean" );
    names.add( "some Date" );
    names.add( "some String" );
    names.add( "some Serializable" );

    List<Class> classes = Arrays
      .asList( Integer.class, Double.class, BigDecimal.class, Boolean.class, Date.class, String.class, Object.class );

    Row row = new DeserializedRow( names, classes, objects );


    assertEquals( new Integer( 100 ), (Integer) row.getInteger( 0 ).get() );
    assertEquals( new Integer( 100 ), (Integer) row.getInteger( "some int" ).get() );

    assertEquals( 100.50, (double) row.getNumber( 1 ).get(), 0.001D );
    assertEquals( 100.50, (double) row.getNumber( "some Double" ).get(), 0.001D );
    assertEquals( bigDecimal, row.getBigNumber( 2 ).get() );
    assertEquals( bigDecimal, row.getBigNumber( "some Decimal" ).get() );

    assertTrue( row.getBoolean( 3 ).get() );
    assertTrue( row.getBoolean( "some Boolean" ).get() );

    assertEquals( date, row.getDate( 4 ).get() );
    assertEquals( date, row.getDate( "some Date" ).get() );

    assertEquals( "A String", row.getString( 5 ).get() );
    assertEquals( "A String", row.getString( "some String" ).get() );

    assertEquals( uri, row.getObject( 6 ).get() );
    assertEquals( uri, row.getObject( "some Serializable" ).get() );
  }
}