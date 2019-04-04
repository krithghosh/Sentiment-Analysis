import org.apache.spark.ml.{Pipeline, PipelineStage}
import org.apache.spark.ml.classification._
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{HashingTF, IDF, StandardScaler, Tokenizer}
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
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

    val Array(trainingData, testData) = df.randomSplit(Array(0.75, 0.25))
    val stages: Array[PipelineStage] = Array(tokenizer, hashingTF, idf, scaler)

    var predicted_results: Array[String] = Array()
    for (label <- Utility.LABELS) {
      val clf = new LogisticRegression()
        .setLabelCol(label)
        .setFeaturesCol("scaledFeatures")
        .setProbabilityCol(label.concat("_prob"))
        .setRawPredictionCol(label.concat("_raw_pred"))
        .setPredictionCol(label.concat("_pred"))

      val paramMaps = new ParamGridBuilder()
        .addGrid(hashingTF.numFeatures, Array(10, 100, 1000))
        .addGrid(clf.regParam, Array(0.3, 0.1, 0.01))
        .addGrid(clf.maxIter, Array(10, 20, 50))
        .addGrid(clf.elasticNetParam, Array(0.0, 0.1, 0.2))
        .build()

      val updatedStages = stages :+ clf
      val pipeline = new Pipeline().setStages(updatedStages)

      val evaluator = new MulticlassClassificationEvaluator()
        .setLabelCol(label)
        .setPredictionCol(label.concat("_pred"))
        .setMetricName("accuracy")

      val cv = new CrossValidator()
        .setEstimator(pipeline)
        .setEstimatorParamMaps(paramMaps)
        .setEvaluator(evaluator)
        .setNumFolds(5)
        .setParallelism(2)

      val model = cv.fit(trainingData)
      val predictions = model.transform(testData)

      val accuracy = evaluator.evaluate(predictions) * 100
      predicted_results = predicted_results :+ label.concat(" accuracy " + accuracy)

      println(label.concat(" accuracy " + accuracy))
    }
    predicted_results.foreach(println)
    spark.stop()
  }
}