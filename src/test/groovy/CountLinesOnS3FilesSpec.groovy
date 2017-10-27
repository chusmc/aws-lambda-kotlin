import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.event.S3EventNotification
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import org.apache.http.client.methods.HttpGet
import spock.lang.Specification
import com.amazonaws.services.lambda.runtime.LambdaLogger

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

        and:
        def mockContext = Mock(Context)
        def mockLogger = Mock(LambdaLogger)
        mockContext.logger >> mockLogger

        and:
        amazonS3.getObject(BUCKET_NAME, OBJECT_KEY) >>
                new S3Object(bucketName: BUCKET_NAME, key: OBJECT_KEY, objectContent: s3ContentStream)

        and:
        def inputEvent = new S3Event([ newRecord(BUCKET_NAME, OBJECT_KEY) ])

        when:
        lambdaClass.handleRequest(inputEvent, mockContext)

        then:
        1 * mockLogger.log("file objectFolder/objectName.txt has 3 lines")

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
