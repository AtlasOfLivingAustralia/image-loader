package au.org.ala.images.upload.commands

import au.org.ala.images.upload.UploadJob
import au.org.ala.images.upload.service.ImageService
import au.org.ala.images.upload.service.WebService

import java.text.SimpleDateFormat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class UploadCommand extends AbstractCommand {

    @Override
    String getCommandName() {
        return "upload"
    }

    @Override
    List<CommandArgument> getAcceptedArgs() {
        return [CommandArguments.databaseFile, CommandArguments.serviceBaseUrl, CommandArguments.uploadBatchSize, CommandArguments.limit, CommandArguments.threadCount]
    }

    @Override
    void executeImpl(Map args) {

        AtomicInteger count = new AtomicInteger(0)
        def threadCount = args.threadCount.toInteger()
        def threadPool = Executors.newFixedThreadPool(threadCount);

        int batchSize = args.uploadBatchSize.toInteger()
        def batch = []
        int limit = args.limit.toInteger()

        def sdf = new SimpleDateFormat("yyyyMMddHHmmss")
        def importBatchId = sdf.format(new Date())

        AtomicInteger batchId = new AtomicInteger(0)

        def context = new UploadJobContext(webService: webService, imageService: imageService, importBatchId: importBatchId)

        imageService.eachImage { srcImage ->

            if (limit == 0 || count.get() < limit) {

                if (srcImage.status != 'OK') {
                    def index = count.incrementAndGet()
                    srcImage.metaData.batchSequenceNumber = index
                    srcImage.metaData.importBatchId = importBatchId
                    batch << srcImage.metaData
                }

                if (batch.size() == batchSize) {
                    batchId.incrementAndGet()
                    threadPool.submit(new UploadJob(batchId.get(), batch, context))
                    batch = []
                }
            }
        }

        if (batch) {
            batchId.incrementAndGet()
            threadPool.submit(new UploadJob(batchId.get(), batch, context))
        }

        threadPool.shutdown()
        threadPool.awaitTermination(10, TimeUnit.DAYS)

        println "Done. ${context.uploadCount.get()} uploaded with ${context.errorCount.get()} errors."
    }
}

public class UploadJobContext {

    AtomicInteger uploadCount = new AtomicInteger(0)
    AtomicInteger errorCount = new AtomicInteger(0)
    WebService webService
    ImageService imageService
    String importBatchId

}


