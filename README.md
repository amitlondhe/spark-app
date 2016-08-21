# Find Location for your next Restaurant

You can use this app to figure out which location may be the best to open a certain type of cuisine in the United States.

## Getting Started

* First screen asks you to select the type of cuisine you would want to start. 
* Based on the cuisine type selection, Geographic Map for U.S. shows up.
* The Map shows different states color coded from lighter to darker where ligher color stands for less average star rating.
* The average star rating per state per cuisine type is obtained by analyzing Yelp data available.
* You can click on any color coded U.S. State to see how the demographics might have played any role in obtaining certain rating. This is shown using the Sankey diagram outlining the relationship.
* The demographic data has been obtained using Spark script accessing <b> U.S Census API. </b>


## Data Analysis

* All the data analysis has been done using <b> Scala on Apache Spark running on Windows desktop </b>.
* Yelp business dataset in JSON format includes a key named "categories" which lists comma separated categories for a particular business.
		<pre><code>
		val businesses = sqlContext.read.json("C:/amit/yelp/data/yelp_academic_dataset_business.json")
		</code></pre>
*  There are '~77K' businesses in the dataset.
*  Exploding the dataset using "categories" element yields '~230K' records.
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
* <b> Fetching Census Data as a Spark RDD </b>
* US 2010 Census data is used to add the Demographic dimension to the data analysis. 
* I have created a JSON file that contained mappings between US States and their FIPS codes. The census data needs to be retrieved using the IDs assigned to each
state.
* Below is the Scala script ran on Spark to get the Population data for U.S. 2010 Census.
		<pre><code>
		val cityStateCensusCode = sqlContext.read.json("C:/amit/yelp/data/yelp-city-state.json")
		  
			// Grab the demographics for the data retrieved above.
			val stateCensusCode = cityStateCensusCode.select("state","code","census_code").distinct()
			stateCensusCode.map(row => {println row})
			
			val censusdata = stateCensusCode.map(row => {
			  val stateCode = row(2)
			  val url = "http://api.census.gov/data/2010/sf1?key=48f46a5c4b5cea8481b12d8f0dc9e2fe416d3d50&get=P0010001,P0080003,P0080004,P0080006,NAME&for=state:" + stateCode;
			  val demographicsForState = scala.io.Source.fromURL(url).mkString;

			  val censusvalues = demographicsForState.split("],")(1).stripPrefix("\n[").stripSuffix("]]").split(",") //.map(c => { println("@@@@" + c + "@@@@") })
			  val jsonout = ("{\"totalpop\":").concat(censusvalues(0)).concat(",\"totalwhites\":").concat(censusvalues(1)).
			  concat(",\"totalblack\":").concat(censusvalues(2)).concat(",\"totalasian\":").concat(censusvalues(3)).concat(",\"state\":")
			  .concat(censusvalues(4)).concat(",\"state_code\":").concat(censusvalues(5)).
			  concat(",\"percentasian\":").concat(censusvalues(3)*100/censusvalues(0)).
			  concat(",\"percentblack\":").concat(censusvalues(2)*100/censusvalues(0)).
			  concat(",\"percentwhite\":").concat(censusvalues(1)*100/censusvalues(0)).
			  concat("}")
			  // return the json constructed
			  jsonout
			})
			
			// Save as a text output. Please note censusdata is a RDD and not DataFrame.
			censusdata.coalesce(1).saveAsTextFile("C:/amit/yelp/data/city-state-census")
		</code></pre>
* Census and Yelp data are then joined for each state to plot the Sankey diagram which shows the demographic distribution and type of cuisine
preferred in that area.
		<pre><code>
		val stateCensusData = sqlContext.read.json("C:/amit/yelp/data/state-census-data.json")
		  
		  val requiredCensusData = stateCensusData.select("state","percentasian","percentblack","percentwhite")
		  val censusWithCuisine = requiredCensusData.join(cuisinetypeAvgRatingPerstate,"state").orderBy("state")
		  val censusWithCuisineSankeyData = censusWithCuisine.select("state","percentasian","percentblack","percentwhite","avg_rating","cuisine_type").map( row => {
			   val cuisineType = JSONArray(List(row.getString(4),row.getString(5),row.getString(0)))
			   val asian = JSONArray(List(row.getString(0),"percentasian",row.getString(1)))
			   val black = JSONArray(List(row.getString(0),"percentblack",row.getString(2)))
			   val white = JSONArray(List(row.getString(0),"percentwhite",row.getString(3)))
			   
			   val sankeydata = JSONArray(List(cuisineType,asian,black,white))
			   val jsonobj = JSONObject(Map(row.getString(0)->sankeydata))
			   jsonobj
		  })
		  censusWithCuisineSankeyData.coalesce(1).saveAsTextFile("C:/amit/yelp/data/percentperstate")
		</code></pre>

## Constraints

* Yelp dataset does not include the statistics for all the states.
* The average rating spans from 1-5 and the Percentage of various races of people span from 0-100. Hence the Sankey diagram shows wider connection between State and Demographic divide than average rating for cuisines in the particular state.
* You can only choose from three cuisines at the moment and those are selected based on the number of reviews available for them in Yelp Dataset.		
		
## Challenges