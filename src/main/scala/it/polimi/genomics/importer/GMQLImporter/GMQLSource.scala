package it.polimi.genomics.importer.GMQLImporter

import ExecutionLevel._


/**
  * Created by Nacho on 10/13/16.
  * Information is a container for the info stored in the xml config
  * file and every specific implementation stores needed information
  * loads specific information from the "source" node defined
  * in the xml file into the loader.
  * for the corresponding GMQLDownloader and Sorter.
  *
  * @param url source url
  * @param outputFolder working directory for the source
  * @param downloader package and name of the downloader to be used for this source
  * @param transformer package and name of the transformer to be used for this source
  * @param downloadEnabled indicates whether download or not the datasets.
  * @param transformEnabled indicates whether transform or not the datasets.
  * @param loadEnabled indicates whether load or not the datasets.
  * @param datasets datasets to be downloaded/transformed/loaded
  */
case class GMQLSource(
                        name:String,
                        url:String,
                        outputFolder:String,
                        rootOutputFolder:String,
                        downloadEnabled: Boolean,
                        downloader:String,
                        transformEnabled: Boolean,
                        transformer:String,
                        loadEnabled: Boolean,
                        loader:String,
                        parameters: Seq[(String,String,String,String)],
                        datasets:Seq[GMQLDataset],
                        cleanerEnabled: Boolean,
                        mapperEnabled: Boolean,
                        enricherEnabled: Boolean,
                        flattenerEnabled: Boolean
                     ) {

  def isEnabled(key:ExecutionLevel): Boolean = key match {
    case Download => downloadEnabled
    case Transform => transformEnabled
    case Load => loadEnabled
    case Clean => cleanerEnabled
    case Map => mapperEnabled
    case Enrich => enricherEnabled
    case Flatten => flattenerEnabled
    case _ => false
  }



}
