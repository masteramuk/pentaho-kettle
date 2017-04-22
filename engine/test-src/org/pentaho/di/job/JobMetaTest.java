/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2017 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.job;

import org.junit.Before;
import org.junit.Test;
import org.pentaho.di.core.NotePadMeta;
import org.pentaho.di.core.exception.IdNotFoundException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.exception.LookupReferencesException;
import org.pentaho.di.core.gui.Point;
import org.pentaho.di.core.listeners.ContentChangedListener;
import org.pentaho.di.job.entries.empty.JobEntryEmpty;
import org.pentaho.di.job.entries.trans.JobEntryTrans;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.repository.ObjectRevision;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.resource.ResourceDefinition;
import org.pentaho.di.resource.ResourceNamingInterface;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

public class JobMetaTest {

  private static final String JOB_META_NAME = "jobName";

  private JobMeta jobMeta;
  private RepositoryDirectoryInterface directoryJob;
  private ContentChangedListener listener;
  private ObjectRevision objectRevision;

  @Before
  public void setUp() {
    jobMeta = new JobMeta();
    // prepare
    directoryJob = mock( RepositoryDirectoryInterface.class );
    when( directoryJob.getPath() ).thenReturn( "directoryPath" );
    listener = mock( ContentChangedListener.class );
    objectRevision = mock( ObjectRevision.class );
    when( objectRevision.getName() ).thenReturn( "revisionName" );
    jobMeta.addContentChangedListener( listener );
    jobMeta.setRepositoryDirectory( directoryJob );
    jobMeta.setName( JOB_META_NAME );
    jobMeta.setObjectRevision( objectRevision );
  }

  @Test
  public void testPathExist() throws KettleXMLException, IOException, URISyntaxException {
    assertTrue( testPath( "je1-je4" ) );
  }

  @Test
  public void testPathNotExist() throws KettleXMLException, IOException, URISyntaxException {
    assertFalse( testPath( "je2-je4" ) );
  }

  private boolean testPath( String branch ) {
    JobEntryEmpty je1 = new JobEntryEmpty();
    je1.setName( "je1" );

    JobEntryEmpty je2 = new JobEntryEmpty();
    je2.setName( "je2" );

    JobHopMeta hop = new JobHopMeta( new JobEntryCopy( je1 ), new JobEntryCopy( je2 ) );
    jobMeta.addJobHop( hop );

    JobEntryEmpty je3 = new JobEntryEmpty();
    je3.setName( "je3" );
    hop = new JobHopMeta( new JobEntryCopy( je1 ), new JobEntryCopy( je3 ) );
    jobMeta.addJobHop( hop );

    JobEntryEmpty je4 = new JobEntryEmpty();
    je4.setName( "je4" );
    hop = new JobHopMeta( new JobEntryCopy( je3 ), new JobEntryCopy( je4 ) );
    jobMeta.addJobHop( hop );

    if ( branch.equals( "je1-je4" ) ) {
      return jobMeta.isPathExist( je1, je4 );
    } else if ( branch.equals( "je2-je4" ) ) {
      return jobMeta.isPathExist( je2, je4 );
    } else {
      return false;
    }
  }

  @Test
  public void testContentChangeListener() throws Exception {
    jobMeta.setChanged();
    jobMeta.setChanged( true );

    verify( listener, times( 2 ) ).contentChanged( same( jobMeta ) );

    jobMeta.clearChanged();
    jobMeta.setChanged( false );

    verify( listener, times( 2 ) ).contentSafe( same( jobMeta ) );

    jobMeta.removeContentChangedListener( listener );
    jobMeta.setChanged();
    jobMeta.setChanged( true );

    verifyNoMoreInteractions( listener );
  }

  @Test
  public void testLookupRepositoryReferences() throws Exception {
    jobMeta.clear();

    JobEntryTrans jobEntryMock = mock( JobEntryTrans.class );
    when( jobEntryMock.hasRepositoryReferences() ).thenReturn( true );

    JobEntryTrans brokenJobEntryMock = mock( JobEntryTrans.class );
    when( brokenJobEntryMock.hasRepositoryReferences() ).thenReturn( true );
    doThrow( mock( IdNotFoundException.class ) ).when( brokenJobEntryMock ).lookupRepositoryReferences( any(
        Repository.class ) );

    JobEntryCopy jobEntryCopy1 = mock( JobEntryCopy.class );
    when( jobEntryCopy1.getEntry() ).thenReturn( jobEntryMock );
    jobMeta.addJobEntry( 0, jobEntryCopy1 );

    JobEntryCopy jobEntryCopy2 = mock( JobEntryCopy.class );
    when( jobEntryCopy2.getEntry() ).thenReturn( brokenJobEntryMock );
    jobMeta.addJobEntry( 1, jobEntryCopy2 );

    JobEntryCopy jobEntryCopy3 = mock( JobEntryCopy.class );
    when( jobEntryCopy3.getEntry() ).thenReturn( jobEntryMock );
    jobMeta.addJobEntry( 2, jobEntryCopy3 );

    try {
      jobMeta.lookupRepositoryReferences( mock( Repository.class ) );
      fail( "no exception for broken entry" );
    } catch ( LookupReferencesException e ) {
      // ok
    }
    verify( jobEntryMock, times( 2 ) ).lookupRepositoryReferences( any( Repository.class ) );
  }

