package it.polimi.genomics.importer.RemoteDatabase

import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}

import slick.driver.PostgresDriver.api._
//import slick.jdbc.PostgresProfile.api._
//import slick.driver.MySQLDriver.api._

import slick.jdbc.meta.MTable
import slick.lifted.Tag

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


object DbHandler {
  val conf = ConfigFactory.load()

  private val DONOR_TABLE_NAME = "donor"
  private val BIOSAMPLE_TABLE_NAME = "biosample"
  private val REPLICATE_TABLE_NAME = "replicate"
  private val CASE_TABLE_NAME = "case_study"
  private val DATASET_TABLE_NAME = "dataset"
  private val PROJECT_TABLE_NAME = "project"
  private val EXPERIMENTTYPE_TABLE_NAME = "experiment_type"
  private val ITEM_TABLE_NAME = "item"
  private val DERIVEDFROM_TABLE_NAME = "derived_from"
  private val CASEITEM_TABLE_NAME = "case2item"
  private val REPLICATEITEM_TABLE_NAME = "replicate2item"
  private val CASE_TCGA_MAPPING = "case_tcga_mapping"
  private val ONTOLOGY_TABLE = "ontology_table"



  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  /*val connectionUrl = "jdbc:postgresql://localhost/gecotest1?user=geco&password=geco78"

   val driver = "org.postgresql.Driver"
   val database = Database.forURL(connectionUrl, driver, keepAliveConnection = true)*/
  val database = Database.forURL(
   conf.getString("database.url"),
   conf.getString("database.username"),
   conf.getString("database.password"),
   driver=conf.getString("database.driver")
  )
  def setDatabase(): Unit = {

    val tables = Await.result(database.run(MTable.getTables), Duration.Inf).toList

    //donors
    logger.info("Connecting to the database...")

    if (!tables.exists(_.name.name == DONOR_TABLE_NAME)) {
      var queries = DBIO.seq(donors.schema.create)
      val setup = database.run(queries)
      Await.result(setup, Duration.Inf)
      logger.info("Table DONORS created")
    }

    //biosample
    if (!tables.exists(_.name.name == BIOSAMPLE_TABLE_NAME)) {
      val queries = DBIO.seq(
      bioSamples.schema.create
    )
      val setup = database.run(queries)
      Await.result(setup, Duration.Inf)
      logger.info("Table BIOSAMPLES created")
    }

    //replicate
    if (!tables.exists(_.name.name == REPLICATE_TABLE_NAME)) {
      val queries = DBIO.seq(
      replicates.schema.create
    )
      val setup = database.run(queries)
      Await.result(setup, Duration.Inf)
      logger.info("Table REPLICATES created")
    }

    //experimentType
    if (!tables.exists(_.name.name == EXPERIMENTTYPE_TABLE_NAME)) {
      val queries = DBIO.seq(
        experimentsType.schema.create
      )
      val setup = database.run(queries)
      Await.result(setup, Duration.Inf)
      logger.info("Table EXPERIMENTSTYPE created")
    }

    //project
    if (!tables.exists(_.name.name == PROJECT_TABLE_NAME)) {
      val queries = DBIO.seq(
      projects.schema.create
      )
      val setup = database.run(queries)
      Await.result(setup, Duration.Inf)
      logger.info("Table PROJECTS created")
    }

    //dataset
    if (!tables.exists(_.name.name == DATASET_TABLE_NAME)) {
      val queries = DBIO.seq(
      datasets.schema.create
      )
      val setup = database.run(queries)
      Await.result(setup, Duration.Inf)
      logger.info("Table DATASETS created")
    }

    //case
    if (!tables.exists(_.name.name == CASE_TABLE_NAME)) {
      val queries = DBIO.seq(
      cases.schema.create
      )
      val setup = database.run(queries)
      Await.result(setup, Duration.Inf)
      logger.info("Table CASES created")
    }

    //item
    if (!tables.exists(_.name.name == ITEM_TABLE_NAME)) {
      val queries = DBIO.seq(
      items.schema.create
      )
      val setup = database.run(queries)
      Await.result(setup, Duration.Inf)
      logger.info("Table ITEMS created")
    }

    if (!tables.exists(_.name.name == REPLICATEITEM_TABLE_NAME)) {
      val queries = DBIO.seq(
        replicatesItems.schema.create
      )
      val setup = database.run(queries)
      Await.result(setup, Duration.Inf)
      logger.info("Table REPLICATESITEMS created")
    }

    //caseitem
    if (!tables.exists(_.name.name == CASEITEM_TABLE_NAME)) {
      val queries = DBIO.seq(
      casesItems.schema.create
      )
      val setup = database.run(queries)
      Await.result(setup, Duration.Inf)
      logger.info("Table CASESITEMS created")
    }

    if (!tables.exists(_.name.name == DERIVEDFROM_TABLE_NAME)) {
      val queries = DBIO.seq(
        derivedFrom.schema.create
      )
      val setup = database.run(queries)
      Await.result(setup, Duration.Inf)
      logger.info("Table DERIVEDFROM created")
    }

    if (!tables.exists(_.name.name == CASE_TCGA_MAPPING)) {
      val queries = DBIO.seq(
        caseTcgaMapping.schema.create
      )
      /*for (line <- Source.fromFile((getClass.getResource("/mapping.csv").getFile)).getLines) {
        val cols = line.split(",").map(_.trim)
        insertCaseTcgaMapping(cols(0),cols(1))
      }*/
      val setup = database.run(queries)
      Await.result(setup, Duration.Inf)
      logger.info("Table CASE TCGA MAPPING created")
    }

    if (!tables.exists(_.name.name == ONTOLOGY_TABLE)) {
      val queries = DBIO.seq(
        ontologyTable.schema.create
      )
      val setup = database.run(queries)
      Await.result(setup, Duration.Inf)
      logger.info("Table ONTOLOGY created")
    }
  }

