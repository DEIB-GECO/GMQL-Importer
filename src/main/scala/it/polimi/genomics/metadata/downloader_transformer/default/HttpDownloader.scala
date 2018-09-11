package it.polimi.genomics.metadata.downloader_transformer.default
import java.io.File
import java.net.URL

import it.polimi.genomics.metadata.downloader_transformer.Downloader
import it.polimi.genomics.metadata.database.{FileDatabase, Stage}
import it.polimi.genomics.metadata.step.xml.Source
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory

import scala.language.postfixOps
import scala.sys.process._

/**
  * Created by Nacho on 19/09/2016.
  * Handles HTTP connection for downloading files
  */
//RESERVED TO DOWNLOAD ROADMAP DATASETS
class HttpDownloader extends Downloader {
  val logger = LoggerFactory.getLogger(this.getClass)
  /**
    * checks if the given URL exists
    *
    * @param path URL to check
    * @return URL exists
    */
  def urlExists(path: String): Boolean = {
    try {
      scala.io.Source.fromURL(path)
      true
    } catch {
      case _: Throwable => false
    }
  }
  /**
    * given a url and destination path, downloads that file into the path
    *
    * @param url  source file url.
    * @param path destination file path and name.
    */
  def downloadFileFromURL(url: String, path: String): Unit = {
    try {
      new URL(url) #> new File(path) !!;
      logger.info("Downloading: " + path + " from: " + url + " DONE")
    }
    catch{
      case e: Throwable => logger.error("Downloading: " + path + " from: " + url + " failed: ")
    }
  }
  /**
    * downloads the files from the source defined in the information
    * into the folder defined in the source and its dataset
    *
    * @param source configuration for the downloader, folders for input and output by regex and also for files.
    */
  override def download(source: Source, parallelExecution: Boolean): Unit = {
    if (urlExists(source.url)) {
      //same as FTP the mark to compare is done here because the iteration on http is based on http folders and not
      //on the source datasets.
      val sourceId = FileDatabase.sourceId(source.name)
      source.datasets.foreach(dataset => {
        if(dataset.downloadEnabled) {
          val datasetId = FileDatabase.datasetId(sourceId,dataset.name)
          val outputPath = source.outputFolder + File.separator + dataset.outputFolder + File.separator + "Downloads"
          if (!new File(outputPath).exists())
            new File(outputPath).mkdirs()
          FileDatabase.markToCompare(datasetId,Stage.DOWNLOAD)
        }
      })

      recursiveDownload(source.url,source)

      source.datasets.foreach(dataset => {
        if(dataset.downloadEnabled) {
          val datasetId = FileDatabase.datasetId(sourceId,dataset.name)
          FileDatabase.markAsOutdated(datasetId,Stage.DOWNLOAD)
        }
      })
    }
  }
  /**
    * recursively checks all folders and subfolders matching with the regular expressions defined in the source
    *
    * @param path       current path of the http connection
    * @param source     configuration for the downloader, folders for input and output by regex and also for files.
    */
  private def recursiveDownload(path: String, source: Source): Unit = {

    if (urlExists(path)) {
      val urlSource = scala.io.Source.fromURL(path)
      val result = urlSource.mkString
      val document: Document = Jsoup.parse(result)

      checkFolderForDownloads(path, document, source)
      downloadSubFolders(path, document, source)
    }
  }

