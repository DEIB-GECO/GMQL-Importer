package it.polimi.genomics.importer.ENCODEImporter

import java.io.{File, _}
import java.util

import it.polimi.genomics.importer.DefaultImporter.utils.Unzipper
import it.polimi.genomics.importer.FileDatabase.{FILE_STATUS, FileDatabase, STAGE}
import it.polimi.genomics.importer.GMQLImporter.{GMQLDataset, GMQLSource, GMQLTransformer}
import org.codehaus.jackson.map.MappingJsonFactory
import org.codehaus.jackson.{JsonNode, JsonParser, JsonToken}
import org.slf4j.LoggerFactory

import scala.io.Source

import collection.JavaConverters._


/**
  * Created by Nacho on 10/13/16.
  * Object meant to be used for transform the data from ENCODE to data for GMQL,
  * files must be in the following format:
  *   - metadata file downloaded from ENCODE (1 single file for all the samples)
  *   - .gz data files downloaded from ENCODE.
  */
class ENCODETransformer extends GMQLTransformer {
  val logger = LoggerFactory.getLogger(this.getClass)

  //--------------------------------------------BASE CLASS SECTION------------------------------------------------------


  /**
    * by receiving an original filename returns the new GDM candidate name.
    *
    * @param filename original filename
    * @param dataset  dataser where the file belongs to
    * @return candidate names for the files derived from the original filename.
    */
  override def getCandidateNames(filename: String, dataset: GMQLDataset, source: GMQLSource): List[String] = {
    val sourceId = FileDatabase.sourceId(source.name)
    val datasetId = FileDatabase.datasetId(sourceId, dataset.name)
    if (filename.endsWith(".gz")) {
      //      if (source.parameters.exists(_._1 == "assembly_exclude")) {
      val path = source.outputFolder + File.separator + dataset.outputFolder + File.separator + "Downloads"
      val file = Source.fromFile(path + File.separator + "metadata" + ".tsv")
      val header = file.getLines().next().split("\t")
      //        val assembly = header.lastIndexOf("Assembly")
      val url = header.lastIndexOf("File download URL")
      //  NOW ALL OF THIS ASSEMBLY HANDLING IS DONE IN DOWNLOADING PHASE.
      //        if (Source.fromFile(path + File.separator + "metadata.tsv").getLines().exists(line => {
      //          line.split("\t")(url).contains(filename) &&
      //            line.split("\t")(assembly).toLowerCase == source.parameters.filter(_._1.toLowerCase ==
      //              "assembly_exclude").head._2.toLowerCase
      //        })
      //        )
      //          List[String]()
      //        else
      List[String](filename.substring(0, filename.lastIndexOf(".")))
      //      }
      //      else
      //        List[String](filename.substring(0, filename.lastIndexOf(".")))
    }
    else {
      val both = source.parameters.exists(_._1 == "metadata_extraction") &&
        source.parameters.filter(_._1 == "metadata_extraction").head._2.contains("json") &&
        source.parameters.filter(_._1 == "metadata_extraction").head._2.contains("tsv")

      val res1 = if (source.parameters.exists(_._1 == "metadata_extraction") &&
        source.parameters.filter(_._1 == "metadata_extraction").head._2.contains("json")) {
        if (filename.endsWith(".gz.json")) {
          val bedFilePath = source.outputFolder + File.separator + dataset.outputFolder + File.separator + "Downloads" +
            File.separator + filename.replace(".json", "")
          //          if (new File(bedFilePath).exists())
          if (both)
            List[String](filename.replace(".gz.json", ".meta.json"))
          else
            List[String](filename.replace(".gz.json", ".meta"))
          //          else List[String]()
        }
        else List[String]()
      }
      else List[String]()
      val res2 = if (source.parameters.exists(_._1 == "metadata_extraction") &&
        source.parameters.filter(_._1 == "metadata_extraction").head._2.contains("tsv") &&
        source.parameters.filter(_._1 == "metadata_suffix").head._2.contains(filename)) {
        import scala.io.Source
        val metadataPath = source.outputFolder + File.separator +
          dataset.outputFolder + File.separator +
          "Downloads" + File.separator + filename
        val header = Source.fromFile(metadataPath).getLines().next().split("\t")

        //this "File download URL" maybe should be in the parameters of the XML.
        val url = header.lastIndexOf("File download URL")

        //here I have to check if the .gz file is UPDATED or OUTDATED.
        Source.fromFile(metadataPath, "UTF-8").getLines().drop(1).filter(line => {
          val fields = line.split("\t")
          val bedFileStatus = FileDatabase.fileStatus(datasetId, fields(url), STAGE.DOWNLOAD).getOrElse(FILE_STATUS.OUTDATED)
          if (bedFileStatus == FILE_STATUS.UPDATED) {
            true
          }
          else {
            false
          }
        }).map(line => {
          //create file .meta
          val fields = line.split("\t")
          val aux1 = fields(url).split("/").last
          val aux2 = aux1.substring(0, aux1.lastIndexOf(".")) + ".meta" //this is the meta name
          aux2
        }).filter(file => {
          val bedFilePath = source.outputFolder + File.separator + dataset.outputFolder + File.separator + "Downloads" +
            File.separator + file.replace(".meta", ".gz")
          if (new File(bedFilePath).exists())
            true
          else
            false
        }).toList
      }
      else List[String]()
      res1 ++ res2
    }
  }