  /**
   * Given job meta object. <br/>
   * When the job is called to export resources, then the existing current directory should be used as a context to
   * locate resources.
   */
  @Test
  public void shouldUseExistingRepositoryDirectoryWhenExporting() throws KettleException {
    final JobMeta jobMetaSpy = spy( jobMeta );
    JobMeta jobMeta = new JobMeta() {
      @Override
      public Object realClone( boolean doClear ) {
        return jobMetaSpy;
      }
    };
    jobMeta.setRepositoryDirectory( directoryJob );
    jobMeta.setName( JOB_META_NAME );
    jobMeta.exportResources( null, new HashMap<String, ResourceDefinition>( 4 ), mock( ResourceNamingInterface.class ),
        null, null );

    // assert
    verify( jobMetaSpy ).setRepositoryDirectory( directoryJob );
  }

  @Test
  public void shouldUseCoordinatesOfItsStepsAndNotesWhenCalculatingMinimumPoint() {
    Point jobEntryPoint = new Point( 500, 500 );
    Point notePadMetaPoint = new Point( 400, 400 );
    JobEntryCopy jobEntryCopy = mock( JobEntryCopy.class );
    when( jobEntryCopy.getLocation() ).thenReturn( jobEntryPoint );
    NotePadMeta notePadMeta = mock( NotePadMeta.class );
    when( notePadMeta.getLocation() ).thenReturn( notePadMetaPoint );

    // empty Job return 0 coordinate point
    Point point = jobMeta.getMinimum();
    assertEquals( 0, point.x );
    assertEquals( 0, point.y );

    // when Job contains a single step or note, then jobMeta should return coordinates of it, subtracting borders
    jobMeta.addJobEntry( 0, jobEntryCopy );
    Point actualStepPoint = jobMeta.getMinimum();
    assertEquals( jobEntryPoint.x - JobMeta.BORDER_INDENT, actualStepPoint.x );
    assertEquals( jobEntryPoint.y - JobMeta.BORDER_INDENT, actualStepPoint.y );

    // when Job contains step or notes, then jobMeta should return minimal coordinates of them, subtracting borders
    jobMeta.addNote( notePadMeta );
    Point stepPoint = jobMeta.getMinimum();
    assertEquals( notePadMetaPoint.x - JobMeta.BORDER_INDENT, stepPoint.x );
    assertEquals( notePadMetaPoint.y - JobMeta.BORDER_INDENT, stepPoint.y );
  }

  @Test
  public void testEquals_oneNameNull() {
    assertFalse( testEquals( null, null, null, null ) );
  }

  @Test
  public void testEquals_secondNameNull() {
    jobMeta.setName( null );
    assertFalse( testEquals( JOB_META_NAME, null, null, null ) );
  }

  @Test
  public void testEquals_sameNameOtherDir() {
    RepositoryDirectoryInterface otherDirectory = mock( RepositoryDirectoryInterface.class );
    when( otherDirectory.getPath() ).thenReturn( "otherDirectoryPath" );
    assertFalse( testEquals( JOB_META_NAME, otherDirectory, null, null ) );
  }

  @Test
  public void testEquals_sameNameSameDirNullRev() {
    assertFalse( testEquals( JOB_META_NAME, directoryJob, null, null ) );
  }

  @Test
  public void testEquals_sameNameSameDirDiffRev() {
    ObjectRevision otherRevision = mock( ObjectRevision.class );
    when( otherRevision.getName() ).thenReturn( "otherRevision" );
    assertFalse( testEquals( JOB_META_NAME, directoryJob, otherRevision, null ) );
  }

  @Test
  public void testEquals_sameNameSameDirSameRev() {
    assertTrue( testEquals( JOB_META_NAME, directoryJob, objectRevision, null ) );
  }

  @Test
  public void testEquals_sameNameSameDirSameRevFilename() {
    assertFalse( testEquals( JOB_META_NAME, directoryJob, objectRevision, "Filename" ) );
  }

  @Test
  public void testEquals_sameFilename() {
    String newFilename = "Filename";
    jobMeta.setFilename( newFilename );
    assertFalse( testEquals( null, null, null, newFilename ) );
  }

  @Test
  public void testEquals_difFilenameSameName() {
    jobMeta.setFilename( "Filename" );
    assertFalse( testEquals( JOB_META_NAME, null, null, "OtherFileName" ) );
  }

  @Test
  public void testEquals_sameFilenameSameName() {
    String newFilename = "Filename";
    jobMeta.setFilename( newFilename );
    assertTrue( testEquals( JOB_META_NAME, null, null, newFilename ) );
  }

  @Test
  public void testEquals_sameFilenameDifName() {
    String newFilename = "Filename";
    jobMeta.setFilename( newFilename );
    assertFalse( testEquals( "OtherName", null, null, newFilename ) );
  }

  private boolean testEquals( String name, RepositoryDirectoryInterface repDirectory, ObjectRevision revision,
      String filename ) {
    JobMeta jobMeta2 = new JobMeta();
    jobMeta2.setName( name );
    jobMeta2.setRepositoryDirectory( repDirectory );
    jobMeta2.setObjectRevision( revision );
    jobMeta2.setFilename( filename );
    return jobMeta.equals( jobMeta2 );
  }
}
