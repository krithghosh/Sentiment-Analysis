import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.{LogisticRegression}
import org.apache.spark.ml.feature.{HashingTF, IDF, StandardScaler, Tokenizer}
import org.apache.spark.sql.SparkSession

class SentimentModel {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder.
      master("local[*]")
      .appName("SentimentAnalysis")
      .getOrCreate()

    val df = spark.read.format("csv").option("header", "true").
      load("src/main/resources/data2.csv")

    val labels = Array("anger", "positive", "anticipation", "disgust", "fear", "joy", "sadness", "surprise",
      "trust", "negative")

    val tokenizer = new Tokenizer().setInputCol("tweet").setOutputCol("words")
    val hashingTF = new HashingTF().setNumFeatures(1000).setInputCol("words").setOutputCol("rawFeatures")
    val idf = new IDF().setInputCol("rawFeatures").setOutputCol("features").setMinDocFreq(2)
    val scaler = new StandardScaler()
      .setInputCol("features")
      .setOutputCol("scaledFeatures")
      .setWithStd(true)
      .setWithMean(false)

    for (label <- labels) {
      df = df.withColumn(label, when(df(label) =!= 0, 1).otherwise(df(label)))
    }

    val Array(train, test) = df.randomSplit(Array(0.8, 0.2))

    var stages = Array(tokenizer, hashingTF, idf, scaler)

    for (label <- labels) {
      stages :+= new LogisticRegression().setLabelCol(label).setMaxIter(10).setRegParam(0.3).setElasticNetParam(0.8)
    }

    val pipeline = new Pipeline().setStages(stages)
    val model = pipeline.fit(train)
    val predictions = model.transform(test)
    print(predictions)
  }
}