  /**
    * recieves .json and .bed.gz files and transform them to get metadata in .meta files and region in .bed files.
    *
    * @param source           source where the files belong to.
    * @param originPath       path for the  "Downloads" folder
    * @param destinationPath  path for the "Transformations" folder
    * @param originalFilename name of the original file .json/.gz
    * @param filename         name of the new file .meta/.bed
    * @return List(fileId, filename) for the transformed files.
    */
  override def transform(source: GMQLSource, originPath: String, destinationPath: String, originalFilename: String,
                         filename: String): Boolean = {
    fillMetadataExclusion(source)
    val fileDownloadPath = originPath + File.separator + originalFilename
    val fileTransformationPath = destinationPath + File.separator + filename
    if (originalFilename.endsWith(".gz")) {
      logger.debug("Start unGzipping: " + originalFilename)
      if (Unzipper.unGzipIt(
        fileDownloadPath,
        fileTransformationPath)) {
        logger.info("UnGzipping: " + originalFilename + " DONE")

        //to copy original json to transform
        //        if(source.parameters.exists(_._1 == "metadata_extraction") &&
        //          source.parameters.filter(_._1 == "metadata_extraction").head._2 == "tsv_json"){
        //          import java.io.{File, FileInputStream, FileOutputStream}
        //          val src = new File(fileDownloadPath + ".json")
        //          val dest = new File(fileTransformationPath + ".json")
        //          new FileOutputStream(dest) getChannel() transferFrom(
        //            new FileInputStream(src) getChannel(), 0, Long.MaxValue)
        //          //here have to add the metadata of copy number and total copies
        //          logger.info("File: " + fileDownloadPath + " copied into " + fileTransformationPath)
        //        }

        true
      }
      else {
        logger.warn("UnGzipping: " + originalFilename + " FAIL")
        false
      }
    }
    else {
      val result1 =
        if (source.parameters.exists(_._1 == "metadata_extraction") &&
          source.parameters.filter(_._1 == "metadata_extraction").head._2.contains("json")) {
          if (originalFilename.endsWith(".gz.json")) {
            logger.debug("Start metadata transformation: " + originalFilename)
            val jsonFileName = filename.split('.').head

            val separator =
              if (source.parameters.exists(_._1 == "metadata_name_separation_char"))
                source.parameters.filter(_._1 == "metadata_name_separation_char").head._2
              else
                "__"
            if (transformMetaFromJson(fileDownloadPath, fileTransformationPath, jsonFileName, separator)) {
              logger.info("Metadata transformation: " + originalFilename + " DONE")
              true
            }
            else false
          }
          else {
            false
            //this is no data nor metadata file, must be the metadata.tsv and is not used due to json selection.
          }
        } else
          false
      //if not json is defined, metadata.tsv will be used.
      val result2 =
        if (source.parameters.exists(_._1 == "metadata_extraction") &&
          source.parameters.filter(_._1 == "metadata_extraction").head._2.contains("tsv")) {
          if (source.parameters.filter(_._1 == "metadata_suffix").head._2.contains(originalFilename)) {
            val accession = filename.split('.').head
            transformMetaFromTsv(fileDownloadPath, destinationPath, accession, source)
            true
          }
          else {
            false
            //is not metadata file
          }
        }
        else
          false
      result1 || result2
    }
  }

