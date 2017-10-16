import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.event.S3EventNotification
import com.amazonaws.services.s3.model.ObjectMetadata

import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import javax.inject.Inject
import com.sun.xml.internal.ws.streaming.XMLStreamReaderUtil.close
import reactor.core.publisher.Flux.fromIterable
import java.io.FileReader
import java.io.BufferedReader



class CountLinesOnS3Files @Inject constructor(val s3Client: AmazonS3): RequestHandler<S3Event, String> {

    constructor() : this(AmazonS3ClientBuilder.defaultClient())

    override fun handleRequest(input: S3Event?, context: Context?): String {

        Flux.fromIterable(input!!.records).subscribe() { record ->
            val s3Object = s3Client.getObject(record.s3.bucket.name, getS3Key(record))

            Flux.using(
                    { s3Object.objectContent.bufferedReader() },
                    { reader -> Flux.fromStream(reader.lines()) },
                    { reader -> reader.close() }
            ).count().subscribe({ count ->  writeCountFile(record, count)})

        }

        return "OK"
    }

    private fun writeCountFile(record: S3EventNotification.S3EventNotificationRecord, count: Long) {
        val bucket = record.s3.bucket.name
        val objName = getS3Key(record) + ".count"

        val byteContent = "$count".toByteArray()

        val meta = ObjectMetadata()
        meta.contentType = "text/plain"
        meta.contentLength = byteContent.size.toLong()


        s3Client.putObject(bucket, objName, ByteArrayInputStream(byteContent), meta)
    }

}

private fun getS3Key(record: S3EventNotification.S3EventNotificationRecord) =
    URLDecoder.decode(record.s3.`object`.key.replace("+", " "), "UTF-8")

