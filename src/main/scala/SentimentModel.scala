import org.apache.spark.ml.{Pipeline, PipelineStage}
import org.apache.spark.ml.classification.{LinearSVC, LogisticRegression, NaiveBayes, RandomForestClassifier}
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{HashingTF, IDF, StandardScaler, Tokenizer}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object SentimentModel {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder.
      master("local[*]")
      .appName("SentimentAnalysis")
      .getOrCreate()

    var df = spark.read.format("csv").option("header", "true").
      load("src/main/resources/data2.csv")

    val tokenizer = new Tokenizer().setInputCol("tweet").setOutputCol("words")
    val hashingTF = new HashingTF().setNumFeatures(1000).setInputCol("words").setOutputCol("rawFeatures")
    val idf = new IDF().setInputCol("rawFeatures").setOutputCol("features").setMinDocFreq(2)
    val scaler = new StandardScaler()
      .setInputCol("features")
      .setOutputCol("scaledFeatures")
      .setWithStd(true)
      .setWithMean(false)

    for (label <- Utility.LABELS) {
      df = df.withColumn(label, when(df(label) =!= 0, 1).when(df(label) === 0, 0))
    }

    val Array(trainingData, testData) = df.randomSplit(Array(0.7, 0.3))
    var stages: Array[PipelineStage] = Array(tokenizer, hashingTF, idf, scaler)

    for (label <- Utility.LABELS) {
      val clf = new LinearSVC()
        .setMaxIter(10)
        .setRegParam(0.3)
        .setLabelCol(label)
        .setFeaturesCol("scaledFeatures")
        .setRawPredictionCol(label.concat("_raw_pred"))
        .setPredictionCol(label.concat("_pred"))

      stages = stages :+ clf
    }

    val pipeline = new Pipeline().setStages(stages)
    val model = pipeline.fit(trainingData)
    val predictions = model.transform(testData)

    var predicted_results: Array[String] = Array()
    for (label <- Utility.LABELS) {
      val evaluator = new MulticlassClassificationEvaluator()
        .setLabelCol(label)
        .setPredictionCol(label.concat("_pred"))
        .setMetricName("accuracy")
      val accuracy = evaluator.evaluate(predictions) * 100
      predicted_results = predicted_results :+ label.concat(" accuracy " + accuracy)
    }
    predicted_results.foreach(println)
  }
}

/*
anger accuracy 79.74066458844598
anticipation accuracy 76.91351064535385
disgust accuracy 83.95901041321217
fear accuracy 77.99794388804139
joy accuracy 78.82536313590236
sadness accuracy 80.48683425084566
surprise accuracy 83.90594945944153
trust accuracy 74.25880480201631
negative accuracy 72.91238310008622
positive accuracy 71.49631889633216
 */