  /**
    * by giving a metadata.tsv file creates all the metadata for the files.
    *
    * @param originPath        full path to metadata.tsv file
    * @param destinationFolder transformations folder of the dataset.
    * @param accession         experiment accession number for the file.
    * @param gmqlSource        source being transformed
    */
  def transformMetaFromTsv(originPath: String, destinationFolder: String, accession: String, gmqlSource: GMQLSource): Unit = {
    import scala.io.Source
    logger.info(s"Splitting ENCODE metadata for $accession")
    val header = Source.fromFile(originPath).getLines().next().split("\t")

    //this "File download URL" maybe should be in the parameters of the XML.
    val url = header.lastIndexOf("File download URL")
    Source.fromFile(originPath).getLines()
      .filter(line => line.split("\t")(url).contains(accession)).foreach(line => {
      //create file .meta
      val fields = line.split("\t")
      val aux1 = fields(url).split("/").last
      val aux2 = aux1.substring(0, aux1.lastIndexOf(".")) + ".meta" //this is the meta name
      val file = new File(destinationFolder + File.separator + aux2)

      val writer = new PrintWriter(file)
      for (i <- 0 until fields.size) {
        if (fields(i).nonEmpty) {
          if (gmqlSource.parameters.exists(_._1 == "multiple_comma_separated") &&
            gmqlSource.parameters.filter(_._1 == "multiple_comma_separated").exists(_._2 == header(i)))
            for (value <- fields(i).split(", "))
              writer.write(header(i) + "\t" + value + "\n")
          else
            writer.write(header(i) + "\t" + fields(i) + "\n")
        }
      }
      writer.close()
    })
  }

  //----------------------------------------METADATA FROM JSON SECTION--------------------------------------------------
  /**
    * by giving a .json file, it generates a .meta file with the json structure.
    * does an exception for the section "files" and needs the file id to achieve this.
    *
    * @param metadataJsonFileName     origin json file
    * @param metadataFileName         destination .meta file
    * @param fileNameWithoutExtension id of the file being converted.
    * @param separator                separator string used to mark nested metadata
    */
  def transformMetaFromJson(metadataJsonFileName: String, metadataFileName: String, fileNameWithoutExtension: String
                            , separator: String): Boolean = {
    val jsonFile = new File(metadataJsonFileName)
    if (jsonFile.exists()) {
      val f = new MappingJsonFactory()
      val jp: JsonParser = f.createJsonParser(jsonFile)

      val current: JsonToken = jp.nextToken()
      if (current != JsonToken.START_OBJECT) {
        logger.error("json root should be object: quiting File: " + metadataJsonFileName)
        false
      }
      else {
        val file = new File(metadataFileName)
        try {
          val writer = new PrintWriter(file)
          //this is the one that could throw an exception
          val node: JsonNode = jp.readValueAsTree()

          val metadataList = new java.util.ArrayList[(String, String)]()
          //here I handle the exceptions as "files" and "replicates"
          val replicateIds = getReplicatesAndWriteFile(node, writer, fileNameWithoutExtension, metadataList, separator)
          writeReplicates(node, writer, replicateIds, metadataList, separator)
          //here is the regular case

          writePlatform(node, writer, replicateIds, metadataList, separator)


          printTree(node, "", writer, metadataList, separator, exclusion = true)
          writer.close()
          true
        }
        catch {
          case e: IOException =>
            logger.error("couldn't read the json tree: " + e.getMessage)
            false
        }
      }
    }
    else {
      logger.warn("Json file not found: " + metadataJsonFileName)
      false
    }
  }