  def closeDatabase(): Unit = {
    val closing = database.shutdown
    Await.result(closing,Duration.Inf)
  }

  def toOption[T](value: T): Option[T] = {
    if(!value.equals(0))
      Option(value)
    else
      None
  }

  //Insert Method

  def insertDonor(sourceId: String, species : String, age: Int, gender: String, ethnicity: String): Int ={
    val idQuery = (donors returning donors.map(_.donorId)) += (None, sourceId, Option(species), None, this.toOption[Int](age), Option(gender), Option(ethnicity), None)
    val executionId = database.run(idQuery)
    val id = Await.result(executionId, Duration.Inf)
    id
  }

  def updateDonor(sourceId: String, species : String, age: Int, gender: String, ethnicity: String): Int ={
    val query = for { donor <- donors if donor.sourceId === sourceId } yield (donor.species, donor.age, donor.gender, donor.ethnicity)
    val updateAction = query.update(Option(species),this.toOption[Int](age),Option(gender),Option(ethnicity))
    val execution = database.run(updateAction)
    Await.result(execution, Duration.Inf)
    val idQuery = donors.filter(_.sourceId === sourceId).map(_.donorId)
    val returnAction = idQuery.result
    val execution2 = database.run(returnAction)
    val id = Await.result(execution2,Duration.Inf)
    id.head
  }

  def updateDonorById(donorId: Int, sourceId: String, species : String, age: Int, gender: String, ethnicity: String): Int ={
    val query = for { donor <- donors if donor.donorId === donorId } yield (donor.sourceId, donor.species, donor.age, donor.gender, donor.ethnicity)
    val updateAction = query.update(sourceId, Option(species),this.toOption[Int](age),Option(gender),Option(ethnicity))
    val execution = database.run(updateAction)
    Await.result(execution, Duration.Inf)
    donorId
  }

  def insertBioSample(donorId: Int, sourceId: String, types : String, tissue: String, cellLine: String, isHealthy: Boolean, disease: String): Int ={
    val idQuery = (bioSamples returning bioSamples.map(_.bioSampleId)) += (None, donorId, sourceId, Option(types), Option(tissue), None, Option(cellLine), None, Option(isHealthy), Option(disease), None)
    val executionId = database.run(idQuery)
    val id = Await.result(executionId, Duration.Inf)
    id
  }

  def updateBioSample(donorId: Int, sourceId: String, types : String, tIussue: String, cellLine: String, isHealthy: Boolean, disease: String): Int ={
    val query = for { bioSample <- bioSamples if bioSample.sourceId === sourceId }
      yield (bioSample.donorId,bioSample.types, bioSample.tissue, bioSample.cellLine, bioSample.isHealthy, bioSample.disease)
    val updateAction = query.update(donorId,Option(types),Option(tIussue),Option(cellLine),Option(isHealthy),Option(disease))
    val execution = database.run(updateAction)
    Await.result(execution, Duration.Inf)
    val idQuery = bioSamples.filter(_.sourceId === sourceId).map(_.bioSampleId)
    val returnAction = idQuery.result
    val execution2 = database.run(returnAction)
    val id = Await.result(execution2,Duration.Inf)
    id.head
  }

  def updateBioSampleById(bioSampleId: Int, donorId: Int, sourceId: String, types : String, tIussue: String, cellLine: String, isHealthy: Boolean, disease: String): Int ={
    val query = for { bioSample <- bioSamples if bioSample.bioSampleId === bioSampleId}
      yield (bioSample.donorId, bioSample.sourceId,bioSample.types, bioSample.tissue, bioSample.cellLine, bioSample.isHealthy, bioSample.disease)
    val updateAction = query.update(donorId, sourceId, Option(types), Option(tIussue), Option(cellLine), Option(isHealthy), Option(disease))
    val execution = database.run(updateAction)
    Await.result(execution, Duration.Inf)
    bioSampleId
  }

  def insertReplicate(bioSampleId: Int, sourceId: String, bioReplicateNum : Int, techReplicateNum: Int): Int ={
    val idQuery = (replicates returning replicates.map(_.replicateId))+= (None, bioSampleId, sourceId, this.toOption[Int](bioReplicateNum), this.toOption[Int](techReplicateNum))
    val executionId = database.run(idQuery)
    val id = Await.result(executionId, Duration.Inf)
    id
  }

  def updateReplicate(bioSampleId: Int, sourceId: String, bioReplicateNum : Int, techReplicateNum: Int): Int ={
    val query = for { replicate <- replicates if replicate.sourceId === sourceId } yield (replicate.bioSampleId, replicate.bioReplicateNum, replicate.techReplicateNum)
    val updateAction = query.update(bioSampleId,this.toOption[Int](bioReplicateNum),this.toOption[Int](techReplicateNum))
    val execution = database.run(updateAction)
    Await.result(execution, Duration.Inf)
    val idQuery = replicates.filter(_.sourceId === sourceId).map(_.replicateId)
    val returnAction = idQuery.result
    val execution2 = database.run(returnAction)
    val id = Await.result(execution2,Duration.Inf)
    id.head
  }

