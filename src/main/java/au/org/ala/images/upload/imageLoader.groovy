

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
        // def serviceBaseUrl = "http://images.ala.org.au"
        def serviceBaseUrl = "http://devt.ala.org.au:8080/ala-images"
        def threadCount = 4;

        for (int i = 0; i < args.length; ++i) {
            def arg = args[i]
            switch (arg) {
                case "-f":
                    if (i < args.length - 1) {
                        i += 1
                        filename = args[i]
                    } else {
                        println "Missing argument for -f (filename)!"
                    }
                    break;
                case "-t":
                    if (i < args.length - 1) {
                        i += 1
                        threadCount = Integer.parseInt(args[i])
                    } else {
                        println "Missing argument for -t (thread count)"
                    }
                    break;
                case "-s":
                    if (i < args.length - 1) {
                        i += 1
                        serviceBaseUrl =  args[i]
                    } else {
                        println "Missing argument for -s (image service base url)"
                    }

                    break;
                case "-?":
                    usage()
                    System.exit(0)
                    break
            }
        }

        if (args.length == 1) {
            filename = args[0]
        }

        if (filename) {
            def f = new File(filename)
            if (f.exists()) {
                println "Using ${threadCount} threads"
                println "Service base url: ${serviceBaseUrl}"

                processFile(f, serviceBaseUrl, threadCount)
                System.exit(0)
            }
        }


        println "Missing or invalid file!"
    }

    private static void processFile(File file, String serviceBaseUrl, int threadCount) {

        def jobDispatcher = new JobDispatcher(threadCount, serviceBaseUrl)

        println "Processing file ${file.absolutePath}..."

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

                def uploadJob = new UploadJob(imageUrl: imageUrl, metaData: metadata)
                jobDispatcher.addJob(uploadJob)

                if (++count % 1000 == 0) {
                    println "${count} urls queued"
                }

            } catch (Exception ex) {
                println "Error on line ${count}: ${ex.message}"
            }
        }

        jobDispatcher.waitUntilFinished()

        def now = new Date()
        def expiredMillis = now.getTime() - startTimeMillis
        def ratePerSecond = (count / expiredMillis) * 1000
        println "Upload complete. ${count} image urls sent (averaging ${ratePerSecond} images per second)"
    }

    private static void usage() {
        println "Usage:"
        println "au.org.ala.images.upload.imageLoader <csvfile>"
    }
}
