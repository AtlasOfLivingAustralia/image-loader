package au.org.ala.images.upload.commands

import au.org.ala.images.upload.service.ImageService
import au.org.ala.images.upload.service.WebService

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class CheckStatusCommand extends AbstractCommand {

    @Override
    String getCommandName() {
        return "checkstatus"
    }

    @Override
    List<CommandArgument> getAcceptedArgs() {
        return [CommandArguments.databaseFile, CommandArguments.serviceBaseUrl, CommandArguments.statusBatchSize, CommandArguments.threadCount]
    }

    @Override
    void executeImpl(Map args) {

        int count = 0
        int batchSize = args.statusBatchSize.toInteger();
        def threadCount = args.threadCount.toInteger();

        def threadPool = Executors.newFixedThreadPool(threadCount);

        def batch = []
        def context = new UpdateStatusJobContext(webService: webService, imageService: imageService)
        imageService.eachImageUrl { imageUrl ->

            batch << [sourceUrl: imageUrl]

            if (++count % batchSize == 0) {
                threadPool.submit(new UpdateStatusJob(batch, context))
                batch = []
            }
        }

        if (batch) {
            threadPool.submit(new UpdateStatusJob(batch, context))
        }

        threadPool.shutdown()
        threadPool.awaitTermination(3, TimeUnit.DAYS)
        println "Done. ${context.count} Statuses updated."
    }
}

public class UpdateStatusJobContext {

    AtomicInteger count = new AtomicInteger(0)
    WebService webService
    ImageService imageService

}


class UpdateStatusJob implements Runnable {

    private List _batch
    private UpdateStatusJobContext _context

    public UpdateStatusJob(List batch, UpdateStatusJobContext context) {
        _batch = batch
        _context = context
    }

    @Override
    void run() {
        try {
            def imageInfoList = _context.webService.getImageInfo(_batch)
            _batch.each { item ->
                def status = ""
                def imageId = ""
                def imageData = imageInfoList[item.sourceUrl]
                if (imageData && imageData.imageId) {
                    status = "OK"
                    imageId = imageData.imageId
                }
                item.status = status
                item.imageId = imageId
            }
            _context.imageService.updateImageStatusBatch(_batch)
            _context.count.addAndGet(_batch.size())
            println("${_context.count.get()} statuses updated.")
        } catch (Exception ex) {
            ex.printStackTrace()
            throw ex
        }
    }

}
