import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.functions._
import scala.collection.mutable.WrappedArray
import org.apache.spark.sql._
import scala.util.parsing.json.JSONArray
import scala.util.parsing.json.JSONObject
import scala.collection.immutable.HashMap
/**
 * Scala script used to analyze the Yelp business data.
 * Please note that this script is built using various spark-scala API calls
 * made in the Spark Shell running on windows.
 */
object BusinessByDemographics {
  def main(args: Array[String]) {
  val sparkConf = new SparkConf().setAppName("BusinessByDemographics")
  val sc = new SparkContext(sparkConf)
  val sqlContext = new SQLContext(sc)

  //val businesses = sqlContext.read.json("C:/casnc/amit/installs/data/sample.json")
  val businesses = sqlContext.read.json("C:/amit/yelp/data/yelp_academic_dataset_business.json")
  val busWithCategory = businesses.select("business_id","categories","city","state","latitude","longitude","stars").
  explode("categories", "category") {
  categories: WrappedArray[String] => categories.mkString(",").split(",") }

  // List down distinct restaurant categories
  val distinctRestCategories = busWithCategory.distinct()

  // Filter type of restaurants Chinese,Indian, American, Mexican etc.
  // Overall Restaurants and average star rating per state
  val restaurants = busWithCategory.filter(upper($"category").equalTo("Restaurants".toUpperCase())).drop("categories")
  val restaurantsAvgStarsPerState = restaurants.groupBy($"state").avg("stars").select($"state",round($"avg(stars)",2).alias("avg_star"))
 
  val fastfoodRests = busWithCategory.filter(upper($"category").equalTo("fast food".toUpperCase()))
  val fastfoodRestAvgStarsPerState = fastfoodRests.groupBy($"state").avg("stars").select($"state",round($"avg(stars)",2).alias("avg_star"))

  val chineseRests = busWithCategory.filter(upper($"category").equalTo("chinese".toUpperCase())).drop("categories")
  val chineseRestsAvgStarsPerState = chineseRests.groupBy($"state").avg("stars").select($"state",round($"avg(stars)",2).alias("avg_star"))
  
  // Store the average calculated above in "state-cuisinetype-avg-rating.json"
  
  val cuisinetypeAvgRatingPerstate = sqlContext.read.json("C:/amit/yelp/data/state-cuisinetype-avg-rating.json").orderBy("state")
  // Merge the different cuisines for a State together in a JSON Array
  
  val stateCensusData = sqlContext.read.json("C:/amit/yelp/data/state-census-data.json")
  
  val requiredCensusData = stateCensusData.select("state","percentasian","percentblack","percentwhite")
  val censusWithCuisine = requiredCensusData.join(cuisinetypeAvgRatingPerstate,"state").orderBy("state")
  val censusWithCuisineSankeyData = censusWithCuisine.select("state","percentasian","percentblack","percentwhite","avg_rating","cuisine_type").map( row => {
       val cuisineType = JSONArray(List(((row.getDouble(4)*100)/5).toString(),row.getString(5),row.getString(0),"Average Rating".concat(row.getString(4))))
       val asian = JSONArray(List(row.getString(0),"percentasian",row.getString(1),"Percent Asian"))
       val black = JSONArray(List(row.getString(0),"percentblack",row.getString(2),"Percent Black"))
       val white = JSONArray(List(row.getString(0),"percentwhite",row.getString(3),"Percent White"))
       
       val sankeydata = JSONArray(List(cuisineType,asian,black,white))
       val jsonobj = JSONObject(Map(row.getString(0)->sankeydata))
       jsonobj
  })
  censusWithCuisineSankeyData.coalesce(1).saveAsTextFile("C:/amit/yelp/data/percentperstate")
  
  
  
     
 
   
   /* Schema for the joined DataFrame
    root
     |-- state: string (nullable = true)
     |-- avg_rating: string (nullable = true)
     |-- cuisine_type: string (nullable = true)
     |-- percentasian: string (nullable = true)
     |-- percentblack: string (nullable = true)
     |-- percentwhite: string (nullable = true)
     |-- state_code: string (nullable = true)
     |-- totalasian: string (nullable = true)
     |-- totalblack: string (nullable = true)
     |-- totalpop: string (nullable = true)
     |-- totalwhites: string (nullable = true)
  */
  val cuisineRatingWithCenusData = cuisinetypeAvgRatingPerstate.join(stateCensusData,"state")

  

  
  
  }
}

/*
 
 Output of Average rating per state

 Average star rating for "Chinese"
 ['US-NC', 3.22],
 ['US-NV', 3.29],
 ['US-AZ', 3.32],
 ['US-IL', 3.41],
 ['US-PA', 3.28],
 ['US-WI', 3.34],
 ['US-SC', 3.73]
 
Average star rating for "Restaurants"
  ['US-NC',3.41],
  ['US-TX', 4.0],
  ['US-NM', 3.0],
  ['US-NV',3.41],
  ['US-AZ',3.42],
  ['US-NW', 4.5],
  ['US-IL',3.35],
  ['US-PA',3.56],
  ['US-WI',3.45],
  ['US-SC',3.34]
 
Average star rating for "Fast Food"

['US- NC', 3.04],
['US- NV', 2.89],
['US- AZ', 2.92],
['US- NW',  4.5],
['US- IL', 2.68],
['US- PA', 2.89],
['US- WI', 3.02],
['US- SC', 2.94]


*/