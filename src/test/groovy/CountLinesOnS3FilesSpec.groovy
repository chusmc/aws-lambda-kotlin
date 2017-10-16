import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.event.S3EventNotification
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import org.apache.http.client.methods.HttpGet
import spock.lang.Specification

import java.time.Instant

class CountLinesOnS3FilesSpec  extends Specification {

    def "When input is null then a null pointer is thrown"() {
        given:
        def amazonS3 = Mock(AmazonS3)
        def lambdaClass = new CountLinesOnS3Files(amazonS3)

        when:
        lambdaClass.handleRequest(null, null)

        then:
        thrown NullPointerException
    }

    def "When input contains a S3 record, a output file with the line count is produced"() {
        def final BUCKET_NAME = 'testBucket'
        def final OBJECT_KEY = 'objectFolder/objectName.txt'
        def final CONTENT = 'testContentLine1\ntesContentLine2\ntestContentLine3'

        given:
        def amazonS3 = Mock(AmazonS3)
        def lambdaClass = new CountLinesOnS3Files(amazonS3)
        def s3ContentStream = new S3ObjectInputStream(new ByteArrayInputStream(CONTENT.bytes), new HttpGet("localhost"))

        amazonS3.getObject(BUCKET_NAME, OBJECT_KEY) >>
                new S3Object(bucketName: BUCKET_NAME, key: OBJECT_KEY, objectContent: s3ContentStream)

        and:
        def inputEvent = new S3Event([ newRecord(BUCKET_NAME, OBJECT_KEY) ])

        when:
        lambdaClass.handleRequest(inputEvent, null)

        then:
        1 * amazonS3.putObject(_ as String, _ as String, _ as InputStream, _ as ObjectMetadata) >> {
            String bucket, String key, InputStream inputStream, ObjectMetadata metadata ->
                assert bucket == BUCKET_NAME
                assert key == OBJECT_KEY + '.count'
                assert new String(inputStream.bytes) == CONTENT.readLines().size()
        }
    }

    private def newRecord(bucket, key) {
        new S3EventNotification.S3EventNotificationRecord(
                "awsRegion",
                "eventName",
                "eventSource",
                Instant.now().toString(),
                "eventVersion",
                null,
                null,
                new S3EventNotification.S3Entity(
                        "configurationId",
                        new S3EventNotification.S3BucketEntity(bucket, null, "arn"),
                        new S3EventNotification.S3ObjectEntity(key, 100, "etag", "versionId"),
                        "schemaVersion"),
                null)
    }

}