  /**
    * by getting the exclusion list of encode metadata, generates (if not generated before) the exclusionRegex list.
    *
    * @param source source with the parameters of exclusion for encode.
    */
  def fillMetadataExclusion(source: GMQLSource): Unit = {
    if (exclusionRegex.isEmpty) {
      //fills the exclusion regexes into the list.
      source.parameters.filter(parameter => parameter._1.equalsIgnoreCase("encode_metadata_exclusion"))
        .foreach(param => {
          exclusionRegex.add(param._2)
        })
    }
  }

  /**
    * handles the particular case of files, writes its metadata and returns a list with the replicates IDs used.
    *
    * @param rootNode                 initial node of the json file.
    * @param writer                   output for metadata.
    * @param fileNameWithoutExtension id of the file that metadata is being extracted without the .meta.
    * @param metaList                 list with already inserted meta to avoid duplication.
    * @param separator                separator string to use for separating the nested metadata
    * @return list with the replicates referred by the file.
    */
  def getReplicatesAndWriteFile(rootNode: JsonNode, writer: PrintWriter, fileNameWithoutExtension: String,
                                metaList: java.util.ArrayList[(String, String)], separator: String): List[String] = {
    //particular cases first one is to find just the correct file to use its metadata.
    var replicates = List[String]()
    if (rootNode.has("files")) {
      val files = rootNode.get("files").getElements
      while (files.hasNext) {
        val file = files.next()
        if (file.has("@id") && file.get("@id").asText().contains(fileNameWithoutExtension)) {
          if (file.has("biological_replicates")) {
            val biologicalReplicates = file.get("biological_replicates")
            if (biologicalReplicates.isArray) {
              val values = biologicalReplicates.getElements
              while (values.hasNext) {
                val replicate = values.next()
                if (replicate.asText() != "") {
                  replicates = replicates :+ replicate.asText()
                }
              }
            }
          }
          //here is where the file is wrote
          printTree(file, "file", writer, metaList, separator, exclusion = false)
        }
      }
    }
    replicates
  }

  /**
    * handles the particular case of biological replicates, writes their metadata from a list of replicates
    *
    * @param rootNode     initial node of the json file.
    * @param writer       output for metadata.
    * @param replicateIds list with the biological_replicate_number used by the file.
    * @param metaList     list with already inserted meta to avoid duplication.
    * @param separator    separator string to use for separating the nested metadata
    */
  def writeReplicates(rootNode: JsonNode, writer: PrintWriter, replicateIds: List[String],
                      metaList: java.util.ArrayList[(String, String)], separator: String): Unit = {
    if (rootNode.has("replicates")) {
      val replicatesNode = rootNode.get("replicates")
      if (replicatesNode.isArray) {
        val replicates = replicatesNode.getElements
        while (replicates.hasNext) {
          val replicate = replicates.next()
          if (replicate.has("biological_replicate_number") &&
            replicateIds.contains(replicate.get("biological_replicate_number").asText()))
            printTree(replicate, s"replicates$separator${replicate.get("biological_replicate_number").asText()}", writer, metaList, separator, exclusion = false)
        }
      }
    }
  }

  /**
    * handles the particular case of biological replicates, writes their metadata from a list of replicates
    *
    * @param rootNode     initial node of the json file.
    * @param writer       output for metadata.
    * @param replicateIds list with the biological_replicate_number used by the file.
    * @param metaList     list with already inserted meta to avoid duplication.
    * @param separator    separator string to use for separating the nested metadata
    */
  def writePlatform(rootNode: JsonNode, writer: PrintWriter, replicateIds: List[String],
                      metaList: java.util.ArrayList[(String, String)], separator: String): Unit = {
    if (rootNode.has("files")) {
      val filesNode = rootNode.get("files")
      if (filesNode.isArray) {
        val files = filesNode.getElements
        while (files.hasNext) {
          val file = files.next()
          if(file.has("platform") && file.has("biological_replicates")){
            val fileReplicates = file.get("biological_replicates")
            if(fileReplicates.isArray){
              val fileReplicateAsText = fileReplicates.getElements().asScala.map(_.asText()).toSet
              val hasIntersection = fileReplicateAsText.intersect(replicateIds.toSet).nonEmpty
              if(hasIntersection)
                printTree(file.get("platform"), s"platform", writer, metaList, separator, exclusion = false)
            }
          }
        }
      }
    }
  }