  def updateReplicateById(replicateId: Int, bioSampleId: Int, sourceId: String, bioReplicateNum : Int, techReplicateNum: Int): Int ={
    val query = for { replicate <- replicates if replicate.replicateId === replicateId } yield (replicate.bioSampleId, replicate.sourceId,replicate.bioReplicateNum, replicate.techReplicateNum)
    val updateAction = query.update(bioSampleId,sourceId, this.toOption[Int](bioReplicateNum),this.toOption[Int](techReplicateNum))
    val execution = database.run(updateAction)
    Await.result(execution, Duration.Inf)
    replicateId
  }

  def insertExperimentType(technique: String, feature: String, target: String, antibody: String): Int ={
    val idQuery = (experimentsType returning experimentsType.map(_.experimentTypeId))+= (None, Option(technique), None, Option(feature), None, Option(target), None, Option(antibody))
    val executionId = database.run(idQuery)
    val id = Await.result(executionId, Duration.Inf)
    id
  }

  def updateExperimentType(technique: String, feature: String, target: String, antibody: String): Int ={
    val query = for { experimentType <- experimentsType
                      if experimentType.technique === technique && experimentType.feature === feature && experimentType.target === target }
      yield experimentType.antibody
    val updateAction = query.update(Option(antibody))
    val execution = database.run(updateAction)
    Await.result(execution, Duration.Inf)
    val idQuery = experimentsType.filter(value => { value.technique === technique && value.feature === feature && value.target === target}).map(_.experimentTypeId)
    val returnAction = idQuery.result
    val execution2 = database.run(returnAction)
    val id = Await.result(execution2,Duration.Inf)
    id.head
  }

  def updateExperimentTypeById(experimentTypeId: Int, technique: String, feature: String, target: String, antibody: String): Int ={
    val query = for { experimentType <- experimentsType
                      if experimentType.experimentTypeId === experimentTypeId}
      yield (experimentType.technique, experimentType.feature, experimentType.target, experimentType.antibody)
    val updateAction = query.update(Option(technique), Option(feature), Option(target), Option(antibody))
    val execution = database.run(updateAction)
    Await.result(execution, Duration.Inf)
    experimentTypeId
  }

  def insertProject(projectName: String, programName: String): Int ={
    val idQuery = (projects returning projects.map(_.projectId)) += (None, projectName, Option(programName))
    val executionId = database.run(idQuery)
    val id = Await.result(executionId, Duration.Inf)
    id
  }

  def updateProject(projectName: String, programName: String): Int ={
    val query = for { project <- projects if project.projectName === projectName } yield project.programName
    val updateAction = query.update(Option(programName))
    val execution = database.run(updateAction)
    Await.result(execution, Duration.Inf)
    val idQuery = projects.filter(_.projectName === projectName).map(_.projectId)
    val returnAction = idQuery.result
    val execution2 = database.run(returnAction)
    val id = Await.result(execution2,Duration.Inf)
    id.head
  }

  def updateProjectById(projectId: Int, projectName: String, programName: String): Int ={
    val query = for { project <- projects if project.projectId === projectId } yield (project.projectName, project.programName)
    val updateAction = query.update(projectName, Option(programName))
    val execution = database.run(updateAction)
    Await.result(execution, Duration.Inf)
    projectId
  }

  def insertCase(projectId: Int, sourceId: String, sourceSite: String, externalRef: String): Int ={
    val idQuery = (cases returning cases.map(_.caseId))+= (None, projectId, sourceId, Option(sourceSite), Option(externalRef))
    val executionId = database.run(idQuery)
    val id = Await.result(executionId, Duration.Inf)
    id
  }

  def updateCase(projectId: Int, sourceId: String, sourceSite: String, externalRef: String): Int ={
    val query = for { cas <- cases if cas.sourceId === sourceId } yield (cas.projectId, cas.sourceSite, cas.externalRef)
    val updateAction = query.update(projectId, Option(sourceSite), Option(externalRef))
    val execution = database.run(updateAction)
    Await.result(execution, Duration.Inf)
    val idQuery = cases.filter(_.sourceId === sourceId).map(_.caseId)
    val returnAction = idQuery.result
    val execution2 = database.run(returnAction)
    val id = Await.result(execution2,Duration.Inf)
    id.head
  }

  def updateCaseById(caseId: Int, projectId: Int, sourceId: String, sourceSite: String, externalRef: String): Int ={
    val query = for { cas <- cases if cas.caseId === caseId } yield (cas.sourceId, cas.projectId, cas.sourceSite, cas.externalRef)
    val updateAction = query.update(sourceId, projectId, Option(sourceSite), Option(externalRef))
    val execution = database.run(updateAction)
    Await.result(execution, Duration.Inf)
    caseId
  }

  def insertDataset(name: String, dataType: String, format: String, assembly: String, isAnn: Boolean, annotation: String, localUrl: String): Int ={
    val idQuery = (datasets returning datasets.map(_.datasetId))+= (None, name, Option(dataType), Option(format), Option(assembly), Option(isAnn), Option(annotation), None, Option(localUrl))
    val executionId = database.run(idQuery)
    val id = Await.result(executionId, Duration.Inf)
    id
  }

