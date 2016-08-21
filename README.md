# Find Location for your next Restaurant

You can use this app to figure out which location may be the best to open a certain type of cuisine in the United States.

## Getting Started

* First screen asks you to select the type of cuisine you would want to start. 
* Based on the cuisine type selection, Geographic Map for U.S. shows up.
* The Map shows different states color coded from lighter to darker where ligher color stands for less average star rating.
* The average star rating per state per cuisine type is obtained by analyzing Yelp data available.
* You can click on any color coded U.S. State to see how the demographics might have played any role in obtaining certain rating. This is shown using the Sankey diagram outlining the relationship.
* The demographic data has been obtained using Spark script accessing <b> U.S Census API. </b>


## Constraints

* Yelp dataset does not include the statistics for all the states.
* The average rating spans from 1-5 and the Percentage of various races of people span from 0-100. Hence the Sankey diagram shows wider connection between State and Demographic divide than average rating for cuisines in the particular state.
* You can only choose from three cuisines at the moment and those are selected based on the number of reviews available for them in Yelp Dataset.

## Data Analysis

* All the data analysis has been done using <b> Scala on Apache Spark running on Windows desktop </b>.
* Yelp business dataset in JSON format includes a key named "categories" which lists comma separated categories for a particular business.
        <pre><code>
        val businesses = sqlContext.read.json("C:/amit/yelp/data/yelp_academic_dataset_business.json")
        </code></pre>
*  There are ~77K businesses in the dataset.
*  Exploding the dataset using "categories" element yields ~230K records.
      <pre><code>
        val busWithCategory = businesses.select("business_id","categories","city","state","latitude","longitude","stars").
          explode("categories", "category") {
          categories: WrappedArray[String] => categories.mkString(",").split(",") }
      </code></pre>
* Sample types of cuisines chose for the app are determined based on the number of businesses listed against those. This too is figured out using a Scala script ran on Spark.
      <pre><code>
        busWithCategory.select("business_id","category").groupBy("category").count().orderBy(desc("count")).show()
      </code></pre> 
* <b> Top category is "Restaurant" which is not a type of cuisine, however still selected solely based on the number of ratings we can work with for this demonstration. </b>
* The number of ratings for business selected are as below
      <pre><code>
      ----------------------
      Category    |  Count 
      ----------------------
      Restaurants |  25071
      Fast Food   |  2851
      Chinese     |  1629
      ----------------------
      </code></pre>
* For these cuisines, find out the average star rating obtained for them per state. These are then stored into state-cuisinetype-avg-rating.json for later use.
  <pre><code>
  val restaurants = busWithCategory.filter(upper($"category").equalTo("Restaurants".toUpperCase())).drop("categories")
  val restaurantsAvgStarsPerState = restaurants.groupBy($"state").avg("stars").select($"state",round($"avg(stars)",2).alias("avg_star"))

  val fastfoodRests = busWithCategory.filter(upper($"category").equalTo("fast food".toUpperCase()))
  val fastfoodRestAvgStarsPerState = fastfoodRests.groupBy($"state").avg("stars").select($"state",round($"avg(stars)",2).alias("avg_star"))
  
  val chineseRests = busWithCategory.filter(upper($"category").equalTo("chinese".toUpperCase())).drop("categories")
  val chineseRestsAvgStarsPerState = chineseRests.groupBy($"state").avg("stars").select($"state",round($"avg(stars)",2).alias("avg_star"))
  <pre><code>

* 
