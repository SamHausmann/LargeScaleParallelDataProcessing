package wikipedia

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import org.apache.spark.rdd.RDD

case class WikipediaArticle(title: String, text: String) {
  /**
    * @return Whether the text of this article mentions `lang` or not
    * @param lang Language to look for (e.g. "Scala")
    */
  def mentionsLanguage(lang: String): Boolean = text.split(' ').contains(lang)
}

object WikipediaRanking {

  val langs = List(
    "JavaScript", "Java", "PHP", "Python", "C#", "C++", "Ruby", "CSS",
    "Objective-C", "Perl", "Scala", "Haskell", "MATLAB", "Clojure", "Groovy")

  val conf: SparkConf = new SparkConf()
      .setAppName("wikipedia")
      .setMaster("local")
  val sc: SparkContext = new SparkContext(conf)
  // Hint: use a combination of `sc.textFile`, `WikipediaData.filePath` and `WikipediaData.parse`
  val wikiRdd: RDD[WikipediaArticle] = sc.textFile(WikipediaData.filePath).map(x => WikipediaData.parse(x)).persist()

  /** Returns the number of articles on which the language `lang` occurs.
   *  Hint1: consider using method `aggregate` on RDD[T].
   *  Hint2: consider using method `mentionsLanguage` on `WikipediaArticle`
   */
  def occurrencesOfLang(lang: String, rdd: RDD[WikipediaArticle]): Int = {
    // filter the rdd to elements that have occurences of the specified language, then return the count.
    // need to convert to Int because '.count' returns a Long. Might switch to aggregate once I figure out how it works
    rdd
    .filter(_.mentionsLanguage(lang))
    .count
    .toInt
  }

  /* (1) Use `occurrencesOfLang` to compute the ranking of the languages
   *     (`val langs`) by determining the number of Wikipedia articles that
   *     mention each language at least once. Don't forget to sort the
   *     languages by their occurrence, in decreasing order!
   *
   *   Note: this operation is long-running. It can potentially run for
   *   several seconds.
   */
  def rankLangs(langs: List[String], rdd: RDD[WikipediaArticle]): List[(String, Int)] = {
    
    // map each language to a tuple where the key is the language and the value is an Int representing
    // how many occurences of the language there was in the RDD of Wikipedia Articles
    langs
    .map(x => (x, occurrencesOfLang(x, rdd)))
    .sortBy(-_._2)
  }

  /* Compute an inverted index of the set of articles, mapping each language
   * to the Wikipedia pages in which it occurs.
   */
  def makeIndex(langs: List[String], rdd: RDD[WikipediaArticle]): RDD[(String, Iterable[WikipediaArticle])] = {

    /* Helper function to get all the langs for a particular WikipediaArticle in a list and maps
     * the article to the filtered list
     */
    def getLangs(langs: List[String], wiki: WikipediaArticle): List[(String, WikipediaArticle)] = {

      // filter languages by prevalent languages in the article and then create a list of language -> article mappings
        langs
        .filter(l => (wiki.mentionsLanguage(l)))
        .map(x => (x -> wiki))
    }

      // flatten the returned list -> article lists from getLangs and then group the articles by keys
      // ex: all articles with key 'Java' are grouped and then assigned to the aforementioned key
      rdd
      .flatMap(x => (getLangs(langs, x)))
      .groupByKey
  }

  /* (2) Compute the language ranking again, but now using the inverted index. Can you notice
   *     a performance improvement?
   *
   *   Note: this operation is long-running. It can potentially run for
   *   several seconds.
   */
  def rankLangsUsingIndex(index: RDD[(String, Iterable[WikipediaArticle])]): List[(String, Int)] = {

    // map the values (the Iterable of WikipediaArticles) in each tuple in the index to its size to get
    // the number of occurrences, then transform the RDD into a list and sort in reverse by the values (largest to smallest)
    index
    .mapValues(_.size)
    .collect
    .toList
    .sortBy(-_._2)
    
  }

  /* (3) Use `reduceByKey` so that the computation of the index and the ranking are combined.
   *     Can you notice an improvement in performance compared to measuring *both* the computation of the index
   *     and the computation of the ranking? If so, can you think of a reason?
   *
   *   Note: this operation is long-running. It can potentially run for
   *   several seconds.
   */
  def rankLangsReduceByKey(langs: List[String], rdd: RDD[WikipediaArticle]): List[(String, Int)] = {

    /* Helper function to produce a list of tuples which represents each language occurence in a Wikipedia Article.
     * The second value of the tuple just just the Int 1, so when you reduceByKey in 'rankLangsReducedByKey', you can
     * see how many occurences there are of each language
     */
    def addLang(langs: List[String], wiki: WikipediaArticle): List[(String, Int)] = {
      // filter languages by prevalent languages in the article and then create a list of language -> article mappings
        langs
        .filter(l => (wiki.mentionsLanguage(l)))
        .map(x => (x, 1))
    }

    rdd.flatMap(x => (addLang(langs, x)))
    .reduceByKey(_+_)
    .sortBy(- _._2)
    .collect
    .toList
    
  }

  def main(args: Array[String]) {

    /* Languages ranked according to (1) */
    val langsRanked: List[(String, Int)] = timed("Part 1: naive ranking", rankLangs(langs, wikiRdd))

    /* An inverted index mapping languages to wikipedia pages on which they appear */
    def index: RDD[(String, Iterable[WikipediaArticle])] = makeIndex(langs, wikiRdd)

    /* Languages ranked according to (2), using the inverted index */
    val langsRanked2: List[(String, Int)] = timed("Part 2: ranking using inverted index", rankLangsUsingIndex(index))

    /* Languages ranked according to (3) */
    val langsRanked3: List[(String, Int)] = timed("Part 3: ranking using reduceByKey", rankLangsReduceByKey(langs, wikiRdd))

    /* Output the speed of each ranking */
    println(timing)
    sc.stop()
  }

  val timing = new StringBuffer
  def timed[T](label: String, code: => T): T = {
    val start = System.currentTimeMillis()
    val result = code
    val stop = System.currentTimeMillis()
    timing.append(s"Processing $label took ${stop - start} ms.\n")
    result
  }
}
