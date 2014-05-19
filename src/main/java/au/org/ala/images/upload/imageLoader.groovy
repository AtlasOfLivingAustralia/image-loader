

package au.org.ala.images.upload

import au.com.bytecode.opencsv.CSVReader
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import net.sf.json.JSON
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.StringBody

import java.text.SimpleDateFormat

class imageLoader {

    static void main(String[] args) {
        if (!args) {
            usage()
            System.exit(1);
        }

        def filename = ""
        def serviceBaseUrl = "http://images.ala.org.au"
        // def serviceBaseUrl = "http://devt.ala.org.au:8080/ala-images"

        if (args.length == 1) {
            filename = args[0]
        }

        if (filename) {
            def f = new File(filename)
            if (f.exists()) {
                processFile(f, serviceBaseUrl)
                System.exit(0)
            }
        }

        println "Missing or invalid file!"
    }

    private static void processFile(File file, String serviceBaseUrl) {

        println "Processing file ${file.absolutePath}"

        def sdf = new SimpleDateFormat("yyyyMMddHHmmss")
        def importBatchId = sdf.format(new Date())
        String [] fields;
        CSVReader reader = new CSVReader(new FileReader(file));
        int count = 0
        def startTimeMillis = new Date().getTime()
        while ((fields = reader.readNext()) != null) {
            try {
                def imageUrl = fields[0]
                def metadata = [:]
                metadata.sourceUrl = fields[0]
                metadata.occurrenceId = fields[1]
                metadata.dataResourceUid = fields[2]
                metadata.collectionCode = fields[3]
                metadata.institutionCode = fields[4]
                metadata.scientificName = fields[5]
                metadata.importBatchId = importBatchId

                if (!sendUploadRequest(serviceBaseUrl, imageUrl, true, metadata)) {
                    println "Upload request failed line ${count}"
                }

                if (++count % 50 == 0) {
                    def now = new Date()
                    def expiredMillis = now.getTime() - startTimeMillis
                    def ratePerSecond = (count / expiredMillis) * 1000
                    println "${count} image urls sent (averaging ${ratePerSecond} images per second)"
                }
            } catch (Exception ex) {
                println "Error on line ${count}: ${ex.message}"
            }
        }
        def now = new Date()
        def expiredMillis = now.getTime() - startTimeMillis
        def ratePerSecond = (count / expiredMillis) * 1000
        println "Upload complete. ${count} image urls sent (averaging ${ratePerSecond} images per second)"
    }

    private static boolean sendUploadRequest(String serviceBase, String imageUrl, boolean detectDuplicate, Map metadata) {
        def results = postMultipart("${serviceBase}/ws/uploadImage", imageUrl, detectDuplicate, metadata)
        return results?.status == 200 && (results.content.success as Boolean)
    }

    static def postMultipart(url, String imageUrl, boolean detectDuplicate, Map metadata) {

        def result = [:]
        HTTPBuilder builder = new HTTPBuilder(url)
        builder.request(Method.POST) { request ->

            requestContentType : 'multipart/form-data'
            MultipartEntity content = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE)
            content.addPart("url", new StringBody(imageUrl))
            if (detectDuplicate) {
                content.addPart("detectDuplicate", new StringBody(detectDuplicate.toString()))
            }
            def json = new JsonBuilder(metadata).toString()
            content.addPart("metadata", new StringBody(json))

            request.setEntity(content)

            response.success = {resp, message ->
                result.status = resp.status
                result.content = message
            }

            response.failure = {resp ->
                result.status = resp.status
                result.error = "Error POSTing to ${url}"
            }

        }
        result
    }



    private static void usage() {
        println "Usage:"
        println "au.org.ala.images.upload.imageLoader <csvfile>"
    }
}
