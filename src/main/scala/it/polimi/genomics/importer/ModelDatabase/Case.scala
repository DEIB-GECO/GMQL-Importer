package it.polimi.genomics.importer.ModelDatabase

class Case(ciao:Prova) extends EncodeTable{

    var projectId : Int = _

    var sourceId : String = _

    var sourceSite : String = _

    var externalRef: String = _

  _hasForeignKeys = true

  _foreignKeysTables = List("PROJECTS")

  this.prova = ciao


  override def setParameter(param: String, dest: String, insertMethod: (String,String) => String): Unit =   dest.toUpperCase() match{
    case "SOURCEID" => this.sourceId = insertMethod(this.sourceId,param)
    case "SOURCESITE" => this.sourceSite = insertMethod(this.sourceSite,param)
    case "EXTERNALREF" => this.externalRef =  insertMethod(this.externalRef,param)
    case _ => noMatching(dest)
  }


  override def insert() = {
    val id = dbHandler.insertCase(this.projectId,this.sourceId,this.sourceSite,this.externalRef)
    EncodesTableId.caseId_(id)
    id
  }

  override def update() = {
    val id = dbHandler.updateCase(this.projectId,this.sourceId,this.sourceSite,this.externalRef)
    EncodesTableId.caseId_(id)
    id
  }

  override def setForeignKeys(table: Table): Unit = {
    this.projectId = table.primaryKey
  }

  override def checkInsert(): Boolean ={
    dbHandler.checkInsertCase(this.sourceId)
  }

  override def getId(): Int = {
    dbHandler.getCaseId(this.sourceId)
  }

  override def checkConsistency(): Boolean = {
    if(this.sourceId != null) true else false
  }
}