  def updateDataset(name: String, dataType: String, format: String, assembly: String, isAnn: Boolean, annotation: String, localUrl: String): Int ={
    val query = for { dataset <- datasets if dataset.dataType === dataType && dataset.format === format && dataset.assembly === assembly && dataset.annotation == annotation }
      yield (dataset.name, dataset.isAnn, dataset.localUrl)
    val updateAction = query.update(name, Option(isAnn), Option(localUrl))
    val execution = database.run(updateAction)
    Await.result(execution, Duration.Inf)
    val idQuery = datasets.filter(value => { value.dataType === dataType && value.format === format && value.assembly === assembly && value.annotation === annotation}).map(_.datasetId)
    val returnAction = idQuery.result
    val execution2 = database.run(returnAction)
    val id = Await.result(execution2,Duration.Inf)
    id.head
  }

  def updateDatasetById(datasetId: Int, name: String, dataType: String, format: String, assembly: String, isAnn: Boolean, annotation: String, localUrl: String): Int ={
    val query = for { dataset <- datasets if dataset.datasetId === datasetId }
      yield (dataset.name, dataset.dataType, dataset.format, dataset.assembly, dataset.isAnn, dataset.annotation, dataset.localUrl)
    val updateAction = query.update(name, Option(dataType), Option(format), Option(assembly), Option(isAnn), Option(annotation), Option(localUrl))
    val execution = database.run(updateAction)
    Await.result(execution, Duration.Inf)
    datasetId
  }


  def insertItem(experimentTypeId: Int, datasetId: Int, sourceId: String, size: Long, platform: String,  pipeline: String, sourceUrl: String): Int ={
    val idQuery = (items returning items.map(_.itemId))+= (None, experimentTypeId, datasetId, sourceId, this.toOption[Long](size), Option(platform),  None, Option(pipeline), Option(sourceUrl))
    val executionId = database.run(idQuery)
    val id = Await.result(executionId, Duration.Inf)
    id
  }

  def updateItem(experimentTypeId: Int, datasetId: Int, sourceId: String, size: Long, platform: String,  pipeline: String, sourceUrl: String): Int ={
    val updateQuery = for { item <- items if item.sourceId === sourceId } yield (item.experimentTypeId, item.datasetId,  item.size, item.platform, item.pipeline, item.sourceUrl)
    val updateAction = updateQuery.update(experimentTypeId, datasetId, this.toOption[Long](size), Option(platform), Option(pipeline), Option(sourceUrl))
    val execution = database.run(updateAction)
    Await.result(execution, Duration.Inf)
    val idQuery = items.filter(_.sourceId === sourceId).map(_.itemId)
    val returnAction = idQuery.result
    val execution2 = database.run(returnAction)
    val id = Await.result(execution2,Duration.Inf)
    id.head
  }

  def updateItemById(itemId: Int, experimentTypeId: Int, datasetId: Int, sourceId: String, size: Long, platform: String,  pipeline: String, sourceUrl: String): Int ={
    val updateQuery = for { item <- items if item.itemId === itemId } yield (item.experimentTypeId, item.datasetId, item.sourceId, item.size, item.platform, item.pipeline, item.sourceUrl)
    val updateAction = updateQuery.update(experimentTypeId, datasetId, sourceId, this.toOption[Long](size), Option(platform), Option(pipeline), Option(sourceUrl))
    val execution = database.run(updateAction)
    Await.result(execution, Duration.Inf)
    itemId
  }

  def insertReplicateItem(itemId: Int, replicateId: Int): Int ={
    val insertActions = DBIO.seq(
      replicatesItems += (itemId,replicateId)
    )
    Await.result(database.run(insertActions), Duration.Inf)
    1
  }

  def insertCaseItem(itemId: Int, caseId: Int): Int ={
    val insertActions = DBIO.seq(
      casesItems += (itemId,caseId)
    )
    Await.result(database.run(insertActions), Duration.Inf)
    1
  }

  def insertDerivedFrom(initialItemId: Int, finalItemId: Int, operation: String): Int ={
    val insertActions = DBIO.seq(
      derivedFrom += (initialItemId,finalItemId, Option(operation))
    )
    Await.result(database.run(insertActions), Duration.Inf)
    1
  }

  def updateDerivedFrom(initialItemId: Int, finalItemId: Int, operation: String): Int ={
    val query = for { derived <- derivedFrom if derived.initialItemId === initialItemId && derived.finalItemId === finalItemId } yield derived.operation
    val updateAction = query.update(Option(operation))
    val execution = database.run(updateAction)
    val id = Await.result(execution, Duration.Inf)
    id
  }

  def insertCaseTcgaMapping(code: String, sourceSite: String): Int = {
    val insertActions = DBIO.seq(
      caseTcgaMapping += (code, sourceSite)
    )
    Await.result(database.run(insertActions), Duration.Inf)
    1
  }

  def insertOntology(tableId: Int, tableName: String, tableColumn: String, originalKey: String, originalValue: String, ontologicalCode: String): Unit = {
    val idQuery = (ontologyTable returning ontologyTable) += (tableId, tableName, tableColumn, originalKey, originalValue, Option(ontologicalCode))
    val executionId = database.run(idQuery)
    Await.result(executionId, Duration.Inf)
  }

