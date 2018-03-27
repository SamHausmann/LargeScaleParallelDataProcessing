package timeusage

import org.apache.spark.sql.{ColumnName, DataFrame, Row}
import org.apache.spark.sql.types.{
  DoubleType,
  StringType,
  StructField,
  StructType
}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.util.Random

@RunWith(classOf[JUnitRunner])
class TimeUsageSuite extends FunSuite with BeforeAndAfterAll {

// test I added
  test("row test") {
    import TimeUsage._
    val list = List("sam", "1.0", "2.0", "3.0")
    val res = (row(list) == Row("sam", 1.0, 2.0, 3.0))
    assert(res, "occurrencesOfLang given (specific) RDD with one element should equal to 1")
  }
}
