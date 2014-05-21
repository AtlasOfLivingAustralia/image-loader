package au.org.ala.images.upload.commands

class CheckStatusCommand extends AbstractCommand {

    @Override
    String getCommandName() {
        return "checkstatus"
    }

    @Override
    List<CommandArgument> getAcceptedArgs() {
        return [CommandArguments.databaseFile, CommandArguments.serviceBaseUrl, CommandArguments.statusBatchSize]
    }

    @Override
    void executeImpl(Map args) {

        int count = 0
        int batchSize = args.statusBatchSize;
        int updateTotal = 0

        def updateBatch = { batch ->

            def imageInfo = webService.getImageInfo(batch)
            batch.each { item ->
                def status = ""
                if (imageInfo[item.sourceUrl]) {
                    status = "OK"
                }
                item.status = status
            }
            imageService.updateImageStatusBatch(batch)
        }

        def batch = []
        imageService.eachImageUrl { imageUrl ->

            batch << [sourceUrl: imageUrl]

            if (++count % batchSize == 0) {

                updateBatch(batch)

                batch = []
                updateTotal += batchSize
                println "${updateTotal} statuses updated."
            }
        }

        if (batch) {
            updateBatch(batch)
            println "${updateTotal + batch.size()} statuses updated."
        }
        println "Done."
    }
}