  /**
    * given a folder, searches all the possible links to download and downloads if signaled by Updater and loader
    *
    * @param path     current directory
    * @param document current location  Jsoup document
    * @param source   contains download information
    */
  def checkFolderForDownloads(path: String, document: Document, source: Source): Unit = {
    val sourceId = FileDatabase.sourceId(source.name)
    for (dataset <- source.datasets) {
      if(dataset.downloadEnabled) {
        val datasetId = FileDatabase.datasetId(sourceId,dataset.name)
        //If the container is table, I got the rows, if not I look for anchor tags to navigate the site
        val elements = if (source.parameters.filter(_._1.toLowerCase == "table").head._2.equalsIgnoreCase("true"))
          document.select("tr")
        else
          document.select("a")

        if (path.matches(dataset.parameters.filter(_._1.toLowerCase == "folder_regex").head._2)) {
          logger.info("Searching into: " + path)
          val outputPath = source.outputFolder + File.separator + dataset.outputFolder + File.separator + "Downloads"

          if (!new java.io.File(outputPath).exists) {
            new java.io.File(outputPath).mkdirs()
          }

          for (i <- 0 until elements.size()) {
            //candidate name is the same as origin name.
            var candidateName: String =
              if (source.parameters.filter(_._1.toLowerCase == "table").head._2.toLowerCase == "true")
                try {
                  elements.get(i).text.trim.split(" ").filterNot(_.isEmpty)(
                    source.parameters.filter(_._1.toLowerCase == "name_index").head._2.toInt)
                } catch {
                  case _: Throwable => ""
                }
              else
                elements.get(i).attr("href")

            if (candidateName.startsWith("/"))
              candidateName = candidateName.substring(1)
            //if the file matches with a regex to download
            if (candidateName.matches(dataset.parameters.filter(_._1.toLowerCase == "files_regex").head._2)) {
              //from the anchor tag, i need to get nextSibling's text, from table I join all the tds on the row
              val dateAndSize =
                if (source.parameters.filter(_._1.toLowerCase == "table").head._2.toLowerCase == "true")
                  elements.get(i).text.trim.split(" ").filterNot(_.isEmpty)
                else
                  elements.get(i).nextSibling().toString.trim.split(" ").filterNot(_.isEmpty)

              val date = dateAndSize(source.parameters.filter(_._1.toLowerCase == "date_index").head._2.toInt) +
                File.separator + dateAndSize(source.parameters.filter(_._1.toLowerCase == "hour_index").head._2.toInt)
              val size = dateAndSize(source.parameters.filter(_._1.toLowerCase == "size_index").head._2.toInt)

              val fileId = FileDatabase.fileId(datasetId,path+candidateName,Stage.DOWNLOAD,candidateName)
              val nameAndCopyNumber: (String, Int) = FileDatabase.getFileNameAndCopyNumber(fileId)
              val name =
                if(nameAndCopyNumber._2==1)nameAndCopyNumber._1
                else nameAndCopyNumber._1.replaceFirst("\\.","_"+nameAndCopyNumber._2+".")

              val checkDownload = FileDatabase.checkIfUpdateFile(fileId,"no hash here",size,date)
              if (checkDownload) {
                logger.info(s"Starting download for ${path + candidateName}")
                downloadFileFromURL(path + candidateName,outputPath + File.separator + name)
                FileDatabase.markAsUpdated(fileId,new File(outputPath + File.separator + name).length.toString)
              }
            }
          }
        }
      }
    }
  }

  /**
    * Finds all subfolders in the working directory and performs checkFolderForDownloads on it
    *
    * @param path     working directory
    * @param document current location Jsoup document
    * @param source   contains download information
    */
  def downloadSubFolders(path: String, document: Document, source: Source): Unit = {
    //directories is to avoid taking backward folders
    val folders = path.split(File.separator)
    var directories = List[String]()
    for (i <- folders) {
      if (directories.nonEmpty)
        directories = directories :+ directories.last + File.separator + i
      else
        directories = directories :+ i
    }
    val elements =
      if(source.parameters.filter(_._1.toLowerCase == "table").head._2.toLowerCase=="true")
        document.select("tr")
      else
        document.select("a")

    for (i <- 0 until elements.size()) {

      var url =
        if (source.parameters.filter(_._1.toLowerCase == "table").head._2.toLowerCase=="true")
          try {
            elements.get(i).text.trim.split(" ").filterNot(_.isEmpty)(source.parameters.filter(_._1.toLowerCase == "name_index").head._2.toInt)
          } catch {
            case _: Throwable => ""
          }
        else
          elements.get(i).attr("href")

      if (url.endsWith(File.separator) && !url.contains(".."+File.separator) && !directories.contains(url)) {
        if (url.startsWith(File.separator))
          url = url.substring(1)
        recursiveDownload(path + url, source)
      }
    }
  }

  /**
    * downloads the failed files from the source defined in the loader
    * into the folder defined in the loader
    *
    * For each dataset, download method should put the downloaded files inside
    * /source.outputFolder/dataset.outputFolder/Downloads
    *
    * @param source contains specific download and sorting info.
    */
  override def downloadFailedFiles(source: Source, parallelExecution: Boolean): Unit = {

  }
}