  def updateOntology(tableId: Int, tableName: String, tableColumn: String, originalKey: String, originalValue: String, ontologicalCode: String): Unit = {
    val query = for { ontology <- ontologyTable if ontology.tableId === tableId && ontology.tableNames === tableName && ontology.tableColumn === tableColumn }
      yield (ontology.originalKey, ontology.originalValue, ontology.ontologicalCode)
    val updateAction = query.update(originalKey, originalValue, Option(ontologicalCode))
    val execution = database.run(updateAction)
    Await.result(execution, Duration.Inf)
  }

  /**
    *
    * @param result A general query
    * @return true if the element must be inserted,
    *         false if the element is already available in the Database
    */
  def checkResult(result: Future[Seq[Any]]): Boolean = {
    val res = Await.result(result, Duration.Inf)
    if(res.isEmpty)
      true
    else
      false
  }

  /**
    *
    * @param result A general query
    * @return the table id
    */
  def checkId(result: Future[Seq[Int]]): Int = {
    val res = Await.result(result, Duration.Inf)
    if(res.isEmpty)
      -1
    else
      res.head
  }

  def checkInsertDonor(sourceId: String): Boolean = {
    val query = donors.filter(_.sourceId === sourceId)
    val action = query.result
    val result = database.run(action)
    checkResult(result)
  }

  def checkInsertBioSample(sourceId: String): Boolean = {
    val query = bioSamples.filter(_.sourceId === sourceId)
    val action = query.result
    val result = database.run(action)
    checkResult(result)
  }

  def checkInsertReplicate(sourceId: String): Boolean = {
    val query = replicates.filter(_.sourceId === sourceId)
    val action = query.result
    val result = database.run(action)
    checkResult(result)
  }

  def checkInsertExperimentType(technique: String, feature: String, target: String): Boolean = {
    val query = experimentsType.filter( value => { value.technique === technique && value.feature === feature && value.target === target})
    val action = query.result
    val result = database.run(action)
    checkResult(result)
  }

  def checkInsertProject(projectName: String): Boolean = {
    val query = projects.filter(_.projectName === projectName)
    val action = query.result
    val result = database.run(action)
    checkResult(result)
  }

  def checkInsertCase(sourceId: String): Boolean = {
    val query = cases.filter(_.sourceId === sourceId)
    val action = query.result
    val result = database.run(action)
    checkResult(result)
  }

  def checkInsertDataset(dataType: String, format: String, assembly: String, annotation: String): Boolean = {
    val query = datasets.filter( value => { value.dataType === dataType && value.format === format && value.assembly === assembly && value.annotation === annotation})
    val action = query.result
    val result = database.run(action)
    checkResult(result)
  }

  def checkInsertItem(sourceId: String): Boolean = {
    val query = items.filter(_.sourceId === sourceId)
    val action = query.result
    val result = database.run(action)
    checkResult(result)
  }

  def checkInsertCaseItem(itemId: Int, caseId: Int): Boolean = {
    val query = casesItems.filter(_.itemId === itemId).filter(_.caseId === caseId)
    val action = query.result
    val result = database.run(action)
    checkResult(result)
  }

  def checkInsertReplicateItem(itemId: Int, replicateId: Int): Boolean = {
    val query = replicatesItems.filter(_.itemId === itemId).filter(_.replicateId === replicateId)
    val action = query.result
    val result = database.run(action)
    checkResult(result)
  }

  def checkInsertDerivedFrom(initialItemId: Int, finalItemId: Int): Boolean = {
    val query = derivedFrom.filter(_.initialItemId === initialItemId).filter(_.finalItemId === finalItemId)
    val action = query.result
    val result = database.run(action)
    checkResult(result)
  }

  def checkInsertOntology(tableId: Int, tableName: String, tableColumn: String): Boolean = {
    val query = ontologyTable.filter( value => { value.tableId === tableId && value.tableNames === tableName && value.tableColumn === tableColumn})
    val action = query.result
    val result = database.run(action)
    checkResult(result)
  }


  def getDonorId(id: String): Int = {
    val query = donors.filter(_.sourceId === id).map(_.donorId)
    val action = query.result
    val result = database.run(action)
    checkId(result)
  }


  def getBioSampleId(sourceId: String): Int = {
    val query = bioSamples.filter(_.sourceId === sourceId).map(_.bioSampleId)
    val action = query.result
    val result = database.run(action)
    checkId(result)
  }

  def getReplicateId(sourceId: String): Int = {
    val query = replicates.filter(_.sourceId === sourceId).map(_.replicateId)
    val action = query.result
    val result = database.run(action)
    checkId(result)
  }

  def getExperimentTypeId(technique: String, feature: String, target: String): Int = {
    val query = experimentsType.filter(value => { value.technique === technique && value.feature === feature && value.target === target}).map(_.experimentTypeId)
    val action = query.result
    val result = database.run(action)
    checkId(result)
  }

  def getProjectId(projectName : String): Int = {
    val query = projects.filter(_.projectName === projectName).map(_.projectId)
    val action = query.result
    val result = database.run(action)
    checkId(result)
  }

  def getCaseId(sourceId : String): Int = {
    val query = cases.filter(_.sourceId === sourceId).map(_.caseId)
    val action = query.result
    val result = database.run(action)
    checkId(result)
  }

  def getDatasetId(dataType : String, format: String, assembly: String, annotation: String): Int = {
    val query = datasets.filter(value => { value.dataType === dataType && value.format === format && value.assembly === assembly && value.annotation === annotation}).map(_.datasetId)
    val action = query.result
    val result = database.run(action)
    checkId(result)
  }

