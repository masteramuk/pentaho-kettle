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

package org.pentaho.di.engine.configuration.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pentaho.di.engine.configuration.api.RunConfiguration;
import org.pentaho.di.engine.configuration.api.RunConfigurationProvider;
import org.pentaho.di.engine.configuration.impl.pentaho.DefaultRunConfiguration;
import org.pentaho.di.engine.configuration.impl.pentaho.DefaultRunConfigurationExecutor;
import org.pentaho.di.engine.configuration.impl.pentaho.DefaultRunConfigurationProvider;
import org.pentaho.di.engine.configuration.impl.spark.SparkRunConfiguration;
import org.pentaho.di.engine.configuration.impl.spark.SparkRunConfigurationExecutor;
import org.pentaho.di.engine.configuration.impl.spark.SparkRunConfigurationProvider;
import org.pentaho.metastore.stores.xml.XmlMetaStore;
import org.pentaho.osgi.metastore.locator.api.MetastoreLocator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by bmorrise on 3/15/17.
 */
@RunWith( MockitoJUnitRunner.class )
public class RunConfigurationManagerTest {

  private static String XML_METASTORE = "src/test/resources/metastore_test";

  private RunConfigurationManager executionConfigurationManager;

  @Mock
  private DefaultRunConfigurationExecutor defaultRunConfigurationExecutor;

  @Before
  public void setup() throws Exception {

    XmlMetaStore xmlMetaStore = new XmlMetaStore( XML_METASTORE );
    MetastoreLocator metastoreLocator = () -> xmlMetaStore;

    DefaultRunConfigurationProvider defaultRunConfigurationProvider =
      new DefaultRunConfigurationProvider( metastoreLocator, defaultRunConfigurationExecutor );

    SparkRunConfigurationExecutor sparkRunConfigurationExecutor = new SparkRunConfigurationExecutor( null );
    SparkRunConfigurationProvider sparkRunConfigurationProvider =
      new SparkRunConfigurationProvider( metastoreLocator, sparkRunConfigurationExecutor );

    List<RunConfigurationProvider> runConfigurationProviders = new ArrayList<>();
    runConfigurationProviders.add( sparkRunConfigurationProvider );

    executionConfigurationManager = new RunConfigurationManager( runConfigurationProviders );
    executionConfigurationManager.setDefaultRunConfigurationProvider( defaultRunConfigurationProvider );

    DefaultRunConfiguration defaultRunConfiguration = new DefaultRunConfiguration();
    defaultRunConfiguration.setName( "Default Configuration" );
    defaultRunConfiguration.setDescription( "Default Configuration Description" );
    defaultRunConfiguration.setLocal( true );

    executionConfigurationManager.save( defaultRunConfiguration );

    SparkRunConfiguration sparkRunConfiguration = new SparkRunConfiguration();
    sparkRunConfiguration.setName( "Spark Configuration" );
    sparkRunConfiguration.setDescription( "Spark Configuration Description" );
    sparkRunConfiguration.setUrl( "127.0.0.1" );

    executionConfigurationManager.save( sparkRunConfiguration );
  }

  @After
  public void tearDown() {
    executionConfigurationManager.delete( "Default Configuration" );
    executionConfigurationManager.delete( "Spark Configuration" );
  }


  @Test
  public void testGetTypes() {
    String[] types = executionConfigurationManager.getTypes();
    assertTrue( Arrays.asList( types ).contains( DefaultRunConfiguration.TYPE ) );
    assertTrue( Arrays.asList( types ).contains( SparkRunConfiguration.TYPE ) );
  }

  @Test
  public void testLoad() {
    List<RunConfiguration> runConfigurations = executionConfigurationManager.load();

    assertEquals( runConfigurations.size(), 3 ); //Includes default
  }

  @Test
  public void testLoadByName() {

    DefaultRunConfiguration defaultRunConfiguration = (DefaultRunConfiguration) executionConfigurationManager
      .load( "Default Configuration" );

    assertNotNull( defaultRunConfiguration );
    assertEquals( defaultRunConfiguration.getName(), "Default Configuration" );
  }

  @Test
  public void testSaveAndDelete() {
    DefaultRunConfiguration defaultRunConfiguration = new DefaultRunConfiguration();
    defaultRunConfiguration.setName( "New Run Configuration" );

    executionConfigurationManager.save( defaultRunConfiguration );

    DefaultRunConfiguration loadedRunConfiguration =
      (DefaultRunConfiguration) executionConfigurationManager.load( "New Run Configuration" );

    assertEquals( loadedRunConfiguration.getName(), defaultRunConfiguration.getName() );

    executionConfigurationManager.delete( "New Run Configuration" );

    loadedRunConfiguration = (DefaultRunConfiguration) executionConfigurationManager.load( "New Run Configuration" );

    assertNull( loadedRunConfiguration );
  }

  @Test
  public void testGetNames() {
    List<String> names = executionConfigurationManager.getNames();

    assertTrue( names.contains( DefaultRunConfigurationProvider.DEFAULT_CONFIG_NAME ) );
    assertTrue( names.contains( "Default Configuration" ) );
    assertTrue( names.contains( "Spark Configuration" ) );
  }

  @Test
  public void testGetRunConfigurationByType() {
    DefaultRunConfiguration defaultRunConfiguration =
      (DefaultRunConfiguration) executionConfigurationManager.getRunConfigurationByType( DefaultRunConfiguration.TYPE );

    SparkRunConfiguration sparkRunConfiguration =
      (SparkRunConfiguration) executionConfigurationManager.getRunConfigurationByType( SparkRunConfiguration.TYPE );

    assertNotNull( defaultRunConfiguration );
    assertNotNull( sparkRunConfiguration );
  }

  @Test
  public void testGetExecutor() {
    DefaultRunConfigurationExecutor defaultRunConfigurationExecutor =
      (DefaultRunConfigurationExecutor) executionConfigurationManager.getExecutor( DefaultRunConfiguration.TYPE );
    assertNotNull( defaultRunConfigurationExecutor );
  }

}