  /**
    * gets the "hard coded" exclusion categories, meant to be used for the particular cases
    * Files and Replicates should be always be there, other exclusions are managed from xml file with regex.
    */
  val exclusionCategories: java.util.ArrayList[String] = {
    val list = new java.util.ArrayList[String]()
    list.add("files")
    list.add("replicates")
    list
  }
  /**
    * loads from the xml "encodeMetadataConfig" the regex set to be excluded fromt he metadata.
    */
  var exclusionRegex: java.util.ArrayList[String] = new util.ArrayList[String]()


  /**
    * by giving an initial node, prints into the .meta file its metadata and its children's metadata also.
    * I use java arraylist as scala list cannot be put as var in the parameters.
    *
    * @param node      current node
    * @param parents   path separated by dots for each level
    * @param writer    file writer with the open .meta file
    * @param metaList  list with already inserted meta to avoid duplication.
    * @param separator separator string to use for separating the nested metadata
    * @param exclusion list with all metadata names to be excluded
    */
  def printTree(node: JsonNode, parents: String, writer: PrintWriter, metaList: java.util.ArrayList[(String, String)], separator: String, exclusion: Boolean): Unit = {
    //base case, the node is value
    if (node.isValueNode && node.asText() != ""
      && !metaList.contains(node.asText(), parents)
    ) {
      writer.write(parents + "\t" + node.asText() + "\n")
      metaList.add((node.asText(), parents))
    }
    else {
      val fields: util.Iterator[String] = node.getFieldNames
      while (fields.hasNext) {
        val name = fields.next()
        if (!exclusionCategories.contains(name) || !exclusion) {
          val element = node.get(name)
          //base case when parents are empty
          val currentName = if (parents == "") name else parents + separator + name
          //check the regex
          var regexMatch = false
          for (i <- 0 until exclusionRegex.size())
            if (currentName.matches(exclusionRegex.get(i)))
              regexMatch = true
          if (!regexMatch) {
            if (element.isArray) {
              val subElements = element.getElements
              while (subElements.hasNext)
                printTree(subElements.next(), currentName, writer, metaList, separator, exclusion)
            }
            else
              printTree(element, currentName, writer, metaList, separator, exclusion)
          }
        }
      }
    }
  }

  //  //--------------------------------------------SCHEMA SECTION----------------------------------------------------------
  //  /**
  //    * using information in the loader should arrange the files into a single folder
  //    * where data and metadata are paired as (file,file.meta) and should put also
  //    * the schema file inside the folder.
  //    * ENCODE schema file is not provided in the same folder as the data
  //    * for the moment the schemas have to be given locally.
  //    *
  //    * @param source contains specific download and sorting info.
  //    */
  //  def organize(source: GMQLSource): Unit = {
  //    source.datasets.foreach(dataset => {
  //      if(dataset.transformEnabled) {
  //        if (dataset.schemaLocation == SCHEMA_LOCATION.LOCAL) {
  //          val src = new File(dataset.schemaUrl)
  //          val dest = new File(source.outputFolder + File.separator + dataset.outputFolder + File.separator +
  //            "Transformations" + File.separator + dataset.name + ".schema")
  //
  //          try {
  //            Files.copy(src, dest)
  //            logger.info("Schema copied from: " + src.getAbsolutePath + " to " + dest.getAbsolutePath)
  //          }
  //          catch {
  //            case e: IOException => logger.error("could not copy the file " +
  //              src.getAbsolutePath + " to " + dest.getAbsolutePath)
  //          }
  //        }
  //      }
  //    })
  //  }

}
