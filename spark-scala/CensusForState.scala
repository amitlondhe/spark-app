
/**
 * Scala script used to fetch the US 2010 census data.
 * Output is saved in a JSON file which is later used by demographic script
 * to add another dimension to the Yelp Business Data.
 * Please note that this script is built using various spark-scala API calls
 * made in the Spark Shell running on windows.
 */
object CensusForState {
  def def main(args: Array[String]) {
    //val cityStateCensusCode = sqlContext.read.json("C:/casnc/amit/installs/data/yelp-city-state-census.json")
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
    

    
  }
}