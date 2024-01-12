import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.sql.{DataFrame, SparkSession}


object ProductRec {

  private def readCSV(filePath: String, spark: SparkSession): DataFrame = {
    spark.read
      .option("header", value = true)
      .option("delimiter", ",")
      .option("inferSchema", "true")
      .csv(filePath)
  }

  def main(args: Array[String]): Unit = {
    // Initialize Spark session
    val spark = SparkSession.builder()
      .appName("Amazon")
      .master("local[*]")
      .getOrCreate()

    // Path to the CSV file
    val filePath = "amazon.csv"

    // Read CSV data into a DataFrame
    val data = readCSV(filePath, spark)

    // Select relevant columns for the linear regression model
    val orderedFeatures: Array[String] = Array("discounted_price", "actual_price", "rating", "rating_count")
    val predictColumn: String = "discount_percentage"

    val orderedColumns = orderedFeatures :+ predictColumn

    val orderedTrainData = data.select(orderedColumns.head, orderedColumns.tail: _*)

    val cleanedTrainData = orderedTrainData.na.drop()

    val seed = 1234L

    val shuffledData = cleanedTrainData.orderBy(org.apache.spark.sql.functions.rand(seed))
    val Array(trainingData, testingData) = shuffledData.randomSplit(Array(0.8, 0.2), seed = seed)

    val featureCols = orderedFeatures.map(col => s"$col")

    val featureAssembler = new VectorAssembler()
      .setInputCols(featureCols)
      .setOutputCol("features")

    val pipeline = new Pipeline()
      .setStages(Array(featureAssembler))

    val fittedPipeline = pipeline.fit(trainingData)
    val preproTrainDf = fittedPipeline.transform(trainingData)
    val preproTestDf = fittedPipeline.transform(testingData)

    // Linear Regression
    val lr = new LinearRegression()
      .setLabelCol("discount_percentage")
      .setFeaturesCol("features")
      .setMaxIter(100)
      .setRegParam(0.1)
      .setElasticNetParam(0.8)

    // Fit the model with train data
    val model = lr.fit(preproTrainDf)

    // Make predictions on the test data
    val predictions = model.transform(preproTestDf)

    // Showing the predictions
    println(predictions.show())

    // Evaluate the models using RMSE
    val evaluator = new RegressionEvaluator()
      .setLabelCol("discount_percentage")
      .setPredictionCol("prediction")
      .setMetricName("rmse")

    val lrRMSE = evaluator.evaluate(predictions)

    println(s"Linear Regression RMSE on test data: $lrRMSE")

    // Stop the Spark session
    spark.stop()
  }
}