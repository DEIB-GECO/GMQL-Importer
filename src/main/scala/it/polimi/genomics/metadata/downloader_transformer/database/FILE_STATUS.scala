package it.polimi.genomics.metadata.downloader_transformer.database

/**
  * Created by Nacho
  * Represents the current status for a file in the local copies
  */
object FILE_STATUS extends Enumeration {
  type FILE_STATUS = Value
  val UPDATED = Value("UPDATED") //FILE HAS TO BE ADDED/UPDATED (EITHER TRANSFORMED OR LOADED)
  val OUTDATED = Value("OUTDATED") //LOCAL FILE EXISTS BUT IN ORIGIN DOES NOT EXIST ANYMORE HAS TO BE DELETED
  val COMPARE = Value("COMPARE") //TEMPORARY STATUS TO CHECK IF FILES HAVE BEEN REMOVED FROM THE SERVER.
  val FAILED = Value("FAILED") //AUXILIARY STATUS TO MARK THAT WHATEVER DONE AFTER CHECK IF UPDATE IS NOT DONE CORRECTLY
}
