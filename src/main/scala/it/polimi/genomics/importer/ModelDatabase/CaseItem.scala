package it.polimi.genomics.importer.ModelDatabase

trait CaseItem extends Table{

  var itemId: Int = _

  var caseId: Int = _

  _hasForeignKeys = true

  _foreignKeysTables = List("ITEMS","CASES")

  override def insertRow(): Int ={
    var id: Int = 0
    if(this.checkInsert()) {
      id = this.insert
    }
    id
  }

  override def insert() : Int ={
    dbHandler.insertCaseItem(itemId,caseId)
  }

  override def update() : Int ={
    -1 //ritorno un valore senza senso in quanto non ci possono essere update per le tabelle di congiunzione, al massimo si inserisce una riga nuova
  }

  override def setForeignKeys(table: Table): Unit = {
    if(table.isInstanceOf[Item])
      this.itemId = table.primaryKey
    if(table.isInstanceOf[Case]) {
      this.caseId = table.primaryKey
    }
  }

  override def checkInsert(): Boolean ={
    dbHandler.checkInsertCaseItem(this.itemId,this.caseId)

  }

  override def getId(): Int = {
    // dbHandler.getCasesItemId(this.itemId,this.caseId)
    -1
  }


}
