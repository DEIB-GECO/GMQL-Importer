package it.polimi.genomics.metadata.downloader_transformer.default.utils

import java.io.{File, FileInputStream, IOException, InputStreamReader}
import java.util

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.{Sheets, SheetsScopes}
import org.slf4j.{Logger, LoggerFactory}


class OAuth(dataStoreDir: String) {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /** Application name. */
  private val APPLICATION_NAME: String = "GMQL-importer"

  /** Directory to store user credentials for this application. */
  private val DATA_STORE_DIR: File = new File(dataStoreDir)//new File(/*System.getProperty("user.home")*/ new File(".").getCanonicalPath, ".credentials/sheets.googleapis.com-GMQL-importer")

  /** Global instance of the FileDataStoreFactory. */
  private val DATA_STORE_FACTORY: FileDataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR)

  /** Global instance of the JSON factory. */
  private val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance

  /** Global instance of the HTTP transport. */
  private val HTTP_TRANSPORT: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()

  /** Global instance of the scopes. */
  private val SCOPES: util.List[String] = util.Arrays.asList(SheetsScopes.SPREADSHEETS_READONLY)

  /**
    * Creates an authorized Credential object.
    *
    * @return an authorized Credential object.
    */
  @throws[IOException]
  def authorize(secretPath: String): Credential = { // Load client secrets.
    val in = new FileInputStream(secretPath)
    val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in))
    // Build flow and trigger user authorization request.
    val flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline").build
    val credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver).authorize("user")
    logger.info("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath)
    credential
  }

  /**
    * Build and return an authorized Sheets API client service.
    *
    * @return an authorized Sheets API client service
    */
  @throws[IOException]
  def getSheetsService(secretPath: String): Sheets = {
    val credential: Credential = authorize(secretPath)
    new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build
  }
}