  def getItemId(sourceId : String): Int = {
    val query = items.filter(_.sourceId === sourceId).map(_.itemId)
    val action = query.result
    val result = database.run(action)
    checkId(result)
  }


  def getSourceSiteByCode(code : String): String ={
    val idQuery = caseTcgaMapping.filter(_.code === code).map(_.sourceSite)
    val returnAction = idQuery.result
    val execution2 = database.run(returnAction)
    val sourceSite = Await.result(execution2,Duration.Inf)
    sourceSite.head
  }

  /*def derivedFromId(initialItemId: Int, finalItemId: Int): Int = {
    val query = derivedFrom.filter(_.initialItemId === initialItemId).filter(_.finalItemId === finalItemId)
    val action = query.result
    val result = database.run(action)
    checkId(result)
  }*/

  /*def getCasesItemId(itemId : Int, caseId: Int): Int = {
    val query = casesItems.filter(_.itemId === itemId).filter(_.caseId === caseId).map(_.pk)
    val action = query.result
    val result = database.run(action)
    checkId(result)
  }*/

  def getDonorById(id: Int): Seq[(String, Option[String], Option[Int], Option[String], Option[String])] = {
    val query = for { donor <- donors if donor.donorId === id } yield (donor.sourceId, donor.species, donor.age, donor.gender, donor.ethnicity)
    val action = query.result
    val result = database.run(action)
    val res = Await.result(result, Duration.Inf)
    res
  }

  def getBiosampleById(id: Int): Seq[(Int, String, Option[String], Option[String], Option[String], Option[Boolean], Option[String])] = {
    val query = for { bioSample <- bioSamples if bioSample.bioSampleId === id } yield (bioSample.donorId, bioSample.sourceId, bioSample.types, bioSample.tissue, bioSample.cellLine, bioSample.isHealthy, bioSample.disease)
    val action = query.result
    val result = database.run(action)
    val res = Await.result(result, Duration.Inf)
    res
  }

  def getItemBySourceId(sourceId: String): Seq[(Int, Int, Int, String, Option[Long], Option[String], Option[String], Option[String])] = {
    val query = for { item <- items if item.sourceId === sourceId } yield (item.itemId, item.experimentTypeId, item.datasetId, item.sourceId,  item.size, item.pipeline, item.platform, item.sourceUrl)
    val action = query.result
    val result = database.run(action)
    val res = Await.result(result, Duration.Inf)
    res
  }


  def getDatasetById(id: Int): Seq[(Int, String, Option[String], Option[String], Option[String], Option[Boolean], Option[String], Option[String])] = {
    val query = for { dataset <- datasets if dataset.datasetId === id } yield (dataset.datasetId, dataset.name, dataset.dataType, dataset.format, dataset.assembly, dataset.isAnn, dataset.annotation, dataset.localUrl)
    val action = query.result
    val result = database.run(action)
    val res = Await.result(result, Duration.Inf)
    res
  }

  def getExperimentTypeById(id: Int): Seq[(Int, Option[String], Option[String], Option[String], Option[String])] = {
    val query = for { experimentType <- experimentsType if experimentType.experimentTypeId === id } yield (experimentType.experimentTypeId, experimentType.technique, experimentType.feature, experimentType.target, experimentType.antibody)
    val action = query.result
    val result = database.run(action)
    val res = Await.result(result, Duration.Inf)
    res
  }

  def getProjectById(id: Int): Seq[(String, Option[String])] = {
    val query = for { project <- projects if project.projectId === id } yield (project.projectName, project.programName)
    val action = query.result
    val result = database.run(action)
    val res = Await.result(result, Duration.Inf)
    res
  }

  def getCaseByItemId(itemId: Int): Seq[(Int, String, Option[String], Option[String])] ={
    val crossJoin = for {
      (caseItem, cases) <- casesItems.filter(_.itemId === itemId).join(cases).on(_.caseId === _.caseId)
    } yield (cases.projectId, cases.sourceId, cases.sourceSite, cases.externalRef)
    val action = crossJoin.result
    val result = database.run(action)
    val res = Await.result(result, Duration.Inf)
    res
  }

  def getReplicateByItemId (itemId: Int): Seq[(Int, String, Option[Int], Option[Int])] ={
    val crossJoin = for {
      (replicateItem, replicates) <- replicatesItems.filter(_.itemId === itemId).join(replicates).on(_.replicateId === _.replicateId)
    } yield (replicates.bioSampleId, replicates.sourceId, replicates.bioReplicateNum, replicates.techReplicateNum)
    val action = crossJoin.result
    val result = database.run(action)
    val res = Await.result(result, Duration.Inf)
    res
  }

  def getItemsByDerivedFromId (finalId: Int): Seq[(Int, Int, Int, String, Option[Long], Option[String], Option[String], Option[String])] ={
    val crossJoin = for {
      (derivedFrom, item) <- derivedFrom.filter(_.finalItemId === finalId).join(items).on(_.initialItemId === _.itemId)
    } yield (item.itemId, item.experimentTypeId, item.datasetId, item.sourceId, item.size, item.platform, item.pipeline, item.sourceUrl)
    val action = crossJoin.result
    val result = database.run(action)
    val res = Await.result(result, Duration.Inf)
    res
  }


  //-------------------------------------DATABASE SCHEMAS---------------------------------------------------------------

  //---------------------------------- Definition of the SOURCES table--------------------------------------------------

