package it.polimi.genomics.importer.ModelDatabase.Utils

import it.polimi.genomics.importer.ModelDatabase.REP.Utils.{BioSampleList, ReplicateList}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer
import scala.xml.XML


class XMLReaderREP(val path: String, val replicates: ReplicateList, val biosamples: BioSampleList, var states: collection.mutable.Map[String, String]) {
  private val xml = XML.loadFile(path)

  private val default: String = "DEFAULT"
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val settingRetriver: XMLSettingRetriver = new XMLSettingRetriver(default)


  private var list = List(((xml \\ "table" \ "mapping" \ "source_key").text), ((xml \\ "table" \ "mapping" \ "global_key").text), (xml \\ "@name"))

  private var operations = new ListBuffer[List[String]]()
  for (x <- xml \\ "table") {
    try {
      for (xi <- x \ "mapping") {
        if ((x \ "@name").toString() != "REPLICATES" && (x \ "@name").toString() != "BIOSAMPLES" && (x \ "@name").toString() != "DONORS") {
          var app = new ListBuffer[String]()
          app += ((x \ "@name").toString())
          app += ((xi \ "source_key").text).replaceAll("X", biosamples.BiosampleList.head)
          app += ((xi \ "global_key").text)
          /*if((xi \ "@method").toString()!="")
            app += ((xi \ "@method").toString())
          else
            app += default */
          app += settingRetriver.getMethod(xi)
          app += settingRetriver.getConcatCharacter(xi)
          app += settingRetriver.getSubCharacter(xi)
          app += settingRetriver.getNewCharacter(xi)
          app += settingRetriver.getRemCharacter(xi)

          operations += app.toList
        } else if ((x \ "@name").toString() == "BIOSAMPLES" || (x \ "@name").toString() == "DONORS") {


          //      }else if((x \ "@name").toString() == "BIOSAMPLES" || ((x \ "@name").toString() == "DONORS" && ((xi \ "source_key").text) != "epi__donor_id")){
          //var position: Int = 0
          //var arrayList = new Array[ListBuffer[List[String]]](biosamples.bioSampleQuantity)
          biosamples.BiosampleList.map(number => {
            var app = new ListBuffer[String]()
            app += ((x \ "@name").toString())
            app += ((xi \ "source_key").text).replaceAll("X", number)
            app += ((xi \ "global_key").text)
            app += settingRetriver.getMethod(xi)
            app += settingRetriver.getConcatCharacter(xi)
            app += settingRetriver.getSubCharacter(xi)
            app += settingRetriver.getNewCharacter(xi)
            app += settingRetriver.getRemCharacter(xi)

            //arrayList(position) += app.toList
            //position += 1
            operations += app.toList
          })
        }
        else {
          //for( position <- 0 to replicates.UuidList.length-1){
          biosamples.BiosampleList.map(number => {
            var app = new ListBuffer[String]()
            app += ((x \ "@name").toString())
            app += ((xi \ "source_key").text).replaceAll("X", number)
            app += ((xi \ "global_key").text)
            app += settingRetriver.getMethod(xi)
            app += settingRetriver.getConcatCharacter(xi)
            app += settingRetriver.getSubCharacter(xi)
            app += settingRetriver.getNewCharacter(xi)
            app += settingRetriver.getRemCharacter(xi)

            //if((xi \ "source_key").text == "epi__sample_alias__X")
            //  states += ((xi \ "source_key").text  + "__" + replicates.TechnicalReplicateNumberList(position)).replaceAll("X", replicates.BiologicalReplicateNumberList(position)) -> replicates.UuidList(position)
            //if((xi \ "source_key").text == "replicates__X__biological_replicate_number")
            //  states += ((xi \ "source_key").text  + "__" + replicates.TechnicalReplicateNumberList(position)).replaceAll("X", replicates.BiologicalReplicateNumberList(position)) -> replicates.BiologicalReplicateNumberList(position)
            //if((xi \ "source_key").text == "replicates__X__technical_replicate_number")
            //  states += ((xi \ "source_key").text  + "__" + replicates.TechnicalReplicateNumberList(position)).replaceAll("X", replicates.BiologicalReplicateNumberList(position)) -> replicates.TechnicalReplicateNumberList(position)

            operations += app.toList
          })
        }
      }
    } catch {
      case e: Exception => logger.warn(s"Source Key ${((x \ "mapping" \ "source_key").text)} not found for table ${((x \ "@name").toString())}")
    }
  }

  private val _operationsList = operations.toList

  def operationsList = _operationsList
}
