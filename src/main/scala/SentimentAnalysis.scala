import java.io.File

import org.apache.hadoop.fs.FileUtil
import org.apache.spark.SparkContext

import scala.util.matching.Regex

object SentimentAnalysis {

  def setupLogging() = {
    import org.apache.log4j.{Level, Logger}
    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)
  }

  def main(args: Array[String]): Unit = {
    val context = new SparkContext("local[*]", "SentimentAnalysis")
    //val context = SparkContext.getOrCreate()
    val lines = context.textFile("src/main/resources/got_new.csv")
    val stopWords = context.textFile("src/main/resources/stopwords.txt")
    val outputFile = "src/main/resources/output.csv"
    FileUtil.fullyDelete(new File(outputFile))

    val pattern = new Regex(Utility.REG_CAMELCASE)

    // Removing the @words
    val parsedHash = lines.map(x => x.replaceAll(Utility.REG_HANDLERS, ""))

    // Removing the links
    val parsedLinks = parsedHash.map(x => x.replaceAll(Utility.REG_LINKS, ""))

    // Removing punctuations
    val parsedPunctuations = parsedLinks.map(x => x.replaceAll(Utility.REG_PUNCTUATIONS, ""))

    // Splitting camelCase
    val parsedCamelCase = parsedPunctuations.map(x => (pattern findAllIn x).mkString(" "))

    // Lowercase
    val parsedLowercase = parsedCamelCase.map(x => x.toLowerCase)

    // Expanding contractions
    val parsedContractions = parsedLowercase.map(x => x.split(" ")
      .map(y => if (Utility.contractions.contains(y)) Utility.contractions(y) else y).mkString(" "))

    // Removing stopWords
    val broadcastStopWords = context.broadcast(stopWords.collect.toSet)
    val parsedStopWords = parsedContractions.map(x => x.split(" ")
      .map(y => if (!broadcastStopWords.value.contains(y)) y else "").mkString(" "))

    // Removing extra whitespaces
    val parsedWhitespaces = parsedStopWords.map(x => x.replaceAll(Utility.REG_WHITESPACES, " "))

    // Trimming the string
    val parsedTrim = parsedWhitespaces.map(x => x.trim())

    // Removing empty tweets
    val parsedEmpty = parsedTrim.filter(x => x.length > 0)

    parsedEmpty.saveAsTextFile(outputFile)
  }
}