  class Donors(tag: Tag) extends
    Table[(Option[Int], String, Option[String], Option[Int], Option[Int], Option[String], Option[String], Option[Int])](tag, DONOR_TABLE_NAME) {
    def donorId = column[Int]("donor_id", O.PrimaryKey, O.AutoInc)

    def sourceId = column[String]("source_id", O.Unique)

    def species = column[Option[String]]("species", O.Default(None))

    def speciesTid = column[Option[Int]]("species_tid", O.Default(None))

    def age = column[Option[Int]]("age", O.Default(None))

    def gender = column[Option[String]]("gender", O.Default(None))

    def ethnicity = column[Option[String]]("ethnicity", O.Default(None))

    def ethnicityTid = column[Option[Int]]("ethnicity_tid", O.Default(None))

    def * = (donorId.?, sourceId, species, speciesTid, age, gender, ethnicity, ethnicityTid)
  }

  val donors = TableQuery[Donors]

  class BioSamples(tag: Tag) extends
    Table[(Option[Int], Int, String, Option[String], Option[String], Option[Int], Option[String], Option[Int], Option[Boolean], Option[String], Option[Int])](tag, BIOSAMPLE_TABLE_NAME) {
    def bioSampleId = column[Int]("biosample_id", O.PrimaryKey, O.AutoInc)

    def donorId = column[Int]("donor_id")

    def sourceId = column[String]("source_id", O.Unique)

    def types = column[Option[String]]("type", O.Default(None))

    def tissue = column[Option[String]]("tissue", O.Default(None))

    def tissueTid = column[Option[Int]]("tissue_tid", O.Default(None))

    def cellLine = column[Option[String]]("cell_line", O.Default(None))

    def cellLineTid = column[Option[Int]]("cell_line_tid", O.Default(None))

    def isHealthy = column[Option[Boolean]]("is_healthy", O.Default(None))

    def disease = column[Option[String]]("disease", O.Default(None))

    def diseaseTid = column[Option[Int]]("disease_tid", O.Default(None))

    def donor = foreignKey("biosamples_donor_fk", donorId, donors)(
      _.donorId,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )

    def * = (bioSampleId.?, donorId, sourceId, types, tissue, tissueTid, cellLine, cellLineTid, isHealthy, disease, diseaseTid)
  }


  val bioSamples = TableQuery[BioSamples]

  class Replicates(tag: Tag) extends
    Table[(Option[Int], Int, String, Option[Int], Option[Int])](tag, REPLICATE_TABLE_NAME) {
    def replicateId = column[Int]("replicate_id", O.PrimaryKey, O.AutoInc)

    def bioSampleId = column[Int]("biosample_id")

    def sourceId = column[String]("source_id", O.Unique)

    def bioReplicateNum = column[Option[Int]]("bio_replicate_num", O.Default(None))

    def techReplicateNum = column[Option[Int]]("tech_replicate_num", O.Default(None))

    def bioSample = foreignKey("replicates_donor_fk", bioSampleId, bioSamples)(
      _.bioSampleId,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )

    def * = (replicateId.?, bioSampleId, sourceId, bioReplicateNum, techReplicateNum)
  }

  val replicates = TableQuery[Replicates]

  class ExperimentsType(tag: Tag) extends
    Table[(Option[Int], Option[String], Option[Int], Option[String], Option[Int], Option[String], Option[Int], Option[String])](tag, EXPERIMENTTYPE_TABLE_NAME) {
    def experimentTypeId = column[Int]("experiment_type_id", O.PrimaryKey, O.AutoInc)

    def technique = column[Option[String]]("technique", O.Default(None))

    def techniqueTid = column[Option[Int]]("technique_tid", O.Default(None))

    def feature = column[Option[String]]("feature", O.Default(None))

    def featureTid = column[Option[Int]]("feature_tid", O.Default(None))

    def target = column[Option[String]]("target", O.Default(None))

    def targetTid = column[Option[Int]]("target_tid", O.Default(None))

    def antibody = column[Option[String]]("antibody", O.Default(None))

    def uniqueKey = index("technique_feature_target", (technique, feature, target), unique = true)

    def * = (experimentTypeId.?, technique, techniqueTid, feature, featureTid, target, targetTid, antibody)
  }

  val experimentsType = TableQuery[ExperimentsType]

  class Projects(tag: Tag) extends
    Table[(Option[Int], String, Option[String])](tag, PROJECT_TABLE_NAME) {
    def projectId = column[Int]("project_id", O.PrimaryKey, O.AutoInc)

    def projectName =  column[String]("project_name", O.Unique)

    def programName =  column[Option[String]]("program_name", O.Default(None))

    def * = (projectId.?, projectName, programName)
  }

  val projects = TableQuery[Projects]

  class Cases(tag: Tag) extends
    Table[(Option[Int], Int, String, Option[String], Option[String])](tag, CASE_TABLE_NAME) {
    def caseId = column[Int]("case_study_id", O.PrimaryKey, O.AutoInc)

    def projectId = column[Int]("project_id")

    def sourceId = column[String]("source_id", O.Unique)

    def sourceSite = column[Option[String]]("source_site", O.Default(None))

    def externalRef = column[Option[String]]("external_ref", O.Default(None))

    def project = foreignKey("cases_project_fk", projectId, projects)(
      _.projectId,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )

    def * = (caseId.?, projectId, sourceId, sourceSite, externalRef)
  }

  val cases = TableQuery[Cases]

