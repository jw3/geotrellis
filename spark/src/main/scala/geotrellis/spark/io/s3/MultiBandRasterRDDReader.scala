package geotrellis.spark.io.s3

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.raster.{MultiBandTile, Tile}
import geotrellis.spark._
import geotrellis.spark.io.avro.codecs.TileCodecs
import geotrellis.spark.io.json._
import geotrellis.spark.io.{Cache, FilteringRasterRDDReader, AttributeCaching}
import geotrellis.spark.io.avro.AvroRecordCodec
import TileCodecs._
import org.apache.avro.Schema
import org.apache.spark.SparkContext
import spray.json.{JsObject, JsonFormat}
import spray.json.DefaultJsonProtocol._

import scala.reflect.ClassTag

class MultiBandRasterRDDReader[K: SpatialComponent: Boundable: AvroRecordCodec: JsonFormat: ClassTag]
(val attributeStore: S3AttributeStore, getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None)(implicit sc: SparkContext)
  extends FilteringRasterRDDReader[K, MultiBandRasterRDD] with AttributeCaching[S3LayerMetaData] with LazyLogging {

  val getS3Client: () => S3Client = () => S3Client.default
  val defaultNumPartitions = sc.defaultParallelism

  def read(id: LayerId, rasterQuery: RasterRDDQuery[K], numPartitions: Int): MultiBandRasterRDD[K] = {
    val metadata  = getLayerMetadata(id)
    val keyBounds = getLayerKeyBounds[K](id)
    val keyIndex  = getLayerKeyIndex[K](id)
    val bucket = metadata.bucket
    val prefix = metadata.key

    val rasterMetadata = metadata.rasterMetaData
    val queryKeyBounds = rasterQuery(rasterMetadata, keyBounds)
    val writerSchema: Schema = (new Schema.Parser).parse(attributeStore.read[JsObject](id, "schema").toString())
    val maxWidth = maxIndexWidth(keyIndex.toIndex(keyBounds.maxKey))
    val keyPath = (index: Long) => makePath(prefix, encodeIndex(index, maxWidth))
    val reader = new RDDReader[K, MultiBandTile](bucket, getS3Client)
    val cache = getCache.map(f => f(id))
    val rdd = reader.read(queryKeyBounds, keyIndex, keyPath, writerSchema, numPartitions, cache)
    new MultiBandRasterRDD[K](rdd, rasterMetadata)
  }
}