import org.apache.spark.SparkContext

object SentimentAnalysis {

  def setupLogging() = {
    import org.apache.log4j.{Level, Logger}
    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)
  }

  def main(args: Array[String]): Unit = {
    val context = new SparkContext("local[*]", "SentimentAnalysis")
    val lines = context.textFile("src/main/resources/tweets_2009_0.csv")

    // Removing the @words
    val parsedHash = lines.map(x => x.replaceAll(Utility.REG_HANDLERS, ""))

    // Removing the links
    val parsedLinks = parsedHash.map(x => x.replaceAll(Utility.REG_LINKS, ""))

    // Removing punctuations
    val parsedPunctuations = parsedLinks.map(x => x.replaceAll(Utility.REG_PUNCTUATIONS, ""))

    // Removing extra whitespaces
    val parsedWhitespaces = parsedPunctuations.map(x => x.replaceAll(Utility.REG_WHITESPACES, " "))

    // Trimming the string
    val parsedTrim = parsedWhitespaces.map(x => x.replaceAll(Utility.REG_TRIM, ""))

    // Lowercase
    val parsedLowercase = parsedTrim.map(x => x.toLowerCase)
    var count = 0
    for (result <- parsedLowercase.collect() if count < 5) {
      println(result)
      count = count + 1
    }
  }
}