  class Datasets(tag: Tag) extends
    Table[(Option[Int], String, Option[String],Option[String], Option[String], Option[Boolean], Option[String], Option[Int], Option[String])](tag, DATASET_TABLE_NAME) {
    def datasetId = column[Int]("dataset_id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name", O.Default("DS_NAME"))

    def dataType = column[Option[String]]("data_type", O.Default(None))

    def format = column[Option[String]]("format", O.Default(None))

    def assembly = column[Option[String]]("assembly", O.Default(None))

    def isAnn = column[Option[Boolean]]("is_ann", O.Default(None))

    def annotation = column[Option[String]]("annotation", O.Default(None))

    def annotationTid = column[Option[Int]]("annotation_tid", O.Default(None))

    def localUrl = column[Option[String]]("local_url", O.Default(None))

    def uniqueKey = index("datatype_format_assembly_annotation", (dataType,format,assembly,annotation), unique = true)

    def * = (datasetId.?, name, dataType, format, assembly, isAnn, annotation, annotationTid, localUrl)
  }

  val datasets = TableQuery[Datasets]

  class Items(tag: Tag) extends
    Table[(Option[Int], Int, Int, String, Option[Long], Option[String], Option[Int], Option[String], Option[String])](tag, ITEM_TABLE_NAME) {
    def itemId = column[Int]("item_id", O.PrimaryKey, O.AutoInc)

    def experimentTypeId = column[Int]("experiment_type_id")

    def datasetId = column[Int]("dataset_id")

    def sourceId = column[String]("source_id", O.Unique)

    def size = column[Option[Long]]("size", O.Default(None))

    def platform = column[Option[String]]("platform", O.Default(None))

    def platformTid = column[Option[Int]]("platform_tid", O.Default(None))

    def pipeline = column[Option[String]]("pipeline", O.Default(None))

    def sourceUrl = column[Option[String]]("source_url", O.Default(None))

    def experimentType = foreignKey("items_experimentType_fk", experimentTypeId, experimentsType)(
      _.experimentTypeId,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )

    def dataset = foreignKey("items_dataset_fk", datasetId, datasets)(
      _.datasetId,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )

    def * = (itemId.?, experimentTypeId, datasetId, sourceId, size, platform, platformTid, pipeline, sourceUrl)
  }

  val items = TableQuery[Items]

  class CasesItems(tag: Tag) extends
    Table[(Int, Int)](tag, CASEITEM_TABLE_NAME) {
    def itemId = column[Int]("item_id")

    def caseId = column[Int]("case_id")

    def pk = primaryKey("item_case_id", (itemId, caseId))

    def item = foreignKey("items_casesitem_fk", itemId, items)(
      _.itemId,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )

    def caseFK = foreignKey("cases_caseitem_fk", caseId, cases)(
      _.caseId,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )

    def * = (itemId, caseId)
  }

  val casesItems = TableQuery[CasesItems]

  class ReplicatesItems(tag: Tag) extends
    Table[(Int, Int)](tag, REPLICATEITEM_TABLE_NAME) {
    def itemId = column[Int]("item_id")

    def replicateId = column[Int]("replicate_id")

    def pk = primaryKey("item_replicate_id_replicatesitem", (itemId, replicateId))

    def item = foreignKey("items_replicateitem_fk", itemId, items)(
      _.itemId,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )

    def caseFK = foreignKey("replicates_replicateitem_fk", replicateId, replicates)(
      _.replicateId,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )

    def * = (itemId, replicateId)
  }

  val replicatesItems = TableQuery[ReplicatesItems]

  class DerivedFrom(tag: Tag) extends
    Table[(Int, Int, Option[String])](tag, DERIVEDFROM_TABLE_NAME) {

    def initialItemId = column[Int]("initial_item_id")

    def finalItemId = column[Int]("final_item_id")

    def operation = column[Option[String]]("operation", O.Default(None))

    def pk = primaryKey("item_replicate_id_derivedfrom", (initialItemId, finalItemId))

    def initialItemIdFK= foreignKey("items_initialitem_fk", initialItemId, items)(
      _.itemId,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )

    def finalItemIdFK = foreignKey("items_finalitem_fk", finalItemId, items)(
      _.itemId,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )

    def * = (initialItemId, finalItemId, operation)
  }

  val derivedFrom = TableQuery[DerivedFrom]

  class CaseTCGAMapping(tag: Tag) extends
    Table[(String, String)](tag, CASE_TCGA_MAPPING) {

    def code = column[String]("tss_code", O.PrimaryKey)

    def sourceSite = column[String]("source_site")

    def * = (code, sourceSite)
  }

  val caseTcgaMapping = TableQuery[CaseTCGAMapping]


  class OntologyTable(tag: Tag) extends
    Table[(Int, String, String, String, String, Option[String])](tag, ONTOLOGY_TABLE) {
    def tableId = column[Int]("table_id")

    def tableNames =  column[String]("table_name")

    def tableColumn =  column[String]("table_column")

      def originalKey = column[String]("original_key")

    def originalValue = column[String]("original_value")

    def ontologicalCode = column[Option[String]]("ontological_code", O.Default(None))

    def pk = ("table_id_table_name_table_column",(tableId,tableNames,tableColumn))

    def * = (tableId, tableNames, tableColumn, originalKey, originalValue, ontologicalCode)
  }

  val ontologyTable = TableQuery[OntologyTable]
}
