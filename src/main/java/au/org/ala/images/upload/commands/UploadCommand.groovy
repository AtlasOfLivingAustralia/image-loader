package au.org.ala.images.upload.commands

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

        def uploadBatch = { batch ->

            def results = webService.uploadImages(batch)
            def statusBatch = []
            batch.each { srcImage ->
                def item = [sourceUrl: srcImage.sourceUrl, status: ""]
                if (results[srcImage.sourceUrl].success) {
                    item.status = "OK"
                    item.imageId = results[srcImage.sourceUrl].imageId
                }
                statusBatch << item
            }
            imageService.updateImageStatusBatch(statusBatch)

        }

        int batchSize = args.uploadBatchSize.toInteger()
        def batch = []
        int limit = args.limit.toInteger()
        AtomicInteger imagesUploaded = new AtomicInteger(0)

        imageService.eachImage { srcImage ->

            if (limit == 0 || count.get() < limit) {

                if (srcImage.status != 'OK') {
                    batch << srcImage.metaData
                    count.incrementAndGet()
                }

                if (batch.size() == batchSize) {
                    threadPool.submit {
                        uploadBatch(batch)
                        imagesUploaded.addAndGet(batchSize)
                        println "${imagesUploaded} images uploaded."
                    }
                    batch = []
                }
            }
        }

        if (batch) {
            threadPool.submit {
                uploadBatch(batch)
                println "${count} images uploaded."
            }
        }

        threadPool.shutdown()
        threadPool.awaitTermination(10, TimeUnit.DAYS)

        println "Done."
    }
}

